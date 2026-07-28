@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ============================================
echo  MockPilot 一键启动（Docker Compose）
echo ============================================

docker info >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker 未运行，请先启动 Docker Desktop。
    pause
    exit /b 1
)

rem 首次使用需先配置 .env（含大模型 / 讯飞密钥）
if not exist ".env" (
    copy .env.example .env >nul
    echo [提示] 未找到 .env，已从 .env.example 生成。
    echo        请编辑 .env 填入 DashScope / 讯飞等密钥后重新执行本脚本。
    pause
    exit /b 1
)

echo [1/2] 构建并启动全部服务（首次构建需要几分钟）...
docker compose up -d --build
if errorlevel 1 (
    echo [错误] 启动失败，请检查上方日志。
    pause
    exit /b 1
)

echo [2/2] 服务状态：
docker compose ps

echo.
echo ============================================
echo  启动完成！
echo    前端:       http://localhost
echo    后端 API:   http://localhost:8002/actuator/health
echo  首次启动后端需等待数据库就绪，约 1-2 分钟。
echo  查看日志:  docker compose logs -f backend
echo ============================================
pause
