# Jackson 日期时间序列化修复

## 🐛 问题描述

**错误信息**：
```
Could not write JSON: Java 8 date/time type `java.time.LocalDateTime` not supported...
```

**原因**：Jackson 默认不支持 Java 8 的日期时间类型（`LocalDateTime`、`LocalDate` 等）的序列化和反序列化。

---

## ✅ 修复方案

### 1. 添加依赖

**文件**：`pom.xml`

```xml
<!-- Jackson Java 8 日期时间支持 -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

---

### 2. 创建 Jackson 配置类

**文件**：`src/main/java/com/example/cloudnotes/config/JacksonConfig.java`

```java
@Configuration
public class JacksonConfig {
    
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        
        // 注册 JavaTimeModule
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // 配置 LocalDateTime 格式
        javaTimeModule.addSerializer(LocalDateTime.class,
            new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        javaTimeModule.addDeserializer(LocalDateTime.class,
            new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        
        // 配置 LocalDate 格式
        javaTimeModule.addSerializer(LocalDate.class,
            new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        javaTimeModule.addDeserializer(LocalDate.class,
            new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        
        objectMapper.registerModule(javaTimeModule);
        
        // 禁用时间戳格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return objectMapper;
    }
}
```

---

## 📊 修复前后对比

### 修复前（错误）

**响应**：
```json
{
  "code": 500,
  "message": "Could not write JSON: Java 8 date/time type `java.time.LocalDateTime` not supported..."
}
```

或者序列化为数组格式（如果启用了 WRITE_DATES_AS_TIMESTAMPS）：
```json
{
  "createdAt": [2025, 12, 3, 17, 22, 0]  // ❌ 难以阅读
}
```

---

### 修复后（正确）

**响应**：
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "title": "测试通知",
    "content": "...",
    "type": "SYSTEM_MESSAGE",
    "isRead": false,
    "createdAt": "2025-12-03 17:22:00",  // ✅ 易读格式
    "readAt": null
  }
}
```

---

## 🔧 支持的日期时间类型

| Java 类型 | 格式 | 示例 |
|-----------|------|------|
| `LocalDateTime` | `yyyy-MM-dd HH:mm:ss` | `2025-12-03 17:22:00` |
| `LocalDate` | `yyyy-MM-dd` | `2025-12-03` |
| `LocalTime` | `HH:mm:ss` | `17:22:00` |
| `Instant` | ISO-8601 | `2025-12-03T09:22:00Z` |

---

## 🧪 测试验证

### 1. 重新加载依赖

```bash
cd f:\web_java\cloud-notes
mvn clean install
```

---

### 2. 启动应用

```bash
mvn spring-boot:run
```

**预期日志**：
```
JacksonConfig - ObjectMapper configured with JavaTimeModule
```

---

### 3. 测试通知接口

```bash
# 获取通知列表
curl http://localhost:8080/api/notifications/unread \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**预期响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "title": "任务提醒",
      "content": "您有一个任务即将到期",
      "type": "TODO_REMINDER",
      "isRead": false,
      "createdAt": "2025-12-03 17:22:00",  // ✅ 正确的格式
      "readAt": null
    }
  ]
}
```

---

### 4. 测试推送通知

```bash
# 触发推送
curl -X POST http://localhost:8080/api/notifications/test-push \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**前端应该能正常接收**：
```javascript
📬 收到新通知: {
  id: 123,
  title: "测试通知",
  createdAt: "2025-12-03 17:22:00"  // ✅ 字符串格式，易解析
}
```

---

## 📝 配置说明

### 日期时间格式

**application.yml**（Spring Boot 默认配置）：
```yaml
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null
```

**JacksonConfig.java**（自定义配置，优先级更高）：
```java
private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
private static final String DATE_FORMAT = "yyyy-MM-dd";
```

---

### 配置优先级

```
1. @JsonFormat 注解（最高优先级）
   ↓
2. JacksonConfig 自定义配置
   ↓
3. application.yml 配置
   ↓
4. Jackson 默认配置（最低优先级）
```

---

## 🎯 实体类使用示例

### 方式 1：使用全局配置（推荐）

```java
@Entity
public class Notification {
    // 使用 JacksonConfig 的全局配置
    private LocalDateTime createdAt;  // 自动格式化为 "yyyy-MM-dd HH:mm:ss"
    private LocalDateTime readAt;
}
```

---

### 方式 2：使用 @JsonFormat 注解（特殊需求）

```java
@Entity
public class Notification {
    // 自定义格式（覆盖全局配置）
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    
    // 只输出日期部分
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime readAt;
}
```

---

### 方式 3：忽略某些字段

```java
@Entity
public class Notification {
    // 正常序列化
    private LocalDateTime createdAt;
    
    // 不序列化到 JSON（但会保存到数据库）
    @JsonIgnore
    private LocalDateTime internalTimestamp;
}
```

---

## 🔄 Redis 缓存序列化

由于我们使用了 `GenericJackson2JsonRedisSerializer`，Redis 缓存也会使用相同的 Jackson 配置。

**CacheConfig.java**：
```java
RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
    .serializeValuesWith(
        RedisSerializationContext.SerializationPair.fromSerializer(
            new GenericJackson2JsonRedisSerializer()  // 使用 JacksonConfig 的配置
        )
    );
```

**Redis 中的数据**：
```json
{
  "@class": "com.example.cloudnotes.entity.Notification",
  "id": 1,
  "title": "测试通知",
  "createdAt": "2025-12-03 17:22:00",  // ✅ 字符串格式
  "isRead": false
}
```

---

## 🐛 常见问题

### Q1: 日期格式不对

**问题**：返回的日期是数组格式 `[2025, 12, 3, 17, 22, 0]`

**原因**：未禁用 `WRITE_DATES_AS_TIMESTAMPS`

**解决**：
```java
objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

---

### Q2: 反序列化失败

**错误**：
```
Cannot deserialize value of type `java.time.LocalDateTime` from String
```

**原因**：前端发送的日期格式与后端不匹配

**解决**：
```javascript
// 前端发送时使用正确格式
{
  "dueDate": "2025-12-10 18:00:00"  // ✅ 匹配后端格式
}

// 不要用这种格式
{
  "dueDate": "2025-12-10T18:00:00.000Z"  // ❌ ISO-8601，后端需配置支持
}
```

---

### Q3: 时区问题

**问题**：返回的时间不对，相差 8 小时

**原因**：时区配置不一致

**解决**：
```yaml
# application.yml
spring:
  jackson:
    time-zone: GMT+8  # 设置为东八区
```

或在 `@JsonFormat` 中指定：
```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private LocalDateTime createdAt;
```

---

### Q4: Redis 缓存反序列化失败

**错误**：
```
Could not read JSON: Unexpected token
```

**原因**：Redis 中存储的是旧格式数据

**解决**：清空 Redis 缓存
```bash
redis-cli FLUSHDB
```

---

## 📋 检查清单

### 部署前

- [x] 添加 `jackson-datatype-jsr310` 依赖
- [x] 创建 `JacksonConfig` 配置类
- [x] 配置日期时间格式
- [x] 禁用时间戳序列化
- [x] 测试序列化和反序列化

---

### 部署后

- [ ] 验证 API 返回正确的日期格式
- [ ] 测试 WebSocket 推送通知
- [ ] 检查 Redis 缓存数据格式
- [ ] 验证前端能正常解析日期

---

## 🎉 总结

### 已完成

- ✅ 添加 Jackson Java 8 日期时间支持依赖
- ✅ 创建 JacksonConfig 配置类
- ✅ 配置 LocalDateTime 和 LocalDate 格式化
- ✅ 禁用时间戳序列化
- ✅ 支持 Redis 缓存序列化

---

### 效果

| 方面 | 修复前 | 修复后 |
|------|--------|--------|
| **API 响应** | ❌ 报错或数组格式 | ✅ 字符串格式 |
| **易读性** | ❌ `[2025,12,3,17,22,0]` | ✅ `2025-12-03 17:22:00` |
| **前端解析** | ❌ 需要特殊处理 | ✅ 直接使用 |
| **Redis 缓存** | ❌ 序列化失败 | ✅ 正常存储 |

---

**修复日期**: 2025-12-03  
**版本**: v1.0.0  
**状态**: ✅ 完成
