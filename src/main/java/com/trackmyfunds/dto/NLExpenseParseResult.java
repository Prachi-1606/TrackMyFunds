package com.trackmyfunds.dto;

import com.trackmyfunds.enums.Category;
import com.trackmyfunds.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Structured representation of a natural-language expense description after
 * Gemini has parsed it. All fields map directly to {@code ExpenseRequestDTO}
 * so callers can pre-fill the new-expense form (or short-circuit straight to
 * createExpense) without further conversion.
 *
 * <p>Returned as {@code null} from {@link com.trackmyfunds.service.GeminiService}
 * when Gemini fails to return parseable JSON.
 */
public record NLExpenseParseResult(
        String        title,
        BigDecimal    amount,
        Category      category,
        PaymentMethod paymentMethod,
        LocalDate     date,
        String        description
) {}
