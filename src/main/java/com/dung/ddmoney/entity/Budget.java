package com.dung.ddmoney.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    // Compatibility pointer for older clients; categories is the source of truth.
    private Category category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "budget_categories",
            joinColumns = @JoinColumn(name = "budget_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Category> categories = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BudgetScope scope = BudgetScope.ALL_CATEGORIES;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_scope", nullable = false, length = 30)
    @Builder.Default
    private WalletScope walletScope = WalletScope.ALL_WALLETS;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 30)
    @Builder.Default
    private PeriodType periodType = PeriodType.MONTH;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", nullable = false, length = 30)
    @Builder.Default
    private RepeatType repeatType = RepeatType.NONE;

    @Column(name = "start_date", nullable = false, columnDefinition = "DATE DEFAULT '2024-01-01'")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false, columnDefinition = "DATE DEFAULT '2024-12-31'")
    private LocalDate endDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // Legacy fields kept so older month/year clients and databases keep working.
    private Integer month;

    private Integer year;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.active == null) {
            this.active = true;
        }
        syncLegacyMonthYear();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        syncLegacyMonthYear();
    }

    private void syncLegacyMonthYear() {
        if (this.startDate != null) {
            this.month = this.startDate.getMonthValue();
            this.year = this.startDate.getYear();
        }
    }

    public enum BudgetScope {
        CATEGORY,
        ALL_CATEGORIES
    }

    public enum WalletScope {
        ONE_WALLET,
        ALL_WALLETS
    }

    public enum PeriodType {
        WEEK,
        MONTH,
        QUARTER,
        YEAR,
        CUSTOM
    }

    public enum RepeatType {
        NONE,
        WEEKLY,
        MONTHLY,
        QUARTERLY,
        YEARLY
    }

    public enum BudgetStatus {
        SAFE,
        WARNING,
        EXCEEDED
    }
}
