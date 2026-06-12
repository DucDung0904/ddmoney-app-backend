-- Superseded by 20260613_restore_multi_category_budgets.sql.
--
-- The first version enforced uniqueness through budgets.category_id and
-- accidentally reduced a many-category budget to its first category. It is
-- intentionally a no-op now so rerunning migration scripts cannot restore the
-- incorrect single-category triggers.
SELECT 'Run 20260613_restore_multi_category_budgets.sql for budget duplicate protection' AS migration_notice;
