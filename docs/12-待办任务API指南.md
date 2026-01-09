# 待办任务 API 使用指南

## 🚀 快速开始

待办任务系统已经完成后端开发，包含完整的任务管理和提醒功能。

---

## 📋 API 接口列表

### 1. 任务管理接口

#### 1.1 创建任务
```
POST /api/todos
```

**请求体示例**：
```json
{
  "title": "完成项目文档",
  "description": "编写用户手册和API文档",
  "priority": "HIGH",
  "dueDate": "2025-11-30T18:00:00",
  "isImportant": true,
  "tagIds": [1, 2],
  "reminders": [
    {
      "remindAt": "2025-11-30T17:00:00",
      "reminderType": "ONCE",
      "advanceMinutes": 60,
      "notifyMethod": "IN_APP"
    }
  ]
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "完成项目文档",
    "description": "编写用户手册和API文档",
    "status": "TODO",
    "priority": "HIGH",
    "dueDate": "2025-11-30T18:00:00",
    "isImportant": true,
    "createdAt": "2025-11-28T10:00:00",
    "updatedAt": "2025-11-28T10:00:00"
  }
}
```

---

#### 1.2 更新任务
```
PUT /api/todos/{id}
```

**请求体示例**：
```json
{
  "title": "完成项目文档（已更新）",
  "status": "IN_PROGRESS",
  "priority": "HIGH"
}
```

---

#### 1.3 删除任务
```
DELETE /api/todos/{id}
```

---

#### 1.4 标记为完成
```
POST /api/todos/{id}/complete
```

**说明**：自动设置状态为 COMPLETED 并记录完成时间，同时取消所有未发送的提醒。

---

#### 1.5 获取任务详情
```
GET /api/todos/{id}
```

---

### 2. 任务查询接口

#### 2.1 获取所有任务
```
GET /api/todos/all
```

返回当前用户的所有任务，按创建时间倒序。

---

#### 2.2 获取今日待办
```
GET /api/todos/today
```

返回截止日期在今天的待办任务（状态为 TODO 或 IN_PROGRESS）。

---

#### 2.3 获取本周待办
```
GET /api/todos/week
```

返回截止日期在本周的待办任务。

---

#### 2.4 获取逾期任务
```
GET /api/todos/overdue
```

返回截止日期已过期但尚未完成的任务。

---

#### 2.5 获取重要任务
```
GET /api/todos/important
```

返回所有标记为重要的任务（isImportant = true）。

---

#### 2.6 获取已完成任务
```
GET /api/todos/completed
```

返回所有已完成的任务，按完成时间倒序。

---

### 3. 任务搜索接口

#### 3.1 搜索任务
```
POST /api/todos/search
```

**请求体示例**：
```json
{
  "keyword": "文档",
  "statuses": ["TODO", "IN_PROGRESS"],
  "priorities": ["HIGH", "MEDIUM"],
  "onlyImportant": false,
  "onlyOverdue": false,
  "tagIds": [1, 2]
}
```

**说明**：
- `keyword`: 搜索标题和描述
- `statuses`: 筛选状态（TODO, IN_PROGRESS, COMPLETED, ARCHIVED）
- `priorities`: 筛选优先级（HIGH, MEDIUM, LOW）
- `onlyImportant`: 只显示重要任务
- `onlyOverdue`: 只显示逾期任务
- `tagIds`: 按标签筛选

---

## 🔔 提醒功能说明

### 提醒类型

| 类型 | 说明 | 示例 |
|------|------|------|
| **ONCE** | 单次提醒 | 在指定时间提醒一次 |
| **DAILY** | 每日提醒 | 每天同一时间提醒 |
| **WEEKLY** | 每周提醒 | 每周同一天同一时间提醒 |
| **MONTHLY** | 每月提醒 | 每月同一日同一时间提醒 |

### 通知方式

| 方式 | 说明 | 状态 |
|------|------|------|
| **IN_APP** | 站内通知 | ✅ 已实现（日志记录） |
| **EMAIL** | 邮件通知 | 🔧 预留接口 |
| **WEBSOCKET** | 实时推送 | 🔧 预留接口 |

### 提醒示例

```json
{
  "reminders": [
    {
      "remindAt": "2025-11-30T17:00:00",
      "reminderType": "ONCE",
      "advanceMinutes": 60,
      "notifyMethod": "IN_APP"
    },
    {
      "remindAt": "2025-11-30T09:00:00",
      "reminderType": "DAILY",
      "advanceMinutes": 0,
      "notifyMethod": "IN_APP"
    }
  ]
}
```

**说明**：
- 第一个提醒：在截止时间前 60 分钟（17:00）提醒一次
- 第二个提醒：每天早上 9:00 提醒

---

## 🎯 使用场景示例

### 场景 1：创建一个简单的待办任务

```bash
POST /api/todos
Content-Type: application/json
Authorization: Bearer {token}

{
  "title": "买菜",
  "dueDate": "2025-11-28T18:00:00"
}
```

---

### 场景 2：创建重要任务并设置提醒

