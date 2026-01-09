# Redis 缓存配置指南

## 🎯 概述

已将通知消息缓存从 **Caffeine 本地缓存** 升级到 **Redis 分布式缓存**。

---

## 🆚 缓存方案对比

| 特性 | Caffeine（本地缓存） | Redis（分布式缓存） |
|------|---------------------|-------------------|
| **部署方式** | 进程内缓存 | 独立服务 |
| **数据共享** | ❌ 单机不共享 | ✅ 多实例共享 |
| **持久化** | ❌ 重启丢失 | ✅ 支持持久化 |
| **容量** | 受 JVM 内存限制 | 可扩展至 GB 级别 |
| **性能** | 🚀 极快（ns级） | ⚡ 快（μs级） |
| **集群支持** | ❌ 不支持 | ✅ 支持集群 |
| **适用场景** | 单机应用 | 分布式系统 |

---

## 📦 已完成的修改

### 1. 依赖变更

**pom.xml**

```xml
<!-- 新增：Redis 依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- 已注释：Caffeine 依赖（可选） -->
<!--
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
-->
```

---

### 2. 缓存配置

**CacheConfig.java**

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    // 缓存名称常量
    public static final String NOTIFICATION_CACHE = "notifications";
    public static final String NOTIFICATION_COUNT_CACHE = "notificationCount";
    public static final String USER_NOTIFICATIONS_CACHE = "userNotifications";
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认配置：JSON 序列化，30分钟过期
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()
                    )
                )
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()
                    )
                )
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues();
        
        // 为不同缓存设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(USER_NOTIFICATIONS_CACHE, 
            defaultConfig.entryTtl(Duration.ofMinutes(10)));  // 通知列表：10分钟
        cacheConfigurations.put(NOTIFICATION_COUNT_CACHE, 
            defaultConfig.entryTtl(Duration.ofMinutes(5)));   // 未读数量：5分钟
        cacheConfigurations.put(NOTIFICATION_CACHE, 
            defaultConfig.entryTtl(Duration.ofMinutes(30)));  // 单个通知：30分钟
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
```

**特性**：
- ✅ Key 使用 String 序列化
- ✅ Value 使用 JSON 序列化（可读性好）
- ✅ 不同缓存使用不同过期时间
- ✅ 支持事务
- ✅ 不缓存 null 值

---

### 3. Redis 连接配置

**application.yml**

```yaml
spring:
  data:
    redis:
      # Redis 服务器地址
      host: localhost
      # Redis 服务器端口
      port: 6379
      # Redis 数据库索引（0-15）
      database: 0
      # Redis 密码（如果设置了密码）
      # password: your_redis_password
      # 连接超时时间
      timeout: 5000
      # Lettuce 连接池配置
      lettuce:
        pool:
          max-active: 8    # 最大连接数
          max-wait: -1ms   # 最大阻塞等待时间
          max-idle: 8      # 最大空闲连接
          min-idle: 0      # 最小空闲连接
```

---

## 🚀 安装 Redis

### Windows 安装

#### 方法 1：使用 Windows 端口版本

1. 下载 Redis for Windows
```
https://github.com/tporadowski/redis/releases
```

2. 下载 `Redis-x64-5.0.14.1.msi` 并安装

3. 启动 Redis
```powershell
# 作为服务启动（安装时可选）
redis-server

# 或手动启动
cd C:\Program Files\Redis
redis-server.exe redis.windows.conf
```

4. 验证安装
```powershell
redis-cli
127.0.0.1:6379> ping
PONG
```

---

#### 方法 2：使用 Docker

```bash
# 拉取 Redis 镜像
docker pull redis:7-alpine

# 启动 Redis 容器
docker run -d \
  --name redis-cache \
  -p 6379:6379 \
  redis:7-alpine

# 验证运行
docker exec -it redis-cache redis-cli ping
```

---

### Linux/macOS 安装

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install redis-server

# macOS
brew install redis

# 启动 Redis
redis-server

# 验证
redis-cli ping
```

---

## 🧪 测试 Redis 缓存

### 1. 启动 Redis

```bash
redis-server
```

