-- Consolidate legacy category seeds into the canonical hierarchy (1001..1036).
-- This migration is idempotent and preserves all transaction/budget references.

CREATE TABLE IF NOT EXISTS categories_legacy_backup_20260612 LIKE categories;

DROP TEMPORARY TABLE IF EXISTS category_legacy_map;
CREATE TEMPORARY TABLE category_legacy_map (
    legacy_id BIGINT PRIMARY KEY,
    canonical_id BIGINT NOT NULL
);

INSERT INTO category_legacy_map (legacy_id, canonical_id) VALUES
    (1, 1001), (2, 1005), (3, 1009), (4, 1021),
    (5, 1032), (6, 1033), (7, 1036),
    (8, 1019), (9, 1028), (10, 1020), (11, 1001),
    (12, 1005), (13, 1021), (14, 1032),
    (15, 1019), (16, 1028), (17, 1020), (18, 1001),
    (19, 1005), (20, 1021), (21, 1032),
    (22, 1019), (23, 1028), (24, 1020), (25, 1001),
    (26, 1005), (27, 1021), (28, 1032),
    (29, 1019), (30, 1028), (31, 1020), (32, 1001),
    (33, 1005), (34, 1021), (35, 1032);

START TRANSACTION;

INSERT IGNORE INTO categories_legacy_backup_20260612
SELECT category.*
FROM categories category
JOIN category_legacy_map migration ON migration.legacy_id = category.id;

UPDATE transactions transaction_record
JOIN category_legacy_map migration ON migration.legacy_id = transaction_record.category_id
SET transaction_record.category_id = migration.canonical_id;

UPDATE budgets budget
JOIN category_legacy_map migration ON migration.legacy_id = budget.category_id
SET budget.category_id = migration.canonical_id;

INSERT IGNORE INTO budget_categories (budget_id, category_id)
SELECT budget_category.budget_id, migration.canonical_id
FROM budget_categories budget_category
JOIN category_legacy_map migration ON migration.legacy_id = budget_category.category_id;

DELETE budget_category
FROM budget_categories budget_category
JOIN category_legacy_map migration ON migration.legacy_id = budget_category.category_id;

UPDATE categories child
JOIN category_legacy_map migration ON migration.legacy_id = child.parent_id
SET child.parent_id = migration.canonical_id;

DELETE category
FROM categories category
JOIN category_legacy_map migration ON migration.legacy_id = category.id;

COMMIT;

DROP TRIGGER IF EXISTS trg_categories_prevent_duplicate_insert;
DROP TRIGGER IF EXISTS trg_categories_prevent_duplicate_update;

DELIMITER //

CREATE TRIGGER trg_categories_prevent_duplicate_insert
BEFORE INSERT ON categories
FOR EACH ROW
BEGIN
    IF NEW.is_deleted = FALSE AND EXISTS (
        SELECT 1
        FROM categories existing
        WHERE existing.id <> NEW.id
          AND existing.is_deleted = FALSE
          AND LOWER(TRIM(existing.name)) = LOWER(TRIM(NEW.name))
          AND existing.type = NEW.type
          AND existing.parent_id <=> NEW.parent_id
          AND (
              (NEW.user_id IS NULL AND existing.user_id IS NULL)
              OR (
                  NEW.user_id IS NOT NULL
                  AND (existing.user_id = NEW.user_id OR existing.is_default = TRUE)
              )
          )
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Danh muc cung ten va cung nhom da ton tai';
    END IF;
END//

CREATE TRIGGER trg_categories_prevent_duplicate_update
BEFORE UPDATE ON categories
FOR EACH ROW
BEGIN
    IF NEW.is_deleted = FALSE AND EXISTS (
        SELECT 1
        FROM categories existing
        WHERE existing.id <> OLD.id
          AND existing.is_deleted = FALSE
          AND LOWER(TRIM(existing.name)) = LOWER(TRIM(NEW.name))
          AND existing.type = NEW.type
          AND existing.parent_id <=> NEW.parent_id
          AND (
              (NEW.user_id IS NULL AND existing.user_id IS NULL)
              OR (
                  NEW.user_id IS NOT NULL
                  AND (existing.user_id = NEW.user_id OR existing.is_default = TRUE)
              )
          )
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Danh muc cung ten va cung nhom da ton tai';
    END IF;
END//

DELIMITER ;
