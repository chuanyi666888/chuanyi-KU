/** 告警系统类型定义 */

export interface AlertRecordVO {
  id: number
  alertCode: string
  sensorId: number
  sensorCode: string
  sensorName: string
  warehouseId?: number
  warehouseName?: string
  depotId?: number
  depotName?: string
  sensorType: string
  alertType: string
  alertTypeName?: string
  thresholdValue?: number
  actualValue: number
  unit: string
  severity: number
  severityName?: string
  status: string
  statusName?: string
  triggeredAt: string
  resolvedAt?: string
  remark?: string
  createdAt?: string
}

export interface AlertProcessLogVO {
  id: number
  alertId: number
  action: string
  actionName?: string
  fromStatus?: string
  toStatus: string
  comment?: string
  processAt: string
  processBy?: number
  processByName?: string
}

export interface AlertProcessRequest {
  alertId: number
  action: 'ACK' | 'RESOLVE' | 'IGNORE' | 'REOPEN'
  comment?: string
}

export interface AlertStatsVO {
  total: number
  pending: number
  processing: number
  resolved: number
  ignored: number
  bySeverity: Record<number, number>
  bySensorType: Record<string, number>
  byDepot: Record<number, number>
  dailyTrend: Record<string, number>
}

export interface AlertQuery {
  page: number
  size: number
  depotId?: number
  warehouseId?: number
  sensorId?: number
  sensorType?: string
  alertType?: string
  severity?: number
  status?: string
  startTime?: string
  endTime?: string
}

/** 告警 WebSocket 通知 */
export interface AlertWsMessage {
  type: 'ALERT'
  alertId: number
  alertCode: string
  sensorId: number
  sensorCode: string
  sensorName: string
  sensorType: string
  alertType: string
  severity: number
  actualValue: number
  thresholdValue?: number
  unit: string
  triggeredAt: string
}
