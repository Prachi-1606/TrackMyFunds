package com.trackmyfunds.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregated data needed for the AI monthly-insight card.
 * The {@code changePercentage} is signed: positive = spent more than last month,
 * negative = spent less.
 *
 * @param categoryTotals   category name → total spend for the target month (sorted desc)
 * @param overallTotal     grand total for the target month
 * @param lastMonthTotal   grand total for the previous month (used to compute the change)
 * @param changePercentage (overallTotal − lastMonthTotal) / lastMonthTotal × 100, rounded to 1dp; 0.0 if lastMonthTotal is zero
 * @param monthName        e.g. "March 2026" — formatted for direct use in the prompt
 */
public record MonthlyInsightDTO(
        Map<String, BigDecimal> categoryTotals,
        BigDecimal              overallTotal,
        BigDecimal              lastMonthTotal,
        double                  changePercentage,
        String                  monthName
) {}
