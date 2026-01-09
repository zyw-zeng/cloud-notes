# Swagger UI 使用指南

## 📋 问题修复

### 问题描述
访问 `http://localhost:8080/swagger-ui.html` 时出现 401 未授权错误，无法访问 Swagger UI。

### 原因分析
Spring Security 配置中没有将 Swagger UI 相关路径添加到白名单，导致访问时被拦截要求认证。

### 解决方案
在 `SecurityConfig.java` 中添加 Swagger UI 路径到白名单：

```java
.authorizeHttpRequests(auth -> auth
    // 允许所有人访问 /api/auth 下的所有接口 (注册、登录)
    .requestMatchers("/api/auth/**").permitAll()
    // 允许 WebSocket 连接（SockJS 需要）
    .requestMatchers("/ws/**").permitAll()
    // ✅ 允许访问 Swagger UI 和 API 文档
    .requestMatchers(
        "/swagger-ui/**",        // Swagger UI 静态资源
        "/swagger-ui.html",      // Swagger UI 主页
        "/v3/api-docs/**",       // OpenAPI 3.0 文档
        "/swagger-resources/**", // Swagger 资源
        "/webjars/**"            // WebJars 资源
    ).permitAll()
    // 其他任何接口，都必须登录后才能访问
    .anyRequest().authenticated()
)
```

---

## 🚀 快速开始

### 1. 启动应用

```bash
mvn spring-boot:run
```

### 2. 访问 Swagger UI

浏览器打开以下地址：

```
http://localhost:8080/swagger-ui.html
```

或者：

```
http://localhost:8080/swagger-ui/index.html
```

### 3. 查看 API 文档

**OpenAPI JSON 文档：**
```
http://localhost:8080/v3/api-docs
```

---

## 📖 使用说明

### 界面介绍

Swagger UI 界面主要包含以下部分：

1. **顶部信息栏**
   - API 标题：Cloud Notes API
   - 版本号：v1.0.0
   - 描述：云笔记系统 RESTful API 文档

2. **服务器选择**
   - 开发环境：http://localhost:8080
   - 生产环境：https://api.cloudnotes.com

3. **认证按钮（Authorize）**
   - 点击配置 JWT Token
   - 配置后可测试需要认证的接口

4. **API 分组（Tags）**
   - 按功能模块分组显示
   - 例如：笔记管理、待办任务、用户管理等

5. **接口列表**
   - 每个接口显示 HTTP 方法、路径、描述
   - 点击展开可查看详细信息

---

## 🔐 使用 JWT 认证测试 API

### 步骤 1：获取 JWT Token

**方法一：使用 Swagger UI**

1. 找到 `auth-controller` 分组
2. 展开 `POST /api/auth/login` 接口
3. 点击 "Try it out"
4. 输入请求体：
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
5. 点击 "Execute"
6. 复制响应中的 `token` 值

**方法二：使用 curl**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**响应示例：**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin"
}
```

### 步骤 2：配置认证

1. 点击 Swagger UI 右上角的 **"Authorize"** 按钮（锁图标）
2. 在弹出的对话框中输入：
   ```
   Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```
   **注意：** 必须以 `Bearer ` 开头（Bearer 后面有一个空格）

3. 点击 **"Authorize"** 按钮
4. 点击 **"Close"** 关闭对话框

### 步骤 3：测试需要认证的接口

现在你可以测试任何需要认证的接口了：

1. 找到想要测试的接口（例如 `GET /api/notes`）
2. 点击 "Try it out"
3. 填写必要的参数
4. 点击 "Execute"
5. 查看响应结果

**接口右侧会显示一个锁图标 🔒，表示该接口需要认证。**

---

## 📝 接口测试示例

### 示例 1：获取笔记列表

**接口：** `GET /api/notes`

**步骤：**
1. 确保已配置 JWT Token（参考上面的认证步骤）
2. 找到 `note-controller` 分组
3. 展开 `GET /api/notes` 接口
4. 点击 "Try it out"
5. 可选：填写查询参数（如 `notebookId`）
6. 点击 "Execute"
7. 查看响应

**响应示例：**
```json
[
  {
    "id": 1,
    "title": "我的第一篇笔记",
    "content": "这是笔记内容...",
    "notebookId": 1,
    "tags": [
      {"id": 1, "name": "工作"}
    ],
    "createdAt": "2026-01-08T10:00:00",
    "updatedAt": "2026-01-08T10:00:00"
  }
]
```

### 示例 2：创建笔记

**接口：** `POST /api/notes`

**步骤：**
1. 找到 `POST /api/notes` 接口
2. 点击 "Try it out"
3. 在 Request body 中输入：
   ```json
   {
     "title": "新笔记标题",
     "content": "笔记内容...",
     "notebookId": 1,
     "tagIds": [1, 2]
   }
   ```
4. 点击 "Execute"
5. 查看响应

**响应示例：**
```json
{
  "id": 2,
  "title": "新笔记标题",
  "content": "笔记内容...",
  "notebookId": 1,
  "tags": [
    {"id": 1, "name": "工作"},
    {"id": 2, "name": "学习"}
  ],
  "createdAt": "2026-01-08T16:30:00",
  "updatedAt": "2026-01-08T16:30:00"
}
```

### 示例 3：更新笔记

**接口：** `PUT /api/notes/{id}`

**步骤：**
1. 找到 `PUT /api/notes/{id}` 接口
2. 点击 "Try it out"
3. 填写路径参数 `id`（例如：1）
4. 在 Request body 中输入更新的数据
5. 点击 "Execute"

---

## 🎨 界面功能说明

### 1. 查看请求参数

每个接口展开后可以看到：

- **Parameters（参数）**
  - Path Parameters：路径参数（如 `/api/notes/{id}` 中的 `id`）
  - Query Parameters：查询参数（如 `?notebookId=1`）
  - Header Parameters：请求头参数
  - Request Body：请求体（POST/PUT 接口）

- **参数说明**
  - 参数名称
  - 数据类型
  - 是否必填
  - 描述和示例值

### 2. 查看响应信息

- **Responses（响应）**
  - 200：成功
  - 201：创建成功
  - 400：请求参数错误
  - 401：未授权
  - 404：资源不存在
  - 500：服务器错误

- **响应示例**
  - Example Value：响应数据示例
  - Schema：数据结构定义

### 3. 查看数据模型

页面底部的 **Schemas** 部分显示所有数据模型定义：

- Note（笔记）
- Notebook（笔记本）
- Todo（待办任务）
- User（用户）
- Tag（标签）
- 等等

点击可以查看每个模型的字段定义。

---

## 🔧 高级功能

### 1. 导出 API 文档

**导出为 JSON：**
```
http://localhost:8080/v3/api-docs
```

**导出为 YAML：**
```
http://localhost:8080/v3/api-docs.yaml
```

### 2. 导入到 Postman

1. 打开 Postman
2. 点击 "Import"
3. 选择 "Link"
4. 输入：`http://localhost:8080/v3/api-docs`
5. 点击 "Continue" 和 "Import"

