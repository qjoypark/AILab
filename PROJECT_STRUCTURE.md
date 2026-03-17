# 项目结构说明

## 目录结构

```
smart-lab-management-system/
├── lab-common/                          # 公共模块
│   ├── src/main/java/com/lab/common/
│   │   ├── result/                      # 统一响应结果
│   │   │   ├── Result.java              # 响应结果封装
│   │   │   └── ResultCode.java          # 响应状态码
│   │   └── exception/                   # 异常处理
│   │       └── BusinessException.java   # 业务异常
│   ├── src/main/resources/
│   │   └── logback-spring.xml           # 日志配置
│   └── pom.xml
│
├── lab-gateway/                         # API网关
│   ├── src/main/java/com/lab/gateway/
│   │   └── GatewayApplication.java      # 网关启动类
│   ├── src/main/resources/
│   │   └── application.yml              # 网关配置
│   └── pom.xml
│
├── lab-service-user/                    # 用户服务
│   ├── src/main/java/com/lab/user/
│   │   ├── controller/                  # 控制器层
│   │   ├── service/                     # 服务层
│   │   ├── mapper/                      # 数据访问层
│   │   ├── entity/                      # 实体类
│   │   └── UserServiceApplication.java  # 启动类
│   ├── src/main/resources/
│   │   ├── application.yml              # 配置文件
│   │   └── mapper/                      # MyBatis映射文件
│   └── pom.xml
│
├── lab-service-material/                # 药品管理服务
│   ├── src/main/java/com/lab/material/
│   │   ├── controller/                  # 控制器层
│   │   ├── service/                     # 服务层
│   │   ├── mapper/                      # 数据访问层
│   │   ├── entity/                      # 实体类
│   │   └── MaterialServiceApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── mapper/
│   └── pom.xml
│
├── lab-service-inventory/               # 库存管理服务
│   ├── src/main/java/com/lab/inventory/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── mapper/
│   │   ├── entity/
│   │   └── InventoryServiceApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── mapper/
│   └── pom.xml
│
├── lab-service-approval/                # 审批流程服务
│   ├── src/main/java/com/lab/approval/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── mapper/
│   │   ├── entity/
│   │   └── ApprovalServiceApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── mapper/
│   └── pom.xml
│
├── sql/                                 # 数据库脚本
│   ├── 01_create_database.sql           # 创建数据库
│   ├── 02_user_tables.sql               # 用户权限表
│   ├── 03_material_tables.sql           # 药品分类表
│   ├── 04_material_info.sql             # 药品信息表
│   ├── 05_inventory_tables.sql          # 库存表
│   ├── 06_stock_in_out.sql              # 入出库表
│   ├── 07_stock_check.sql               # 盘点表
│   ├── 08_application_tables.sql        # 申请表
│   ├── 09_approval_tables.sql           # 审批表
│   ├── 10_alert_notification.sql        # 预警通知表
│   ├── 11_audit_log.sql                 # 审计日志表
│   └── 12_init_data.sql                 # 初始化数据
│
├── docker-compose.yml                   # Docker编排文件
├── start-all.sh                         # Linux/Mac启动脚本
├── start-all.bat                        # Windows启动脚本
├── pom.xml                              # 父POM文件
├── README.md                            # 项目说明
├── DEPLOYMENT.md                        # 部署指南
├── PROJECT_STRUCTURE.md                 # 本文件
└── .gitignore                           # Git忽略文件

```

## 模块说明

### 1. lab-common (公共模块)

提供所有服务共用的基础功能：
- 统一响应结果封装 (Result, ResultCode)
- 业务异常定义 (BusinessException)
- 日志配置 (logback-spring.xml)
- 工具类和常量定义

### 2. lab-gateway (API网关)

功能：
- 统一入口，路由转发
- 负载均衡
- 跨域处理
- 统一认证（后续实现）

端口：8080

### 3. lab-service-user (用户服务)

功能：
- 用户认证与授权
- 用户管理
- 角色权限管理
- JWT令牌管理

端口：8081

