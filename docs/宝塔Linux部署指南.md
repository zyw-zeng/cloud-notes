# 宝塔Linux面板部署指南

## 📋 项目信息
- **项目名称**: cloud-notes
- **技术栈**: Spring Boot 3.4.1 + MySQL + Redis
- **Java版本**: JDK 21
- **构建工具**: Maven

---

## 🚀 部署步骤

### 一、服务器环境准备

#### 1. 安装宝塔Linux面板
```bash
# CentOS/RHEL
yum install -y wget && wget -O install.sh https://download.bt.cn/install/install_6.0.sh && sh install.sh

# Ubuntu/Debian
wget -O install.sh https://download.bt.cn/install/install-ubuntu_6.0.sh && sudo bash install.sh
```

#### 2. 登录宝塔面板
- 安装完成后，记录面板地址、用户名和密码
- 浏览器访问: `http://你的服务器IP:8888`
- 首次登录需要绑定宝塔账号

#### 3. 安装必要软件（通过宝塔面板）
在宝塔面板 → 软件商店 → 安装以下软件：

- **Java项目管理器** (或手动安装JDK 21)
- **MySQL 8.0+**
- **Redis**
- **Nginx** (可选，用于反向代理)
- **PM2管理器** (可选，用于进程管理)

---

### 二、手动安装JDK 21（如果宝塔没有）

```bash
# 1. 下载JDK 21
cd /usr/local
wget https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz

# 2. 解压
tar -zxvf jdk-21_linux-x64_bin.tar.gz
mv jdk-21* jdk-21

# 3. 配置环境变量
vim /etc/profile

# 添加以下内容：
export JAVA_HOME=/usr/local/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar

# 4. 使配置生效
source /etc/profile

# 5. 验证安装
java -version
```

---

### 三、安装Maven

```bash
# 1. 下载Maven
cd /usr/local
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz

# 2. 解压
tar -zxvf apache-maven-3.9.6-bin.tar.gz
mv apache-maven-3.9.6 maven

# 3. 配置环境变量
vim /etc/profile

# 添加：
export MAVEN_HOME=/usr/local/maven
export PATH=$MAVEN_HOME/bin:$PATH

# 4. 使配置生效
source /etc/profile

# 5. 验证
mvn -version
```

---

### 四、配置MySQL数据库

#### 1. 通过宝塔面板创建数据库
- 进入宝塔面板 → 数据库 → 添加数据库
- **数据库名**: `cloud_notes_db`
- **用户名**: `cloud_notes_user`
- **密码**: 设置强密码（记录下来）
- **访问权限**: 本地服务器（127.0.0.1）

#### 2. 或通过命令行创建
```bash
mysql -u root -p

CREATE DATABASE cloud_notes_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'cloud_notes_user'@'localhost' IDENTIFIED BY '你的强密码';
GRANT ALL PRIVILEGES ON cloud_notes_db.* TO 'cloud_notes_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

---

### 五、配置Redis

#### 1. 通过宝塔面板配置
- 进入宝塔面板 → Redis → 设置
- 设置密码（推荐）
- 确保Redis已启动

#### 2. 或通过命令行配置
```bash
# 编辑Redis配置
vim /etc/redis.conf

# 设置密码（找到requirepass行，取消注释并设置密码）
requirepass 你的Redis密码

# 重启Redis
systemctl restart redis
```

---

### 六、部署应用

#### 1. 创建应用目录
```bash
mkdir -p /www/wwwroot/cloud-notes
cd /www/wwwroot/cloud-notes
```

#### 2. 上传项目文件
**方法一：使用宝塔面板上传**
- 进入宝塔面板 → 文件 → 进入 `/www/wwwroot/cloud-notes`
- 上传整个项目文件夹（或压缩包后上传再解压）

**方法二：使用Git**
```bash
cd /www/wwwroot/cloud-notes
git clone 你的项目仓库地址 .
```

**方法三：使用SCP/SFTP**
```bash
# 在本地电脑执行
scp -r f:\web_java\cloud-notes root@你的服务器IP:/www/wwwroot/
```

#### 3. 配置生产环境变量
```bash
cd /www/wwwroot/cloud-notes

# 创建生产环境配置文件
vim .env.prod
```

添加以下内容：
```properties
# 数据库配置
DB_USERNAME=cloud_notes_user
DB_PASSWORD=你的数据库密码

# JWT 配置（生产环境必须使用强密钥）
JWT_SECRET=你的JWT密钥

