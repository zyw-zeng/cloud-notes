# 笔记搜索功能 API 文档

## 📋 功能概述

云笔记系统提供了强大的全文搜索功能，支持：
- ✅ 关键词搜索（同时搜索标题和内容）
- ✅ 标题单独搜索
- ✅ 内容单独搜索
- ✅ 标签匹配搜索（支持 AND/OR 逻辑）
- ✅ 组合条件搜索
- ✅ 笔记本范围限定搜索

---

## 🔍 搜索接口列表

### 1. 智能搜索（推荐）

**接口**: `POST /api/notes/search`

**描述**: 支持多条件组合搜索，自动选择最优搜索策略。

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求体**:
```json
{
  "keyword": "Spring Boot",           // 通用关键词（搜索标题+内容）
  "title": "笔记",                    // 标题关键词（可选）
  "content": "学习",                  // 内容关键词（可选）
  "tagIds": [1, 2, 3],               // 标签ID列表（可选）
  "notebookId": 5,                   // 笔记本ID（可选，限定范围）
  "matchAllTags": false              // 是否匹配所有标签（默认false）
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "Spring Boot 学习笔记",
      "content": "这是关于 Spring Boot 的详细内容...",
      "createdAt": "2025-11-28T10:00:00",
      "updatedAt": "2025-11-28T15:30:00",
      "tags": [
        {"id": 1, "name": "Java"},
        {"id": 2, "name": "后端"}
      ]
    }
  ]
}
```

---

### 2. 快速搜索

**接口**: `GET /api/notes/search/quick?keyword={keyword}`

**描述**: 快速搜索，同时匹配标题和内容。

**示例**:
```bash
GET /api/notes/search/quick?keyword=Spring
```

**响应**: 同智能搜索

---

### 3. 标签搜索

**接口**: `GET /api/notes/search/tags?tagIds=1,2,3&matchAll=false`

**描述**: 根据标签ID搜索笔记。

**参数**:
- `tagIds`: 标签ID列表（逗号分隔）
- `matchAll`: 是否需要匹配所有标签
  - `false`: 匹配任意一个标签即可（OR 逻辑）
  - `true`: 必须匹配所有标签（AND 逻辑）

**示例**:
```bash
# 匹配任意标签（OR）
GET /api/notes/search/tags?tagIds=1,2&matchAll=false

# 必须同时拥有所有标签（AND）
GET /api/notes/search/tags?tagIds=1,2&matchAll=true
```

---

### 4. 标题搜索

**接口**: `GET /api/notes/search/title?title={title}`

**描述**: 只在标题中搜索关键词。

**示例**:
```bash
GET /api/notes/search/title?title=学习笔记
```

---

### 5. 获取所有笔记

**接口**: `GET /api/notes/all`

**描述**: 获取当前用户的所有笔记（按创建时间倒序）。

**示例**:
```bash
GET /api/notes/all
```

---

## 💡 使用场景示例

### 场景 1: 全局关键词搜索

用户在搜索框输入 "Spring"，搜索所有包含该关键词的笔记：

```bash
POST /api/notes/search
{
  "keyword": "Spring"
}
```

---

### 场景 2: 在特定笔记本内搜索

在 "技术笔记" 笔记本（ID=5）中搜索包含 "数据库" 的笔记：

```bash
POST /api/notes/search
{
  "keyword": "数据库",
  "notebookId": 5
}
```

---

### 场景 3: 标签筛选

查找同时拥有 "Java" 和 "后端" 标签的笔记：

```bash
GET /api/notes/search/tags?tagIds=1,2&matchAll=true
```

查找拥有 "Java" 或 "Python" 标签的笔记：

```bash
GET /api/notes/search/tags?tagIds=1,3&matchAll=false
```

---

### 场景 4: 精确组合搜索

查找标题包含 "面试"，内容包含 "算法"，且带有 "重要" 标签的笔记：

