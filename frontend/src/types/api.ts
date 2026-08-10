/** 统一 API 响应结构 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: string
}

/** 分页响应 */
export interface PageResult<T = unknown> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

/** 业务码常量 */
export const ResultCode = {
  SUCCESS: 0,
  PARAM_INVALID: 40001,
  UNAUTHORIZED: 40101,
  TOKEN_EXPIRED: 40102,
  TOKEN_INVALID: 40103,
  FORBIDDEN: 40301,
  NOT_FOUND: 40401,
  CONFLICT: 40901,
  RATE_LIMITED: 42901,
  LOGIN_LOCKED: 42902,
  INTERNAL_ERROR: 50000
} as const

/** 用户信息 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  roles: string[]
  permissions: string[]
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

export interface RoleVO {
  id: number
  roleCode: string
  roleName: string
  description?: string
  status: number
  builtin?: number
  createdAt?: string
  userCount?: number
}

export interface PermissionVO {
  id: number
  parentId: number
  permissionCode: string
  permissionName: string
  permissionType: number
  path?: string
  component?: string
  icon?: string
  sortOrder?: number
  status: number
  children?: PermissionVO[]
}

export interface UserVO {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  status: number
  remark?: string
  lastLoginAt?: string
  lastLoginIp?: string
  createdAt?: string
  roles: RoleVO[]
}

export interface AuditLog {
  id: number
  operatorId?: number
  operator?: string
  operation: string
  module: string
  target?: string
  method?: string
  requestUrl?: string
  requestIp?: string
  requestBody?: string
  result: number
  errorMsg?: string
  costMs?: number
  createdAt: string
}
