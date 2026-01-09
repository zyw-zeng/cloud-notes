-- 简化 todo_reminders 表（MySQL 兼容版本）
-- 移除不再需要的字段，因为任务重复类型已经移到 Todo 实体中

-- 1. 智能删除 reminder_type 字段
SET @sql = 'ALTER TABLE todo_reminders DROP COLUMN reminder_type';
SET @check = (SELECT COUNT(*) FROM information_schema.columns 
             WHERE table_schema = DATABASE() 
               AND table_name = 'todo_reminders' 
               AND column_name = 'reminder_type');
SET @sql = IF(@check > 0, @sql, 'SELECT "reminder_type字段不存在" AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 智能删除 remind_at 字段
SET @sql = 'ALTER TABLE todo_reminders DROP COLUMN remind_at';
SET @check = (SELECT COUNT(*) FROM information_schema.columns 
             WHERE table_schema = DATABASE() 
               AND table_name = 'todo_reminders' 
               AND column_name = 'remind_at');
SET @sql = IF(@check > 0, @sql, 'SELECT "remind_at字段不存在" AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 智能删除 is_sent 字段
SET @sql = 'ALTER TABLE todo_reminders DROP COLUMN is_sent';
SET @check = (SELECT COUNT(*) FROM information_schema.columns 
             WHERE table_schema = DATABASE() 
               AND table_name = 'todo_reminders' 
               AND column_name = 'is_sent');
SET @sql = IF(@check > 0, @sql, 'SELECT "is_sent字段不存在" AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 智能删除 sent_at 字段
SET @sql = 'ALTER TABLE todo_reminders DROP COLUMN sent_at';
SET @check = (SELECT COUNT(*) FROM information_schema.columns 
             WHERE table_schema = DATABASE() 
               AND table_name = 'todo_reminders' 
               AND column_name = 'sent_at');
SET @sql = IF(@check > 0, @sql, 'SELECT "sent_at字段不存在" AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. 智能添加 advance_minutes 字段
SET @sql = 'ALTER TABLE todo_reminders ADD COLUMN advance_minutes INTEGER NOT NULL DEFAULT 0';
SET @check = (SELECT COUNT(*) FROM information_schema.columns 
             WHERE table_schema = DATABASE() 
               AND table_name = 'todo_reminders' 
               AND column_name = 'advance_minutes');
SET @sql = IF(@check = 0, @sql, 'SELECT "advance_minutes字段已存在" AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. 智能添加 notify_method 字段
SET @sql = 'ALTER TABLE todo_reminders ADD COLUMN notify_method VARCHAR(50) NOT NULL DEFAULT \'IN_APP\'';
SET @check = (SELECT COUNT(*) FROM information_schema.columns 
             WHERE table_schema = DATABASE() 
               AND table_name = 'todo_reminders' 
               AND column_name = 'notify_method');
SET @sql = IF(@check = 0, @sql, 'SELECT "notify_method字段已存在" AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7. 智能添加 is_active 字段
SET @sql = 'ALTER TABLE todo_reminders ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true';
SET @check = (SELECT COUNT(*) FROM information_schema.columns 
             WHERE table_schema = DATABASE() 
               AND table_name = 'todo_reminders' 
               AND column_name = 'is_active');
SET @sql = IF(@check = 0, @sql, 'SELECT "is_active字段已存在" AS message');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 8. 添加注释（MySQL 语法）
ALTER TABLE todo_reminders MODIFY COLUMN advance_minutes INTEGER NOT NULL DEFAULT 0 COMMENT '提前多少分钟提醒（0 表示准时提醒）';
ALTER TABLE todo_reminders MODIFY COLUMN notify_method VARCHAR(50) NOT NULL DEFAULT 'IN_APP' COMMENT '提醒方式：IN_APP（站内通知）、EMAIL（邮件）、WEBSOCKET（实时推送）';
ALTER TABLE todo_reminders MODIFY COLUMN is_active BOOLEAN NOT NULL DEFAULT true COMMENT '是否激活（可用于暂停提醒）';
