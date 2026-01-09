#!/bin/bash

# ========================================
# Cloud Notes 应用重启脚本
# ========================================

APP_HOME="/www/wwwroot/cloud-notes"

echo "========================================"
echo "重启 Cloud Notes 应用"
echo "========================================"

# 停止应用
if [ -f "$APP_HOME/stop.sh" ]; then
    bash $APP_HOME/stop.sh
else
    echo "错误: stop.sh 脚本不存在"
    exit 1
fi

# 等待2秒
sleep 2

# 启动应用
if [ -f "$APP_HOME/start.sh" ]; then
    bash $APP_HOME/start.sh
else
    echo "错误: start.sh 脚本不存在"
    exit 1
fi
