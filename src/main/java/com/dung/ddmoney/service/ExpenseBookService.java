package com.dung.ddmoney.service;

import com.dung.ddmoney.dto.ExpenseBookDto;
import com.dung.ddmoney.entity.Transaction;
import com.dung.ddmoney.entity.User;
import com.dung.ddmoney.repository.TransactionRepository;
import com.dung.ddmoney.repository.UserRepository;
import com.dung.ddmoney.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.sql.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseBookService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public ExpenseBookDto.Summary getSummary(LocalDate fromDate, LocalDate toDate) {
        Object[] raw = unwrapSingleRow(transactionRepository.getExpenseBookSummary(currentUserId(), fromDate, toDate));

        ExpenseBookDto.Summary summary = new ExpenseBookDto.Summary();
        summary.setTotalExpense(toBigDecimal(valueAt(raw, 0)));
        summary.setTotalIncome(toBigDecimal(valueAt(raw, 1)));
        summary.setBalance(summary.getTotalIncome().subtract(summary.getTotalExpense()));
        summary.setTransactionCount(toLong(valueAt(raw, 2)));
        return summary;
    }

    public List<ExpenseBookDto.TransactionItem> getTransactions(
            LocalDate fromDate,
            LocalDate toDate,
            Transaction.TransactionType type,
            Long categoryId,
            Long walletId
    ) {
        return transactionRepository
                .findExpenseBookTransactions(currentUserId(), fromDate, toDate, type, categoryId, walletId)
                .stream()
                .map(ExpenseBookDto.TransactionItem::from)
                .toList();
    }

    public List<ExpenseBookDto.CategoryStatistic> getCategoryStatistics(LocalDate fromDate, LocalDate toDate) {
        List<Object[]> raw = transactionRepository.getExpenseBookCategoryStatistics(currentUserId(), fromDate, toDate);
        BigDecimal total = raw.stream()
                .map(row -> toBigDecimal(row[4]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return raw.stream().map(row -> {
            BigDecimal amount = toBigDecimal(row[4]);

            ExpenseBookDto.CategoryStatistic statistic = new ExpenseBookDto.CategoryStatistic();
            statistic.setCategoryId(toLong(row[0]));
            statistic.setCategoryName((String) row[1]);
            statistic.setCategoryIcon((String) row[2]);
            statistic.setCategoryColor((String) row[3]);
            statistic.setTotalAmount(amount);
            statistic.setPercentage(
                    total.compareTo(BigDecimal.ZERO) == 0
                            ? 0f
                            : amount.divide(total, 4, RoundingMode.HALF_UP).floatValue()
            );
            return statistic;
        }).toList();
    }

    public List<ExpenseBookDto.DailySummary> getDailySummary(LocalDate fromDate, LocalDate toDate) {
        return transactionRepository.getExpenseBookDailySummary(currentUserId(), fromDate, toDate)
                .stream()
                .map(row -> {
                    BigDecimal totalExpense = toBigDecimal(row[1]);
                    BigDecimal totalIncome = toBigDecimal(row[2]);

                    ExpenseBookDto.DailySummary summary = new ExpenseBookDto.DailySummary();
                    summary.setDate(toLocalDate(row[0]));
                    summary.setTotalExpense(totalExpense);
                    summary.setTotalIncome(totalIncome);
                    summary.setBalance(totalIncome.subtract(totalExpense));
                    summary.setTransactionCount(toLong(row[3]));
                    return summary;
                })
                .toList();
    }

    private Long currentUserId() {
        User user = userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    private Object[] unwrapSingleRow(Object[] raw) {
        if (raw == null) return new Object[0];
        Object[] row = raw;
        while (row.length == 1 && row[0] instanceof Object[] nested) {
            row = nested;
        }
        return row;
    }

    private Object valueAt(Object[] row, int index) {
        return row != null && row.length > index ? row[index] : null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return new BigDecimal(number.toString());
        if (value instanceof Object[] row && row.length == 1) return toBigDecimal(row[0]);
        return new BigDecimal(value.toString());
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(value.toString());
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof Date sqlDate) return sqlDate.toLocalDate();
        return LocalDate.parse(value.toString());
    }
}
