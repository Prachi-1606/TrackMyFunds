package com.trackmyfunds.dto;

import com.trackmyfunds.enums.Category;
import com.trackmyfunds.enums.PaymentMethod;
import com.trackmyfunds.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponseDTO(
        Long          id,
        String        title,
        BigDecimal    amount,
        Category      category,
        PaymentMethod paymentMethod,
        LocalDate     date,
        String        description,
        LocalDateTime createdAt
) {
    public static ExpenseResponseDTO from(Expense e) {
        return new ExpenseResponseDTO(
                e.getId(),
                e.getTitle(),
                e.getAmount(),
                e.getCategory(),
                e.getPaymentMethod(),
                e.getDate(),
                e.getDescription(),
                e.getCreatedAt()
        );
    }
}
