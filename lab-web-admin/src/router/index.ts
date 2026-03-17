import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const normalizeToken = (value: unknown): string => {
  if (typeof value === 'string') {
    const normalized = value.trim()
    if (normalized && normalized !== 'undefined' && normalized !== 'null') {
      return normalized
    }
  }

  if (
    value &&
    typeof value === 'object' &&
    'value' in value &&
    typeof (value as { value?: unknown }).value === 'string'
  ) {
    return normalizeToken((value as { value?: unknown }).value)
  }

  return ''
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'system/users',
        name: 'UserManagement',
        component: () => import('@/views/system/UserManagement.vue'),
        meta: { title: '用户管理', permissions: ['system:user:list'] }
      },
      {
        path: 'system/roles',
        name: 'RoleManagement',
        component: () => import('@/views/system/RoleManagement.vue'),
        meta: { title: '角色管理', permissions: ['system:role:list'] }
      },
      {
        path: 'materials',
        name: 'MaterialList',
        component: () => import('@/views/material/MaterialList.vue'),
        meta: { title: '药品管理', permissions: ['material:list'] }
      },
      {
        path: 'inventory/stock',
        name: 'StockList',
        component: () => import('@/views/inventory/StockList.vue'),
        meta: { title: '库存查询', permissions: ['inventory:stock:list'] }
      },
      {
        path: 'inventory/stock-in',
        name: 'StockInManagement',
        component: () => import('@/views/inventory/StockInManagement.vue'),
        meta: { title: '入库管理', permissions: ['inventory:stock-in:list'] }
      },
      {
        path: 'applications',
        name: 'ApplicationList',
        component: () => import('@/views/approval/ApplicationList.vue'),
        meta: { title: '领用申请', permissions: ['application:list'] }
      },
      {
        path: 'approval/todo',
        name: 'ApprovalTodo',
        component: () => import('@/views/approval/ApprovalTodo.vue'),
        meta: { title: '待审批事项', permissions: ['application:approve'] }
      },
      {
        path: 'inventory/stock-out',
        name: 'StockOutManagement',
        component: () => import('@/views/inventory/StockOutManagement.vue'),
        meta: { title: '出库管理', permissions: ['inventory:stock-out:list'] }
      },
      {
        path: 'inventory/stock-check',
        name: 'StockCheckManagement',
        component: () => import('@/views/inventory/StockCheckManagement.vue'),
        meta: { title: '库存盘点', permissions: ['inventory:stock-check:list'] }
      },
      {
        path: 'hazardous/usage-records',
        name: 'UsageRecordList',
        component: () => import('@/views/hazardous/UsageRecordList.vue'),
        meta: { title: '危化品使用记录', permissions: ['hazardous:usage:list'] }
      },
      {
        path: 'hazardous/ledger',
        name: 'HazardousLedger',
        component: () => import('@/views/hazardous/HazardousLedger.vue'),
        meta: { title: '危化品台账', permissions: ['hazardous:ledger:view'] }
      },
      {
        path: 'alerts',
        name: 'AlertList',
        component: () => import('@/views/alert/AlertList.vue'),
        meta: { title: '预警管理', permissions: ['alert:list'] }
      },
      {
        path: 'notifications',
        name: 'MessageCenter',
        component: () => import('@/views/notification/MessageCenter.vue'),
        meta: { title: '消息中心' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth !== false
  const accessToken = normalizeToken(userStore.token)
  
  // 需要认证但没有token
  if (requiresAuth && !accessToken) {
    next('/login')
    return
  }
  
  // 已登录访问登录页，重定向到首页
  if (to.path === '/login' && accessToken) {
    next('/')
    return
  }
  
  // 检查权限
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

export default router
