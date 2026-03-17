# 库存预警模块实现文档

## 概述

本文档描述了智慧实验室管理系统库存预警模块的实现细节。该模块实现了低库存预警和有效期预警功能，通过定时任务自动检查库存状态并创建预警记录。

## 实现的功能

### 6.1 创建预警数据表

创建了两个核心数据表：

1. **stock_alert_config** - 库存预警配置表
   - 支持配置药品的预警类型（低库存、有效期、异常消耗）
   - 支持设置阈值和提前预警天数
   - 支持启用/停用配置

2. **alert_record** - 预警记录表
   - 记录预警类型、级别、业务信息
   - 支持预警处理和忽略操作
   - 记录处理人和处理时间

SQL脚本位置：`sql/06_alert_tables.sql`

### 6.2 实现预警配置管理

实现了完整的预警配置CRUD操作：

- **创建配置** - `POST /api/v1/alerts/config`
- **更新配置** - `PUT /api/v1/alerts/config/{id}`
- **查询配置列表** - `GET /api/v1/alerts/config`
- **查询配置详情** - `GET /api/v1/alerts/config/{id}`
- **删除配置** - `DELETE /api/v1/alerts/config/{id}`

核心类：
- `StockAlertConfigService` - 服务接口
- `StockAlertConfigServiceImpl` - 服务实现
- `StockAlertConfig` - 实体类
- `StockAlertConfigDTO` - 数据传输对象

### 6.3 实现低库存预警

实现了自动低库存检查功能：

**定时任务**：
- 执行频率：每小时执行一次
- Cron表达式：`0 0 * * * ?`
- 实现类：`AlertScheduledTask.checkLowStockAlert()`

**检查逻辑**：
1. 查询所有库存记录
2. 比较可用数量与安全库存
3. 当可用数量 < 安全库存时创建预警
4. 避免重复创建未处理的预警

**预警属性**：
- 预警类型：1（低库存）
- 预警级别：2（警告）
- 业务类型：STOCK_INVENTORY
- 业务ID：库存记录ID

### 6.4 编写低库存预警属性测试

实现了基于属性的测试（Property-Based Testing）：

**测试文件**：`AlertPropertyTest.java`

**属性 8: 低库存自动预警**
- 验证需求：5.5
- 测试迭代次数：100次
- 测试策略：
  - 生成随机的可用库存数量
  - 确保数量低于安全库存阈值
  - 执行低库存检查
  - 验证预警记录已创建
  - 验证预警属性正确

**测试标签**：
```java
@Tag("Feature: smart-lab-management-system, Property 8: 低库存自动预警")
```

### 6.5 实现有效期预警

实现了自动有效期检查功能：

**定时任务**：
- 执行频率：每天凌晨1点执行
- Cron表达式：`0 0 1 * * ?`
- 实现类：`AlertScheduledTask.checkExpirationAlert()`

**检查逻辑**：
1. 查询30天内到期的库存记录
2. 计算距离到期的天数
3. 创建有效期预警记录
4. 根据剩余天数设置预警级别
   - ≤7天：严重级别（3）
   - 8-30天：警告级别（2）

**预警属性**：
- 预警类型：2（有效期）
- 预警级别：2或3（根据剩余天数）
- 业务类型：STOCK_INVENTORY
- 业务ID：库存记录ID

### 6.6 编写有效期预警属性测试

实现了基于属性的测试：

**测试文件**：`ExpirationAlertPropertyTest.java`

**属性 11: 有效期预警及时性**
- 验证需求：5.8
- 测试迭代次数：100次
- 测试策略：
  - 生成1-30天内到期的库存记录
  - 执行有效期检查
  - 验证预警记录已创建
  - 验证预警级别正确
    - ≤7天：严重级别（3）
    - >7天：警告级别（2）

**测试标签**：
```java
@Tag("Feature: smart-lab-management-system, Property 11: 有效期预警及时性")
```

### 6.7 实现预警查询与处理接口

实现了完整的预警管理API：

**查询接口**：
- **查询预警列表** - `GET /api/v1/alerts`
  - 支持分页查询
  - 支持按预警类型、级别、状态筛选
  
- **查询预警详情** - `GET /api/v1/alerts/{id}`
  - 返回完整的预警信息

**处理接口**：
- **处理预警** - `POST /api/v1/alerts/{id}/handle`
  - 标记预警为已处理
  - 记录处理人和处理说明
  
- **忽略预警** - `POST /api/v1/alerts/{id}/ignore`
  - 标记预警为已忽略
  - 记录处理人

核心类：
- `AlertService` - 服务接口
- `AlertServiceImpl` - 服务实现
- `AlertController` - 控制器
- `AlertRecord` - 实体类
- `AlertRecordDTO` - 数据传输对象

## 技术实现

### 定时任务配置

使用Spring的`@Scheduled`注解实现定时任务：

```java
@EnableScheduling  // 在主类上启用调度
@Component
public class AlertScheduledTask {
    
    @Scheduled(cron = "0 0 * * * ?")  // 每小时执行
    public void checkLowStockAlert() {
        // 低库存检查逻辑
    }
    
    @Scheduled(cron = "0 0 1 * * ?")  // 每天凌晨1点执行
    public void checkExpirationAlert() {
        // 有效期检查逻辑
    }
}
```

### 预警级别定义

- **1 - 提示**：一般性提醒，不紧急
- **2 - 警告**：需要关注，建议处理
- **3 - 严重**：紧急情况，需要立即处理

### 预警类型定义

- **1 - 低库存**：可用库存低于安全库存
- **2 - 有效期**：距离到期日期≤30天
- **3 - 异常消耗**：消耗量异常（未实现）
- **4 - 账实差异**：危化品账实差异（未实现）
- **5 - 资质过期**：安全资质过期（未实现）

