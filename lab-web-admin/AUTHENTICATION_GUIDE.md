# 登录与权限控制实现指南

## 概述

本文档说明了Web管理后台的登录功能和权限控制系统的实现，包括：
1. 登录页面UI（使用Element Plus）
2. JWT令牌的存储、刷新机制
3. 路由守卫实现权限验证
4. 根据用户权限动态加载菜单

## 功能特性

### 1. JWT令牌管理

#### 令牌存储
- **Access Token**: 存储在 localStorage 和 Pinia store 中，有效期2小时
- **Refresh Token**: 存储在 localStorage 和 Pinia store 中，有效期7天

#### 自动刷新机制
当API请求返回401错误时，系统会自动：
1. 使用 Refresh Token 请求新的 Access Token
2. 将等待中的请求加入队列
3. 刷新成功后，使用新token重试所有队列中的请求
4. 刷新失败则清空token并跳转到登录页

**实现位置**: `src/utils/request.ts`

```typescript
// 响应拦截器中的自动刷新逻辑
if (error.response?.status === 401 && !originalRequest._retry) {
  // 刷新token并重试请求
}
```

### 2. 路由守卫

#### 认证守卫
- 检查用户是否已登录（是否有token）
- 未登录用户访问需要认证的页面时，重定向到登录页
- 已登录用户访问登录页时，重定向到首页

#### 权限守卫
- 检查用户是否有访问特定路由的权限
- 权限通过路由meta中的permissions字段配置
- 用户需要拥有permissions数组中的任意一个权限即可访问

**实现位置**: `src/router/index.ts`

```typescript
router.beforeEach(async (to, from, next) => {
  // 认证检查
  if (requiresAuth && !userStore.token) {
    next('/login')
    return
  }
  
  // 权限检查
  if (requiresAuth && to.meta.permissions) {
    const permissions = to.meta.permissions as string[]
    if (!userStore.hasAnyPermission(permissions)) {
      ElMessage.error('没有权限访问该页面')
      next(from.path || '/')
      return
    }
  }
  
  next()
})
```

### 3. 动态菜单

#### 菜单配置
菜单配置支持两种方式：
1. **后端返回**: 登录接口返回用户可访问的菜单列表
2. **前端默认**: 使用前端配置的默认菜单，根据权限过滤

**实现位置**: `src/composables/useMenu.ts`

#### 菜单结构
```typescript
interface MenuItem {
  id: number
  path: string
  name: string
  title: string
  icon?: string
  children?: MenuItem[]
  meta?: {
    title: string
    icon?: string
    permissions?: string[]
  }
}
```

#### 权限过滤
- 菜单项通过meta.permissions字段配置所需权限
- 系统自动过滤用户无权访问的菜单项
- 如果父菜单的所有子菜单都被过滤，父菜单也会被隐藏

### 4. 按钮级权限控制

#### 自定义指令
提供了两个自定义指令用于按钮级权限控制：

**v-permission**: 基于权限控制
```vue
<el-button v-permission="['material:create']">新增药品</el-button>
<el-button v-permission="['material:edit', 'material:delete']">编辑</el-button>
```

**v-role**: 基于角色控制
```vue
<el-button v-role="'admin'">管理员功能</el-button>
<el-button v-role="['admin', 'manager']">高级功能</el-button>
```

**实现位置**: `src/directives/permission.ts`

## 使用示例

### 1. 登录流程

```typescript
// 在Login.vue中
const handleLogin = async () => {
  const res = await authApi.login(loginForm)
  
  // 保存token
  userStore.setToken(res.accessToken, res.refreshToken)
  
  // 保存用户信息
  userStore.setUserInfo(res.userInfo)
  
  // 保存权限列表
  userStore.setPermissions(res.permissions)
  
  // 保存菜单列表（可选）
  if (res.menus) {
    userStore.setMenuList(res.menus)
  }
  
  // 跳转到首页
  router.push('/')
}
```

### 2. 配置路由权限