### 2. 启动应用

```bash
cd f:\web_java\cloud-notes
mvn spring-boot:run
```

### 3. 观察日志

应该看到 Redis 连接成功：
```
Lettuce: Successfully connected to localhost:6379
```

---

### 4. 测试缓存写入

调用通知接口：

```bash
# 获取未读数量（第一次，写入缓存）
curl http://localhost:8080/api/notifications/unread/count \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 5. 查看 Redis 中的数据

```bash
# 连接 Redis
redis-cli

# 查看所有 key
127.0.0.1:6379> KEYS *
1) "notificationCount::zyw"

# 查看缓存内容
127.0.0.1:6379> GET "notificationCount::zyw"
"0"

# 查看 TTL（剩余过期时间）
127.0.0.1:6379> TTL "notificationCount::zyw"
(integer) 298  # 还有 298 秒过期
```

---

### 6. 测试缓存失效

```bash
# 标记通知为已读（触发缓存清除）
curl -X POST http://localhost:8080/api/notifications/123/read \
  -H "Authorization: Bearer YOUR_TOKEN"

# 再次查看 Redis
redis-cli
127.0.0.1:6379> KEYS *notificationCount*
(empty array)  # 缓存已被清除
```

---

## 📊 缓存策略说明

### 缓存名称和过期时间

| 缓存名称 | 用途 | 过期时间 | 原因 |
|---------|------|---------|------|
| `userNotifications` | 用户通知列表 | 10 分钟 | 数据量较大，刷新频繁 |
| `notificationCount` | 未读数量 | 5 分钟 | 高频查询，但需及时更新 |
| `notifications` | 单个通知详情 | 30 分钟 | 变化较少，可长期缓存 |

---

### Key 命名规则

Redis 中的 Key 格式：

```
<缓存名称>::<用户名>_<参数>

示例：
notificationCount::zyw
userNotifications::zyw_unread
userNotifications::zyw_recent_10
```

---

### 缓存清除策略

使用 `@CacheEvict` 注解自动清除：

```java
// 标记已读时清除缓存
@CacheEvict(value = {
    CacheConfig.USER_NOTIFICATIONS_CACHE,
    CacheConfig.NOTIFICATION_COUNT_CACHE
}, key = "#username")
public void markAsRead(Long id, String username) {
    // 业务逻辑...
}
```

**触发缓存清除的操作**：
- ✅ 创建新通知
- ✅ 标记通知为已读
- ✅ 标记所有通知为已读
- ✅ 清理旧通知

---

## 🔧 高级配置

### 配置 Redis 密码

**application.yml**

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_secure_password  # 取消注释并设置密码
```

**设置 Redis 密码**：

```bash
# 编辑 redis.conf
requirepass your_secure_password

# 重启 Redis
redis-server redis.conf
```

---

### 配置 Redis 集群

**application.yml**

```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - 192.168.1.100:6379
          - 192.168.1.101:6379
          - 192.168.1.102:6379
        max-redirects: 3
      password: cluster_password
```

---

### 配置 Redis Sentinel（高可用）

**application.yml**

```yaml
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes:
          - 192.168.1.100:26379
          - 192.168.1.101:26379
          - 192.168.1.102:26379
      password: sentinel_password
```

---

## 📈 性能监控

### 查看 Redis 统计信息

```bash
redis-cli

# 查看服务器信息
127.0.0.1:6379> INFO stats

# 查看命中率
127.0.0.1:6379> INFO stats | grep keyspace
keyspace_hits:1000
keyspace_misses:100
# 命中率 = 1000 / (1000 + 100) = 90.9%
```

---

### 监控缓存命中情况

```bash
# 实时监控 Redis 命令
redis-cli MONITOR

# 输出示例
1638360000.123456 [0 127.0.0.1:50000] "GET" "notificationCount::zyw"
1638360001.234567 [0 127.0.0.1:50000] "SET" "notificationCount::zyw" "5" "EX" "300"
```

---

## 🐛 常见问题

### 问题 1：连接失败

