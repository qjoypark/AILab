import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo, MenuItem } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<UserInfo | null>(null)
  const permissions = ref<string[]>([])
  const menuList = ref<MenuItem[]>([])

  const setToken = (newToken: string, newRefreshToken?: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
    if (newRefreshToken) {
      refreshToken.value = newRefreshToken
      localStorage.setItem('refreshToken', newRefreshToken)
    }
  }

  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info
  }

  const setPermissions = (perms: string[]) => {
    permissions.value = perms
  }

  const setMenuList = (menus: MenuItem[]) => {
    menuList.value = menus
  }

  const logout = () => {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    permissions.value = []
    menuList.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  const hasPermission = (permission: string) => {
    return permissions.value.includes(permission)
  }

  const hasAnyPermission = (perms: string[]) => {
    return perms.some(perm => permissions.value.includes(perm))
  }

  return {
    token,
    refreshToken,
    userInfo,
    permissions,
    menuList,
    setToken,
    setUserInfo,
    setPermissions,
    setMenuList,
    logout,
    hasPermission,
    hasAnyPermission
  }
})