```typescript
{
  path: '/system/users',
  name: 'SystemUsers',
  component: () => import('@/views/system/Users.vue'),
  meta: { 
    title: '用户管理',
    permissions: ['system:user:view'] // 需要的权限
  }
}
```

### 3. 在组件中检查权限

```typescript
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 检查单个权限
if (userStore.hasPermission('material:create')) {
  // 有权限
}

// 检查多个权限（任意一个即可）
if (userStore.hasAnyPermission(['material:edit', 'material:delete'])) {
  // 有权限
}
```

### 4. 在模板中使用权限指令

```vue
<template>
  <div>
    <!-- 只有拥有创建权限的用户才能看到此按钮 -->
    <el-button 
      v-permission="['material:create']" 
      type="primary"
      @click="handleCreate"
    >
      新增药品
    </el-button>
    
    <!-- 只有管理员角色才能看到此按钮 -->
    <el-button 
      v-role="'admin'" 
      type="danger"
      @click="handleDelete"
    >
      删除
    </el-button>
  </div>
</template>
```

## 权限编码规范

权限编码采用 `模块:资源:操作` 的格式：

- `system:user:view` - 查看用户
- `system:user:create` - 创建用户
- `system:user:edit` - 编辑用户
- `system:user:delete` - 删除用户
- `material:view` - 查看药品
- `material:create` - 创建药品
- `inventory:in:view` - 查看入库
- `application:approve` - 审批申请

## 安全注意事项

1. **前端权限控制仅用于UI展示**，真正的权限验证必须在后端进行
2. Token存储在localStorage中，注意XSS攻击防护
3. 敏感操作应该要求二次确认
4. 定期检查token有效性，及时清理过期token
5. 使用HTTPS传输，防止token被窃取

## API接口要求

### 登录接口
```
POST /api/v1/auth/login
Request:
{
  "username": "string",
  "password": "string"
}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "string",
    "refreshToken": "string",
    "expiresIn": 7200,
    "userInfo": {
      "id": 1,
      "username": "admin",
      "realName": "管理员",
      "roles": ["admin"]
    },
    "permissions": ["system:user:view", "material:view"],
    "menus": [] // 可选
  }
}
```

### 刷新Token接口
```
POST /api/v1/auth/refresh
Request:
{
  "refreshToken": "string"
}

Response:
{
  "code": 200,
  "data": {
    "accessToken": "string",
    "refreshToken": "string"
  }
}
```

### 登出接口
```
POST /api/v1/auth/logout
Headers:
  Authorization: Bearer {token}

Response:
{
  "code": 200,
  "message": "success"
}
```

## 测试建议

1. **登录测试**
   - 正确的用户名密码
   - 错误的用户名密码
   - 空用户名或密码

2. **Token刷新测试**
   - Token过期后自动刷新
   - Refresh Token过期后跳转登录
   - 多个并发请求时的刷新处理

3. **权限测试**
   - 有权限用户访问受保护页面
   - 无权限用户访问受保护页面
   - 动态菜单根据权限正确显示/隐藏

4. **登出测试**
   - 正常登出
   - 登出后清空所有状态
   - 登出后无法访问受保护页面

## 故障排查

### Token刷新失败
- 检查Refresh Token是否过期
- 检查刷新接口是否正常
- 查看浏览器控制台错误信息

### 菜单不显示
- 检查用户权限是否正确
- 检查菜单配置的permissions字段
- 查看useMenu composable的过滤逻辑

### 路由守卫不生效
- 检查路由meta配置
- 确认userStore中的permissions已正确设置
- 查看router.beforeEach的执行日志

## 后续优化建议

1. **Token加密**: 考虑对存储在localStorage中的token进行加密
2. **权限缓存**: 将权限信息缓存到sessionStorage，减少重复请求
3. **菜单缓存**: 将菜单配置缓存，避免每次刷新都重新计算
4. **权限预加载**: 在应用启动时预加载用户权限信息
5. **细粒度权限**: 支持更细粒度的权限控制，如字段级权限
