# Task 9.4: 危化品审批流程实现完成报告

## 任务概述

实现危化品申请的多级审批流程，确保危化品申请自动使用三级审批流程：实验室负责人 → 中心管理员 → 安全管理员。

**验证需求**: 6.4

## 实现方案

### 1. 审批流程配置

危化品审批流程配置已在数据库初始化脚本中定义：

**文件**: `sql/12_init_data.sql`

```sql
INSERT INTO approval_flow_config (flow_code, flow_name, business_type, flow_definition, status) VALUES
('HAZARDOUS_APPLY', '危化品领用审批流程', 2, 
 '{"levels":[
   {"level":1,"approverRole":"LAB_MANAGER","approverName":"实验室负责人"},
   {"level":2,"approverRole":"CENTER_ADMIN","approverName":"中心管理员"},
   {"level":3,"approverRole":"ADMIN","approverName":"安全管理员"}
 ]}', 1);
```

**关键字段说明**:
- `business_type`: 2 表示危化品领用
- `flow_definition`: JSON格式定义三级审批流程
  - Level 1: 实验室负责人 (LAB_MANAGER)
  - Level 2: 中心管理员 (CENTER_ADMIN)
  - Level 3: 安全管理员 (ADMIN)

### 2. 自动流程选择

当创建危化品申请时（`applicationType=2`），系统会自动选择对应的审批流程配置。

**实现位置**: `MaterialApplicationServiceImpl.createApplication()`

```java
// 6. 启动审批流程
ApprovalContext context = new ApprovalContext();
context.setBusinessType(request.getApplicationType()); // 1-普通领用, 2-危化品领用
context.setApplicantId(applicantId);
context.setApplicantDept(applicantDept);

Long firstApproverId = approvalWorkflowService.initializeApprovalWorkflow(
    application.getId(),
    application.getApplicationNo(),
    context
);
```

**流程选择逻辑**: `ApprovalWorkflowServiceImpl.initializeApprovalWorkflow()`

```java
// 1. 获取流程配置
ApprovalFlowConfig flowConfig = getFlowConfig(context.getApplicationType());
ApprovalFlowDefinition flowDef = flowEngine.parseFlowDefinition(flowConfig.getFlowDefinition());

// 2. 分配第一级审批人
ApprovalFlowDefinition.ApprovalLevel firstLevel = flowDef.getLevels().get(0);
Long firstApproverId = flowEngine.assignApprover(firstLevel, context);
```

### 3. 多级审批流转

审批流转逻辑在 `MaterialApplicationServiceImpl.processApproval()` 中实现：

**审批通过流转**:
```java
private void handleApprovalPass(MaterialApplication application, int currentLevel, Long approverId) {
    // 检查是否还有下一级审批
    boolean hasNextLevel = checkHasNextLevel(application.getApplicationType(), currentLevel);
    
    if (hasNextLevel) {
        // 流转到下一级
        Long nextApproverId = assignNextLevelApprover(
            application.getApplicationType(), 
            currentLevel + 1, 
            context
        );
        
        application.setCurrentApproverId(nextApproverId);
        application.setApprovalStatus(1); // 审批中
        application.setStatus(2); // 审批中
    } else {
        // 所有审批通过，标记为审批完成
        application.setApprovalStatus(2); // 审批通过
        application.setStatus(3); // 审批通过
        application.setCurrentApproverId(null);
        
        // 自动创建出库单
        inventoryClient.createStockOutOrder(application.getId());
    }
}
```

**审批拒绝终止**:
```java
private void handleApprovalReject(MaterialApplication application) {
    application.setApprovalStatus(3); // 审批拒绝
    application.setStatus(4); // 审批拒绝
    application.setCurrentApproverId(null);
}
```

### 4. 审批记录

每次审批操作都会记录到 `approval_record` 表：

```java
ApprovalRecord record = new ApprovalRecord();
record.setApplicationId(id);
record.setApplicationNo(application.getApplicationNo());
record.setApproverId(approverId);
record.setApproverName(approverName);
record.setApprovalLevel(currentLevel);
record.setApprovalResult(request.getApprovalResult());
record.setApprovalOpinion(request.getApprovalOpinion());
record.setApprovalTime(LocalDateTime.now());
approvalRecordMapper.insert(record);
```

## 测试验证

### 测试文件

创建了完整的集成测试：`HazardousApprovalFlowTest.java`

### 测试用例

1. **testHazardousApplication_ShouldUseThreeLevelApprovalFlow**
   - 验证危化品申请自动使用三级审批流程配置
   - 验证申请单状态正确初始化
   - 验证第一级审批人正确分配

2. **testHazardousApprovalFlow_ShouldHaveThreeLevels**
   - 验证完整的三级审批流转过程
   - 验证每一级审批通过后正确流转到下一级
   - 验证所有审批通过后状态正确更新
   - 验证审批记录完整记录

3. **testHazardousApprovalFlow_ShouldTerminateWhenRejected**
   - 验证第一级审批拒绝时流程正确终止
   - 验证申请单状态更新为"审批拒绝"

