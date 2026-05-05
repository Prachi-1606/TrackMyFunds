package com.trackmyfunds.service;

import com.trackmyfunds.dto.DashboardDTO;
import com.trackmyfunds.dto.ExpenseFilterDTO;
import com.trackmyfunds.dto.ExpenseRequestDTO;
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

    private Expense findOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found: " + id));
    }
}
