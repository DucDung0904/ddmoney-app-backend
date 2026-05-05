-- ============================================================
--  DDMoney — MySQL Database Schema
--  Chạy script này trong phpMyAdmin hoặc MySQL Workbench
--  Laragon: http://localhost/phpmyadmin
-- ============================================================

-- 1. Tạo database
CREATE DATABASE IF NOT EXISTS ddmoney
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ddmoney;

-- ============================================================
-- 2. BẢNG USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(100) NOT NULL,
    full_name   VARCHAR(100),
    avatar_url  TEXT,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email    (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. BẢNG WALLETS (Ví)
-- ============================================================
CREATE TABLE IF NOT EXISTS wallets (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)   NOT NULL,
    balance     DECIMAL(18,2)  NOT NULL DEFAULT 0.00,
    type        ENUM('CASH','BANK','EWALLET','CREDIT') NOT NULL DEFAULT 'CASH',
    bank_name   VARCHAR(100),
    card_number VARCHAR(20),
    color_hex   VARCHAR(10)    DEFAULT '#4659A6',
    is_active   TINYINT(1)     DEFAULT 1,
    created_at  DATETIME       DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. BẢNG CATEGORIES (Danh mục)
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    icon        VARCHAR(10)  DEFAULT '📦',
    color_hex   VARCHAR(10)  DEFAULT '#4659A6',
    type        ENUM('INCOME','EXPENSE','DEBT','BOTH') NOT NULL,
    is_default  TINYINT(1)   DEFAULT 0,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. BẢNG TRANSACTIONS (Giao dịch)
-- ============================================================
CREATE TABLE IF NOT EXISTS transactions (
    id                    BIGINT         NOT NULL AUTO_INCREMENT,
    title                 VARCHAR(200)   NOT NULL,
    amount                DECIMAL(18,2)  NOT NULL,
    type                  ENUM('INCOME','EXPENSE','TRANSFER','DEBT') NOT NULL,
    date                  DATE           NOT NULL,
    note                  TEXT,
    wallet_id             BIGINT         NOT NULL,
    category_id           BIGINT         NOT NULL,
    transfer_to_wallet_id BIGINT,
    created_at            DATETIME       DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tx_wallet     FOREIGN KEY (wallet_id)             REFERENCES wallets(id)    ON DELETE RESTRICT,
    CONSTRAINT fk_tx_category   FOREIGN KEY (category_id)           REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tx_transfer   FOREIGN KEY (transfer_to_wallet_id) REFERENCES wallets(id)    ON DELETE SET NULL,
    INDEX idx_tx_date       (date),
    INDEX idx_tx_type       (type),
    INDEX idx_tx_wallet     (wallet_id),
    INDEX idx_tx_category   (category_id),
    INDEX idx_tx_month_year (MONTH(date), YEAR(date))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. BẢNG BUDGETS (Ngân sách)
-- ============================================================
CREATE TABLE IF NOT EXISTS budgets (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    category_id BIGINT        NOT NULL,
    amount      DECIMAL(18,2) NOT NULL,
    month       INT           NOT NULL COMMENT '1-12',
    year        INT           NOT NULL COMMENT 'VD: 2026',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_budget_cat_month_year (category_id, month, year),
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 7. SEED DATA — Danh mục mặc định
--    (Spring Boot DataInitializer cũng tự seed nếu bảng trống)
-- ============================================================
INSERT IGNORE INTO categories (name, icon, color_hex, type, is_default) VALUES
-- Chi tiêu (EXPENSE)
('Ăn uống',    '🍜', '#F44336', 'EXPENSE', 1),
('Di chuyển',  '🚗', '#FF9800', 'EXPENSE', 1),
('Mua sắm',    '🛍️', '#E91E63', 'EXPENSE', 1),
('Sức khỏe',   '💊', '#4CAF50', 'EXPENSE', 1),
('Giải trí',   '🎮', '#9C27B0', 'EXPENSE', 1),
('Giáo dục',   '📚', '#2196F3', 'EXPENSE', 1),
('Hóa đơn',    '💡', '#FFC107', 'EXPENSE', 1),
('Nhà ở',      '🏠', '#795548', 'EXPENSE', 1),
('Khác',       '📦', '#607D8B', 'EXPENSE', 1),
-- Thu nhập (INCOME)
('Lương',      '💰', '#4CAF50', 'INCOME',  1),
('Freelance',  '🎨', '#4659A6', 'INCOME',  1),
('Đầu tư',     '📈', '#FFC107', 'INCOME',  1),
('Quà tặng',   '🎁', '#E91E63', 'INCOME',  1),
('Thu khác',   '✨', '#607D8B', 'INCOME',  1),
-- Vay nợ (DEBT)
('Cho vay',    '🤝', '#7C4DFF', 'DEBT',    1),
('Đi vay',     '🏦', '#FF6D00', 'DEBT',    1),
('Trả nợ',     '💸', '#D50000', 'DEBT',    1),
('Thu nợ',     '💹', '#00897B', 'DEBT',    1),
-- Chuyển tiền (BOTH)
('Chuyển tiền','↔️', '#2196F3', 'BOTH',    1);

-- ============================================================
-- 8. SEED DATA — Ví mặc định
-- ============================================================
INSERT IGNORE INTO wallets (name, balance, type, color_hex, is_active) VALUES
('Tiền mặt',  0.00, 'CASH', '#4659A6', 1),
('Ngân hàng', 0.00, 'BANK', '#003CC7', 1);

-- ============================================================
-- 9. Kiểm tra kết quả
-- ============================================================
SELECT 'TABLES:' AS '';
SHOW TABLES;

SELECT CONCAT('Categories: ', COUNT(*), ' rows') AS '' FROM categories;
SELECT CONCAT('Wallets:    ', COUNT(*), ' rows') AS '' FROM wallets;
