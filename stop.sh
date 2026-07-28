#!/usr/bin/env bash
# MockPilot 一键停止（Linux / macOS）
set -e
cd "$(dirname "$0")"

echo "停止 MockPilot 全部服务（数据卷保留）..."
docker compose down

echo "已停止。如需连数据一起清空，执行: docker compose down -v"
