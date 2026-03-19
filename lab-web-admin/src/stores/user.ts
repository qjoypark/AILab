import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo, MenuItem } from '@/types/user'

const TOKEN_KEY = 'token'
const REFRESH_TOKEN_KEY = 'refreshToken'
const USER_INFO_KEY = 'userInfo'
const PERMISSIONS_KEY = 'permissions'
const MENU_LIST_KEY = 'menuList'

const safeParse = <T>(raw: string | null, fallback: T): T => {
  if (!raw || raw === 'undefined' || raw === 'null') {
    return fallback
  }
  try {
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

const normalizePermission = (permission: unknown): string => {
  return typeof permission === 'string' ? permission.trim() : ''
}

const normalizeTokenValue = (value: unknown): string => {
  if (typeof value !== 'string') {
    return ''
  }
  const normalized = value.trim()
  if (!normalized || normalized === 'undefined' || normalized === 'null') {
    return ''
  }
  return normalized
}

const normalizePermissions = (permissions: unknown): string[] => {
  if (!Array.isArray(permissions)) {
    return []
  }
  return Array.from(
    new Set(
      permissions
        .map(normalizePermission)
        .filter(Boolean)
    )
  )
}

const normalizeRoleCode = (role: unknown): string => {
  if (typeof role === 'string') {
    return role.trim().toUpperCase().replace(/^ROLE_/, '')
  }

  if (role && typeof role === 'object') {
    const roleObject = role as Record<string, unknown>
    const roleCandidates = [
      roleObject.roleCode,
      roleObject.code,
      roleObject.authority,
      roleObject.name
    ]

    for (const candidate of roleCandidates) {
      if (typeof candidate === 'string' && candidate.trim()) {
        return normalizeRoleCode(candidate)
      }
    }
  }

  return ''
}

const normalizeAdminAlias = (roleCode: string): string => {
  const roleAliasMap: Record<string, string> = {
    SUPER_ADMIN: 'ADMIN',
    SYSTEM_ADMIN: 'ADMIN',
    ROOT: 'ADMIN',
    管理员: 'ADMIN',
    系统管理员: 'ADMIN'
  }
  return roleAliasMap[roleCode] ?? roleCode
}

const normalizeRoles = (roles: unknown): string[] => {
  if (!Array.isArray(roles)) {
    return []
  }

  return Array.from(
    new Set(
      roles
        .map(normalizeRoleCode)
        .map(normalizeAdminAlias)
        .filter(Boolean)
    )
  )
}

const parseJwtPayload = (token: string): Record<string, unknown> | null => {
  if (!token) {
    return null
  }

  const payloadPart = token.split('.')[1]
  if (!payloadPart) {
    return null
  }

  try {
    const base64 = payloadPart.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
    const decoded = decodeURIComponent(
      atob(padded)
        .split('')
        .map(character => `%${(`00${character.charCodeAt(0).toString(16)}`).slice(-2)}`)
        .join('')
    )
    return JSON.parse(decoded) as Record<string, unknown>
  } catch {
    return null
  }
}

const normalizeUserInfo = (info: UserInfo | null): UserInfo | null => {
  if (!info) {
    return null
  }

  return {
    ...info,
    roles: normalizeRoles(info.roles),
    permissions: normalizePermissions(info.permissions)
  }
}

export const useUserStore = defineStore('user', () => {
  const storedToken = localStorage.getItem(TOKEN_KEY)
  const storedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
  const token = ref<string>(
    storedToken && storedToken !== 'undefined' && storedToken !== 'null' ? storedToken : ''
  )
  const refreshToken = ref<string>(
    storedRefreshToken && storedRefreshToken !== 'undefined' && storedRefreshToken !== 'null'
      ? storedRefreshToken
      : ''
  )
  const userInfo = ref<UserInfo | null>(
    normalizeUserInfo(safeParse<UserInfo | null>(localStorage.getItem(USER_INFO_KEY), null))
  )
  const permissions = ref<string[]>(
    normalizePermissions(safeParse<string[]>(localStorage.getItem(PERMISSIONS_KEY), []))
  )
  const menuList = ref<MenuItem[]>(
    safeParse<MenuItem[]>(localStorage.getItem(MENU_LIST_KEY), [])
  )

  const setToken = (newToken: string, newRefreshToken?: string) => {
    const normalizedToken = normalizeTokenValue(newToken)
    token.value = normalizedToken
    if (normalizedToken) {
      localStorage.setItem(TOKEN_KEY, normalizedToken)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
    if (newRefreshToken) {
      const normalizedRefreshToken = normalizeTokenValue(newRefreshToken)
      refreshToken.value = normalizedRefreshToken
      if (normalizedRefreshToken) {
        localStorage.setItem(REFRESH_TOKEN_KEY, normalizedRefreshToken)
      } else {
        localStorage.removeItem(REFRESH_TOKEN_KEY)
      }
    }
  }

  const setUserInfo = (info: UserInfo) => {
    const normalized = normalizeUserInfo(info)
    userInfo.value = normalized
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(normalized))
  }

  const setPermissions = (perms?: string[]) => {
    const normalized = normalizePermissions(perms ?? [])
    permissions.value = normalized
    localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(normalized))
  }

  const setMenuList = (menus?: MenuItem[]) => {
    const normalized = Array.isArray(menus) ? menus : []
    menuList.value = normalized
    localStorage.setItem(MENU_LIST_KEY, JSON.stringify(normalized))
  }

  const getCurrentRoles = () => {
    const stateRoles = normalizeRoles(userInfo.value?.roles ?? [])
    const tokenRoles = normalizeRoles(parseJwtPayload(token.value)?.roles ?? [])
    return Array.from(new Set([...stateRoles, ...tokenRoles]))
  }

  const logout = () => {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    permissions.value = []
    menuList.value = []
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
    localStorage.removeItem(PERMISSIONS_KEY)
    localStorage.removeItem(MENU_LIST_KEY)
  }

  const hasPermission = (permission: string) => {
    if (isAdmin()) {
      return true
    }
    const normalizedPermission = normalizePermission(permission)
    return permissions.value.includes(normalizedPermission)
  }

  const hasAnyPermission = (perms: string[]) => {
    if (!perms || perms.length === 0) {
      return true
    }
    if (isAdmin()) {
      return true
    }
    if (permissions.value.length === 0) {
      return false
    }
    return perms.some(perm => permissions.value.includes(normalizePermission(perm)))
  }

  const hasRole = (roles: string | string[]) => {
    if (isAdmin()) {
      return true
    }

    const requiredRoles = normalizeRoles(Array.isArray(roles) ? roles : [roles])
    if (requiredRoles.length === 0) {
      return true
    }

    const currentRoles = getCurrentRoles()
    return requiredRoles.some(role => currentRoles.includes(role))
  }

  const isAdmin = () => {
    if (getCurrentRoles().includes('ADMIN')) {
      return true
    }

    const username = (userInfo.value?.username ?? '').trim().toLowerCase()
    if (username === 'admin') {
      return true
    }

    const payload = parseJwtPayload(token.value)
    const tokenUsername = typeof payload?.sub === 'string' ? payload.sub.trim().toLowerCase() : ''
    return tokenUsername === 'admin'
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
    hasAnyPermission,
    hasRole,
    isAdmin
  }
})
