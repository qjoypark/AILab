@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul

echo =========================================
echo Smart Lab Management System - Start Script
echo =========================================

echo.
echo 1. Starting infrastructure (MySQL, Redis, RabbitMQ, MinIO, Nacos)...
docker-compose up -d
if errorlevel 1 (
  echo.
  echo ERROR: docker-compose start failed.
  exit /b 1
)

echo.
echo 2. Waiting for Nacos to become healthy...
set "NACOS_READY=0"
for /L %%i in (1,1,120) do (
  for /f "delims=" %%a in ('curl -s --max-time 2 http://localhost:8848/nacos/v1/ns/operator/metrics 2^>nul') do (
    echo %%a | findstr /C:"\"status\":\"UP\"" >nul && set "NACOS_READY=1"
  )
  if "!NACOS_READY!"=="1" goto nacos_ready
  echo [%%i/120] Waiting Nacos...
  timeout /t 2 /nobreak >nul
)

echo.
echo ERROR: Nacos is not healthy after 240 seconds.
echo Please run: docker logs -f lab-nacos
exit /b 1

:nacos_ready
echo Nacos is healthy.

echo.
echo =========================================
echo Infrastructure Ready
echo =========================================
echo.
echo Service addresses:
echo - MySQL: localhost:3306 (root/root)
echo - Redis: localhost:6379
echo - RabbitMQ: http://localhost:15672 (guest/guest)
echo - MinIO: http://localhost:9001 (minioadmin/minioadmin)
echo - Nacos: http://localhost:8848/nacos (nacos/nacos)
echo.
echo Start microservices:
echo   cd lab-gateway ^&^& mvn spring-boot:run
echo   cd lab-service-user ^&^& mvn spring-boot:run
echo   cd lab-service-material ^&^& mvn spring-boot:run
echo   cd lab-service-inventory ^&^& mvn spring-boot:run
echo   cd lab-service-approval ^&^& mvn spring-boot:run
echo.
pause
