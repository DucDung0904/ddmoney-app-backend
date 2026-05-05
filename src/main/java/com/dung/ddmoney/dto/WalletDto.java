package com.dung.ddmoney.dto;

import com.dung.ddmoney.entity.Wallet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

// ─── Wallet DTOs ──────────────────────────────────────────────────────
public class WalletDto {

    @Data
    public static class Request {
        @NotBlank(message = "Tên ví không được trống")
        private String name;

        private BigDecimal balance = BigDecimal.ZERO;

        @NotNull(message = "Loại ví không được trống")
        private Wallet.WalletType type;

        private String bankName;
        private String cardNumber;
        private String colorHex = "#4659A6";
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private BigDecimal balance;
        private Wallet.WalletType type;
        private String bankName;
        private String cardNumber;
        private String colorHex;
        private Boolean isActive;

        public static Response from(Wallet w) {
            Response r = new Response();
            r.setId(w.getId());
            r.setName(w.getName());
            r.setBalance(w.getBalance());
            r.setType(w.getType());
            r.setBankName(w.getBankName());
            r.setCardNumber(w.getCardNumber());
            r.setColorHex(w.getColorHex());
            r.setIsActive(w.getIsActive());
            return r;
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
