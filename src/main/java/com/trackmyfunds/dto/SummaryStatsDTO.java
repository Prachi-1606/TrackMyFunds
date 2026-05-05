package com.trackmyfunds.dto;

import java.math.BigDecimal;

public record SummaryStatsDTO(
        BigDecimal totalAmount,
        long       totalCount,
        BigDecimal averageAmount,
        BigDecimal highestExpense
) {}
