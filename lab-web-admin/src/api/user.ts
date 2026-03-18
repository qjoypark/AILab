import request from '@/utils/request'
import type { User, Role, Permission, UserQuery, UserForm, RoleForm } from '@/types/user'

interface PageResult<T> {
  records?: T[]
  list?: T[]
  total?: number
}

const toListResult = <T>(result: PageResult<T>) => ({
  list: result?.records ?? result?.list ?? [],
  total: result?.total ?? 0
})

export const userApi = {
  getUserList(params: UserQuery) {
    return request.get<any, PageResult<User>>('/system/users', { params }).then(toListResult<User>)
  },

  getUserById(id: number) {
    return request.get<any, User>(`/system/users/${id}`)
  },

  createUser(data: UserForm) {
    return request.post<any, User>('/system/users', data)
  },

  updateUser(id: number, data: UserForm) {
    return request.put<any, User>(`/system/users/${id}`, data)
  },

  deleteUser(id: number) {
    return request.delete(`/system/users/${id}`)
  },

  assignRoles(userId: number, roleIds: number[]) {
    return request.post(`/system/users/${userId}/roles`, roleIds)
  },

  getUserRoles(userId: number) {
    return request.get<any, number[]>(`/system/users/${userId}/roles`)
  },

  getRoleList(params?: { keyword?: string }) {
    return request.get<any, PageResult<Role> | Role[]>('/system/roles', { params }).then((result) => {
      if (Array.isArray(result)) {
        return {
          list: result,
          total: result.length
        }
      }
      return toListResult<Role>(result)
    })
  },

  getRoleById(id: number) {
    return request.get<any, Role>(`/system/roles/${id}`)
  },

  createRole(data: RoleForm) {
    return request.post<any, Role>('/system/roles', data)
  },

  updateRole(id: number, data: RoleForm) {
    return request.put<any, Role>(`/system/roles/${id}`, data)
  },

  deleteRole(id: number) {
    return request.delete(`/system/roles/${id}`)
  },

  assignPermissions(roleId: number, permissionIds: number[]) {
    return request.post(`/system/roles/${roleId}/permissions`, permissionIds)
  },

  getRolePermissions(roleId: number) {
    return request.get<any, number[]>(`/system/roles/${roleId}/permissions`)
  },

  getPermissionTree() {
    return request.get<any, Permission[]>('/system/roles/permissions')
  }
}
