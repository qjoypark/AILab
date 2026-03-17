# 腾讯云 All-in-One 测试部署方案

## 服务器配置

### 推荐配置（测试环境）
- 机型：标准型 S5.2XLARGE16（8核16GB）
- 系统盘：50GB SSD
- 数据盘：100GB SSD
- 操作系统：Ubuntu 22.04 LTS
- 带宽：5Mbps（按需调整）
- 地域：根据实际需求选择

### 最低配置（省钱版）
- 机型：标准型 S5.LARGE8（4核8GB）
- 系统盘：50GB
- 数据盘：50GB
- 操作系统：Ubuntu 22.04 LTS
- 带宽：3Mbps

## 快速部署步骤

### 1. 安装基础环境

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装 Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 安装 Docker Compose
sudo apt install docker-compose -y

# 安装 JDK 17
sudo apt install openjdk-17-jdk -y

# 安装 Maven
sudo apt install maven -y

# 安装 Git
sudo apt install git -y
```

### 2. 克隆项目

```bash
cd /opt
sudo git clone <your-repo-url> lab-system
sudo chown -R $USER:$USER lab-system
cd lab-system
```

### 3. 启动基础设施

```bash
# 启动 MySQL、Redis、RabbitMQ、MinIO、Nacos
docker-compose up -d

# 等待服务启动（约2分钟）
sleep 120

# 检查服务状态
docker-compose ps
```

### 4. 编译打包

```bash
# 编译所有服务
mvn clean package -DskipTests

# 创建部署目录
mkdir -p /opt/lab-deploy
```

### 5. 部署服务

创建启动脚本 `/opt/lab-deploy/start-all.sh`：

```bash
#!/bin/bash

# 设置 JVM 参数（根据服务器配置调整）
JVM_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC"

# 启动网关
nohup java $JVM_OPTS -jar /opt/lab-system/lab-gateway/target/lab-gateway-1.0.0.jar > /opt/lab-deploy/gateway.log 2>&1 &
echo $! > /opt/lab-deploy/gateway.pid

# 等待网关启动
sleep 10

# 启动用户服务
nohup java $JVM_OPTS -jar /opt/lab-system/lab-service-user/target/lab-service-user-1.0.0.jar > /opt/lab-deploy/user.log 2>&1 &
echo $! > /opt/lab-deploy/user.pid

# 启动药品服务
nohup java $JVM_OPTS -jar /opt/lab-system/lab-service-material/target/lab-service-material-1.0.0.jar > /opt/lab-deploy/material.log 2>&1 &
echo $! > /opt/lab-deploy/material.pid

# 启动库存服务
nohup java $JVM_OPTS -jar /opt/lab-system/lab-service-inventory/target/lab-service-inventory-1.0.0.jar > /opt/lab-deploy/inventory.log 2>&1 &
echo $! > /opt/lab-deploy/inventory.pid

# 启动审批服务
nohup java $JVM_OPTS -jar /opt/lab-system/lab-service-approval/target/lab-service-approval-1.0.0.jar > /opt/lab-deploy/approval.log 2>&1 &
echo $! > /opt/lab-deploy/approval.pid

echo "所有服务已启动"
```

创建停止脚本 `/opt/lab-deploy/stop-all.sh`：

```bash
#!/bin/bash

# 停止所有服务
for pid_file in /opt/lab-deploy/*.pid; do
    if [ -f "$pid_file" ]; then
        pid=$(cat "$pid_file")
        kill $pid 2>/dev/null
        rm "$pid_file"
    fi
done

echo "所有服务已停止"
```

赋予执行权限并启动：

```bash
chmod +x /opt/lab-deploy/*.sh
/opt/lab-deploy/start-all.sh
```

### 6. 配置防火墙

```bash
# 开放必要端口
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 8080/tcp    # 网关
sudo ufw allow 8848/tcp    # Nacos（可选，建议内网访问）
sudo ufw enable
```

## 配置修改

### 修改数据库密码（推荐）

编辑 `docker-compose.yml`：
```yaml
MYSQL_ROOT_PASSWORD: your_secure_password
```

然后修改各服务的 `application.yml` 中的数据库密码。

### 配置域名访问（可选）

在腾讯云购买域名并配置 DNS 解析到服务器公网 IP，然后配置 Nginx 反向代理：

```bash
sudo apt install nginx -y

# 创建配置文件
sudo nano /etc/nginx/sites-available/lab-system
```

Nginx 配置示例：
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

启用配置：
```bash
sudo ln -s /etc/nginx/sites-available/lab-system /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

## 监控与维护

### 查看日志

```bash
# 查看服务日志
tail -f /opt/lab-deploy/gateway.log
tail -f /opt/lab-deploy/user.log

# 查看 Docker 日志
docker logs -f lab-mysql
docker logs -f lab-nacos
```

### 重启服务

```bash
# 重启所有服务
/opt/lab-deploy/stop-all.sh
/opt/lab-deploy/start-all.sh

# 重启单个服务（以网关为例）
kill $(cat /opt/lab-deploy/gateway.pid)
nohup java -Xms512m -Xmx1g -jar /opt/lab-system/lab-gateway/target/lab-gateway-1.0.0.jar > /opt/lab-deploy/gateway.log 2>&1 &
echo $! > /opt/lab-deploy/gateway.pid
```

### 数据备份

```bash
# 备份数据库
docker exec lab-mysql mysqldump -uroot -proot lab_management > /opt/backup/lab_$(date +%Y%m%d).sql

# 定时备份（添加到 crontab）
0 2 * * * docker exec lab-mysql mysqldump -uroot -proot lab_management > /opt/backup/lab_$(date +\%Y\%m\%d).sql
```

## 访问地址

部署完成后，通过以下地址访问：

- 应用网关：http://your-server-ip:8080
- Nacos 控制台：http://your-server-ip:8848/nacos（nacos/nacos）
- RabbitMQ 管理界面：http://your-server-ip:15672（guest/guest）
- MinIO 控制台：http://your-server-ip:9001（minioadmin/minioadmin）

## 成本估算

### 8核16GB 配置
- 服务器：约 ¥400-500/月（按量付费）
- 带宽：约 ¥50-100/月（5Mbps）
- 存储：约 ¥30/月（100GB SSD）
- 总计：约 ¥500-650/月

### 4核8GB 配置（省钱版）
- 服务器：约 ¥200-300/月
- 带宽：约 ¥30-60/月（3Mbps）
- 存储：约 ¥15/月（50GB）
- 总计：约 ¥250-400/月

## 常见问题

### 服务启动失败
- 检查端口是否被占用：`netstat -tlnp | grep 8080`
- 检查 Nacos 是否正常：`curl http://localhost:8848/nacos`
- 查看日志排查问题

### 内存不足
- 减少 JVM 堆内存：`-Xms256m -Xmx512m`
- 关闭不必要的服务
- 升级服务器配置

### 连接超时
- 检查防火墙规则
- 检查安全组配置
- 确认服务是否正常运行
