#!/bin/bash

# ========================================
# Cloud Notes 应用停止脚本
# ========================================

APP_NAME="cloud-notes"
APP_HOME="/www/wwwroot/cloud-notes"
PID_FILE="$APP_HOME/app.pid"

echo "========================================"
echo "停止 $APP_NAME"
echo "========================================"

# 检查PID文件是否存在
if [ ! -f "$PID_FILE" ]; then
    echo "应用未运行（PID文件不存在）"
    exit 0
fi

# 读取PID
PID=$(cat $PID_FILE)

# 检查进程是否存在
if ! ps -p $PID > /dev/null 2>&1; then
    echo "应用未运行（进程不存在）"
    rm -f $PID_FILE
    exit 0
fi

echo "正在停止应用，PID: $PID"

# 优雅关闭（发送SIGTERM信号）
kill $PID

# 等待进程结束（最多等待30秒）
for i in {1..30}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "✓ 应用已停止"
        rm -f $PID_FILE
        exit 0
    fi
    echo -n "."
    sleep 1
done

echo ""
echo "应用未能在30秒内停止，强制终止..."

# 强制终止（发送SIGKILL信号）
kill -9 $PID

sleep 1

if ! ps -p $PID > /dev/null 2>&1; then
    echo "✓ 应用已强制停止"
    rm -f $PID_FILE
    exit 0
else
    echo "✗ 无法停止应用"
    exit 1
fi
