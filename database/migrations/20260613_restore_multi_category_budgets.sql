-- Restore many-to-many budget categories and enforce category overlap per period.
-- A budget may contain many categories, but an active category can belong to only
-- one active budget for the same user, period type and exact date range.

CREATE TABLE IF NOT EXISTS budget_categories (
    budget_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (budget_id, category_id),
    CONSTRAINT fk_budget_categories_budget
        FOREIGN KEY (budget_id) REFERENCES budgets(id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_categories_category
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TRIGGER IF EXISTS trg_budgets_prevent_duplicate_insert;
DROP TRIGGER IF EXISTS trg_budgets_prevent_duplicate_update;
DROP TRIGGER IF EXISTS trg_budget_categories_prevent_duplicate_insert;
DROP TRIGGER IF EXISTS trg_budget_categories_prevent_duplicate_update;

-- Recover the original selections stored in generated budget names.
INSERT IGNORE INTO budget_categories (budget_id, category_id)
SELECT budget.id, category.id
FROM budgets budget
JOIN categories category
  ON category.type = 'EXPENSE'
 AND category.is_deleted = FALSE
 AND (category.user_id IS NULL OR category.user_id = budget.user_id)
 AND FIND_IN_SET(category.name, REPLACE(budget.name, ', ', ',')) > 0;

-- Always preserve the legacy first-category reference as a selected category.
INSERT IGNORE INTO budget_categories (budget_id, category_id)
SELECT id, category_id
FROM budgets
WHERE category_id IS NOT NULL;

UPDATE budgets budget
SET budget.category_id = (
        SELECT MIN(budget_category.category_id)
        FROM budget_categories budget_category
        WHERE budget_category.budget_id = budget.id
    ),
    budget.scope = CASE
        WHEN EXISTS (
            SELECT 1
            FROM budget_categories budget_category
            WHERE budget_category.budget_id = budget.id
        ) THEN 'CATEGORY'
        ELSE 'ALL_CATEGORIES'
    END;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'budget_categories'
       AND index_name = 'idx_budget_categories_category_budget') = 0,
    'CREATE INDEX idx_budget_categories_category_budget ON budget_categories(category_id, budget_id)',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'budgets'
       AND index_name = 'idx_budget_period_active') = 0,
    'CREATE INDEX idx_budget_period_active ON budgets(user_id, period_type, start_date, end_date, active)',
    'SELECT 1'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

-- Prefer the budget covering more categories when cleaning legacy overlaps.
-- For equal-sized budgets, keep the older budget.
DROP TEMPORARY TABLE IF EXISTS candidate_budget_category_counts;
CREATE TEMPORARY TABLE candidate_budget_category_counts AS
SELECT budget.id, COUNT(budget_category.category_id) AS category_count
FROM budgets budget
JOIN budget_categories budget_category ON budget_category.budget_id = budget.id
WHERE budget.active = TRUE
GROUP BY budget.id;

DROP TEMPORARY TABLE IF EXISTS winner_budget_category_counts;
CREATE TEMPORARY TABLE winner_budget_category_counts AS
SELECT *
FROM candidate_budget_category_counts;

DROP TEMPORARY TABLE IF EXISTS duplicate_budgets_to_deactivate;
CREATE TEMPORARY TABLE duplicate_budgets_to_deactivate AS
SELECT DISTINCT candidate.id
FROM budgets candidate
JOIN candidate_budget_category_counts candidate_count ON candidate_count.id = candidate.id
JOIN budget_categories candidate_category ON candidate_category.budget_id = candidate.id
JOIN budget_categories winner_category
  ON winner_category.category_id = candidate_category.category_id
 AND winner_category.budget_id <> candidate.id
JOIN budgets winner ON winner.id = winner_category.budget_id
JOIN winner_budget_category_counts winner_count ON winner_count.id = winner.id
WHERE candidate.active = TRUE
  AND winner.active = TRUE
  AND winner.user_id = candidate.user_id
  AND winner.period_type = candidate.period_type
  AND winner.start_date = candidate.start_date
  AND winner.end_date = candidate.end_date
  AND (
      winner_count.category_count > candidate_count.category_count
      OR (
          winner_count.category_count = candidate_count.category_count
          AND winner.id < candidate.id
      )
  );

CREATE TABLE IF NOT EXISTS budgets_duplicate_backup_20260613 LIKE budgets;

