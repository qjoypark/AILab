# 快速启动指南

## 默认账号密码

**管理员账号**:
- 用户名: `admin`
- 密码: `admin123`

## 启动步骤

### 1. 启动基础设施

首先确保Docker Desktop已启动，然后运行：

```bash
# 启动MySQL, Redis, RabbitMQ, MinIO, Nacos
docker-compose up -d

# 等待30秒让服务完全启动
```

### 2. 启动微服务

在不同的终端窗口中分别启动各个服务：

```bash
# 终端1: 启动网关服务 (端口8080)
cd lab-gateway && mvn spring-boot:run

# 终端2: 启动用户服务 (端口8081)
cd lab-service-user && mvn spring-boot:run

# 终端3: 启动物料服务 (端口8082)
cd lab-service-material && mvn spring-boot:run

# 终端4: 启动库存服务 (端口8083)
cd lab-service-inventory && mvn spring-boot:run

# 终端5: 启动审批服务 (端口8084)
cd lab-service-approval && mvn spring-boot:run
```

### 3. 启动前端

```bash
# 启动Web管理后台 (端口3000)
cd lab-web-admin && npm run dev
```

## 访问地址

- **Web管理后台**: http://localhost:3000
- **API网关**: http://localhost:8080
- **Nacos控制台**: http://localhost:8848/nacos (nacos/nacos)
- **RabbitMQ管理界面**: http://localhost:15672 (guest/guest)
- **MinIO控制台**: http://localhost:9001 (minioadmin/minioadmin)

## 常见问题

### 登录返回500错误
- 检查后端服务是否全部启动
- 检查Nacos是否正常运行
- 检查MySQL数据库是否已初始化
- 查看后端日志确认具体错误

### 服务无法注册到Nacos
- 确认Nacos已启动: `curl http://localhost:8848/nacos`
- 检查application.yml中的Nacos地址配置

### 数据库连接失败
- 确认MySQL容器已启动: `docker ps | grep lab-mysql`
- 确认数据库已初始化: 查看sql目录下的脚本是否已执行
