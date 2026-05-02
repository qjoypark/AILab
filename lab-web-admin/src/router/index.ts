import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  ALERT_PERMISSIONS,
  APPROVAL_PERMISSIONS,
  APPROVAL_TODO_PERMISSIONS,
  HAZARDOUS_PERMISSIONS,
  INVENTORY_STOCK_CHECK_PERMISSIONS,
  INVENTORY_STOCK_IN_PERMISSIONS,
  INVENTORY_STOCK_OUT_PERMISSIONS,
  INVENTORY_STOCK_PERMISSIONS,
  LAB_ROOM_PERMISSIONS,
  LAB_USAGE_PERMISSIONS,
  MATERIAL_PERMISSIONS
} from '@/constants/permissions'

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
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人信息' }
      },
      {
        path: 'system/users',
        name: 'UserManagement',
        component: () => import('@/views/system/UserManagement.vue'),
        meta: { title: '用户管理', roles: ['ADMIN', 'CENTER_ADMIN'] }
      },
      {
        path: 'system/roles',
        name: 'RoleManagement',
        component: () => import('@/views/system/RoleManagement.vue'),
        meta: { title: '角色管理', roles: ['ADMIN'] }
      },
      {
        path: 'system/warehouses',
        name: 'WarehouseManagement',
        component: () => import('@/views/inventory/WarehouseManagement.vue'),
        meta: { title: '仓库管理', roles: ['ADMIN'] }
      },
      {
        path: 'inventory/warehouses',
        redirect: '/system/warehouses',
        meta: { requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'materials',
        name: 'MaterialList',
        component: () => import('@/views/material/MaterialList.vue'),
        meta: { title: '药品列表', permissions: MATERIAL_PERMISSIONS }
      },
      {
        path: 'instruments',
        name: 'InstrumentList',
        component: () => import('@/views/material/InstrumentList.vue'),
        meta: { title: '仪器列表', permissions: MATERIAL_PERMISSIONS }
      },
      {
        path: 'inventory/stock',
        name: 'StockList',
        component: () => import('@/views/inventory/StockList.vue'),
        meta: { title: '库存查询', permissions: INVENTORY_STOCK_PERMISSIONS }
      },
      {
        path: 'inventory/stock-in',
        name: 'StockInManagement',
        component: () => import('@/views/inventory/StockInManagement.vue'),
        meta: { title: '入库管理', permissions: INVENTORY_STOCK_IN_PERMISSIONS }
      },
      {
        path: 'applications',
        name: 'ApplicationList',
        component: () => import('@/views/approval/ApplicationList.vue'),
        meta: { title: '领用申请', permissions: APPROVAL_PERMISSIONS }
      },
      {
        path: 'labs/rooms',
        name: 'LabRoomList',
        component: () => import('@/views/lab/LabRoomList.vue'),
        meta: { title: '实验室列表', permissions: LAB_ROOM_PERMISSIONS }
      },
      {
        path: 'labs/usage-applications',
        name: 'LabUsageApplicationList',
        component: () => import('@/views/lab/LabUsageApplicationList.vue'),
        meta: { title: '实验室使用申请', permissions: LAB_USAGE_PERMISSIONS }
      },
      {
        path: 'labs/usage-schedule',
        name: 'LabUsageSchedule',
        component: () => import('@/views/lab/LabUsageSchedule.vue'),
        meta: { title: '实验室使用日程', permissions: ['lab-usage:schedule:view'] }
      },
      {
        path: 'approval/todo',
        name: 'ApprovalTodo',
        component: () => import('@/views/approval/ApprovalTodo.vue'),
        meta: { title: '待审批事项', permissions: APPROVAL_TODO_PERMISSIONS }
      },
      {
        path: 'inventory/stock-out',
        name: 'StockOutManagement',
        component: () => import('@/views/inventory/StockOutManagement.vue'),
        meta: { title: '出库管理', permissions: INVENTORY_STOCK_OUT_PERMISSIONS }
      },
      {
        path: 'inventory/stock-check',
        name: 'StockCheckManagement',
        component: () => import('@/views/inventory/StockCheckManagement.vue'),
        meta: { title: '库存盘点', permissions: INVENTORY_STOCK_CHECK_PERMISSIONS }
      },
      {
        path: 'usage-records',
        name: 'MedicationUsageRecordList',
        component: () => import('@/views/hazardous/UsageRecordList.vue'),
        meta: { title: '药品使用' }
      },
      {
        path: 'hazardous/usage-records',
        redirect: '/usage-records',
        meta: { requiresAuth: true }
      },
      {
        path: 'hazardous/ledger',
        name: 'HazardousLedger',
        component: () => import('@/views/hazardous/HazardousLedger.vue'),
        meta: { title: '危化品台账', permissions: HAZARDOUS_PERMISSIONS }
      },
      {
        path: 'alerts',
        name: 'AlertList',
        component: () => import('@/views/alert/AlertList.vue'),
        meta: { title: '预警管理', permissions: ALERT_PERMISSIONS }
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

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth !== false
  const accessToken = normalizeToken(userStore.token)

  if (to.path === from.path) {
    next()
    return
  }

  if (requiresAuth && !accessToken) {
    next('/login')
    return
  }

  if (to.path === '/login' && accessToken) {
    next('/')
    return
  }

  if (requiresAuth && to.meta.roles) {
    const roles = to.meta.roles as string[]
    if (!userStore.hasRole(roles)) {
      ElMessage.error('没有权限访问该页面')
      next(from.path || '/')
      return
    }
  }

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
