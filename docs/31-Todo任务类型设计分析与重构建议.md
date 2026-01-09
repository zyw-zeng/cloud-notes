# Todo 任务类型设计分析与重构建议

## 📋 当前设计分析

### 现有设计

#### Todo 实体（任务本身）
```java
@Entity
public class Todo {
    private String title;              // 任务标题
    private String description;        // 任务描述
    private TodoStatus status;         // 状态：TODO、IN_PROGRESS、COMPLETED、ARCHIVED
    private TodoPriority priority;     // 优先级：HIGH、MEDIUM、LOW
    private LocalDateTime dueDate;     // 截止日期
    private Boolean isImportant;       // 是否重要
    // ❌ 缺少：任务重复类型（单次、每日、每周、每月）
}
```

#### TodoReminder 实体（提醒设置）
```java
@Entity
public class TodoReminder {
    private LocalDateTime remindAt;           // 提醒时间
    private ReminderType reminderType;        // ⚠️ 提醒类型：ONCE、DAILY、WEEKLY、MONTHLY
    private Integer advanceMinutes;           // 提前多少分钟提醒
    private NotifyMethod notifyMethod;        // 提醒方式：IN_APP、EMAIL、WEBSOCKET
    private Boolean isSent;                   // 是否已发送
    private Boolean isActive;                 // 是否激活
}
```

---

## 🔍 设计问题分析

### 问题 1：概念混淆

**当前设计的问题：**
- `TodoReminder.reminderType` 包含 `DAILY`、`WEEKLY`、`MONTHLY`
- 这些类型实际上是**任务的重复类型**，而不是**提醒的类型**

**举例说明：**

**场景 1：每日任务**
```
任务：每天下班打卡
- 这是一个每日重复的任务（任务类型）
- 可以设置提前 10 分钟提醒（提醒设置）
```

**场景 2：单次任务**
```
任务：参加项目会议（2026-01-15 14:00）
- 这是一个单次任务（任务类型）
- 可以设置提前 30 分钟提醒（提醒设置）
```

**当前设计的混淆：**
- ❌ 将"每日任务"的概念放在了 `TodoReminder.reminderType` 中
- ❌ 导致"任务重复"和"提醒重复"混为一谈

---

### 问题 2：语义不清晰

**TodoReminder.reminderType 的语义问题：**

| 值 | 当前语义 | 实际应该的语义 |
|----|---------|--------------|
| `ONCE` | 单次提醒 | ✅ 正确：提醒一次 |
| `DAILY` | ❌ 每天提醒？还是每天的任务？ | 应该是：任务每天重复 |
| `WEEKLY` | ❌ 每周提醒？还是每周的任务？ | 应该是：任务每周重复 |
| `MONTHLY` | ❌ 每月提醒？还是每月的任务？ | 应该是：任务每月重复 |

**混淆的后果：**
1. 开发者难以理解：`reminderType = DAILY` 到底是什么意思？
2. 业务逻辑复杂：需要在提醒逻辑中处理任务重复
3. 扩展困难：如果要添加"每两周"、"每季度"等类型，应该加在哪里？

---

### 问题 3：数据冗余和不一致

**当前设计导致的问题：**

**场景：每日任务 "下班打卡"**
```
Todo: 下班打卡
- dueDate: 2026-01-09 18:00

TodoReminder #1:
- remindAt: 2025-12-03 10:30
- reminderType: DAILY
- isSent: true

TodoReminder #2:
- remindAt: 2025-12-04 10:30
- reminderType: DAILY
- isSent: true

... (39 条提醒记录！)
```

**问题：**
1. **数据冗余**：39 条提醒记录，每条都存储相同的 `reminderType = DAILY`
2. **性能问题**：查询任务时需要加载大量提醒记录
3. **维护困难**：如果要修改任务类型，需要更新所有提醒记录
4. **逻辑混乱**：任务的重复规则分散在多个提醒记录中

---

## ✅ 正确的设计方案

### 方案 1：任务类型 + 提醒设置（推荐）

#### 核心思想
- **任务类型**：定义任务如何重复（单次、每日、每周、每月）
- **提醒设置**：定义何时提醒用户（提前多少分钟、通过什么方式）

#### 设计方案

**1. 在 Todo 实体中添加任务类型**

```java
@Entity
public class Todo {
    // ... 现有字段
    
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
    
    /**
     * 任务类型枚举
     */
    public enum TodoType {
        ONCE,       // 单次任务
        DAILY,      // 每日任务
        WEEKLY,     // 每周任务
        MONTHLY     // 每月任务
    }
}
```

**2. 简化 TodoReminder 实体**

