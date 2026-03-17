# 用户认证属性测试执行指南

## 测试概述

本文档说明如何执行任务 2.3 的用户认证属性测试。

**测试文件**: `lab-service-user/src/test/java/com/lab/user/property/AuthenticationPropertyTest.java`

**测试属性**: 属性 1 - 用户登录后角色权限正确分配

**验证需求**: 需求 1.2 - 用户登录时，系统应验证用户身份并分配对应角色权限

## 测试说明

该属性测试使用 jqwik 框架进行基于属性的测试（Property-Based Testing），运行 100 次迭代，验证：

1. 用户成功登录后，系统返回该用户的所有角色
2. 每个角色包含其对应的权限列表
3. 访问令牌和刷新令牌正确生成

## 前置条件

### 1. Java 环境
- Java 17 或更高版本
- Maven 3.6+ 或使用 IDE 内置的 Maven

### 2. 依赖服务
测试需要以下服务运行：

#### Redis (端口 6379)
```bash
# 使用 Docker 启动 Redis
docker run -d --name redis-test -p 6379:6379 redis:7-alpine
```

#### RabbitMQ (端口 5672)
```bash
# 使用 Docker 启动 RabbitMQ
docker run -d --name rabbitmq-test -p 5672:5672 -p 15672:15672 rabbitmq:3-management-alpine
```

### 3. 数据库
测试使用 H2 内存数据库，无需额外配置。测试数据会在 `schema.sql` 中自动初始化。

## 执行测试

### 方法 1: 使用 Maven 命令行

```bash
# 在项目根目录执行
mvn test -Dtest=AuthenticationPropertyTest -pl lab-service-user

# 或者只运行该模块的所有测试
mvn test -pl lab-service-user
```

### 方法 2: 使用 IDE (IntelliJ IDEA / Eclipse)

1. 打开 `AuthenticationPropertyTest.java` 文件
2. 右键点击类名或测试方法
3. 选择 "Run 'AuthenticationPropertyTest'" 或 "Run 'userLoginAssignsRolesAndPermissionsCorrectly()'"

### 方法 3: 使用 Docker Compose (推荐)

如果项目根目录有 `docker-compose.yml`，可以一次性启动所有依赖服务：

```bash
# 启动所有服务
docker-compose up -d redis rabbitmq

# 等待服务就绪（约 10 秒）
sleep 10

# 运行测试
mvn test -Dtest=AuthenticationPropertyTest -pl lab-service-user

# 测试完成后停止服务
docker-compose down
```

## 测试输出

### 成功输出示例
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.lab.user.property.AuthenticationPropertyTest
tries = 100                  | # of calls to property
checks = 100                 | # of not rejected calls
generation = RANDOMIZED      | parameters are randomly generated
after-failure = PREVIOUS_SEED | use the previous seed
when-fixed-seed = ALLOW      | fixing the random seed is allowed
edge-cases#mode = MIXIN      | edge cases are mixed in
edge-cases#total = 0         | # of all combined edge cases
edge-cases#tried = 0         | # of edge cases tried in current run
seed = 1234567890            | random seed to reproduce generated values

[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 失败输出示例
如果测试失败，jqwik 会提供失败的反例：

```
org.opentest4j.AssertionFailedError: 
用户应该拥有所有分配的角色
Expecting ArrayList:
  ["ADMIN", "TEACHER"]
to contain exactly in any order:
  ["ADMIN", "TEACHER", "STUDENT"]
elements not found:
  ["STUDENT"]

Shrunk Sample (5 steps)
----------------------
  testUser: TestUser{username='test_abc123', password='Pass123', realName='张三', userType=1, roleCount=3}

Original Sample
---------------
  testUser: TestUser{username='test_xyz789abc', password='Password123', realName='李四王五', userType=2, roleCount=3}
```

## 测试数据

测试使用的初始数据在 `lab-service-user/src/test/resources/schema.sql` 中定义：

- **角色**: ADMIN (管理员), TEACHER (教师), STUDENT (学生)
- **权限**: 10 个基础权限（用户管理、药品管理、库存管理等）
- **角色权限映射**:
  - ADMIN: 所有权限
  - TEACHER: material:view, inventory:view, inventory:manage
  - STUDENT: material:view, inventory:view

## 故障排查

### 问题 1: Redis 连接失败
```
Error: Unable to connect to Redis at localhost:6379
```
**解决方案**: 确保 Redis 服务正在运行，端口 6379 未被占用

### 问题 2: RabbitMQ 连接失败
```
Error: Unable to connect to RabbitMQ at localhost:5672
```
**解决方案**: 确保 RabbitMQ 服务正在运行，端口 5672 未被占用

### 问题 3: Java 版本不匹配
```
Error: Unsupported class file major version 61
```
**解决方案**: 确保使用 Java 17 或更高版本
```bash
java -version  # 应显示 17 或更高
```

### 问题 4: 测试超时
```
Error: Test timed out after 60 seconds
```
**解决方案**: 
- 检查 Redis 和 RabbitMQ 是否响应正常
- 增加测试超时时间（在测试类上添加 `@Timeout(120)`）

## 验证测试结果

测试通过后，验证以下内容：

1. ✅ 测试运行了 100 次迭代（tries = 100）
2. ✅ 所有迭代都通过（checks = 100）
3. ✅ 没有失败或错误（Failures: 0, Errors: 0）
4. ✅ 测试覆盖了不同的用户类型（1-管理员, 2-教师, 3-学生）
5. ✅ 测试覆盖了不同的角色数量（1-3 个角色）

## 下一步

测试通过后，可以继续执行任务列表中的下一个任务。如果测试失败，请：

1. 检查失败的反例（Shrunk Sample）
2. 分析失败原因（角色权限分配逻辑、数据库查询等）
3. 修复代码后重新运行测试
4. 确保所有 100 次迭代都通过

## 参考资料

- [jqwik 用户指南](https://jqwik.net/docs/current/user-guide.html)
- [Property-Based Testing 介绍](https://hypothesis.works/articles/what-is-property-based-testing/)
- [Spring Boot 测试文档](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
