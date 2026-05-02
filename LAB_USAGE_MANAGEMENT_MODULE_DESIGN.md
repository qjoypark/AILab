# 实验室使用管理模块设计开发文件

## 1. 背景与目标

本模块用于在现有智慧实验室管理系统中增加“实验室使用管理”能力。教师可以发起实验室使用申请，系统管理员可以维护实验室基础信息及实验室管理人员。申请提交后走统一审批系统，审批顺序为：

1. 实验室管理人员审批
2. 中心主任审批
3. 分管院长审批
4. 院长审批

审批通过后，系统在实验室使用视图中展示“某实验室在某时段由哪些老师使用”。同一个实验室允许多个教师在同一时段共享使用，因此实验室使用申请不能按 `lab_id + start_time + end_time` 做唯一性约束，也不能用普通预约系统的“时间冲突直接拒绝”逻辑。

设计目标：

- 复用药品申请的审批流程、审批记录、审批人分配能力，避免为实验室申请另起一套审批代码。
- 实验室使用申请与药品领用申请在业务数据上分表，在审批系统上共用流程引擎。
- 系统管理员能维护实验室管理人员，实验室管理人员只审批和查看自己负责实验室的数据。
- 不同用户进入系统后看到的菜单、列表、按钮和数据范围不同。
- 后续实现时先打通最小闭环，再补齐统计、导出、通知等增强能力。

## 2. 当前系统可复用点

从现有代码看，审批服务已有这些基础：

- `lab-service-approval` 中已有 `ApprovalWorkflowService`、`ApprovalFlowEngine`、`ApproverAssignmentService`。
- `approval_flow_config` 通过 `business_type` 区分流程，`flow_definition` 保存 JSON 审批层级。
- `approval_record` 记录审批历史。
- 前端 `lab-web-admin` 已有 `approvalApi`、`ApplicationList.vue`、`ApprovalTodo.vue` 这类药品申请和待审批页面模式。
- 前端权限常量集中在 `lab-web-admin/src/constants/permissions.ts`，菜单集中在 `lab-web-admin/src/composables/useMenu.ts`，路由集中在 `lab-web-admin/src/router/index.ts`。
- 系统角色和权限已有 RBAC 基础表：`sys_user`、`sys_role`、`sys_user_role`、`sys_permission`、`sys_role_permission`。

需要注意的现状：

- 导出的 `lab_management.sql` 中角色码有 `001` 院长、`002` 分管院长、`003` 实验中心主任、`005` 实验中心管理人员、`006` 教师。
- 初始化脚本 `sql/12_init_data.sql` 中又有 `CENTER_ADMIN`、`LAB_MANAGER` 等英文角色码。
- 后续实现时建议统一采用“审批角色编码”做抽象层，例如 `LAB_ROOM_MANAGER`、`CENTER_DIRECTOR`、`DEPUTY_DEAN`、`DEAN`，再通过配置映射到实际 `sys_role.role_code`，避免代码直接写死 `001/002/003/005`。

## 3. 业务角色与权限

### 3.1 角色定义

| 角色 | 现有可能角色码 | 主要职责 |
| --- | --- | --- |
| 系统管理员 | `ADMIN` | 维护实验室、维护实验室管理人员、维护流程配置、查看全部数据 |
| 实验室管理人员 | `LAB_ROOM_MANAGER` 或 `005` | 审批自己负责实验室的使用申请，查看自己负责实验室的申请和使用情况 |
| 中心主任 | `CENTER_DIRECTOR` 或 `003` | 第二级审批，查看中心范围申请和使用情况 |
| 分管院长 | `DEPUTY_DEAN` 或 `002` | 第三级审批，查看院级审批范围申请和使用情况 |
| 院长 | `DEAN` 或 `001` | 第四级审批，查看院级审批范围申请和使用情况 |
| 教师 | `TEACHER` 或 `006` | 发起申请、查看自己的申请、查看已审批通过的实验室使用安排 |

### 3.2 权限点设计

新增权限建议：

