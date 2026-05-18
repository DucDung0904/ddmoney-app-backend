package com.dung.ddmoney.dto;

import com.dung.ddmoney.entity.Wallet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

// Wallet DTOs
public class WalletDto {

    @Data
    public static class Request {
        @NotBlank(message = "Ten vi khong duoc trong")
        private String name;

        private BigDecimal balance = BigDecimal.ZERO;

        @NotNull(message = "Loai vi khong duoc trong")
        private Wallet.WalletType type;

        private String bankName;
        private String cardNumber;
        private String colorHex = "#4659A6";
        private String icon = "wallet";
        private String currency = "VND";
        private Boolean isDefault = false;
        private Boolean isIncludedInTotal = true;
        private Integer sortOrder = 0;

        private BigDecimal creditLimit;
        private BigDecimal currentDebt;
        private Integer billingDay;
        private Integer paymentDueDay;
    }

    @Data
    public static class Response {
        private Long id;
        private Long userId;
        private String name;
        private BigDecimal balance;
        private Wallet.WalletType type;
        private String bankName;
        private String cardNumber;
        private String colorHex;
        private String icon;
        private String currency;
        private Boolean isActive;
        private Boolean isDefault;
        private Boolean isArchived;
        private Boolean isIncludedInTotal;
        private Integer sortOrder;
        private BigDecimal creditLimit;
        private BigDecimal currentDebt;
        private Integer billingDay;
        private Integer paymentDueDay;

        public static Response from(Wallet w) {
            Response r = new Response();
            r.setId(w.getId());
            r.setUserId(w.getUser() != null ? w.getUser().getId() : null);
            r.setName(w.getName());
            r.setBalance(w.getBalance());
            r.setType(normalizedType(w.getType()));
            r.setBankName(w.getBankName());
            r.setCardNumber(w.getCardNumber());
            r.setColorHex(w.getColorHex());
            r.setIcon(w.getIcon());
            r.setCurrency(w.getCurrency());
            r.setIsActive(w.getIsActive());
            r.setIsDefault(w.getIsDefault());
            r.setIsArchived(w.getIsArchived());
            r.setIsIncludedInTotal(w.getIsIncludedInTotal());
            r.setSortOrder(w.getSortOrder());
            r.setCreditLimit(w.getCreditLimit());
            r.setCurrentDebt(w.getCurrentDebt());
            r.setBillingDay(w.getBillingDay());
            r.setPaymentDueDay(w.getPaymentDueDay());
            return r;
        }

        private static Wallet.WalletType normalizedType(Wallet.WalletType type) {
            return type == Wallet.WalletType.CREDIT ? Wallet.WalletType.CREDIT_CARD : type;
        }
    }

    @Data
    public static class TransferRequest {
        @NotNull private Long fromWalletId;
        @NotNull private Long toWalletId;
        @NotNull private BigDecimal amount;
        private String note;
    }
}
