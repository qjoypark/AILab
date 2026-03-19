<template>
  <el-container class="layout-container">
    <el-aside :width="sidebarWidth" class="sidebar">
      <div class="logo">
        <el-icon class="logo-icon"><Operation /></el-icon>
        <div v-show="!isCollapse" class="logo-text">
          <h2>智慧实验室</h2>
          <p>管理系统</p>
        </div>
      </div>

      <el-scrollbar class="menu-scrollbar">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          unique-opened
          router
          class="side-menu"
        >
          <template v-for="menu in accessibleMenus" :key="menu.id">
            <el-menu-item v-if="!menu.children || menu.children.length === 0" :index="menu.path">
              <el-icon v-if="menu.icon">
                <component :is="menu.icon" />
              </el-icon>
              <template #title>{{ menu.title }}</template>
            </el-menu-item>

            <el-sub-menu v-else :index="menu.path">
              <template #title>
                <el-icon v-if="menu.icon">
                  <component :is="menu.icon" />
                </el-icon>
                <span>{{ menu.title }}</span>
              </template>
              <el-menu-item v-for="child in menu.children" :key="child.id" :index="child.path">
                {{ child.title }}
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="layout-main">
      <el-header class="header">
        <div class="header-left">
          <el-button circle plain class="collapse-btn" @click="toggleCollapse">
            <el-icon>
              <component :is="isCollapse ? 'Expand' : 'Fold'" />
            </el-icon>
          </el-button>

          <el-breadcrumb separator="/">
            <el-breadcrumb-item to="/dashboard">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRouteTitle">{{ currentRouteTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tooltip content="消息中心" placement="bottom">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0">
              <el-button circle class="icon-btn" @click="goToNotifications">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
          </el-tooltip>

          <el-dropdown @command="handleCommand">
            <div class="user-entry">
              <el-avatar :size="34" class="user-avatar">
                {{ userInitial }}
              </el-avatar>
              <div class="user-name">{{ userStore.userInfo?.realName || '用户' }}</div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <div class="page-shell">
          <router-view v-slot="{ Component }">
            <transition name="fade-slide" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMenu } from '@/composables/useMenu'
import { authApi } from '@/api/auth'
import { notificationApi } from '@/api/notification'

const COLLAPSE_KEY = 'layout.sidebar.collapsed'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { accessibleMenus } = useMenu()

const unreadCount = ref(0)
const isCollapse = ref(localStorage.getItem(COLLAPSE_KEY) === '1')

watch(isCollapse, value => {
  localStorage.setItem(COLLAPSE_KEY, value ? '1' : '0')
})

const sidebarWidth = computed(() => (isCollapse.value ? '76px' : '236px'))
const activeMenu = computed(() => route.path)
const currentRouteTitle = computed(() => (route.meta.title as string) || '')
const userInitial = computed(() => {
  const name = userStore.userInfo?.realName || userStore.userInfo?.username || '用户'
  return name.slice(0, 1).toUpperCase()
})

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const syncCurrentUserPermissions = async () => {
  if (!userStore.token) {
    return
  }

  try {
    const currentUser = await authApi.getCurrentUser()
    userStore.setUserInfo(currentUser)
    userStore.setPermissions(currentUser.permissions ?? [])
  } catch (error) {
    console.warn('同步当前用户权限失败:', error)
  }
}

const loadUnreadCount = async () => {
  const userId = userStore.userInfo?.id
  if (!userId) {
    unreadCount.value = 0
    return
  }

  try {
    unreadCount.value = await notificationApi.getUnreadCount(userId)
  } catch (error) {
    console.error('加载未读消息数量失败:', error)
  }
}

const goToNotifications = () => {
  router.push('/notifications')
}

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    try {
      await authApi.logout()
    } catch (error) {
      console.warn('退出登录接口调用失败，将继续本地退出', error)
    } finally {
      userStore.logout()
      router.push('/login')
    }
    return
  }

  if (command === 'profile') {
    router.push('/profile')
  }
}

onMounted(async () => {
  await syncCurrentUserPermissions()
  await loadUnreadCount()
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  border-right: 1px solid #1f2937;
  background: #0b1220;
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.04);
  transition: width 0.2s ease;
}

.logo {
  height: 64px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
}

.logo-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #38bdf8, #3b82f6);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.logo-text h2 {
  font-size: 18px;
  line-height: 1.2;
  margin: 0;
  color: #ffffff;
  font-weight: 700;
  letter-spacing: 0.3px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

.logo-text p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #dbeafe;
  font-weight: 500;
  opacity: 1;
}

.menu-scrollbar {
  height: calc(100vh - 64px);
}

.side-menu {
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #f3f4f6;
  --el-menu-hover-bg-color: #1f2937;
  --el-menu-active-color: #ffffff;
}

.side-menu:deep(.el-menu) {
  border-right: none;
  background: transparent;
}

.side-menu:deep(.el-menu-item),
.side-menu:deep(.el-sub-menu__title) {
  margin: 6px 10px;
  border-radius: 10px;
  background: transparent;
  color: #f3f4f6 !important;
  font-weight: 600;
}

.side-menu:deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  background: #111827;
  color: #ffffff !important;
}

.side-menu:deep(.el-sub-menu .el-menu) {
  margin: 0 10px 8px;
  padding: 4px 0;
  border-radius: 10px;
  background: #111827;
}

.side-menu:deep(.el-sub-menu .el-menu-item) {
  margin: 4px 8px;
  min-height: 36px;
  line-height: 36px;
  border-radius: 8px;
  color: #f3f4f6 !important;
  background: transparent;
}

.side-menu:deep(.el-menu-item:hover),
.side-menu:deep(.el-sub-menu__title:hover),
.side-menu:deep(.el-sub-menu .el-menu-item:hover) {
  background: #1f2937 !important;
  color: #fff !important;
}

.side-menu:deep(.el-menu-item.is-active),
.side-menu:deep(.el-sub-menu .el-menu-item.is-active) {
  background: #2563eb !important;
  color: #fff !important;
}

.side-menu:deep(.el-menu--popup) {
  background: #0f172a !important;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 10px;
  padding: 4px;
}

.side-menu:deep(.el-menu--popup .el-menu-item) {
  border-radius: 8px;
  color: #e2e8f0 !important;
}

.side-menu:deep(.el-menu--popup .el-menu-item:hover) {
  background: rgba(59, 130, 246, 0.25) !important;
  color: #fff !important;
}

.layout-main {
  background: transparent;
}

.header {
  height: 64px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid #e6edf9;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn,
.icon-btn {
  border: none;
  background: #eef4ff;
  color: #3b82f6;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  border: 1px solid #e6edf9;
  background: #fff;
  cursor: pointer;
  user-select: none;
}

.user-avatar {
  background: linear-gradient(135deg, #3b82f6, #0ea5e9);
  color: #fff;
  font-weight: 600;
}

.user-name {
  font-size: 13px;
  color: #334155;
}

.main-content {
  background: transparent;
  padding: 18px 20px;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
