import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 权限指令
 * 用法：v-permission="['permission1', 'permission2']"
 * 或：v-permission="'permission1'"
 */
export const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const userStore = useUserStore()

    if (value) {
      const permissions = Array.isArray(value) ? value : [value]
      const hasPermission = permissions.some(permission => 
        userStore.hasPermission(permission)
      )

      if (!hasPermission) {
        // 移除元素
        el.parentNode?.removeChild(el)
      }
    } else {
      throw new Error('需要指定权限，如 v-permission="[\'permission1\']"')
    }
  }
}

/**
 * 角色指令
 * 用法：v-role="['admin', 'manager']"
 * 或：v-role="'admin'"
 */
export const role: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const userStore = useUserStore()

    if (value && userStore.userInfo) {
      const roles = Array.isArray(value) ? value : [value]
      const hasRole = roles.some(role => 
        userStore.userInfo?.roles.includes(role)
      )

      if (!hasRole) {
        // 移除元素
        el.parentNode?.removeChild(el)
      }
    } else {
      throw new Error('需要指定角色，如 v-role="[\'admin\']"')
    }
  }
}
