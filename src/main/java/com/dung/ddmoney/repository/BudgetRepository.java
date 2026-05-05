package com.dung.ddmoney.repository;

import com.dung.ddmoney.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByMonthAndYear(int month, int year);

    Optional<Budget> findByCategoryIdAndMonthAndYear(Long categoryId, int month, int year);

    @Query("""
        SELECT b FROM Budget b
        WHERE b.month = :month AND b.year = :year
        ORDER BY b.category.name
    """)
    List<Budget> findByMonthAndYearOrdered(@Param("month") int month, @Param("year") int year);
}
