@echo off
chcp 65001 >nul
echo =========================================
echo 智慧实验室管理系统 - 启动脚本
echo =========================================

echo.
echo 1. 启动基础设施 (MySQL, Redis, RabbitMQ, MinIO, Nacos)...
docker-compose up -d

echo 等待服务启动完成...
timeout /t 30 /nobreak >nul

echo.
echo =========================================
echo 基础设施启动完成！
echo =========================================
echo.
echo 服务访问地址:
echo - MySQL: localhost:3306 (用户名: root, 密码: root)
echo - Redis: localhost:6379
echo - RabbitMQ管理界面: http://localhost:15672 (guest/guest)
echo - MinIO控制台: http://localhost:9001 (minioadmin/minioadmin)
echo - Nacos控制台: http://localhost:8848/nacos (nacos/nacos)
echo.
echo =========================================
echo 提示: 现在可以启动微服务应用
echo =========================================
echo.
echo 启动命令:
echo   cd lab-gateway ^&^& mvn spring-boot:run
echo   cd lab-service-user ^&^& mvn spring-boot:run
echo   cd lab-service-material ^&^& mvn spring-boot:run
echo   cd lab-service-inventory ^&^& mvn spring-boot:run
echo   cd lab-service-approval ^&^& mvn spring-boot:run
echo.
pause
