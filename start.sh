#!/usr/bin/env bash
# MockPilot 一键启动（Linux / macOS）
# 本仓库即部署入口：前端镜像默认直接从 GitHub 仓库远程构建，无需手动克隆
set -e
cd "$(dirname "$0")"

echo "============================================"
echo " MockPilot 一键启动（Docker Compose）"
echo "============================================"

if ! docker info >/dev/null 2>&1; then
    echo "[错误] Docker 未运行或当前用户无权限（可尝试 sudo，或将用户加入 docker 组）。"
    exit 1
fi

# Elasticsearch 需要 vm.max_map_count >= 262144
if [ "$(uname -s)" = "Linux" ]; then
    current=$(sysctl -n vm.max_map_count 2>/dev/null || echo 0)
    if [ "$current" -lt 262144 ]; then
        echo "[提示] vm.max_map_count=$current 过低，Elasticsearch 可能启动失败。"
        echo "       请先执行: sudo sysctl -w vm.max_map_count=262144"
        echo "       持久化:   echo 'vm.max_map_count=262144' | sudo tee -a /etc/sysctl.conf"
    fi
fi

# 首次使用需先配置 .env（含大模型 / 讯飞密钥）
if [ ! -f ".env" ]; then
    cp .env.example .env
    echo "[提示] 未找到 .env，已从 .env.example 生成。"
    echo "       请编辑 .env 填入 DashScope / 讯飞等密钥后重新执行本脚本。"
    exit 1
fi

echo "[1/2] 构建并启动全部服务（首次构建需要几分钟）..."
docker compose up -d --build

echo "[2/2] 服务状态："
docker compose ps

env_val() { grep -E "^$1=" .env 2>/dev/null | head -n1 | cut -d= -f2-; }

# 读取 .env 中的实际端口（缺省 80 / 8002）
FRONTEND_PORT=$(env_val FRONTEND_PORT)
FRONTEND_PORT=${FRONTEND_PORT:-80}
BACKEND_PORT=$(env_val BACKEND_PORT)
BACKEND_PORT=${BACKEND_PORT:-8002}

# 探测本机局域网 IP（虚拟机/服务器部署时从外部用此地址访问）
LAN_IP=$(hostname -I 2>/dev/null | awk '{print $1}')
[ -z "$LAN_IP" ] && LAN_IP=$(ip route get 1.1.1.1 2>/dev/null | awk '{for(i=1;i<=NF;i++) if($i=="src") print $(i+1)}' | head -n1)

front_url() { [ "$FRONTEND_PORT" = "80" ] && echo "http://$1" || echo "http://$1:$FRONTEND_PORT"; }

echo ""
echo "============================================"
echo " 启动完成！"
echo "   本机访问:   $(front_url localhost)"
if [ -n "$LAN_IP" ]; then
    echo "   外部访问:   $(front_url "$LAN_IP")   （虚拟机/服务器部署用这个）"
fi
echo "   后端健康:   http://${LAN_IP:-localhost}:${BACKEND_PORT}/actuator/health"
echo " 首次启动后端需等待数据库就绪，约 1-2 分钟。"
echo " 查看日志:  docker compose logs -f backend"
echo "============================================"
