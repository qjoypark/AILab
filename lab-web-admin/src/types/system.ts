/**
 * 系统管理相关类型定义
 */

// 用户类型枚举
export enum UserType {
  ADMIN = 1,
  TEACHER = 2,
  STUDENT = 3
}

// 用户状态枚举
export enum UserStatus {
  DISABLED = 0,
  ENABLED = 1
}

// 安全资质状态枚举
export enum SafetyCertStatus {
  NOT_CERTIFIED = 0,
  CERTIFIED = 1
}

// 角色状态枚举
export enum RoleStatus {
  DISABLED = 0,
  ENABLED = 1
}

// 权限类型枚举
export enum PermissionType {
  MENU = 1,
  BUTTON = 2,
  API = 3
}

/**
 * 系统用户
 */
export interface SysUser {
  id: number
  username: string
  realName: string
  phone?: string
  email?: string
  userType: UserType
  department?: string
  status: UserStatus
  safetyCertStatus: SafetyCertStatus
  safetyCertExpireDate?: string
  createdTime: string
  updatedTime: string
}

/**
 * 用户表单数据
 */
export interface UserFormData {
  id?: number
  username: string
  password?: string
  realName: string
  phone?: string
  email?: string
  userType: UserType
  department?: string
  status: UserStatus
  roleIds?: number[]
}

/**
 * 系统角色
 */
export interface SysRole {
  id: number
  roleCode: string
  roleName: string
  description?: string
  status: RoleStatus
  createdTime: string
  updatedTime: string
}

/**
 * 角色表单数据
 */
export interface RoleFormData {
  id?: number
  roleCode: string
  roleName: string
  description?: string
  status: RoleStatus
}

/**
 * 系统权限
 */
export interface SysPermission {
  id: number
  permissionCode: string
  permissionName: string
  permissionType: PermissionType
  parentId: number
  path?: string
  component?: string
  icon?: string
  sortOrder: number
  status: number
  children?: SysPermission[]
}

/**
 * 权限树节点
 */
export interface PermissionTreeNode {
  id: number
  label: string
  children?: PermissionTreeNode[]
}

/**
 * 用户查询参数
 */
export interface UserQueryParams {
  page: number
  size: number
  keyword?: string
  userType?: UserType
}

/**
 * 分页响应
 */
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
