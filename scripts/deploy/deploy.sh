#!/bin/bash

# 部署脚本 - 构建前端和后端并打包
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR" || exit 1

echo "========================================="
echo "开始构建 DGA 平台..."
echo "========================================="

# 1. 构建前端
echo "[1/4] 构建前端项目..."
cd dga-frontend
if [ ! -d "node_modules" ]; then
    echo "安装前端依赖..."
    npm install
fi
echo "编译前端资源..."
npm run build

if [ $? -ne 0 ]; then
    echo "前端构建失败！"
    exit 1
fi
cd "$ROOT_DIR" || exit 1

# 2. 准备静态资源
echo "[2/4] 迁移前端资源到后端..."
STATIC_DIR="dga-backend/src/main/resources/static"

# 确保目录存在
mkdir -p $STATIC_DIR

# 清理旧资源 (保留目录结构)
rm -rf $STATIC_DIR/*

# 复制新构建的资源
cp -r dga-frontend/dist/* $STATIC_DIR/

echo "前端资源已复制到 $STATIC_DIR"

# 3. 构建后端
echo "[3/4] 构建后端 JAR 包..."
cd dga-backend
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "后端构建失败！"
    exit 1
fi
cd "$ROOT_DIR" || exit 1

# 4. 完成
echo "========================================="
echo "构建成功！"
echo "========================================="
echo "JAR 包位置: dga-backend/target/dga-backend-0.0.1-SNAPSHOT.jar"
echo ""
echo "启动命令 (测试环境):"
echo "java -jar dga-backend/target/dga-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=test"
echo ""
echo "后台运行命令:"
echo "nohup java -jar dga-backend/target/dga-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=test > runtime/logs/dga.log 2>&1 &"
echo "========================================="
