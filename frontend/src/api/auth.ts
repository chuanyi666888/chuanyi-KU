import { request } from './request'
import type { LoginResponse, UserInfo } from '@/types/api'

export interface LoginParams {
  username: string
  password: string
  captcha?: string
}

export interface ChangePasswordParams {
  oldPassword: string
  newPassword: string
}

export const authApi = {
  login: (data: LoginParams) =>
    request.post<LoginResponse>('/auth/login', data),

  logout: () =>
    request.post<void>('/auth/logout'),

  refresh: (refreshToken: string) =>
    request.post<LoginResponse>('/auth/refresh', null, { params: { refreshToken } }),

  userInfo: () =>
    request.get<UserInfo>('/auth/userinfo'),

  changePassword: (data: ChangePasswordParams) =>
    request.post<void>('/auth/password', data)
}
