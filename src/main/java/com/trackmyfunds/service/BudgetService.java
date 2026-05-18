package com.trackmyfunds.service;

import com.trackmyfunds.dto.BudgetStatusDTO;
import com.trackmyfunds.enums.Category;
import com.trackmyfunds.model.Budget;
import com.trackmyfunds.repository.BudgetRepository;
import com.trackmyfunds.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository  budgetRepository;
    private final ExpenseRepository expenseRepository;

    /**
     * Creates the budget for the current month/year if absent, otherwise updates
     * the existing row's limit. The (category, month, year) uniqueness is
     * enforced by a DB constraint as well.
     */
    @Transactional
    public Budget setBudget(Category category, BigDecimal limit) {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year  = today.getYear();

        Budget budget = budgetRepository
                .findByCategoryAndMonthAndYear(category, month, year)
                .orElseGet(() -> Budget.builder()
                        .category(category)
                        .month(month)
                        .year(year)
                        .build());

        budget.setMonthlyLimit(limit);
        return budgetRepository.save(budget);
    }

    /**
     * Joins each budget for the current month with the running spend total for
     * its category to produce a list of {@link BudgetStatusDTO}.
     */
    @Transactional(readOnly = true)
    public List<BudgetStatusDTO> getCurrentMonthBudgets() {
        LocalDate today = LocalDate.now();
        int       month = today.getMonthValue();
        int       year  = today.getYear();

        YearMonth ym         = YearMonth.of(year, month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd   = ym.atEndOfMonth();

        return budgetRepository.findByMonthAndYear(month, year).stream()
                .map(budget -> toStatus(budget, monthStart, monthEnd))
                .toList();
    }

    private BudgetStatusDTO toStatus(Budget budget, LocalDate start, LocalDate end) {
        BigDecimal spent = expenseRepository.sumByCategoryAndDateBetween(
                budget.getCategory(), start, end);

        BigDecimal limit     = budget.getMonthlyLimit();
        BigDecimal remaining = limit.subtract(spent);

        double percentage = limit.signum() == 0
                ? 0.0
                : spent.multiply(BigDecimal.valueOf(100))
                       .divide(limit, 2, RoundingMode.HALF_UP)
                       .doubleValue();

        boolean over = spent.compareTo(limit) > 0;

        return new BudgetStatusDTO(
                budget.getCategory(), limit, spent, remaining, percentage, over);
    }
}
