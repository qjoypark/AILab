# 库存管理模块实施文档

## 概述

库存管理模块实现了实验室药品的入库、出库、库存查询和盘点功能，支持批次管理、FIFO出库策略和完整的审计日志记录。

## 功能特性

### 1. 仓库与存储位置管理
- 支持普通仓库和危化品仓库分类管理
- 存储位置支持货架号和层号管理
- 仓库编码和位置编码唯一性校验

### 2. 入库管理
- 支持采购入库、退货入库、盘盈入库、归还入库四种类型
- 批次号、生产日期、有效期管理
- 入库确认后自动更新库存
- 支持多个药品明细的批量入库
- 自动生成入库单号（格式：IN+日期+序号）

### 3. 出库管理
- 支持领用出库、报废出库、调拨出库三种类型
- **FIFO（先进先出）策略**：按生产日期自动选择最早批次出库
- 库存不足自动校验
- 支持关联领用申请单
- 自动生成出库单号（格式：OUT+日期+序号）

### 4. 库存查询
- 实时库存查询，支持按药品、仓库筛选
- 库存明细查询，显示所有批次信息
- 可用数量 = 库存数量 - 锁定数量
- 支持低库存预警筛选（待实现）

### 5. 库存盘点
- 创建盘点单，记录盘点日期和盘点人
- 提交盘点明细，记录账面数量和实际数量
- 完成盘点后自动调整库存
- 记录盘盈盘亏原因
- 自动生成盘点单号（格式：CHK+日期+序号）

## 数据库设计

### 核心表结构

#### warehouse（仓库表）
- id: 仓库ID
- warehouse_code: 仓库编码（唯一）
- warehouse_name: 仓库名称
- warehouse_type: 仓库类型（1-普通仓库，2-危化品仓库）
- location: 位置
- manager_id: 负责人ID
- status: 状态

#### storage_location（存储位置表）
- id: 位置ID
- warehouse_id: 仓库ID
- location_code: 位置编码
- location_name: 位置名称
- shelf_number: 货架号
- layer_number: 层号
- status: 状态

#### stock_inventory（库存表）
- id: 库存ID
- material_id: 药品ID
- warehouse_id: 仓库ID
- storage_location_id: 存储位置ID
- batch_number: 批次号
- quantity: 库存数量
- available_quantity: 可用数量
- locked_quantity: 锁定数量
- production_date: 生产日期
- expire_date: 有效期至
- unit_price: 单价
- total_amount: 总金额
- last_check_date: 最后盘点日期

#### stock_in（入库单表）
- id: 入库单ID
- in_order_no: 入库单号（唯一）
- in_type: 入库类型
- warehouse_id: 仓库ID
- supplier_id: 供应商ID
- total_amount: 总金额
- in_date: 入库日期
- operator_id: 经手人ID
- status: 状态（1-待入库，2-已入库，3-已取消）
- remark: 备注

#### stock_in_detail（入库单明细表）
- id: 明细ID
- in_order_id: 入库单ID
- material_id: 药品ID
- batch_number: 批次号
- quantity: 入库数量
- unit_price: 单价
- total_amount: 金额
- production_date: 生产日期
- expire_date: 有效期至
- storage_location_id: 存储位置ID

#### stock_out（出库单表）
- id: 出库单ID
- out_order_no: 出库单号（唯一）
- out_type: 出库类型
- warehouse_id: 仓库ID
- application_id: 关联申请单ID
- receiver_id: 领用人ID
- receiver_name: 领用人姓名
- receiver_dept: 领用部门
- out_date: 出库日期
- operator_id: 经手人ID
- status: 状态（1-待出库，2-已出库，3-已取消）
- remark: 备注

#### stock_out_detail（出库单明细表）
- id: 明细ID
- out_order_id: 出库单ID
- material_id: 药品ID
- batch_number: 批次号
- quantity: 出库数量
- unit_price: 单价
- total_amount: 金额
- storage_location_id: 存储位置ID

#### stock_check（库存盘点表）
- id: 盘点ID
- check_no: 盘点单号（唯一）
- warehouse_id: 仓库ID
- check_date: 盘点日期
- checker_id: 盘点人ID
- status: 状态（1-盘点中，2-已完成）
- remark: 备注

#### stock_check_detail（库存盘点明细表）
- id: 明细ID
- check_id: 盘点ID
- material_id: 药品ID
- batch_number: 批次号
- book_quantity: 账面数量
- actual_quantity: 实际数量
- diff_quantity: 差异数量
- diff_reason: 差异原因
- storage_location_id: 存储位置ID

## API接口

### 仓库管理
- GET /api/v1/inventory/warehouses - 查询仓库列表
- GET /api/v1/inventory/warehouses/{id} - 查询仓库详情
- POST /api/v1/inventory/warehouses - 创建仓库
- PUT /api/v1/inventory/warehouses/{id} - 更新仓库
- DELETE /api/v1/inventory/warehouses/{id} - 删除仓库

### 存储位置管理
- GET /api/v1/inventory/storage-locations - 查询存储位置列表
- GET /api/v1/inventory/storage-locations/by-warehouse/{warehouseId} - 根据仓库ID查询所有存储位置
- GET /api/v1/inventory/storage-locations/{id} - 查询存储位置详情
- POST /api/v1/inventory/storage-locations - 创建存储位置
- PUT /api/v1/inventory/storage-locations/{id} - 更新存储位置
- DELETE /api/v1/inventory/storage-locations/{id} - 删除存储位置