```bash
POST /api/todos

{
  "title": "明天的项目汇报",
  "description": "准备PPT和演讲稿",
  "priority": "HIGH",
  "dueDate": "2025-11-29T14:00:00",
  "isImportant": true,
  "reminders": [
    {
      "remindAt": "2025-11-29T09:00:00",
      "reminderType": "ONCE",
      "notifyMethod": "IN_APP"
    },
    {
      "remindAt": "2025-11-29T13:00:00",
      "reminderType": "ONCE",
      "advanceMinutes": 60,
      "notifyMethod": "IN_APP"
    }
  ]
}
```

**说明**：
- 早上 9:00 提醒一次
- 会议前 1 小时（13:00）再提醒一次

---

### 场景 3：每天的重复任务

```bash
POST /api/todos

{
  "title": "每日健身",
  "priority": "MEDIUM",
  "reminders": [
    {
      "remindAt": "2025-11-28T18:00:00",
      "reminderType": "DAILY",
      "notifyMethod": "IN_APP"
    }
  ]
}
```

**说明**：每天下午 6:00 提醒

---

### 场景 4：查看今天要做的事

```bash
GET /api/todos/today
Authorization: Bearer {token}
```

---

### 场景 5：搜索包含"文档"的高优先级任务

```bash
POST /api/todos/search

{
  "keyword": "文档",
  "priorities": ["HIGH"],
  "statuses": ["TODO", "IN_PROGRESS"]
}
```

---

## ⚙️ 定时任务说明

### 提醒扫描机制

系统每分钟自动扫描一次到期的提醒：

1. **扫描频率**：每分钟第 0 秒执行（如 10:00:00, 10:01:00）
2. **扫描条件**：
   - 提醒时间 <= 当前时间
   - 未发送（isSent = false）
   - 已激活（isActive = true）

3. **发送流程**：
   - 构建提醒消息
   - 根据通知方式发送（IN_APP/EMAIL/WEBSOCKET）
   - 标记为已发送
   - 如果是重复提醒，创建下次提醒

### 重复提醒规则

- **DAILY**：创建第二天同一时间的提醒
- **WEEKLY**：创建下周同一天同一时间的提醒
- **MONTHLY**：创建下月同一日同一时间的提醒

---

## 📝 任务状态说明

| 状态 | 说明 | 操作 |
|------|------|------|
| **TODO** | 待办 | 默认状态 |
| **IN_PROGRESS** | 进行中 | 手动设置 |
| **COMPLETED** | 已完成 | 调用 complete 接口或手动设置 |
| **ARCHIVED** | 已归档 | 手动设置 |

---

## 🎨 优先级说明

| 优先级 | 说明 | 建议使用场景 |
|--------|------|-------------|
| **HIGH** | 高 | 紧急重要的任务 |
| **MEDIUM** | 中 | 常规任务 |
| **LOW** | 低 | 不紧急的任务 |

---

## 🔒 权限说明

所有接口都需要 JWT Token 认证：

```
Authorization: Bearer {your_jwt_token}
```

用户只能：
- 查看自己的任务
- 操作自己的任务
- 接收自己的提醒

---

## 📊 数据库表结构

### todos 表
```sql
- id: 任务ID
- title: 标题（必填）
- description: 描述
- status: 状态（TODO/IN_PROGRESS/COMPLETED/ARCHIVED）
- priority: 优先级（HIGH/MEDIUM/LOW）
- due_date: 截止日期
- completed_at: 完成时间
- is_important: 是否重要
- order_index: 排序索引
- user_id: 所属用户
- created_at: 创建时间
- updated_at: 更新时间
```

### todo_reminders 表
```sql
- id: 提醒ID
- todo_id: 关联的任务
- remind_at: 提醒时间
- reminder_type: 提醒类型（ONCE/DAILY/WEEKLY/MONTHLY）
- advance_minutes: 提前多少分钟
- notify_method: 通知方式（IN_APP/EMAIL/WEBSOCKET）
- is_sent: 是否已发送
- sent_at: 发送时间
- is_active: 是否激活
- created_at: 创建时间
```

---

## ⚠️ 注意事项

1. **提醒精度**：定时扫描每分钟执行一次，提醒误差在 1 分钟以内
2. **重复提醒**：重复提醒会自动创建下次提醒，无需手动设置
3. **完成任务**：任务完成后会自动取消所有未发送的提醒
4. **时间格式**：使用 ISO 8601 格式（yyyy-MM-dd'T'HH:mm:ss）

---

## 🚀 下一步

1. **数据库初始化**：应用启动时会自动创建表结构
2. **测试接口**：使用 Postman 或其他工具测试 API
3. **前端对接**：根据此文档开发前端页面
4. **扩展功能**：
   - 实现邮件通知
   - 实现 WebSocket 实时推送
   - 添加子任务功能
   - 添加任务附件

---

## 🎉 总结

待办任务系统后端已完成，包含：
- ✅ 完整的任务 CRUD
- ✅ 多种查询和筛选
- ✅ 灵活的提醒系统
- ✅ 定时任务自动扫描
- ✅ 权限控制
- ✅ 日志记录

现在可以启动应用并开始使用了！
