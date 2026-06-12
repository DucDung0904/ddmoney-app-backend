package com.dung.ddmoney.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String UPSERT_CATEGORY = """
            INSERT INTO categories (
                id, user_id, parent_id, name, icon, color_hex, type,
                is_default, is_editable, is_deletable, is_deleted, sort_order, seed_version
            ) VALUES (?, NULL, ?, ?, ?, ?, ?, TRUE, FALSE, FALSE, FALSE, ?, 2)
            ON DUPLICATE KEY UPDATE
                user_id = NULL,
                parent_id = VALUES(parent_id),
                name = VALUES(name),
                icon = VALUES(icon),
                color_hex = VALUES(color_hex),
                type = VALUES(type),
                is_default = TRUE,
                is_editable = FALSE,
                is_deletable = FALSE,
                is_deleted = FALSE,
                sort_order = VALUES(sort_order),
                seed_version = 2
            """;

    private static final List<CategorySeed> DEFAULT_CATEGORIES = List.of(
            parent(1001, "Ăn uống", "restaurant", "#E24B4A", "EXPENSE", 10),
            child(1002, 1001, "Ăn sáng", "breakfast", "#EB7070", "EXPENSE", 11),
            child(1003, 1001, "Cafe", "coffee", "#8A5606", "EXPENSE", 12),
            child(1004, 1001, "Nhà hàng", "local_dining", "#E24B4A", "EXPENSE", 13),

            parent(1005, "Di chuyển", "directions_car", "#185FA5", "EXPENSE", 20),
            child(1006, 1005, "Xăng xe", "local_gas_station", "#378ADD", "EXPENSE", 21),
            child(1007, 1005, "Taxi / Grab", "local_taxi", "#185FA5", "EXPENSE", 22),
            child(1008, 1005, "Bảo dưỡng xe", "car_repair", "#0C447C", "EXPENSE", 23),

            parent(1009, "Mua sắm", "shopping_bag", "#EF9F27", "EXPENSE", 30),
            child(1010, 1009, "Đồ dùng cá nhân", "shopping_bag", "#F3BC56", "EXPENSE", 31),
            child(1011, 1009, "Đồ gia dụng", "chair", "#EF9F27", "EXPENSE", 32),
            child(1012, 1009, "Quần áo", "checkroom", "#8A5606", "EXPENSE", 33),

            parent(1013, "Hóa đơn", "receipt_long", "#1D9E75", "EXPENSE", 40),
            child(1014, 1013, "Điện", "electric_bolt", "#EF9F27", "EXPENSE", 41),
            child(1015, 1013, "Nước", "water_drop", "#378ADD", "EXPENSE", 42),
            child(1016, 1013, "Internet", "wifi", "#185FA5", "EXPENSE", 43),
            child(1017, 1013, "Điện thoại", "phone_android", "#1D9E75", "EXPENSE", 44),

            parent(1018, "Nhà cửa", "house", "#639922", "EXPENSE", 50),
            child(1019, 1018, "Tiền thuê nhà", "home_work", "#639922", "EXPENSE", 51),
            child(1020, 1018, "Sửa chữa nhà", "home_repair_service", "#3D5E14", "EXPENSE", 52),

            parent(1021, "Giải trí", "sports_esports", "#F3BC56", "EXPENSE", 60),
            child(1022, 1021, "Xem phim", "movie", "#EF9F27", "EXPENSE", 61),
            child(1023, 1021, "Du lịch", "flight_takeoff", "#378ADD", "EXPENSE", 62),
            child(1024, 1021, "Game / App", "sports_esports", "#8A5606", "EXPENSE", 63),

            parent(1025, "Sức khỏe", "local_hospital", "#639922", "EXPENSE", 70),
            child(1026, 1025, "Thuốc", "medication", "#82BB44", "EXPENSE", 71),
            child(1027, 1025, "Khám bệnh", "local_hospital", "#3D5E14", "EXPENSE", 72),

            parent(1028, "Giáo dục", "school", "#1D9E75", "EXPENSE", 80),
            child(1029, 1028, "Học phí", "school", "#1D9E75", "EXPENSE", 81),
            child(1030, 1028, "Sách / Tài liệu", "menu_book", "#0F5C43", "EXPENSE", 82),

            parent(1031, "Thu nhập", "payments", "#639922", "INCOME", 90),
            child(1032, 1031, "Lương", "payments", "#639922", "INCOME", 91),
            child(1033, 1031, "Thưởng", "celebration", "#82BB44", "INCOME", 92),
            child(1034, 1031, "Freelance", "work", "#378ADD", "INCOME", 93),
            child(1035, 1031, "Quà tặng", "card_giftcard", "#F3BC56", "INCOME", 94),
            child(1036, 1031, "Đầu tư", "trending_up", "#EF9F27", "INCOME", 95)
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        DEFAULT_CATEGORIES.forEach(seed -> jdbcTemplate.update(
                UPSERT_CATEGORY,
                seed.id(),
                seed.parentId(),
                seed.name(),
                seed.icon(),
                seed.colorHex(),
                seed.type(),
                seed.sortOrder()
        ));
        log.info("Đã đồng bộ {} danh mục mặc định phiên bản 2", DEFAULT_CATEGORIES.size());
    }

    private static CategorySeed parent(long id, String name, String icon,
                                       String colorHex, String type, int sortOrder) {
        return new CategorySeed(id, null, name, icon, colorHex, type, sortOrder);
    }

    private static CategorySeed child(long id, long parentId, String name, String icon,
                                      String colorHex, String type, int sortOrder) {
        return new CategorySeed(id, parentId, name, icon, colorHex, type, sortOrder);
    }

    private record CategorySeed(long id, Long parentId, String name, String icon,
                                String colorHex, String type, int sortOrder) {
    }
}
