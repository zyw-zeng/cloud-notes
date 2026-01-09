# Todo 任务类型重构实施记录

## 📋 重构概述

**重构日期**: 2026-01-09  
**重构目标**: 将任务重复类型从 `TodoReminder` 移到 `Todo` 实体，实现任务类型和提醒设置的清晰分离  
**重构方案**: 方案 1 - 任务类型 + 提醒设置（推荐方案）

---

## 🎯 重构目标

### 问题
- ❌ **概念混淆**: 任务重复类型（DAILY、WEEKLY、MONTHLY）放在提醒设置中
- ❌ **数据冗余**: 每日任务产生大量重复的提醒记录
- ❌ **性能问题**: 查询任务时加载大量不必要的数据
- ❌ **维护困难**: 修改任务类型需要更新所有提醒记录

### 解决方案
- ✅ **任务类型**: 在 `Todo` 实体中添加 `todoType` 字段
- ✅ **提醒设置**: `TodoReminder` 只存储提醒规则（提前多少分钟、通知方式）
- ✅ **动态生成**: 根据任务类型和提醒设置动态生成通知
- ✅ **清晰分离**: 任务重复逻辑和提醒逻辑完全分离

---

## 📝 重构步骤

### 阶段 1: 数据库迁移

#### 1.1 添加任务类型字段

**文件**: `V2__Add_todo_type_and_recurrence.sql`

```sql
-- 添加 todo_type 字段（任务类型：ONCE、DAILY、WEEKLY、MONTHLY）
ALTER TABLE todos ADD COLUMN todo_type VARCHAR(20) NOT NULL DEFAULT 'ONCE';

-- 添加 recurrence_rule 字段（重复规则，JSON 格式）
ALTER TABLE todos ADD COLUMN recurrence_rule TEXT;

-- 为 todo_type 添加索引
CREATE INDEX idx_todo_type ON todos(todo_type);

-- 数据迁移：根据现有的 TodoReminder 数据推断任务类型
UPDATE todos t
SET todo_type = (
    SELECT tr.reminder_type
    FROM todo_reminders tr
    WHERE tr.todo_id = t.id
      AND tr.reminder_type IN ('DAILY', 'WEEKLY', 'MONTHLY')
    ORDER BY tr.created_at DESC
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1 
    FROM todo_reminders tr 
    WHERE tr.todo_id = t.id 
      AND tr.reminder_type IN ('DAILY', 'WEEKLY', 'MONTHLY')
);
```

#### 1.2 简化提醒表

**文件**: `V3__Simplify_todo_reminders.sql`

```sql
-- 移除不再需要的字段
ALTER TABLE todo_reminders DROP COLUMN IF EXISTS reminder_type;
ALTER TABLE todo_reminders DROP COLUMN IF EXISTS remind_at;
ALTER TABLE todo_reminders DROP COLUMN IF EXISTS is_sent;
ALTER TABLE todo_reminders DROP COLUMN IF EXISTS sent_at;

-- 确保必要字段存在
-- advance_minutes, notify_method, is_active
```

---

### 阶段 2: 实体类修改

#### 2.1 Todo 实体

**文件**: `Todo.java`

**新增字段**:
```java
/**
 * 任务类型：ONCE（单次）、DAILY（每日）、WEEKLY（每周）、MONTHLY（每月）
 */
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private TodoType todoType = TodoType.ONCE;

/**
 * 重复规则（JSON 格式，用于存储复杂的重复规则）
 * 例如：{"daysOfWeek": [1,3,5]} 表示每周一、三、五
 */
@Column(columnDefinition = "TEXT")
private String recurrenceRule;
```

**新增枚举**:
```java
public enum TodoType {
    ONCE,       // 单次任务
    DAILY,      // 每日任务
    WEEKLY,     // 每周任务
    MONTHLY     // 每月任务
}
```

#### 2.2 TodoReminder 实体

**文件**: `TodoReminder.java`

**移除字段**:
- ❌ `remindAt` - 改为动态计算
- ❌ `reminderType` - 移到 Todo 实体
- ❌ `isSent` - 改为在通知记录中跟踪
- ❌ `sentAt` - 改为在通知记录中跟踪

