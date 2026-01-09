# 待办任务完整API文档

## 📚 目录

- [1. 接口概览](#1-接口概览)
- [2. 认证说明](#2-认证说明)
- [3. 任务管理接口](#3-任务管理接口)
- [4. 提醒管理接口](#4-提醒管理接口)
- [5. 通知管理接口](#5-通知管理接口)
- [6. 数据模型](#6-数据模型)
- [7. 错误码说明](#7-错误码说明)
- [8. 使用示例](#8-使用示例)

---

## 1. 接口概览

### 基础信息

| 项目 | 说明 |
|------|------|
| 协议 | HTTP/HTTPS |
| 基础URL | `http://localhost:8080` |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |

### 接口列表

| 分类 | 接口数量 | 说明 |
|------|---------|------|
| 任务管理 | 14个 | 增删改查、状态管理 |
| 提醒管理 | 3个 | 创建、查询、删除提醒 |
| 通知管理 | 7个 | 通知查询、已读管理 |

---

## 2. 认证说明

### JWT Token 认证

所有接口（除注册登录外）都需要在请求头中携带 JWT Token：

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 获取 Token

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "expiresIn": 86400000
  }
}
```

---

## 3. 任务管理接口

### 3.1 创建任务

**接口地址**：`POST /api/todos`

**请求示例**：
```json
{
  "title": "完成项目文档",
  "description": "编写完整的API文档，包括所有接口说明",
  "dueDate": "2025-12-10T18:00:00",
  "priority": "HIGH",
  "isImportant": true,
  "tagIds": [1, 2, 3],
  "reminders": [
    {
      "remindAt": "2025-12-10T17:00:00",
      "reminderType": "ONCE",
      "advanceMinutes": 60,
      "notifyMethod": "IN_APP"
    },
    {
      "remindAt": "2025-12-10T09:00:00",
      "reminderType": "DAILY",
      "notifyMethod": "EMAIL"
    }
  ]
}
```

**请求参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | ✅ | 任务标题（1-255字符）|
| description | String | ❌ | 任务描述（最多5000字符）|
| dueDate | DateTime | ❌ | 截止时间（ISO 8601格式）|
| priority | String | ❌ | 优先级：LOW/MEDIUM/HIGH |
| isImportant | Boolean | ❌ | 是否重要（默认false）|
| tagIds | Long[] | ❌ | 标签ID列表 |
| reminders | Object[] | ❌ | 提醒列表 |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 123,
    "title": "完成项目文档",
    "description": "编写完整的API文档，包括所有接口说明",
    "status": "TODO",
    "priority": "HIGH",
    "isImportant": true,
    "dueDate": "2025-12-10T18:00:00",
    "createdAt": "2025-12-02T17:30:00",
    "updatedAt": "2025-12-02T17:30:00",
    "tags": [
      {"id": 1, "name": "工作"},
      {"id": 2, "name": "文档"}
    ],
    "reminders": [
      {
        "id": 456,
        "remindAt": "2025-12-10T17:00:00",
        "reminderType": "ONCE",
        "notifyMethod": "IN_APP",
        "isSent": false,
        "isActive": true
      }
    ]
  }
}
```

---

### 3.2 更新任务

**接口地址**：`PUT /api/todos/{id}`

**请求示例**：
```json
{
  "title": "完成项目文档（已更新）",
  "description": "添加使用示例章节",
  "status": "IN_PROGRESS",
  "priority": "HIGH"
}
```

**响应**：同创建任务

---

### 3.3 删除任务

**接口地址**：`DELETE /api/todos/{id}`

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

### 3.4 完成任务

**接口地址**：`POST /api/todos/{id}/complete`

**功能说明**：
- 将任务状态设置为 `COMPLETED`
- 自动取消所有未发送的提醒
- 记录完成时间

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 123,
    "status": "COMPLETED",
    "completedAt": "2025-12-02T18:00:00"
  }
}
```

---

### 3.5 获取任务详情

**接口地址**：`GET /api/todos/{id}`

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 123,
    "title": "完成项目文档",
    "description": "编写完整的API文档",
    "status": "TODO",
    "priority": "HIGH",
    "isImportant": true,
    "dueDate": "2025-12-10T18:00:00",
    "completedAt": null,
    "createdAt": "2025-12-02T17:30:00",
    "updatedAt": "2025-12-02T17:30:00",
    "tags": [...],
    "reminders": [...]
  }
}
```

---

### 3.6 获取所有任务

**接口地址**：`GET /api/todos/all`

**查询参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| status | String | 筛选状态：TODO/IN_PROGRESS/COMPLETED/ARCHIVED |
| priority | String | 筛选优先级：LOW/MEDIUM/HIGH |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 123,
      "title": "完成项目文档",
      "status": "TODO",
      "priority": "HIGH",
      "dueDate": "2025-12-10T18:00:00"
    },
    {
      "id": 124,
      "title": "代码审查",
      "status": "IN_PROGRESS",
      "priority": "MEDIUM",
      "dueDate": "2025-12-05T16:00:00"
    }
  ]
}
```

---

### 3.7 获取今日任务

**接口地址**：`GET /api/todos/today`

**说明**：返回今天截止的所有 TODO 和 IN_PROGRESS 状态的任务

**响应**：同获取所有任务

---

### 3.8 获取本周任务

**接口地址**：`GET /api/todos/week`

**说明**：返回本周内截止的所有任务

---

### 3.9 获取逾期任务

**接口地址**：`GET /api/todos/overdue`

**说明**：返回已逾期但未完成的任务

---

### 3.10 获取重要任务

**接口地址**：`GET /api/todos/important`

**说明**：返回标记为重要的所有任务

---

### 3.11 获取已完成任务

**接口地址**：`GET /api/todos/completed`

**说明**：返回所有已完成的任务，按完成时间倒序

---

### 3.12 高级搜索

**接口地址**：`POST /api/todos/search`

**请求示例**：
```json
{
  "keyword": "文档",
  "statuses": ["TODO", "IN_PROGRESS"],
  "priorities": ["HIGH", "MEDIUM"],
  "tagIds": [1, 2],
  "onlyImportant": true,
  "dueDateStart": "2025-12-01T00:00:00",
  "dueDateEnd": "2025-12-31T23:59:59",
  "onlyOverdue": false
}
```

**搜索参数说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| keyword | String | 关键词（搜索标题和描述）|
| statuses | String[] | 状态列表 |
| priorities | String[] | 优先级列表 |
| tagIds | Long[] | 标签ID列表 |
| onlyImportant | Boolean | 只显示重要任务 |
| dueDateStart | DateTime | 截止日期起始 |
| dueDateEnd | DateTime | 截止日期结束 |
| onlyOverdue | Boolean | 只显示逾期任务 |

**响应**：同获取所有任务

---

### 3.13 获取任务统计

**接口地址**：`GET /api/todos/statistics`

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 50,
    "todoCount": 15,
    "inProgressCount": 10,
    "completedCount": 20,
    "archivedCount": 5,
    "overdueCount": 3,
    "todayCount": 5,
    "weekCount": 12,
    "importantCount": 8,
    "completionRate": 40.0
  }
}
```

---

### 3.14 归档任务

**接口地址**：`POST /api/todos/{id}/archive`

**说明**：将已完成的任务归档

---

## 4. 提醒管理接口

### 4.1 添加提醒

**接口地址**：`POST /api/todos/{todoId}/reminders`

**请求示例**：
```json
{
  "remindAt": "2025-12-10T17:00:00",
  "reminderType": "ONCE",
  "advanceMinutes": 60,
  "notifyMethod": "IN_APP"
}
```

**提醒参数说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| remindAt | DateTime | ✅ | 提醒时间 |
| reminderType | String | ✅ | 提醒类型：ONCE/DAILY/WEEKLY/MONTHLY |
| advanceMinutes | Integer | ❌ | 提前提醒分钟数（默认0）|
| notifyMethod | String | ✅ | 通知方式：IN_APP/EMAIL/SMS |

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 789,
    "remindAt": "2025-12-10T17:00:00",
    "reminderType": "ONCE",
    "advanceMinutes": 60,
    "notifyMethod": "IN_APP",
    "isSent": false,
    "isActive": true,
    "createdAt": "2025-12-02T18:00:00"
  }
}
```

---

### 4.2 获取任务的所有提醒

**接口地址**：`GET /api/todos/{todoId}/reminders`

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 789,
      "remindAt": "2025-12-10T17:00:00",
      "reminderType": "ONCE",
      "notifyMethod": "IN_APP",
      "isSent": false,
      "isActive": true
    },
    {
      "id": 790,
      "remindAt": "2025-12-10T09:00:00",
      "reminderType": "DAILY",
      "notifyMethod": "EMAIL",
      "isSent": false,
      "isActive": true
    }
  ]
}
```

