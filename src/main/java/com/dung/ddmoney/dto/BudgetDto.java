package com.dung.ddmoney.dto;

import com.dung.ddmoney.entity.Budget;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

public class BudgetDto {

    @Data
    public static class Request {
        @NotNull private Long categoryId;

        @NotNull @Positive
        private BigDecimal amount;

        @NotNull private Integer month;
        @NotNull private Integer year;
    }

    @Data
    public static class Response {
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private BigDecimal budgetAmount;
        private BigDecimal spentAmount;
        private BigDecimal remainingAmount;
        private float percentage;
        private int month;
        private int year;

        public static Response from(Budget b) {
            Response r = new Response();
            r.setId(b.getId());
            r.setMonth(b.getMonth());
            r.setYear(b.getYear());
            r.setBudgetAmount(b.getAmount());
            if (b.getCategory() != null) {
                r.setCategoryId(b.getCategory().getId());
                r.setCategoryName(b.getCategory().getName());
                r.setCategoryIcon(b.getCategory().getIcon());
                r.setCategoryColor(b.getCategory().getColorHex());
            }
            return r;
        }
    }
}
