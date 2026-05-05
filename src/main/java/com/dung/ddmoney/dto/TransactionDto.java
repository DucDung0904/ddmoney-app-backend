package com.dung.ddmoney.dto;

import com.dung.ddmoney.entity.Transaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionDto {

    @Data
    public static class Request {
        private String title;

        @NotNull @Positive
        private BigDecimal amount;

        @NotNull
        private Transaction.TransactionType type;

        @NotNull
        private LocalDate date;

        @NotNull
        private Long walletId;

        @NotNull
        private Long categoryId;

        // Only for TRANSFER type
        private Long transferToWalletId;

        private String note;
    }

    @Data
    public static class Response {
        private Long id;
        private String title;
        private BigDecimal amount;
        private Transaction.TransactionType type;
        private LocalDate date;
        private String note;

        // Wallet info
        private Long walletId;
        private String walletName;

        // Category info
        private Long categoryId;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;

        // Transfer destination
        private Long transferToWalletId;
        private String transferToWalletName;

        public static Response from(Transaction t) {
            Response r = new Response();
            r.setId(t.getId());
            r.setTitle(t.getTitle());
            r.setAmount(t.getAmount());
            r.setType(t.getType());
            r.setDate(t.getDate());
            r.setNote(t.getNote());

            if (t.getWallet() != null) {
                r.setWalletId(t.getWallet().getId());
                r.setWalletName(t.getWallet().getName());
            }
            if (t.getCategory() != null) {
                r.setCategoryId(t.getCategory().getId());
                r.setCategoryName(t.getCategory().getName());
                r.setCategoryIcon(t.getCategory().getIcon());
                r.setCategoryColor(t.getCategory().getColorHex());
            }
            if (t.getTransferToWallet() != null) {
                r.setTransferToWalletId(t.getTransferToWallet().getId());
                r.setTransferToWalletName(t.getTransferToWallet().getName());
            }
            return r;
        }
    }

    @Data
    public static class Summary {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal balance;
        private int month;
        private int year;
    }

    @Data
    public static class MonthlyChart {
        private int month;
        private int year;
        private String monthLabel;
        private BigDecimal income;
        private BigDecimal expense;
    }

    @Data
    public static class CategorySpending {
        private Long categoryId;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private BigDecimal amount;
        private float percentage;
    }
}