**错误**：
```
Unable to connect to Redis; nested exception is 
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

**解决方案**：

1. 检查 Redis 是否运行
```bash
redis-cli ping
```

2. 检查端口占用
```bash
netstat -ano | findstr :6379
```

3. 检查防火墙
```bash
# Windows
netsh advfirewall firewall add rule name="Redis" dir=in action=allow protocol=TCP localport=6379
```

---

### 问题 2：序列化错误

**错误**：
```
Cannot deserialize; nested exception is 
org.springframework.data.redis.serializer.SerializationException
```

**原因**：实体类未实现 `Serializable` 或 JSON 序列化失败

**解决方案**：

确保实体类正确配置：

```java
@Entity
@Data
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 确保所有字段都可以被 Jackson 序列化
    @JsonIgnore  // 忽略不需要序列化的字段
    private transient SomeField field;
}
```

---

### 问题 3：缓存未生效

**现象**：每次都查询数据库，缓存没有命中

**排查步骤**：

1. 检查 `@EnableCaching` 注解
```java
@Configuration
@EnableCaching  // ✅ 必须有这个注解
public class CacheConfig {
```

2. 检查方法是否被代理
```java
// ❌ 错误：在同一个类中调用
public void method1() {
    this.method2();  // 不会触发缓存
}

@Cacheable("cache")
public void method2() {
    // ...
}

// ✅ 正确：通过 Spring 代理调用
@Autowired
private MyService myService;

public void method1() {
    myService.method2();  // 会触发缓存
}
```

3. 检查 Redis 日志
```bash
redis-cli MONITOR
```

---

### 问题 4：缓存不过期

**现象**：数据过期时间不生效

**解决方案**：

检查 Redis 配置：

```bash
redis-cli
127.0.0.1:6379> CONFIG GET maxmemory-policy
1) "maxmemory-policy"
2) "noeviction"  # ❌ 不会自动删除过期 key

# 设置为自动删除过期 key
127.0.0.1:6379> CONFIG SET maxmemory-policy allkeys-lru
```

---

## 🔄 切换回 Caffeine

如果需要切换回本地缓存：

### 1. 修改 pom.xml

```xml
<!-- 注释 Redis -->
<!--
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
-->

<!-- 启用 Caffeine -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 2. 恢复 CacheConfig.java

参考 `CACHE_GUIDE.md` 中的 Caffeine 配置。

---

## 📝 最佳实践

### 1. 合理设置过期时间

```java
// ✅ 推荐：根据数据特点设置
- 高频更新的数据：短过期时间（5-10分钟）
- 较稳定的数据：长过期时间（30-60分钟）
- 很少变化的数据：更长过期时间（数小时）

// ❌ 避免：
- 过期时间过长：数据不一致
- 过期时间过短：缓存命中率低
```

---

### 2. 使用合适的 Key

```java
// ✅ 推荐：清晰的命名
"user:123:notifications"
"notification:count:zyw"

// ❌ 避免：
"n:u:123"  // 难以理解
"key123"   // 无语义
```

---

### 3. 避免缓存穿透

```java
// 对于不存在的数据，缓存一个空值
@Cacheable(value = "users", unless = "#result == null")
public User getUser(Long id) {
    return userRepository.findById(id).orElse(null);
}
```

---

### 4. 监控缓存性能

定期检查：
- ✅ 缓存命中率（>80% 为佳）
- ✅ 内存使用情况
- ✅ 过期 Key 数量
- ✅ 慢查询日志

---

## 🎯 总结

### 已完成

- ✅ 添加 Redis 依赖
- ✅ 配置 RedisCacheManager
- ✅ 设置 Redis 连接信息
- ✅ 配置不同缓存的过期时间
- ✅ 使用 JSON 序列化

### 优势

- ✅ 支持分布式部署
- ✅ 数据持久化
- ✅ 多实例数据共享
- ✅ 可扩展性强
- ✅ 支持集群和高可用

### 下一步

1. 安装并启动 Redis
2. 重启应用验证连接
3. 测试缓存功能
4. 监控缓存性能

---

**更新日期**: 2025-12-03  
**版本**: v2.0.0  
**状态**: ✅ 完成
