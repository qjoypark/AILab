# Web管理后台开发状态

## 已完成功能

### 1. 基础架构 ✅
- [x] Vue 3 + TypeScript + Vite 项目初始化
- [x] Element Plus UI组件库集成
- [x] Vue Router 4 路由配置
- [x] Pinia 状态管理
- [x] Axios HTTP客户端配置
- [x] 主布局组件 (MainLayout)

### 2. 用户认证与权限 ✅
- [x] 登录页面 (Login.vue)
- [x] JWT令牌存储和管理
- [x] 路由守卫（权限验证）
- [x] 用户状态管理 (stores/user.ts)
- [x] 认证API (api/auth.ts)

### 3. 用户权限管理 ✅
- [x] 用户管理页面 (system/UserManagement.vue)
  - 用户列表查询（支持关键词、类型、状态筛选）
  - 用户创建、编辑、删除
  - 用户状态切换
  - 分配角色功能
- [x] 角色管理页面 (system/RoleManagement.vue)
  - 角色列表查询
  - 角色创建、编辑、删除
  - 分配权限功能（树形结构）
- [x] 用户权限API (api/user.ts)
- [x] 类型定义 (types/user.ts)

### 4. 药品管理 ✅
- [x] 药品列表页面 (material/MaterialList.vue)
  - 药品列表查询（支持关键词、类型、管控类型筛选）
  - 药品创建、编辑、删除
  - 支持危化品特殊字段（CAS号、危险类别、管控类型）
  - 分类树选择
- [x] 药品管理API (api/material.ts)
- [x] 类型定义 (types/material.ts)

## 待开发功能

### 5. 库存管理 ✅
- [x] 库存查询页面 (inventory/StockList.vue)
  - 库存列表（支持多条件筛选）
  - 库存明细查看
  - 低库存预警提示（红色标记）
  - 有效期预警提示（橙色标记）
- [x] 入库管理页面 (inventory/StockInManagement.vue)
  - 创建入库单
  - 入库单列表
  - 入库单确认
  - 入库单详情查看
- [x] 库存管理API (api/inventory.ts)
- [x] 类型定义 (types/inventory.ts)
- [ ] 出库管理页面（待开发）
- [ ] 库存盘点页面（待开发）

### 6. 申请审批 ✅
- [x] 领用申请页面 (approval/ApplicationList.vue)
  - 创建申请（选择药品、填写数量和用途）
  - 我的申请列表
  - 申请详情（查看审批流程和历史）
  - 取消申请
- [x] 申请审批API (api/approval.ts)
- [x] 类型定义 (types/approval.ts)
- [ ] 审批处理页面（待开发）
- [ ] 待办事项页面（待开发）

### 7. 危化品管理 (优先级: 高)
- [ ] 危化品使用记录页面
  - 使用记录列表
  - 使用记录详情
- [ ] 危化品归还页面
  - 归还表单（实际使用量、归还量、废弃量）
- [ ] 危化品台账报表页面
  - 台账查询（按时间范围、药品筛选）
  - 账实差异显示
  - 导出Excel
- [ ] 危化品管理API
- [ ] 类型定义

### 8. 预警与通知 (优先级: 中)
- [ ] 预警列表页面
  - 按类型和级别筛选
  - 预警处理
- [ ] 消息中心页面
  - 消息列表
  - 未读消息提示
  - 标记已读
- [ ] 待办事项页面
  - 待审批申请
  - 待处理预警
- [ ] 通知API
- [ ] 类型定义

### 9. 报表统计 (优先级: 中)
- [ ] 库存汇总报表页面
  - 按分类统计
  - ECharts图表展示
  - 导出功能
- [ ] 消耗统计报表页面
  - 按时间范围统计
  - 柱状图、饼图展示
  - 导出功能
- [ ] 审计日志查询页面
  - 日志列表
  - 多条件筛选
  - 导出审计报告
- [ ] 报表API
- [ ] 类型定义

### 10. 其他功能 (优先级: 低)
- [ ] 仓库管理页面
- [ ] 供应商管理页面
- [ ] 药品分类管理页面
- [ ] 个人信息页面
- [ ] 系统设置页面
- [ ] Dashboard首页（数据概览）

## 技术栈

- **框架**: Vue 3.4 + TypeScript 5.3
- **构建工具**: Vite 5.1
- **UI组件库**: Element Plus 2.6
- **状态管理**: Pinia 2.1
- **路由**: Vue Router 4.3
- **HTTP客户端**: Axios 1.6
- **图表**: ECharts 5.5
- **测试**: Vitest 1.3 + @vue/test-utils 2.4

## 开发规范

### 目录结构
```
src/
├── api/          # API接口定义
├── assets/       # 静态资源
├── components/   # 公共组件
├── composables/  # 组合式函数
├── directives/   # 自定义指令
├── layouts/      # 布局组件
├── router/       # 路由配置
├── stores/       # Pinia状态管理
├── types/        # TypeScript类型定义
├── utils/        # 工具函数
└── views/        # 页面组件
    ├── system/   # 系统管理
    ├── material/ # 药品管理
    ├── inventory/# 库存管理
    ├── approval/ # 申请审批
    └── report/   # 报表统计
```

### 命名规范
- **组件**: PascalCase (UserManagement.vue)
- **文件夹**: kebab-case (material-list/)
- **API文件**: camelCase (materialApi)
- **类型定义**: PascalCase (Material, MaterialQuery)

### 代码规范
- 使用 Composition API (setup script)
- 使用 TypeScript 类型定义
- 使用 Element Plus 组件
- 统一错误处理
- 统一加载状态管理

## 下一步计划

1. **库存管理模块** (预计2-3天)
   - 创建库存相关API和类型定义
   - 实现库存查询页面
   - 实现入库/出库管理页面
   - 实现库存盘点页面

2. **申请审批模块** (预计2-3天)
   - 创建申请审批API和类型定义
   - 实现领用申请页面
   - 实现审批处理页面
   - 集成审批流程展示

3. **危化品管理模块** (预计1-2天)
   - 创建危化品管理API
   - 实现使用记录和归还页面
   - 实现危化品台账报表

4. **预警通知模块** (预计1天)
   - 实现预警列表和处理页面
   - 实现消息中心
   - 实现待办事项

5. **报表统计模块** (预计1-2天)
   - 集成ECharts
   - 实现各类报表页面
   - 实现导出功能

## 启动开发服务器

```bash
cd lab-web-admin
npm install
npm run dev
```

访问: http://localhost:5173

## 构建生产版本

```bash
npm run build
```

## 注意事项

1. 所有API调用需要携带JWT令牌
2. 权限控制通过路由meta.permissions配置
3. 表单验证使用Element Plus的FormRules
4. 列表页面统一使用分页
5. 删除操作需要二次确认
6. 错误信息统一使用ElMessage提示