---

### 4.3 删除提醒

**接口地址**：`DELETE /api/todos/{todoId}/reminders/{reminderId}`

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

## 5. 通知管理接口

### 5.1 获取所有通知

**接口地址**：`GET /api/notifications`

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1001,
      "title": "任务提醒",
      "content": "任务提醒：完成项目文档\n截止时间：2025-12-10 18:00:00",
      "type": "TODO_REMINDER",
      "relatedId": 123,
      "relatedType": "TODO",
      "isRead": false,
      "createdAt": "2025-12-10T17:00:00"
    }
  ]
}
```

---

### 5.2 获取未读通知

**接口地址**：`GET /api/notifications/unread`

**缓存说明**：✅ Redis 缓存，10分钟过期

---

### 5.3 获取最近通知

**接口地址**：`GET /api/notifications/recent?limit=10`

**查询参数**：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| limit | Integer | 10 | 返回数量 |

**缓存说明**：✅ Redis 缓存，10分钟过期

---

### 5.4 获取未读通知数量

**接口地址**：`GET /api/notifications/unread/count`

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 5
}
```

**缓存说明**：✅ Redis 缓存，5分钟过期（高频查询）

---

### 5.5 标记通知为已读

**接口地址**：`POST /api/notifications/{id}/read`

**响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

