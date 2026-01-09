# WebSocket 通知系统问题修复指南

## 🐛 发现的问题

根据前端日志分析，发现以下问题：

### 1. WebSocket 订阅时机错误
```
✅ WebSocket 连接成功
重新订阅所有频道...
❌ TypeError: There is no underlying STOMP connection
```

### 2. React 组件重复渲染
```
🔄 初始化通知中心（仅执行一次）  // 实际执行了 3 次
🧹 清理通知中心
🔄 初始化通知中心（仅执行一次）
```

### 3. 订阅管理混乱
```
SUBSCRIBE id:sub-0
SUBSCRIBE id:sub-1
UNSUBSCRIBE id:sub-1
SUBSCRIBE id:sub-2
```

---

## 🎯 修复方案

### 修复 1：WebSocket Service 单例模式

**问题**：每次组件挂载都创建新的 WebSocket 连接

**修复**：使用单例模式，确保全局只有一个 WebSocket 实例

```javascript
// websocket.service.js

class WebSocketService {
  constructor() {
    if (WebSocketService.instance) {
      return WebSocketService.instance;
    }
    
    this.client = null;
    this.connected = false;
    this.subscriptions = new Map();
    
    WebSocketService.instance = this;
  }

  connect() {
    // 如果已经连接，直接返回
    if (this.connected && this.client?.connected) {
      console.log('✅ WebSocket 已经连接，跳过重复连接');
      return Promise.resolve();
    }

    console.log('🔌 正在连接 WebSocket...');
    
    return new Promise((resolve, reject) => {
      const socket = new SockJS('/ws');
      this.client = Stomp.over(socket);
      
      this.client.connect(
        {},
        (frame) => {
          console.log('✅ WebSocket 连接成功', frame);
          this.connected = true;
          resolve(frame);
        },
        (error) => {
          console.error('❌ WebSocket 连接失败', error);
          this.connected = false;
          reject(error);
        }
      );
    });
  }

  subscribe(destination, callback) {
    // 确保已连接
    if (!this.connected || !this.client?.connected) {
      console.warn('⚠️ WebSocket 未连接，等待连接...');
      return this.connect().then(() => this.subscribe(destination, callback));
    }

    // 如果已经订阅，先取消
    if (this.subscriptions.has(destination)) {
      console.log('🔄 检测到重复订阅，先取消旧订阅:', destination);
      this.unsubscribe(destination);
    }

    const subscription = this.client.subscribe(destination, (message) => {
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions.set(destination, subscription);
    console.log('✅ 已订阅:', destination);
    
    return subscription;
  }

  unsubscribe(destination) {
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
      console.log('✅ 已取消订阅:', destination);
    }
  }

  disconnect() {
    // 取消所有订阅
    this.subscriptions.forEach((subscription, destination) => {
      subscription.unsubscribe();
      console.log('🧹 清理订阅:', destination);
    });
    this.subscriptions.clear();

    // 断开连接
    if (this.client) {
      this.client.disconnect(() => {
        console.log('👋 WebSocket 已断开');
        this.connected = false;
      });
    }
  }
}

// 导出单例实例
export default new WebSocketService();
```

---

### 修复 2：优化 React 组件（NotificationCenter.jsx）

**问题**：useEffect 在 React 严格模式下会执行多次

**修复**：使用 useRef 防止重复初始化