| 权限码 | 名称 | 建议分配 |
| --- | --- | --- |
| `lab-room:list` | 实验室列表查看 | 管理员、实验室管理人员、中心主任、分管院长、院长、教师 |
| `lab-room:create` | 实验室新增 | 管理员 |
| `lab-room:update` | 实验室编辑 | 管理员 |
| `lab-room:delete` | 实验室删除 | 管理员 |
| `lab-room:manager:update` | 实验室管理人员配置 | 管理员 |
| `lab-usage:list` | 实验室使用申请查看 | 管理员、实验室管理人员、中心主任、分管院长、院长、教师 |
| `lab-usage:create` | 实验室使用申请创建 | 教师、管理员 |
| `lab-usage:cancel` | 实验室使用申请取消 | 申请人、管理员 |
| `lab-usage:approve` | 实验室使用申请审批 | 实验室管理人员、中心主任、分管院长、院长、管理员 |
| `lab-usage:schedule:view` | 实验室使用安排查看 | 全部业务角色 |

### 3.3 数据可见范围

| 用户类型 | 可见数据 |
| --- | --- |
| 系统管理员 | 全部实验室、全部申请、全部审批记录、全部使用安排 |
| 实验室管理人员 | 自己负责的实验室、这些实验室的申请、自己待审批的申请、这些实验室已通过的使用安排 |
| 中心主任 | 全部或中心范围内实验室申请，重点是自己待审批和已审批记录 |
| 分管院长 | 全部或院级范围内实验室申请，重点是自己待审批和已审批记录 |
| 院长 | 全部或院级范围内实验室申请，重点是自己待审批和最终审批记录 |
| 教师 | 自己提交或自己被列为共同使用人的申请；可查看已通过的公共使用安排，但不能看到无关审批意见中的敏感管理备注 |

后端必须做数据权限过滤，前端菜单隐藏只能作为体验优化，不能作为安全边界。

## 4. 核心业务规则

### 4.1 实验室基础信息

实验室应包含：

- 实验室编号：如 `A101`
- 实验室名称：如 `植物生理实验室`
- 所在楼宇、楼层、房间号
- 容纳人数
- 实验室类型：教学、科研、公共平台、危化相关等
- 状态：启用、停用、维护中
- 主要设备说明
- 安全等级或注意事项
- 管理人员列表

### 4.2 实验室管理人员

- 一个实验室可以有多个管理人员。
- 一个管理人员可以负责多个实验室。
- 系统管理员可增删实验室管理人员关系。
- 实验室申请第一级审批人从该实验室的管理人员中产生。
- 如果一个实验室没有配置管理人员，教师提交申请时应给出明确提示，或者允许提交但进入“待分配管理人员”异常状态。建议第一期采用“没有管理人员则不允许提交”，能减少审批悬空。

### 4.3 使用申请

教师申请字段建议：

- 申请单号
- 申请人
- 申请部门
- 实验室
- 使用开始时间
- 使用结束时间
- 使用用途：教学、科研、竞赛、培训、其他
- 课程或项目名称
- 预计人数
- 共同使用教师，可选多名
- 学生班级或参与对象说明
- 是否需要特殊设备
- 安全承诺或注意事项确认
- 备注

校验规则：

- 开始时间必须早于结束时间。
- 申请实验室必须启用。
- 申请人必须具备 `lab-usage:create` 权限。
- 申请提交时只检查时间合法性，不阻止同一实验室同一时段已有其他通过申请。
- 可选做“共享提醒”：如果同一实验室同一时段已有通过或审批中的申请，前端展示提示，后端返回 `overlapApplications` 供用户参考，但不作为拒绝条件。

### 4.4 共享使用展示

审批通过后，系统在“实验室使用安排”中展示：

- 实验室
- 使用日期和时间段
- 使用教师列表，包含申请人和共同使用教师
- 用途或课程/项目名称
- 申请单状态
- 当前是否正在使用

同一实验室同一时间段允许多条已通过申请同时存在。展示层需要按实验室和时间聚合：

- 列表视图：每条申请一行。
- 日历/时间轴视图：同一实验室同一时段聚合显示多个教师。
- 实验室详情页：展示该实验室的历史、当前、未来使用安排。

## 5. 数据模型设计

### 5.1 新增业务表

#### `lab_room`

实验室基础信息表。

