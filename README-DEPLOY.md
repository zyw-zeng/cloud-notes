# Cloud Notes 部署快速指南

## 📦 部署文件说明

本项目提供了完整的宝塔Linux面板部署方案，包含以下文件：

### 📄 文档
- **`docs/宝塔Linux部署指南.md`** - 详细的部署步骤和说明文档

### 🔧 脚本文件
- **`start.sh`** - 应用启动脚本
- **`stop.sh`** - 应用停止脚本
- **`restart.sh`** - 应用重启脚本
- **`deploy.sh`** - 一键部署脚本（自动化部署）

### ⚙️ 配置文件
- **`cloud-notes.service`** - systemd服务配置文件
- **`.env.prod.example`** - 生产环境变量模板
- **`nginx.conf.example`** - Nginx反向代理配置模板

---

## 🚀 快速开始

### 方法一：使用一键部署脚本（推荐）

```bash
# 1. 上传项目到服务器
scp -r cloud-notes root@your-server:/www/wwwroot/

# 2. 登录服务器
ssh root@your-server

# 3. 配置环境变量
cd /www/wwwroot/cloud-notes
cp .env.prod .env.prod
vim .env.prod  # 修改数据库、Redis、JWT等配置

# 4. 运行一键部署脚本
chmod +x deploy.sh
bash deploy.sh
```

### 方法二：手动部署

```bash
# 1. 构建项目
mvn clean package -DskipTests

# 2. 配置环境变量
cp .env.prod .env.prod
vim .env.prod

# 3. 安装systemd服务
cp cloud-notes.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable cloud-notes

# 4. 启动服务
systemctl start cloud-notes

# 5. 查看状态
systemctl status cloud-notes
```

### 方法三：使用启动脚本

```bash
# 1. 赋予执行权限
chmod +x start.sh stop.sh restart.sh

# 2. 启动应用
./start.sh

# 3. 停止应用
./stop.sh

# 4. 重启应用
./restart.sh
```

---

## 📋 部署前准备

### 1. 服务器要求
- **操作系统**: CentOS 7+, Ubuntu 18.04+, Debian 9+
- **内存**: 至少 2GB（推荐 4GB+）
- **磁盘**: 至少 10GB 可用空间
- **CPU**: 2核心+

### 2. 必需软件
- **宝塔Linux面板** 7.x+
- **JDK 21**
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **Nginx**（可选，用于反向代理）

### 3. 数据库准备
```sql
CREATE DATABASE cloud_notes_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'cloud_notes_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON cloud_notes_db.* TO 'cloud_notes_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## 🔐 环境变量配置

编辑 `.env.prod` 文件：

```properties
# 数据库配置
DB_USERNAME=cloud_notes_user
DB_PASSWORD=your_database_password

# JWT密钥（使用 openssl rand -base64 64 生成）
JWT_SECRET=your_jwt_secret_key

# Redis密码
REDIS_PASSWORD=your_redis_password
```

---

## 🌐 Nginx配置（可选）

```bash
# 1. 复制配置文件
cp nginx.conf.example /etc/nginx/conf.d/cloud-notes.conf

# 2. 修改域名
vim /etc/nginx/conf.d/cloud-notes.conf
# 将 your-domain.com 改为你的实际域名

# 3. 测试配置
nginx -t

# 4. 重启Nginx
systemctl restart nginx
```

---

## 📊 常用命令

### systemd服务管理
```bash
# 启动服务
systemctl start cloud-notes

# 停止服务
systemctl stop cloud-notes

# 重启服务
systemctl restart cloud-notes

# 查看状态
systemctl status cloud-notes

# 查看日志
journalctl -u cloud-notes -f

# 开机自启
systemctl enable cloud-notes
```

### 脚本管理
```bash
# 启动
./start.sh

# 停止
./stop.sh

# 重启
./restart.sh

# 查看日志
tail -f logs/app.log
```

---

## 🔍 验证部署

### 1. 检查服务状态
```bash
systemctl status cloud-notes
```

### 2. 检查端口
```bash
netstat -tlnp | grep 8080
```

### 3. 测试API
```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 通过域名访问
curl http://your-domain.com/actuator/health
```

### 4. 访问Swagger文档
浏览器访问: `http://your-domain.com/swagger-ui.html`

---

## 🐛 故障排查

### 应用无法启动
```bash
# 查看详细日志
journalctl -u cloud-notes -n 100 --no-pager

# 或查看应用日志
tail -n 100 /www/wwwroot/cloud-notes/logs/app.log
```

### 端口被占用
```bash
# 查看占用8080端口的进程
lsof -i :8080

# 杀死进程
kill -9 <PID>
```

### 数据库连接失败
```bash
# 检查MySQL服务
systemctl status mysql

# 测试数据库连接
mysql -u cloud_notes_user -p cloud_notes_db
```

### Redis连接失败
```bash
# 检查Redis服务
systemctl status redis

# 测试Redis连接
redis-cli -a your_password ping
```

---

## 📚 更多信息

详细的部署步骤和配置说明，请查看：
- **完整部署指南**: `docs/宝塔Linux部署指南.md`
- **数据库迁移**: `docs/04-Flyway数据库迁移指南.md`

---

## 📞 技术支持

如遇到问题，请检查：
1. 应用日志: `/www/wwwroot/cloud-notes/logs/app.log`
2. 系统日志: `journalctl -u cloud-notes -f`
3. Nginx日志: `/www/wwwlogs/cloud-notes-error.log`

---

**祝部署顺利！** 🎉