```java
@Entity
public class TodoReminder {
    // ... 现有字段
    
    /**
     * 提醒时间（相对于任务截止时间）
     * 例如：提前 30 分钟提醒
     */
    @Column(nullable = false)
    private Integer advanceMinutes = 0;
    
    /**
     * 提醒方式：IN_APP（站内通知）、EMAIL（邮件）、WEBSOCKET（实时推送）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotifyMethod notifyMethod = NotifyMethod.IN_APP;
    
    /**
     * 是否激活（可用于暂停提醒）
     */
    @Column(nullable = false)
    private Boolean isActive = true;
    
    // ❌ 移除：reminderType（不再需要）
    // ❌ 移除：remindAt（改为根据 Todo.dueDate 和 advanceMinutes 计算）
    // ❌ 移除：isSent、sentAt（改为在通知记录中跟踪）
}
```

**3. 提醒生成逻辑**

```java
@Service
public class TodoReminderService {
    
    /**
     * 为任务生成提醒
     */
    public void generateReminders(Todo todo) {
        if (todo.getReminders() == null || todo.getReminders().isEmpty()) {
            return;
        }
        
        // 根据任务类型生成提醒时间
        List<LocalDateTime> reminderTimes = calculateReminderTimes(todo);
        
        // 为每个提醒时间创建通知
        for (LocalDateTime reminderTime : reminderTimes) {
            for (TodoReminder reminder : todo.getReminders()) {
                if (reminder.getIsActive()) {
                    createNotification(todo, reminder, reminderTime);
                }
            }
        }
    }
    
    /**
     * 根据任务类型计算提醒时间
     */
    private List<LocalDateTime> calculateReminderTimes(Todo todo) {
        LocalDateTime dueDate = todo.getDueDate();
        if (dueDate == null) {
            return Collections.emptyList();
        }
        
        List<LocalDateTime> times = new ArrayList<>();
        
        switch (todo.getTodoType()) {
            case ONCE:
                // 单次任务：只提醒一次
                times.add(dueDate);
                break;
                
            case DAILY:
                // 每日任务：每天提醒
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime nextDay = dueDate;
                while (nextDay.isBefore(now.plusDays(7))) {
                    times.add(nextDay);
                    nextDay = nextDay.plusDays(1);
                }
                break;
                
            case WEEKLY:
                // 每周任务：每周提醒
                // ... 类似逻辑
                break;
                
            case MONTHLY:
                // 每月任务：每月提醒
                // ... 类似逻辑
                break;
        }
        
        return times;
    }
}
```

---

### 方案 2：使用 iCalendar 标准（高级方案）

如果需要支持复杂的重复规则，可以使用 iCalendar 的 RRULE 标准。

**示例：**
```java
@Entity
public class Todo {
    /**
     * 重复规则（iCalendar RRULE 格式）
     * 例如：FREQ=DAILY;INTERVAL=1;COUNT=30
     */
    @Column(columnDefinition = "TEXT")
    private String rrule;
}
```

**支持的规则：**
- `FREQ=DAILY;INTERVAL=1` - 每天
- `FREQ=WEEKLY;BYDAY=MO,WE,FR` - 每周一、三、五
- `FREQ=MONTHLY;BYMONTHDAY=1` - 每月 1 号
- `FREQ=YEARLY;BYMONTH=1;BYMONTHDAY=1` - 每年 1 月 1 日

**优点：**
- 标准化，易于扩展
- 支持复杂规则
- 有成熟的库支持（如 iCal4j）

**缺点：**
- 学习成本高
- 实现复杂

---

## 📊 设计对比

| 方面 | 当前设计 | 方案 1（推荐） | 方案 2（高级） |
|------|---------|--------------|--------------|
| **概念清晰度** | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **数据冗余** | ❌ 高 | ✅ 低 | ✅ 低 |
| **查询性能** | ❌ 差 | ✅ 好 | ✅ 好 |
| **扩展性** | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **实现复杂度** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **维护成本** | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |

---

## 🔧 重构步骤

### 阶段 1：数据库迁移

**1. 添加新字段到 Todo 表**
```sql
ALTER TABLE todos ADD COLUMN todo_type VARCHAR(20) DEFAULT 'ONCE';
ALTER TABLE todos ADD COLUMN recurrence_rule TEXT;
```

**2. 数据迁移**
```sql
-- 根据现有的 TodoReminder 数据推断任务类型
UPDATE todos t
SET todo_type = (
    SELECT DISTINCT tr.reminder_type
    FROM todo_reminders tr
    WHERE tr.todo_id = t.id
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1 FROM todo_reminders tr WHERE tr.todo_id = t.id
);
```

**3. 简化 TodoReminder 表**
```sql
-- 移除不再需要的字段
ALTER TABLE todo_reminders DROP COLUMN reminder_type;
ALTER TABLE todo_reminders DROP COLUMN remind_at;
ALTER TABLE todo_reminders DROP COLUMN is_sent;
ALTER TABLE todo_reminders DROP COLUMN sent_at;
```

---

### 阶段 2：代码重构

**1. 修改 Todo 实体**
```java
@Entity
public class Todo {
    // 添加任务类型
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodoType todoType = TodoType.ONCE;
    
    @Column(columnDefinition = "TEXT")
    private String recurrenceRule;
    
    public enum TodoType {
        ONCE, DAILY, WEEKLY, MONTHLY
    }
}
```