### 4. lab-service-material (药品管理服务)

功能：
- 药品信息管理
- 药品分类管理
- 供应商管理
- 文件上传（MinIO）

端口：8082

### 5. lab-service-inventory (库存管理服务)

功能：
- 库存查询
- 入库管理
- 出库管理
- 库存盘点
- 库存预警

端口：8083

### 6. lab-service-approval (审批流程服务)

功能：
- 领用申请管理
- 审批流程引擎
- 危化品管理
- 消息通知

端口：8084

## 技术架构

### 服务注册与发现
- Nacos: 服务注册中心和配置中心
- 端口: 8848

### 数据存储
- MySQL 8.0: 主数据库
- Redis 7.x: 缓存和会话存储
- MinIO: 对象存储（文件、图片）

### 消息队列
- RabbitMQ: 异步消息处理

### API文档
- Knife4j: 基于Swagger的API文档工具
- 访问路径: /doc.html

### 日志框架
- SLF4J + Logback
- 日志文件位置: logs/

## 数据库设计

### 核心表

1. **用户权限模块**
   - sys_user: 用户表
   - sys_role: 角色表
   - sys_user_role: 用户角色关联表
   - sys_permission: 权限表
   - sys_role_permission: 角色权限关联表

2. **药品管理模块**
   - material_category: 药品分类表
   - material: 药品信息表
   - supplier: 供应商表

3. **库存管理模块**
   - warehouse: 仓库表
   - storage_location: 存储位置表
   - stock_inventory: 库存表
   - stock_in: 入库单表
   - stock_in_detail: 入库单明细表
   - stock_out: 出库单表
   - stock_out_detail: 出库单明细表
   - stock_check: 库存盘点表
   - stock_check_detail: 库存盘点明细表

4. **申请审批模块**
   - material_application: 领用申请单表
   - material_application_item: 领用申请明细表
   - approval_flow_config: 审批流程配置表
   - approval_record: 审批记录表
   - hazardous_usage_record: 危化品使用记录表

5. **预警通知模块**
   - stock_alert_config: 库存预警配置表
   - alert_record: 预警记录表
   - notification: 消息通知表

6. **审计日志模块**
   - audit_log: 操作日志表

## 配置说明

### 端口分配
- 8080: API网关
- 8081: 用户服务
- 8082: 药品管理服务
- 8083: 库存管理服务
- 8084: 审批流程服务
- 3306: MySQL
- 6379: Redis
- 5672: RabbitMQ
- 15672: RabbitMQ管理界面
- 9000: MinIO API
- 9001: MinIO控制台
- 8848: Nacos

### 默认账号

**系统管理员**
- 用户名: admin
- 密码: admin123

**基础设施**
- MySQL: root/root
- Redis: 无密码
- RabbitMQ: guest/guest
- MinIO: minioadmin/minioadmin
- Nacos: nacos/nacos

## 开发规范

### 包结构
```
com.lab.{module}/
├── controller/      # 控制器层，处理HTTP请求
├── service/         # 服务层，业务逻辑
│   └── impl/        # 服务实现
├── mapper/          # 数据访问层，MyBatis接口
├── entity/          # 实体类
├── dto/             # 数据传输对象
├── vo/              # 视图对象
├── config/          # 配置类
└── util/            # 工具类
```

### 命名规范
- 实体类: XxxEntity 或 Xxx
- DTO: XxxDTO
- VO: XxxVO
- Service接口: XxxService
- Service实现: XxxServiceImpl
- Controller: XxxController
- Mapper: XxxMapper

### 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1234567890
}
```

## 下一步开发

1. 实现用户认证与权限管理模块（任务2）
2. 实现审计日志模块（任务3）
3. 实现药品基础信息管理模块（任务4）
4. 实现库存管理模块（任务5）
5. 实现库存预警模块（任务6）
6. 实现领用申请与审批模块（任务8）
7. 实现危化品安全合规管理模块（任务9）
8. 实现通知与消息模块（任务10）
9. 实现报表统计模块（任务11）