**缓存说明**：✅ 自动清除 Redis 缓存

---

### 5.6 标记所有通知为已读

**接口地址**：`POST /api/notifications/read-all`

**缓存说明**：✅ 清除所有 Redis 缓存

---

### 5.7 清理已读通知

**接口地址**：`DELETE /api/notifications/clean`

**说明**：删除30天前的已读通知

**缓存说明**：✅ 清除所有 Redis 缓存

---

## 6. 数据模型

### 6.1 任务（Todo）

```typescript
interface Todo {
  id: number;                    // 任务ID
  title: string;                 // 标题
  description?: string;          // 描述
  status: TodoStatus;            // 状态
  priority: Priority;            // 优先级
  isImportant: boolean;          // 是否重要
  dueDate?: DateTime;            // 截止时间
  completedAt?: DateTime;        // 完成时间
  createdAt: DateTime;           // 创建时间
  updatedAt: DateTime;           // 更新时间
  tags: Tag[];                   // 标签列表
  reminders: Reminder[];         // 提醒列表
}
```

### 6.2 任务状态（TodoStatus）

```typescript
enum TodoStatus {
  TODO = "TODO",               // 待办
  IN_PROGRESS = "IN_PROGRESS", // 进行中
  COMPLETED = "COMPLETED",     // 已完成
  ARCHIVED = "ARCHIVED"        // 已归档
}
```

### 6.3 优先级（Priority）

```typescript
enum Priority {
  LOW = "LOW",          // 低优先级
  MEDIUM = "MEDIUM",    // 中优先级
  HIGH = "HIGH"         // 高优先级
}
```

### 6.4 提醒（Reminder）

