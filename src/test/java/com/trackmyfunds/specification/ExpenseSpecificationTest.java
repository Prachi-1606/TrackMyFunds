package com.trackmyfunds.specification;

import com.trackmyfunds.dto.ExpenseFilterDTO;
import com.trackmyfunds.enums.Category;
import com.trackmyfunds.enums.PaymentMethod;
import com.trackmyfunds.model.Expense;
import com.trackmyfunds.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-style tests that run specifications against a real H2 schema.
 * @DataJpaTest replaces the datasource with an embedded H2 and rolls back
 * every test, so each test starts with only the data saved in @BeforeEach.
 */
@DataJpaTest
@ActiveProfiles("test")
class ExpenseSpecificationTest {

    @Autowired
    private ExpenseRepository repository;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        save("Groceries",  "500.00",  Category.FOOD,      PaymentMethod.CARD,       LocalDate.of(2024, 1, 10), null);
        save("Restaurant", "1200.00", Category.FOOD,      PaymentMethod.UPI,        LocalDate.of(2024, 2, 15), "dinner out");
        save("Metro Card", "300.00",  Category.TRANSPORT, PaymentMethod.CASH,       LocalDate.of(2024, 1, 20), null);
        save("Medicine",   "800.00",  Category.HEALTH,    PaymentMethod.NETBANKING, LocalDate.of(2024, 3,  5), "pharmacy bill");
    }

    private void save(String title, String amount, Category cat,
                      PaymentMethod pm, LocalDate date, String desc) {
        repository.save(Expense.builder()
                .title(title).amount(new BigDecimal(amount)).category(cat)
                .paymentMethod(pm).date(date).description(desc).build());
    }

    // ── no filter ─────────────────────────────────────────────────────────────

    @Test
    void emptyFilter_returnsAllRows() {
        assertThat(repository.findAll(ExpenseFilterDTO.empty().toSpecification())).hasSize(4);
    }

    // ── category ──────────────────────────────────────────────────────────────

    @Test
    void byCategory_food_returnsTwoMatchingRows() {
        ExpenseFilterDTO f = new ExpenseFilterDTO(Category.FOOD, null, null, null, null, null, null);
        List<Expense> result = repository.findAll(f.toSpecification());
        assertThat(result)
                .hasSize(2)
                .allMatch(e -> e.getCategory() == Category.FOOD);
    }

    // ── payment method ────────────────────────────────────────────────────────

    @Test
    void byPaymentMethod_upi_returnsSingleRow() {
        ExpenseFilterDTO f = new ExpenseFilterDTO(null, PaymentMethod.UPI, null, null, null, null, null);
        List<Expense> result = repository.findAll(f.toSpecification());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Restaurant");
    }

    // ── date range ────────────────────────────────────────────────────────────

    @Test
    void byDateRange_bothBounds_inclusiveMatch() {
        Specification<Expense> spec = ExpenseSpecification.byDateRange(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        List<Expense> result = repository.findAll(spec);

        assertThat(result).hasSize(2)     // Groceries (Jan 10) + Metro Card (Jan 20)
                .allMatch(e -> !e.getDate().isBefore(LocalDate.of(2024, 1, 1))
                            && !e.getDate().isAfter(LocalDate.of(2024, 1, 31)));
    }

    @Test
    void byDateRange_onlyFrom_returnsOnOrAfter() {
        Specification<Expense> spec = ExpenseSpecification.byDateRange(
                LocalDate.of(2024, 2, 1), null);

        List<Expense> result = repository.findAll(spec);

        assertThat(result).hasSize(2)     // Restaurant (Feb) + Medicine (Mar)
                .allMatch(e -> !e.getDate().isBefore(LocalDate.of(2024, 2, 1)));
    }

    @Test
    void byDateRange_onlyTo_returnsOnOrBefore() {
        Specification<Expense> spec = ExpenseSpecification.byDateRange(
                null, LocalDate.of(2024, 1, 31));

        List<Expense> result = repository.findAll(spec);

        assertThat(result).hasSize(2);    // Groceries (Jan 10) + Metro Card (Jan 20)
    }

    // ── amount range ──────────────────────────────────────────────────────────

    @Test
    void byAmountRange_bothBounds_inclusiveMatch() {
        Specification<Expense> spec = ExpenseSpecification.byAmountRange(
                new BigDecimal("400.00"), new BigDecimal("900.00"));

        List<Expense> result = repository.findAll(spec);

        assertThat(result).hasSize(2)     // Groceries (500) + Medicine (800)
                .allMatch(e -> e.getAmount().compareTo(new BigDecimal("400.00")) >= 0
                            && e.getAmount().compareTo(new BigDecimal("900.00")) <= 0);
    }

    @Test
    void byAmountRange_onlyMin_returnsAtLeastMin() {
        Specification<Expense> spec = ExpenseSpecification.byAmountRange(
                new BigDecimal("800.00"), null);

        List<Expense> result = repository.findAll(spec);

        assertThat(result).hasSize(2)     // Restaurant (1200) + Medicine (800)
                .allMatch(e -> e.getAmount().compareTo(new BigDecimal("800.00")) >= 0);
    }

    @Test
    void byAmountRange_onlyMax_returnsAtMostMax() {
        Specification<Expense> spec = ExpenseSpecification.byAmountRange(
                null, new BigDecimal("500.00"));

        List<Expense> result = repository.findAll(spec);

        assertThat(result).hasSize(2);    // Groceries (500) + Metro Card (300)
    }

    // ── keyword ───────────────────────────────────────────────────────────────

    @Test
    void byKeyword_matchesTitle_caseInsensitive() {
        List<Expense> result = repository.findAll(ExpenseSpecification.byKeyword("METRO"));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Metro Card");
    }

    @Test
    void byKeyword_matchesDescription() {
        List<Expense> result = repository.findAll(ExpenseSpecification.byKeyword("pharmacy"));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(Category.HEALTH);
    }

    // ── combined filters ──────────────────────────────────────────────────────

    @Test
    void combined_categoryAndMinAmount_returnsIntersection() {
        // FOOD with amount >= 1000 → only Restaurant (1200)
        ExpenseFilterDTO f = new ExpenseFilterDTO(
                Category.FOOD, null, null, null, new BigDecimal("1000.00"), null, null);

        List<Expense> result = repository.findAll(f.toSpecification());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Restaurant");
    }
}
