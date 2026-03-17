# 智慧实验室管理系统 - Web管理后台

## 项目概述

基于 Vue 3 + TypeScript + Element Plus 开发的实验室管理系统Web管理后台，提供完整的用户权限管理、药品管理、库存管理和申请审批功能。

## 技术栈

- **框架**: Vue 3.4 (Composition API)
- **语言**: TypeScript 5.3
- **构建工具**: Vite 5.1
- **UI组件库**: Element Plus 2.6
- **状态管理**: Pinia 2.1
- **路由**: Vue Router 4.3
- **HTTP客户端**: Axios 1.6
- **图表**: ECharts 5.5

## 功能模块

### 1. 用户权限管理 ✅
- **用户管理**: 用户CRUD、状态管理、角色分配
- **角色管理**: 角色CRUD、权限分配（树形结构）
- **权限控制**: 基于路由的权限验证

### 2. 药品管理 ✅
- **药品列表**: 查询、创建、编辑、删除
- **危化品管理**: CAS号、危险类别、管控类型
- **分类管理**: 树形分类结构
- **药品选择器**: 通用组件，可在多处复用

### 3. 库存管理 ✅
- **库存查询**: 多条件筛选、低库存预警、有效期预警
- **入库管理**: 创建入库单、确认入库、更新库存
- **出库管理**: 创建出库单、确认出库、扣减库存
- **库存盘点**: 创建盘点单、录入明细、自动计算差异、调整库存

### 4. 申请审批 ✅
- **领用申请**: 创建申请、查看申请、取消申请
- **审批处理**: 待审批列表、审批通过/拒绝、修改批准数量
- **审批流程**: 审批记录时间线展示

## 项目结构

```
lab-web-admin/
├── public/                 # 静态资源
├── src/
│   ├── api/               # API接口
│   │   ├── auth.ts        # 认证API
│   │   ├── user.ts        # 用户权限API
│   │   ├── material.ts    # 药品管理API
│   │   ├── inventory.ts   # 库存管理API
│   │   └── approval.ts    # 申请审批API
│   ├── assets/            # 资源文件
│   ├── components/        # 公共组件
│   │   └── MaterialSelector.vue  # 药品选择器
│   ├── composables/       # 组合式函数
│   ├── directives/        # 自定义指令
│   ├── layouts/           # 布局组件
│   │   └── MainLayout.vue # 主布局
│   ├── router/            # 路由配置
│   │   └── index.ts
│   ├── stores/            # 状态管理
│   │   └── user.ts        # 用户状态
│   ├── types/             # 类型定义
│   │   ├── user.ts
│   │   ├── material.ts
│   │   ├── inventory.ts
│   │   └── approval.ts
│   ├── utils/             # 工具函数
│   │   └── request.ts     # HTTP请求封装
│   ├── views/             # 页面组件
│   │   ├── system/        # 系统管理
│   │   │   ├── UserManagement.vue
│   │   │   └── RoleManagement.vue
│   │   ├── material/      # 药品管理
│   │   │   └── MaterialList.vue
│   │   ├── inventory/     # 库存管理
│   │   │   ├── StockList.vue
│   │   │   ├── StockInManagement.vue
│   │   │   ├── StockOutManagement.vue
│   │   │   └── StockCheckManagement.vue
│   │   ├── approval/      # 申请审批
│   │   │   ├── ApplicationList.vue
│   │   │   └── ApprovalTodo.vue
│   │   ├── Dashboard.vue  # 首页
│   │   └── Login.vue      # 登录页
│   ├── App.vue
│   ├── main.ts
│   └── style.css
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## 快速开始

### 环境要求
- Node.js >= 16
- npm >= 8

### 安装依赖
```bash
npm install
```

### 开发模式
```bash
npm run dev
```
访问: http://localhost:5173

### 生产构建
```bash
npm run build
```

### 类型检查
```bash
npm run type-check
```

## 路由列表

| 路径 | 名称 | 组件 | 权限 |
|------|------|------|------|
| `/login` | 登录 | Login.vue | - |
| `/` | 首页 | Dashboard.vue | - |
| `/system/users` | 用户管理 | UserManagement.vue | system:user:list |
| `/system/roles` | 角色管理 | RoleManagement.vue | system:role:list |
| `/materials` | 药品管理 | MaterialList.vue | material:list |
| `/inventory/stock` | 库存查询 | StockList.vue | inventory:stock:list |
| `/inventory/stock-in` | 入库管理 | StockInManagement.vue | inventory:stock-in:list |
| `/inventory/stock-out` | 出库管理 | StockOutManagement.vue | inventory:stock-out:list |
| `/inventory/stock-check` | 库存盘点 | StockCheckManagement.vue | inventory:stock-check:list |
| `/applications` | 领用申请 | ApplicationList.vue | application:list |
| `/approval/todo` | 待审批事项 | ApprovalTodo.vue | application:approve |

## API配置

在 `src/utils/request.ts` 中配置API基础URL：

```typescript
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
```

创建 `.env.development` 文件：
```
VITE_API_BASE_URL=http://localhost:8080
```

创建 `.env.production` 文件：
```
VITE_API_BASE_URL=https://api.yourdomain.com
```

## 核心功能说明

### 1. 药品选择组件
`MaterialSelector.vue` 是一个通用的药品选择组件，可在多个页面复用：
- 入库单添加药品
- 出库单添加药品
- 申请单添加药品
- 盘点单添加药品

使用示例：
```vue
<template>
  <MaterialSelector
    v-model="selectorVisible"
    @select="handleMaterialSelected"
  />
