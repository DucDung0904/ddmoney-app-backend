package com.dung.ddmoney.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletType type;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "card_number", length = 20)
    private String cardNumber;

    @Column(name = "color_hex", length = 10)
    @Builder.Default
    private String colorHex = "#4659A6";

    @Column(length = 50)
    @Builder.Default
    private String icon = "wallet";

    @Column(length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_archived")
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "is_included_in_total")
    @Builder.Default
    private Boolean isIncludedInTotal = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "credit_limit", precision = 18, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "current_debt", precision = 18, scale = 2)
    private BigDecimal currentDebt;

    @Column(name = "billing_day")
    private Integer billingDay;

    @Column(name = "payment_due_day")
    private Integer paymentDueDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void normalizeLegacyType() {
        if (type == WalletType.CREDIT) {
            type = WalletType.CREDIT_CARD;
        }
    }

    public enum WalletType {
        CASH, BANK, EWALLET, CREDIT_CARD, SAVINGS, INVESTMENT, CREDIT
    }
}