```sql
CREATE TABLE lab_room (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实验室ID',
  room_code VARCHAR(50) NOT NULL COMMENT '实验室编号',
  room_name VARCHAR(100) NOT NULL COMMENT '实验室名称',
  building VARCHAR(100) COMMENT '楼宇',
  floor VARCHAR(50) COMMENT '楼层',
  room_no VARCHAR(50) COMMENT '房间号',
  capacity INT COMMENT '容纳人数',
  room_type TINYINT DEFAULT 1 COMMENT '类型:1-教学,2-科研,3-公共平台,4-其他',
  safety_level TINYINT DEFAULT 1 COMMENT '安全等级:1-普通,2-重点,3-高风险',
  equipment_summary TEXT COMMENT '主要设备说明',
  notice TEXT COMMENT '使用注意事项',
  status TINYINT DEFAULT 1 COMMENT '状态:0-停用,1-启用,2-维护中',
  created_by BIGINT COMMENT '创建人',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_by BIGINT COMMENT '更新人',
  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记:0-未删除,1-已删除',
  UNIQUE KEY uk_lab_room_code (room_code),
  INDEX idx_lab_room_status (status),
  INDEX idx_lab_room_type (room_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室房间表';
```

#### `lab_room_manager`

实验室与管理人员关系表。

```sql
CREATE TABLE lab_room_manager (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  lab_room_id BIGINT NOT NULL COMMENT '实验室ID',
  manager_id BIGINT NOT NULL COMMENT '管理人员用户ID',
  manager_name VARCHAR(50) NOT NULL COMMENT '管理人员姓名快照',
  is_primary TINYINT DEFAULT 0 COMMENT '是否主负责人:0-否,1-是',
  status TINYINT DEFAULT 1 COMMENT '状态:0-停用,1-启用',
  created_by BIGINT COMMENT '创建人',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_lab_room_manager (lab_room_id, manager_id),
  INDEX idx_lab_room_manager_room (lab_room_id),
  INDEX idx_lab_room_manager_user (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室管理人员关系表';
```

#### `lab_usage_application`

实验室使用申请表。

```sql
CREATE TABLE lab_usage_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
  application_no VARCHAR(50) NOT NULL COMMENT '申请单号',
  applicant_id BIGINT NOT NULL COMMENT '申请人ID',
  applicant_name VARCHAR(50) NOT NULL COMMENT '申请人姓名',
  applicant_dept VARCHAR(100) COMMENT '申请部门',
  lab_room_id BIGINT NOT NULL COMMENT '实验室ID',
  lab_room_code VARCHAR(50) NOT NULL COMMENT '实验室编号快照',
  lab_room_name VARCHAR(100) NOT NULL COMMENT '实验室名称快照',
  usage_type TINYINT DEFAULT 1 COMMENT '用途类型:1-教学,2-科研,3-竞赛,4-培训,5-其他',
  usage_purpose VARCHAR(500) NOT NULL COMMENT '使用目的',
  project_name VARCHAR(200) COMMENT '课程或项目名称',
  expected_attendee_count INT COMMENT '预计人数',
  start_time DATETIME NOT NULL COMMENT '使用开始时间',
  end_time DATETIME NOT NULL COMMENT '使用结束时间',
  special_equipment TEXT COMMENT '特殊设备需求',
  safety_commitment TINYINT DEFAULT 0 COMMENT '是否确认安全承诺:0-否,1-是',
  status TINYINT DEFAULT 1 COMMENT '状态:1-待审批,2-审批中,3-审批通过,4-审批拒绝,5-已取消',
  approval_status TINYINT DEFAULT 1 COMMENT '审批状态:1-审批中,2-审批通过,3-审批拒绝',
  current_approver_id BIGINT COMMENT '当前审批人ID',
  current_approver_role VARCHAR(50) COMMENT '当前审批角色',
  remark TEXT COMMENT '备注',
  created_by BIGINT COMMENT '创建人',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_by BIGINT COMMENT '更新人',
  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记:0-未删除,1-已删除',
  UNIQUE KEY uk_lab_usage_application_no (application_no),
  INDEX idx_lab_usage_applicant (applicant_id),
  INDEX idx_lab_usage_room_time (lab_room_id, start_time, end_time),
  INDEX idx_lab_usage_status (status, approval_status),
  INDEX idx_lab_usage_current_approver (current_approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室使用申请表';
```

这里特意不增加 `lab_room_id + start_time + end_time` 唯一索引，因为同一实验室同一时段允许共享使用。

#### `lab_usage_participant`

共同使用教师表。

```sql
CREATE TABLE lab_usage_participant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  application_id BIGINT NOT NULL COMMENT '实验室使用申请ID',
  user_id BIGINT NOT NULL COMMENT '共同使用教师ID',
  real_name VARCHAR(50) NOT NULL COMMENT '共同使用教师姓名快照',
  dept_name VARCHAR(100) COMMENT '部门名称快照',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_lab_usage_participant (application_id, user_id),
  INDEX idx_lab_usage_participant_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室使用共同教师表';
```

