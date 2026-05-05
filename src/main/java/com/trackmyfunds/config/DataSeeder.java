package com.trackmyfunds.config;

import com.trackmyfunds.enums.Category;
import com.trackmyfunds.enums.PaymentMethod;
import com.trackmyfunds.model.Expense;
import com.trackmyfunds.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ExpenseRepository expenseRepository;

    @Override
    public void run(String... args) {
        if (expenseRepository.count() > 0) {
            log.info("DataSeeder: data already present, skipping seed.");
            return;
        }

        LocalDate now = LocalDate.now();

        List<Expense> expenses = List.of(

            // ── FOOD (8) ─────────────────────────────────────────────────────────
            build("Monthly grocery run",        "3200.00", Category.FOOD,          PaymentMethod.CARD,       now.minusMonths(5).withDayOfMonth(3),  "Big Bazaar — monthly stock-up"),
            build("Dominos pizza delivery",     "650.00",  Category.FOOD,          PaymentMethod.UPI,        now.minusMonths(5).withDayOfMonth(14), "Family movie-night pizza"),
            build("Restaurant dinner",          "1200.00", Category.FOOD,          PaymentMethod.CARD,       now.minusMonths(4).withDayOfMonth(8),  "Anniversary dinner"),
            build("Morning coffee",             "180.00",  Category.FOOD,          PaymentMethod.CASH,       now.minusMonths(4).withDayOfMonth(20), null),
            build("Zomato order",               "420.00",  Category.FOOD,          PaymentMethod.UPI,        now.minusMonths(3).withDayOfMonth(5),  "Late-night biryani"),
            build("Supermarket groceries",      "2100.00", Category.FOOD,          PaymentMethod.NETBANKING, now.minusMonths(3).withDayOfMonth(18), null),
            build("Cafe breakfast",             "350.00",  Category.FOOD,          PaymentMethod.CASH,       now.minusMonths(2).withDayOfMonth(10), null),
            build("Weekly groceries",           "1850.00", Category.FOOD,          PaymentMethod.CARD,       now.minusMonths(1).withDayOfMonth(6),  "Fruits and vegetables"),

            // ── TRANSPORT (7) ────────────────────────────────────────────────────
            build("Monthly metro pass",         "500.00",  Category.TRANSPORT,     PaymentMethod.CARD,       now.minusMonths(5).withDayOfMonth(1),  null),
            build("Uber to office",             "280.00",  Category.TRANSPORT,     PaymentMethod.UPI,        now.minusMonths(5).withDayOfMonth(7),  null),
            build("Auto fare",                  "120.00",  Category.TRANSPORT,     PaymentMethod.CASH,       now.minusMonths(4).withDayOfMonth(15), null),
            build("Petrol fill-up",             "2500.00", Category.TRANSPORT,     PaymentMethod.CARD,       now.minusMonths(3).withDayOfMonth(12), "Full tank"),
            build("Ola cab to airport",         "850.00",  Category.TRANSPORT,     PaymentMethod.UPI,        now.minusMonths(2).withDayOfMonth(3),  "Early morning flight"),
            build("Bus ticket",                 "80.00",   Category.TRANSPORT,     PaymentMethod.CASH,       now.minusMonths(2).withDayOfMonth(22), null),
            build("Train ticket Mumbai–Pune",   "650.00",  Category.TRANSPORT,     PaymentMethod.NETBANKING, now.minusMonths(1).withDayOfMonth(14), "Weekend trip"),

            // ── ENTERTAINMENT (6) ────────────────────────────────────────────────
            build("Netflix subscription",       "499.00",  Category.ENTERTAINMENT, PaymentMethod.CARD,       now.minusMonths(5).withDayOfMonth(10), "Monthly renewal"),
            build("Movie tickets",              "600.00",  Category.ENTERTAINMENT, PaymentMethod.UPI,        now.minusMonths(4).withDayOfMonth(19), "2 tickets — PVR Gold"),
            build("Spotify premium",            "119.00",  Category.ENTERTAINMENT, PaymentMethod.CARD,       now.minusMonths(3).withDayOfMonth(10), null),
            build("Amazon Prime annual",        "1499.00", Category.ENTERTAINMENT, PaymentMethod.CARD,       now.minusMonths(3).withDayOfMonth(25), "Annual plan"),
            build("Amusement park entry",       "1200.00", Category.ENTERTAINMENT, PaymentMethod.CARD,       now.minusMonths(2).withDayOfMonth(16), "Wonderla with friends"),
            build("Gaming top-up",              "500.00",  Category.ENTERTAINMENT, PaymentMethod.UPI,        now.minusMonths(1).withDayOfMonth(9),  "BGMI UC purchase"),

            // ── HEALTH (6) ───────────────────────────────────────────────────────
            build("Doctor consultation",        "800.00",  Category.HEALTH,        PaymentMethod.CARD,       now.minusMonths(5).withDayOfMonth(22), "GP visit — fever"),
            build("Pharmacy medicines",         "1350.00", Category.HEALTH,        PaymentMethod.CASH,       now.minusMonths(4).withDayOfMonth(2),  "Antibiotics and vitamins"),
            build("Gym membership",             "2000.00", Category.HEALTH,        PaymentMethod.NETBANKING, now.minusMonths(4).withDayOfMonth(1),  "Monthly plan"),
            build("Annual health checkup",      "3500.00", Category.HEALTH,        PaymentMethod.CARD,       now.minusMonths(3).withDayOfMonth(15), "Full-body blood work"),
            build("Dental checkup",             "1200.00", Category.HEALTH,        PaymentMethod.UPI,        now.minusMonths(2).withDayOfMonth(8),  "Cleaning + X-ray"),
            build("Vitamins and supplements",   "950.00",  Category.HEALTH,        PaymentMethod.CARD,       now.minusMonths(1).withDayOfMonth(20), "Whey protein + multivitamin"),

            // ── UTILITIES (6) ────────────────────────────────────────────────────
            build("Electricity bill",           "1800.00", Category.UTILITIES,     PaymentMethod.NETBANKING, now.minusMonths(5).withDayOfMonth(5),  "MSEB monthly bill"),
            build("Broadband internet",         "999.00",  Category.UTILITIES,     PaymentMethod.NETBANKING, now.minusMonths(5).withDayOfMonth(5),  "ACT Fibernet"),
            build("Gas cylinder refill",        "1050.00", Category.UTILITIES,     PaymentMethod.CASH,       now.minusMonths(4).withDayOfMonth(12), null),
            build("Water bill",                 "350.00",  Category.UTILITIES,     PaymentMethod.NETBANKING, now.minusMonths(3).withDayOfMonth(8),  null),
            build("Mobile recharge",            "599.00",  Category.UTILITIES,     PaymentMethod.UPI,        now.minusMonths(2).withDayOfMonth(1),  "Jio 84-day plan"),
            build("DTH recharge",               "399.00",  Category.UTILITIES,     PaymentMethod.UPI,        now.minusMonths(1).withDayOfMonth(3),  "Tata Play monthly"),

            // ── SHOPPING (6) ─────────────────────────────────────────────────────
            build("Casual t-shirts (3-pack)",   "1499.00", Category.SHOPPING,      PaymentMethod.CARD,       now.minusMonths(5).withDayOfMonth(18), "H&M sale"),
            build("Running shoes",              "3999.00", Category.SHOPPING,      PaymentMethod.CARD,       now.minusMonths(4).withDayOfMonth(26), "Nike Revolution 6"),
            build("Wireless earphones",         "1299.00", Category.SHOPPING,      PaymentMethod.UPI,        now.minusMonths(3).withDayOfMonth(20), "Boat Airdopes"),
            build("Household supplies",         "850.00",  Category.SHOPPING,      PaymentMethod.CASH,       now.minusMonths(2).withDayOfMonth(13), "Cleaning products"),
            build("Formal shirt",               "2200.00", Category.SHOPPING,      PaymentMethod.CARD,       now.minusMonths(1).withDayOfMonth(17), "Office wear — Van Heusen"),
            build("Kitchen utensils set",       "1650.00", Category.SHOPPING,      PaymentMethod.UPI,        now.minusDays(20),                     "Stainless steel cookware"),

            // ── EDUCATION (6) ────────────────────────────────────────────────────
            build("Udemy course bundle",        "499.00",  Category.EDUCATION,     PaymentMethod.CARD,       now.minusMonths(5).withDayOfMonth(25), "Java + Spring Boot"),
            build("Programming books",          "1200.00", Category.EDUCATION,     PaymentMethod.CARD,       now.minusMonths(4).withDayOfMonth(10), "Clean Code + Design Patterns"),
            build("Coursera subscription",      "2999.00", Category.EDUCATION,     PaymentMethod.NETBANKING, now.minusMonths(3).withDayOfMonth(1),  "3-month plan"),
            build("Notebook and stationery",    "350.00",  Category.EDUCATION,     PaymentMethod.CASH,       now.minusMonths(2).withDayOfMonth(5),  null),
            build("System design workshop",     "1500.00", Category.EDUCATION,     PaymentMethod.UPI,        now.minusMonths(1).withDayOfMonth(25), "Online masterclass"),
            build("LinkedIn Learning",          "1800.00", Category.EDUCATION,     PaymentMethod.NETBANKING, now.minusMonths(1).withDayOfMonth(28), "Annual subscription"),

            // ── OTHER (5) ────────────────────────────────────────────────────────
            build("Birthday gift",              "1800.00", Category.OTHER,         PaymentMethod.CARD,       now.minusMonths(4).withDayOfMonth(28), "Friend's birthday"),
            build("Charity donation",           "500.00",  Category.OTHER,         PaymentMethod.NETBANKING, now.minusMonths(3).withDayOfMonth(10), "NGO contribution"),
            build("Bank locker fee",            "500.00",  Category.OTHER,         PaymentMethod.NETBANKING, now.minusMonths(2).withDayOfMonth(1),  "Annual locker charge"),
            build("Magazine subscription",      "299.00",  Category.OTHER,         PaymentMethod.UPI,        now.minusMonths(1).withDayOfMonth(12), null),
            build("Miscellaneous expenses",     "650.00",  Category.OTHER,         PaymentMethod.CASH,       now.minusDays(10),                     null)
        );

        expenseRepository.saveAll(expenses);
        log.info("DataSeeder: seeded {} expense records.", expenses.size());
    }

    private Expense build(String title, String amount, Category category,
                          PaymentMethod paymentMethod, LocalDate date, String description) {
        return Expense.builder()
                .title(title)
                .amount(new BigDecimal(amount))
                .category(category)
                .paymentMethod(paymentMethod)
                .date(date)
                .description(description)
                .build();
    }
}
