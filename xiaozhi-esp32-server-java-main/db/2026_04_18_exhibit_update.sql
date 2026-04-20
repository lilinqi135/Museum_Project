-- 智博导览展品表扩表脚本
-- 日期：2026-04-18

USE `xiaozhi`;

SET @column_museum_id_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_exhibit'
      AND column_name = 'museum_id'
);
SET @sql_add_museum_id = IF(
    @column_museum_id_exists = 0,
    'ALTER TABLE `sys_exhibit` ADD COLUMN `museum_id` BIGINT NOT NULL DEFAULT 1 COMMENT ''博物馆ID'' AFTER `id`',
    'SELECT 1'
);
PREPARE stmt_add_museum_id FROM @sql_add_museum_id;
EXECUTE stmt_add_museum_id;
DEALLOCATE PREPARE stmt_add_museum_id;

SET @column_category_id_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_exhibit'
      AND column_name = 'category_id'
);
SET @sql_add_category_id = IF(
    @column_category_id_exists = 0,
    'ALTER TABLE `sys_exhibit` ADD COLUMN `category_id` BIGINT NULL COMMENT ''分类ID'' AFTER `museum_id`',
    'SELECT 1'
);
PREPARE stmt_add_category_id FROM @sql_add_category_id;
EXECUTE stmt_add_category_id;
DEALLOCATE PREPARE stmt_add_category_id;

SET @column_image_url_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_exhibit'
      AND column_name = 'image_url'
);
SET @sql_add_image_url = IF(
    @column_image_url_exists = 0,
    'ALTER TABLE `sys_exhibit` ADD COLUMN `image_url` VARCHAR(500) NULL COMMENT ''展品图片'' AFTER `hall`',
    'SELECT 1'
);
PREPARE stmt_add_image_url FROM @sql_add_image_url;
EXECUTE stmt_add_image_url;
DEALLOCATE PREPARE stmt_add_image_url;

SET @column_tags_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_exhibit'
      AND column_name = 'tags'
);
SET @sql_add_tags = IF(
    @column_tags_exists = 0,
    'ALTER TABLE `sys_exhibit` ADD COLUMN `tags` VARCHAR(500) NULL COMMENT ''标签，逗号分隔'' AFTER `image_url`',
    'SELECT 1'
);
PREPARE stmt_add_tags FROM @sql_add_tags;
EXECUTE stmt_add_tags;
DEALLOCATE PREPARE stmt_add_tags;

SET @column_status_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_exhibit'
      AND column_name = 'status'
);
SET @sql_add_status = IF(
    @column_status_exists = 0,
    'ALTER TABLE `sys_exhibit` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT ''ENABLED'' COMMENT ''状态'' AFTER `tags`',
    'SELECT 1'
);
PREPARE stmt_add_status FROM @sql_add_status;
EXECUTE stmt_add_status;
DEALLOCATE PREPARE stmt_add_status;

SET @column_sort_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_exhibit'
      AND column_name = 'sort'
);
SET @sql_add_sort = IF(
    @column_sort_exists = 0,
    'ALTER TABLE `sys_exhibit` ADD COLUMN `sort` INT NOT NULL DEFAULT 0 COMMENT ''排序值'' AFTER `status`',
    'SELECT 1'
);
PREPARE stmt_add_sort FROM @sql_add_sort;
EXECUTE stmt_add_sort;
DEALLOCATE PREPARE stmt_add_sort;

SET @column_deleted_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_exhibit'
      AND column_name = 'deleted'
);
SET @sql_add_deleted = IF(
    @column_deleted_exists = 0,
    'ALTER TABLE `sys_exhibit` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除'' AFTER `sort`',
    'SELECT 1'
);
PREPARE stmt_add_deleted FROM @sql_add_deleted;
EXECUTE stmt_add_deleted;
DEALLOCATE PREPARE stmt_add_deleted;

SET @idx_exhibit_museum_status_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_exhibit'
      AND index_name = 'idx_exhibit_museum_status'
);
SET @sql_idx_exhibit_museum_status = IF(
    @idx_exhibit_museum_status_exists = 0,
    'CREATE INDEX `idx_exhibit_museum_status` ON `sys_exhibit` (`museum_id`, `status`, `deleted`)',
    'SELECT 1'
);
PREPARE stmt_idx_exhibit_museum_status FROM @sql_idx_exhibit_museum_status;
EXECUTE stmt_idx_exhibit_museum_status;
DEALLOCATE PREPARE stmt_idx_exhibit_museum_status;

SET @idx_exhibit_name_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_exhibit'
      AND index_name = 'idx_exhibit_name'
);
SET @sql_idx_exhibit_name = IF(
    @idx_exhibit_name_exists = 0,
    'CREATE INDEX `idx_exhibit_name` ON `sys_exhibit` (`name`)',
    'SELECT 1'
);
PREPARE stmt_idx_exhibit_name FROM @sql_idx_exhibit_name;
EXECUTE stmt_idx_exhibit_name;
DEALLOCATE PREPARE stmt_idx_exhibit_name;