### 5.2 审批通用表改造

当前 `approval_record` 只有 `application_id`，药品申请和实验室申请都可能出现相同 ID。为了共用审批系统，建议把审批对象抽象出来。

推荐兼容式改造：

```sql
ALTER TABLE approval_record
  ADD COLUMN business_type INT NOT NULL DEFAULT 1 COMMENT '业务类型:1-普通药品领用,2-危化品领用,3-实验室使用申请' AFTER id,
  ADD COLUMN business_no VARCHAR(50) NULL COMMENT '业务单号' AFTER business_type,
  ADD INDEX idx_approval_record_business (business_type, application_id);
```

说明：

- 第一阶段可以继续使用 `application_id` 字段表示“业务单ID”，减少实体大改。
- 文档和代码中逐步把它称为 `businessId`，后续再考虑数据库字段重命名。
- 旧数据 `business_type` 默认为 `1`，兼容普通药品领用。
- 审批历史查询必须按 `business_type + application_id` 查询，不能只按 ID 查询。

`approval_flow_config.business_type` 新增业务类型：

| business_type | 业务 |
| --- | --- |
| `1` | 普通药品领用 |
| `2` | 危化品领用 |
| `3` | 实验室使用申请 |

实验室使用申请默认流程：

```json
{
  "levels": [
    {
      "level": 1,
      "approverRole": "LAB_ROOM_MANAGER",
      "approverName": "实验室管理人员"
    },
    {
      "level": 2,
      "approverRole": "CENTER_DIRECTOR",
      "approverName": "中心主任"
    },
    {
      "level": 3,
      "approverRole": "DEPUTY_DEAN",
      "approverName": "分管院长"
    },
    {
      "level": 4,
      "approverRole": "DEAN",
      "approverName": "院长"
    }
  ]
}
```

### 5.3 审批上下文扩展

`ApprovalContext` 建议扩展字段：

| 字段 | 用途 |
| --- | --- |
| `businessType` | 通用审批业务类型 |
| `businessId` | 通用业务单ID，可先映射到现有 `applicationId` |
| `businessNo` | 通用业务单号 |
| `labRoomId` | 实验室ID，第一级审批人分配需要 |
| `applicantDept` | 申请部门，后续可做数据范围 |
| `applicantId` | 申请人 |

## 6. 统一审批系统设计

### 6.1 设计原则

审批引擎只负责：

- 根据 `business_type` 找流程配置。
- 解析流程 JSON。
- 根据当前层级找到候选审批人。
- 记录审批动作。
- 返回下一步结果：进入下一层、全部通过、拒绝、转审。

具体业务服务负责：

- 创建业务申请单。
- 更新业务申请单状态。
- 更新当前审批人。
- 审批全部通过后的业务动作。
- 判断数据权限。

这样药品申请和实验室申请能共用审批流程，但不会把实验室字段塞进药品申请表。

### 6.2 业务适配器

建议新增一个小接口，让审批系统回调不同业务：

```java
public interface ApprovalBusinessAdapter {
    Integer supportBusinessType();

    ApprovalBusinessInfo getBusinessInfo(Long businessId);

    void updateCurrentApprover(Long businessId, Long approverId, String approverRole);

    void markApproved(Long businessId);

    void markRejected(Long businessId);

    void markTransferred(Long businessId, Long transferToUserId);
}
```

药品申请实现 `MaterialApplicationApprovalAdapter`，实验室申请实现 `LabUsageApprovalAdapter`。审批工作流内部用 `businessType` 找对应 adapter。

这样可以替代在 `ApprovalWorkflowServiceImpl` 里写 `if businessType == 1/2/3` 的大分支。

### 6.3 审批人分配

审批角色到用户的解析规则：

| 审批角色 | 分配规则 |
| --- | --- |
| `LAB_ROOM_MANAGER` | 查询 `lab_room_manager` 中该实验室启用的管理人员，优先 `is_primary = 1`，否则按用户ID升序 |
| `CENTER_DIRECTOR` | 查询角色码映射后的中心主任用户 |
| `DEPUTY_DEAN` | 查询角色码映射后的分管院长用户 |
| `DEAN` | 查询角色码映射后的院长用户 |

角色映射建议配置化：