# Redis 配置
REDIS_PASSWORD=你的Redis密码
```

**生成强JWT密钥：**
```bash
openssl rand -base64 64
```

#### 4. 修改生产环境配置
编辑 `src/main/resources/application-prod.yml`：
```bash
vim src/main/resources/application-prod.yml
```

确保数据库和Redis配置正确：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cloud_notes_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}
```

#### 5. 构建项目
```bash
cd /www/wwwroot/cloud-notes

# 清理并打包（跳过测试）
mvn clean package -DskipTests

# 打包后的jar文件位置：
# target/cloud-notes-0.0.1-SNAPSHOT.jar
```

---

### 七、启动应用

#### 方法一：使用启动脚本（推荐）

创建启动脚本：
```bash
vim /www/wwwroot/cloud-notes/start.sh
```

添加以下内容：
```bash
#!/bin/bash

# 加载环境变量
export $(cat /www/wwwroot/cloud-notes/.env.prod | xargs)

# 设置JVM参数
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 启动应用
nohup java $JAVA_OPTS \
  -Dspring.profiles.active=prod \
  -jar /www/wwwroot/cloud-notes/target/cloud-notes-0.0.1-SNAPSHOT.jar \
  > /www/wwwroot/cloud-notes/logs/app.log 2>&1 &

echo $! > /www/wwwroot/cloud-notes/app.pid
echo "应用已启动，PID: $(cat /www/wwwroot/cloud-notes/app.pid)"
```

赋予执行权限并启动：
```bash
chmod +x /www/wwwroot/cloud-notes/start.sh
./start.sh
```

#### 方法二：使用systemd服务（推荐生产环境）

创建服务文件：
```bash
vim /etc/systemd/system/cloud-notes.service
```

添加以下内容：
```ini
[Unit]
Description=Cloud Notes Application
After=network.target mysql.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/www/wwwroot/cloud-notes
EnvironmentFile=/www/wwwroot/cloud-notes/.env.prod
ExecStart=/usr/local/jdk-21/bin/java -Xms512m -Xmx1024m -XX:+UseG1GC -Dspring.profiles.active=prod -jar /www/wwwroot/cloud-notes/target/cloud-notes-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=append:/www/wwwroot/cloud-notes/logs/app.log
StandardError=append:/www/wwwroot/cloud-notes/logs/error.log

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
# 重新加载systemd配置
systemctl daemon-reload

# 启动服务
systemctl start cloud-notes

# 设置开机自启
systemctl enable cloud-notes

# 查看状态
systemctl status cloud-notes

# 查看日志
journalctl -u cloud-notes -f
```

---

### 八、配置Nginx反向代理（可选但推荐）

#### 1. 通过宝塔面板配置
- 进入宝塔面板 → 网站 → 添加站点
- **域名**: 你的域名（如 notes.example.com）
- **根目录**: 任意（不使用）
- 创建后，点击站点设置 → 反向代理

添加反向代理：
```
代理名称: cloud-notes
目标URL: http://127.0.0.1:8080
发送域名: $host
```

#### 2. 或手动配置Nginx
```bash
vim /etc/nginx/conf.d/cloud-notes.conf
```

添加以下内容：
```nginx
server {
    listen 80;
    server_name 你的域名或IP;

    # 日志
    access_log /www/wwwlogs/cloud-notes-access.log;
    error_log /www/wwwlogs/cloud-notes-error.log;

    # 反向代理到Spring Boot应用
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # 静态资源缓存
    location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
        proxy_pass http://127.0.0.1:8080;
        expires 30d;
    }
}
```

重启Nginx：
```bash
nginx -t
systemctl restart nginx
```

---

### 九、配置防火墙

#### 1. 通过宝塔面板
- 进入宝塔面板 → 安全
- 放行端口：80, 443（如果使用HTTPS）
- 如果需要直接访问应用：放行 8080

#### 2. 或使用firewalld
```bash
# 放行HTTP/HTTPS
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https

# 或放行8080端口（如果不使用Nginx）
firewall-cmd --permanent --add-port=8080/tcp

# 重载防火墙
firewall-cmd --reload
```

---

### 十、验证部署

#### 1. 检查应用状态
```bash
# 查看进程
ps aux | grep cloud-notes

# 查看端口
netstat -tlnp | grep 8080

# 查看日志
tail -f /www/wwwroot/cloud-notes/logs/app.log
```

#### 2. 测试API
```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 或通过域名
curl http://你的域名/actuator/health
```

#### 3. 访问Swagger文档
浏览器访问: `http://你的域名/swagger-ui.html`

