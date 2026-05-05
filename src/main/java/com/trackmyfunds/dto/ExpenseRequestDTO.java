package com.trackmyfunds.dto;

import com.trackmyfunds.enums.Category;
import com.trackmyfunds.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequestDTO(

        @NotBlank
        String title,

        @NotNull @Positive
        BigDecimal amount,

        @NotNull
        Category category,

        @NotNull
        PaymentMethod paymentMethod,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date,

        String description
) {}