### 库存查询
- GET /api/v1/inventory/stock - 查询库存列表
- GET /api/v1/inventory/stock/{materialId}/detail - 查询指定药品的库存明细

### 入库管理
- GET /api/v1/inventory/stock-in - 查询入库单列表
- GET /api/v1/inventory/stock-in/{id} - 查询入库单详情
- POST /api/v1/inventory/stock-in - 创建入库单
- POST /api/v1/inventory/stock-in/{id}/confirm - 确认入库
- POST /api/v1/inventory/stock-in/{id}/cancel - 取消入库单

### 出库管理
- GET /api/v1/inventory/stock-out - 查询出库单列表
- GET /api/v1/inventory/stock-out/{id} - 查询出库单详情
- POST /api/v1/inventory/stock-out - 创建出库单
- POST /api/v1/inventory/stock-out/{id}/confirm - 确认出库
- POST /api/v1/inventory/stock-out/{id}/cancel - 取消出库单

### 库存盘点
- GET /api/v1/inventory/stock-check - 查询盘点单列表
- GET /api/v1/inventory/stock-check/{id} - 查询盘点单详情
- POST /api/v1/inventory/stock-check - 创建盘点单
- POST /api/v1/inventory/stock-check/{id}/items - 提交盘点明细
- POST /api/v1/inventory/stock-check/{id}/complete - 完成盘点

## 核心业务逻辑

### 入库流程
1. 创建入库单，状态为"待入库"
2. 填写入库明细（药品、批次号、数量、单价、生产日期、有效期等）
3. 确认入库：
   - 检查入库单状态
   - 遍历入库明细
   - 查询是否已存在该批次的库存
   - 如果存在，更新库存数量和可用数量
   - 如果不存在，创建新的库存记录
   - 更新入库单状态为"已入库"
   - 记录审计日志

### 出库流程（FIFO策略）
1. 创建出库单，状态为"待出库"
2. 填写出库明细（药品、数量等）
3. 确认出库：
   - 检查出库单状态
   - 遍历出库明细
   - 查询该药品在该仓库的所有批次库存，按生产日期升序排序（FIFO）
   - 从最早的批次开始扣减库存
   - 如果单个批次库存不足，继续扣减下一个批次
   - 更新库存数量和可用数量
   - 更新出库单状态为"已出库"
   - 记录审计日志

### 盘点流程
1. 创建盘点单，状态为"盘点中"
2. 提交盘点明细（药品、批次号、账面数量、实际数量、差异原因）
3. 完成盘点：
   - 检查盘点单状态
   - 遍历盘点明细
   - 如果差异数量不为0，调整库存
   - 更新库存数量和可用数量
   - 更新最后盘点日期
   - 更新盘点单状态为"已完成"
   - 记录审计日志

## 审计日志

所有关键操作都使用@AuditLog注解记录审计日志：
- 创建/更新/删除仓库
- 创建/更新/删除存储位置
- 创建入库单、确认入库、取消入库单
- 创建出库单、确认出库、取消出库单
- 创建盘点单、提交盘点明细、完成盘点

## 技术栈

- Spring Boot 3.2.0
- MyBatis-Plus 3.5.x
- MySQL 8.0
- Redis 7.x
- Knife4j (Swagger 3)

## 部署说明

### 数据库初始化
```bash
# 执行SQL脚本
mysql -u root -p lab_management < sql/05_inventory_tables.sql
```

### 启动服务
```bash
# 确保MySQL、Redis、Nacos已启动
cd lab-service-inventory
mvn spring-boot:run
```

### 访问API文档
http://localhost:8083/doc.html

## 待实现功能

1. 低库存预警筛选（需要关联material表获取安全库存）
2. 库存锁定功能（领用申请审批通过后锁定库存）
3. 库存预警定时任务
4. 有效期预警定时任务
5. 库存报表统计
6. 库存流水查询

## 测试说明

### 单元测试
- 测试入库、出库、盘点的核心业务逻辑
- 测试FIFO出库策略
- 测试库存数量一致性

### 属性测试
- 属性5: 入库操作完整记录
- 属性6: 出库操作完整记录
- 属性7: 库存数量一致性
- 属性9: 库存盘点记录完整性

### 集成测试
- 测试完整的入库流程
- 测试完整的出库流程（包括FIFO策略）
- 测试完整的盘点流程
- 测试库存查询功能

## 注意事项

1. 入库确认和出库确认操作不可逆，请谨慎操作
2. 盘点完成后会直接调整库存，请确保盘点数据准确
3. FIFO出库策略基于生产日期，请确保入库时填写正确的生产日期
4. 批次号在同一仓库中必须唯一
5. 所有金额计算使用BigDecimal，避免精度丢失

## 版本历史

### v1.0.0 (2024-01-XX)
- 实现仓库和存储位置管理
- 实现入库管理功能
- 实现出库管理功能（支持FIFO策略）
- 实现库存查询功能
- 实现库存盘点功能
- 集成审计日志
- 完成API文档