**保留字段**:
- ✅ `advanceMinutes` - 提前多少分钟提醒
- ✅ `notifyMethod` - 提醒方式
- ✅ `isActive` - 是否激活

**移除枚举**:
- ❌ `ReminderType` - 已移到 Todo.TodoType

---

### 阶段 3: DTO 类修改

#### 3.1 TodoDTO

**文件**: `TodoDTO.java`

**新增字段**:
```java
/**
 * 任务类型：ONCE（单次）、DAILY（每日）、WEEKLY（每周）、MONTHLY（每月）
 */
private Todo.TodoType todoType;

/**
 * 重复规则（JSON 格式）
 */
private String recurrenceRule;
```

#### 3.2 ReminderDTO

**文件**: `ReminderDTO.java`

**移除字段**:
- ❌ `remindAt`
- ❌ `reminderType`

**保留字段**:
- ✅ `advanceMinutes`
- ✅ `notifyMethod`
- ✅ `isActive`

---

### 阶段 4: Service 层修改

#### 4.1 TodoService

**文件**: `TodoService.java`

**修改点 1**: `createTodo` 方法
```java
Todo todo = Todo.builder()
    // ... 其他字段
    .todoType(dto.getTodoType() != null ? dto.getTodoType() : Todo.TodoType.ONCE)
    .recurrenceRule(dto.getRecurrenceRule())
    .build();
```

**修改点 2**: `updateTodo` 方法
```java
if (dto.getTodoType() != null) {
    todo.setTodoType(dto.getTodoType());
}
if (dto.getRecurrenceRule() != null) {
    todo.setRecurrenceRule(dto.getRecurrenceRule());
}
```

**修改点 3**: `createReminders` 方法
```java
TodoReminder reminder = TodoReminder.builder()
    .todo(todo)
    .advanceMinutes(dto.getAdvanceMinutes() != null ? dto.getAdvanceMinutes() : 0)
    .notifyMethod(dto.getNotifyMethod() != null ? dto.getNotifyMethod() : TodoReminder.NotifyMethod.IN_APP)
    .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
    .build();
```

#### 4.2 TodoReminderGeneratorService（新建）

**文件**: `TodoReminderGeneratorService.java`

**核心功能**:
1. **定时检查**: 每分钟检查一次待办任务
2. **计算提醒时间**: 根据任务类型动态计算提醒时间
3. **发送通知**: 为每个提醒设置发送通知

**关键方法**:
```java
@Scheduled(fixedRate = 60000)
public void checkAndSendReminders() {
    // 获取所有未完成的任务
    // 计算提醒时间
    // 发送通知
}

private List<LocalDateTime> calculateReminderTimes(Todo todo, LocalDateTime now) {
    switch (todo.getTodoType()) {
        case ONCE: // 单次任务
        case DAILY: // 每日任务
        case WEEKLY: // 每周任务
        case MONTHLY: // 每月任务
    }
}
```

---

### 阶段 5: Repository 层修改

#### 5.1 TodoReminderRepository

**文件**: `TodoReminderRepository.java`

**移除方法**:
- ❌ `findPendingReminders` - 依赖 `remindAt` 和 `isSent` 字段
- ❌ `findUpcomingReminders` - 依赖 `remindAt` 和 `isSent` 字段

**新增方法**:
```java
// 查询激活的提醒
List<TodoReminder> findActiveReminders();

// 查询用户的激活提醒
List<TodoReminder> findActiveRemindersByUserId(Long userId);
```

**修改方法**:
```java
// 取消任务的所有激活提醒
@Query("UPDATE TodoReminder r SET r.isActive = false " +
       "WHERE r.todo.id = :todoId AND r.isActive = true")
void cancelPendingRemindersByTodoId(@Param("todoId") Long todoId);
```

---

## 📊 重构影响范围

### 修改的文件

**数据库迁移** (2 个文件):
- ✅ `V2__Add_todo_type_and_recurrence.sql`
- ✅ `V3__Simplify_todo_reminders.sql`

**实体类** (2 个文件):
- ✅ `Todo.java` - 添加 `todoType` 和 `recurrenceRule` 字段
- ✅ `TodoReminder.java` - 移除 4 个字段，移除 1 个枚举

