#!/bin/bash

# 腾讯云 All-in-One 一键部署脚本
# 使用方法: sudo bash deploy-tencent.sh

set -e

echo "=========================================="
echo "  智慧实验室管理系统 - 腾讯云部署脚本"
echo "=========================================="

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then 
    echo "请使用 sudo 运行此脚本"
    exit 1
fi

# 1. 安装基础环境
echo "[1/6] 安装基础环境..."
apt update -y
apt install -y docker.io docker-compose openjdk-17-jdk maven git curl

# 启动 Docker
systemctl start docker
systemctl enable docker

# 2. 启动基础设施
echo "[2/6] 启动基础设施（MySQL、Redis、RabbitMQ、MinIO、Nacos）..."
docker-compose up -d

echo "等待基础设施启动（120秒）..."
sleep 120

# 3. 编译项目
echo "[3/6] 编译项目..."
mvn clean package -DskipTests

# 4. 创建部署目录
echo "[4/6] 创建部署目录..."
mkdir -p /opt/lab-deploy

# 5. 创建启动脚本
echo "[5/6] 创建启动脚本..."
cat > /opt/lab-deploy/start-all.sh << 'EOF'
#!/bin/bash
JVM_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC"
BASE_DIR="/opt/lab-system"

echo "启动网关..."
nohup java $JVM_OPTS -jar $BASE_DIR/lab-gateway/target/lab-gateway-1.0.0.jar > /opt/lab-deploy/gateway.log 2>&1 &
echo $! > /opt/lab-deploy/gateway.pid
sleep 10

echo "启动用户服务..."
nohup java $JVM_OPTS -jar $BASE_DIR/lab-service-user/target/lab-service-user-1.0.0.jar > /opt/lab-deploy/user.log 2>&1 &
echo $! > /opt/lab-deploy/user.pid
sleep 5

echo "启动药品服务..."
nohup java $JVM_OPTS -jar $BASE_DIR/lab-service-material/target/lab-service-material-1.0.0.jar > /opt/lab-deploy/material.log 2>&1 &
echo $! > /opt/lab-deploy/material.pid
sleep 5

echo "启动库存服务..."
nohup java $JVM_OPTS -jar $BASE_DIR/lab-service-inventory/target/lab-service-inventory-1.0.0.jar > /opt/lab-deploy/inventory.log 2>&1 &
echo $! > /opt/lab-deploy/inventory.pid
sleep 5

echo "启动审批服务..."
nohup java $JVM_OPTS -jar $BASE_DIR/lab-service-approval/target/lab-service-approval-1.0.0.jar > /opt/lab-deploy/approval.log 2>&1 &
echo $! > /opt/lab-deploy/approval.pid

echo "所有服务已启动！"
EOF

cat > /opt/lab-deploy/stop-all.sh << 'EOF'
#!/bin/bash
echo "停止所有服务..."
for pid_file in /opt/lab-deploy/*.pid; do
    if [ -f "$pid_file" ]; then
        pid=$(cat "$pid_file")
        kill $pid 2>/dev/null && echo "已停止进程 $pid"
        rm "$pid_file"
    fi
done
echo "所有服务已停止！"
EOF

cat > /opt/lab-deploy/status.sh << 'EOF'
#!/bin/bash
echo "========== 服务状态 =========="
for pid_file in /opt/lab-deploy/*.pid; do
    if [ -f "$pid_file" ]; then
        service_name=$(basename "$pid_file" .pid)
        pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null; then
            echo "✓ $service_name (PID: $pid) - 运行中"
        else
            echo "✗ $service_name - 已停止"
        fi
    fi
done
echo ""
echo "========== Docker 服务 =========="
docker-compose ps
EOF

chmod +x /opt/lab-deploy/*.sh

# 6. 启动服务
echo "[6/6] 启动所有服务..."
cd $(dirname $0)
/opt/lab-deploy/start-all.sh

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "访问地址："
echo "  - 应用网关: http://$(curl -s ifconfig.me):8080"
echo "  - Nacos: http://$(curl -s ifconfig.me):8848/nacos (nacos/nacos)"
echo "  - RabbitMQ: http://$(curl -s ifconfig.me):15672 (guest/guest)"
echo "  - MinIO: http://$(curl -s ifconfig.me):9001 (minioadmin/minioadmin)"
echo ""
echo "常用命令："
echo "  - 查看状态: /opt/lab-deploy/status.sh"
echo "  - 停止服务: /opt/lab-deploy/stop-all.sh"
echo "  - 启动服务: /opt/lab-deploy/start-all.sh"
echo "  - 查看日志: tail -f /opt/lab-deploy/gateway.log"
echo ""
echo "注意：请在腾讯云安全组中开放以下端口："
echo "  - 8080 (应用网关)"
echo "  - 8848 (Nacos，可选)"
echo "  - 15672 (RabbitMQ，可选)"
echo "  - 9001 (MinIO，可选)"
echo ""
