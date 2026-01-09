-- 添加任务类型和重复规则字段到 todos 表
-- 这是重构的第一步：将任务重复类型从 TodoReminder 移到 Todo

-- 1. 添加 todo_type 字段（任务类型：ONCE、DAILY、WEEKLY、MONTHLY）
ALTER TABLE todos ADD COLUMN todo_type VARCHAR(20) NOT NULL DEFAULT 'ONCE';

-- 2. 添加 recurrence_rule 字段（重复规则，JSON 格式）
ALTER TABLE todos ADD COLUMN recurrence_rule TEXT;

-- 3. 为 todo_type 添加索引（用于查询特定类型的任务）
CREATE INDEX idx_todo_type ON todos(todo_type);

-- 4. 添加注释（MySQL语法）
ALTER TABLE todos MODIFY COLUMN todo_type VARCHAR(20) NOT NULL DEFAULT 'ONCE' COMMENT '任务类型：ONCE（单次）、DAILY（每日）、WEEKLY（每周）、MONTHLY（每月）';
ALTER TABLE todos MODIFY COLUMN recurrence_rule TEXT COMMENT '重复规则（JSON格式），用于存储复杂的重复规则，例如：{"daysOfWeek": [1,3,5]} 表示每周一、三、五';
