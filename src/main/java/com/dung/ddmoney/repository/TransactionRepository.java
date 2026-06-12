package com.dung.ddmoney.repository;

import com.dung.ddmoney.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByDateDescCreatedAtDesc(Long userId);

    List<Transaction> findByUserIdAndDateBetweenOrderByDateDescCreatedAtDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdAndTypeAndDateBetweenOrderByDateDescCreatedAtDesc(
            Long userId, Transaction.TransactionType type, LocalDate startDate, LocalDate endDate);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.user.id = :userId
              AND MONTH(t.date) = :month
              AND YEAR(t.date) = :year
            ORDER BY t.date DESC, t.createdAt DESC
            """)
    List<Transaction> findByUserIdAndMonthAndYear(@Param("userId") Long userId,
                                                  @Param("month") Integer month,
                                                  @Param("year") Integer year);

    @Query("""
            SELECT t.category.name, SUM(t.amount)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.type = :type
              AND MONTH(t.date) = :month
              AND YEAR(t.date) = :year
            GROUP BY t.category.name
            """)
    List<Object[]> getCategorySpending(@Param("userId") Long userId,
                                       @Param("type") Transaction.TransactionType type,
                                       @Param("month") Integer month,
                                       @Param("year") Integer year);

    @Query("""
            SELECT SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END),
                   SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND MONTH(t.date) = :month
              AND YEAR(t.date) = :year
            """)
    Object[] getMonthlySummary(@Param("userId") Long userId,
                               @Param("month") Integer month,
                               @Param("year") Integer year);

    @Query("""
            SELECT MONTH(t.date), SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END),
                   SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END)
            FROM Transaction t
            WHERE t.user.id = :userId AND YEAR(t.date) = :year
            GROUP BY MONTH(t.date)
            ORDER BY MONTH(t.date)
            """)
    List<Object[]> getYearlyChart(@Param("userId") Long userId, @Param("year") Integer year);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND MONTH(t.date) = :month
              AND YEAR(t.date) = :year
            """)
    Object[] getMonthlySummaryByUserId(@Param("userId") Long userId,
                                       @Param("month") Integer month,
                                       @Param("year") Integer year);

    @Query("""
            SELECT MONTH(t.date), YEAR(t.date),
                   COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.date >= :fromDate
            GROUP BY YEAR(t.date), MONTH(t.date)
            ORDER BY YEAR(t.date), MONTH(t.date)
            """)
    List<Object[]> findMonthlyChartDataByUserId(@Param("userId") Long userId,
                                                @Param("fromDate") LocalDate fromDate);

    @Query("""
            SELECT t.category.id, COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.type = 'EXPENSE'
              AND MONTH(t.date) = :month
              AND YEAR(t.date) = :year
            GROUP BY t.category.id
            ORDER BY COALESCE(SUM(t.amount), 0) DESC
            """)
    List<Object[]> getCategorySpendingByUserId(@Param("userId") Long userId,
                                               @Param("month") Integer month,
                                               @Param("year") Integer year);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            LEFT JOIN t.category category
            LEFT JOIN category.parent parent
            WHERE t.user.id = :userId
              AND t.type = :type
              AND t.date BETWEEN :startDate AND :endDate
              AND (:allCategories = true
                   OR category.id IN :categoryIds
                   OR parent.id IN :categoryIds)
              AND (:walletId IS NULL OR t.wallet.id = :walletId)
              AND (:walletId IS NOT NULL OR (t.wallet.isActive = true AND t.wallet.isArchived = false))
            """)
    BigDecimal sumExpenseForBudget(@Param("userId") Long userId,
                                   @Param("type") Transaction.TransactionType type,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   @Param("categoryIds") List<Long> categoryIds,
                                   @Param("allCategories") boolean allCategories,
                                   @Param("walletId") Long walletId);

    @Query("""
            SELECT t FROM Transaction t
            LEFT JOIN t.category category
            LEFT JOIN category.parent parent
            WHERE t.user.id = :userId
              AND t.type = :type
              AND t.date BETWEEN :startDate AND :endDate
              AND (:allCategories = true
                   OR category.id IN :categoryIds
                   OR parent.id IN :categoryIds)
              AND (:walletId IS NULL OR t.wallet.id = :walletId)
              AND (:walletId IS NOT NULL OR (t.wallet.isActive = true AND t.wallet.isArchived = false))
            ORDER BY t.date DESC, t.createdAt DESC
            """)
    List<Transaction> findExpensesForBudget(@Param("userId") Long userId,
                                            @Param("type") Transaction.TransactionType type,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("categoryIds") List<Long> categoryIds,
                                            @Param("allCategories") boolean allCategories,
                                            @Param("walletId") Long walletId);

    @Query("""
            SELECT category.id, category.name, category.icon, category.colorHex, COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            LEFT JOIN t.category category
            LEFT JOIN category.parent parent
            WHERE t.user.id = :userId
              AND t.type = :type
              AND t.date BETWEEN :startDate AND :endDate
              AND (:allCategories = true
                   OR category.id IN :categoryIds
                   OR parent.id IN :categoryIds)
              AND (:walletId IS NULL OR t.wallet.id = :walletId)
              AND (:walletId IS NOT NULL OR (t.wallet.isActive = true AND t.wallet.isArchived = false))
            GROUP BY category.id, category.name, category.icon, category.colorHex
            ORDER BY COALESCE(SUM(t.amount), 0) DESC
            """)
    List<Object[]> sumExpenseGroupByCategoryForBudget(@Param("userId") Long userId,
                                                      @Param("type") Transaction.TransactionType type,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate,
                                                      @Param("categoryIds") List<Long> categoryIds,
                                                      @Param("allCategories") boolean allCategories,
                                                      @Param("walletId") Long walletId);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
                   COUNT(t)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.date BETWEEN :fromDate AND :toDate
            """)
    Object[] getExpenseBookSummary(@Param("userId") Long userId,
                                   @Param("fromDate") LocalDate fromDate,
                                   @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.user.id = :userId
              AND t.date BETWEEN :fromDate AND :toDate
              AND (:type IS NULL OR t.type = :type)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:walletId IS NULL OR t.wallet.id = :walletId)
            ORDER BY t.date DESC, t.createdAt DESC
            """)
    List<Transaction> findExpenseBookTransactions(@Param("userId") Long userId,
                                                  @Param("fromDate") LocalDate fromDate,
                                                  @Param("toDate") LocalDate toDate,
                                                  @Param("type") Transaction.TransactionType type,
                                                  @Param("categoryId") Long categoryId,
                                                  @Param("walletId") Long walletId);

    @Query("""
            SELECT t.category.id, t.category.name, t.category.icon, t.category.colorHex,
                   COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.type = 'EXPENSE'
              AND t.date BETWEEN :fromDate AND :toDate
            GROUP BY t.category.id, t.category.name, t.category.icon, t.category.colorHex
            ORDER BY COALESCE(SUM(t.amount), 0) DESC
            """)
    List<Object[]> getExpenseBookCategoryStatistics(@Param("userId") Long userId,
                                                    @Param("fromDate") LocalDate fromDate,
                                                    @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT t.date,
                   COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
                   COUNT(t)
            FROM Transaction t
            WHERE t.user.id = :userId
              AND t.date BETWEEN :fromDate AND :toDate
            GROUP BY t.date
            ORDER BY t.date DESC
            """)
    List<Object[]> getExpenseBookDailySummary(@Param("userId") Long userId,
                                              @Param("fromDate") LocalDate fromDate,
                                              @Param("toDate") LocalDate toDate);
}
