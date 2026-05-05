package com.trackmyfunds.specification;

import com.trackmyfunds.dto.ExpenseFilterDTO;
import com.trackmyfunds.enums.Category;
import com.trackmyfunds.enums.PaymentMethod;
import com.trackmyfunds.model.Expense;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ExpenseSpecification {

    private ExpenseSpecification() {}

    public static Specification<Expense> byCategory(Category category) {
        return (root, query, cb) ->
                cb.equal(root.get("category"), category);
    }

    public static Specification<Expense> byPaymentMethod(PaymentMethod method) {
        return (root, query, cb) ->
                cb.equal(root.get("paymentMethod"), method);
    }

    /**
     * Handles open-ended ranges: either bound may be null.
     * Both null → returns a conjunction (no-op predicate).
     */
    public static Specification<Expense> byDateRange(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("date"), from, to);
            } else if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("date"), from);
            } else if (to != null) {
                return cb.lessThanOrEqualTo(root.get("date"), to);
            }
            return cb.conjunction();
        };
    }

    /**
     * Handles open-ended ranges: either bound may be null.
     * Both null → returns a conjunction (no-op predicate).
     */
    public static Specification<Expense> byAmountRange(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("amount"), min, max);
            } else if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("amount"), min);
            } else if (max != null) {
                return cb.lessThanOrEqualTo(root.get("amount"), max);
            }
            return cb.conjunction();
        };
    }

    /** Case-insensitive LIKE search across title and description. */
    public static Specification<Expense> byKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")),       pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    /**
     * Combines every non-null field in the filter into a single Specification.
     * Specification.where(null) is a match-all; .and(null) is a no-op —
     * so filters with no active fields return all rows.
     */
    public static Specification<Expense> buildSpecification(ExpenseFilterDTO filter) {
        return Specification
                .where(filter.category() != null
                        ? byCategory(filter.category()) : null)
                .and(filter.paymentMethod() != null
                        ? byPaymentMethod(filter.paymentMethod()) : null)
                .and(filter.dateFrom() != null || filter.dateTo() != null
                        ? byDateRange(filter.dateFrom(), filter.dateTo()) : null)
                .and(filter.amountMin() != null || filter.amountMax() != null
                        ? byAmountRange(filter.amountMin(), filter.amountMax()) : null)
                .and(filter.keyword() != null && !filter.keyword().isBlank()
                        ? byKeyword(filter.keyword()) : null);
    }
}
