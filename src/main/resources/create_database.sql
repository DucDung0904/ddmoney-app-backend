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
    google_id   VARCHAR(100),
    provider    VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email    (email),
    UNIQUE KEY uk_google_id (google_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. BẢNG WALLETS (Ví)
-- ============================================================
CREATE TABLE IF NOT EXISTS wallets (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    user_id             BIGINT         NOT NULL,
    name                VARCHAR(100)   NOT NULL,
    balance             DECIMAL(18,2)  NOT NULL DEFAULT 0.00,
    type                VARCHAR(20)    NOT NULL DEFAULT 'CASH', -- CASH, BANK, EWALLET, CREDIT_CARD, SAVINGS, INVESTMENT
    bank_name           VARCHAR(100),
    card_number         VARCHAR(20),
    color_hex           VARCHAR(10)    DEFAULT '#4659A6',
    icon                VARCHAR(50)    DEFAULT 'wallet',
    currency            VARCHAR(10)    DEFAULT 'VND',
    is_active           TINYINT(1)     DEFAULT 1,
    is_default          TINYINT(1)     DEFAULT 0,
    is_archived         TINYINT(1)     DEFAULT 0,
    is_included_in_total TINYINT(1)     DEFAULT 1,
    sort_order          INT            DEFAULT 0,
    credit_limit        DECIMAL(18,2)  NULL,
    current_debt        DECIMAL(18,2)  NULL,
    billing_day         INT            NULL,
    payment_due_day     INT            NULL,
    created_at          DATETIME       DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_wallets_user_visible (user_id, is_active, is_archived, sort_order),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Wallet migration: keep older databases compatible with the production wallet contract.
SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'user_id') = 0, 'ALTER TABLE wallets ADD COLUMN user_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE wallets MODIFY COLUMN type VARCHAR(20) NOT NULL DEFAULT 'CASH';

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'icon') = 0, 'ALTER TABLE wallets ADD COLUMN icon VARCHAR(50) DEFAULT ''wallet''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'currency') = 0, 'ALTER TABLE wallets ADD COLUMN currency VARCHAR(10) DEFAULT ''VND''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'is_default') = 0, 'ALTER TABLE wallets ADD COLUMN is_default BOOLEAN DEFAULT FALSE', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'is_archived') = 0, 'ALTER TABLE wallets ADD COLUMN is_archived BOOLEAN DEFAULT FALSE', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'is_included_in_total') = 0, 'ALTER TABLE wallets ADD COLUMN is_included_in_total BOOLEAN DEFAULT TRUE', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'sort_order') = 0, 'ALTER TABLE wallets ADD COLUMN sort_order INT DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'credit_limit') = 0, 'ALTER TABLE wallets ADD COLUMN credit_limit DECIMAL(18,2) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'current_debt') = 0, 'ALTER TABLE wallets ADD COLUMN current_debt DECIMAL(18,2) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'billing_day') = 0, 'ALTER TABLE wallets ADD COLUMN billing_day INT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallets' AND COLUMN_NAME = 'payment_due_day') = 0, 'ALTER TABLE wallets ADD COLUMN payment_due_day INT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE wallets SET type = 'CREDIT_CARD' WHERE type = 'CREDIT';

-- ============================================================
-- 4. B?NG CATEGORIES (Danh m?c)
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    icon        VARCHAR(64)  DEFAULT '📦',
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
    user_id               BIGINT         NOT NULL,
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
    CONSTRAINT fk_tx_user       FOREIGN KEY (user_id)               REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_tx_wallet     FOREIGN KEY (wallet_id)             REFERENCES wallets(id)    ON DELETE RESTRICT,
    CONSTRAINT fk_tx_category   FOREIGN KEY (category_id)           REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tx_transfer   FOREIGN KEY (transfer_to_wallet_id) REFERENCES wallets(id)    ON DELETE SET NULL,
    INDEX idx_tx_user       (user_id),
    INDEX idx_tx_date       (date),
    INDEX idx_tx_type       (type),
    INDEX idx_tx_wallet     (wallet_id),
    INDEX idx_tx_category   (category_id),
    INDEX idx_tx_month_year (MONTH(date), YEAR(date))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Transaction migration: ensure transaction ownership exists for budget calculation/security.
SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transactions' AND COLUMN_NAME = 'user_id') = 0, 'ALTER TABLE transactions ADD COLUMN user_id BIGINT NULL AFTER id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transactions' AND INDEX_NAME = 'idx_tx_user') = 0, 'CREATE INDEX idx_tx_user ON transactions(user_id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- ============================================================
-- 6. BẢNG BUDGETS (Ngân sách)
-- ============================================================
CREATE TABLE IF NOT EXISTS budgets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    category_id BIGINT NULL,
    wallet_id BIGINT NULL,
    scope VARCHAR(30) NOT NULL DEFAULT 'ALL_CATEGORIES',
    wallet_scope VARCHAR(30) NOT NULL DEFAULT 'ALL_WALLETS',
    period_type VARCHAR(30) NOT NULL DEFAULT 'MONTH',
    repeat_type VARCHAR(30) NOT NULL DEFAULT 'NONE',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    month INT NULL COMMENT 'Legacy compatibility: 1-12',
    year INT NULL COMMENT 'Legacy compatibility: 2026',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    category_key BIGINT GENERATED ALWAYS AS (COALESCE(category_id, -1)) STORED,
    wallet_key BIGINT GENERATED ALWAYS AS (COALESCE(wallet_id, -1)) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_budget_identity (user_id, category_key, wallet_key, start_date, end_date, active),
    INDEX idx_budgets_user_active (user_id, active),
    INDEX idx_budgets_period (user_id, start_date, end_date),
    INDEX idx_budgets_category (category_id),
    INDEX idx_budgets_wallet (wallet_id),
    CONSTRAINT fk_budget_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_budget_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE SET NULL,
    CONSTRAINT chk_budget_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_budget_scope CHECK (scope IN ('CATEGORY', 'ALL_CATEGORIES')),
    CONSTRAINT chk_budget_wallet_scope CHECK (wallet_scope IN ('ONE_WALLET', 'ALL_WALLETS')),
    CONSTRAINT chk_budget_period_type CHECK (period_type IN ('WEEK', 'MONTH', 'QUARTER', 'YEAR', 'CUSTOM')),
    CONSTRAINT chk_budget_repeat_type CHECK (repeat_type IN ('NONE', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    CONSTRAINT chk_budget_date_range CHECK (start_date <= end_date),
    CONSTRAINT chk_budget_category_scope CHECK ((scope = 'CATEGORY' AND category_id IS NOT NULL) OR (scope = 'ALL_CATEGORIES' AND category_id IS NULL)),
    CONSTRAINT chk_budget_wallet_scope_id CHECK ((wallet_scope = 'ONE_WALLET' AND wallet_id IS NOT NULL) OR (wallet_scope = 'ALL_WALLETS' AND wallet_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Budget migration: update older month/year + budget_categories schema safely.
SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'user_id') = 0, 'ALTER TABLE budgets ADD COLUMN user_id BIGINT NULL AFTER id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'name') = 0, 'ALTER TABLE budgets ADD COLUMN name VARCHAR(150) NOT NULL DEFAULT ''Ngân sách'' AFTER user_id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'wallet_id') = 0, 'ALTER TABLE budgets ADD COLUMN wallet_id BIGINT NULL AFTER category_id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'scope') = 0, 'ALTER TABLE budgets ADD COLUMN scope VARCHAR(30) NOT NULL DEFAULT ''ALL_CATEGORIES'' AFTER wallet_id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'wallet_scope') = 0, 'ALTER TABLE budgets ADD COLUMN wallet_scope VARCHAR(30) NOT NULL DEFAULT ''ALL_WALLETS'' AFTER scope', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'period_type') = 0, 'ALTER TABLE budgets ADD COLUMN period_type VARCHAR(30) NOT NULL DEFAULT ''MONTH'' AFTER wallet_scope', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'repeat_type') = 0, 'ALTER TABLE budgets ADD COLUMN repeat_type VARCHAR(30) NOT NULL DEFAULT ''NONE'' AFTER period_type', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'start_date') = 0, 'ALTER TABLE budgets ADD COLUMN start_date DATE NULL AFTER repeat_type', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'end_date') = 0, 'ALTER TABLE budgets ADD COLUMN end_date DATE NULL AFTER start_date', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'active') = 0, 'ALTER TABLE budgets ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE AFTER end_date', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE budgets SET start_date = COALESCE(start_date, STR_TO_DATE(CONCAT(year, '-', LPAD(month, 2, '0'), '-01'), '%Y-%m-%d')) WHERE month IS NOT NULL AND year IS NOT NULL;
UPDATE budgets SET end_date = COALESCE(end_date, LAST_DAY(start_date)) WHERE start_date IS NOT NULL;
UPDATE budgets SET start_date = COALESCE(start_date, CURDATE()), end_date = COALESCE(end_date, CURDATE());
-- Backfill category_id from legacy budget_categories. If one old budget had many categories,
-- keep the first category and use new single-category budget semantics.
SET @ddl = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budget_categories') > 0,
    'UPDATE budgets b SET category_id = COALESCE(category_id, (SELECT MIN(bc.category_id) FROM budget_categories bc WHERE bc.budget_id = b.id))',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE budgets SET name = COALESCE(NULLIF(name, ''), 'Ngân sách'), scope = IF(category_id IS NULL, 'ALL_CATEGORIES', 'CATEGORY'), wallet_scope = IF(wallet_id IS NULL, 'ALL_WALLETS', 'ONE_WALLET');

