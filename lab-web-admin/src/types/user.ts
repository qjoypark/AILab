export interface UserInfo {
  id: number
  username: string
  realName: string
  userType?: number
  department?: string
  email?: string
  phone?: string
  departmentId?: number
  departmentName?: string
  roles: string[]
  permissions?: string[]
}

export interface LoginRequest {
  username: string
  password: string
}

export interface UpdateProfileRequest {
  username: string
  currentPassword?: string
  newPassword?: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  userInfo: UserInfo
  permissions?: string[]
  menus?: MenuItem[]
}

export interface MenuItem {
  id: number
  path: string
  name: string
  component?: string
  icon?: string
  title: string
  hidden?: boolean
  children?: MenuItem[]
  meta?: {
    title: string
    icon?: string
    permissions?: string[]
    roles?: string[]
  }
}

export interface User {
  id: number
  username: string
  realName: string
  userType: number
  department?: string
  phone?: string
  email?: string
  status: number
  roles?: Role[]
  permissions?: string[]
  createdTime?: string
  updatedTime?: string
}

export interface Role {
  id: number
  roleName: string
  roleCode: string
  description?: string
  status: number
  permissions?: Permission[]
  createdTime?: string
}

export interface Permission {
  id: number
  permissionName: string
  permissionCode: string
  permissionType: number
  parentId?: number
  path?: string
  icon?: string
  sortOrder?: number
  children?: Permission[]
}

export interface UserQuery {
  keyword?: string
  userType?: number
  department?: string
  status?: number
  page?: number
  size?: number
}

export interface UserForm {
  id?: number
  username: string
  password?: string
  realName: string
  userType: number
  department?: string
  phone?: string
  email?: string
  status?: number
  roleIds?: number[]
}

export interface RoleForm {
  id?: number
  roleName: string
  roleCode: string
  description?: string
  status?: number
}