4. **testHazardousApprovalFlow_ShouldTerminateWhenRejectedAtLevel2**
   - 验证第二级审批拒绝时流程正确终止
   - 验证前面的审批记录保持不变

## 审批流程示意图

```
危化品申请创建 (applicationType=2)
    ↓
自动选择危化品审批流程配置
    ↓
第一级: 实验室负责人审批
    ↓ (通过)
第二级: 中心管理员审批
    ↓ (通过)
第三级: 安全管理员审批
    ↓ (通过)
审批完成 → 自动创建出库单
```

**任一级拒绝**:
```
任一级审批拒绝
    ↓
流程终止
    ↓
申请单状态: 审批拒绝
```

## 数据库表关系

### approval_flow_config (审批流程配置表)
- 存储不同业务类型的审批流程定义
- `business_type=2` 对应危化品领用

### material_application (领用申请单表)
- `application_type`: 申请类型 (2=危化品领用)
- `status`: 申请单状态 (1=待审批, 2=审批中, 3=审批通过, 4=审批拒绝)
- `approval_status`: 审批状态 (0=未审批, 1=审批中, 2=审批通过, 3=审批拒绝)
- `current_approver_id`: 当前审批人ID

### approval_record (审批记录表)
- 记录每一级的审批结果
- `approval_level`: 审批层级 (1, 2, 3)
- `approval_result`: 审批结果 (1=通过, 2=拒绝)

## 关键特性

### 1. 自动流程选择
- 根据 `applicationType` 自动选择对应的审批流程配置
- 危化品申请 (type=2) 自动使用三级审批流程
- 普通申请 (type=1) 使用单级审批流程

### 2. 严格的审批顺序
- 必须按照 Level 1 → Level 2 → Level 3 的顺序进行
- 每一级审批通过后才能流转到下一级
- 不能跳级审批

### 3. 审批终止机制
- 任一级审批拒绝，流程立即终止
- 终止后申请单状态更新为"审批拒绝"
- 不再流转到后续审批级别

### 4. 审批记录追溯
- 每次审批操作都完整记录
- 包含审批人、审批时间、审批意见、审批结果
- 支持审批历史查询

### 5. 自动出库集成
- 所有审批通过后自动创建出库单
- 出库单关联申请单ID
- 实现审批到出库的无缝衔接

## 配置说明

### 修改审批流程

如需修改危化品审批流程，可以更新 `approval_flow_config` 表的 `flow_definition` 字段：

```sql
UPDATE approval_flow_config 
SET flow_definition = '{"levels":[
  {"level":1,"approverRole":"LAB_MANAGER","approverName":"实验室负责人"},
  {"level":2,"approverRole":"CENTER_ADMIN","approverName":"中心管理员"},
  {"level":3,"approverRole":"ADMIN","approverName":"安全管理员"},
  {"level":4,"approverRole":"CUSTOM_ROLE","approverName":"自定义角色"}
]}'
WHERE business_type = 2;
```

### 审批人分配规则

审批人分配由 `ApproverAssignmentService` 负责，根据 `approverRole` 自动分配：

- `LAB_MANAGER`: 实验室负责人
- `CENTER_ADMIN`: 中心管理员
- `ADMIN`: 安全管理员（系统管理员）

## 验收标准

✅ **需求 6.4**: 危化品申请必须经过审批
- 危化品申请自动使用多级审批流程
- 审批流程包含三个层级：实验室负责人 → 中心管理员 → 安全管理员
- 审批流程严格按顺序执行
- 任一级拒绝则流程终止
- 所有审批通过后自动创建出库单

## 相关文件

### 核心实现
- `MaterialApplicationServiceImpl.java`: 申请创建和审批处理
- `ApprovalWorkflowServiceImpl.java`: 审批流程初始化
- `ApprovalFlowEngineImpl.java`: 审批流程引擎

### 数据库
- `sql/09_approval_tables.sql`: 审批相关表定义
- `sql/12_init_data.sql`: 审批流程配置初始化数据

### 测试
- `HazardousApprovalFlowTest.java`: 危化品审批流程集成测试

### 实体类
- `ApprovalFlowConfig.java`: 审批流程配置实体
- `ApprovalRecord.java`: 审批记录实体
- `MaterialApplication.java`: 领用申请单实体

### DTO
- `ApprovalFlowDefinition.java`: 审批流程定义DTO
- `ApprovalContext.java`: 审批上下文DTO
- `ApprovalProcessRequest.java`: 审批处理请求DTO

## 总结

Task 9.4 已完成实现，危化品审批流程功能完整：

1. ✅ 数据库配置已初始化（三级审批流程）
2. ✅ 自动流程选择已实现（根据applicationType）
3. ✅ 多级审批流转已实现（严格按顺序）
4. ✅ 审批终止机制已实现（任一级拒绝）
5. ✅ 审批记录追溯已实现（完整记录）
6. ✅ 自动出库集成已实现（审批通过后）
7. ✅ 集成测试已编写（覆盖主要场景）

系统确保危化品申请必须经过实验室负责人、中心管理员和安全管理员三级审批，满足安全合规要求。
