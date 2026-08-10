import { request } from './request'
import type { PageResult, AuditLog } from '@/types/api'

export interface AuditQuery {
  page?: number
  size?: number
  operator?: string
  module?: string
  operation?: string
  startTime?: string
  endTime?: string
}

export const auditApi = {
  page: (params: AuditQuery) =>
    request.get<PageResult<AuditLog>>('/audit/logs', params)
}
