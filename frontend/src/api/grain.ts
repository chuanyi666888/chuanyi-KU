import { request } from './request'
import type { PageResult } from '@/types/api'
import type {
  DepotVO,
  WarehouseVO,
  BatchVO,
  InboundOrderVO,
  OutboundOrderVO,
  InventoryVO,
  DepotQuery,
  WarehouseQuery,
  BatchQuery,
  OrderQuery,
  InventoryQuery,
  AuditRequest
} from '@/types/grain'

/** 粮库管理 API */
export const depotApi = {
  page: (params: DepotQuery) =>
    request.get<PageResult<DepotVO>>('/depots', params),

  all: () =>
    request.get<DepotVO[]>('/depots/all'),

  detail: (id: number) =>
    request.get<DepotVO>(`/depots/${id}`),

  create: (data: Partial<DepotVO>) =>
    request.post<number>('/depots', data),

  update: (data: Partial<DepotVO>) =>
    request.put<void>('/depots', data),

  remove: (id: number) =>
    request.delete<void>(`/depots/${id}`)
}

/** 粮仓管理 API */
export const warehouseApi = {
  page: (params: WarehouseQuery) =>
    request.get<PageResult<WarehouseVO>>('/warehouses', params),

  listByDepot: (depotId: number) =>
    request.get<WarehouseVO[]>(`/warehouses/by-depot/${depotId}`),

  detail: (id: number) =>
    request.get<WarehouseVO>(`/warehouses/${id}`),

  create: (data: Partial<WarehouseVO>) =>
    request.post<number>('/warehouses', data),

  update: (data: Partial<WarehouseVO>) =>
    request.put<void>('/warehouses', data),

  remove: (id: number) =>
    request.delete<void>(`/warehouses/${id}`)
}

/** 批次管理 API */
export const batchApi = {
  page: (params: BatchQuery) =>
    request.get<PageResult<BatchVO>>('/batches', params),

  detail: (id: number) =>
    request.get<BatchVO>(`/batches/${id}`),

  create: (data: Partial<BatchVO>) =>
    request.post<number>('/batches', data),

  update: (data: Partial<BatchVO>) =>
    request.put<void>('/batches', data),

  remove: (id: number) =>
    request.delete<void>(`/batches/${id}`)
}

/** 入库管理 API */
export const inboundApi = {
  page: (params: OrderQuery) =>
    request.get<PageResult<InboundOrderVO>>('/inbound-orders', params),

  detail: (id: number) =>
    request.get<InboundOrderVO>(`/inbound-orders/${id}`),

  create: (data: Partial<InboundOrderVO>) =>
    request.post<number>('/inbound-orders', data),

  audit: (data: AuditRequest) =>
    request.post<void>('/inbound-orders/audit', data)
}

/** 出库管理 API */
export const outboundApi = {
  page: (params: OrderQuery) =>
    request.get<PageResult<OutboundOrderVO>>('/outbound-orders', params),

  detail: (id: number) =>
    request.get<OutboundOrderVO>(`/outbound-orders/${id}`),

  create: (data: Partial<OutboundOrderVO>) =>
    request.post<number>('/outbound-orders', data),

  audit: (data: AuditRequest) =>
    request.post<void>('/outbound-orders/audit', data)
}

/** 库存查询 API */
export const inventoryApi = {
  page: (params: InventoryQuery) =>
    request.get<PageResult<InventoryVO>>('/inventories', params)
}
