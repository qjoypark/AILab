# Task 9.9: 危化品账实差异计算实现总结

## 任务概述

实现定时任务（每日执行）计算危化品账实差异，并在差异超过5%时触发预警。

## 实现内容

### 1. 核心服务实现

#### 1.1 HazardousDiscrepancyService
- **位置**: `lab-service-inventory/src/main/java/com/lab/inventory/service/HazardousDiscrepancyService.java`
- **功能**: 定义危化品账实差异计算服务接口

#### 1.2 HazardousDiscrepancyServiceImpl
- **位置**: `lab-service-inventory/src/main/java/com/lab/inventory/service/impl/HazardousDiscrepancyServiceImpl.java`
- **功能**: 实现危化品账实差异计算逻辑
- **计算公式**:
  - 账实差异 = (账面库存 - 实际库存) / 账面库存 × 100%
  - 实际库存 = 账面库存 - 已领用未归还数量
- **预警规则**: 当差异绝对值超过5%时，创建严重级别预警

### 2. 定时任务配置

#### 2.1 AlertScheduledTask
- **位置**: `lab-service-inventory/src/main/java/com/lab/inventory/task/AlertScheduledTask.java`
- **新增方法**: `calculateHazardousDiscrepancy()`
- **执行时间**: 每天凌晨2:00执行
- **Cron表达式**: `0 0 2 * * ?`

### 3. 服务间调用接口

#### 3.1 MaterialClient扩展
- **位置**: `lab-service-inventory/src/main/java/com/lab/inventory/client/MaterialClient.java`
- **新增方法**: `getHazardousMaterials()` - 获取所有危化品列表
- **实现位置**: `lab-service-inventory/src/main/java/com/lab/inventory/client/impl/MaterialClientImpl.java`

#### 3.2 ApprovalClient扩展
- **位置**: `lab-service-inventory/src/main/java/com/lab/inventory/client/ApprovalClient.java`
- **新增方法**: `getUnreturnedQuantity(Long materialId)` - 获取危化品已领用未归还数量
- **实现位置**: `lab-service-inventory/src/main/java/com/lab/inventory/client/impl/ApprovalClientImpl.java`

### 4. 审批服务API扩展

#### 4.1 HazardousUsageRecordService
- **位置**: `lab-service-approval/src/main/java/com/lab/approval/service/HazardousUsageRecordService.java`
- **新增方法**: `getUnreturnedQuantity(Long materialId)` - 查询已领用未归还数量
- **实现位置**: `lab-service-approval/src/main/java/com/lab/approval/service/impl/HazardousUsageRecordServiceImpl.java`
- **查询逻辑**: 汇总status=1(使用中)的记录的receivedQuantity

#### 4.2 HazardousUsageRecordController
- **位置**: `lab-service-approval/src/main/java/com/lab/approval/controller/HazardousUsageRecordController.java`
- **新增接口**: `GET /api/v1/hazardous/usage-records/unreturned/{materialId}`
- **功能**: 返回指定危化品的已领用未归还数量

### 5. 药品服务API扩展

#### 5.1 MaterialService
- **位置**: `lab-service-material/src/main/java/com/lab/material/service/MaterialService.java`
- **新增方法**: `getHazardousMaterials()` - 查询所有危化品
- **实现位置**: `lab-service-material/src/main/java/com/lab/material/service/impl/MaterialServiceImpl.java`
- **查询条件**: materialType=3 或 isControlled>0

#### 5.2 MaterialController
- **位置**: `lab-service-material/src/main/java/com/lab/material/controller/MaterialController.java`
- **新增接口**: `GET /api/v1/materials/hazardous`
- **功能**: 返回所有危化品列表

### 6. 单元测试

#### 6.1 HazardousDiscrepancyServiceTest
- **位置**: `lab-service-inventory/src/test/java/com/lab/inventory/service/HazardousDiscrepancyServiceTest.java`
- **测试场景**:
  1. 无危化品时不计算差异
  2. 账面库存为0时跳过计算
  3. 差异小于5%时不触发预警
  4. 差异超过5%时触发预警
  5. 多个危化品计算
  6. 异常处理不影响其他材料

## 实现流程

```
定时任务(每日2:00) 
  ↓
获取所有危化品列表(MaterialClient)
  ↓
遍历每个危化品
  ↓
查询账面库存(StockInventoryMapper)
  ↓
查询已领用未归还数量(ApprovalClient)
  ↓
计算实际库存 = 账面库存 - 已领用未归还数量
  ↓
计算账实差异 = (账面库存 - 实际库存) / 账面库存 × 100%
  ↓
如果|差异| > 5%
  ↓
创建预警记录(AlertService)
```

