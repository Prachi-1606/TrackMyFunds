package com.trackmyfunds.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackmyfunds.dto.NLExpenseParseResult;
import com.trackmyfunds.exception.GeminiApiException;
import com.trackmyfunds.model.Expense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * Resume bullets (for interview prep — quick reference)
 * ───────────────────────────────────────────────────────────────────────────
 * • Integrated Google Gemini 1.5-flash via RestTemplate to power five AI
 *   features in a Spring Boot 3 app: natural-language expense parsing, voice
 *   input via the Web Speech API, grounded finance Q&A chat, monthly insight
 *   reports, and an empty-data short-circuit.
 *
 * • Designed strict prompt engineering: forbids outside knowledge, requires
 *   JSON-only responses for parsing, and asks the model to reply "cannot be
 *   determined" when data is missing — eliminating hallucinated answers in
 *   user testing.
 *
 * • Built an in-memory hourly rate limiter (AtomicInteger + windowed timestamp)
 *   capping calls at 12/hour to stay within Gemini's free-tier quota; returns
 *   a friendly "limit reached" message instead of hitting the API.
 *
 * • Wired GeminiApiException + a ControllerAdvice that renders a user-friendly
 *   error page when the API key is unconfigured; per-feature graceful
 *   fallbacks keep the UI functional during transient Gemini errors.
 *
 * • Mapped Gemini's JSON response straight to a Java record via Jackson
 *   (LocalDate, BigDecimal, Category and PaymentMethod enums) so parsed
 *   sentences flow straight into the existing ExpenseRequestDTO pipeline.
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String FALLBACK      = "AI service unavailable. Please try again.";
    private static final String RATE_LIMITED  = "Daily AI limit reached. Try again later.";

    private final RestTemplate     restTemplate;
    private final ObjectMapper     objectMapper;
    private final RateLimitService rateLimitService;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public GeminiService(RestTemplate restTemplate,
                         ObjectMapper objectMapper,
                         RateLimitService rateLimitService) {
        this.restTemplate     = restTemplate;
        this.objectMapper     = objectMapper;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Single entry point for every AI feature in the app.
     * <ul>
     *   <li>Throws {@link GeminiApiException} if the API key is unconfigured
     *       — the global handler renders a friendly error page.</li>
     *   <li>Returns the rate-limit message when the hourly budget is gone.</li>
     *   <li>Returns the generic fallback on any other network/parse failure
     *       so individual features can degrade in place.</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private String callGemini(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiApiException(
                    "Gemini API key is not configured. " +
                    "Set the GEMINI_API_KEY environment variable and restart.");
        }

        if (!rateLimitService.tryAcquire()) {
            return RATE_LIMITED;
        }

        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            String url = apiUrl + "?key=" + apiKey;

            Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);

            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content =
                    (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");

            return (String) parts.get(0).get("text");

        } catch (Exception e) {
            log.error("Gemini API call failed: {} — {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return FALLBACK;
        }
    }

    /**
     * Asks Gemini to translate a natural-language sentence (e.g. "Spent 350 on
     * dinner yesterday with UPI") into a strongly-typed {@link NLExpenseParseResult}.
     * Returns {@code null} when Gemini does not return parseable JSON.
     */
    public NLExpenseParseResult parseNaturalLanguageExpense(String userInput) {
        String today = LocalDate.now().toString();

        String prompt = "Parse the following expense description into structured JSON. "
                + "Return ONLY a valid JSON object with no extra text, no markdown, no backticks. "
                + "Today's date is " + today + ". "
                + "Fields to extract: title (string), amount (number), "
                + "category (one of: FOOD, TRANSPORT, ENTERTAINMENT, HEALTH, UTILITIES, SHOPPING, EDUCATION, OTHER), "
                + "paymentMethod (one of: CASH, CARD, UPI, NETBANKING), "
                + "date (format: yyyy-MM-dd, use today if not mentioned), "
                + "description (string, optional context). "
                + "Input: " + userInput + ". "
                + "Return exactly: {title, amount, category, paymentMethod, date, description}";

        String raw = callGemini(prompt);

        try {
            return objectMapper.readValue(raw, NLExpenseParseResult.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Produces the short narrative shown on the dashboard's Monthly Insight card.
     * Skips the API call entirely when there's no spend for the month — saving a
     * round-trip and giving the user a sensible empty-state message instead.
     */
    public String generateMonthlyInsight(Map<String, BigDecimal> categoryTotals,
                                         BigDecimal overallTotal,
                                         BigDecimal lastMonthTotal,
                                         String monthName) {
        if (overallTotal == null || overallTotal.signum() == 0) {
            return "No spending recorded for " + monthName + " yet. "
                 + "Once you log a few expenses, this card will show personalised insights.";
        }

        String comparison;
        if (lastMonthTotal == null || lastMonthTotal.signum() == 0) {
            comparison = "no spending data for last month";
        } else {
            double pct = overallTotal.subtract(lastMonthTotal)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(lastMonthTotal, 1, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
            String direction = pct >= 0 ? "increase" : "decrease";
            comparison = direction + " by " + String.format("%.1f", Math.abs(pct)) + "%";
        }

        String breakdown = categoryTotals.entrySet().stream()
                .map(e -> e.getKey() + ": ₹" + e.getValue())
                .collect(Collectors.joining(", "));

        String prompt = "You are a personal finance advisor. "
                + "Generate a concise 4-5 line spending insight report for " + monthName + ". "
                + "Be specific, friendly, and actionable. "
                + "Data: Total spent: ₹" + overallTotal + ", "
                + "Category breakdown: " + breakdown + ", "
                + "Compared to last month: " + comparison + ". "
                + "Include: top spending category, one positive observation, "
                + "one area to improve, one specific actionable tip.";

        return callGemini(prompt);
    }

    /**
     * Answers a free-form finance question grounded ONLY in the user's own
     * expense history. The prompt explicitly forbids outside knowledge, so the
     * model will say "I can't tell from your data" rather than guess.
     */
    public String answerFinanceQuestion(String userQuestion, List<Expense> expenses) {
        String prompt = "You are a personal finance assistant. "
                + "Answer the user's question based ONLY on the following expense data. "
                + "Do not use outside knowledge. "
                + "If the answer cannot be determined from the data, say so clearly.\n\n"
                + "Expense Data:\n" + buildSummary(expenses) + "\n\n"
                + "Today's date: " + LocalDate.now() + ".\n"
                + "User question: " + userQuestion;

        return callGemini(prompt);
    }

    private static String buildSummary(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return "No expenses on file.";
        }

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long       count   = expenses.size();
        BigDecimal average = total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);

        Expense highest = expenses.stream()
                .max(Comparator.comparing(Expense::getAmount))
                .orElseThrow();

        LocalDate earliest = expenses.stream().map(Expense::getDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate latest   = expenses.stream().map(Expense::getDate).max(LocalDate::compareTo).orElseThrow();

        String mostUsedPayment = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getPaymentMethod, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().name())
                .orElse("UNKNOWN");

        Map<String, BigDecimal> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().name(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        StringBuilder sb = new StringBuilder();
        sb.append("Date range:     ").append(earliest).append(" to ").append(latest).append('\n');
        sb.append("Total records:  ").append(count).append('\n');
        sb.append("Overall total:  ₹").append(total).append('\n');
        sb.append("Average:        ₹").append(average).append('\n');
        sb.append("Highest expense: ₹").append(highest.getAmount())
          .append(" (").append(highest.getTitle())
          .append(", ").append(highest.getCategory())
          .append(", ").append(highest.getDate()).append(")\n");
        sb.append("Most-used payment method: ").append(mostUsedPayment).append('\n');
        sb.append("Category totals:\n");
        categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .forEach(e -> sb.append("  - ").append(e.getKey())
                                .append(": ₹").append(e.getValue()).append('\n'));

        return sb.toString();
    }
}