ALTER TABLE budgets MODIFY COLUMN category_id BIGINT NULL;
ALTER TABLE budgets MODIFY COLUMN month INT NULL;
ALTER TABLE budgets MODIFY COLUMN year INT NULL;
ALTER TABLE budgets MODIFY COLUMN start_date DATE NOT NULL;
ALTER TABLE budgets MODIFY COLUMN end_date DATE NOT NULL;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND INDEX_NAME = 'uk_budget_cat_month_year') > 0, 'DROP INDEX uk_budget_cat_month_year ON budgets', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'category_key') = 0, 'ALTER TABLE budgets ADD COLUMN category_key BIGINT GENERATED ALWAYS AS (COALESCE(category_id, -1)) STORED', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND COLUMN_NAME = 'wallet_key') = 0, 'ALTER TABLE budgets ADD COLUMN wallet_key BIGINT GENERATED ALWAYS AS (COALESCE(wallet_id, -1)) STORED', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'budgets' AND INDEX_NAME = 'uk_budget_identity') = 0, 'CREATE UNIQUE INDEX uk_budget_identity ON budgets(user_id, category_key, wallet_key, start_date, end_date, active)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transactions' AND INDEX_NAME = 'idx_transactions_budget_calc') = 0, 'CREATE INDEX idx_transactions_budget_calc ON transactions(user_id, type, date, category_id, wallet_id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS budget_categories (
    budget_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (budget_id, category_id),
    FOREIGN KEY (budget_id) REFERENCES budgets(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Category migration: allow emoji sequences and icon keys.
ALTER TABLE categories MODIFY COLUMN icon VARCHAR(64) DEFAULT '📦';

-- 7. SEED DATA — Danh mục mặc định
--    (Spring Boot DataInitializer cũng tự seed nếu bảng trống)

SELECT 'TABLES:' AS '';
SHOW TABLES;

SELECT CONCAT('Categories: ', COUNT(*), ' rows') AS '' FROM categories;
SELECT CONCAT('Wallets:    ', COUNT(*), ' rows') AS '' FROM wallets;



