/**
 * 系统管理API
 */
import request from '@/utils/request'
import type {
  SysUser,
  UserFormData,
  UserQueryParams,
  PageResponse,
  SysRole,
  RoleFormData,
  PermissionTreeNode
} from '@/types/system'

/**
 * 用户管理API
 */
export const userApi = {
  /**
   * 查询用户列表
   */
  listUsers(params: UserQueryParams) {
    return request.get<any, PageResponse<SysUser>>('/system/users', { params })
  },

  /**
   * 查询用户详情
   */
  getUserById(id: number) {
    return request.get<any, SysUser>(`/system/users/${id}`)
  },

  /**
   * 创建用户
   */
  createUser(data: UserFormData) {
    return request.post<any, number>('/system/users', data)
  },

  /**
   * 更新用户
   */
  updateUser(id: number, data: UserFormData) {
    return request.put(`/system/users/${id}`, data)
  },

  /**
   * 删除用户
   */
  deleteUser(id: number) {
    return request.delete(`/system/users/${id}`)
  },

  /**
   * 查询用户的角色
   */
  getUserRoles(id: number) {
    return request.get<any, number[]>(`/system/users/${id}/roles`)
  },

  /**
   * 分配用户角色
   */
  assignUserRoles(id: number, roleIds: number[]) {
    return request.post(`/system/users/${id}/roles`, roleIds)
  }
}

/**
 * 角色管理API
 */
export const roleApi = {
  /**
   * 查询角色列表
   */
  listRoles() {
    return request.get<any, SysRole[]>('/system/roles')
  },

  /**
   * 查询角色详情
   */
  getRoleById(id: number) {
    return request.get<any, SysRole>(`/system/roles/${id}`)
  },

  /**
   * 创建角色
   */
  createRole(data: RoleFormData) {
    return request.post<any, number>('/system/roles', data)
  },

  /**
   * 更新角色
   */
  updateRole(id: number, data: RoleFormData) {
    return request.put(`/system/roles/${id}`, data)
  },

  /**
   * 删除角色
   */
  deleteRole(id: number) {
    return request.delete(`/system/roles/${id}`)
  },

  /**
   * 查询权限树
   */
  getPermissionTree() {
    return request.get<any, PermissionTreeNode[]>('/system/roles/permissions')
  },

  /**
   * 查询角色的权限
   */
  getRolePermissions(id: number) {
    return request.get<any, number[]>(`/system/roles/${id}/permissions`)
  },

  /**
   * 分配角色权限
   */
  assignPermissions(id: number, permissionIds: number[]) {
    return request.post(`/system/roles/${id}/permissions`, permissionIds)
  }
}
