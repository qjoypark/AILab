import request from '@/utils/request'
import type { LoginRequest, LoginResponse } from '@/types/user'

export const authApi = {
  login(data: LoginRequest) {
    return request.post<any, LoginResponse>('/auth/login', data)
  },

  logout() {
    return request.post('/auth/logout')
  },

  refreshToken(refreshToken: string) {
    return request.post('/auth/refresh', { refreshToken })
  },

  getCurrentUser() {
    return request.get('/auth/current-user')
  }
}
