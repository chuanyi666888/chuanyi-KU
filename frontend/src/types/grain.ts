/** 粮库业务类型定义 */

export interface DepotVO {
  id: number
  code: string
  name: string
  address?: string
  province?: string
  city?: string
  district?: string
  longitude?: number
  latitude?: number
  capacityTons?: number
  manager?: string
  phone?: string
  status: number
  remark?: string
  createdAt?: string
}

export interface WarehouseVO {
  id: number
  depotId: number
  depotName?: string
  code: string
  name: string
  type: string
  capacityTons?: number
  currentTons?: number
  usageRate?: number
  status: number
  remark?: string
  createdAt?: string
}

export interface BatchVO {
  id: number
  batchNo: string
  grainType: string
  variety?: string
  grade?: string
  origin?: string
  productionYear?: number
  moisture?: number
  impurity?: number
  testWeight?: number
  totalInTons?: number
  totalOutTons?: number
  stockTons?: number
  status: number
  remark?: string
  createdAt?: string
}

export interface InboundOrderVO {
  id: number
  orderNo: string
  batchId: number
  batchNo: string
  depotId: number
  depotName?: string
  warehouseId: number
  warehouseName?: string
  weightTons: number
  inboundType: string
  carrier?: string
  vehicleNo?: string
  qualityReport?: string
  operatorId?: number
  operatorName?: string
  inboundAt?: string
  status: number
  auditRemark?: string
  auditedBy?: number
  auditedAt?: string
  remark?: string
  createdAt?: string
}

export interface OutboundOrderVO {
  id: number
  orderNo: string
  batchId: number
  batchNo: string
  depotId: number
  depotName?: string
  warehouseId: number
  warehouseName?: string
  weightTons: number
  outboundType: string
  customer?: string
  vehicleNo?: string
  operatorId?: number
  operatorName?: string
  outboundAt?: string
  status: number
  auditRemark?: string
  auditedBy?: number
  auditedAt?: string
  remark?: string
  createdAt?: string
}

export interface InventoryVO {
  id: number
  depotId: number
  depotName?: string
  warehouseId: number
  warehouseName?: string
  batchId: number
  batchNo?: string
  grainType: string
  currentTons: number
  frozenTons: number
  availableTons: number
  updatedAt?: string
}

/** 查询参数 */
export interface DepotQuery {
  page: number
  size: number
  keyword?: string
  status?: number
}

export interface WarehouseQuery {
  page: number
  size: number
  depotId?: number
  keyword?: string
  status?: number
}

export interface BatchQuery {
  page: number
  size: number
  keyword?: string
  grainType?: string
  status?: number
}

export interface OrderQuery {
  page: number
  size: number
  depotId?: number
  warehouseId?: number
  batchNo?: string
  status?: number
  keyword?: string
}

export interface InventoryQuery {
  page: number
  size: number
  depotId?: number
  warehouseId?: number
  batchId?: number
  grainType?: string
}

/** 入库/出库审核请求 */
export interface AuditRequest {
  id: number
  auditResult: number
  auditRemark?: string
}