```typescript
interface Reminder {
  id: number;                      // 提醒ID
  remindAt: DateTime;              // 提醒时间
  reminderType: ReminderType;      // 提醒类型
  advanceMinutes: number;          // 提前分钟数
  notifyMethod: NotifyMethod;      // 通知方式
  isSent: boolean;                 // 是否已发送
  isActive: boolean;               // 是否激活
  createdAt: DateTime;             // 创建时间
}
```

### 6.5 提醒类型（ReminderType）

```typescript
enum ReminderType {
  ONCE = "ONCE",       // 单次提醒
  DAILY = "DAILY",     // 每日提醒
  WEEKLY = "WEEKLY",   // 每周提醒
  MONTHLY = "MONTHLY"  // 每月提醒
}
```

### 6.6 通知方式（NotifyMethod）

```typescript
enum NotifyMethod {
  IN_APP = "IN_APP",   // 站内通知
  EMAIL = "EMAIL",     // 邮件通知
  SMS = "SMS"          // 短信通知
}
```

### 6.7 通知（Notification）

```typescript
interface Notification {
  id: number;                         // 通知ID
  title: string;                      // 通知标题
  content: string;                    // 通知内容
  type: NotificationType;             // 通知类型
  relatedId?: number;                 // 关联资源ID
  relatedType?: string;               // 关联资源类型
  isRead: boolean;                    // 是否已读
  readAt?: DateTime;                  // 已读时间
  createdAt: DateTime;                // 创建时间
}
```

### 6.8 通知类型（NotificationType）

```typescript
enum NotificationType {
  TODO_REMINDER = "TODO_REMINDER",     // 任务提醒
  TODO_OVERDUE = "TODO_OVERDUE",       // 任务逾期
  SYSTEM_MESSAGE = "SYSTEM_MESSAGE",   // 系统消息
  COMMENT_REPLY = "COMMENT_REPLY",     // 评论回复
  NOTE_SHARE = "NOTE_SHARE"            // 笔记分享
}
```

---

## 7. 错误码说明

### 标准响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 错误码列表

| 错误码 | 说明 | HTTP状态码 |
|--------|------|-----------|
| 200 | 操作成功 | 200 |
| 400 | 请求参数错误 | 400 |
| 401 | 未授权（Token失效或缺失）| 401 |
| 403 | 无权限操作 | 403 |
| 404 | 资源不存在 | 404 |
| 500 | 服务器内部错误 | 500 |

### 错误响应示例

```json
{
  "code": 404,
  "message": "任务不存在: ID = 999",
  "data": null
}
```

```json
{
  "code": 400,
  "message": "任务标题不能为空",
  "data": null
}
```

---

## 8. 使用示例

### 8.1 完整工作流：创建任务并设置提醒

#### 步骤1：登录获取Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

#### 步骤2：创建任务

```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "title": "准备周会PPT",
    "description": "总结本周工作进展",
    "dueDate": "2025-12-06T14:00:00",
    "priority": "HIGH",
    "isImportant": true,
    "reminders": [
      {
        "remindAt": "2025-12-06T13:00:00",
        "reminderType": "ONCE",
        "advanceMinutes": 60,
        "notifyMethod": "IN_APP"
      }
    ]
  }'
```

#### 步骤3：查看今日任务

```bash
curl -X GET http://localhost:8080/api/todos/today \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 步骤4：完成任务

```bash
curl -X POST http://localhost:8080/api/todos/123/complete \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 8.2 高级搜索示例

```bash
curl -X POST http://localhost:8080/api/todos/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "keyword": "文档",
    "statuses": ["TODO", "IN_PROGRESS"],
    "priorities": ["HIGH", "MEDIUM"],
    "onlyImportant": true,
    "dueDateStart": "2025-12-01T00:00:00",
    "dueDateEnd": "2025-12-31T23:59:59"
  }'
```

---

### 8.3 通知管理示例

#### 获取未读通知数量（带缓存）

```bash
curl -X GET http://localhost:8080/api/notifications/unread/count \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 获取最近10条通知（带缓存）

```bash
curl -X GET "http://localhost:8080/api/notifications/recent?limit=10" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 标记通知为已读（自动清除缓存）