## 数据流

1. **库存服务** → **药品服务**: 获取危化品列表
2. **库存服务** → **库存数据库**: 查询账面库存
3. **库存服务** → **审批服务**: 获取已领用未归还数量
4. **审批服务** → **审批数据库**: 查询使用中的记录
5. **库存服务** → **预警服务**: 创建预警记录

## 预警信息格式

```
标题: 危化品账实差异预警

内容:
危化品: [药品名称]
账面库存: [数量] [单位]
已领用未归还: [数量] [单位]
实际库存: [数量] [单位]
账实差异: [百分比]%
预警原因: 账实差异绝对值超过5%
```

## 验证需求

- **需求 6.7**: 危化品账实差异计算
  - 账实差异 = (账面库存 - 实际库存) / 账面库存 × 100%
  - 实际库存 = 账面库存 - 已领用未归还数量

## 技术要点

1. **定时任务**: 使用Spring的@Scheduled注解，每日凌晨2:00执行
2. **服务间调用**: 通过RestTemplate实现HTTP调用（后续可改为Feign）
3. **事务管理**: 使用@Transactional确保数据一致性
4. **异常处理**: 单个材料计算失败不影响其他材料
5. **精度控制**: 使用BigDecimal进行金额和数量计算，保留4位小数
6. **预警级别**: 账实差异预警为严重级别(level=3)

## 注意事项

1. 定时任务在凌晨2:00执行，避免业务高峰期
2. 只计算启用状态(status=1)的危化品
3. 账面库存为0的材料跳过计算
4. 差异计算使用绝对值，正负差异都会触发预警
5. 预警类型为4(账实差异)，业务类型为HAZARDOUS_MATERIAL
6. 服务间调用失败时返回默认值(0)，不影响整体流程

## 后续优化建议

1. 将RestTemplate调用改为Feign声明式调用
2. 添加缓存机制，减少重复查询
3. 支持手动触发计算
4. 添加计算历史记录表，保存每次计算结果
5. 支持自定义预警阈值（目前固定为5%）
6. 添加计算结果的可视化报表

## 相关文件清单

### 新增文件
1. `lab-service-inventory/src/main/java/com/lab/inventory/service/HazardousDiscrepancyService.java`
2. `lab-service-inventory/src/main/java/com/lab/inventory/service/impl/HazardousDiscrepancyServiceImpl.java`
3. `lab-service-inventory/src/test/java/com/lab/inventory/service/HazardousDiscrepancyServiceTest.java`

### 修改文件
1. `lab-service-inventory/src/main/java/com/lab/inventory/task/AlertScheduledTask.java`
2. `lab-service-inventory/src/main/java/com/lab/inventory/client/MaterialClient.java`
3. `lab-service-inventory/src/main/java/com/lab/inventory/client/impl/MaterialClientImpl.java`
4. `lab-service-inventory/src/main/java/com/lab/inventory/client/ApprovalClient.java`
5. `lab-service-inventory/src/main/java/com/lab/inventory/client/impl/ApprovalClientImpl.java`
6. `lab-service-approval/src/main/java/com/lab/approval/service/HazardousUsageRecordService.java`
7. `lab-service-approval/src/main/java/com/lab/approval/service/impl/HazardousUsageRecordServiceImpl.java`
8. `lab-service-approval/src/main/java/com/lab/approval/controller/HazardousUsageRecordController.java`
9. `lab-service-material/src/main/java/com/lab/material/service/MaterialService.java`
10. `lab-service-material/src/main/java/com/lab/material/service/impl/MaterialServiceImpl.java`
11. `lab-service-material/src/main/java/com/lab/material/controller/MaterialController.java`

## 测试建议

### 单元测试
- ✅ 已创建HazardousDiscrepancyServiceTest，覆盖主要场景

### 集成测试
1. 启动所有服务（material、approval、inventory）
2. 创建测试危化品数据
3. 创建库存记录
4. 创建使用记录（status=1）
5. 手动调用calculateDiscrepancy()方法
6. 验证预警记录是否正确创建

### 定时任务测试
1. 修改cron表达式为每分钟执行：`0 * * * * ?`
2. 观察日志输出
3. 验证预警记录
4. 恢复原cron表达式

## 完成状态

✅ 核心功能实现完成
✅ 服务间接口扩展完成
✅ 定时任务配置完成
✅ 单元测试编写完成
✅ 代码无编译错误
✅ 符合需求6.7规范

任务已完成，可以进行集成测试和验收。
