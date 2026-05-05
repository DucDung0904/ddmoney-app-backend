package com.dung.ddmoney.config;

import com.dung.ddmoney.entity.Category;
import com.dung.ddmoney.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        // Init default categories if not exist
        if (categoryRepository.count() == 0) {
            List<Category> defaultCategories = List.of(
                    Category.builder().name("Ăn uống").icon("🍜").colorHex("#F44336").type(Category.CategoryType.EXPENSE).isDefault(true).build(),
                    Category.builder().name("Di chuyển").icon("🚗").colorHex("#FF9800").type(Category.CategoryType.EXPENSE).isDefault(true).build(),
                    Category.builder().name("Mua sắm").icon("🛍️").colorHex("#E91E63").type(Category.CategoryType.EXPENSE).isDefault(true).build(),
                    Category.builder().name("Giải trí").icon("🎬").colorHex("#9C27B0").type(Category.CategoryType.EXPENSE).isDefault(true).build(),
                    Category.builder().name("Lương").icon("💰").colorHex("#4CAF50").type(Category.CategoryType.INCOME).isDefault(true).build(),
                    Category.builder().name("Thưởng").icon("🎁").colorHex("#8BC34A").type(Category.CategoryType.INCOME).isDefault(true).build(),
                    Category.builder().name("Đầu tư").icon("📈").colorHex("#00BCD4").type(Category.CategoryType.INCOME).isDefault(true).build()
            );
            categoryRepository.saveAll(defaultCategories);
            System.out.println("✅ Đã khởi tạo danh mục mặc định.");
        }
    }
}
