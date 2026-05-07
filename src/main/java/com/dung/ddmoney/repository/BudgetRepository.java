package com.dung.ddmoney.repository;

import com.dung.ddmoney.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdAndMonthAndYear(Long userId, int month, int year);

    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYear(Long userId, Long categoryId, int month, int year);

    @Query("""
        SELECT b FROM Budget b
        WHERE b.user.id = :userId AND b.month = :month AND b.year = :year
        ORDER BY b.category.name
    """)
    List<Budget> findByUserIdAndMonthAndYearOrdered(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);
}
