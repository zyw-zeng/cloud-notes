# API 文档替代方案

## ⚠️ Swagger UI 兼容性问题

### 问题说明

SpringDoc OpenAPI（Swagger）目前与 Spring Boot 4.0.0 存在兼容性问题：

**错误信息：**
```
java.lang.NoSuchMethodError: 'void org.springframework.web.method.ControllerAdviceBean.<init>(java.lang.Object)'
```

**原因：**
- Spring Boot 4.0.0 基于 Spring Framework 6.2.x
- SpringDoc OpenAPI 最新版本（2.6.0）尚未完全兼容 Spring Framework 6.2.x 的 API 变更
- `ControllerAdviceBean` 构造函数签名在新版本中发生了变化

### 临时解决方案

已暂时注释掉 SpringDoc 依赖，等待官方发布兼容版本。

---

## 📚 替代方案

### 方案一：使用 Postman Collection（推荐）

**优势：**
- ✅ 功能强大，支持完整的 API 测试
- ✅ 可以导出分享给团队
- ✅ 支持环境变量管理
- ✅ 可以编写测试脚本

**使用步骤：**

#### 1. 创建 Postman Collection

在项目根目录创建 `postman_collection.json`：

```json
{
  "info": {
    "name": "Cloud Notes API",
    "description": "云笔记系统 API 接口文档",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "auth": {
    "type": "bearer",
    "bearer": [
      {
        "key": "token",
        "value": "{{jwt_token}}",
        "type": "string"
      }
    ]
  },
  "item": [
    {
      "name": "认证",
      "item": [
        {
          "name": "用户登录",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"admin\",\n  \"password\": \"admin123\"\n}"
            },
            "url": {
              "raw": "{{base_url}}/api/auth/login",
              "host": ["{{base_url}}"],
              "path": ["api", "auth", "login"]
            }
          }
        },
        {
          "name": "用户注册",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"newuser\",\n  \"password\": \"password123\",\n  \"email\": \"user@example.com\"\n}"
            },
            "url": {
              "raw": "{{base_url}}/api/auth/register",
              "host": ["{{base_url}}"],
              "path": ["api", "auth", "register"]
            }
          }
        }
      ]
    },
    {
      "name": "笔记管理",
      "item": [
        {
          "name": "获取笔记列表",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{base_url}}/api/notes",
              "host": ["{{base_url}}"],
              "path": ["api", "notes"]
            }
          }
        },
        {
          "name": "创建笔记",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"title\": \"新笔记\",\n  \"content\": \"笔记内容\",\n  \"notebookId\": 1,\n  \"tagIds\": [1, 2]\n}"
            },
            "url": {
              "raw": "{{base_url}}/api/notes",
              "host": ["{{base_url}}"],
              "path": ["api", "notes"]
            }
          }
        },
        {
          "name": "更新笔记",
          "request": {
            "method": "PUT",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"title\": \"更新的标题\",\n  \"content\": \"更新的内容\"\n}"
            },
            "url": {
              "raw": "{{base_url}}/api/notes/:id",
              "host": ["{{base_url}}"],
              "path": ["api", "notes", ":id"],
              "variable": [
                {
                  "key": "id",
                  "value": "1"
                }
              ]
            }
          }
        },
        {
          "name": "删除笔记",
          "request": {
            "method": "DELETE",
            "header": [],
            "url": {
              "raw": "{{base_url}}/api/notes/:id",
              "host": ["{{base_url}}"],
              "path": ["api", "notes", ":id"],
              "variable": [
                {
                  "key": "id",
                  "value": "1"
                }
              ]
            }
          }
        }
      ]
    },
    {
      "name": "待办任务",
      "item": [
        {
          "name": "获取待办列表",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{base_url}}/api/todos",
              "host": ["{{base_url}}"],
              "path": ["api", "todos"]
            }
          }
        },
        {
          "name": "创建待办任务",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"title\": \"新任务\",\n  \"description\": \"任务描述\",\n  \"priority\": \"HIGH\",\n  \"dueDate\": \"2026-01-10T10:00:00\"\n}"
            },
            "url": {
              "raw": "{{base_url}}/api/todos",
              "host": ["{{base_url}}"],
              "path": ["api", "todos"]
            }
          }
        }
      ]
    }
  ],
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8080",
      "type": "string"
    },
    {
      "key": "jwt_token",
      "value": "",
      "type": "string"
    }
  ]
}
```

#### 2. 导入到 Postman

1. 打开 Postman
2. 点击 "Import"
3. 选择 `postman_collection.json` 文件
4. 导入完成

#### 3. 配置环境变量

1. 点击右上角的环境选择器
2. 创建新环境 "Development"
3. 添加变量：
   - `base_url`: `http://localhost:8080`
   - `jwt_token`: （登录后填入）

#### 4. 使用流程

1. 先调用 "用户登录" 接口
2. 复制返回的 token
3. 设置到环境变量 `jwt_token` 中
4. 其他接口会自动使用该 token

---

### 方案二：使用 REST Client（VS Code 插件）

**优势：**
- ✅ 轻量级，直接在 IDE 中使用
- ✅ 支持版本控制
- ✅ 语法简单

**使用步骤：**

#### 1. 安装插件