```yaml
approval:
  role-mapping:
    LAB_ROOM_MANAGER: ["LAB_ROOM_MANAGER", "005"]
    CENTER_DIRECTOR: ["CENTER_DIRECTOR", "003", "CENTER_ADMIN"]
    DEPUTY_DEAN: ["DEPUTY_DEAN", "002"]
    DEAN: ["DEAN", "001"]
```

如果配置化暂时不做，可以先在 `ApproverAssignmentServiceImpl` 中集中维护一个 `Map<String, List<String>>`，不要散落在 controller 或 service 中。

### 6.4 多个实验室管理人员的审批规则

第一期建议采用“任一管理人员审批即可进入下一层”：

- 当前审批人可以保存主负责人 ID。
- 详情页展示当前可审批人列表。
- 后端审批时校验当前用户是否在当前层候选审批人列表中，而不是只判断是否等于单个 `current_approver_id`。

如果短期不想改太大，可以先由系统自动选主负责人作为 `current_approver_id`。但文档建议最终支持候选人列表，否则多个实验室管理人员的权限体验会不自然。

## 7. 后端模块设计

### 7.1 建议放置位置

为了复用审批服务，实验室使用申请相关后端建议放在 `lab-service-approval`：

- `com.lab.approval.controller.LabRoomController`
- `com.lab.approval.controller.LabUsageApplicationController`
- `com.lab.approval.entity.LabRoom`
- `com.lab.approval.entity.LabRoomManager`
- `com.lab.approval.entity.LabUsageApplication`
- `com.lab.approval.entity.LabUsageParticipant`
- `com.lab.approval.dto.LabRoomDTO`
- `com.lab.approval.dto.LabUsageApplicationDTO`
- `com.lab.approval.dto.CreateLabUsageApplicationRequest`
- `com.lab.approval.dto.LabUsageScheduleDTO`
- `com.lab.approval.mapper.LabRoomMapper`
- `com.lab.approval.mapper.LabRoomManagerMapper`
- `com.lab.approval.mapper.LabUsageApplicationMapper`
- `com.lab.approval.mapper.LabUsageParticipantMapper`
- `com.lab.approval.service.LabRoomService`
- `com.lab.approval.service.LabUsageApplicationService`
- `com.lab.approval.service.impl.LabRoomServiceImpl`
- `com.lab.approval.service.impl.LabUsageApplicationServiceImpl`

理由：

- 实验室申请审批和药品申请审批共用 `ApprovalWorkflowService`，放同一服务减少跨服务调用。
- 实验室基础信息暂时不复杂，没必要单独拆 `lab-service-room`。
- 后续如果实验室管理模块变大，可以再从审批服务中拆分。

### 7.2 后端接口

#### 实验室管理

| 方法 | 路径 | 功能 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/lab-rooms` | 分页查询实验室 | `lab-room:list` |
| `GET` | `/api/v1/lab-rooms/{id}` | 查看实验室详情 | `lab-room:list` |
| `POST` | `/api/v1/lab-rooms` | 新增实验室 | `lab-room:create` |
| `PUT` | `/api/v1/lab-rooms/{id}` | 编辑实验室 | `lab-room:update` |
| `DELETE` | `/api/v1/lab-rooms/{id}` | 删除实验室 | `lab-room:delete` |
| `GET` | `/api/v1/lab-rooms/{id}/managers` | 查询管理人员 | `lab-room:list` |
| `PUT` | `/api/v1/lab-rooms/{id}/managers` | 保存管理人员 | `lab-room:manager:update` |

#### 实验室使用申请

| 方法 | 路径 | 功能 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/lab-usage-applications` | 查询申请列表 | `lab-usage:list` |
| `GET` | `/api/v1/lab-usage-applications/pending` | 查询我的待审批 | `lab-usage:approve` |
| `GET` | `/api/v1/lab-usage-applications/{id}` | 查看申请详情 | `lab-usage:list` |
| `POST` | `/api/v1/lab-usage-applications` | 创建使用申请 | `lab-usage:create` |
| `POST` | `/api/v1/lab-usage-applications/{id}/cancel` | 取消申请 | `lab-usage:cancel` |
| `POST` | `/api/v1/lab-usage-applications/{id}/approve` | 审批申请 | `lab-usage:approve` |
| `GET` | `/api/v1/lab-usage-schedules` | 查询实验室使用安排 | `lab-usage:schedule:view` |

### 7.3 申请状态流转

