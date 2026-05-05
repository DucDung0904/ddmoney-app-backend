package com.dung.ddmoney.dto;

import com.dung.ddmoney.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class CategoryDto {

    @Data
    public static class Request {
        @NotBlank(message = "Tên danh mục không được trống")
        private String name;

        private String icon = "📦";
        private String colorHex = "#4659A6";

        @NotNull(message = "Loại danh mục không được trống")
        private Category.CategoryType type;
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private String icon;
        private String colorHex;
        private Category.CategoryType type;
        private Boolean isDefault;

        public static Response from(Category c) {
            Response r = new Response();
            r.setId(c.getId());
            r.setName(c.getName());
            r.setIcon(c.getIcon());
            r.setColorHex(c.getColorHex());
            r.setType(c.getType());
            r.setIsDefault(c.getIsDefault());
            return r;
        }
    }
}
