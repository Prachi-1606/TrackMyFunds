package com.trackmyfunds.controller;

import com.trackmyfunds.dto.FinanceSummaryDTO;
import com.trackmyfunds.dto.MonthlyInsightDTO;
import com.trackmyfunds.model.Expense;
import com.trackmyfunds.repository.ExpenseRepository;
import com.trackmyfunds.service.ExpenseService;
import com.trackmyfunds.service.GeminiService;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private static final List<String> SUGGESTED_QUESTIONS = List.of(
            "How much did I spend this month?",
            "Which category costs me the most?",
            "What was my biggest expense?",
            "How does this month compare to last month?"
    );

    private final ExpenseService    expenseService;
    private final ExpenseRepository expenseRepository;
    private final GeminiService     geminiService;

    /** Landing page — index of every AI tool available in the app. */
    @GetMapping
    public String index() {
        return "ai/index";
    }

    @GetMapping("/chat")
    public String chatPage(Model model) {
        FinanceSummaryDTO context = expenseService.getExpenseSummaryForAI();
        model.addAttribute("suggestedQuestions", SUGGESTED_QUESTIONS);
        model.addAttribute("context",            context);
        return "ai/chat";
    }

    @PostMapping("/chat")
    public String ask(@RequestParam(value = "question", required = false) String question,
                      Model model) {
        // Be lenient: an empty or missing question just sends the user back to
        // the chat page instead of producing a 400 stack trace.
        if (question == null || question.isBlank()) {
            return "redirect:/ai/chat";
        }

        FinanceSummaryDTO context  = expenseService.getExpenseSummaryForAI();
        List<Expense>     expenses = expenseRepository.findAll();
        String            answer   = geminiService.answerFinanceQuestion(question, expenses);

        model.addAttribute("suggestedQuestions", SUGGESTED_QUESTIONS);
        model.addAttribute("context",            context);
        model.addAttribute("question",           question);
        model.addAttribute("answer",             answer);
        return "ai/chat";
    }

    /**
     * Plain-text insight for the dashboard card — fetched via JS on page load.
     * Returns the AI narrative verbatim so the front-end can drop it into a
     * styled card without further parsing.
     */
    @GetMapping(value = "/insight/{month}/{year}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String monthlyInsight(@PathVariable int month, @PathVariable int year) {
        MonthlyInsightDTO data = expenseService.getMonthlyInsightData(month, year);
        return geminiService.generateMonthlyInsight(
                data.categoryTotals(),
                data.overallTotal(),
                data.lastMonthTotal(),
                data.monthName());
    }
}
