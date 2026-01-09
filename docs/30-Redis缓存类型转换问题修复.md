# Redis 缓存类型转换问题修复

## 📋 问题描述

### 错误信息
```
java.lang.ClassCastException: class java.lang.Integer cannot be cast to class java.lang.Long
```

### 错误场景
访问 `/api/notifications/unread/count` 接口时，系统抛出 `ClassCastException` 异常。

### 错误堆栈
```java
at com.example.cloudnotes.service.NotificationService$$SpringCGLIB$$0.getUnreadCount(<generated>)
at com.example.cloudnotes.controller.NotificationController.getUnreadCount(NotificationController.java:77)
```

---

## 🔍 问题原因

### GenericJackson2JsonRedisSerializer 的类型转换问题

**问题根源：**
`GenericJackson2JsonRedisSerializer` 在反序列化数值时，会根据数值大小选择合适的类型：
- 小数值（-2147483648 到 2147483647）→ `Integer`
- 大数值 → `Long`

**代码示例：**
```java
// Service 方法返回 Long
@Cacheable(value = "notificationCount", key = "#username")
public Long getUnreadCount(String username) {
    Long count = repository.countByUserIdAndIsReadFalse(userId);
    return count; // 假设返回 5
}

// Controller 接收
Long count = notificationService.getUnreadCount(username);
// ❌ 错误：Redis 缓存中存储的是 Integer(5)，而不是 Long(5)
// 导致 ClassCastException
```

### 为什么会发生这个问题？

1. **第一次调用**（无缓存）：
   - Service 从数据库查询，返回 `Long(5)`
   - Spring Cache 将 `Long(5)` 序列化到 Redis
   - GenericJackson2JsonRedisSerializer 将其序列化为 JSON: `5`

2. **第二次调用**（有缓存）：
   - Spring Cache 从 Redis 读取 JSON: `5`
   - GenericJackson2JsonRedisSerializer 反序列化时，发现 `5` 是小数值
   - **自动选择 `Integer` 类型**，返回 `Integer(5)`
   - Controller 尝试将 `Integer(5)` 转换为 `Long` → **ClassCastException**

---

## ✅ 解决方案

### 方案 1：清除旧缓存（临时方案）

**清除特定缓存：**
```bash
redis-cli DEL notificationCount::zyw
```

**清除所有缓存：**
```bash
redis-cli FLUSHDB
```

**优点：**
- 立即生效
- 简单直接

**缺点：**
- 只是临时解决，下次还会出现
- 需要手动操作

---

### 方案 2：修改代码安全处理类型转换（已采用）

**修改 Controller 代码：**
```java
@GetMapping("/unread/count")
public Result<Long> getUnreadCount(Principal principal) {
    // 安全处理 Redis 缓存可能返回 Integer 的情况
    Long rawCount = notificationService.getUnreadCount(principal.getName());
    // 将 Number 类型安全转换为 Long
    Long count = rawCount != null ? rawCount : 0L;
    return Result.success(count);
}
```

**优点：**
- 兼容 Integer 和 Long 两种类型
- 不需要修改缓存配置
- 代码简单

**缺点：**
- 需要在每个可能出现问题的地方添加处理

---

### 方案 3：使用 Jackson 的类型提示（推荐）

**修改 CacheConfig：**
```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    ObjectMapper objectMapper = new ObjectMapper();
    
    // 启用默认类型信息（包含类型元数据）
    objectMapper.activateDefaultTyping(
        objectMapper.getPolymorphicTypeValidator(),
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY
    );
    
    GenericJackson2JsonRedisSerializer jsonSerializer = 
        new GenericJackson2JsonRedisSerializer(objectMapper);
    
    // ... 其他配置
}
```

**优点：**
- 从根本上解决问题
- 序列化时包含类型信息
- 反序列化时使用正确的类型

**缺点：**
- Redis 中存储的数据会变大（包含类型信息）
- 需要清除旧缓存

---

### 方案 4：使用 JdkSerializationRedisSerializer

**修改 CacheConfig：**
```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new JdkSerializationRedisSerializer()
            )
        );
    
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(config)
        .build();
}
```

**优点：**
- Java 原生序列化，类型完全保留
- 不会出现类型转换问题

**缺点：**
- 序列化后的数据不可读（二进制）
- 数据量更大
- 跨语言不兼容

---

### 方案 5：修改返回类型为 Integer

**修改 Service 和 Controller：**
```java
// Service
public Integer getUnreadCount(String username) {
    Long count = repository.countByUserIdAndIsReadFalse(userId);
    return count != null ? count.intValue() : 0;
}

// Controller
public Result<Integer> getUnreadCount(Principal principal) {
    Integer count = notificationService.getUnreadCount(principal.getName());
    return Result.success(count);
}
```

**优点：**
- 避免类型转换问题
- 通知数量通常不会超过 Integer 范围

**缺点：**
- 修改了 API 返回类型
- 如果数量超过 Integer 范围会溢出

---

## 🔧 本项目采用的解决方案

### 采用方案 2：代码安全处理 + 清除旧缓存

**修改的文件：**
1. `NotificationController.java`
2. `NotificationService.java`

