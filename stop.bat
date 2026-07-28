@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo 停止 MockPilot 全部服务（数据卷保留）...
docker compose down

echo 已停止。如需连数据一起清空，执行: docker compose down -v
pause
