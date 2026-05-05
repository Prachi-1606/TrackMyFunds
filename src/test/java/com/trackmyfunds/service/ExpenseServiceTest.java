package com.trackmyfunds.service;

import com.trackmyfunds.dto.DashboardDTO;
import com.trackmyfunds.dto.ExpenseFilterDTO;
import com.trackmyfunds.dto.ExpenseRequestDTO;
import com.trackmyfunds.dto.SummaryStatsDTO;
import com.trackmyfunds.enums.Category;
import com.trackmyfunds.enums.PaymentMethod;
import com.trackmyfunds.model.Expense;
import com.trackmyfunds.repository.ExpenseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    ExpenseRepository repo;

    @InjectMocks
    ExpenseServiceImpl service;

    // ── fixture helper ────────────────────────────────────────────────────────

    private Expense expense(Long id, String title, BigDecimal amount, Category cat) {
        return Expense.builder()
                .id(id).title(title).amount(amount).category(cat)
                .paymentMethod(PaymentMethod.CARD)
                .date(LocalDate.of(2024, 1, 15))
                .build();
    }

    // ── getFilteredExpenses ───────────────────────────────────────────────────

    @Test
    void getFilteredExpenses_delegatesToRepository_andReturnsPage() {
        Expense e = expense(1L, "Lunch", new BigDecimal("200"), Category.FOOD);
        when(repo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(e)));

        Page<Expense> page = service.getFilteredExpenses(
                ExpenseFilterDTO.empty(), 0, 10, "date", "desc");

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Lunch");
        verify(repo).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void getFilteredExpenses_ascSortDir_buildsAscPageRequest() {
        when(repo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

        service.getFilteredExpenses(ExpenseFilterDTO.empty(), 0, 10, "amount", "asc");

        verify(repo).findAll(
                any(Specification.class),
                argThat((PageRequest pr) ->
                        pr.getSort().getOrderFor("amount").getDirection() == Sort.Direction.ASC));
    }

    // ── createExpense ─────────────────────────────────────────────────────────

    @Test
    void createExpense_persistsCorrectFields_andReturnsEntity() {
        ExpenseRequestDTO dto = new ExpenseRequestDTO(
                "Groceries", new BigDecimal("350.00"), Category.FOOD,
                PaymentMethod.UPI, LocalDate.of(2024, 3, 1), "weekly shop");

        Expense saved = expense(1L, "Groceries", new BigDecimal("350.00"), Category.FOOD);
        when(repo.save(any())).thenReturn(saved);

        Expense result = service.createExpense(dto);

        assertThat(result.getId()).isEqualTo(1L);
        verify(repo).save(argThat(e ->
                "Groceries".equals(e.getTitle())
                && e.getAmount().compareTo(new BigDecimal("350.00")) == 0
                && e.getCategory()      == Category.FOOD
                && e.getPaymentMethod() == PaymentMethod.UPI));
    }

    // ── updateExpense ─────────────────────────────────────────────────────────

    @Test
    void updateExpense_unknownId_throwsEntityNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        ExpenseRequestDTO dto = new ExpenseRequestDTO(
                "X", BigDecimal.ONE, Category.OTHER, PaymentMethod.CASH, LocalDate.now(), null);

        assertThatThrownBy(() -> service.updateExpense(99L, dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateExpense_knownId_updatesAllMutableFields() {
        Expense existing = expense(1L, "Old Title", new BigDecimal("100"), Category.FOOD);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExpenseRequestDTO dto = new ExpenseRequestDTO(
                "New Title", new BigDecimal("999"), Category.HEALTH,
                PaymentMethod.NETBANKING, LocalDate.of(2024, 6, 1), "updated notes");

        Expense result = service.updateExpense(1L, dto);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("999"));
        assertThat(result.getCategory()).isEqualTo(Category.HEALTH);
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.NETBANKING);
        assertThat(result.getDescription()).isEqualTo("updated notes");
    }

    // ── deleteExpense ─────────────────────────────────────────────────────────

    @Test
    void deleteExpense_unknownId_throwsEntityNotFound_neverCallsDeleteById() {
        when(repo.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteExpense(42L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("42");
        verify(repo, never()).deleteById(any());
    }

    @Test
    void deleteExpense_knownId_invokesDeleteById() {
        when(repo.existsById(1L)).thenReturn(true);

        service.deleteExpense(1L);

        verify(repo).deleteById(1L);
    }

    // ── getExpenseById ────────────────────────────────────────────────────────

    @Test
    void getExpenseById_unknownId_throwsEntityNotFound() {
        when(repo.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExpenseById(7L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getExpenseById_knownId_returnsExpense() {
        Expense e = expense(5L, "Medicine", new BigDecimal("400"), Category.HEALTH);
        when(repo.findById(5L)).thenReturn(Optional.of(e));

        Expense result = service.getExpenseById(5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getCategory()).isEqualTo(Category.HEALTH);
    }

    // ── getSummaryStats ───────────────────────────────────────────────────────

    @Test
    void getSummaryStats_noExpenses_returnsAllZeros() {
        when(repo.findAll(any(Specification.class))).thenReturn(List.of());

        SummaryStatsDTO stats = service.getSummaryStats(ExpenseFilterDTO.empty());

        assertThat(stats.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.totalCount()).isZero();
        assertThat(stats.averageAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.highestExpense()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getSummaryStats_threeExpenses_computesTotalAverageAndHighest() {
        when(repo.findAll(any(Specification.class))).thenReturn(List.of(
                expense(1L, "A", new BigDecimal("100.00"), Category.FOOD),
                expense(2L, "B", new BigDecimal("200.00"), Category.TRANSPORT),
                expense(3L, "C", new BigDecimal("300.00"), Category.HEALTH)));

        SummaryStatsDTO stats = service.getSummaryStats(ExpenseFilterDTO.empty());

        assertThat(stats.totalCount()).isEqualTo(3);
        assertThat(stats.totalAmount()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(stats.averageAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(stats.highestExpense()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    // ── getDashboardData ──────────────────────────────────────────────────────

    @Test
    void getDashboardData_groupsByCategory_sumsAndSortsDescByTotal() {
        when(repo.findAll()).thenReturn(List.of(
                expense(1L, "A", new BigDecimal("500.00"), Category.FOOD),
                expense(2L, "B", new BigDecimal("300.00"), Category.FOOD),      // FOOD total = 800
                expense(3L, "C", new BigDecimal("700.00"), Category.TRANSPORT)  // TRANSPORT total = 700
        ));

        DashboardDTO data = service.getDashboardData();

        assertThat(data.spendingByCategory().get("FOOD"))
                .isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(data.spendingByCategory().get("TRANSPORT"))
                .isEqualByComparingTo(new BigDecimal("700.00"));

        List<String> orderedKeys = List.copyOf(data.spendingByCategory().keySet());
        assertThat(orderedKeys.get(0)).isEqualTo("FOOD");      // highest total first
        assertThat(orderedKeys.get(1)).isEqualTo("TRANSPORT");
    }

    @Test
    void getDashboardData_monthlySpending_allSixSlotsPresentWithZeroWhenNoData() {
        when(repo.findAll()).thenReturn(List.of());

        DashboardDTO data = service.getDashboardData();

        assertThat(data.spendingByMonth()).hasSize(6);
        data.spendingByMonth().values()
                .forEach(v -> assertThat(v).isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    void getDashboardData_top5_limitedToFiveHighestAmountsDescending() {
        // 6 expenses — only the top 5 by amount should appear; 100 is dropped
        when(repo.findAll()).thenReturn(List.of(
                expense(1L, "A", new BigDecimal("100"), Category.FOOD),
                expense(2L, "B", new BigDecimal("600"), Category.FOOD),
                expense(3L, "C", new BigDecimal("300"), Category.FOOD),
                expense(4L, "D", new BigDecimal("500"), Category.FOOD),
                expense(5L, "E", new BigDecimal("200"), Category.FOOD),
                expense(6L, "F", new BigDecimal("400"), Category.FOOD)
        ));

        DashboardDTO data = service.getDashboardData();

        assertThat(data.topExpenses()).hasSize(5);
        assertThat(data.topExpenses().get(0).getAmount())
                .isEqualByComparingTo(new BigDecimal("600"));  // highest first
        assertThat(data.topExpenses().get(4).getAmount())
                .isEqualByComparingTo(new BigDecimal("200"));  // 100 excluded
    }
}