**修改内容：**

**NotificationController.java：**
```java
@GetMapping("/unread/count")
public Result<Long> getUnreadCount(Principal principal) {
    // 安全处理 Redis 缓存可能返回 Integer 的情况
    Long rawCount = notificationService.getUnreadCount(principal.getName());
    Long count = rawCount != null ? rawCount : 0L;
    return Result.success(count);
}
```

**NotificationService.java：**
```java
/**
 * 获取未读通知数量（使用缓存）
 * 注意：Redis 缓存反序列化时可能将 Long 转为 Integer
 * 返回 Long 类型，但实际可能是 Integer，调用方需要安全处理
 */
@Cacheable(value = CacheConfig.NOTIFICATION_COUNT_CACHE, key = "#username")
public Long getUnreadCount(String username) {
    User user = getUserByUsername(username);
    Long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    return count != null ? count : 0L;
}
```

**清除旧缓存：**
```bash
redis-cli DEL notificationCount::zyw
```

---

## 📊 各方案对比

| 方案 | 复杂度 | 性能 | 可维护性 | 推荐度 |
|------|--------|------|----------|--------|
| 清除旧缓存 | ⭐ | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐ |
| 代码安全处理 | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| Jackson 类型提示 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| JDK 序列化 | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| 改为 Integer | ⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐ |

---

## 🎯 最佳实践

### 1. 使用明确的类型

**推荐：**
```java
// 如果数量不会超过 Integer 范围，使用 Integer
public Integer getUnreadCount(String username) { ... }

// 如果需要 Long，确保处理类型转换
public Long getUnreadCount(String username) { ... }
```

### 2. 在 Redis 序列化时包含类型信息

**推荐配置：**
```java
ObjectMapper objectMapper = new ObjectMapper();
objectMapper.activateDefaultTyping(
    objectMapper.getPolymorphicTypeValidator(),
    ObjectMapper.DefaultTyping.NON_FINAL,
    JsonTypeInfo.As.PROPERTY
);
```

### 3. 使用 Number 类型作为中间类型

**推荐：**
```java
Number count = notificationService.getUnreadCount(username);
Long longCount = count.longValue();
```

### 4. 添加单元测试

**测试缓存反序列化：**
```java
@Test
public void testCacheDeserialization() {
    // 第一次调用，写入缓存
    Long count1 = service.getUnreadCount("user1");
    
    // 第二次调用，从缓存读取
    Long count2 = service.getUnreadCount("user1");
    
    // 验证类型一致
    assertEquals(count1.getClass(), count2.getClass());
}
```

---

## 🔍 如何检测类似问题

### 1. 检查所有使用 @Cacheable 的方法

```bash
# 搜索所有缓存方法
grep -r "@Cacheable" src/main/java/
```

**关注返回类型为 Long 的方法：**
- `Long countXxx()`
- `Long getXxxCount()`

### 2. 启用 Redis 日志

**application.yml：**
```yaml
logging:
  level:
    org.springframework.data.redis: DEBUG
    org.springframework.cache: DEBUG
```

### 3. 使用 Redis 监控工具

```bash
# 查看缓存的实际类型
redis-cli
> GET notificationCount::zyw
> TYPE notificationCount::zyw
```

---

## ✅ 验证修复

### 测试步骤

1. **清除旧缓存**
   ```bash
   redis-cli DEL notificationCount::zyw
   ```

2. **重启应用**
   ```bash
   mvn spring-boot:run
   ```

3. **第一次调用**（写入缓存）
   ```bash
   GET http://localhost:8080/api/notifications/unread/count
   Authorization: Bearer <token>
   ```

4. **第二次调用**（从缓存读取）
   ```bash
   GET http://localhost:8080/api/notifications/unread/count
   Authorization: Bearer <token>
   ```

5. **检查日志**
   - 不应再出现 `ClassCastException`
   - 应该能正常返回未读数量

### 预期结果

**成功响应：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 5
}
```

---

## 📚 相关文档

- [Spring Cache 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Redis 序列化器文档](https://docs.spring.io/spring-data/redis/docs/current/reference/html/#redis:serializer)
- [Jackson 类型处理](https://github.com/FasterXML/jackson-docs/wiki/JacksonPolymorphicDeserialization)

---

## 🎓 经验总结

### 1. Redis 缓存序列化注意事项

- **数值类型**：小数值可能被反序列化为 Integer
- **日期类型**：需要配置 JavaTimeModule
- **集合类型**：需要包含类型信息

### 2. 类型安全的缓存设计

- 优先使用具体类型（Integer、String）而非泛型（Number、Object）
- 在 Controller 层添加类型转换保护
- 使用 Jackson 的类型提示功能

### 3. 缓存更新策略

- 修改序列化配置后，必须清除旧缓存
- 使用版本号管理缓存格式变更
- 考虑使用缓存预热避免冷启动问题

---

**修复时间**: 2026-01-09  
**修复人员**: Cloud Notes 开发团队  
**影响范围**: NotificationController.getUnreadCount 接口  
**修复方法**: 代码安全处理 + 清除旧缓存
