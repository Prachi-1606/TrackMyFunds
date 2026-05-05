package com.trackmyfunds.dto;

import com.trackmyfunds.model.Expense;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardDTO(
        Map<String, BigDecimal> spendingByCategory,
        Map<String, BigDecimal> spendingByMonth,
        List<Expense>           topExpenses
) {}
