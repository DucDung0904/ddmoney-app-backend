package com.dung.ddmoney.dto;

import com.dung.ddmoney.entity.Transaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseBookDto {

    @Data
    public static class Summary {
        private BigDecimal totalExpense = BigDecimal.ZERO;
        private BigDecimal totalIncome = BigDecimal.ZERO;
        private BigDecimal balance = BigDecimal.ZERO;
        private long transactionCount;
    }

    @Data
    public static class TransactionItem {
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private Long walletId;
        private String walletName;
        private BigDecimal amount;
        private Transaction.TransactionType type;
        private String note;
        private LocalDate transactionDate;

        public static TransactionItem from(Transaction transaction) {
            TransactionItem item = new TransactionItem();
            item.setId(transaction.getId());
            item.setAmount(transaction.getAmount());
            item.setType(transaction.getType());
            item.setNote(transaction.getNote());
            item.setTransactionDate(transaction.getDate());

            if (transaction.getCategory() != null) {
                item.setCategoryId(transaction.getCategory().getId());
                item.setCategoryName(transaction.getCategory().getName());
                item.setCategoryIcon(transaction.getCategory().getIcon());
                item.setCategoryColor(transaction.getCategory().getColorHex());
            }

            if (transaction.getWallet() != null) {
                item.setWalletId(transaction.getWallet().getId());
                item.setWalletName(transaction.getWallet().getName());
            }

            return item;
        }
    }

    @Data
    public static class CategoryStatistic {
        private Long categoryId;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private float percentage;
    }

    @Data
    public static class DailySummary {
        private LocalDate date;
        private BigDecimal totalExpense = BigDecimal.ZERO;
        private BigDecimal totalIncome = BigDecimal.ZERO;
        private BigDecimal balance = BigDecimal.ZERO;
        private long transactionCount;
    }
}
