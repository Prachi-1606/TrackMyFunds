package com.trackmyfunds.controller;

import com.trackmyfunds.dto.ExpenseFilterDTO;
import com.trackmyfunds.dto.ExpenseRequestDTO;
import com.trackmyfunds.dto.ExpenseResponseDTO;
import com.trackmyfunds.dto.SummaryStatsDTO;
import com.trackmyfunds.enums.Category;
import com.trackmyfunds.enums.PaymentMethod;
import com.trackmyfunds.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseRestController {

    private final ExpenseService expenseService;

    /* ── GET /api/expenses ─────────────────────────────────────────────────── */
    @GetMapping
    public Page<ExpenseResponseDTO> getExpenses(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "20")   int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        ExpenseFilterDTO filter = new ExpenseFilterDTO(
                category, paymentMethod, dateFrom, dateTo, amountMin, amountMax, keyword);
        return expenseService
                .getFilteredExpenses(filter, page, size, sortBy, sortDir)
                .map(ExpenseResponseDTO::from);
    }

    /* ── GET /api/expenses/summary ─────────────────────────────────────────── */
    // Defined before /{id} so Spring's literal-path preference matches it first.
    @GetMapping("/summary")
    public SummaryStatsDTO getSummary(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) String keyword) {

        ExpenseFilterDTO filter = new ExpenseFilterDTO(
                category, paymentMethod, dateFrom, dateTo, amountMin, amountMax, keyword);
        return expenseService.getSummaryStats(filter);
    }

    /* ── GET /api/expenses/{id} ────────────────────────────────────────────── */
    @GetMapping("/{id}")
    public ExpenseResponseDTO getById(@PathVariable Long id) {
        return ExpenseResponseDTO.from(expenseService.getExpenseById(id));
    }

    /* ── POST /api/expenses ────────────────────────────────────────────────── */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponseDTO create(@Valid @RequestBody ExpenseRequestDTO dto) {
        return ExpenseResponseDTO.from(expenseService.createExpense(dto));
    }

    /* ── PUT /api/expenses/{id} ────────────────────────────────────────────── */
    @PutMapping("/{id}")
    public ExpenseResponseDTO update(@PathVariable Long id,
                                     @Valid @RequestBody ExpenseRequestDTO dto) {
        return ExpenseResponseDTO.from(expenseService.updateExpense(id, dto));
    }

    /* ── DELETE /api/expenses/{id} ─────────────────────────────────────────── */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        expenseService.deleteExpense(id);
    }
}
