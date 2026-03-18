import request from '@/utils/request'
import type { LoginRequest, LoginResponse, UpdateProfileRequest, UserInfo } from '@/types/user'

export const authApi = {
  login(data: LoginRequest) {
    return request.post<any, LoginResponse>('/auth/login', data)
  },

  logout() {
    return request.post('/auth/logout')
  },

  refreshToken(refreshToken: string) {
    return request.post('/auth/refresh', null, {
      headers: {
        Authorization: `Bearer ${refreshToken}`
      }
    })
  },

  getCurrentUser() {
    return request.get<any, UserInfo>('/auth/current-user')
  },

  updateProfile(data: UpdateProfileRequest) {
    return request.put<any, UserInfo>('/auth/profile', data)
  }
}
