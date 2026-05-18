package com.trackmyfunds.dto;

import com.trackmyfunds.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Aggregated snapshot of the user's expense history — the single object the
 * AI finance chat (and any future analytics UI) needs to answer questions
 * without hitting the database itself.
 *
 * @param categoryTotals        category name → total spend (sorted desc)
 * @param monthlyTotals         month label ("Mar 2026") → total spend, last 6 months, oldest→newest
 * @param topExpenses           up to 5 highest single expenses, highest first
 * @param totalAmount           grand total across all expenses
 * @param totalCount            number of records contributing to the totals
 * @param averageAmount         totalAmount ÷ totalCount, rounded to 2dp
 * @param highestAmount         amount of the single largest expense
 * @param mostUsedPaymentMethod modal payment method (e.g. "UPI"), or null if no data
 * @param earliestDate          date of the oldest expense, or null if no data
 * @param latestDate            date of the newest expense, or null if no data
 */
public record FinanceSummaryDTO(
        Map<String, BigDecimal> categoryTotals,
        Map<String, BigDecimal> monthlyTotals,
        List<Expense>           topExpenses,
        BigDecimal              totalAmount,
        long                    totalCount,
        BigDecimal              averageAmount,
        BigDecimal              highestAmount,
        String                  mostUsedPaymentMethod,
        LocalDate               earliestDate,
        LocalDate               latestDate
) {}
