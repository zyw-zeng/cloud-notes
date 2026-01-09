# WebSocket 通知功能测试指南

## 🎯 测试目标

验证 WebSocket 实时通知功能是否正常工作。

---

## 📋 测试准备

### 1. 确保后端运行

```bash
cd f:\web_java\cloud-notes
mvn spring-boot:run
```

### 2. 确保前端运行

```bash
cd [前端项目目录]
npm run dev
```

### 3. 获取用户 Token

登录系统，从浏览器控制台或请求头中获取 JWT Token。

---

## 🧪 测试步骤

### 测试 1：验证 WebSocket 连接

#### 预期日志

打开浏览器控制台（F12），应该看到：

```javascript
🔌 正在连接 WebSocket... /ws
📍 环境: development
📍 完整地址: http://localhost:5190/ws

STOMP: Opening Web Socket...
STOMP: Web Socket Opened...
STOMP: connected to server
✅ WebSocket 连接成功
✅ 已订阅: /user/queue/notifications
```

#### ✅ 成功标准

- 只连接一次（没有重复的"正在连接"日志）
- 没有错误信息
- 显示"连接成功"

#### ❌ 失败现象

```javascript
❌ WebSocket 连接失败
❌ TypeError: There is no underlying STOMP connection
```

**解决方案**：参考 `WEBSOCKET_FIX_GUIDE.md`

---

### 测试 2：验证通知拉取

#### 预期日志

```javascript
📋 加载通知列表: []
🔢 未读数量: 0
```

#### ✅ 成功标准

- API 请求成功（status: 200）
- 返回数据结构正确
- 未读数量显示正确

---

### 测试 3：验证 WebSocket 推送

#### 步骤 1：调用测试接口

```bash
curl -X POST http://localhost:8080/api/notifications/test-push \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 步骤 2：观察前端日志

**预期日志**：

```javascript
📬 收到新通知: {
  id: 123,
  title: "测试通知",
  content: "这是一条测试通知消息，用于验证 WebSocket 推送功能是否正常工作",
  type: "SYSTEM",
  isRead: false,
  createdAt: "2025-12-02T18:17:00"
}

🔢 未读数量: 1
```

#### 步骤 3：检查 UI 更新

- ✅ 通知列表中出现新通知
- ✅ 未读数量从 0 变为 1
- ✅ 浏览器显示桌面通知（如果已授权）

#### ✅ 成功标准

- 前端实时收到通知（无需刷新页面）
- 通知数据完整
- UI 正确更新

#### ❌ 失败现象

- 前端没有收到通知
- 需要刷新页面才能看到
- 收到的数据不完整

**调试方法**：

1. 检查后端日志：
```
【站内通知】已发送通知到用户 zyw
【WebSocket】推送消息到 /user/zyw/queue/notifications
```

2. 检查前端订阅状态：
```javascript
console.log('订阅列表:', webSocketService.subscriptions);
```

---

### 测试 4：验证标记已读

#### 步骤 1：标记通知为已读

```bash
curl -X POST http://localhost:8080/api/notifications/123/read \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 步骤 2：观察前端日志

**预期日志**：

```javascript
✅ 已标记为已读: 123
🔢 未读数量: 0
```

#### ✅ 成功标准

- 通知的 `isRead` 变为 `true`
- 未读数量减 1
- UI 样式更新（通知变灰或标记为已读）

---

### 测试 5：验证组件卸载清理

#### 步骤：切换页面或刷新

**预期日志**：

```javascript
🧹 清理通知中心
✅ 已取消订阅: /user/queue/notifications
```

#### ✅ 成功标准

- 订阅被正确取消
- 没有错误信息
- WebSocket 连接保持（不断开）

---

## 🐛 常见问题排查

### 问题 1：WebSocket 连接失败

**现象**：
```
❌ WebSocket 连接失败: Error during WebSocket handshake
```

**排查步骤**：

1. 检查后端是否运行
```bash
curl http://localhost:8080/api/auth/login
```

2. 检查 WebSocket 端点
```bash
curl http://localhost:8080/ws
# 应该返回 404 或类似错误（因为需要 WebSocket 握手）
```

3. 检查 Security 配置
```java
// SecurityConfig.java
.requestMatchers("/ws/**").permitAll()  // 必须有这一行
```

---

### 问题 2：收不到推送消息

**现象**：调用 test-push 接口后，前端没反应

**排查步骤**：

1. 检查后端日志
```
# 应该有这两条日志
【站内通知】已发送通知到用户 zyw
【WebSocket】推送消息到 /user/zyw/queue/notifications
```

2. 检查订阅状态
```javascript
// 在浏览器控制台执行
console.log('已连接:', webSocketService.connected);
console.log('订阅列表:', webSocketService.subscriptions);
```

3. 检查用户名是否匹配
```javascript
// JWT Token 中的用户名必须与后端推送的用户名一致
console.log('当前用户:', principal.getName());
```

---

### 问题 3：重复订阅

**现象**：
```
SUBSCRIBE id:sub-0
SUBSCRIBE id:sub-1
SUBSCRIBE id:sub-2
```

**解决方案**：参考 `WEBSOCKET_FIX_GUIDE.md` 中的单例模式和 useRef 方案

---

### 问题 4：订阅时报错

**现象**：
```
❌ TypeError: There is no underlying STOMP connection
```

**原因**：在连接完成前就尝试订阅

