import { request } from './request'
import type { PageResult } from '@/types/api'
import type { SensorVO, ReadingVO, SensorQuery } from '@/types/iot'

/** 传感器管理 API */
export const sensorApi = {
  page: (params: SensorQuery) =>
    request.get<PageResult<SensorVO>>('/iot/sensors', params),

  listByWarehouse: (warehouseId: number) =>
    request.get<SensorVO[]>(`/iot/sensors/by-warehouse/${warehouseId}`),

  detail: (id: number) =>
    request.get<SensorVO>(`/iot/sensors/${id}`),

  readings: (id: number, limit = 60) =>
    request.get<ReadingVO[]>(`/iot/sensors/${id}/readings`, { limit }),

  create: (data: any) =>
    request.post<number>('/iot/sensors', data),

  update: (data: any) =>
    request.put<void>('/iot/sensors', data),

  remove: (id: number) =>
    request.delete<void>(`/iot/sensors/${id}`),

  wsStatus: () =>
    request.get<{ online: number }>('/iot/ws-status')
}
