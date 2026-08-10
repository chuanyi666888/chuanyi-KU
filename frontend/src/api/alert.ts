import { request } from './request'
import type { PageResult } from '@/types/api'
import type {
  AlertRecordVO,
  AlertProcessLogVO,
  AlertProcessRequest,
  AlertStatsVO,
  AlertQuery
} from '@/types/alert'

/** 告警管理 API */
export const alertApi = {
  page: (params: AlertQuery) =>
    request.get<PageResult<AlertRecordVO>>('/alerts', params),

  detail: (id: number) =>
    request.get<AlertRecordVO>(`/alerts/${id}`),

  process: (data: AlertProcessRequest) =>
    request.post<number>('/alerts/process', data),

  remove: (id: number) =>
    request.delete<void>(`/alerts/${id}`),

  processLogs: (id: number) =>
    request.get<AlertProcessLogVO[]>(`/alerts/${id}/logs`),

  stats: () =>
    request.get<AlertStatsVO>('/alerts/stats')
}