| 状态 | 含义 | 触发 |
| --- | --- | --- |
| `1` | 待审批 | 教师提交申请后 |
| `2` | 审批中 | 已进入多级审批流程，可与待审批合并显示 |
| `3` | 审批通过 | 院长审批通过后 |
| `4` | 审批拒绝 | 任一级审批拒绝后 |
| `5` | 已取消 | 申请人或管理员取消后 |

建议第一期创建申请后直接设为：

- `status = 2`
- `approval_status = 1`
- `current_approver_id = 第一级审批人`
- `current_approver_role = LAB_ROOM_MANAGER`

### 7.4 关键服务流程

创建实验室使用申请：

1. 读取当前登录用户。
2. 校验用户可创建实验室使用申请。
3. 校验实验室存在且启用。
4. 校验开始结束时间。
5. 校验实验室已配置管理人员。
6. 保存 `lab_usage_application`。
7. 保存 `lab_usage_participant`。
8. 构造 `ApprovalContext`，包含 `businessType = 3`、`labRoomId`。
9. 调用统一审批服务初始化流程。
10. 更新当前审批人和当前审批角色。
11. 返回申请ID和共享提醒信息。

审批实验室使用申请：

1. 校验申请存在且处于审批中。
2. 校验当前用户是否有 `lab-usage:approve`。
3. 校验当前用户是否为当前层级候选审批人。
4. 调用统一审批服务记录审批动作。
5. 如果通过且还有下一层，更新当前审批人和角色。
6. 如果全部通过，更新申请为审批通过。
7. 如果拒绝，更新申请为审批拒绝。
8. 返回最新申请详情。

查询实验室使用安排：

1. 只查询 `status = 3` 或 `approval_status = 2` 的申请。
2. 按时间范围、实验室、教师过滤。
3. 教师默认可以看到通过审批的公共安排，但详情中的审批意见按权限脱敏。
4. 实验室管理人员默认只看自己管理实验室。

## 8. 前端模块设计

### 8.1 新增 API 和类型

建议新增：

- `lab-web-admin/src/api/lab.ts`
- `lab-web-admin/src/types/lab.ts`

主要类型：

- `LabRoom`
- `LabRoomForm`
- `LabRoomManager`
- `LabUsageApplication`
- `LabUsageApplicationForm`
- `LabUsageSchedule`
- `LabUsageQuery`
- `LabUsageApprovalRequest`

### 8.2 新增页面

建议页面：

| 页面 | 路由 | 功能 |
| --- | --- | --- |
| 实验室列表 | `/labs/rooms` | 查询实验室，管理员可新增编辑删除 |
| 实验室详情 | `/labs/rooms/:id` 或抽屉 | 查看基础信息、管理人员、近期使用 |
| 管理人员配置 | 可集成在实验室详情中 | 管理员为实验室配置管理人员 |
| 使用申请列表 | `/labs/usage-applications` | 教师看我的申请，管理人员和领导看权限范围内申请 |
| 新建使用申请 | 弹窗或 `/labs/usage-applications/create` | 教师创建申请 |
| 待审批事项 | 可复用 `/approval/todo`，增加业务类型筛选 | 统一展示药品和实验室待审批 |
| 使用安排 | `/labs/schedules` | 按实验室和时间查看通过审批的使用情况 |

### 8.3 菜单设计

在 `useMenu.ts` 中新增一级菜单“实验室管理”：

- 实验室列表：管理员、实验室管理人员、领导、教师都可看，但按钮不同。
- 使用申请：教师可发起，审批人可审批。
- 使用安排：所有业务角色可看。

菜单可见性基于新增权限常量：

```ts
export const LAB_ROOM_PERMISSIONS = [
  'lab-room:list',
  'lab-room:create',
  'lab-room:update',
  'lab-room:delete',
  'lab-room:manager:update'
] as const

export const LAB_USAGE_PERMISSIONS = [
  'lab-usage:list',
  'lab-usage:create',
  'lab-usage:cancel',
  'lab-usage:approve',
  'lab-usage:schedule:view'
] as const
```

### 8.4 页面权限差异

实验室列表：

- 管理员：显示新增、编辑、删除、配置管理人员。
- 实验室管理人员：只显示查看详情和负责实验室的近期使用。
- 教师：只显示查看详情和申请使用。
- 领导：显示查看详情、使用安排和审批相关入口。

使用申请列表：

- 教师：默认展示“我的申请”，显示“新建申请”“取消未审批申请”。
- 实验室管理人员：默认展示“我负责实验室的申请”和“待我审批”。
- 中心主任、分管院长、院长：默认展示“待我审批”和“已审批”。
- 管理员：可查看全部，必要时可取消异常申请。