---

## 🔧 常用管理命令

### 应用管理（systemd方式）
```bash
# 启动
systemctl start cloud-notes

# 停止
systemctl stop cloud-notes

# 重启
systemctl restart cloud-notes

# 查看状态
systemctl status cloud-notes

# 查看日志
journalctl -u cloud-notes -f
```

### 应用管理（脚本方式）
```bash
# 停止应用
kill $(cat /www/wwwroot/cloud-notes/app.pid)

# 启动应用
/www/wwwroot/cloud-notes/start.sh

# 查看日志
tail -f /www/wwwroot/cloud-notes/logs/app.log
```

### 数据库管理
```bash
# 备份数据库
mysqldump -u cloud_notes_user -p cloud_notes_db > backup_$(date +%Y%m%d).sql

# 恢复数据库
mysql -u cloud_notes_user -p cloud_notes_db < backup_20240109.sql
```

---

## 🔒 安全建议

### 1. 修改默认端口
- 修改宝塔面板默认端口（8888）
- 修改SSH端口（22）

### 2. 配置SSL证书（HTTPS）
- 在宝塔面板 → 网站 → SSL → Let's Encrypt
- 申请免费SSL证书并开启强制HTTPS

### 3. 定期备份
- 在宝塔面板 → 计划任务 → 添加备份任务
- 备份数据库和应用文件

### 4. 更新系统和软件
```bash
# CentOS/RHEL
yum update -y

# Ubuntu/Debian
apt update && apt upgrade -y
```

### 5. 配置日志轮转
```bash
vim /etc/logrotate.d/cloud-notes
```

添加：
```
/www/wwwroot/cloud-notes/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 root root
}
```

---

## 📊 监控和日志

### 1. 查看应用日志
```bash
# 实时查看
tail -f /www/wwwroot/cloud-notes/logs/app.log

# 查看错误日志
tail -f /www/wwwroot/cloud-notes/logs/error.log

# 查看最近100行
tail -n 100 /www/wwwroot/cloud-notes/logs/app.log
```

### 2. 监控系统资源
```bash
# CPU和内存
top

# 磁盘使用
df -h

# 查看Java进程资源占用
jps -l
jstat -gc <pid>
```

### 3. 使用宝塔面板监控
- 宝塔面板 → 监控 → 查看CPU、内存、磁盘、网络使用情况

---

## ❓ 常见问题

### 1. 应用无法启动
- 检查Java版本: `java -version`
- 检查端口占用: `netstat -tlnp | grep 8080`
- 查看日志: `tail -f logs/app.log`

### 2. 数据库连接失败
- 检查MySQL服务: `systemctl status mysql`
- 验证数据库配置和密码
- 检查防火墙规则

### 3. Redis连接失败
- 检查Redis服务: `systemctl status redis`
- 验证Redis密码配置
- 测试连接: `redis-cli -a 你的密码 ping`

### 4. 内存不足
- 调整JVM参数（减小 -Xmx 值）
- 增加服务器内存
- 优化应用代码

### 5. 更新应用
```bash
# 1. 停止应用
systemctl stop cloud-notes

# 2. 备份当前版本
cp target/cloud-notes-0.0.1-SNAPSHOT.jar target/cloud-notes-backup.jar

# 3. 拉取最新代码
git pull

# 4. 重新构建
mvn clean package -DskipTests

# 5. 启动应用
systemctl start cloud-notes
```

---

## 📝 部署检查清单

- [ ] JDK 21 已安装并配置
- [ ] Maven 已安装并配置
- [ ] MySQL 数据库已创建
- [ ] Redis 已安装并配置
- [ ] 项目文件已上传
- [ ] 生产环境变量已配置（.env.prod）
- [ ] 应用已成功构建（mvn package）
- [ ] 应用已启动并运行
- [ ] 防火墙端口已放行
- [ ] Nginx反向代理已配置（可选）
- [ ] SSL证书已配置（推荐）
- [ ] 定期备份已设置
- [ ] 应用可以正常访问
- [ ] API接口测试通过

---

## 📞 技术支持

如遇到问题，请检查：
1. 应用日志: `/www/wwwroot/cloud-notes/logs/app.log`
2. 系统日志: `journalctl -u cloud-notes -f`
3. Nginx日志: `/www/wwwlogs/cloud-notes-error.log`

---

**部署完成！** 🎉

访问地址: `http://你的域名` 或 `http://你的IP:8080`
Swagger文档: `http://你的域名/swagger-ui.html`
