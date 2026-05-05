package com.trackmyfunds.dto;

import com.trackmyfunds.enums.Category;
import com.trackmyfunds.enums.PaymentMethod;
import com.trackmyfunds.model.Expense;
import com.trackmyfunds.specification.ExpenseSpecification;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * All fields are nullable — only the non-null ones are wired into the query.
 * Use the static {@link #empty()} factory when no filters are needed (returns all rows).
 */
public record ExpenseFilterDTO(
        Category   category,
        PaymentMethod paymentMethod,
        LocalDate  dateFrom,
        LocalDate  dateTo,
        BigDecimal amountMin,
        BigDecimal amountMax,
        String     keyword
) {

    /** Convenience: builds and returns the combined Specification for this filter. */
    public Specification<Expense> toSpecification() {
        return ExpenseSpecification.buildSpecification(this);
    }

    /** Returns a filter with no constraints — matches every row. */
    public static ExpenseFilterDTO empty() {
        return new ExpenseFilterDTO(null, null, null, null, null, null, null);
    }
}
