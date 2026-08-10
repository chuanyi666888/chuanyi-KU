<template>
  <div class="iot-monitor-page">
    <!-- 顶部搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="所属粮库">
          <el-select
            v-model="query.depotId"
            placeholder="全部"
            clearable
            filterable
            style="width: 180px"
            @change="handleDepotChange"
          >
            <el-option
              v-for="d in depotOptions"
              :key="d.id"
              :label="d.name"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属粮仓">
          <el-select
            v-model="query.warehouseId"
            placeholder="全部"
            clearable
            filterable
            style="width: 180px"
          >
            <el-option
              v-for="w in warehouseOptions"
              :key="w.id"
              :label="w.name"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="传感器类型">
          <el-select v-model="query.sensorType" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="(label, key) in sensorTypeMap"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
        <el-form-item>
          <!-- WebSocket 状态指示器 -->
          <div class="ws-indicator">
            <span :class="['ws-dot', wsConnected ? 'online' : 'offline']" />
            <span class="ws-text">{{ wsConnected ? '已连接' : '已断开' }}</span>
            <el-divider direction="vertical" />
            <span class="ws-text">在线客户端：{{ wsOnline }}</span>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 传感器卡片网格 -->
    <el-card class="grid-card" v-loading="loading">
      <div v-if="sensors.length === 0 && !loading" class="empty-tip">
        <el-empty description="暂无传感器数据" />
      </div>
      <el-row :gutter="12">
        <el-col
          v-for="sensor in sensors"
          :key="sensor.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <div
            :class="['sensor-card', { 'alarm': isAlarm(sensor) }]"
            @click="handleSelectSensor(sensor)"
          >
            <div class="card-header">
              <span class="card-name">{{ sensor.name }}</span>
              <el-tag :type="sensorTypeTag(sensor.sensorType)" size="small">
                {{ sensorTypeText(sensor.sensorType) }}
              </el-tag>
            </div>
            <div class="card-value">
              <span class="value-num">{{ formatSensorValue(sensor.lastValue) }}</span>
              <span class="value-unit">{{ sensor.unit }}</span>
            </div>
            <div class="card-range">
              阈值范围：{{ formatSensorValue(sensor.minValue) }} ~ {{ formatSensorValue(sensor.maxValue) }} {{ sensor.unit }}
            </div>
            <div class="card-footer">
              <span class="card-time">{{ formatRelativeTime(sensor.lastAt) }}</span>
              <el-tag :type="sensor.status === 1 ? 'success' : 'info'" size="small">
                {{ sensor.status === 1 ? '在线' : '离线' }}
              </el-tag>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 实时图表区域 -->
    <el-card class="chart-card">
      <template #header>
        <div class="chart-header">
          <span class="chart-title">
            实时读数曲线
            <span v-if="selectedSensor" class="chart-sub-title">
              —— {{ selectedSensor.name }}（{{ selectedSensor.code }}）
            </span>
          </span>
          <el-button
            v-if="selectedSensor"
            type="primary"
            size="small"
            :icon="Refresh"
            @click="loadReadings(selectedSensor.id)"
          >刷新历史</el-button>
        </div>
      </template>
      <div v-if="!selectedSensor" class="empty-tip">
        <el-empty description="点击上方传感器卡片查看实时曲线" />
      </div>
      <v-chart
        v-else
        class="chart"
        :option="chartOption"
        autoresize
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { VChart } from 'vue-echarts'
import { sensorApi } from '@/api/iot'
import { depotApi, warehouseApi } from '@/api/grain'
import { formatRelativeTime } from '@/utils/format'
import type { SensorVO, ReadingVO, WsMessage } from '@/types/iot'
import type { DepotVO, WarehouseVO } from '@/types/grain'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

// 传感器类型映射
const sensorTypeMap: Record<string, string> = {
  TEMP: '温度',
  HUMIDITY: '湿度',
  GRAIN_TEMP: '粮温',
  PEST: '虫害',
  GAS: '气体'
}

