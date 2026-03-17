import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

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
  
  // 需要认证但没有token
  if (requiresAuth && !userStore.token) {
    next('/login')
    return
  }
  
  // 已登录访问登录页，重定向到首页
  if (to.path === '/login' && userStore.token) {
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