</template>

<script setup>
import MaterialSelector from '@/components/MaterialSelector.vue'

const selectorVisible = ref(false)

const handleMaterialSelected = (material) => {
  console.log('选中的药品:', material)
}
</script>
```

### 2. 权限控制
路由权限通过 `meta.permissions` 配置：
```typescript
{
  path: '/system/users',
  meta: { permissions: ['system:user:list'] }
}
```

在路由守卫中验证权限：
```typescript
router.beforeEach((to, from, next) => {
  if (to.meta.permissions) {
    const hasPermission = userStore.hasAnyPermission(to.meta.permissions)
    if (!hasPermission) {
      ElMessage.error('没有权限访问该页面')
      return next(false)
    }
  }
  next()
})
```

### 3. 状态管理
使用 Pinia 管理全局状态：
```typescript
// stores/user.ts
export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null,
    permissions: []
  }),
  actions: {
    setToken(token) {
      this.token = token
    },
    hasPermission(permission) {
      return this.permissions.includes(permission)
    }
  }
})
```

### 4. HTTP请求
统一的HTTP请求封装：
```typescript
// utils/request.ts
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

// 请求拦截器 - 添加token
request.interceptors.request.use(config => {
  const token = useUserStore().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 - 统一错误处理
request.interceptors.response.use(
  response => response.data,
  error => {
    ElMessage.error(error.message)
    return Promise.reject(error)
  }
)
```

## 开发规范

### 命名规范
- **组件**: PascalCase (UserManagement.vue)
- **文件夹**: kebab-case (material-list/)
- **变量/函数**: camelCase (loadUserList)
- **常量**: UPPER_SNAKE_CASE (API_BASE_URL)
- **类型**: PascalCase (User, Material)

### 代码规范
- 使用 Composition API (setup script)
- 使用 TypeScript 类型定义
- 统一使用 Element Plus 组件
- 统一错误处理
- 统一加载状态管理

### Git提交规范
```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 重构
test: 测试相关
chore: 构建/工具相关
```

## 待开发功能

### 高优先级
- [ ] 危化品使用记录页面
- [ ] 危化品归还页面
- [ ] 危化品台账报表

### 中优先级
- [ ] 预警列表页面
- [ ] 消息中心页面
- [ ] 库存汇总报表（ECharts）
- [ ] 消耗统计报表

### 低优先级
- [ ] Dashboard首页优化
- [ ] 个人信息页面
- [ ] 药品分类管理
- [ ] 供应商管理
- [ ] 审计日志查询

## 常见问题

### 1. 跨域问题
开发环境配置Vite代理：
```typescript
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### 2. 路由404
确保使用 `createWebHistory` 模式，并配置服务器重定向：
```typescript
const router = createRouter({
  history: createWebHistory(),
  routes
})
```

### 3. 权限验证失败
检查后端返回的权限列表格式是否正确：
```json
{
  "permissions": ["system:user:list", "material:list"]
}
```

## 性能优化

### 1. 路由懒加载
```typescript
{
  path: '/users',
  component: () => import('@/views/system/UserManagement.vue')
}
```

### 2. 组件按需引入
```typescript
import { ElButton, ElTable } from 'element-plus'
```

### 3. 图片懒加载
```vue
<el-image lazy :src="imageUrl" />
```

## 部署

### Nginx配置
```nginx
server {
  listen 80;
  server_name yourdomain.com;
  root /var/www/lab-web-admin/dist;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /api {
    proxy_pass http://localhost:8080;
  }
}
```

## 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

MIT License

## 联系方式

- 项目地址: [GitHub Repository]
- 问题反馈: [Issues]
- 文档: [Documentation]

---

**开发状态**: 核心功能已完成 ✅  
**最后更新**: 2026-03-17  
**版本**: 1.0.0
