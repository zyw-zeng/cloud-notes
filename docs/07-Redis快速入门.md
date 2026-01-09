# Redis 缓存快速开始

## 🚀 5 分钟快速配置

### 步骤 1：安装 Redis（Windows）

#### 选项 A：使用安装包（推荐）

1. 下载 Redis for Windows
```
https://github.com/tporadowski/redis/releases
下载 Redis-x64-5.0.14.1.msi
```

2. 运行安装程序，默认安装到 `C:\Program Files\Redis`

3. 安装完成后，Redis 会自动作为 Windows 服务启动

---

#### 选项 B：使用 Docker

```bash
# 拉取镜像
docker pull redis:7-alpine

# 启动容器
docker run -d --name redis-cache -p 6379:6379 redis:7-alpine
```

---

### 步骤 2：验证 Redis 运行

打开命令行：

```bash
redis-cli ping
```

**预期输出**：
```
PONG
```

✅ 如果看到 `PONG`，说明 Redis 已成功运行！

---

### 步骤 3：重新加载 Maven 依赖

在 IDE 中：

1. 打开 `pom.xml`
2. 右键 → Maven → Reload Project
3. 等待依赖下载完成

或使用命令行：

```bash
cd f:\web_java\cloud-notes
mvn clean install
```

---

### 步骤 4：启动应用

```bash
mvn spring-boot:run
```

**预期日志**：

```
Lettuce: Successfully connected to localhost:6379
INFO  CacheConfig - Redis 缓存管理器已启动
INFO  CloudNotesApplication - Started in 5.234 seconds
```

✅ 看到连接成功，说明 Redis 缓存已启用！

---

### 步骤 5：测试缓存功能

#### 1. 获取通知（写入缓存）

```bash
curl http://localhost:8080/api/notifications/unread/count \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**响应**：
```json
{
  "code": 200,
  "data": 0
}
```

---

#### 2. 查看 Redis 中的数据

```bash
redis-cli

127.0.0.1:6379> KEYS *
1) "notificationCount::zyw"

127.0.0.1:6379> GET "notificationCount::zyw"
"0"

127.0.0.1:6379> TTL "notificationCount::zyw"
(integer) 295  # 还有 295 秒过期（5分钟）
```

---

#### 3. 再次请求（命中缓存）

```bash
curl http://localhost:8080/api/notifications/unread/count \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**后端日志**：
```
# 第一次：查询数据库
【缓存】查询数据库 - 用户 zyw 的未读通知数量

# 第二次：没有日志（命中缓存）
```

✅ 缓存生效！

---

## 🎯 配置说明

### 当前配置（application.yml）

```yaml
spring:
  data:
    redis:
      host: localhost      # Redis 地址
      port: 6379          # Redis 端口
      database: 0         # 数据库索引
      timeout: 5000       # 连接超时（毫秒）
```

### 缓存过期时间

| 缓存 | 过期时间 | 说明 |
|------|---------|------|
| 通知列表 | 10 分钟 | 数据量大，刷新频繁 |
| 未读数量 | 5 分钟 | 高频查询，需及时更新 |
| 单个通知 | 30 分钟 | 变化少，可长期缓存 |

---

## 🔧 常见问题

### Q1: 启动时报错 "Unable to connect to Redis"

**原因**：Redis 未启动

**解决**：

```bash
# 检查 Redis 服务
sc query Redis

# 启动 Redis 服务
sc start Redis

# 或手动启动
cd "C:\Program Files\Redis"
redis-server.exe
```

---

### Q2: 缓存不生效

**排查步骤**：

1. 检查 Redis 连接
```bash
redis-cli ping
```

2. 查看应用日志
```
Lettuce: Successfully connected to localhost:6379
```

3. 监控 Redis 命令
```bash
redis-cli MONITOR
```

---

### Q3: 如何清空所有缓存？

```bash
redis-cli
127.0.0.1:6379> FLUSHDB
OK
```

⚠️ **注意**：会删除当前数据库的所有数据！

---

## 📊 验证缓存效果

### 方法 1：查看日志

**首次查询**：
```
【缓存】查询数据库 - 用户 zyw 的未读通知数量
```

**再次查询**：
```
（无日志，命中缓存）
```

---

### 方法 2：查看 Redis

```bash
redis-cli

# 查看所有缓存 key
127.0.0.1:6379> KEYS *

# 查看缓存内容
127.0.0.1:6379> GET "notificationCount::zyw"

# 查看剩余过期时间
127.0.0.1:6379> TTL "notificationCount::zyw"
```

---

### 方法 3：对比响应时间

```bash
# 首次查询（查数据库）
time curl http://localhost:8080/api/notifications/unread/count \
  -H "Authorization: Bearer YOUR_TOKEN"
# 响应时间: ~50ms

# 再次查询（读缓存）
time curl http://localhost:8080/api/notifications/unread/count \
  -H "Authorization: Bearer YOUR_TOKEN"
# 响应时间: ~5ms
```

⚡ **性能提升 10 倍！**

---

## 🎉 完成！

现在您的通知系统已经使用 Redis 缓存了！

### ✅ 实现的功能

- ✅ 自动缓存通知列表
- ✅ 自动缓存未读数量
- ✅ 写操作自动清除缓存
- ✅ 缓存自动过期
- ✅ 支持分布式部署

### 📖 了解更多

- **详细配置**：查看 [`REDIS_CACHE_GUIDE.md`](REDIS_CACHE_GUIDE.md)
- **高级功能**：集群、哨兵、持久化配置
- **性能监控**：缓存命中率、慢查询分析

---

## 🆘 需要帮助？

### 问题反馈模板

```
### 环境信息
- OS: Windows 11
- Redis 版本: 5.0.14
- Java 版本: 17
- Spring Boot 版本: 3.x

### 问题描述
[描述问题]

### 错误日志
[粘贴错误日志]

### 已尝试的解决方案
1. 
2. 
```

---

**更新日期**: 2025-12-03  
**版本**: v1.0.0  
**状态**: ✅ 完成
