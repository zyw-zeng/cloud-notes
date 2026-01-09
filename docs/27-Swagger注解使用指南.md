# Swagger 注解使用指南

## 📚 概述

本文档详细说明如何使用 Swagger/OpenAPI 注解来增强 API 文档的可读性和完整性。

---

## 🎯 核心注解说明

### 1. @Tag - Controller 类级别注解

用于对 API 接口进行分组和描述。

**位置：** Controller 类上

**示例：**
```java
@Tag(name = "笔记管理", description = "笔记的增删改查、搜索等操作")
@RestController
@RequestMapping("/api/notes")
public class NoteController {
    // ...
}
```

**效果：**
- 在 Swagger UI 中将相关接口分组显示
- 提供分组的中文名称和描述

---

### 2. @Operation - 方法级别注解

描述单个 API 接口的功能。

**位置：** Controller 方法上

**示例：**
```java
@Operation(
    summary = "创建笔记",
    description = "在指定笔记本中创建一篇新笔记，可以添加标题、内容和标签"
)
@PostMapping
public Result<Note> createNote(@RequestBody NoteDTO dto, Principal principal) {
    // ...
}
```

**参数说明：**
- `summary`: 简短摘要（显示在接口列表中）
- `description`: 详细描述（展开后显示）

---

### 3. @Parameter - 参数注解

描述方法参数的详细信息。

**位置：** 方法参数前

**示例：**
```java
public Result<Note> createNote(
    @Parameter(description = "笔记信息，包含标题、内容、笔记本ID和标签ID列表", required = true)
    @RequestBody NoteDTO dto,
    Principal principal
) {
    // ...
}
```

**常用属性：**
- `description`: 参数描述
- `required`: 是否必填
- `example`: 示例值
- `schema`: 数据类型定义

**更多示例：**
```java
// 路径参数
@Parameter(description = "笔记ID", required = true, example = "1")
@PathVariable Long id

// 查询参数
@Parameter(description = "搜索关键词，将在标题和内容中进行模糊匹配", required = true, example = "会议")
@RequestParam String keyword

// 可选参数
@Parameter(description = "匹配模式：true=必须包含所有标签，false=包含任一标签即可", example = "false")
@RequestParam(defaultValue = "false") Boolean matchAll
```

---

### 4. @ApiResponses - 响应状态码注解

描述接口可能返回的各种响应状态。

**位置：** Controller 方法上

**示例：**
```java
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "创建成功",
        content = @Content(schema = @Schema(implementation = Note.class))),
    @ApiResponse(responseCode = "400", description = "请求参数错误"),
    @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
    @ApiResponse(responseCode = "403", description = "无权限操作"),
    @ApiResponse(responseCode = "404", description = "资源不存在")
})
```

**常用状态码：**
- `200`: 成功
- `201`: 创建成功
- `400`: 请求参数错误
- `401`: 未授权
- `403`: 禁止访问
- `404`: 资源不存在
- `500`: 服务器错误

---

### 5. @Schema - DTO 类和字段注解

描述数据模型和字段信息。

**位置：** DTO 类或字段上

**类级别示例：**
```java
@Data
@Schema(description = "笔记数据传输对象")
public class NoteDTO {
    // ...
}
```

**字段级别示例：**
```java
@Schema(description = "笔记标题", example = "项目会议纪要", required = true, maxLength = 200)
private String title;

@Schema(description = "笔记内容，支持Markdown格式", 
        example = "# 会议内容\n\n1. 讨论项目进度\n2. 确定下一步计划", 
        required = false)
private String content;

@Schema(description = "笔记本ID，指定笔记所属的笔记本", example = "1", required = true)
private Long notebookId;

@Schema(description = "标签ID列表，为笔记添加分类标签", example = "[1, 2, 3]", required = false)
private List<Long> tagIds;
```

**常用属性：**
- `description`: 字段描述
- `example`: 示例值
- `required`: 是否必填
- `maxLength`: 最大长度
- `minLength`: 最小长度
- `minimum`: 最小值（数字）
- `maximum`: 最大值（数字）
- `pattern`: 正则表达式
- `defaultValue`: 默认值

---

## 📝 完整示例

### Controller 完整示例

```java
package com.example.cloudnotes.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Tag(name = "待办任务", description = "待办任务的增删改查、状态管理等操作")
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @Operation(
        summary = "创建待办任务",
        description = "创建一个新的待办任务，可以设置标题、描述、优先级、截止日期等信息"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(schema = @Schema(implementation = Todo.class))),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录")
    })
    @PostMapping
    public Result<Todo> createTodo(
            @Parameter(description = "待办任务信息", required = true)
            @RequestBody TodoDTO dto,
            Principal principal) {
        Todo todo = todoService.createTodo(dto, principal.getName());
        return Result.success(todo);
    }

    @Operation(
        summary = "标记任务完成",
        description = "将指定的待办任务标记为已完成状态"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "404", description = "任务不存在"),
        @ApiResponse(responseCode = "403", description = "无权限操作此任务")
    })
    @PostMapping("/{id}/complete")
    public Result<Todo> completeTodo(
            @Parameter(description = "任务ID", required = true, example = "1")
            @PathVariable Long id,
            Principal principal) {
        Todo todo = todoService.completeTodo(id, principal.getName());
        return Result.success(todo);
    }

    @Operation(
        summary = "获取今日待办",
        description = "获取截止日期为今天的所有待办任务"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/today")
    public Result<List<Todo>> getTodayTodos(Principal principal) {
        List<Todo> todos = todoService.getTodayTodos(principal.getName());
        return Result.success(todos);
    }
}
```

### DTO 完整示例