**2. 修改 TodoReminder 实体**
```java
@Entity
public class TodoReminder {
    // 移除 reminderType、remindAt、isSent、sentAt
    // 保留 advanceMinutes、notifyMethod、isActive
}
```

**3. 创建提醒生成服务**
```java
@Service
public class TodoReminderGeneratorService {
    public void generateRemindersForTodo(Todo todo) {
        // 根据任务类型生成提醒
    }
}
```

**4. 修改定时任务**
```java
@Scheduled(fixedRate = 60000) // 每分钟执行
public void checkAndSendReminders() {
    // 查询所有需要提醒的任务
    // 根据任务类型和提醒设置发送通知
}
```

---

### 阶段 3：API 调整

**1. 创建任务 API**
```java
@PostMapping
public Result<Todo> createTodo(@RequestBody TodoCreateDTO dto) {
    // dto 包含 todoType 和 recurrenceRule
}
```

**2. 更新任务 API**
```java
@PutMapping("/{id}")
public Result<Todo> updateTodo(@PathVariable Long id, @RequestBody TodoUpdateDTO dto) {
    // 支持修改 todoType 和 recurrenceRule
}
```

**3. 添加提醒 API**
```java
@PostMapping("/{id}/reminders")
public Result<TodoReminder> addReminder(@PathVariable Long id, @RequestBody ReminderDTO dto) {
    // dto 只包含 advanceMinutes 和 notifyMethod
}
```

---

## 🎯 实际应用场景

### 场景 1：每日任务

**创建任务：**
```json
{
  "title": "下班打卡",
  "description": "每天下班记得打卡",
  "todoType": "DAILY",
  "dueDate": "2026-01-09 18:00:00",
  "reminders": [
    {
      "advanceMinutes": 30,
      "notifyMethod": "IN_APP"
    }
  ]
}
```

**系统行为：**
1. 创建一个任务，类型为 `DAILY`
2. 创建一个提醒设置：提前 30 分钟
3. 定时任务每天检查，在 17:30 发送通知
4. 任务完成后，第二天继续提醒

---

### 场景 2：每周任务

**创建任务：**
```json
{
  "title": "周报提交",
  "description": "每周五提交周报",
  "todoType": "WEEKLY",
  "dueDate": "2026-01-10 17:00:00",
  "recurrenceRule": "{\"daysOfWeek\": [5]}",
  "reminders": [
    {
      "advanceMinutes": 60,
      "notifyMethod": "EMAIL"
    }
  ]
}
```

**系统行为：**
1. 创建一个任务，类型为 `WEEKLY`，每周五重复
2. 创建一个提醒设置：提前 60 分钟，通过邮件
3. 定时任务每周五 16:00 发送邮件提醒

---

### 场景 3：单次任务

**创建任务：**
```json
{
  "title": "参加项目会议",
  "description": "讨论 Q1 规划",
  "todoType": "ONCE",
  "dueDate": "2026-01-15 14:00:00",
  "reminders": [
    {
      "advanceMinutes": 30,
      "notifyMethod": "WEBSOCKET"
    },
    {
      "advanceMinutes": 5,
      "notifyMethod": "IN_APP"
    }
  ]
}
```

**系统行为：**
1. 创建一个任务，类型为 `ONCE`
2. 创建两个提醒设置：提前 30 分钟（WebSocket）和提前 5 分钟（站内）
3. 定时任务在 13:30 和 13:55 分别发送通知
4. 任务完成后，不再提醒

---

## 📚 参考资料

### 类似产品的设计

**1. Microsoft To Do**
- 任务类型：单次、每日、每周、每月、每年
- 提醒设置：提前多少时间、通知方式

**2. Todoist**
- 任务类型：使用自然语言（every day、every Monday、every 2 weeks）
- 提醒设置：独立的提醒时间设置

**3. Google Tasks**
- 任务类型：单次、重复（每天、每周、每月、每年、自定义）
- 提醒设置：提醒时间、通知方式

---

## ✅ 总结

### 当前设计的问题
1. ❌ **概念混淆**：任务重复类型放在提醒设置中
2. ❌ **数据冗余**：每日任务产生大量提醒记录
3. ❌ **性能问题**：查询任务时加载大量不必要的数据
4. ❌ **维护困难**：修改任务类型需要更新所有提醒记录

### 推荐方案
1. ✅ **任务类型**：在 `Todo` 实体中添加 `todoType` 字段
2. ✅ **提醒设置**：`TodoReminder` 只存储提醒规则（提前多少分钟、通知方式）
3. ✅ **动态生成**：根据任务类型和提醒设置动态生成通知
4. ✅ **清晰分离**：任务重复逻辑和提醒逻辑完全分离

### 重构优先级
- **高优先级**：添加 `Todo.todoType` 字段
- **中优先级**：简化 `TodoReminder` 实体
- **低优先级**：支持复杂的重复规则（iCalendar RRULE）

---

**文档创建时间**: 2026-01-09  
**作者**: Cloud Notes 开发团队  
**版本**: 1.0
