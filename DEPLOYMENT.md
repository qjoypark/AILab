# 部署指南

## 环境要求

### 开发环境
- JDK 17+
- Maven 3.8+
- Docker 20.10+
- Docker Compose 2.0+
- Git

### 生产环境
- 操作系统: Linux (推荐 Ubuntu 20.04+ 或 CentOS 7+)
- 内存: 最低8GB，推荐16GB
- CPU: 最低4核，推荐8核
- 磁盘: 最低100GB SSD

## 开发环境部署

### 1. 克隆项目

```bash
git clone <repository-url>
cd smart-lab-management-system
```

### 2. 启动基础设施

#### Linux/Mac
```bash
./start-all.sh
```

#### Windows
```bash
start-all.bat
```

或手动启动：
```bash
docker-compose up -d
```

### 3. 验证基础设施

等待约1-2分钟后，访问以下地址验证服务状态：

- Nacos: http://localhost:8848/nacos (nacos/nacos)
- RabbitMQ: http://localhost:15672 (guest/guest)
- MinIO: http://localhost:9001 (minioadmin/minioadmin)

### 4. 编译项目

```bash
mvn clean install -DskipTests
```

### 5. 启动微服务

在不同的终端窗口中依次启动：

```bash
# 终端1: 网关
cd lab-gateway
mvn spring-boot:run

# 终端2: 用户服务
cd lab-service-user
mvn spring-boot:run

# 终端3: 药品管理服务
cd lab-service-material
mvn spring-boot:run

# 终端4: 库存管理服务
cd lab-service-inventory
mvn spring-boot:run

# 终端5: 审批流程服务
cd lab-service-approval
mvn spring-boot:run
```

### 6. 验证部署

访问 http://localhost:8080 验证网关是否正常运行。

访问各服务的API文档：
- 用户服务: http://localhost:8081/doc.html
- 药品管理服务: http://localhost:8082/doc.html
- 库存管理服务: http://localhost:8083/doc.html
- 审批流程服务: http://localhost:8084/doc.html

## 生产环境部署

### 方案一: Docker Compose部署（推荐小规模）

#### 1. 构建镜像

```bash
# 构建各服务镜像
mvn clean package -DskipTests

# 为每个服务创建Dockerfile
docker build -t lab-gateway:1.0.0 ./lab-gateway
docker build -t lab-service-user:1.0.0 ./lab-service-user
docker build -t lab-service-material:1.0.0 ./lab-service-material
docker build -t lab-service-inventory:1.0.0 ./lab-service-inventory
docker build -t lab-service-approval:1.0.0 ./lab-service-approval
```

#### 2. 修改docker-compose.yml

添加应用服务配置（参考设计文档中的完整配置）。

#### 3. 启动所有服务

```bash
docker-compose up -d
```

### 方案二: Kubernetes部署（推荐大规模）

#### 1. 准备Kubernetes集群

确保已安装kubectl并配置好集群访问。

#### 2. 创建命名空间

```bash
kubectl create namespace lab-system
```

#### 3. 部署基础设施

```bash
# 部署MySQL
kubectl apply -f k8s/mysql-deployment.yaml

# 部署Redis
kubectl apply -f k8s/redis-deployment.yaml

# 部署RabbitMQ
kubectl apply -f k8s/rabbitmq-deployment.yaml

# 部署MinIO
kubectl apply -f k8s/minio-deployment.yaml

# 部署Nacos
kubectl apply -f k8s/nacos-deployment.yaml
```

#### 4. 部署应用服务

```bash
kubectl apply -f k8s/gateway-deployment.yaml
kubectl apply -f k8s/user-service-deployment.yaml
kubectl apply -f k8s/material-service-deployment.yaml
kubectl apply -f k8s/inventory-service-deployment.yaml
kubectl apply -f k8s/approval-service-deployment.yaml
```

#### 5. 配置Ingress

```bash
kubectl apply -f k8s/ingress.yaml
```

## 配置说明

### 数据库配置

修改各服务的application.yml：

```yaml
spring:
  datasource:
    url: jdbc:mysql://your-mysql-host:3306/lab_management
    username: your-username
    password: your-password
```

### Nacos配置

修改各服务的application.yml：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: your-nacos-host:8848
      config:
        server-addr: your-nacos-host:8848
```

### Redis配置

```yaml
spring:
  redis:
    host: your-redis-host
    port: 6379
    password: your-password
```

## 监控与日志

### 日志查看

开发环境：
```bash
tail -f logs/application.log
```

Docker环境：
```bash
docker logs -f <container-name>
```

Kubernetes环境：
```bash
kubectl logs -f <pod-name> -n lab-system
```

### 健康检查

访问各服务的健康检查端点：
- http://localhost:8081/actuator/health
- http://localhost:8082/actuator/health
- http://localhost:8083/actuator/health
- http://localhost:8084/actuator/health

## 备份与恢复

### 数据库备份

```bash
# 全量备份
docker exec lab-mysql mysqldump -u root -proot lab_management > backup.sql

# 恢复
docker exec -i lab-mysql mysql -u root -proot lab_management < backup.sql
```

### 配置备份

定期备份Nacos配置：
```bash
# 导出配置
curl -X GET "http://localhost:8848/nacos/v1/cs/configs?export=true&group=DEFAULT_GROUP"
```

## 故障排查

### 服务无法启动

1. 检查端口是否被占用
2. 检查Nacos是否正常运行
3. 检查数据库连接是否正常
4. 查看日志文件

### 服务注册失败

1. 检查Nacos地址配置
2. 检查网络连接
3. 检查Nacos认证配置

### 数据库连接失败

1. 检查MySQL是否启动
2. 检查用户名密码
3. 检查数据库是否已创建
4. 检查防火墙设置

## 性能优化

### JVM参数优化

```bash
java -Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar app.jar
```

### 数据库优化

1. 添加适当的索引
2. 配置连接池参数
3. 启用查询缓存

### Redis优化

1. 配置合适的内存限制
2. 启用持久化
3. 配置合适的过期策略

## 安全加固

### 1. 修改默认密码

- MySQL root密码
- Redis密码
- RabbitMQ密码
- MinIO密码
- Nacos密码

### 2. 启用HTTPS

配置SSL证书，强制使用HTTPS。

### 3. 配置防火墙

只开放必要的端口。

### 4. 定期更新

定期更新依赖包和基础镜像。