```bash
curl -X POST http://localhost:8080/api/notifications/1001/read \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 8.4 WebSocket 实时通知

#### JavaScript 示例

```javascript
// 1. 建立 WebSocket 连接
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// 2. 连接并订阅通知
stompClient.connect({}, function(frame) {
    console.log('已连接: ' + frame);
    
    // 订阅用户通知队列
    stompClient.subscribe('/user/queue/notifications', function(message) {
        const notification = JSON.parse(message.body);
        console.log('收到通知:', notification);
        
        // 显示通知
        showNotification(notification.title, notification.content);
    });
});

// 3. 显示通知函数
function showNotification(title, content) {
    if (Notification.permission === "granted") {
        new Notification(title, {
            body: content,
            icon: '/icon.png'
        });
    }
}
```

---

## 9. 性能优化

### 9.1 缓存策略

| 接口 | 缓存时长 | 说明 |
|------|---------|------|
| 未读通知 | 30分钟 | 写入后10分钟无访问则过期 |
| 未读数量 | 30分钟 | 适合频繁查询 |
| 最近通知 | 30分钟 | 按limit参数分别缓存 |

### 9.2 缓存清除时机

- 创建通知时
- 标记已读时
- 清理旧通知时

### 9.3 数据库优化

- 使用 JOIN FETCH 预加载关联对象
- 添加索引：`user_id`, `status`, `due_date`, `is_read`
- 定时清理30天前的已读通知

---

## 10. 最佳实践

### 10.1 提醒设置建议

```json
{
  "reminders": [
    // 截止前1小时提醒（站内）
    {
      "remindAt": "2025-12-10T17:00:00",
      "reminderType": "ONCE",
      "advanceMinutes": 60,
      "notifyMethod": "IN_APP"
    },
    // 每天早上9点提醒（邮件）
    {
      "remindAt": "2025-12-10T09:00:00",
      "reminderType": "DAILY",
      "notifyMethod": "EMAIL"
    }
  ]
}
```

### 10.2 搜索性能优化

- 使用JPA Specification在数据库层过滤
- 避免加载不必要的关联对象
- 限制返回数量（分页）

### 10.3 权限控制

- 所有接口都验证用户身份
- 只能操作自己的任务和通知
- Token有效期24小时，需定期刷新

---

## 11. 常见问题

### Q1: 如何设置重复提醒？

**A**: 使用 `reminderType` 参数：

```json
{
  "reminderType": "DAILY",    // 每天提醒
  "remindAt": "2025-12-10T09:00:00"
}
```

重复提醒会在发送后自动创建下一次提醒。

---

### Q2: 完成任务后提醒会怎样？

**A**: 调用完成接口后：
- 任务状态变为 `COMPLETED`
- 所有未发送的提醒自动取消（`isActive = false`）
- 不再创建新的重复提醒

---

### Q3: 缓存会影响数据一致性吗？

**A**: 不会。修改操作会自动清除相关缓存：
- 创建通知 → 清除该用户的缓存
- 标记已读 → 清除该用户的缓存
- 只有查询操作使用缓存

---

### Q4: 如何清空所有通知？

**A**: 分两步：
1. 标记所有为已读：`POST /api/notifications/read-all`
2. 清理已读通知：`DELETE /api/notifications/clean`

---

## 12. 更新日志

### v1.0.0 (2025-12-02)
- ✅ 完整的任务CRUD接口
- ✅ 提醒管理功能
- ✅ WebSocket实时通知
- ✅ 通知缓存机制
- ✅ 高级搜索功能
- ✅ 懒加载异常修复

---

## 📞 技术支持

如有问题，请查看：
- **NOTIFICATION_GUIDE.md** - 通知系统详细说明
- **LAZY_LOADING_FIX.md** - 懒加载问题解决
- **FIX_SUMMARY.md** - 所有修复记录

---

**文档版本**: v1.0.0  
**最后更新**: 2025-12-02