INSERT IGNORE INTO budgets_duplicate_backup_20260613 (
    id, user_id, name, amount, month, year, created_at, updated_at,
    active, period_type, repeat_type, scope, wallet_scope,
    category_id, wallet_id, end_date, start_date
)
SELECT
    budget.id, budget.user_id, budget.name, budget.amount, budget.month, budget.year,
    budget.created_at, budget.updated_at, budget.active, budget.period_type,
    budget.repeat_type, budget.scope, budget.wallet_scope, budget.category_id,
    budget.wallet_id, budget.end_date, budget.start_date
FROM budgets budget
JOIN duplicate_budgets_to_deactivate duplicate_budget
  ON duplicate_budget.id = budget.id;

UPDATE budgets budget
JOIN duplicate_budgets_to_deactivate duplicate_budget
  ON duplicate_budget.id = budget.id
SET budget.active = FALSE;

DELIMITER //

CREATE TRIGGER trg_budget_categories_prevent_duplicate_insert
BEFORE INSERT ON budget_categories
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
        FROM budgets new_budget
        JOIN budgets existing_budget
          ON existing_budget.user_id = new_budget.user_id
         AND existing_budget.period_type = new_budget.period_type
         AND existing_budget.start_date = new_budget.start_date
         AND existing_budget.end_date = new_budget.end_date
         AND existing_budget.active = TRUE
         AND existing_budget.id <> new_budget.id
        JOIN budget_categories existing_category
          ON existing_category.budget_id = existing_budget.id
         AND existing_category.category_id = NEW.category_id
        WHERE new_budget.id = NEW.budget_id
          AND new_budget.active = TRUE
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Danh muc nay da co ngan sach trong cung ky';
    END IF;
END//

CREATE TRIGGER trg_budget_categories_prevent_duplicate_update
BEFORE UPDATE ON budget_categories
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
        FROM budgets new_budget
        JOIN budgets existing_budget
          ON existing_budget.user_id = new_budget.user_id
         AND existing_budget.period_type = new_budget.period_type
         AND existing_budget.start_date = new_budget.start_date
         AND existing_budget.end_date = new_budget.end_date
         AND existing_budget.active = TRUE
         AND existing_budget.id <> new_budget.id
        JOIN budget_categories existing_category
          ON existing_category.budget_id = existing_budget.id
         AND existing_category.category_id = NEW.category_id
        WHERE new_budget.id = NEW.budget_id
          AND new_budget.active = TRUE
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Danh muc nay da co ngan sach trong cung ky';
    END IF;
END//

CREATE TRIGGER trg_budgets_prevent_duplicate_insert
BEFORE INSERT ON budgets
FOR EACH ROW
BEGIN
    IF NEW.active = TRUE
       AND NEW.scope = 'ALL_CATEGORIES'
       AND EXISTS (
           SELECT 1
           FROM budgets existing_budget
           WHERE existing_budget.active = TRUE
             AND existing_budget.user_id = NEW.user_id
             AND existing_budget.period_type = NEW.period_type
             AND existing_budget.start_date = NEW.start_date
             AND existing_budget.end_date = NEW.end_date
       )
    THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ky nay da co ngan sach';
    END IF;
END//

CREATE TRIGGER trg_budgets_prevent_duplicate_update
BEFORE UPDATE ON budgets
FOR EACH ROW
BEGIN
    IF NEW.active = TRUE
       AND (
           (
               NEW.scope = 'ALL_CATEGORIES'
               AND EXISTS (
                   SELECT 1
                   FROM budgets existing_budget
                   WHERE existing_budget.id <> OLD.id
                     AND existing_budget.active = TRUE
                     AND existing_budget.user_id = NEW.user_id
                     AND existing_budget.period_type = NEW.period_type
                     AND existing_budget.start_date = NEW.start_date
                     AND existing_budget.end_date = NEW.end_date
               )
           )
           OR (
               NEW.scope = 'CATEGORY'
               AND EXISTS (
                   SELECT 1
                   FROM budget_categories selected_category
                   JOIN budget_categories existing_category
                     ON existing_category.category_id = selected_category.category_id
                    AND existing_category.budget_id <> OLD.id
                   JOIN budgets existing_budget
                     ON existing_budget.id = existing_category.budget_id
                    AND existing_budget.active = TRUE
                    AND existing_budget.user_id = NEW.user_id
                    AND existing_budget.period_type = NEW.period_type
                    AND existing_budget.start_date = NEW.start_date
                    AND existing_budget.end_date = NEW.end_date
                   WHERE selected_category.budget_id = OLD.id
               )
           )
       )
    THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Danh muc nay da co ngan sach trong cung ky';
    END IF;
END//

DELIMITER ;