function sensorTypeText(type: string): string {
  return sensorTypeMap[type] || type
}

function sensorTypeTag(type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const tagMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    TEMP: 'warning',
    HUMIDITY: 'primary',
    GRAIN_TEMP: 'success',
    PEST: 'danger',
    GAS: 'info'
  }
  return tagMap[type] || 'info'
}

function formatSensorValue(val?: number): string {
  if (val === null || val === undefined) return '--'
  return val.toFixed(1)
}

// 判断是否告警（超阈值）
function isAlarm(sensor: SensorVO): boolean {
  if (sensor.lastValue === null || sensor.lastValue === undefined) return false
  if (sensor.minValue !== undefined && sensor.lastValue < sensor.minValue) return true
  if (sensor.maxValue !== undefined && sensor.lastValue > sensor.maxValue) return true
  return false
}

// 搜索条件
const query = reactive({
  page: 1,
  size: 100,
  depotId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  sensorType: undefined as string | undefined,
  status: undefined as number | undefined
})

// 下拉数据
const depotOptions = ref<DepotVO[]>([])
const warehouseOptions = ref<WarehouseVO[]>([])

// 传感器数据
const loading = ref(false)
const sensors = ref<SensorVO[]>([])

async function fetchDepots() {
  depotOptions.value = await depotApi.all()
}

async function handleDepotChange(depotId: number | undefined) {
  query.warehouseId = undefined
  warehouseOptions.value = []
  if (depotId) {
    warehouseOptions.value = await warehouseApi.listByDepot(depotId)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await sensorApi.page(query)
    sensors.value = res.records
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  fetchData()
}

function handleReset() {
  query.depotId = undefined
  query.warehouseId = undefined
  query.sensorType = undefined
  query.status = undefined
  query.page = 1
  warehouseOptions.value = []
  fetchData()
}

// WebSocket 状态
const wsConnected = ref(false)
const wsOnline = ref(0)
let ws: WebSocket | null = null

async function fetchWsStatus() {
  try {
    const res = await sensorApi.wsStatus()
    wsOnline.value = res.online
  } catch (e) {
    // 忽略
  }
}

function connectWs() {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const url = `${proto}://${location.host}/ws/iot`
  try {
    ws = new WebSocket(url)
  } catch (e) {
    wsConnected.value = false
    return
  }

  ws.onopen = () => {
    wsConnected.value = true
    ElMessage.success('WebSocket 已连接')
  }

  ws.onclose = () => {
    wsConnected.value = false
  }

  ws.onerror = () => {
    wsConnected.value = false
  }

  ws.onmessage = (event) => {
    try {
      const msg: WsMessage = JSON.parse(event.data)
      handleWsMessage(msg)
    } catch (e) {
      // 忽略解析失败
    }
  }
}

function handleWsMessage(msg: WsMessage) {
  // 更新对应传感器卡片的 lastValue
  const target = sensors.value.find((s) => s.id === msg.sensorId)
  if (target) {
    target.lastValue = msg.value
    target.lastAt = msg.readingAt
  }
  // 如果当前选中的传感器匹配，追加数据到图表
  if (selectedSensor.value && selectedSensor.value.id === msg.sensorId) {
    chartData.value.push({
      id: Date.now(),
      sensorId: msg.sensorId,
      sensorType: msg.sensorType,
      unit: msg.unit,
      value: msg.value,
      readingAt: msg.readingAt
    })
    if (chartData.value.length > 60) {
      chartData.value.shift()
    }
  }
}

function disconnectWs() {
  if (ws) {
    ws.onclose = null
    ws.onerror = null
    ws.onmessage = null
    ws.onopen = null
    if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
      ws.close()
    }
    ws = null
  }
  wsConnected.value = false
}

// 选中传感器 & 图表
const selectedSensor = ref<SensorVO | null>(null)
const chartData = ref<ReadingVO[]>([])

async function handleSelectSensor(sensor: SensorVO) {
  selectedSensor.value = sensor
  await loadReadings(sensor.id)
}

