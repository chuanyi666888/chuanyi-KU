import { request } from './request'

export interface OverviewVO {
  depotCount: number
  warehouseCount: number
  batchCount: number
  userCount: number
  sensorCount: number
  sensorTotal: number
  totalStockTons: number
  activeWarehouses: number
  alertPending: number
  alertProcessing: number
  alertResolved: number
  alertTotal: number
}

export interface DepotDistribution {
  id: number
  name: string
  province?: string
  city?: string
  longitude?: number
  latitude?: number
  capacityTons?: number
  stockTons: number
  manager?: string
  phone?: string
}

export interface WarehouseUsage {
  id: number
  name: string
  depotId: number
  capacityTons?: number
  currentTons?: number
  usageRate: number
  status: number
}

export interface GrainTypeDistribution {
  type: string
  name: string
  tons: number
}

export interface SensorTypeDist {
  type: string
  name: string
  total: number
  online: number
}

export interface AlertTrend {
  dates: string[]
  total: number[]
  high: number[]
}

export const dashboardApi = {
  overview: () => request.get<OverviewVO>('/dashboard/overview'),
  depots: () => request.get<DepotDistribution[]>('/dashboard/depots'),
  warehouses: () => request.get<WarehouseUsage[]>('/dashboard/warehouses'),
  grainTypes: () => request.get<GrainTypeDistribution[]>('/dashboard/grain-types'),
  sensorTypes: () => request.get<SensorTypeDist[]>('/dashboard/sensor-types'),
  alertTrend: () => request.get<AlertTrend>('/dashboard/alert-trend')
}
