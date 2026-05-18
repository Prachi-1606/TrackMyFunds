package com.trackmyfunds.dto;

import java.math.BigDecimal;

/**
 * Result of a rule-based anomaly check on a newly created expense.
 *
 * @param isAnomaly       true when the new amount exceeds the configured threshold
 *                        (2x the 3-month category average)
 * @param message         user-facing copy shown in the warning banner; meaningful only when isAnomaly is true
 * @param categoryAverage the average for that category over the last 3 months
 * @param multiplier      newAmount / categoryAverage — exposed so the UI can render "Nx your average"
 */
public record AnomalyResult(
        boolean    isAnomaly,
        String     message,
        BigDecimal categoryAverage,
        double     multiplier
) {
    public static AnomalyResult none() {
        return new AnomalyResult(false, null, BigDecimal.ZERO, 0.0);
    }
}