async function loadReadings(sensorId: number) {
  try {
    const readings = await sensorApi.readings(sensorId, 60)
    chartData.value = readings || []
  } catch (e) {
    chartData.value = []
  }
}

const chartOption = computed(() => {
  const data = chartData.value
  const xs = data.map((r) => formatRelativeTime(r.readingAt))
  const ys = data.map((r) => r.value)
  const unit = selectedSensor.value?.unit || ''
  const minLine = selectedSensor.value?.minValue
  const maxLine = selectedSensor.value?.maxValue

  const series: any[] = [
    {
      name: selectedSensor.value?.name || '读数',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      data: ys,
      itemStyle: { color: '#409EFF' },
      lineStyle: { width: 2, color: '#409EFF' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(64,158,255,0.3)' },
            { offset: 1, color: 'rgba(64,158,255,0.02)' }
          ]
        }
      }
    }
  ]

  if (minLine !== undefined && minLine !== null) {
    series.push({
      name: '下限',
      type: 'line',
      symbol: 'none',
      data: data.map(() => minLine),
      lineStyle: { color: '#E6A23C', type: 'dashed', width: 1 },
      itemStyle: { color: '#E6A23C' }
    })
  }
  if (maxLine !== undefined && maxLine !== null) {
    series.push({
      name: '上限',
      type: 'line',
      symbol: 'none',
      data: data.map(() => maxLine),
      lineStyle: { color: '#F56C6C', type: 'dashed', width: 1 },
      itemStyle: { color: '#F56C6C' }
    })
  }

  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any[]) => {
        if (!params || params.length === 0) return ''
        const head = params[0].axisValue
        const lines = params
          .map((p) => `${p.marker} ${p.seriesName}：${p.value}${unit}`)
          .join('<br/>')
        return `${head}<br/>${lines}`
      }
    },
    legend: {
      data: series.map((s) => s.name),
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 40,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xs
    },
    yAxis: {
      type: 'value',
      name: unit,
      axisLabel: { formatter: `{value} ${unit}` }
    },
    series
  }
})

let wsTimer: number | undefined

onMounted(() => {
  fetchDepots()
  fetchData()
  fetchWsStatus()
  connectWs()
  // 定时刷新在线客户端数
  wsTimer = window.setInterval(fetchWsStatus, 30000)
})

onUnmounted(() => {
  disconnectWs()
  if (wsTimer) {
    clearInterval(wsTimer)
    wsTimer = undefined
  }
})
</script>

<style scoped lang="scss">
.iot-monitor-page {
  padding: 0;
}
.search-card {
  margin-bottom: 12px;
}
.grid-card {
  margin-bottom: 12px;
}
.empty-tip {
  padding: 24px 0;
}
.ws-indicator {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}
.ws-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #909399;
}
.ws-dot.online {
  background: #67c23a;
  box-shadow: 0 0 6px #67c23a;
}
.ws-dot.offline {
  background: #f56c6c;
  box-shadow: 0 0 6px #f56c6c;
}
.ws-text {
  font-size: 13px;
  color: #606266;
}
.sensor-card {
  position: relative;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.sensor-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}
.sensor-card.alarm {
  border-color: #f56c6c;
  animation: alarm-blink 1.2s ease-in-out infinite;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.card-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 60%;
}
.card-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 8px;
}
.value-num {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
}
.value-unit {
  font-size: 13px;
  color: #909399;
}
.card-range {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}
.card-time {
  font-size: 12px;
}
.chart-card {
  margin-bottom: 12px;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.chart-sub-title {
  font-size: 13px;
  font-weight: normal;
  color: #909399;
  margin-left: 4px;
}
.chart {
  width: 100%;
  height: 360px;
}
@keyframes alarm-blink {
  0%,
  100% {
    box-shadow: 0 0 8px #f56c6c;
  }
  50% {
    box-shadow: 0 0 16px #f56c6c;
  }
}
</style>
