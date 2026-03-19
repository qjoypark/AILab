-- Fix username reuse after logical delete:
-- 1) drop unique index on username (if exists)
-- 2) add unique index on (username, deleted) (if missing)

SET @target_table := 'sys_user';
SET @current_schema := DATABASE();

-- Find single-column unique index on username (excluding PRIMARY)
SET @single_username_unique_idx := NULL;
SELECT idx.index_name INTO @single_username_unique_idx
FROM (
    SELECT
        index_name,
        MAX(non_unique) AS non_unique,
        COUNT(*) AS col_count,
        SUM(CASE WHEN column_name = 'username' THEN 1 ELSE 0 END) AS username_cols
    FROM information_schema.statistics
    WHERE table_schema = @current_schema
      AND table_name = @target_table
      AND index_name <> 'PRIMARY'
    GROUP BY index_name
) idx
WHERE idx.non_unique = 0
  AND idx.col_count = 1
  AND idx.username_cols = 1
LIMIT 1;

SET @drop_sql := IF(
    @single_username_unique_idx IS NULL,
    'SELECT ''skip drop single username unique index''',
    CONCAT('ALTER TABLE ', @target_table, ' DROP INDEX `', @single_username_unique_idx, '`')
);
PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

-- Check whether any unique index already covers (username, deleted)
SET @has_username_deleted_unique := (
    SELECT COUNT(*)
    FROM (
        SELECT
            index_name,
            MAX(non_unique) AS non_unique,
            COUNT(*) AS col_count,
            SUM(CASE WHEN column_name = 'username' THEN 1 ELSE 0 END) AS username_cols,
            SUM(CASE WHEN column_name = 'deleted' THEN 1 ELSE 0 END) AS deleted_cols
        FROM information_schema.statistics
        WHERE table_schema = @current_schema
          AND table_name = @target_table
          AND index_name <> 'PRIMARY'
        GROUP BY index_name
    ) idx
    WHERE idx.non_unique = 0
      AND idx.col_count = 2
      AND idx.username_cols = 1
      AND idx.deleted_cols = 1
);

SET @add_sql := IF(
    @has_username_deleted_unique > 0,
    'SELECT ''skip add username+deleted unique index''',
    CONCAT('ALTER TABLE ', @target_table, ' ADD UNIQUE KEY uk_username_deleted (username, deleted)')
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;
