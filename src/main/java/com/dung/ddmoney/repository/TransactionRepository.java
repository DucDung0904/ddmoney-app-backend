package com.dung.ddmoney.repository;

import com.dung.ddmoney.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // All transactions sorted by date desc
    List<Transaction> findByUserIdOrderByDateDescCreatedAtDesc(Long userId);

    // By month + year
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.id = :userId AND MONTH(t.date) = :month AND YEAR(t.date) = :year
        ORDER BY t.date DESC, t.createdAt DESC
    """)
    List<Transaction> findByUserIdAndMonthAndYear(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);

    // By type + month + year
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.id = :userId AND t.type = :type
          AND MONTH(t.date) = :month AND YEAR(t.date) = :year
        ORDER BY t.date DESC
    """)
    List<Transaction> findByUserIdAndTypeAndMonthYear(
        @Param("userId") Long userId,
        @Param("type") Transaction.TransactionType type,
        @Param("month") int month,
        @Param("year") int year
    );

    // Sum income/expense per month for chart (last N months)
    @Query("""
        SELECT MONTH(t.date), YEAR(t.date),
               SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END),
               SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END)
        FROM Transaction t
        WHERE t.user.id = :userId AND t.date >= :from
          AND t.type IN ('INCOME', 'EXPENSE')
        GROUP BY YEAR(t.date), MONTH(t.date)
        ORDER BY YEAR(t.date), MONTH(t.date)
    """)
    List<Object[]> findMonthlyChartDataByUserId(@Param("userId") Long userId, @Param("from") LocalDate from);

    // Summary for current month
    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
        FROM Transaction t
        WHERE t.user.id = :userId AND MONTH(t.date) = :month AND YEAR(t.date) = :year
    """)
    Object[] getMonthlySummaryByUserId(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);

    // Category spending for current month
    @Query("""
        SELECT t.category.id, SUM(t.amount)
        FROM Transaction t
        WHERE t.user.id = :userId AND t.type = 'EXPENSE'
          AND MONTH(t.date) = :month AND YEAR(t.date) = :year
        GROUP BY t.category.id
        ORDER BY SUM(t.amount) DESC
    """)
    List<Object[]> getCategorySpendingByUserId(@Param("userId") Long userId, @Param("month") int month, @Param("year") int year);
}
