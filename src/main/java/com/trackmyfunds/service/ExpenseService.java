package com.trackmyfunds.service;

import com.trackmyfunds.dto.AnomalyResult;
import com.trackmyfunds.dto.DashboardDTO;
import com.trackmyfunds.dto.ExpenseFilterDTO;
import com.trackmyfunds.dto.ExpenseRequestDTO;
import com.trackmyfunds.dto.FinanceSummaryDTO;
import com.trackmyfunds.dto.MonthlyInsightDTO;
import com.trackmyfunds.dto.SummaryStatsDTO;
import com.trackmyfunds.model.Expense;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ExpenseService {

    Page<Expense> getFilteredExpenses(ExpenseFilterDTO filter, int page, int size,
                                      String sortBy, String sortDir);

    /** Returns all matching rows (no pagination) sorted by date desc — used for CSV export. */
    List<Expense> getAllFiltered(ExpenseFilterDTO filter);

    Expense createExpense(ExpenseRequestDTO dto);

    Expense updateExpense(Long id, ExpenseRequestDTO dto);

    void deleteExpense(Long id);

    Expense getExpenseById(Long id);

    SummaryStatsDTO getSummaryStats(ExpenseFilterDTO filter);

    DashboardDTO getDashboardData();

    /**
     * Rule-based check: compare the new expense against the 3-month average for
     * its category. Returns {@link AnomalyResult#none()} when there's no prior
     * data or the amount is within 2x the average.
     */
    AnomalyResult checkAnomaly(Expense newExpense);

    /**
     * Aggregated snapshot of all expenses — the single object the AI chat (and
     * any future analytics widget) needs to answer questions without re-querying.
     */
    FinanceSummaryDTO getExpenseSummaryForAI();

    /**
     * Data needed to render the AI monthly-insight card: category totals + grand
     * total for the target month, plus the prior month's total and the signed
     * % change between them.
     */
    MonthlyInsightDTO getMonthlyInsightData(int month, int year);
}
