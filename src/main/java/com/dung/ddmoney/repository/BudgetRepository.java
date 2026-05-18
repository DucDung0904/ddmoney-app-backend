package com.dung.ddmoney.repository;

import com.dung.ddmoney.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query("""
            SELECT b FROM Budget b
            WHERE b.user.id = :userId
              AND b.active = true
              AND b.startDate <= :endDate
              AND b.endDate >= :startDate
            ORDER BY b.startDate DESC, b.name ASC
            """)
    List<Budget> findActiveByUserAndPeriodOverlap(@Param("userId") Long userId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT b FROM Budget b
            WHERE b.user.id = :userId
              AND b.active = true
              AND :date BETWEEN b.startDate AND b.endDate
            ORDER BY b.startDate DESC, b.name ASC
            """)
    List<Budget> findCurrentActiveBudgets(@Param("userId") Long userId,
                                           @Param("date") LocalDate date);

    @Query("""
            SELECT b FROM Budget b
            WHERE b.user.id = :userId
              AND b.active = true
            ORDER BY b.startDate DESC, b.name ASC
            """)
    List<Budget> findByUserIdAndActiveTrueOrderByStartDateDescNameAsc(@Param("userId") Long userId);

    @Query("""
            SELECT b FROM Budget b
            WHERE b.id = :budgetId
              AND b.user.id = :userId
              AND b.active = true
            """)
    Optional<Budget> findActiveByIdAndUserId(@Param("budgetId") Long budgetId,
                                             @Param("userId") Long userId);

    @Query("""
            SELECT b FROM Budget b
            WHERE b.user.id = :userId
              AND b.active = true
              AND (:excludeId IS NULL OR b.id <> :excludeId)
              AND ((:categoryId IS NULL AND b.category IS NULL) OR b.category.id = :categoryId)
              AND ((:walletId IS NULL AND b.wallet IS NULL) OR b.wallet.id = :walletId)
              AND b.startDate <= :endDate
              AND b.endDate >= :startDate
            """)
    Optional<Budget> findDuplicateBudget(@Param("userId") Long userId,
                                         @Param("categoryId") Long categoryId,
                                         @Param("walletId") Long walletId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("excludeId") Long excludeId);

    // Compatibility for existing mobile calls that still request budgets by month/year.
    default List<Budget> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return findActiveByUserAndPeriodOverlap(userId, startDate, endDate);
    }

    default List<Budget> findByUserIdAndMonthAndYearOrdered(Long userId, Integer month, Integer year) {
        return findByUserIdAndMonthAndYear(userId, month, year);
    }
}
