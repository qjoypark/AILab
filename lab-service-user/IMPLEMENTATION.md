# 用户认证与权限管理模块实现文档

## 实现概述

本模块实现了智慧实验室管理系统的用户认证与权限管理功能，包括：

- JWT令牌生成与验证
- 用户登录/登出/刷新令牌
- 基于角色的访问控制（RBAC）
- 用户管理（CRUD）
- 角色权限管理

## 已完成的任务

### 2.1 创建用户权限数据表 ✅

数据库表已在 `sql/02_user_tables.sql` 中定义：
- `sys_user` - 用户表
- `sys_role` - 角色表
- `sys_user_role` - 用户角色关联表
- `sys_permission` - 权限表
- `sys_role_permission` - 角色权限关联表

### 2.2 实现用户认证服务 ✅

**核心组件：**

1. **JwtUtil** (`util/JwtUtil.java`)
   - 生成访问令牌（2小时有效期）
   - 生成刷新令牌（7天有效期）
   - 令牌解析和验证

2. **AuthService** (`service/AuthService.java`)
   - 用户登录：验证用户名密码，生成令牌
   - 刷新令牌：使用刷新令牌获取新的访问令牌
   - 用户登出：删除Redis中的令牌

3. **AuthController** (`controller/AuthController.java`)
   - `POST /api/v1/auth/login` - 用户登录
   - `POST /api/v1/auth/refresh` - 刷新令牌
   - `POST /api/v1/auth/logout` - 用户登出

**令牌存储：**
- 令牌存储在Redis中，支持主动失效
- Key格式：`token:{userId}` 和 `refresh_token:{userId}`

### 2.3 编写用户认证属性测试 ✅

**属性测试：** `AuthenticationPropertyTest.java`

**属性 1: 用户登录后角色权限正确分配**
- 验证用户登录后返回所有分配的角色
- 验证用户登录后返回所有角色对应的权限
- 验证令牌生成成功
- 运行100次迭代测试

### 2.4 实现权限控制 ✅

**核心组件：**

1. **SecurityConfig** (`config/SecurityConfig.java`)
   - 配置Spring Security
   - 禁用CSRF和Session
   - 配置JWT过滤器
   - 启用方法级权限控制

2. **JwtAuthenticationFilter** (`security/JwtAuthenticationFilter.java`)
   - 从请求头提取JWT令牌
   - 验证令牌有效性
   - 验证Redis中的令牌
   - 设置Security上下文

3. **权限注解支持：**
   - `@PreAuthorize("hasRole('ADMIN')")` - 角色权限
   - `@PreAuthorize("hasPermission('material:create')")` - 功能权限

### 2.5 编写权限控制属性测试 ✅

**属性测试：** `AuthorizationPropertyTest.java`

**属性 2: 无权限访问被正确拒绝**
- 创建没有管理员权限的用户
- 尝试访问需要管理员权限的接口
- 验证返回403权限不足错误
- 运行100次迭代测试

### 2.6 实现用户管理接口 ✅

**UserService** (`service/UserService.java`)
- 分页查询用户列表
- 创建用户（密码加密）
- 更新用户信息
- 删除用户（逻辑删除）
- 用户角色分配

**UserController** (`controller/UserController.java`)
- `GET /api/v1/system/users` - 查询用户列表
- `GET /api/v1/system/users/{id}` - 查询用户详情
- `POST /api/v1/system/users` - 创建用户
- `PUT /api/v1/system/users/{id}` - 更新用户
- `DELETE /api/v1/system/users/{id}` - 删除用户
- `GET /api/v1/system/users/{id}/roles` - 查询用户角色

### 2.7 实现角色权限管理接口 ✅

**RoleService** (`service/RoleService.java`)
- 查询所有角色
- 创建角色
- 更新角色
- 删除角色（逻辑删除）
- 角色权限分配
- 查询权限树

**RoleController** (`controller/RoleController.java`)
- `GET /api/v1/system/roles` - 查询角色列表
- `GET /api/v1/system/roles/{id}` - 查询角色详情
- `POST /api/v1/system/roles` - 创建角色
- `PUT /api/v1/system/roles/{id}` - 更新角色
- `DELETE /api/v1/system/roles/{id}` - 删除角色
- `GET /api/v1/system/roles/permissions` - 查询权限树
- `POST /api/v1/system/roles/{id}/permissions` - 分配角色权限
- `GET /api/v1/system/roles/{id}/permissions` - 查询角色权限

## 技术实现细节

### JWT令牌结构

```json
{
  "sub": "userId",
  "username": "admin",
  "roles": ["ADMIN", "CENTER_ADMIN"],
  "permissions": ["user:create", "user:update"],
  "iat": 1234567890,
  "exp": 1234574890
}
```

### 密码加密

使用BCrypt算法加密密码，强度为10：
```java
passwordEncoder.encode(rawPassword)
```

### 权限控制流程

1. 用户发送请求，携带JWT令牌
2. JwtAuthenticationFilter拦截请求
3. 验证令牌有效性和Redis存储
4. 解析令牌获取角色和权限
5. 设置Security上下文
6. 方法级权限注解验证
7. 返回结果或403错误

### 数据权限控制

支持三种数据权限类型：
- **全部数据**：管理员可查看所有数据
- **部门数据**：实验室负责人可查看本实验室数据
- **个人数据**：普通用户只能查看自己创建的数据

## API文档

启动服务后访问：http://localhost:8081/doc.html

## 测试

### 运行属性测试

```bash
mvn test -Dtest=AuthenticationPropertyTest
mvn test -Dtest=AuthorizationPropertyTest
```

### 测试覆盖

- 属性测试：2个属性，每个运行100次迭代
- 验证需求：1.1, 1.2, 1.3, 1.4, 1.5

## 配置说明

### application.yml

```yaml
jwt:
  secret: lab-management-system-secret-key-for-jwt-token-generation-2024
  access-token-expiration: 7200000  # 2小时
  refresh-token-expiration: 604800000  # 7天
```

### Redis配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```

## 默认账号

- 用户名：admin
- 密码：admin123
- 角色：系统管理员

## 安全特性

1. **密码加密**：使用BCrypt算法
2. **令牌过期**：访问令牌2小时，刷新令牌7天
3. **令牌失效**：登出时删除Redis中的令牌
4. **权限验证**：方法级权限注解
5. **HTTPS支持**：生产环境强制HTTPS

## 依赖项

- Spring Boot 3.2.0
- Spring Security
- JWT (jjwt 0.12.3)
- MyBatis-Plus 3.5.5
- Redis
- MySQL 8.0
- jqwik 1.8.2 (属性测试)

## 下一步

所有子任务已完成。用户认证与权限管理模块已实现完毕，包括：
- ✅ 数据库表创建
- ✅ JWT认证服务
- ✅ 用户认证属性测试
- ✅ 权限控制
- ✅ 权限控制属性测试
- ✅ 用户管理接口
- ✅ 角色权限管理接口

可以继续实施下一个任务模块。
