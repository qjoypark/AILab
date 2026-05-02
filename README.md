# 智慧实验室管理系统 - 药品管理系统

## 项目简介

智慧实验室管理系统第一阶段聚焦于实验室药品管理系统，包括耗材、试剂和危化品的全生命周期管理。系统采用前后端分离架构，后端采用微服务架构，支持校内私有化部署。

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2.0
- Spring Cloud 2023.0.0
- Spring Cloud Alibaba 2022.0.0.0
- MyBatis-Plus 3.5.5
- MySQL 8.0
- Redis 7.x
- RabbitMQ 3.x
- MinIO
- Nacos 2.2.3

### 测试
- JUnit 5
- Mockito
- jqwik (Property-Based Testing)

## 项目结构

```
smart-lab-management-system/
├── lab-common/              # 公共模块
├── lab-gateway/             # API网关
├── lab-service-user/        # 用户服务
├── lab-service-material/    # 药品管理服务
├── lab-service-inventory/   # 库存管理服务
├── lab-service-approval/    # 审批流程服务
├── sql/                     # 数据库脚本
├── docker-compose.yml       # Docker编排文件
└── pom.xml                  # 父POM文件
```

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. 启动基础设施

使用Docker Compose启动MySQL、Redis、RabbitMQ、MinIO和Nacos：

```bash
docker-compose up -d
```

等待所有服务启动完成（约1-2分钟）。

Windows 开发环境也可以直接使用一键脚本启动基础设施、后端服务和前端：

```powershell
.\start-all.ps1
```

如需强制重启已占用端口的本地服务：

```powershell
.\start-all.ps1 -RestartServices
```

### 2. 验证服务状态

- MySQL: `mysql -h localhost -P 3306 -u root -p` (密码: root)
- Redis: `redis-cli -h localhost -p 6379`
- RabbitMQ管理界面: http://localhost:15672 (用户名/密码: guest/guest)
- MinIO控制台: http://localhost:9001 (用户名/密码: minioadmin/minioadmin)
- Nacos控制台: http://localhost:8848/nacos (用户名/密码: nacos/nacos)

### 3. 初始化数据库

数据库会在MySQL容器启动时自动初始化。如需手动执行：

```bash
mysql -h localhost -P 3306 -u root -p < sql/01_create_database.sql
for f in sql/*.sql; do
  [ "$(basename "$f")" = "01_create_database.sql" ] && continue
  mysql -h localhost -P 3306 -u root -p lab_management < "$f"
done
```

在 PowerShell 中可以使用：

```powershell
Get-Content .\sql\01_create_database.sql | mysql -h localhost -P 3306 -u root -p
Get-ChildItem sql/*.sql |
  Where-Object { $_.Name -ne '01_create_database.sql' } |
  Sort-Object Name |
  ForEach-Object { Get-Content $_.FullName | mysql -h localhost -P 3306 -u root -p lab_management }
```

请确认 `sql/17_lab_usage_tables.sql` 和 `sql/18_lab_usage_approval_config.sql` 已执行；它们分别提供实验室使用管理表、实验室审批流程、权限点和角色权限回填。

### 4. 编译项目

```bash
mvn clean install
```

### 5. 启动微服务

按以下顺序启动各个服务：

```bash
# 1. 启动网关
cd lab-gateway
mvn spring-boot:run

# 2. 启动用户服务
cd lab-service-user
mvn spring-boot:run

# 3. 启动药品管理服务
cd lab-service-material
mvn spring-boot:run

# 4. 启动库存管理服务
cd lab-service-inventory
mvn spring-boot:run

# 5. 启动审批流程服务
cd lab-service-approval
mvn spring-boot:run
```

### 6. 访问API文档

- 网关: http://localhost:8080
- 用户服务API文档: http://localhost:8081/doc.html
- 药品管理服务API文档: http://localhost:8082/doc.html
- 库存管理服务API文档: http://localhost:8083/doc.html
- 审批流程服务API文档: http://localhost:8084/doc.html

## 默认账号

- 用户名: admin
- 密码: admin123

## 开发指南

### 代码规范

- 遵循阿里巴巴Java开发手册
- 使用Lombok简化代码
- 所有实体类使用MyBatis-Plus注解
- 统一使用Result类封装响应结果

### 测试规范

- 单元测试覆盖率要求：核心业务逻辑80%以上
- 使用jqwik编写属性测试，每个属性测试运行100次迭代
- 测试类命名：XxxTest（单元测试）、XxxPropertyTest（属性测试）

### 提交规范

- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式调整
- refactor: 重构
- test: 测试相关
- chore: 构建/工具链相关

## 常见问题

### 1. Nacos启动失败

确保MySQL已启动并创建了nacos_config数据库。

### 2. 服务注册到Nacos失败

检查Nacos是否正常运行，确认application.yml中的nacos地址配置正确。

### 3. 数据库连接失败

检查MySQL是否启动，确认用户名密码是否正确。

## 许可证

本项目仅供学习和研究使用。
