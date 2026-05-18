package com.trackmyfunds.controller;

import com.trackmyfunds.dto.AnomalyResult;
import com.trackmyfunds.dto.DashboardDTO;
import com.trackmyfunds.dto.ExpenseFilterDTO;
import com.trackmyfunds.dto.ExpenseRequestDTO;
import com.trackmyfunds.dto.NLExpenseParseResult;
import com.trackmyfunds.enums.Category;
import com.trackmyfunds.enums.PaymentMethod;
import com.trackmyfunds.model.Expense;
import com.trackmyfunds.service.BudgetService;
import com.trackmyfunds.service.CsvExportService;
import com.trackmyfunds.service.ExpenseService;
import com.trackmyfunds.service.GeminiService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService   expenseService;
    private final CsvExportService csvExportService;
    private final BudgetService    budgetService;
    private final GeminiService    geminiService;

    @GetMapping("/")
    public String home() {
        return "redirect:/expenses";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardDTO data = expenseService.getDashboardData();
        model.addAttribute("categoryLabels", new ArrayList<>(data.spendingByCategory().keySet()));
        model.addAttribute("categoryValues", new ArrayList<>(data.spendingByCategory().values()));
        model.addAttribute("monthLabels",    new ArrayList<>(data.spendingByMonth().keySet()));
        model.addAttribute("monthValues",    new ArrayList<>(data.spendingByMonth().values()));
        model.addAttribute("topExpenses",    data.topExpenses());
        return "dashboard";
    }

    @GetMapping("/expenses")
    public String listExpenses(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {

        ExpenseFilterDTO filter = new ExpenseFilterDTO(
                category, paymentMethod, dateFrom, dateTo, amountMin, amountMax, keyword);

        Page<Expense> expensePage = expenseService.getFilteredExpenses(filter, page, size, sortBy, sortDir);

        model.addAttribute("expenses",       expensePage);
        model.addAttribute("stats",          expenseService.getSummaryStats(filter));
        model.addAttribute("totalPages",     expensePage.getTotalPages());
        model.addAttribute("currentPage",    page);
        model.addAttribute("size",           size);
        model.addAttribute("sortBy",         sortBy);
        model.addAttribute("sortDir",        sortDir);
        model.addAttribute("reverseSortDir", "asc".equals(sortDir) ? "desc" : "asc");

        // repopulate filter fields in the UI
        model.addAttribute("filterCategory",      category);
        model.addAttribute("filterPaymentMethod", paymentMethod);
        model.addAttribute("filterDateFrom",      dateFrom);
        model.addAttribute("filterDateTo",        dateTo);
        model.addAttribute("filterAmountMin",     amountMin);
        model.addAttribute("filterAmountMax",     amountMax);
        model.addAttribute("filterKeyword",       keyword);

        model.addAttribute("categories",     Category.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());

        // Compact warning strip — only over-budget categories for the current month
        model.addAttribute("overBudgetStatuses",
                budgetService.getCurrentMonthBudgets().stream()
                        .filter(s -> s.isOverBudget())
                        .toList());

        return "expenses/list";
    }

    @GetMapping("/expenses/export")
    public void exportCsv(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) String keyword,
            HttpServletResponse response) throws IOException {

        ExpenseFilterDTO filter = new ExpenseFilterDTO(
                category, paymentMethod, dateFrom, dateTo, amountMin, amountMax, keyword);

        List<Expense> expenses = expenseService.getAllFiltered(filter);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"expenses.csv\"");

        String csv = csvExportService.exportToCSV(expenses);
        response.getWriter().write(csv);
        response.getWriter().flush();
    }

    /**
     * AJAX endpoint used by the "Parse with AI" button on the new-expense form.
     * Returns 200 + the structured fields on success, or 422 + an error envelope
     * when Gemini doesn't return parseable JSON.
     */
    @PostMapping("/expenses/parse-nl")
    @ResponseBody
    public ResponseEntity<Object> parseNaturalLanguage(@RequestParam("input") String input) {
        NLExpenseParseResult result = geminiService.parseNaturalLanguageExpense(input);
        if (result == null) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("error", "Could not parse. Please fill manually."));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/expenses/new")
    public String newExpenseForm(Model model) {
        model.addAttribute("expense",       new ExpenseRequestDTO(null, null, null, null, null, null));
        model.addAttribute("categories",    Category.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("isEdit",        false);
        return "expenses/form";
    }

    @PostMapping("/expenses")
    public String createExpense(
            @Valid @ModelAttribute("expense") ExpenseRequestDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttrs) {

        if (result.hasErrors()) {
            model.addAttribute("categories",    Category.values());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("isEdit",        false);
            return "expenses/form";
        }

        Expense saved = expenseService.createExpense(dto);
        redirectAttrs.addFlashAttribute("successMessage", "Expense created successfully.");

        AnomalyResult anomaly = expenseService.checkAnomaly(saved);
        if (anomaly.isAnomaly()) {
            redirectAttrs.addFlashAttribute("anomaly", anomaly);
        }
        return "redirect:/expenses";
    }

    @GetMapping("/expenses/{id}/edit")
    public String editExpenseForm(@PathVariable Long id, Model model) {
        Expense e = expenseService.getExpenseById(id);
        model.addAttribute("expense", new ExpenseRequestDTO(
                e.getTitle(), e.getAmount(), e.getCategory(),
                e.getPaymentMethod(), e.getDate(), e.getDescription()));
        model.addAttribute("expenseId",     id);
        model.addAttribute("categories",    Category.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("isEdit",        true);
        return "expenses/form";
    }

    @PostMapping("/expenses/{id}")
    public String updateExpense(
            @PathVariable Long id,
            @Valid @ModelAttribute("expense") ExpenseRequestDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttrs) {

        if (result.hasErrors()) {
            model.addAttribute("expenseId",     id);
            model.addAttribute("categories",    Category.values());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("isEdit",        true);
            return "expenses/form";
        }

        expenseService.updateExpense(id, dto);
        redirectAttrs.addFlashAttribute("successMessage", "Expense updated successfully.");
        return "redirect:/expenses";
    }

    @PostMapping("/expenses/{id}/delete")
    public String deleteExpense(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        expenseService.deleteExpense(id);
        redirectAttrs.addFlashAttribute("successMessage", "Expense deleted.");
        return "redirect:/expenses";
    }
}