```jsx
// NotificationCenter.jsx

import { useState, useEffect, useRef } from 'react';
import webSocketService from '../services/websocket.service';
import notificationAPI from '../api/notification.api';

function NotificationCenter() {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  
  // 使用 ref 标记是否已初始化
  const isInitialized = useRef(false);
  const subscriptionRef = useRef(null);

  useEffect(() => {
    // 防止重复初始化
    if (isInitialized.current) {
      console.log('⏭️ 跳过重复初始化');
      return;
    }

    console.log('🔄 初始化通知中心');
    isInitialized.current = true;

    // 初始化函数
    const initializeNotifications = async () => {
      try {
        // 1. 连接 WebSocket
        await webSocketService.connect();
        
        // 2. 订阅通知频道
        subscriptionRef.current = webSocketService.subscribe(
          '/user/queue/notifications',
          handleNewNotification
        );

        // 3. 加载初始数据
        await loadNotifications();
        await loadUnreadCount();

        console.log('✅ 通知中心初始化完成');
      } catch (error) {
        console.error('❌ 通知中心初始化失败', error);
      }
    };

    initializeNotifications();

    // 清理函数
    return () => {
      console.log('🧹 清理通知中心');
      
      // 只在组件真正卸载时清理
      if (subscriptionRef.current) {
        webSocketService.unsubscribe('/user/queue/notifications');
        subscriptionRef.current = null;
      }
      
      // 不要断开 WebSocket，因为其他组件可能还在使用
      // webSocketService.disconnect(); // ❌ 不要这样做
    };
  }, []); // 空依赖数组

  // 处理新通知
  const handleNewNotification = (notification) => {
    console.log('📬 收到新通知:', notification);
    setNotifications(prev => [notification, ...prev]);
    setUnreadCount(prev => prev + 1);
    
    // 显示浏览器通知
    if (Notification.permission === 'granted') {
      new Notification(notification.title, {
        body: notification.content,
        icon: '/notification-icon.png'
      });
    }
  };

  // 加载通知列表
  const loadNotifications = async () => {
    try {
      const response = await notificationAPI.getRecent(10);
      console.log('📋 加载通知列表:', response.data);
      setNotifications(response.data.data || []);
    } catch (error) {
      console.error('❌ 加载通知失败', error);
    }
  };

  // 加载未读数量
  const loadUnreadCount = async () => {
    try {
      const response = await notificationAPI.getUnreadCount();
      console.log('🔢 未读数量:', response.data.data);
      setUnreadCount(response.data.data || 0);
    } catch (error) {
      console.error('❌ 加载未读数量失败', error);
    }
  };

  // 标记为已读
  const markAsRead = async (id) => {
    try {
      await notificationAPI.markAsRead(id);
      setNotifications(prev => 
        prev.map(n => n.id === id ? { ...n, isRead: true } : n)
      );
      setUnreadCount(prev => Math.max(0, prev - 1));
      console.log('✅ 已标记为已读:', id);
    } catch (error) {
      console.error('❌ 标记失败', error);
    }
  };

  // 标记所有为已读
  const markAllAsRead = async () => {
    try {
      await notificationAPI.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
      setUnreadCount(0);
      console.log('✅ 已全部标记为已读');
    } catch (error) {
      console.error('❌ 全部标记失败', error);
    }
  };

  return (
    <div className="notification-center">
      {/* UI 渲染 */}
    </div>
  );
}

export default NotificationCenter;
```

---

### 修复 3：处理 React 严格模式

**选项 A：在开发环境禁用严格模式（不推荐）**

```jsx
// main.jsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

ReactDOM.createRoot(document.getElementById('root')).render(
  // 移除 <React.StrictMode>
  <App />
);
```

**选项 B：适配严格模式（推荐）**

使用上面的 `useRef` + 单例模式方案，让代码在严格模式下也能正常工作。

---

### 修复 4：订阅时机优化

**问题**：在 `onConnect` 回调中立即订阅可能失败

**修复**：添加延迟或使用 Promise 链

```javascript
// websocket.service.js

connect() {
  return new Promise((resolve, reject) => {
    const socket = new SockJS('/ws');
    this.client = Stomp.over(socket);
    
    this.client.connect(
      {},
      (frame) => {
        console.log('✅ WebSocket 连接成功');
        this.connected = true;
        
        // ✅ 等待连接完全建立后再 resolve
        setTimeout(() => {
          resolve(frame);
        }, 100); // 给 STOMP 100ms 初始化时间
      },
      (error) => {
        console.error('❌ WebSocket 连接失败', error);
        this.connected = false;
        reject(error);
      }
    );
  });
}
```

---

## 🔧 后端需要检查的点

### 1. WebSocket 配置

确认后端 WebSocket 配置正确：

