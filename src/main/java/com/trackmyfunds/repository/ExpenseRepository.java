package com.trackmyfunds.repository;

import com.trackmyfunds.enums.Category;
import com.trackmyfunds.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    /** Returns all expenses in the given category whose date is strictly after the cutoff. */
    List<Expense> findByCategoryAndDateAfter(Category category, LocalDate date);

    /** Total spend for one category within an inclusive date window. Returns 0 if none. */
    @Query("""
           SELECT COALESCE(SUM(e.amount), 0)
             FROM Expense e
            WHERE e.category = :category
              AND e.date BETWEEN :start AND :end
           """)
    BigDecimal sumByCategoryAndDateBetween(
            @Param("category") Category category,
            @Param("start")    LocalDate start,
            @Param("end")      LocalDate end);
}