```java
package com.example.cloudnotes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "待办任务数据传输对象")
public class TodoDTO {
    
    @Schema(description = "任务标题", example = "完成项目文档", required = true, maxLength = 100)
    private String title;
    
    @Schema(description = "任务详细描述", example = "编写API文档和用户手册", required = false, maxLength = 500)
    private String description;
    
    @Schema(description = "优先级：LOW(低)、MEDIUM(中)、HIGH(高)", 
            example = "HIGH", 
            allowableValues = {"LOW", "MEDIUM", "HIGH"},
            required = false,
            defaultValue = "MEDIUM")
    private String priority;
    
    @Schema(description = "截止日期时间", example = "2026-01-10T18:00:00", required = false)
    private LocalDateTime dueDate;
    
    @Schema(description = "是否标记为重要任务", example = "true", defaultValue = "false")
    private Boolean isImportant;
    
    @Schema(description = "标签ID列表", example = "[1, 2]", required = false)
    private List<Long> tagIds;
}
```

---

## 🎨 高级用法

### 1. 枚举类型注解

```java
@Schema(description = "任务状态")
public enum TodoStatus {
    @Schema(description = "待处理")
    PENDING,
    
    @Schema(description = "进行中")
    IN_PROGRESS,
    
    @Schema(description = "已完成")
    COMPLETED,
    
    @Schema(description = "已取消")
    CANCELLED
}
```

### 2. 嵌套对象注解

```java
@Data
@Schema(description = "用户信息")
public class UserDTO {
    
    @Schema(description = "用户名", example = "admin", required = true)
    private String username;
    
    @Schema(description = "用户配置信息")
    private UserSettings settings;
}

@Data
@Schema(description = "用户配置")
class UserSettings {
    
    @Schema(description = "主题：light(浅色) 或 dark(深色)", example = "light")
    private String theme;
    
    @Schema(description = "是否启用通知", example = "true")
    private Boolean notificationsEnabled;
}
```

### 3. 数组/列表注解

```java
@Operation(summary = "批量删除笔记")
@DeleteMapping("/batch")
public Result<Void> batchDelete(
    @Parameter(description = "要删除的笔记ID列表", 
               required = true,
               example = "[1, 2, 3, 4, 5]",
               schema = @Schema(type = "array", implementation = Long.class))
    @RequestBody List<Long> ids,
    Principal principal
) {
    // ...
}
```

### 4. 分页参数注解

```java
@Operation(summary = "分页获取笔记列表")
@GetMapping("/page")
public Result<Page<Note>> getNotesPage(
    @Parameter(description = "页码，从0开始", example = "0")
    @RequestParam(defaultValue = "0") Integer page,
    
    @Parameter(description = "每页数量", example = "10")
    @RequestParam(defaultValue = "10") Integer size,
    
    @Parameter(description = "排序字段", example = "createdAt")
    @RequestParam(defaultValue = "createdAt") String sortBy,
    
    @Parameter(description = "排序方向：asc(升序) 或 desc(降序)", example = "desc")
    @RequestParam(defaultValue = "desc") String sortDir,
    
    Principal principal
) {
    // ...
}
```

---

## 📋 最佳实践

### 1. 命名规范

- **summary**: 使用动词开头，简洁明了（如"创建笔记"、"获取列表"）
- **description**: 提供详细说明，包括功能、参数要求、注意事项
- **example**: 提供真实可用的示例值

### 2. 必填字段标注

```java
@Schema(description = "用户名", example = "admin", required = true)
private String username;
```

### 3. 提供示例值

```java
@Parameter(description = "搜索关键词", required = true, example = "会议")
@RequestParam String keyword
```

### 4. 说明数据格式

```java
@Schema(description = "截止日期，格式：yyyy-MM-dd'T'HH:mm:ss", 
        example = "2026-01-10T18:00:00")
private LocalDateTime dueDate;
```

### 5. 说明取值范围

```java
@Schema(description = "优先级", 
        allowableValues = {"LOW", "MEDIUM", "HIGH"},
        example = "HIGH")
private String priority;
```

### 6. 添加默认值说明

```java
@Schema(description = "是否标记为重要", 
        example = "false", 
        defaultValue = "false")
private Boolean isImportant;
```

---

## 🔍 Swagger UI 效果

添加注解后，在 Swagger UI 中会显示：

### 接口列表
- ✅ 按 `@Tag` 分组显示
- ✅ 显示 `@Operation.summary` 作为接口名称
- ✅ 显示 HTTP 方法和路径

### 接口详情
- ✅ 显示 `@Operation.description` 详细说明
- ✅ 显示所有参数的描述和示例
- ✅ 显示可能的响应状态码
- ✅ 提供 "Try it out" 在线测试功能

### 数据模型
- ✅ 显示所有 DTO 的字段说明
- ✅ 显示字段类型、是否必填
- ✅ 显示示例值
- ✅ 显示数据验证规则

---

## ✅ 检查清单

在编写 API 时，确保：

- [ ] Controller 类添加了 `@Tag` 注解
- [ ] 每个接口方法添加了 `@Operation` 注解
- [ ] 所有参数添加了 `@Parameter` 注解（除了 Principal 等框架参数）
- [ ] 添加了 `@ApiResponses` 说明可能的响应状态
- [ ] DTO 类添加了 `@Schema` 注解
- [ ] DTO 字段添加了详细的 `@Schema` 注解
- [ ] 提供了真实可用的示例值
- [ ] 标注了必填字段
- [ ] 说明了特殊格式或取值范围

---

## 📚 参考资源

- [OpenAPI 3.0 规范](https://swagger.io/specification/)
- [SpringDoc 官方文档](https://springdoc.org/)
- [Swagger 注解文档](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)

---

**最后更新**: 2026-01-08  
**维护者**: Cloud Notes 开发团队
