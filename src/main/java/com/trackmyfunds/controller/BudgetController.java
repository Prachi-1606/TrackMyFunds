package com.trackmyfunds.controller;

import com.trackmyfunds.dto.BudgetStatusDTO;
import com.trackmyfunds.enums.Category;
import com.trackmyfunds.service.BudgetService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /** Budget management page — current month's statuses + form to set a new limit. */
    @GetMapping
    public String budgetDashboard(Model model) {
        List<BudgetStatusDTO> statuses = budgetService.getCurrentMonthBudgets();

        Set<Category> withBudgets = statuses.stream()
                .map(BudgetStatusDTO::category)
                .collect(Collectors.toSet());

        List<Category> withoutBudgets = Arrays.stream(Category.values())
                .filter(c -> !withBudgets.contains(c))
                .toList();

        model.addAttribute("budgetStatuses",         statuses);
        model.addAttribute("categoriesWithoutBudget", withoutBudgets);
        model.addAttribute("categories",             Category.values());
        return "budget/dashboard";
    }

    /** Save (or update) the limit for one category for the current month/year. */
    @PostMapping("/set")
    public String setBudget(
            @NotNull   @RequestParam Category   category,
            @Positive  @RequestParam BigDecimal limit,
            RedirectAttributes redirectAttrs) {

        budgetService.setBudget(category, limit);
        redirectAttrs.addFlashAttribute("successMessage",
                "Budget for " + category + " saved.");
        return "redirect:/budget";
    }

    /** JSON snapshot of every budget in the current month — used by AJAX / external clients. */
    @GetMapping("/status")
    @ResponseBody
    public List<BudgetStatusDTO> status() {
        return budgetService.getCurrentMonthBudgets();
    }
}