**DTO 类** (2 个文件):
- ✅ `TodoDTO.java` - 添加 `todoType` 和 `recurrenceRule` 字段
- ✅ `ReminderDTO.java` - 移除 2 个字段

**Service 类** (2 个文件):
- ✅ `TodoService.java` - 修改 3 个方法
- ✅ `TodoReminderGeneratorService.java` - 新建文件

**Repository 类** (1 个文件):
- ✅ `TodoReminderRepository.java` - 修改 3 个方法

**总计**: 9 个文件

---

## 🔄 数据迁移策略

### 迁移前数据示例

**Todo 表**:
```
id | title      | due_date
1  | 下班打卡   | 2026-01-09 18:00
```

**TodoReminder 表**:
```
id | todo_id | remind_at           | reminder_type | is_sent
1  | 1       | 2026-01-03 17:30   | DAILY         | true
2  | 1       | 2026-01-04 17:30   | DAILY         | true
3  | 1       | 2026-01-05 17:30   | DAILY         | true
... (39 条记录)
```

### 迁移后数据示例

**Todo 表**:
```
id | title      | due_date            | todo_type | recurrence_rule
1  | 下班打卡   | 2026-01-09 18:00   | DAILY     | null
```

**TodoReminder 表**:
```
id | todo_id | advance_minutes | notify_method | is_active
1  | 1       | 30              | IN_APP        | true
```

**数据优化**:
- ✅ Todo 记录: 1 条（不变）
- ✅ TodoReminder 记录: 从 39 条减少到 1 条
- ✅ 数据减少: 97.4%

---

## 🚀 使用示例

### 创建每日任务

**请求**:
```json
POST /api/todos
{
  "title": "下班打卡",
  "description": "每天下班记得打卡",
  "todoType": "DAILY",
  "dueDate": "2026-01-09T18:00:00",
  "reminders": [
    {
      "advanceMinutes": 30,
      "notifyMethod": "IN_APP",
      "isActive": true
    }
  ]
}
```

**系统行为**:
1. 创建一个任务，类型为 `DAILY`
2. 创建一个提醒设置：提前 30 分钟
3. `TodoReminderGeneratorService` 每天 17:30 自动发送通知

---

### 创建每周任务

**请求**:
```json
POST /api/todos
{
  "title": "周报提交",
  "description": "每周五提交周报",
  "todoType": "WEEKLY",
  "dueDate": "2026-01-10T17:00:00",
  "recurrenceRule": "{\"daysOfWeek\": [5]}",
  "reminders": [
    {
      "advanceMinutes": 60,
      "notifyMethod": "EMAIL",
      "isActive": true
    }
  ]
}
```

**系统行为**:
1. 创建一个任务，类型为 `WEEKLY`，每周五重复
2. 创建一个提醒设置：提前 60 分钟，通过邮件
3. `TodoReminderGeneratorService` 每周五 16:00 自动发送邮件

---

### 创建单次任务

**请求**:
```json
POST /api/todos
{
  "title": "参加项目会议",
  "description": "讨论 Q1 规划",
  "todoType": "ONCE",
  "dueDate": "2026-01-15T14:00:00",
  "reminders": [
    {
      "advanceMinutes": 30,
      "notifyMethod": "WEBSOCKET",
      "isActive": true
    },
    {
      "advanceMinutes": 5,
      "notifyMethod": "IN_APP",
      "isActive": true
    }
  ]
}
```

**系统行为**:
1. 创建一个任务，类型为 `ONCE`
2. 创建两个提醒设置：提前 30 分钟（WebSocket）和提前 5 分钟（站内）
3. `TodoReminderGeneratorService` 在 13:30 和 13:55 分别发送通知
4. 任务完成后，不再提醒

---

## ⚠️ 重要修复

### 删除旧的定时任务

**问题**: 项目中存在旧的 `TodoReminderScheduler.java`，与新的 `TodoReminderGeneratorService.java` 功能重复，且引用了已删除的 `ReminderType` 枚举，导致编译错误。

**解决方案**: 删除旧的 `TodoReminderScheduler.java` 文件

