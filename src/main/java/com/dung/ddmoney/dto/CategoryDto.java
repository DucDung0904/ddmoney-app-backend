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
        private Long parentId;
        private Integer sortOrder = 10_000;
    }

    @Data
    public static class Response {
        private Long id;
        private Long userId;
        private String name;
        private String icon;
        private String colorHex;
        private Category.CategoryType type;
        private Boolean isDefault;
        private Boolean isEditable;
        private Boolean isDeletable;
        private Boolean isDeleted;
        private Long parentId;
        private Integer sortOrder;

        public static Response from(Category c) {
            Response r = new Response();
            r.setId(c.getId());
            r.setUserId(c.getUser() != null ? c.getUser().getId() : null);
            r.setName(c.getName());
            r.setIcon(c.getIcon());
            r.setColorHex(c.getColorHex());
            r.setType(c.getType());
            r.setIsDefault(c.getIsDefault());
            r.setIsEditable(c.getIsEditable());
            r.setIsDeletable(c.getIsDeletable());
            r.setIsDeleted(c.getIsDeleted());
            r.setParentId(c.getParent() != null ? c.getParent().getId() : null);
            r.setSortOrder(c.getSortOrder());
            return r;
        }
    }
}
