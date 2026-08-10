/** IoT 监控类型定义 */

export interface SensorVO {
  id: number
  code: string
  name: string
  warehouseId: number
  warehouseName?: string
  depotId: number
  depotName?: string
  sensorType: string
  unit: string
  minValue?: number
  maxValue?: number
  lastValue?: number
  lastAt?: string
  status: number
  remark?: string
  createdAt?: string
}

export interface ReadingVO {
  id: number
  sensorId: number
  sensorCode?: string
  sensorName?: string
  sensorType: string
  unit: string
  value: number
  readingAt: string
}

export interface SensorQuery {
  page: number
  size: number
  depotId?: number
  warehouseId?: number
  sensorType?: string
  status?: number
}

export interface WsMessage {
  sensorId: number
  sensorCode: string
  sensorName: string
  sensorType: string
  unit: string
  value: number
  readingAt: string
  alarm: boolean
}
