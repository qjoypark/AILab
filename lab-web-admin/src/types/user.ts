export interface UserInfo {
  id: number
  username: string
  realName: string
  email?: string
  phone?: string
  departmentId?: number
  departmentName?: string
  roles: string[]
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  userInfo: UserInfo
  permissions: string[]
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
  }
}
