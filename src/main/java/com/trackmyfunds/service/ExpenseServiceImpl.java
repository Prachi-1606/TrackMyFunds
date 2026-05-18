package com.trackmyfunds.service;

import com.trackmyfunds.dto.AnomalyResult;
import com.trackmyfunds.dto.DashboardDTO;
import com.trackmyfunds.dto.ExpenseFilterDTO;
import com.trackmyfunds.dto.ExpenseRequestDTO;
import com.trackmyfunds.dto.FinanceSummaryDTO;
import com.trackmyfunds.dto.MonthlyInsightDTO;
import com.trackmyfunds.dto.SummaryStatsDTO;
import com.trackmyfunds.model.Expense;
import com.trackmyfunds.repository.ExpenseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Expense> getFilteredExpenses(ExpenseFilterDTO filter, int page, int size,
                                             String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Specification<Expense> spec = filter.toSpecification();
        return expenseRepository.findAll(spec, pageRequest);
    }

    @Override
    @Transactional
    public Expense createExpense(ExpenseRequestDTO dto) {
        Expense expense = Expense.builder()
                .title(dto.title())
                .amount(dto.amount())
                .category(dto.category())
                .paymentMethod(dto.paymentMethod())
                .date(dto.date())
                .description(dto.description())
                .build();
        return expenseRepository.save(expense);
    }

    @Override
    @Transactional
    public Expense updateExpense(Long id, ExpenseRequestDTO dto) {
        Expense expense = findOrThrow(id);
        expense.setTitle(dto.title());
        expense.setAmount(dto.amount());
        expense.setCategory(dto.category());
        expense.setPaymentMethod(dto.paymentMethod());
        expense.setDate(dto.date());
        expense.setDescription(dto.description());
        return expenseRepository.save(expense);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new EntityNotFoundException("Expense not found: " + id);
        }
        expenseRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expense> getAllFiltered(ExpenseFilterDTO filter) {
        return expenseRepository.findAll(filter.toSpecification(), Sort.by("date").descending());
    }

    @Override
    @Transactional(readOnly = true)
    public Expense getExpenseById(Long id) {
        return findOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SummaryStatsDTO getSummaryStats(ExpenseFilterDTO filter) {
        Specification<Expense> spec = filter.toSpecification();
        List<Expense> expenses = expenseRepository.findAll(spec);

        long count = expenses.size();
        if (count == 0) {
            return new SummaryStatsDTO(BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        BigDecimal highest = expenses.stream()
                .map(Expense::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return new SummaryStatsDTO(total, count, average, highest);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardDTO getDashboardData() {
        List<Expense> all = expenseRepository.findAll();

        // Spending by category — sorted largest first
        Map<String, BigDecimal> byCategory = all.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().name(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        // Spending by month — last 6 calendar months, oldest → newest, all slots pre-filled with 0
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy");
        LocalDate today = LocalDate.now();
        Map<String, BigDecimal> byMonth = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            byMonth.put(YearMonth.from(today).minusMonths(i).format(monthFmt), BigDecimal.ZERO);
        }
        LocalDate cutoff = today.minusMonths(6).withDayOfMonth(1);
        all.stream()
                .filter(e -> !e.getDate().isBefore(cutoff))
                .forEach(e -> {
                    String key = YearMonth.from(e.getDate()).format(monthFmt);
                    byMonth.computeIfPresent(key, (k, v) -> v.add(e.getAmount()));
                });

        // Top 5 most expensive transactions overall
        List<Expense> top5 = all.stream()
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .limit(5)
                .toList();

        return new DashboardDTO(byCategory, byMonth, top5);
    }

    @Override
    @Transactional(readOnly = true)
    public AnomalyResult checkAnomaly(Expense newExpense) {
        LocalDate cutoff = LocalDate.now().minusMonths(3);

        List<Expense> recent = expenseRepository
                .findByCategoryAndDateAfter(newExpense.getCategory(), cutoff)
                .stream()
                .filter(e -> !e.getId().equals(newExpense.getId())) // exclude the row just saved
                .toList();

        if (recent.isEmpty()) {
            return AnomalyResult.none();
        }

        BigDecimal total = recent.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = total.divide(BigDecimal.valueOf(recent.size()), 2, RoundingMode.HALF_UP);

        if (average.signum() == 0) {
            return AnomalyResult.none();
        }

        double multiplier = newExpense.getAmount()
                .divide(average, 2, RoundingMode.HALF_UP)
                .doubleValue();

        if (multiplier <= 2.0) {
            return new AnomalyResult(false, null, average, multiplier);
        }

        String message = String.format(
                "This ₹%s %s expense is %.0fx your monthly average of ₹%s. Was this intentional?",
                formatMoney(newExpense.getAmount()),
                friendlyCategory(newExpense.getCategory().name()),
                multiplier,
                formatMoney(average));

        return new AnomalyResult(true, message, average, multiplier);
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceSummaryDTO getExpenseSummaryForAI() {
        List<Expense> all = expenseRepository.findAll();

        if (all.isEmpty()) {
            return new FinanceSummaryDTO(
                    Map.of(), Map.of(), List.of(),
                    BigDecimal.ZERO, 0L, BigDecimal.ZERO, BigDecimal.ZERO,
                    null, null, null);
        }

        // Category totals — sorted descending so the largest spend leads the prompt
        Map<String, BigDecimal> categoryTotals = all.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().name(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        // Monthly totals — last 6 calendar months, oldest → newest, pre-seeded with zero
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy");
        LocalDate today = LocalDate.now();
        Map<String, BigDecimal> monthlyTotals = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            monthlyTotals.put(YearMonth.from(today).minusMonths(i).format(monthFmt), BigDecimal.ZERO);
        }
        LocalDate monthlyCutoff = today.minusMonths(6).withDayOfMonth(1);
        all.stream()
                .filter(e -> !e.getDate().isBefore(monthlyCutoff))
                .forEach(e -> {
                    String key = YearMonth.from(e.getDate()).format(monthFmt);
                    monthlyTotals.computeIfPresent(key, (k, v) -> v.add(e.getAmount()));
                });

        // Top 5 highest single expenses
        List<Expense> topExpenses = all.stream()
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .limit(5)
                .toList();

        // Overall stats
        BigDecimal totalAmount = all.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long       totalCount    = all.size();
        BigDecimal averageAmount = totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
        BigDecimal highestAmount = topExpenses.get(0).getAmount();

        // Modal payment method
        String mostUsedPaymentMethod = all.stream()
                .collect(Collectors.groupingBy(Expense::getPaymentMethod, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().name())
                .orElse(null);

        // Date range
        LocalDate earliest = all.stream().map(Expense::getDate).min(LocalDate::compareTo).orElse(null);
        LocalDate latest   = all.stream().map(Expense::getDate).max(LocalDate::compareTo).orElse(null);

        return new FinanceSummaryDTO(
                categoryTotals, monthlyTotals, topExpenses,
                totalAmount, totalCount, averageAmount, highestAmount,
                mostUsedPaymentMethod, earliest, latest);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyInsightDTO getMonthlyInsightData(int month, int year) {
        YearMonth target    = YearMonth.of(year, month);
        YearMonth previous  = target.minusMonths(1);

        LocalDate targetStart = target.atDay(1);
        LocalDate targetEnd   = target.atEndOfMonth();
        LocalDate prevStart   = previous.atDay(1);
        LocalDate prevEnd     = previous.atEndOfMonth();

        List<Expense> thisMonth = expenseRepository.findAll().stream()
                .filter(e -> !e.getDate().isBefore(targetStart) && !e.getDate().isAfter(targetEnd))
                .toList();

        Map<String, BigDecimal> categoryTotals = thisMonth.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().name(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        BigDecimal overallTotal = thisMonth.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lastMonthTotal = expenseRepository.findAll().stream()
                .filter(e -> !e.getDate().isBefore(prevStart) && !e.getDate().isAfter(prevEnd))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double changePercentage = 0.0;
        if (lastMonthTotal.signum() != 0) {
            changePercentage = overallTotal.subtract(lastMonthTotal)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(lastMonthTotal, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        String monthName = target.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + year;

        return new MonthlyInsightDTO(
                categoryTotals, overallTotal, lastMonthTotal, changePercentage, monthName);
    }

    private static String formatMoney(BigDecimal amount) {
        return String.format("%,.0f", amount);
    }

    private static String friendlyCategory(String enumName) {
        return enumName.charAt(0) + enumName.substring(1).toLowerCase();
    }

    private Expense findOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found: " + id));
    }
}
