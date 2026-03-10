#!/bin/bash

# 部署脚本 - 上传 JAR 包并启动
# 用法: ./upload_and_deploy.sh [服务器IP] [服务器用户] [目标路径]
# 示例: ./upload_and_deploy.sh 10.0.20.107 root /opt/dga

SERVER_IP=${1:-"10.0.20.107"}
SERVER_USER=${2:-"root"}
TARGET_DIR=${3:-"/opt/dga"}
JAR_FILE="dga-backend/target/dga-backend-0.0.1-SNAPSHOT.jar"

echo "========================================="
echo "DGA 远程部署脚本"
echo "目标服务器: $SERVER_USER@$SERVER_IP"
echo "目标路径: $TARGET_DIR"
echo "========================================="

# 1. 本地构建
echo "[1/3] 本地构建项目..."
./deploy.sh
if [ $? -ne 0 ]; then
    echo "本地构建失败！"
    exit 1
fi

# 2. 上传文件
echo "[2/3] 上传 JAR 包到服务器..."
ssh $SERVER_USER@$SERVER_IP "mkdir -p $TARGET_DIR"
scp $JAR_FILE $SERVER_USER@$SERVER_IP:$TARGET_DIR/dga-backend.jar

if [ $? -ne 0 ]; then
    echo "上传失败！请检查 SSH 连接。"
    exit 1
fi

# 3. 远程启动
echo "[3/3] 重启远程服务..."
ssh $SERVER_USER@$SERVER_IP << EOF
    cd $TARGET_DIR
    # 停止旧进程
    pkill -f dga-backend.jar || true
    
    # 启动新进程 (测试环境配置)
    nohup java -jar dga-backend.jar --spring.profiles.active=test > dga.log 2>&1 &
    
    echo "服务已启动，日志文件: $TARGET_DIR/dga.log"
    sleep 2
    ps -ef | grep dga-backend.jar | grep -v grep
EOF

echo "========================================="
echo "部署完成！"
echo "========================================="