**解决方案**：
```javascript
// 方案 1：在 connect() 后添加延迟
setTimeout(() => {
  resolve(frame);
}, 100);

// 方案 2：使用 Promise 链
await webSocketService.connect();
webSocketService.subscribe('/user/queue/notifications', callback);
```

---

## 📊 测试结果记录

### 环境信息

- **后端地址**: http://localhost:8080
- **前端地址**: http://localhost:5190
- **用户名**: zyw
- **浏览器**: Chrome / Firefox / Safari

### 测试结果

| 测试项 | 预期结果 | 实际结果 | 状态 |
|--------|---------|---------|------|
| WebSocket 连接 | 连接成功，无重复 | | ⬜ |
| 通知列表拉取 | 返回空数组 | | ⬜ |
| 未读数量拉取 | 返回 0 | | ⬜ |
| WebSocket 推送 | 实时收到通知 | | ⬜ |
| 标记已读 | 未读数-1，样式变化 | | ⬜ |
| 组件卸载清理 | 订阅取消，无错误 | | ⬜ |

**状态说明**：
- ✅ 通过
- ❌ 失败
- ⚠️ 部分通过
- ⬜ 未测试

---

## 🎯 完整测试流程（推荐）

### 1. 准备阶段

```bash
# 启动后端
cd f:\web_java\cloud-notes
mvn spring-boot:run

# 启动前端
cd [前端项目]
npm run dev
```

### 2. 登录系统

访问 http://localhost:5190，登录账号：`zyw`

### 3. 打开浏览器控制台

按 F12，切换到 Console 标签

### 4. 观察连接日志

应该看到：
```
✅ WebSocket 连接成功
✅ 已订阅: /user/queue/notifications
📋 加载通知列表: []
🔢 未读数量: 0
```

### 5. 测试推送

在另一个终端执行：

```bash
# 获取 Token（从浏览器 Application > Local Storage 中复制）
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# 发送测试通知
curl -X POST http://localhost:8080/api/notifications/test-push \
  -H "Authorization: Bearer $TOKEN"
```

### 6. 验证收到通知

前端应该立即显示：
```
📬 收到新通知: {...}
🔢 未读数量: 1
```

### 7. 标记已读

点击通知或调用接口：
```bash
curl -X POST http://localhost:8080/api/notifications/[通知ID]/read \
  -H "Authorization: Bearer $TOKEN"
```

验证未读数量变为 0

### 8. 切换页面

导航到其他页面，验证清理日志：
```
🧹 清理通知中心
✅ 已取消订阅
```

---

## 🔧 调试技巧

### 1. 查看 WebSocket 帧

Chrome DevTools > Network > WS > 选择连接 > Messages

可以看到：
- ⬆️ SUBSCRIBE 订阅消息
- ⬇️ MESSAGE 推送的通知

### 2. 查看后端日志

```bash
tail -f logs/spring.log | grep -E "WebSocket|Notification"
```

应该看到：
```
【站内通知】已发送通知到用户 zyw
【WebSocket】推送消息到 /user/zyw/queue/notifications
```

### 3. 手动测试 REST API

```bash
# 获取通知列表
curl http://localhost:8080/api/notifications/recent?limit=5 \
  -H "Authorization: Bearer $TOKEN"

# 获取未读数量
curl http://localhost:8080/api/notifications/unread/count \
  -H "Authorization: Bearer $TOKEN"
```

---

## ✅ 测试完成标准

全部测试项通过后，应该达到以下效果：

### 前端表现

- ✅ 页面加载时自动连接 WebSocket（只连接一次）
- ✅ 自动订阅通知频道（只订阅一次）
- ✅ 能实时收到后端推送的通知
- ✅ 通知列表实时更新
- ✅ 未读数量实时更新
- ✅ 页面切换时正确清理资源
- ✅ 没有报错日志

### 后端表现

- ✅ WebSocket 连接建立成功
- ✅ 能正确推送通知到指定用户
- ✅ 日志显示推送成功
- ✅ REST API 正常工作

---

## 📝 问题反馈模板

如果测试失败，请提供以下信息：

```
### 环境信息
- 后端版本：
- 前端版本：
- 浏览器：
- 操作系统：

### 问题描述
[描述问题现象]

### 前端日志
[粘贴浏览器控制台日志]

### 后端日志
[粘贴后端日志]

### 复现步骤
1. 
2. 
3. 

### 预期结果
[应该看到什么]

### 实际结果
[实际看到什么]
```

---

## 🎉 测试成功示例

**完美的日志应该是这样的**：

```javascript
// 页面加载
🔌 正在连接 WebSocket... /ws
STOMP: Web Socket Opened...
STOMP: connected to server
✅ WebSocket 连接成功
✅ 已订阅: /user/queue/notifications
📋 加载通知列表: []
🔢 未读数量: 0
✅ 通知中心初始化完成

// 收到推送（调用 test-push 后）
📬 收到新通知: {
  id: 123,
  title: "测试通知",
  content: "这是一条测试通知消息...",
  type: "SYSTEM",
  isRead: false
}
🔢 未读数量: 1

// 标记已读
✅ 已标记为已读: 123
🔢 未读数量: 0

// 页面切换
🧹 清理通知中心
✅ 已取消订阅: /user/queue/notifications
```

**特点**：
- 简洁清晰
- 没有重复日志
- 没有错误信息
- 功能完全正常

---

**更新日期**: 2025-12-02  
**版本**: v1.0.0  
**状态**: ✅ 完成
