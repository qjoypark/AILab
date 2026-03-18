<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <h3>实验室管理</h3>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <template v-for="menu in accessibleMenus" :key="menu.id">
          <!-- 无子菜单的菜单项 -->
          <el-menu-item v-if="!menu.children || menu.children.length === 0" :index="menu.path">
            <el-icon v-if="menu.icon">
              <component :is="menu.icon" />
            </el-icon>
            <span>{{ menu.title }}</span>
          </el-menu-item>
          
          <!-- 有子菜单的菜单项 -->
          <el-sub-menu v-else :index="menu.path">
            <template #title>
              <el-icon v-if="menu.icon">
                <component :is="menu.icon" />
              </el-icon>
              <span>{{ menu.title }}</span>
            </template>
            <el-menu-item
              v-for="child in menu.children"
              :key="child.id"
              :index="child.path"
            >
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute">{{ currentRoute }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
            <el-icon :size="20" @click="goToNotifications"><Bell /></el-icon>
          </el-badge>
          
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              <span>{{ userStore.userInfo?.realName || '用户' }}</span>
            </span>
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
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMenu } from '@/composables/useMenu'
import { authApi } from '@/api/auth'
import { notificationApi } from '@/api/notification'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { accessibleMenus } = useMenu()
const unreadCount = ref(0)

const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route.meta.title as string)

const syncCurrentUserPermissions = async () => {
  if (!userStore.token) {
    return
  }

  try {
    const currentUser = await authApi.getCurrentUser()
    userStore.setUserInfo(currentUser)
    userStore.setPermissions(currentUser.permissions ?? [])
  } catch (error) {
    console.warn('同步当前用户权限快照失败:', error)
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
      console.warn('退出登录接口调用失败，将执行本地登出:', error)
    } finally {
      userStore.logout()
      router.push('/login')
    }
  } else if (command === 'profile') {
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
  background-color: #304156;
  overflow-y: auto;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: bold;
  padding: 0 8px;
  text-align: center;
}

.logo h3 {
  margin: 0;
  width: 100%;
  font-size: 0;
  line-height: 1.3;
}

.logo h3::after {
  content: '呼伦贝尔学院农学院实验室管理系统';
  font-size: 14px;
  white-space: normal;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-badge {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
