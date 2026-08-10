import { request } from './request'
import type { PageResult, UserVO, RoleVO, PermissionVO } from '@/types/api'

export interface UserQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
}

export interface UserCreatePayload {
  username: string
  password: string
  nickname: string
  email?: string
  phone?: string
  roleIds?: number[]
  remark?: string
}

export interface UserUpdatePayload {
  id: number
  nickname: string
  email?: string
  phone?: string
  roleIds?: number[]
  status?: number
  remark?: string
}

export const userApi = {
  page: (params: UserQuery) =>
    request.get<PageResult<UserVO>>('/users', params),

  detail: (id: number) =>
    request.get<UserVO>(`/users/${id}`),

  create: (data: UserCreatePayload) =>
    request.post<number>('/users', data),

  update: (data: UserUpdatePayload) =>
    request.put<void>('/users', data),

  remove: (id: number) =>
    request.delete<void>(`/users/${id}`),

  resetPassword: (userId: number, newPassword: string) =>
    request.post<void>('/users/reset-password', { userId, newPassword })
}

export const roleApi = {
  page: (params: { page?: number; size?: number; keyword?: string }) =>
    request.get<PageResult<RoleVO>>('/roles', params),

  all: () =>
    request.get<RoleVO[]>('/roles/all'),

  detail: (id: number) =>
    request.get<RoleVO>(`/roles/${id}`),

  create: (data: { roleCode: string; roleName: string; description?: string }) =>
    request.post<number>('/roles', data),

  update: (data: { id: number; roleName: string; description?: string; status?: number }) =>
    request.put<void>('/roles', data),

  remove: (id: number) =>
    request.delete<void>(`/roles/${id}`),

  assignPermissions: (roleId: number, permissionIds: number[]) =>
    request.post<void>('/roles/permissions', { roleId, permissionIds }),

  permissionIds: (id: number) =>
    request.get<number[]>(`/roles/${id}/permissions`)
}

export const permissionApi = {
  tree: () =>
    request.get<PermissionVO[]>('/permissions/tree'),

  menuTree: () =>
    request.get<PermissionVO[]>('/permissions/menu-tree')
}
