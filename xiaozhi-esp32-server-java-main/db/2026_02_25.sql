-- 删除 sys_device 表的 lastLogin 字段
-- 该字段已废弃，设备最后活跃时间改用 updateTime 字段替代
-- 执行时间：2026-02-25
-- 幂等处理（兼容低版本 MySQL）：列不存在时不报错
SET @last_login_column_exists = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = 'xiaozhi'
      AND table_name = 'sys_device'
      AND column_name = 'lastLogin'
);
SET @sql_drop_last_login = IF(
    @last_login_column_exists = 1,
    'ALTER TABLE `xiaozhi`.`sys_device` DROP COLUMN `lastLogin`',
    'SELECT 1'
);
PREPARE stmt_drop_last_login FROM @sql_drop_last_login;
EXECUTE stmt_drop_last_login;
DEALLOCATE PREPARE stmt_drop_last_login;
