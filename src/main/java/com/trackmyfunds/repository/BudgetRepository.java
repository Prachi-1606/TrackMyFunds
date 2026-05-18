package com.trackmyfunds.repository;

import com.trackmyfunds.enums.Category;
import com.trackmyfunds.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByCategoryAndMonthAndYear(Category category, int month, int year);

    List<Budget> findByMonthAndYear(int month, int year);
}
