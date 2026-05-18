package com.trackmyfunds.dto;

import com.trackmyfunds.enums.Category;

import java.math.BigDecimal;

/**
 * Snapshot of a single category's budget vs. actual spend for the current month.
 *
 * @param category        the spending category
 * @param monthlyLimit    the budget set for this category this month
 * @param amountSpent     total spent so far this month in this category
 * @param amountRemaining monthlyLimit − amountSpent (can be negative)
 * @param percentageUsed  amountSpent / monthlyLimit × 100, rounded to two decimals
 * @param isOverBudget    true when amountSpent > monthlyLimit
 */
public record BudgetStatusDTO(
        Category   category,
        BigDecimal monthlyLimit,
        BigDecimal amountSpent,
        BigDecimal amountRemaining,
        double     percentageUsed,
        boolean    isOverBudget
) {}
