#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ -f "$ENV_FILE" ]; then
    export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

echo "=========================================="
echo "  个人书单互换借阅平台 - 启动中"
echo "=========================================="
echo ""

echo "[1/4] 检查 Docker 环境..."
if ! command -v docker &> /dev/null; then
    echo "错误: 未检测到 Docker，请先安装 Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "错误: 未检测到 Docker Compose，请先安装 Docker Compose"
    exit 1
fi
echo "  ✓ Docker 环境正常"
echo ""

echo "[2/4] 构建并启动服务..."
cd "$SCRIPT_DIR"
docker compose up --build -d
echo "  ✓ 服务启动完成"
echo ""

echo "[3/4] 等待服务就绪..."
MAX_RETRIES=60
RETRY=0
while [ $RETRY -lt $MAX_RETRIES ]; do
    if docker compose ps frontend &>/dev/null && \
       docker compose ps backend &>/dev/null && \
       docker compose ps mysql &>/dev/null && \
       docker compose ps redis &>/dev/null; then
        
        FRONTEND_STATUS=$(docker inspect --format='{{.State.Status}}' "${PROJECT_NAME}-frontend" 2>/dev/null || echo "unknown")
        BACKEND_STATUS=$(docker inspect --format='{{.State.Status}}' "${PROJECT_NAME}-backend" 2>/dev/null || echo "unknown")
        MYSQL_STATUS=$(docker inspect --format='{{.State.Status}}' "${PROJECT_NAME}-mysql" 2>/dev/null || echo "unknown")
        REDIS_STATUS=$(docker inspect --format='{{.State.Status}}' "${PROJECT_NAME}-redis" 2>/dev/null || echo "unknown")
        
        MYSQL_HEALTH=$(docker inspect --format='{{.State.Health.Status}}' "${PROJECT_NAME}-mysql" 2>/dev/null || echo "unknown")
        REDIS_HEALTH=$(docker inspect --format='{{.State.Health.Status}}' "${PROJECT_NAME}-redis" 2>/dev/null || echo "unknown")
        
        if [ "$FRONTEND_STATUS" = "running" ] && \
           [ "$BACKEND_STATUS" = "running" ] && \
           [ "$MYSQL_STATUS" = "running" ] && \
           [ "$REDIS_STATUS" = "running" ] && \
           [ "$MYSQL_HEALTH" = "healthy" ] && \
           [ "$REDIS_HEALTH" = "healthy" ]; then
            echo "  ✓ 所有服务就绪"
            break
        fi
    fi
    
    RETRY=$((RETRY + 1))
    echo -n "  等待中... ($RETRY/$MAX_RETRIES) "
    echo -ne "\r"
    sleep 2
done
echo ""
echo ""

echo "[4/4] 服务状态汇总..."
echo ""
docker compose ps
echo ""

echo "=========================================="
echo "  🎉  项目启动成功！"
echo "=========================================="
echo ""
echo "  🌐 前端访问地址:  http://localhost:${FRONTEND_PORT}"
echo "  🔧 后端API地址:   http://localhost:${BACKEND_PORT}/api"
echo "  🗄️  MySQL端口:    ${MYSQL_PORT}"
echo "  📦 Redis端口:     ${REDIS_PORT}"
echo ""
echo "  停止服务: docker compose down"
echo "  查看日志: docker compose logs -f"
echo ""
echo "=========================================="