申请详情：

- 申请人和审批人都可看申请基本信息。
- 审批按钮只在当前用户是当前层级候选审批人时显示。
- 审批历史所有相关人可见，但普通教师不显示内部管理备注字段。

使用安排：

- 默认按周展示。
- 支持按实验室、教师、日期范围筛选。
- 同一实验室同一时段多个教师共享时，在同一时间块中聚合显示教师姓名，不判定冲突。

## 9. SQL 脚本规划

建议新增脚本：

- `sql/17_lab_usage_tables.sql`：新增实验室、管理人员、使用申请、共同教师表。
- `sql/18_lab_usage_approval_config.sql`：新增实验室审批流程、审批记录表兼容字段、权限点、角色权限分配。

`sql/18_lab_usage_approval_config.sql` 应包含：

- `approval_record` 增加 `business_type`、`business_no`。
- `approval_flow_config` 插入 `LAB_USAGE_APPLY`，`business_type = 3`。
- `sys_permission` 插入 `module:lab`、`lab-room:*`、`lab-usage:*`。
- `sys_role_permission` 给管理员全量权限。
- 给教师分配 `lab-room:list`、`lab-usage:list`、`lab-usage:create`、`lab-usage:cancel`、`lab-usage:schedule:view`。
- 给实验室管理人员、中心主任、分管院长、院长分配 `lab-usage:approve` 和对应查看权限。

权限脚本要用 `INSERT ... SELECT ... WHERE NOT EXISTS` 风格，方便重复执行。

## 10. 需要修改的主要文件清单

后端：

- `lab-service-approval/src/main/java/com/lab/approval/dto/ApprovalContext.java`
- `lab-service-approval/src/main/java/com/lab/approval/dto/ApprovalRequest.java`
- `lab-service-approval/src/main/java/com/lab/approval/entity/ApprovalRecord.java`
- `lab-service-approval/src/main/java/com/lab/approval/service/ApprovalWorkflowService.java`
- `lab-service-approval/src/main/java/com/lab/approval/service/impl/ApprovalWorkflowServiceImpl.java`
- `lab-service-approval/src/main/java/com/lab/approval/service/ApproverAssignmentService.java`
- `lab-service-approval/src/main/java/com/lab/approval/service/impl/ApproverAssignmentServiceImpl.java`
- 新增实验室相关 controller、entity、dto、mapper、service。

前端：

- `lab-web-admin/src/constants/permissions.ts`
- `lab-web-admin/src/api/lab.ts`
- `lab-web-admin/src/types/lab.ts`
- `lab-web-admin/src/composables/useMenu.ts`
- `lab-web-admin/src/router/index.ts`
- 新增 `lab-web-admin/src/views/lab/LabRoomList.vue`
- 新增 `lab-web-admin/src/views/lab/LabUsageApplicationList.vue`
- 新增 `lab-web-admin/src/views/lab/LabUsageSchedule.vue`
- 可选改造 `lab-web-admin/src/views/approval/ApprovalTodo.vue`，支持药品和实验室统一待审批。

SQL：

- `sql/17_lab_usage_tables.sql`
- `sql/18_lab_usage_approval_config.sql`
- 如需要同步完整导出，再更新 `lab_management.sql`。

测试：

- `lab-service-approval/src/test/java/com/lab/approval/service/LabUsageApplicationServiceTest.java`
- `lab-service-approval/src/test/java/com/lab/approval/service/LabUsageApprovalFlowTest.java`
- `lab-service-approval/src/test/java/com/lab/approval/service/ApproverAssignmentServiceTest.java`
- 前端至少执行类型检查和构建。

## 11. 分阶段实施计划

### 第一阶段：数据库和通用审批扩展

- 新增实验室相关表。
- 新增实验室使用审批流程配置。
- 给 `approval_record` 增加 `business_type`。
- 扩展 `ApprovalContext`。
- 调整审批历史查询按 `business_type + application_id`。
- 扩展审批人分配，支持 `LAB_ROOM_MANAGER`、`CENTER_DIRECTOR`、`DEPUTY_DEAN`、`DEAN`。

验收：

- 能通过测试证明药品申请旧流程不受影响。
- 能根据实验室ID找到第一级实验室管理人员。

### 第二阶段：实验室基础信息和管理人员配置

