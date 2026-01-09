# Flyway 数据库迁移指南

## 📋 目录
- [什么是 Flyway](#什么是-flyway)
- [为什么要使用 Flyway](#为什么要使用-flyway)
- [项目配置说明](#项目配置说明)
- [迁移脚本命名规则](#迁移脚本命名规则)
- [如何添加新的迁移脚本](#如何添加新的迁移脚本)
- [常用操作](#常用操作)
- [注意事项](#注意事项)
- [故障排查](#故障排查)

---

## 什么是 Flyway

Flyway 是一个开源的数据库版本管理工具，它可以：
- 📝 通过 SQL 脚本管理数据库表结构变更
- 🔄 自动追踪和执行数据库迁移
- 📊 记录每次迁移的版本和状态
- ✅ 确保所有环境的数据库结构一致

---

## 为什么要使用 Flyway

### ❌ 之前的问题（使用 JPA ddl-auto=update）

```yaml
hibernate:
  ddl-auto: update  # ⚠️ 生产环境危险！
```

**存在的风险：**
1. ❌ **数据丢失风险** - 自动修改表结构可能导致数据丢失
2. ❌ **无法回滚** - 无法撤销错误的表结构变更
3. ❌ **缺少审计** - 不知道谁在什么时候修改了表结构
4. ❌ **环境不一致** - 开发、测试、生产环境可能不同步
5. ❌ **团队协作困难** - 多人开发时容易冲突

### ✅ 使用 Flyway 的优势

```yaml
hibernate:
  ddl-auto: validate  # ✅ 只验证，不修改
flyway:
  enabled: true       # ✅ 使用 Flyway 管理
```

**带来的好处：**
1. ✅ **版本控制** - 每个变更都有版本号，可追溯
2. ✅ **可回滚** - 支持编写回滚脚本
3. ✅ **安全可靠** - 生产环境不会自动修改表结构
4. ✅ **团队协作** - SQL 脚本纳入 Git 版本管理
5. ✅ **环境一致** - 所有环境执行相同的迁移脚本

---

## 项目配置说明

### 1. Maven 依赖

已在 `pom.xml` 中添加：

```xml
<!-- Flyway 数据库迁移工具 -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

### 2. 配置文件

#### application.yml（通用配置）
```yaml
spring:
  flyway:
    enabled: true                          # 启用 Flyway
    baseline-on-migrate: true              # 已有表时创建基线
    locations: classpath:db/migration      # SQL 脚本位置
    encoding: UTF-8
    validate-on-migrate: true
    
  jpa:
    hibernate:
      ddl-auto: validate  # ⚠️ 重要：改为 validate
```

#### application-dev.yml（开发环境）
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    clean-disabled: false  # 开发环境允许 clean
    
  jpa:
    hibernate:
      ddl-auto: validate  # 推荐使用 validate
```

#### application-prod.yml（生产环境）
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    clean-disabled: true   # ⚠️ 生产环境禁止 clean
    validate-on-migrate: true
    
  jpa:
    hibernate:
      ddl-auto: validate  # ⚠️ 生产环境必须 validate
```

### 3. 目录结构

```
src/main/resources/
└── db/
    └── migration/
        ├── V1__Initial_schema.sql          # 初始化表结构
        ├── V2__Add_user_phone_column.sql   # 示例：添加字段
        └── V3__Create_index_on_notes.sql   # 示例：添加索引
```

---

## 迁移脚本命名规则

### 命名格式

```
V{版本号}__{描述}.sql
```

- **V** - 固定前缀（大写）
- **版本号** - 递增的数字，如：1, 2, 3 或 1.0, 1.1, 2.0
- **双下划线** - 两个下划线 `__`
- **描述** - 英文描述，用下划线分隔单词
- **.sql** - 文件扩展名

### 示例

✅ **正确命名：**
```
V1__Initial_schema.sql
V2__Add_user_phone_column.sql
V3__Create_index_on_notes.sql
V4__Add_todo_category_table.sql
V2.1__Fix_user_email_constraint.sql
```

❌ **错误命名：**
```
v1__initial.sql              # v 必须大写
V1_initial.sql               # 只有一个下划线
V1__初始化表结构.sql          # 不能用中文
1__initial.sql               # 缺少 V 前缀
V1__initial.txt              # 扩展名必须是 .sql
```

---

## 如何添加新的迁移脚本

### 场景 1：添加新表

**需求：** 添加一个用户反馈表

**步骤：**

1. 创建文件：`V5__Create_feedback_table.sql`

```sql
-- V5: 创建用户反馈表
CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content TEXT NOT NULL COMMENT '反馈内容',
    type VARCHAR(50) NOT NULL COMMENT '反馈类型',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';
```

2. 重启应用，Flyway 会自动执行

### 场景 2：添加字段

**需求：** 给 users 表添加手机号字段

**步骤：**

1. 创建文件：`V6__Add_user_phone_column.sql`

```sql
-- V6: 给用户表添加手机号字段
ALTER TABLE users 
ADD COLUMN phone VARCHAR(20) COMMENT '手机号' AFTER email;

-- 添加索引
CREATE INDEX idx_phone ON users(phone);
```

2. 重启应用

### 场景 3：修改字段

**需求：** 修改 notes 表的 title 字段长度

**步骤：**

1. 创建文件：`V7__Modify_note_title_length.sql`

```sql
-- V7: 修改笔记标题字段长度
ALTER TABLE notes 
MODIFY COLUMN title VARCHAR(500) NOT NULL COMMENT '笔记标题';
```

### 场景 4：添加索引

**需求：** 给 todos 表添加复合索引

**步骤：**

1. 创建文件：`V8__Add_index_on_todos.sql`

```sql
-- V8: 给待办任务表添加复合索引
CREATE INDEX idx_user_status_priority 
ON todos(user_id, status, priority);

-- 添加全文索引
CREATE FULLTEXT INDEX ft_title_description 
ON todos(title, description);
```

---

## 常用操作

### 1. 查看迁移状态

Flyway 会在数据库中创建 `flyway_schema_history` 表来记录迁移历史。

**查询 SQL：**
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

**字段说明：**
- `installed_rank` - 执行顺序
- `version` - 版本号
- `description` - 描述
- `script` - 脚本文件名
- `success` - 是否成功
- `installed_on` - 执行时间

### 2. 首次启用 Flyway（已有数据库）

如果你的数据库已经有表了，首次启用 Flyway 时：

1. **方式一：使用 baseline（推荐）**
   
   配置文件中已设置：
   ```yaml
   flyway:
     baseline-on-migrate: true
   ```
   
   Flyway 会自动创建基线版本，跳过 V1 脚本。

2. **方式二：手动 baseline**
   
   ```bash
   mvn flyway:baseline
   ```

### 3. 清空数据库（仅开发环境）

⚠️ **危险操作！仅限开发环境！**

```bash
# Maven 命令
mvn flyway:clean

# 或在配置中启用
spring:
  flyway:
    clean-disabled: false  # 允许 clean
```

### 4. 修复失败的迁移

如果迁移失败，Flyway 会记录失败状态，需要手动修复：

```bash
# 1. 修复数据库问题
# 2. 删除失败记录
DELETE FROM flyway_schema_history WHERE success = 0;

# 3. 重启应用
```

---

## 注意事项

### ⚠️ 重要规则

1. **已执行的脚本不能修改**
   - ❌ 不能修改已经执行过的 SQL 文件
   - ✅ 如需修改，创建新的迁移脚本

2. **版本号必须递增**
   - ❌ 不能使用已存在的版本号
   - ✅ 使用比最新版本更大的版本号

3. **脚本必须幂等**
   - ✅ 使用 `IF NOT EXISTS`
   - ✅ 使用 `IF EXISTS`
   - ✅ 考虑脚本可能重复执行的情况

4. **生产环境谨慎操作**
   - ⚠️ 生产环境必须设置 `ddl-auto: validate`
   - ⚠️ 生产环境必须设置 `clean-disabled: true`
   - ⚠️ 迁移前务必备份数据库

5. **团队协作**
   - 📝 迁移脚本纳入 Git 版本管理
   - 🔄 拉取代码后及时执行迁移
   - 💬 重大变更提前沟通

### ✅ 最佳实践

1. **脚本要简洁**
   - 一个脚本只做一件事
   - 添加清晰的注释

2. **测试后再提交**
   - 在开发环境测试通过
   - 确认脚本可以成功执行

3. **备份很重要**
   - 生产环境执行前备份
   - 准备回滚方案

4. **使用事务**
   ```sql
   -- 大部分 DDL 语句在 MySQL 中会自动提交
   -- 但可以将多个 DML 语句包在事务中
   START TRANSACTION;
   -- 你的 SQL 语句
   COMMIT;
   ```

---

## 故障排查

### 问题 1：启动报错 "Validate failed"

**原因：** 数据库表结构与 Entity 定义不一致

**解决：**
```bash
# 1. 检查数据库表结构
# 2. 检查 Entity 类定义
# 3. 创建迁移脚本修复差异
```

### 问题 2：迁移脚本执行失败

**原因：** SQL 语法错误或表已存在

**解决：**
```sql
-- 1. 查看失败记录
SELECT * FROM flyway_schema_history WHERE success = 0;

-- 2. 手动修复数据库
-- 3. 删除失败记录
DELETE FROM flyway_schema_history WHERE version = 'X';

-- 4. 重启应用
```

### 问题 3：版本冲突

**原因：** 多人同时创建了相同版本号的脚本

**解决：**
```bash
# 1. 协商版本号
# 2. 重命名其中一个脚本
# 3. 提交代码前先拉取最新代码
```

### 问题 4：需要回滚

**方案：**
```sql
-- Flyway 不直接支持回滚，需要手动编写回滚脚本
-- 例如：V9__Rollback_v8_changes.sql

-- 回滚 V8 添加的索引
DROP INDEX idx_user_status_priority ON todos;
DROP INDEX ft_title_description ON todos;
```

---

## 快速参考

### 常用 Maven 命令

```bash
# 查看迁移状态
mvn flyway:info

# 执行迁移
mvn flyway:migrate

# 验证迁移
mvn flyway:validate

# 清空数据库（危险！）
mvn flyway:clean

# 创建基线
mvn flyway:baseline

# 修复失败的迁移
mvn flyway:repair
```

### 配置参数速查

| 参数 | 说明 | 开发环境 | 生产环境 |
|------|------|----------|----------|
| `enabled` | 是否启用 | `true` | `true` |
| `baseline-on-migrate` | 已有表时创建基线 | `true` | `true` |
| `clean-disabled` | 禁止 clean | `false` | `true` ⚠️ |
| `validate-on-migrate` | 迁移前验证 | `true` | `true` |
| `ddl-auto` | JPA 建表策略 | `validate` | `validate` ⚠️ |

---

## 总结

✅ **已完成的优化：**

1. ✅ 添加了 Flyway 依赖
2. ✅ 创建了初始化 SQL 脚本（V1）
3. ✅ 修改了配置文件（区分开发/生产环境）
4. ✅ 将 `ddl-auto` 改为 `validate`
5. ✅ 生产环境禁用了危险操作

✅ **带来的好处：**

- 🔒 **更安全** - 生产环境不会自动修改表结构
- 📝 **可追溯** - 每次变更都有记录
- 🔄 **可回滚** - 支持编写回滚脚本
- 👥 **易协作** - SQL 脚本纳入版本管理
- ✅ **更规范** - 符合生产环境最佳实践

---

## 下一步

1. **首次启动应用**
   - Flyway 会自动执行 V1 脚本创建表
   - 或者如果表已存在，会创建基线版本

2. **后续开发**
   - 需要修改表结构时，创建新的迁移脚本
   - 按照命名规则：`V{版本号}__{描述}.sql`

3. **团队协作**
   - 将迁移脚本提交到 Git
   - 其他成员拉取代码后，Flyway 会自动执行新脚本

---

**🎉 恭喜！你的项目已经成功引入 Flyway 数据库迁移工具！**
