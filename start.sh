#!/bin/bash

# ========================================
# Cloud Notes 应用启动脚本
# ========================================

APP_NAME="cloud-notes"
APP_HOME="/www/wwwroot/cloud-notes"
JAR_FILE="$APP_HOME/target/cloud-notes-0.0.1-SNAPSHOT.jar"
PID_FILE="$APP_HOME/app.pid"
LOG_DIR="$APP_HOME/logs"
ENV_FILE="$APP_HOME/.env.prod"

# 创建日志目录
mkdir -p $LOG_DIR

# 检查jar文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "错误: JAR文件不存在: $JAR_FILE"
    echo "请先执行: mvn clean package -DskipTests"
    exit 1
fi

# 检查是否已经在运行
if [ -f "$PID_FILE" ]; then
    PID=$(cat $PID_FILE)
    if ps -p $PID > /dev/null 2>&1; then
        echo "应用已经在运行中，PID: $PID"
        exit 1
    else
        echo "删除过期的PID文件"
        rm -f $PID_FILE
    fi
fi

# 加载环境变量
if [ -f "$ENV_FILE" ]; then
    echo "加载环境变量: $ENV_FILE"
    export $(cat $ENV_FILE | grep -v '^#' | xargs)
else
    echo "警告: 环境变量文件不存在: $ENV_FILE"
    echo "将使用默认配置"
fi

# JVM参数配置
JAVA_OPTS="-Xms512m -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"
JAVA_OPTS="$JAVA_OPTS -XX:MaxGCPauseMillis=200"
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=$LOG_DIR/heapdump.hprof"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"
JAVA_OPTS="$JAVA_OPTS -Duser.timezone=Asia/Shanghai"

# Spring Boot配置
SPRING_OPTS="-Dspring.profiles.active=prod"

echo "========================================"
echo "启动 $APP_NAME"
echo "========================================"
echo "应用目录: $APP_HOME"
echo "JAR文件: $JAR_FILE"
echo "日志目录: $LOG_DIR"
echo "JVM参数: $JAVA_OPTS"
echo "========================================"

# 启动应用
nohup java $JAVA_OPTS $SPRING_OPTS -jar $JAR_FILE \
    > $LOG_DIR/app.log 2>&1 &

# 保存PID
echo $! > $PID_FILE

sleep 2

# 检查是否启动成功
if ps -p $(cat $PID_FILE) > /dev/null 2>&1; then
    echo "✓ 应用启动成功！"
    echo "PID: $(cat $PID_FILE)"
    echo "日志文件: $LOG_DIR/app.log"
    echo ""
    echo "查看日志: tail -f $LOG_DIR/app.log"
    echo "停止应用: ./stop.sh"
else
    echo "✗ 应用启动失败，请查看日志: $LOG_DIR/app.log"
    rm -f $PID_FILE
    exit 1
fi