- 实现实验室 CRUD。
- 实现实验室管理人员维护。
- 前端完成实验室列表、详情、管理人员配置入口。

验收：

- 管理员能维护实验室及管理人员。
- 非管理员看不到编辑和管理人员配置按钮。

### 第三阶段：实验室使用申请

- 实现创建申请、列表、详情、取消。
- 创建申请时进入统一审批流程。
- 教师前端能选择实验室、时间段、共同教师并提交。
- 提交时返回同时间段已有使用提示，但不拦截。

验收：

- 教师能提交申请。
- 同一实验室同一时间段能提交多条申请。
- 没有管理人员的实验室不能提交申请，并给出清晰错误。

### 第四阶段：四级审批闭环

- 实现实验室申请审批接口。
- 支持实验室管理人员、中心主任、分管院长、院长逐级审批。
- 审批通过后申请出现在使用安排中。
- 统一待办页面支持业务类型筛选。

验收：

- 四级审批顺序正确。
- 非当前层级审批人不能审批。
- 任一级拒绝后流程终止。
- 最终通过后使用安排可见。

### 第五阶段：使用安排和权限体验完善

- 实现使用安排列表或日历视图。
- 按角色调整默认筛选和可见按钮。
- 完善审批历史展示和教师端脱敏。
- 增加统计、导出、通知等增强能力。

验收：

- 教师、实验室管理人员、中心主任、分管院长、院长、管理员看到的菜单和数据范围符合预期。
- 同一实验室同一时段多个老师共享使用时，前端展示清晰。

## 12. 测试用例清单

后端必测：

- 教师创建实验室使用申请成功。
- 禁用实验室不能申请。
- 开始时间晚于结束时间时创建失败。
- 未配置管理人员的实验室创建申请失败。
- 同一实验室同一时段多名教师可分别申请成功。
- 实验室管理人员只能审批自己负责实验室的申请。
- 中心主任、分管院长、院长按顺序审批。
- 非当前审批人审批失败。
- 任一级拒绝后申请状态为审批拒绝。
- 院长通过后申请状态为审批通过。
- 审批历史按 `business_type + application_id` 隔离，不串到药品申请。
- 药品申请原有审批流程仍可正常执行。

前端必测：

- 管理员看到实验室管理人员配置入口。
- 教师看不到实验室编辑和管理人员配置按钮。
- 教师可提交实验室使用申请。
- 审批人只在待自己审批的申请上看到审批按钮。
- 使用安排能展示同一实验室同一时段多个教师。
- 路由守卫能拦截无权限页面。

## 13. 关键风险与处理

### 13.1 审批记录串单风险

如果审批记录仍只按 `application_id` 查询，实验室申请 ID 与药品申请 ID 重复时会串审批历史。必须增加并使用 `business_type`。

### 13.2 角色码不统一风险

当前脚本里存在英文角色码和数字角色码。实现时应集中做角色映射，避免业务代码到处判断 `003`、`CENTER_ADMIN`。

### 13.3 多审批候选人风险

一个实验室多个管理人员时，如果只保存一个 `current_approver_id`，其他管理人员可能明明负责该实验室却看不到审批按钮。第一期可先选主负责人，最终建议支持候选审批人列表校验。

### 13.4 共享使用被误判冲突

实验室使用不是独占预约，不能照搬预约系统的时间冲突拒绝逻辑。可以提示重叠，但不能阻止提交或审批通过。

### 13.5 前端隐藏不等于权限控制

菜单和按钮隐藏只是体验，后端接口仍必须按角色、权限、实验室管理关系和申请归属做校验。

## 14. 待确认细节

这些问题不阻塞第一期设计，但实现前最好明确：

1. 实验室管理人员审批是“任一管理人员通过即可”，还是必须主负责人审批。
2. 中心主任、分管院长、院长是否全院唯一，还是将来可能按学院、部门、实验中心拆分。
3. 教师是否可以代表其他教师提交申请，还是只能以自己为申请人、其他教师作为共同使用人。
4. 已审批通过的申请是否允许撤销或变更时间；如果允许，是否需要重新走审批。
5. 使用安排是否需要记录实际签到、签退；本设计先只覆盖“审批通过后的计划使用”。

## 15. 推荐第一步实现范围

后续正式开发建议从“第一阶段：数据库和通用审批扩展”开始，先把审批系统抽象好。只要这一步打稳，后面的实验室 CRUD、申请页面、审批页面都能顺着现有药品申请模式往前推，代码量会更可控。
