package com.dung.ddmoney.dto;

import com.dung.ddmoney.entity.Budget;
import com.dung.ddmoney.entity.Category;
import com.dung.ddmoney.entity.Wallet;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BudgetDto {

    @Data
    public static class Request {
        @NotBlank(message = "Tên ngân sách không được để trống")
        private String name;

        @DecimalMin(value = "0.01", message = "Số tiền ngân sách phải lớn hơn 0")
        private BigDecimal amount;

        // Legacy single-category field. New clients should send categoryIds.
        @Deprecated
        private Long categoryId;
        private Long walletId;
        private Budget.BudgetScope scope;
        private Budget.WalletScope walletScope;
        private Budget.PeriodType periodType;
        private Budget.RepeatType repeatType;
        private Boolean repeat;
        private LocalDate startDate;
        private LocalDate endDate;

        // Multi-category selection used by the current mobile app.
        private List<Long> categoryIds;
        // Legacy month/year fields retained for older clients.
        @Deprecated
        private Integer month;
        @Deprecated
        private Integer year;
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private BigDecimal amount;
        @Deprecated
        private BigDecimal budgetAmount;
        private BigDecimal spentAmount;
        private BigDecimal remainingAmount;
        private Float percentUsed;
        @Deprecated
        private Float percentage;
        private Budget.BudgetStatus status;
        @Deprecated
        private Long categoryId;
        @Deprecated
        private String categoryName;
        private Long walletId;
        private String walletName;
        private Budget.BudgetScope scope;
        private Budget.WalletScope walletScope;
        private Budget.PeriodType periodType;
        private Budget.RepeatType repeatType;
        private LocalDate startDate;
        private LocalDate endDate;
        @Deprecated
        private Integer month;
        @Deprecated
        private Integer year;
        private List<Long> categoryIds = new ArrayList<>();
        private List<CategoryDto.Response> categories = new ArrayList<>();

        public static Response from(Budget budget, BigDecimal spentAmount) {
            Response response = new Response();
            response.fillFrom(budget, spentAmount);
            return response;
        }

        protected void fillFrom(Budget budget, BigDecimal spentAmount) {
            BigDecimal budgetAmount = safeMoney(budget.getAmount());
            BigDecimal spent = safeMoney(spentAmount);
            BigDecimal remaining = budgetAmount.subtract(spent);
            BigDecimal percent = calculatePercent(spent, budgetAmount);

            this.id = budget.getId();
            this.name = budget.getName();
            this.amount = budgetAmount;
            this.budgetAmount = budgetAmount;
            this.spentAmount = spent;
            this.remainingAmount = remaining;
            this.percentUsed = percent.floatValue();
            this.percentage = budgetAmount.compareTo(BigDecimal.ZERO) > 0
                    ? spent.divide(budgetAmount, 4, RoundingMode.HALF_UP).floatValue()
                    : 0f;
            this.status = determineStatus(percent);
            this.scope = budget.getScope();
            this.walletScope = budget.getWalletScope();
            this.periodType = budget.getPeriodType();
            this.repeatType = budget.getRepeatType();
            this.startDate = budget.getStartDate();
            this.endDate = budget.getEndDate();
            this.month = budget.getMonth();
            this.year = budget.getYear();

            List<Category> selectedCategories = budget.getCategories() == null
                    ? List.of()
                    : budget.getCategories().stream()
                            .sorted(Comparator.comparing(
                                    Category::getSortOrder,
                                    Comparator.nullsLast(Integer::compareTo)
                            ).thenComparing(Category::getName))
                            .toList();
            Category legacyCategory = budget.getCategory();
            Category primaryCategory = selectedCategories.isEmpty() ? legacyCategory : selectedCategories.get(0);
            if (primaryCategory != null) {
                this.categoryId = primaryCategory.getId();
                this.categoryName = primaryCategory.getName();
                this.categories = selectedCategories.isEmpty()
                        ? List.of(CategoryDto.Response.from(primaryCategory))
                        : selectedCategories.stream().map(CategoryDto.Response::from).toList();
                this.categoryIds = this.categories.stream().map(CategoryDto.Response::getId).toList();
            } else {
                this.categoryName = "Tất cả danh mục";
                this.categories = List.of();
                this.categoryIds = List.of();
            }

            Wallet wallet = budget.getWallet();
            if (wallet != null) {
                this.walletId = wallet.getId();
                this.walletName = wallet.getName();
            } else {
                this.walletName = "Tất cả ví";
            }
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class DetailResponse extends Response {
        private BigDecimal dailySuggestedAmount;
        private BigDecimal projectedSpending;
        private BigDecimal actualDailySpending;
        private List<CategorySpendingResponse> spentByCategory = new ArrayList<>();
        private List<TransactionDto.Response> transactions = new ArrayList<>();

        public static DetailResponse from(Budget budget, BigDecimal spentAmount) {
            DetailResponse response = new DetailResponse();
            response.fillFrom(budget, spentAmount);
            return response;
        }
    }

    @Data
    public static class SummaryResponse {
        private BigDecimal totalBudgetAmount;
        private BigDecimal totalSpentAmount;
        private BigDecimal totalRemainingAmount;
        private Float percentUsed;
        private Budget.BudgetStatus status;
        private Integer count;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class CategorySpendingResponse {
        private Long categoryId;
        private String categoryName;
        private String icon;
        private String colorHex;
        private BigDecimal amount;
        private Float percentUsed;
    }

    private static BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal calculatePercent(BigDecimal spentAmount, BigDecimal budgetAmount) {
        if (budgetAmount == null || budgetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount.multiply(BigDecimal.valueOf(100))
                .divide(budgetAmount, 2, RoundingMode.HALF_UP);
    }

    private static Budget.BudgetStatus determineStatus(BigDecimal percentUsed) {
        if (percentUsed.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return Budget.BudgetStatus.EXCEEDED;
        }
        if (percentUsed.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return Budget.BudgetStatus.WARNING;
        }
        return Budget.BudgetStatus.SAFE;
    }
}