在 VS Code 中搜索并安装 "REST Client"

#### 2. 创建 API 测试文件

创建 `api-tests.http`：

```http
### 变量定义
@baseUrl = http://localhost:8080
@token = your_jwt_token_here

### 用户登录
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

### 用户注册
POST {{baseUrl}}/api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com"
}

### 获取笔记列表
GET {{baseUrl}}/api/notes
Authorization: Bearer {{token}}

### 创建笔记
POST {{baseUrl}}/api/notes
Content-Type: application/json
Authorization: Bearer {{token}}

{
  "title": "新笔记",
  "content": "笔记内容",
  "notebookId": 1,
  "tagIds": [1, 2]
}

### 更新笔记
PUT {{baseUrl}}/api/notes/1
Content-Type: application/json
Authorization: Bearer {{token}}

{
  "title": "更新的标题",
  "content": "更新的内容"
}

### 删除笔记
DELETE {{baseUrl}}/api/notes/1
Authorization: Bearer {{token}}

### 获取待办任务列表
GET {{baseUrl}}/api/todos
Authorization: Bearer {{token}}

### 创建待办任务
POST {{baseUrl}}/api/todos
Content-Type: application/json
Authorization: Bearer {{token}}

{
  "title": "新任务",
  "description": "任务描述",
  "priority": "HIGH",
  "dueDate": "2026-01-10T10:00:00"
}

### 获取今日待办
GET {{baseUrl}}/api/todos/today
Authorization: Bearer {{token}}

### 获取本周待办
GET {{baseUrl}}/api/todos/week
Authorization: Bearer {{token}}

### 标记任务完成
POST {{baseUrl}}/api/todos/1/complete
Authorization: Bearer {{token}}
```

#### 3. 使用方法

1. 先执行登录请求，获取 token
2. 将 token 填入 `@token` 变量
3. 点击请求上方的 "Send Request" 执行
4. 查看响应结果

---

### 方案三：编写 Markdown API 文档

**优势：**
- ✅ 简单直观
- ✅ 易于维护
- ✅ 支持版本控制

**示例：**

创建 `docs/API接口文档.md`：

```markdown
# Cloud Notes API 接口文档

## 认证接口

### 用户登录
- **URL**: `/api/auth/login`
- **方法**: `POST`
- **认证**: 无需认证
- **请求体**:
  ```json
  {
    "username": "admin",
    "password": "admin123"
  }
  ```
- **响应**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "admin"
  }
  ```

### 用户注册
- **URL**: `/api/auth/register`
- **方法**: `POST`
- **认证**: 无需认证
- **请求体**:
  ```json
  {
    "username": "newuser",
    "password": "password123",
    "email": "user@example.com"
  }
  ```

## 笔记接口

### 获取笔记列表
- **URL**: `/api/notes`
- **方法**: `GET`
- **认证**: 需要 JWT Token
- **请求头**:
  ```
  Authorization: Bearer {token}
  ```
- **响应**:
  ```json
  [
    {
      "id": 1,
      "title": "我的笔记",
      "content": "笔记内容...",
      "notebookId": 1,
      "tags": [{"id": 1, "name": "工作"}],
      "createdAt": "2026-01-08T10:00:00",
      "updatedAt": "2026-01-08T10:00:00"
    }
  ]
  ```

### 创建笔记
- **URL**: `/api/notes`
- **方法**: `POST`
- **认证**: 需要 JWT Token
- **请求体**:
  ```json
  {
    "title": "新笔记",
    "content": "笔记内容",
    "notebookId": 1,
    "tagIds": [1, 2]
  }
  ```

（继续添加其他接口...）
```

---

## 🔄 未来计划

### 等待 SpringDoc 更新

SpringDoc 团队正在开发兼容 Spring Boot 4.0 的版本。一旦发布，我们将：

1. 更新 `pom.xml` 中的 SpringDoc 版本
2. 取消注释依赖
3. 重新启用 Swagger UI

**关注进度：**
- GitHub: https://github.com/springdoc/springdoc-openapi
- 预计发布时间：2026 年 Q1

### 降级 Spring Boot（不推荐）

如果急需 Swagger UI，可以考虑降级到 Spring Boot 3.3.x：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>  <!-- 降级到 3.3.x -->
</parent>
```

**注意：**
- 需要测试所有功能兼容性
- 可能需要调整部分代码
- 不推荐，除非有特殊需求

---

## 📝 推荐方案总结

| 方案 | 适用场景 | 优先级 |
|------|---------|--------|
| **Postman Collection** | 团队协作、完整测试 | ⭐⭐⭐⭐⭐ |
| **REST Client** | 个人开发、快速测试 | ⭐⭐⭐⭐ |
| **Markdown 文档** | 文档归档、版本控制 | ⭐⭐⭐ |

**建议：**
1. 使用 Postman Collection 作为主要测试工具
2. 维护 Markdown 文档作为参考
3. 等待 SpringDoc 更新后再启用 Swagger UI

---

## ✅ 当前状态

- ❌ Swagger UI：暂时不可用（兼容性问题）
- ✅ Postman：可用（推荐）
- ✅ REST Client：可用
- ✅ Markdown 文档：可用

---

**最后更新**: 2026-01-08  
**维护者**: Cloud Notes 开发团队