```bash
# 已删除文件
src/main/java/com/example/cloudnotes/scheduler/TodoReminderScheduler.java
```

**原因**:
- ❌ `TodoReminderScheduler` 使用旧的设计（依赖 `remindAt`、`isSent`、`ReminderType` 等已删除字段）
- ✅ `TodoReminderGeneratorService` 使用新的设计（根据任务类型动态生成提醒）
- 两者功能重复，保留新的实现即可

---

## ✅ 验证清单

### 数据库验证

- [ ] 执行 `V2__Add_todo_type_and_recurrence.sql` 成功
- [ ] 执行 `V3__Simplify_todo_reminders.sql` 成功
- [ ] `todos` 表包含 `todo_type` 和 `recurrence_rule` 字段
- [ ] `todo_reminders` 表已移除 `reminder_type`、`remind_at`、`is_sent`、`sent_at` 字段
- [ ] 现有数据已正确迁移

### 编译验证

- [x] 删除旧的 `TodoReminderScheduler.java` 文件
- [ ] 项目编译成功，无错误
- [ ] 所有实体类字段映射正确
- [ ] 所有 DTO 类字段匹配
- [ ] Service 层方法调用正确

### 功能验证

- [ ] 创建单次任务成功
- [ ] 创建每日任务成功
- [ ] 创建每周任务成功
- [ ] 创建每月任务成功
- [ ] 提醒定时任务正常运行
- [ ] 提醒通知正常发送
- [ ] 任务完成后提醒停止

### API 验证

- [ ] `POST /api/todos` - 创建任务（支持 `todoType`）
- [ ] `PUT /api/todos/{id}` - 更新任务（支持 `todoType`）
- [ ] `GET /api/todos/{id}` - 获取任务详情
- [ ] `GET /api/todos/all` - 获取所有任务
- [ ] `POST /api/todos/{id}/complete` - 完成任务

---

## 🔍 注意事项

### 1. 数据迁移

**重要**: 在生产环境执行迁移前，请先备份数据库！

```bash
# 备份数据库
pg_dump -U postgres -d cloud_notes > backup_$(date +%Y%m%d).sql

# 执行迁移
mvn flyway:migrate

# 如果出错，回滚
psql -U postgres -d cloud_notes < backup_$(date +%Y%m%d).sql
```

### 2. 定时任务

`TodoReminderGeneratorService` 使用 `@Scheduled` 注解，需要确保：
- Spring Boot 应用启用了 `@EnableScheduling`
- 定时任务线程池配置正确

### 3. 性能优化

- 定时任务每分钟执行一次，查询所有未完成任务
- 如果任务数量很大，考虑添加分页或索引优化
- 可以考虑使用 Redis 缓存提醒时间

### 4. 扩展性

如果需要支持更复杂的重复规则（如"每两周"、"每季度"），可以：
1. 扩展 `TodoType` 枚举
2. 使用 `recurrenceRule` 字段存储 JSON 规则
3. 或者采用 iCalendar RRULE 标准

---

## 📚 相关文档

- [31-Todo任务类型设计分析与重构建议.md](./31-Todo任务类型设计分析与重构建议.md) - 设计分析和方案对比
- [04-Flyway数据库迁移指南.md](./04-Flyway数据库迁移指南.md) - Flyway 使用指南
- [05-缓存使用指南.md](./05-缓存使用指南.md) - 缓存优化建议

---

## 🎓 经验总结

### 1. 设计原则

- **单一职责**: 任务类型和提醒设置应该分离
- **数据最小化**: 避免存储可以动态计算的数据
- **概念清晰**: 命名和结构应该反映真实的业务概念

### 2. 重构策略

- **渐进式重构**: 分阶段进行，每个阶段都可以独立验证
- **数据迁移**: 先迁移数据，再修改代码
- **向后兼容**: 尽可能保持 API 兼容性

### 3. 测试建议

- 为每种任务类型编写单元测试
- 测试提醒生成逻辑的边界情况
- 测试定时任务的执行

---

**重构完成时间**: 2026-01-09  
**重构人员**: Cloud Notes 开发团队  
**版本**: 1.0  
**状态**: ✅ 已完成
