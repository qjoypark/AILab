# Task 10.4: 待办事项功能实现总结

## 任务概述

实现待办事项列表查询功能，聚合待审批申请和待处理预警，按优先级和截止时间排序。

**验证需求**: 18.8, 18.9

## 实现内容

### 1. 数据传输对象 (DTOs)

#### TodoItemDTO.java
统一的待办事项数据结构，包含：
- `type`: 待办事项类型（APPROVAL-待审批, ALERT-待处理预警）
- `businessId`: 业务ID
- `businessNo`: 业务编号（申请单号或预警编号）
- `title`: 标题
- `content`: 内容描述
- `priority`: 优先级（1-低, 2-中, 3-高, 4-紧急）
- `deadline`: 截止时间
- `applicantName`: 申请人/触发人姓名
- `applicantDept`: 申请部门

#### TodoListDTO.java
待办事项列表响应，包含：
- `total`: 待办事项总数
- `approvalCount`: 待审批数量
- `alertCount`: 待处理预警数量
- `list`: 待办事项列表

### 2. 服务层实现

#### TodoService.java (接口)
定义待办事项服务接口：
- `getTodoList(Long userId)`: 获取用户的待办事项列表

#### TodoServiceImpl.java (实现)
实现待办事项聚合逻辑：

**待审批申请聚合**:
- 调用 `ApprovalClient.getPendingApprovals()` 查询当前用户的待审批申请
- 将申请单转换为待办事项
- 危化品领用（类型2）优先级为3（高），普通领用优先级为2（中）
- 截止时间使用期望领用日期

**待处理预警聚合**:
- 查询 `alert_record` 表中状态为1（未处理）的预警记录
- 将预警记录转换为待办事项
- 预警级别映射到优先级：
  - 1-提示 → 优先级1（低）
  - 2-警告 → 优先级2（中）
  - 3-严重 → 优先级4（紧急）

**排序规则**:
1. 按优先级降序（优先级高的排在前面）
2. 相同优先级按截止时间升序（最早截止的排在前面）
3. 无截止时间的排在最后

### 3. 控制器层

#### TodoController.java
提供待办事项查询接口：

```
GET /api/v1/todo?userId={userId}
```

