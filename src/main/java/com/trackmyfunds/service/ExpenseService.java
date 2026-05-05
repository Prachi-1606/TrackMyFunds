package com.trackmyfunds.service;

import com.trackmyfunds.dto.DashboardDTO;
import com.trackmyfunds.dto.ExpenseFilterDTO;
import com.trackmyfunds.dto.ExpenseRequestDTO;
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
}
