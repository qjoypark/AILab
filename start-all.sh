#!/bin/bash

echo "========================================="
echo "智慧实验室管理系统 - 启动脚本"
echo "========================================="

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "错误: Docker未运行，请先启动Docker"
    exit 1
fi

# 启动基础设施
echo ""
echo "1. 启动基础设施 (MySQL, Redis, RabbitMQ, MinIO, Nacos)..."
docker-compose up -d

echo "等待服务启动完成..."
sleep 30

# 检查MySQL是否就绪
echo ""
echo "2. 检查MySQL连接..."
until docker exec lab-mysql mysqladmin ping -h localhost -u root -proot --silent; do
    echo "等待MySQL启动..."
    sleep 5
done
echo "MySQL已就绪"

# 检查Nacos是否就绪
echo ""
echo "3. 检查Nacos连接..."
until curl -s http://localhost:8848/nacos > /dev/null; do
    echo "等待Nacos启动..."
    sleep 5
done
echo "Nacos已就绪"

echo ""
echo "========================================="
echo "基础设施启动完成！"
echo "========================================="
echo ""
echo "服务访问地址:"
echo "- MySQL: localhost:3306 (用户名: root, 密码: root)"
echo "- Redis: localhost:6379"
echo "- RabbitMQ管理界面: http://localhost:15672 (guest/guest)"
echo "- MinIO控制台: http://localhost:9001 (minioadmin/minioadmin)"
echo "- Nacos控制台: http://localhost:8848/nacos (nacos/nacos)"
echo ""
echo "========================================="
echo "提示: 现在可以启动微服务应用"
echo "========================================="
echo ""
echo "启动命令:"
echo "  cd lab-gateway && mvn spring-boot:run"
echo "  cd lab-service-user && mvn spring-boot:run"
echo "  cd lab-service-material && mvn spring-boot:run"
echo "  cd lab-service-inventory && mvn spring-boot:run"
echo "  cd lab-service-approval && mvn spring-boot:run"
echo ""