**请求参数**:
- `userId`: 用户ID（必填）

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 4,
    "approvalCount": 2,
    "alertCount": 2,
    "list": [
      {
        "type": "ALERT",
        "typeDesc": "待处理预警",
        "businessId": 2,
        "businessNo": "2",
        "title": "[严重] 有效期预警",
        "content": "试剂B即将过期",
        "priority": 4,
        "priorityDesc": "紧急",
        "deadline": null,
        "createdTime": "2024-03-17T03:00:00",
        "applicantName": null,
        "applicantDept": null
      },
      {
        "type": "APPROVAL",
        "typeDesc": "待审批",
        "businessId": 2,
        "businessNo": "APP002",
        "title": "[危化品领用] 李四 提交的领用申请",
        "content": "申请单号: APP002, 用途: 危化品实验",
        "priority": 3,
        "priorityDesc": "高",
        "deadline": "2024-03-18T00:00:00",
        "createdTime": "2024-03-17T06:00:00",
        "applicantName": "李四",
        "applicantDept": "生物系"
      }
    ]
  }
}
```

### 4. 客户端接口扩展

#### ApprovalClient.java
新增方法：
- `getPendingApprovals(Long approverId)`: 查询用户的待审批申请列表

#### ApprovalClientImpl.java
实现 `getPendingApprovals()` 方法：
- 调用审批服务的 `/api/v1/applications/pending?approverId={approverId}` 接口
- 将响应数据转换为 `MaterialApplicationDTO` 列表
- 异常处理：返回空列表

#### MaterialApplicationDTO.java
扩展字段：
- `expectedDate`: 期望领用日期
- `approvalStatus`: 审批状态
- `createdTime`: 创建时间

### 5. 单元测试

#### TodoServiceTest.java
测试用例：
1. ✅ 获取待办事项列表 - 包含待审批申请和待处理预警
2. ✅ 获取待办事项列表 - 仅包含待审批申请
3. ✅ 获取待办事项列表 - 仅包含待处理预警
4. ✅ 获取待办事项列表 - 空列表
5. ✅ 待办事项排序 - 按优先级和截止时间
6. ✅ 危化品领用申请优先级高于普通领用

#### TodoControllerTest.java
测试用例：
1. ✅ GET /api/v1/todo - 获取待办事项列表成功
2. ✅ GET /api/v1/todo - 空待办事项列表
3. ✅ GET /api/v1/todo - 缺少userId参数

## 需求验证

### 需求 18.8: "THE 系统 SHALL 在用户登录后显示待办事项列表"
✅ **已实现**: 
- 提供 `GET /api/v1/todo` 接口查询待办事项列表
- 聚合待审批申请和待处理预警
- 返回统一的待办事项数据结构

### 需求 18.9: "THE 系统 SHALL 按优先级和截止时间排序待办事项"
✅ **已实现**:
- 按优先级降序排序（优先级高的排在前面）
- 相同优先级按截止时间升序排序（最早截止的排在前面）
- 无截止时间的排在最后

## 优先级映射规则

### 待审批申请
- 危化品领用（类型2）: 优先级3（高）
- 普通领用（类型1）: 优先级2（中）

### 待处理预警
- 严重预警（级别3）: 优先级4（紧急）
- 警告预警（级别2）: 优先级2（中）
- 提示预警（级别1）: 优先级1（低）

## 技术实现亮点

1. **服务聚合**: 通过 `ApprovalClient` 调用审批服务，实现跨服务数据聚合
2. **统一数据模型**: 使用 `TodoItemDTO` 统一表示不同类型的待办事项
3. **智能排序**: 多维度排序（优先级 + 截止时间），确保重要事项优先展示
4. **优先级映射**: 根据业务类型和预警级别智能映射优先级
5. **异常处理**: 客户端调用失败时返回空列表，不影响整体功能
6. **完整测试**: 单元测试覆盖核心业务逻辑和边界情况

## 文件清单

### 新增文件
1. `lab-service-inventory/src/main/java/com/lab/inventory/dto/TodoItemDTO.java`
2. `lab-service-inventory/src/main/java/com/lab/inventory/dto/TodoListDTO.java`
3. `lab-service-inventory/src/main/java/com/lab/inventory/service/TodoService.java`
4. `lab-service-inventory/src/main/java/com/lab/inventory/service/impl/TodoServiceImpl.java`
5. `lab-service-inventory/src/main/java/com/lab/inventory/controller/TodoController.java`
6. `lab-service-inventory/src/test/java/com/lab/inventory/service/TodoServiceTest.java`
7. `lab-service-inventory/src/test/java/com/lab/inventory/controller/TodoControllerTest.java`

### 修改文件
1. `lab-service-inventory/src/main/java/com/lab/inventory/client/ApprovalClient.java` - 新增 `getPendingApprovals()` 方法
2. `lab-service-inventory/src/main/java/com/lab/inventory/client/impl/ApprovalClientImpl.java` - 实现 `getPendingApprovals()` 方法
3. `lab-service-inventory/src/main/java/com/lab/inventory/dto/MaterialApplicationDTO.java` - 新增字段

## 后续工作建议

1. **审批服务接口实现**: 在 `lab-service-approval` 中实现 `GET /api/v1/applications/pending` 接口
2. **Feign客户端替换**: 将 `ApprovalClientImpl` 的 RestTemplate 实现替换为 Feign 客户端
3. **用户权限验证**: 添加用户权限验证，确保只能查询自己的待办事项
4. **分页支持**: 如果待办事项数量较多，考虑添加分页功能
5. **实时推送**: 集成 WebSocket 或 SSE，实现待办事项实时推送
6. **待办事项统计**: 添加待办事项统计接口，用于首页展示

## 总结

本任务成功实现了待办事项功能，聚合了待审批申请和待处理预警，并按优先级和截止时间进行排序。实现符合需求 18.8 和 18.9 的要求，为用户提供了统一的待办事项视图，提升了工作效率。
