package com.dung.ddmoney.config;

import com.dung.ddmoney.entity.Category;
import com.dung.ddmoney.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        initCategories();
    }

    private void initCategories() {
        if (categoryRepository.count() > 0) return;

        log.info(">> Khởi tạo danh mục mặc định...");

        List<Category> defaults = List.of(
            // ── EXPENSE ──────────────────────────────────────
            cat("Ăn uống",      "🍜", "#F44336", Category.CategoryType.EXPENSE),
            cat("Di chuyển",    "🚗", "#FF9800", Category.CategoryType.EXPENSE),
            cat("Mua sắm",      "🛍️", "#E91E63", Category.CategoryType.EXPENSE),
            cat("Sức khỏe",     "💊", "#4CAF50", Category.CategoryType.EXPENSE),
            cat("Giải trí",     "🎮", "#9C27B0", Category.CategoryType.EXPENSE),
            cat("Giáo dục",     "📚", "#2196F3", Category.CategoryType.EXPENSE),
            cat("Hóa đơn",      "💡", "#FFC107", Category.CategoryType.EXPENSE),
            cat("Nhà ở",        "🏠", "#795548", Category.CategoryType.EXPENSE),
            cat("Khác",         "📦", "#607D8B", Category.CategoryType.EXPENSE),
            // ── INCOME ───────────────────────────────────────
            cat("Lương",        "💰", "#4CAF50", Category.CategoryType.INCOME),
            cat("Freelance",    "🎨", "#4659A6", Category.CategoryType.INCOME),
            cat("Đầu tư",       "📈", "#FFC107", Category.CategoryType.INCOME),
            cat("Quà tặng",     "🎁", "#E91E63", Category.CategoryType.INCOME),
            cat("Thu khác",     "✨", "#607D8B", Category.CategoryType.INCOME),
            // ── DEBT ─────────────────────────────────────────
            cat("Cho vay",      "🤝", "#7C4DFF", Category.CategoryType.DEBT),
            cat("Đi vay",       "🏦", "#FF6D00", Category.CategoryType.DEBT),
            cat("Trả nợ",       "💸", "#D50000", Category.CategoryType.DEBT),
            cat("Thu nợ",       "💹", "#00897B", Category.CategoryType.DEBT),
            // ── TRANSFER ─────────────────────────────────────
            cat("Chuyển tiền",  "↔️",  "#2196F3", Category.CategoryType.BOTH)
        );

        defaults.forEach(c -> c.setIsDefault(true));
        categoryRepository.saveAll(defaults);
        log.info(">> Đã tạo {} danh mục mặc định", defaults.size());
    }

    private Category cat(String name, String icon, String color, Category.CategoryType type) {
        return Category.builder()
                .name(name).icon(icon).colorHex(color).type(type).isDefault(true).build();
    }
}
