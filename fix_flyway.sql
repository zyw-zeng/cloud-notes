-- ============================================
-- 强制清理 Flyway 迁移记录和数据库结构
-- ============================================
-- ⚠️ 重要：请按顺序执行每个步骤，如果某步报错可以忽略

USE cloud_notes_db;

-- 🔍 步骤 1: 查看当前状态
SELECT '=== 当前迁移状态 ===' AS info;
SELECT installed_rank, version, description, success
FROM flyway_schema_history 
ORDER BY installed_rank;

SELECT '=== todos 表结构 ===' AS info;
SHOW COLUMNS FROM todos;

-- 🧹 步骤 2: 强制删除失败的迁移记录
SELECT '=== 删除失败的迁移记录 ===' AS info;
-- 只删除失败的 V3 迁移记录，V2 已经成功
DELETE FROM flyway_schema_history WHERE version = '3' AND success = 0;

-- 🧹 步骤 3: 强制删除索引（可能报错，忽略即可）
SELECT '=== 删除索引 ===' AS info;
SET @sql = 'DROP INDEX idx_todo_type ON todos';
SET @check = (SELECT COUNT(*) FROM information_schema.statistics 
             WHERE table_schema = 'cloud_notes_db' 
               AND table_name = 'todos' 
               AND index_name = 'idx_todo_type');
SET @sql = IF(@check > 0, @sql, 'SELECT "索引不存在" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 🧹 步骤 4: 强制删除列（可能报错，忽略即可）
SELECT '=== 删除列 ===' AS info;

-- 删除 todo_type 列
SET @sql = 'ALTER TABLE todos DROP COLUMN todo_type';
SET @check = (SELECT COUNT(*) FROM information_schema.columns 
             WHERE table_schema = 'cloud_notes_db' 
               AND table_name = 'todos' 
               AND column_name = 'todo_type');
SET @sql = IF(@check > 0, @sql, 'SELECT "todo_type列不存在" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 删除 recurrence_rule 列
SET @sql = 'ALTER TABLE todos DROP COLUMN recurrence_rule';
SET @check = (SELECT COUNT(*) FROM information_schema.columns 
             WHERE table_schema = 'cloud_notes_db' 
               AND table_name = 'todos' 
               AND column_name = 'recurrence_rule');
SET @sql = IF(@check > 0, @sql, 'SELECT "recurrence_rule列不存在" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ✅ 步骤 5: 确认清理成功
SELECT '=== 清理后的状态 ===' AS info;
SELECT installed_rank, version, description, success
FROM flyway_schema_history 
ORDER BY installed_rank;

SELECT '=== 清理后的表结构 ===' AS info;
SHOW COLUMNS FROM todos;

SELECT '=== 清理完成 ===' AS info;
SELECT '现在可以重启 Spring Boot 应用了!' AS message;