```bash
POST /api/notes/search
{
  "title": "面试",
  "content": "算法",
  "tagIds": [5]
}
```

---

### 场景 5: 只搜索标题

快速查找标题中包含 "TODO" 的笔记：

```bash
GET /api/notes/search/title?title=TODO
```

---

## 🔧 前端集成示例

### JavaScript / Fetch API

```javascript
// 1. 快速搜索
async function quickSearch(keyword) {
  const response = await fetch(
    `/api/notes/search/quick?keyword=${encodeURIComponent(keyword)}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  return await response.json();
}

// 2. 智能搜索
async function smartSearch(searchParams) {
  const response = await fetch('/api/notes/search', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(searchParams)
  });
  return await response.json();
}

// 3. 标签搜索
async function searchByTags(tagIds, matchAll = false) {
  const params = new URLSearchParams({
    tagIds: tagIds.join(','),
    matchAll: matchAll
  });
  const response = await fetch(`/api/notes/search/tags?${params}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return await response.json();
}
```

### Axios

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
});

// 智能搜索
export const searchNotes = (searchDTO) => {
  return api.post('/notes/search', searchDTO);
};

// 快速搜索
export const quickSearch = (keyword) => {
  return api.get('/notes/search/quick', { params: { keyword } });
};

// 标签搜索
export const searchByTags = (tagIds, matchAll = false) => {
  return api.get('/notes/search/tags', { 
    params: { tagIds, matchAll } 
  });
};
```

---

## 📝 搜索特性说明

### 1. 大小写不敏感
所有搜索都不区分大小写：
- 搜索 "spring" 可以匹配 "Spring"、"SPRING"、"sPrInG"

### 2. 模糊匹配
使用 LIKE 模糊匹配：
- 搜索 "学习" 可以匹配 "我的学习笔记"、"学习 Java"

### 3. 权限控制
- 只能搜索自己的笔记
- 自动过滤其他用户的笔记

### 4. 智能策略
- 当提供 `keyword` 时，优先使用关键词搜索
- 没有 `keyword` 时，使用组合条件搜索
- 自动去除空白字符

---

## ⚠️ 注意事项

1. **搜索性能**
   - 对于大量数据，建议限定笔记本范围
   - 内容搜索比标题搜索慢（因为内容字段通常更长）

2. **参数验证**
   - 关键词为空时会返回空结果
   - 标签ID列表为空时会返回空结果

3. **返回顺序**
   - 默认按创建时间倒序返回
   - 可以在前端自行排序

4. **标签逻辑**
   - `matchAll=false`: 只要包含任意一个标签（OR）
   - `matchAll=true`: 必须包含所有标签（AND）

---

## 🚀 性能优化建议

1. **使用索引**: 确保数据库表的 `title` 和 `content` 字段有适当的索引
2. **分页查询**: 对于大量结果，建议实现分页
3. **缓存热门搜索**: 可以缓存常用搜索结果
4. **全文搜索引擎**: 数据量很大时，考虑使用 Elasticsearch

---

## 📊 搜索 API 对比

| 接口 | 速度 | 精确度 | 适用场景 |
|------|------|--------|----------|
| 快速搜索 | ⚡⚡⚡ | ⭐⭐ | 用户搜索框 |
| 智能搜索 | ⚡⚡ | ⭐⭐⭐ | 高级搜索功能 |
| 标题搜索 | ⚡⚡⚡ | ⭐⭐⭐ | 快速定位笔记 |
| 标签搜索 | ⚡⚡⚡ | ⭐⭐⭐⭐ | 分类筛选 |
| 组合搜索 | ⚡ | ⭐⭐⭐⭐⭐ | 精确查找 |

---

## 🎯 总结

云笔记搜索系统提供了灵活强大的搜索能力：
- ✅ 简单快速的关键词搜索
- ✅ 精确的组合条件搜索
- ✅ 灵活的标签筛选
- ✅ 完善的权限控制

根据实际需求选择合适的搜索接口，即可实现高效的笔记检索！