```java
// WebSocketConfig.java

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 配置消息代理
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### 2. Security 配置

确认 WebSocket 端点被允许访问：

```java
// SecurityConfig.java

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/ws/**").permitAll()  // ✅ 允许 WebSocket
            .anyRequest().authenticated()
        );
    return http.build();
}
```

### 3. 测试发送通知

创建一个测试接口验证通知推送：

```java
// NotificationController.java

@PostMapping("/test-push")
public Result<Void> testPush(Principal principal) {
    notificationService.createAndSendNotification(
        principal.getName(),
        "测试通知",
        "这是一条测试通知消息",
        Notification.NotificationType.SYSTEM,
        null,
        null
    );
    return Result.success();
}
```

---

## 🧪 测试流程

### 1. 测试 WebSocket 连接

打开浏览器控制台，应该看到：

```
🔌 正在连接 WebSocket...
STOMP: Web Socket Opened...
STOMP: connected to server
✅ WebSocket 连接成功
```

**预期**：只看到一次连接日志

---

### 2. 测试订阅

```
✅ 已订阅: /user/queue/notifications
```

**预期**：只订阅一次，没有重复的 sub-0, sub-1, sub-2

---

### 3. 测试通知接收

在后端调用测试接口：

```bash
curl -X POST http://localhost:8080/api/notifications/test-push \
  -H "Authorization: Bearer YOUR_TOKEN"
```

前端应该看到：

```
📬 收到新通知: {title: "测试通知", content: "..."}
🔢 未读数量: 1
```

---

### 4. 测试组件卸载

切换页面或刷新，应该看到：

```
🧹 清理通知中心
✅ 已取消订阅: /user/queue/notifications
```

**预期**：没有错误，订阅被正确清理

---

## 📊 问题对比

| 问题 | 修复前 | 修复后 |
|------|--------|--------|
| WebSocket 连接 | 多次连接 | 单例，只连接一次 |
| 订阅管理 | sub-0, sub-1, sub-2... | 只有一个订阅 |
| 组件渲染 | 重复初始化 | 使用 ref 防止 |
| 连接时机 | 立即订阅报错 | 等待连接就绪 |
| 清理逻辑 | 断开所有连接 | 只取消订阅 |

---

## 🎯 最佳实践

### 1. WebSocket 生命周期管理

```
应用启动 → 创建 WebSocket 单例
用户登录 → 连接 WebSocket
组件挂载 → 订阅频道
组件卸载 → 取消订阅（但不断开连接）
用户登出 → 断开 WebSocket
应用关闭 → 清理所有资源
```

### 2. 订阅策略

- ✅ 一个频道只订阅一次
- ✅ 使用 Map 管理订阅
- ✅ 组件卸载时取消订阅
- ❌ 不要在每次渲染时重新订阅

### 3. 错误处理

```javascript
subscribe(destination, callback) {
  try {
    // 检查连接状态
    if (!this.connected) {
      throw new Error('WebSocket 未连接');
    }

    // 检查重复订阅
    if (this.subscriptions.has(destination)) {
      console.warn('已存在订阅，先取消', destination);
      this.unsubscribe(destination);
    }

    // 执行订阅
    const subscription = this.client.subscribe(destination, callback);
    this.subscriptions.set(destination, subscription);
    
    return subscription;
  } catch (error) {
    console.error('订阅失败', error);
    throw error;
  }
}
```

---

## ⚠️ 注意事项

### 1. React 严格模式

React 18 在开发环境会故意执行两次 `useEffect`，这是正常行为：
- 目的：帮助发现副作用问题
- 影响：WebSocket 会被连接两次
- 解决：使用 `useRef` 防止重复初始化

### 2. 生产环境行为

在生产环境（`npm run build`）：
- ✅ React 严格模式不生效
- ✅ useEffect 只执行一次
- ✅ WebSocket 只连接一次

### 3. 浏览器通知权限

```javascript
// 请求通知权限
if (Notification.permission === 'default') {
  Notification.requestPermission();
}
```

---

## 📝 检查清单

### 前端

- [ ] WebSocketService 改为单例模式
- [ ] NotificationCenter 使用 useRef 防止重复初始化
- [ ] 只在组件卸载时取消订阅，不断开连接
- [ ] 订阅前检查是否已订阅
- [ ] 添加连接状态检查
- [ ] 处理订阅失败情况

### 后端

- [ ] WebSocket 配置正确
- [ ] Security 允许 /ws/** 访问
- [ ] 通知推送使用正确的目的地
- [ ] 添加测试接口验证推送
- [ ] 检查日志确认消息发送成功

---

## 🎉 预期效果

修复后，前端日志应该是这样的：

```
🔌 正在连接 WebSocket... /ws
STOMP: Web Socket Opened...
STOMP: connected to server
✅ WebSocket 连接成功
✅ 已订阅: /user/queue/notifications
📋 加载通知列表: []
🔢 未读数量: 0
✅ 通知中心初始化完成

// 收到新通知
📬 收到新通知: {title: "提醒", content: "..."}
🔢 未读数量: 1

// 页面切换
🧹 清理通知中心
✅ 已取消订阅: /user/queue/notifications
```

**特点**：
- ✅ 只连接一次
- ✅ 只订阅一次
- ✅ 没有重复的 sub-0, sub-1, sub-2
- ✅ 没有 "There is no underlying STOMP connection" 错误
- ✅ 能正常接收通知

---

**更新日期**: 2025-12-02  
**版本**: v1.0.0  
**状态**: ✅ 完成
