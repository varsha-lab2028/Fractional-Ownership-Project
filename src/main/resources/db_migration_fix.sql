-- ============================================================
-- DB MIGRATION — Run this ONCE if your local MySQL DB was
-- created with 'name' instead of 'investor_name' / 'asset_name'
-- ============================================================
-- Check first: SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
--              WHERE TABLE_NAME='INVESTOR' AND TABLE_SCHEMA=DATABASE();
-- If you see 'name' instead of 'investor_name', run this file.
-- ============================================================

-- Rename INVESTOR.name -> investor_name (if it exists as 'name')
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'INVESTOR'
      AND COLUMN_NAME  = 'name'
);

SET @sql = IF(@col_exists > 0,
    'ALTER TABLE INVESTOR CHANGE COLUMN `name` `investor_name` VARCHAR(100) DEFAULT NULL',
    'SELECT ''INVESTOR.investor_name already correct — skipping'' AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Rename ASSET.name -> asset_name (if it exists as 'name')
SET @col_exists2 = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'ASSET'
      AND COLUMN_NAME  = 'name'
);

SET @sql2 = IF(@col_exists2 > 0,
    'ALTER TABLE ASSET CHANGE COLUMN `name` `asset_name` VARCHAR(100) DEFAULT NULL',
    'SELECT ''ASSET.asset_name already correct — skipping'' AS status'
);
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

SELECT 'Migration complete. Column names now match the application schema.' AS result;