所有 API 接口会自动导入到 Postman 中。

### 3. 生成客户端 SDK

使用 OpenAPI Generator 可以生成各种语言的客户端 SDK：

```bash
# 安装 OpenAPI Generator
npm install @openapitools/openapi-generator-cli -g

# 生成 Java 客户端
openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g java \
  -o ./client-java

# 生成 JavaScript 客户端
openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g javascript \
  -o ./client-js

# 生成 Python 客户端
openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g python \
  -o ./client-python
```

---

## ⚠️ 注意事项

### 1. 生产环境安全

**生产环境建议关闭 Swagger UI：**

```yaml
# application-prod.yml
springdoc:
  swagger-ui:
    enabled: false  # 生产环境关闭
  api-docs:
    enabled: false  # 生产环境关闭
```

**或者添加访问控制：**

```java
// SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
        .hasRole("ADMIN")  // 仅管理员可访问
    // ...
)
```

### 2. Token 过期处理

JWT Token 默认有效期为 24 小时。如果 Token 过期：

1. 重新登录获取新 Token
2. 在 Swagger UI 中重新配置认证
3. 继续测试

### 3. CORS 问题

如果前端调用 API 时遇到 CORS 问题，需要在 `WebConfig.java` 中配置：

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins("http://localhost:3000")
        .allowedMethods("GET", "POST", "PUT", "DELETE")
        .allowedHeaders("*")
        .allowCredentials(true);
}
```

---

## 🐛 常见问题

### Q1: 访问 Swagger UI 显示 404

**原因：** 应用未启动或端口错误

**解决：**
```bash
# 检查应用是否启动
curl http://localhost:8080/actuator/health

# 检查端口配置
# application.yml 中 server.port 是否为 8080
```

### Q2: 访问 Swagger UI 显示 401 未授权

**原因：** SecurityConfig 未放行 Swagger 路径（已修复）

**解决：** 确认 `SecurityConfig.java` 中已添加：
```java
.requestMatchers(
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**"
).permitAll()
```

### Q3: 配置 Token 后仍然返回 401

**可能原因：**
1. Token 格式错误（缺少 `Bearer ` 前缀）
2. Token 已过期
3. Token 签名验证失败

**解决：**
- 确保格式为：`Bearer your_token_here`
- 重新登录获取新 Token
- 检查 JWT 密钥配置是否正确

### Q4: 接口列表为空

**原因：** Controller 未添加 Swagger 注解或包扫描路径错误

**解决：**
```java
// 在 Controller 上添加注解
@Tag(name = "笔记管理", description = "笔记的增删改查操作")
@RestController
@RequestMapping("/api/notes")
public class NoteController {
    // ...
}
```

### Q5: 中文显示乱码

**解决：**
```yaml
# application.yml
spring:
  http:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

---

## 📚 相关文档

- [24-架构优化总结-Redis与API文档.md](./24-架构优化总结-Redis与API文档.md) - Swagger 集成说明
- [OpenAPI 3.0 规范](https://swagger.io/specification/)
- [SpringDoc 官方文档](https://springdoc.org/)

---

## ✅ 验收清单

- [x] 可以访问 `http://localhost:8080/swagger-ui.html`
- [x] 可以查看所有 API 接口
- [x] 可以配置 JWT Token
- [x] 可以在线测试接口
- [x] 可以查看请求/响应示例
- [x] 可以导出 OpenAPI 文档

---

**文档创建时间**: 2026-01-08  
**最后更新**: 2026-01-08  
**维护者**: Cloud Notes 开发团队
