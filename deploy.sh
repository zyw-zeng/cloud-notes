#!/bin/bash

# ========================================
# Cloud Notes 一键部署脚本
# ========================================
# 此脚本用于在服务器上快速部署应用
# 使用方法: bash deploy.sh
# ========================================

set -e  # 遇到错误立即退出

APP_NAME="cloud-notes"
APP_HOME="/www/wwwroot/cloud-notes"
SERVICE_NAME="cloud-notes.service"

echo "========================================"
echo "Cloud Notes 一键部署脚本"
echo "========================================"

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then 
    echo "请使用root用户运行此脚本"
    exit 1
fi

# 1. 检查Java环境
echo ""
echo "[1/8] 检查Java环境..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    echo "✓ Java已安装，版本: $(java -version 2>&1 | head -n 1)"
    if [ "$JAVA_VERSION" -lt 21 ]; then
        echo "⚠ 警告: 需要Java 21或更高版本"
    fi
else
    echo "✗ Java未安装，请先安装JDK 21"
    exit 1
fi

# 2. 检查Maven环境
echo ""
echo "[2/8] 检查Maven环境..."
if command -v mvn &> /dev/null; then
    echo "✓ Maven已安装: $(mvn -version | head -n 1)"
else
    echo "✗ Maven未安装，请先安装Maven"
    exit 1
fi

# 3. 检查MySQL
echo ""
echo "[3/8] 检查MySQL服务..."
if systemctl is-active --quiet mysql || systemctl is-active --quiet mysqld; then
    echo "✓ MySQL服务运行中"
else
    echo "⚠ MySQL服务未运行，请启动MySQL"
fi

# 4. 检查Redis
echo ""
echo "[4/8] 检查Redis服务..."
if systemctl is-active --quiet redis; then
    echo "✓ Redis服务运行中"
else
    echo "⚠ Redis服务未运行，请启动Redis"
fi

# 5. 检查环境变量文件
echo ""
echo "[5/8] 检查环境变量配置..."
if [ ! -f "$APP_HOME/.env.prod" ]; then
    echo "⚠ 生产环境配置文件不存在"
    if [ -f "$APP_HOME/.env.prod.example" ]; then
        echo "正在创建 .env.prod 文件..."
        cp $APP_HOME/.env.prod $APP_HOME/.env.prod
        chmod 600 $APP_HOME/.env.prod
        echo "✓ 已创建 .env.prod，请编辑此文件并填写正确的配置"
        echo "编辑命令: vim $APP_HOME/.env.prod"
        read -p "配置完成后按Enter继续..."
    else
        echo "✗ 找不到 .env.prod.example 文件"
        exit 1
    fi
else
    echo "✓ 环境变量文件已存在"
fi

# 6. 构建应用
echo ""
echo "[6/8] 构建应用..."
cd $APP_HOME
echo "执行: mvn clean package -DskipTests"
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✓ 应用构建成功"
else
    echo "✗ 应用构建失败"
    exit 1
fi

# 7. 安装systemd服务
echo ""
echo "[7/8] 配置systemd服务..."
if [ -f "$APP_HOME/cloud-notes.service" ]; then
    # 停止旧服务（如果存在）
    if systemctl is-active --quiet cloud-notes; then
        echo "停止旧服务..."
        systemctl stop cloud-notes
    fi
    
    # 复制服务文件
    cp $APP_HOME/cloud-notes.service /etc/systemd/system/
    
    # 重新加载systemd
    systemctl daemon-reload
    
    # 启用服务
    systemctl enable cloud-notes
    
    echo "✓ systemd服务已配置"
else
    echo "⚠ 未找到 cloud-notes.service 文件，跳过systemd配置"
fi

# 8. 启动应用
echo ""
echo "[8/8] 启动应用..."
read -p "是否现在启动应用？(y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    systemctl start cloud-notes
    sleep 3
    
    if systemctl is-active --quiet cloud-notes; then
        echo "✓ 应用启动成功"
        echo ""
        echo "========================================"
        echo "部署完成！"
        echo "========================================"
        echo "应用状态: systemctl status cloud-notes"
        echo "查看日志: journalctl -u cloud-notes -f"
        echo "停止应用: systemctl stop cloud-notes"
        echo "重启应用: systemctl restart cloud-notes"
        echo ""
        echo "访问地址: http://$(hostname -I | awk '{print $1}'):8080"
        echo "Swagger文档: http://$(hostname -I | awk '{print $1}'):8080/swagger-ui.html"
        echo "========================================"
    else
        echo "✗ 应用启动失败，请查看日志"
        journalctl -u cloud-notes -n 50
        exit 1
    fi
else
    echo "跳过启动，稍后可手动启动: systemctl start cloud-notes"
fi

echo ""
echo "✓ 部署脚本执行完成"