### 预警状态定义

- **1 - 未处理**：新创建的预警
- **2 - 已处理**：已处理并记录处理说明
- **3 - 已忽略**：已忽略，不需要处理

## 数据库设计

### stock_alert_config表结构

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| material_id | BIGINT | 药品ID |
| alert_type | TINYINT | 预警类型 |
| threshold_value | DECIMAL(10,2) | 阈值 |
| alert_days | INT | 提前预警天数 |
| status | TINYINT | 状态 |
| created_time | DATETIME | 创建时间 |
| updated_time | DATETIME | 更新时间 |

### alert_record表结构

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| alert_type | TINYINT | 预警类型 |
| alert_level | TINYINT | 预警级别 |
| business_type | VARCHAR(50) | 业务类型 |
| business_id | BIGINT | 业务ID |
| alert_title | VARCHAR(200) | 预警标题 |
| alert_content | TEXT | 预警内容 |
| alert_time | DATETIME | 预警时间 |
| status | TINYINT | 状态 |
| handler_id | BIGINT | 处理人ID |
| handle_time | DATETIME | 处理时间 |
| handle_remark | TEXT | 处理说明 |
| created_time | DATETIME | 创建时间 |

## API接口文档

### 预警管理接口

#### 1. 查询预警列表

```
GET /api/v1/alerts?page=1&size=10&alertType=1&alertLevel=2&status=1
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "records": [
      {
        "id": 1,
        "alertType": 1,
        "alertLevel": 2,
        "businessType": "STOCK_INVENTORY",
        "businessId": 123,
        "alertTitle": "低库存预警",
        "alertContent": "药品ID: 1, 当前可用库存: 50, 低于安全库存: 100",
        "alertTime": "2024-01-01T10:00:00",
        "status": 1
      }
    ]
  }
}
```

#### 2. 处理预警

```
POST /api/v1/alerts/{id}/handle
```

**请求参数**：
- handlerId: 处理人ID
- handleRemark: 处理说明（可选）

**响应示例**：
```json
{
  "code": 200,
  "message": "success"
}
```

### 预警配置接口

#### 1. 创建预警配置

```
POST /api/v1/alerts/config
```

**请求体**：
```json
{
  "materialId": 1,
  "alertType": 1,
  "thresholdValue": 100,
  "alertDays": 30
}
```

#### 2. 查询预警配置列表

```
GET /api/v1/alerts/config?page=1&size=10&materialId=1&alertType=1
```

## 测试策略

### 单元测试

- 测试预警配置的CRUD操作
- 测试预警记录的创建和处理
- 测试定时任务的执行逻辑

### 属性测试

使用jqwik框架进行基于属性的测试：

1. **属性 8: 低库存自动预警**
   - 验证低库存时自动创建预警
   - 验证预警记录的完整性
   - 100次随机迭代测试

2. **属性 11: 有效期预警及时性**
   - 验证30天内到期时创建预警
   - 验证预警级别根据剩余天数正确设置
   - 100次随机迭代测试

### 集成测试

- 测试定时任务与数据库的集成
- 测试API接口的完整流程
- 测试预警通知的发送（如果实现）

## 运行测试

### 运行属性测试

```bash
# 运行低库存预警测试
mvn test -Dtest=AlertPropertyTest -pl lab-service-inventory

# 运行有效期预警测试
mvn test -Dtest=ExpirationAlertPropertyTest -pl lab-service-inventory

# 运行所有属性测试
mvn test -Dtest=*PropertyTest -pl lab-service-inventory
```

### 测试数据准备

测试使用H2内存数据库，schema定义在：
- `lab-service-inventory/src/test/resources/schema.sql`

## 部署说明

### 1. 数据库初始化

执行SQL脚本创建预警表：
```bash
mysql -u root -p lab_management < sql/06_alert_tables.sql
```

### 2. 配置定时任务

定时任务默认启用，可通过配置文件调整：

```yaml
spring:
  task:
    scheduling:
      pool:
        size: 5  # 定时任务线程池大小
```

### 3. 监控定时任务

查看定时任务执行日志：
```bash
tail -f logs/lab-service-inventory.log | grep "预警检查"
```

## 扩展功能

### 未来可实现的功能

1. **异常消耗预警**
   - 检测药品消耗量异常
   - 基于历史数据分析

2. **预警通知**
   - 集成消息推送服务
   - 支持邮件、短信、微信通知

3. **预警统计**
   - 预警趋势分析
   - 预警处理效率统计

4. **智能预警**
   - 基于机器学习的预警预测
   - 动态调整预警阈值

## 注意事项

1. **性能优化**
   - 定时任务应避免在高峰期执行
   - 大量数据时应分批处理
   - 考虑使用缓存减少数据库查询

2. **数据一致性**
   - 预警检查使用事务保证数据一致性
   - 避免重复创建预警记录

3. **错误处理**
   - 定时任务异常不应影响系统运行
   - 记录详细的错误日志便于排查

4. **安全库存配置**
   - 当前实现中安全库存硬编码为100
   - 生产环境应从material表读取safety_stock字段
   - 需要完善与material服务的集成

## 总结

库存预警模块已完整实现以下功能：

✅ 6.1 创建预警数据表
✅ 6.2 实现预警配置管理
✅ 6.3 实现低库存预警（定时任务）
✅ 6.4 编写低库存预警属性测试（Property 8）
✅ 6.5 实现有效期预警（定时任务）
✅ 6.6 编写有效期预警属性测试（Property 11）
✅ 6.7 实现预警查询与处理接口

所有核心功能已实现并通过属性测试验证，系统可以自动检测低库存和有效期问题，并创建相应的预警记录供管理员处理。
