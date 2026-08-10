<template>
  <div class="screen-page">
    <!-- 顶部标题栏 -->
    <header class="screen-header">
      <div class="header-left">
        <span class="badge online"></span>
        <span>实时数据</span>
      </div>
      <div class="header-title">智慧粮库国产化综合运维与业务管理平台</div>
      <div class="header-right">
        <span>{{ currentTime }}</span>
      </div>
    </header>

    <!-- 主体 -->
    <main class="screen-body">
      <!-- KPI 行 -->
      <section class="kpi-row">
        <div class="kpi-card" v-for="kpi in kpis" :key="kpi.label">
          <div class="kpi-icon" :style="{ color: kpi.color }">
            <el-icon :size="26"><component :is="kpi.icon" /></el-icon>
          </div>
          <div class="kpi-text">
            <div class="kpi-value" :style="{ color: kpi.color }">{{ kpi.value }}</div>
            <div class="kpi-label">{{ kpi.label }}</div>
          </div>
        </div>
      </section>

      <!-- 三列图表 -->
      <section class="chart-grid">
        <!-- 左列 -->
        <div class="col col-left">
          <div class="panel">
            <div class="panel-title">粮食品种分布</div>
            <v-chart class="chart-small" :option="grainTypeOption" autoresize />
          </div>
          <div class="panel">
            <div class="panel-title">传感器类型分布</div>
            <v-chart class="chart-small" :option="sensorTypeOption" autoresize />
          </div>
        </div>

        <!-- 中列 -->
        <div class="col col-center">
          <div class="panel">
            <div class="panel-title">粮仓使用率 TOP</div>
            <v-chart class="chart-tall" :option="warehouseOption" autoresize />
          </div>
        </div>

        <!-- 右列 -->
        <div class="col col-right">
          <div class="panel">
            <div class="panel-title">近 7 日告警趋势</div>
            <v-chart class="chart-small" :option="alertTrendOption" autoresize />
          </div>
          <div class="panel">
            <div class="panel-title">实时告警事件</div>
            <div class="alert-stream">
              <div v-if="!alertStream.length" class="empty-tip">暂无新告警</div>
              <transition-group name="stream" tag="div">
                <div
                  v-for="a in alertStream.slice(0, 8)"
                  :key="a.alertId"
                  class="alert-item"
                  :class="`sev-${a.severity}`"
                >
                  <div class="alert-item-head">
                    <el-tag :type="severityTag(a.severity)" effect="dark" size="small">
                      {{ severityName(a.severity) }}
                    </el-tag>
                    <span class="alert-name">{{ a.sensorName }}</span>
                    <span class="alert-time">{{ formatRelativeTime(a.triggeredAt) }}</span>
                  </div>
                  <div class="alert-item-body">
                    实际 {{ a.actualValue }}{{ a.unit }}（阈值 {{ a.thresholdValue }}{{ a.unit }}）
                  </div>
                </div>
              </transition-group>
            </div>
          </div>
        </div>
      </section>

      <!-- 底部：粮库库存排名 -->
      <section class="panel panel-bottom">
        <div class="panel-title">粮库库存排名</div>
        <v-chart class="chart-bottom" :option="depotRankOption" autoresize />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, shallowRef } from 'vue'
import { VChart } from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart, LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DataZoomComponent
} from 'echarts/components'
import {
  OfficeBuilding, House, Coin, Cpu, Warning, User, DataLine
} from '@element-plus/icons-vue'
import { dashboardApi } from '@/api/dashboard'
import type {
  OverviewVO, DepotDistribution, WarehouseUsage,
  GrainTypeDistribution, SensorTypeDist, AlertTrend
} from '@/api/dashboard'
import { formatRelativeTime } from '@/utils/format'

use([
  CanvasRenderer, BarChart, PieChart, LineChart,
  GridComponent, TooltipComponent, LegendComponent, TitleComponent, DataZoomComponent
])

// ===== 时间 =====
const currentTime = ref('')
let timer: number | null = null
const updateTime = () => {
  const d = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')
  currentTime.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// ===== 数据 =====
const overview = ref<OverviewVO | null>(null)
const depots = ref<DepotDistribution[]>([])
const warehouses = ref<WarehouseUsage[]>([])
const grainTypes = ref<GrainTypeDistribution[]>([])
const sensorTypes = ref<SensorTypeDist[]>([])
const alertTrend = ref<AlertTrend | null>(null)

const kpis = computed(() => {
  const o = overview.value
  return [
    { label: '粮库总数', value: o?.depotCount ?? 0, icon: OfficeBuilding, color: '#4fc3f7' },
    { label: '粮仓总数', value: o?.warehouseCount ?? 0, icon: House, color: '#81c784' },
    { label: '库存总量(吨)', value: o?.totalStockTons ?? 0, icon: Coin, color: '#ffb74d' },
    { label: '在线传感器', value: `${o?.sensorCount ?? 0}/${o?.sensorTotal ?? 0}`, icon: Cpu, color: '#ba68c8' },
    { label: '待处理告警', value: o?.alertPending ?? 0, icon: Warning, color: '#e57373' },
    { label: '系统用户', value: o?.userCount ?? 0, icon: User, color: '#4dd0e1' }
  ]
})

// ===== 图表配置 =====
const darkTooltip = {
  backgroundColor: 'rgba(20,30,50,0.85)',
  borderColor: '#3a506b',
  textStyle: { color: '#fff' }
}

const grainTypeOption = computed(() => ({
  tooltip: { trigger: 'item', ...darkTooltip },
  legend: { bottom: 0, textStyle: { color: '#c5d0e0' } },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    center: ['50%', '45%'],
    label: { color: '#c5d0e0', formatter: '{b}\n{c}吨' },
    data: grainTypes.value.map(g => ({ name: g.name, value: g.tons })),
    itemStyle: { borderColor: '#0a1f3d', borderWidth: 2 }
  }]
}))

const sensorTypeOption = computed(() => ({
  tooltip: { trigger: 'axis', ...darkTooltip },
  legend: { bottom: 0, textStyle: { color: '#c5d0e0' }, data: ['在线', '总数'] },
  grid: { left: 40, right: 20, top: 30, bottom: 40 },
  xAxis: { type: 'category', data: sensorTypes.value.map(s => s.name), axisLine: { lineStyle: { color: '#3a506b' } }, axisLabel: { color: '#c5d0e0' } },
  yAxis: { type: 'value', minInterval: 1, axisLine: { lineStyle: { color: '#3a506b' } }, axisLabel: { color: '#c5d0e0' }, splitLine: { lineStyle: { color: 'rgba(58,80,107,0.3)' } } },
  series: [
    { name: '在线', type: 'bar', data: sensorTypes.value.map(s => s.online), itemStyle: { color: '#4fc3f7' }, barWidth: '30%' },
    { name: '总数', type: 'bar', data: sensorTypes.value.map(s => s.total), itemStyle: { color: '#3a506b' }, barWidth: '30%' }
  ]
}))

const warehouseOption = computed(() => {
  const sorted = [...warehouses.value].sort((a, b) => b.usageRate - a.usageRate).slice(0, 10)
  return {
    tooltip: { trigger: 'axis', ...darkTooltip, formatter: (p: any) => `${p[0].name}<br/>使用率: ${p[0].value}%<br/>当前: ${sorted[p[0].dataIndex]?.currentTons || 0}吨 / 容量: ${sorted[p[0].dataIndex]?.capacityTons || 0}吨` },
    grid: { left: 100, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value', max: 100, axisLine: { lineStyle: { color: '#3a506b' } }, axisLabel: { color: '#c5d0e0', formatter: '{value}%' }, splitLine: { lineStyle: { color: 'rgba(58,80,107,0.3)' } } },
    yAxis: { type: 'category', data: sorted.map(w => w.name).reverse(), axisLine: { lineStyle: { color: '#3a506b' } }, axisLabel: { color: '#c5d0e0' } },
    series: [{
      type: 'bar',
      data: sorted.map(w => w.usageRate).reverse(),
      barWidth: '50%',
      itemStyle: {
        color: (params: any) => {
          const v = params.value
          if (v >= 90) return '#e57373'
          if (v >= 70) return '#ffb74d'
          return '#81c784'
        },
        borderRadius: [0, 4, 4, 0]
      },
      label: { show: true, position: 'right', color: '#c5d0e0', formatter: '{c}%' }
    }]
  }
})

const alertTrendOption = computed(() => {
  const t = alertTrend.value
  return {
    tooltip: { trigger: 'axis', ...darkTooltip },
    legend: { bottom: 0, textStyle: { color: '#c5d0e0' }, data: ['总告警', '高严重'] },
    grid: { left: 35, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: t?.dates || [], boundaryGap: false, axisLine: { lineStyle: { color: '#3a506b' } }, axisLabel: { color: '#c5d0e0' } },
    yAxis: { type: 'value', minInterval: 1, axisLine: { lineStyle: { color: '#3a506b' } }, axisLabel: { color: '#c5d0e0' }, splitLine: { lineStyle: { color: 'rgba(58,80,107,0.3)' } } },
    series: [
      {
        name: '总告警', type: 'line', smooth: true,
        data: t?.total || [],
        itemStyle: { color: '#ffb74d' },
        lineStyle: { color: '#ffb74d', width: 2 },
        areaStyle: { opacity: 0.3, color: '#ffb74d' }
      },
      {
        name: '高严重', type: 'line', smooth: true,
        data: t?.high || [],
        itemStyle: { color: '#e57373' },
        lineStyle: { color: '#e57373', width: 2 },
        areaStyle: { opacity: 0.3, color: '#e57373' }
      }
    ]
  }
})

const depotRankOption = computed(() => {
  const sorted = [...depots.value].sort((a, b) => b.stockTons - a.stockTons)
  return {
    tooltip: { trigger: 'axis', ...darkTooltip, formatter: (p: any) => `${p[0].name}<br/>库存: ${p[0].value}吨<br/>容量: ${sorted[p[0].dataIndex]?.capacityTons || 0}吨` },
    grid: { left: 60, right: 30, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: sorted.map(d => d.name), axisLine: { lineStyle: { color: '#3a506b' } }, axisLabel: { color: '#c5d0e0' } },
    yAxis: { type: 'value', axisLine: { lineStyle: { color: '#3a506b' } }, axisLabel: { color: '#c5d0e0' }, splitLine: { lineStyle: { color: 'rgba(58,80,107,0.3)' } } },
    series: [{
      type: 'bar',
      data: sorted.map(d => d.stockTons),
      barWidth: '40%',
      itemStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#4fc3f7' },
            { offset: 1, color: '#1a3a5c' }
          ]
        },
        borderRadius: [4, 4, 0, 0]
      },
      label: { show: true, position: 'top', color: '#c5d0e0', formatter: '{c}吨' }
    }]
  }
})

// ===== 实时告警 WebSocket =====
interface AlertEvent {
  alertId: number
  alertCode: string
  sensorName: string
  severity: number
  actualValue: number
  thresholdValue: number
  unit: string
  triggeredAt: string
}
const alertStream = shallowRef<AlertEvent[]>([])
let ws: WebSocket | null = null
const connectWs = () => {
  try {
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    ws = new WebSocket(`${proto}://${location.host}/ws/iot`)
    ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data)
        if (msg.type === 'ALERT') {
          const event: AlertEvent = {
            alertId: msg.alertId,
            alertCode: msg.alertCode,
            sensorName: msg.sensorName,
            severity: msg.severity,
            actualValue: msg.actualValue,
            thresholdValue: msg.thresholdValue,
            unit: msg.unit,
            triggeredAt: msg.triggeredAt
          }
          // 头部插入，限制 30 条
          alertStream.value = [event, ...alertStream.value].slice(0, 30)
        }
      } catch (e) {
        // ignore
      }
    }
    ws.onclose = () => {
      setTimeout(connectWs, 5000)
    }
  } catch (e) {
    // ignore
  }
}

// ===== 辅助 =====
const severityName = (s: number) => s === 3 ? '高' : s === 2 ? '中' : '低'
const severityTag = (s: number) => s === 3 ? 'danger' : s === 2 ? 'warning' : 'info'

const loadAll = async () => {
  const tasks = [
    dashboardApi.overview(),
    dashboardApi.depots(),
    dashboardApi.warehouses(),
    dashboardApi.grainTypes(),
    dashboardApi.sensorTypes(),
    dashboardApi.alertTrend()
  ]
  const [o, d, w, g, s, at] = await Promise.allSettled(tasks)
  if (o.status === 'fulfilled') overview.value = o.value
  if (d.status === 'fulfilled') depots.value = d.value
  if (w.status === 'fulfilled') warehouses.value = w.value
  if (g.status === 'fulfilled') grainTypes.value = g.value
  if (s.status === 'fulfilled') sensorTypes.value = s.value
  if (at.status === 'fulfilled') alertTrend.value = at.value
}

let refreshTimer: number | null = null

onMounted(() => {
  updateTime()
  timer = window.setInterval(updateTime, 1000)
  loadAll()
  refreshTimer = window.setInterval(loadAll, 30000) // 30 秒刷新
  connectWs()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (refreshTimer) clearInterval(refreshTimer)
  if (ws) ws.close()
})
</script>

<style scoped lang="scss">
.screen-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 20% 0%, rgba(79, 195, 247, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 100%, rgba(186, 104, 200, 0.08) 0%, transparent 50%),
    linear-gradient(180deg, #0a1f3d 0%, #0a1428 100%);
  color: #c5d0e0;
  display: flex;
  flex-direction: column;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.screen-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: linear-gradient(90deg, rgba(10, 31, 61, 0.9), rgba(20, 50, 90, 0.9), rgba(10, 31, 61, 0.9));
  border-bottom: 1px solid rgba(79, 195, 247, 0.3);

  .header-left, .header-right {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 200px;
    font-size: 13px;
    color: #8fa8c5;
  }
  .header-right { justify-content: flex-end; }

  .badge {
    width: 8px; height: 8px; border-radius: 50%;
    &.online {
      background: #4fc3f7;
      box-shadow: 0 0 8px #4fc3f7;
      animation: pulse 2s infinite;
    }
  }

  .header-title {
    font-size: 22px;
    font-weight: 600;
    color: #fff;
    letter-spacing: 4px;
    background: linear-gradient(90deg, #4fc3f7, #81c784);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.screen-body {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: auto;
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.kpi-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: linear-gradient(135deg, rgba(20, 50, 90, 0.5), rgba(10, 31, 61, 0.5));
  border: 1px solid rgba(79, 195, 247, 0.2);
  border-radius: 6px;
  backdrop-filter: blur(8px);

  .kpi-icon {
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255,255,255,0.05);
    border-radius: 50%;
  }
  .kpi-value {
    font-size: 24px;
    font-weight: 700;
    line-height: 1;
  }
  .kpi-label {
    margin-top: 4px;
    font-size: 12px;
    color: #8fa8c5;
  }
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1.5fr 1fr;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(10, 31, 61, 0.5);
  border: 1px solid rgba(79, 195, 247, 0.2);
  border-radius: 6px;
  padding: 12px 14px;
  min-height: 0;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent, #4fc3f7, transparent);
  }

  .panel-title {
    font-size: 14px;
    font-weight: 600;
    color: #fff;
    margin-bottom: 8px;
    padding-left: 8px;
    border-left: 3px solid #4fc3f7;
  }
}

.chart-small { height: 200px; flex: 1; }
.chart-tall { height: 100%; flex: 1; min-height: 300px; }
.chart-bottom { height: 220px; }

.alert-stream {
  flex: 1;
  overflow: auto;
  padding-right: 4px;

  .empty-tip {
    text-align: center;
    color: #5a7090;
    padding: 40px 0;
    font-size: 13px;
  }

  .alert-item {
    background: rgba(255,255,255,0.03);
    border-left: 3px solid;
    padding: 8px 10px;
    margin-bottom: 8px;
    border-radius: 0 4px 4px 0;
    transition: all 0.3s;

    &.sev-3 { border-left-color: #e57373; background: rgba(229, 115, 115, 0.1); }
    &.sev-2 { border-left-color: #ffb74d; background: rgba(255, 183, 77, 0.08); }
    &.sev-1 { border-left-color: #81c784; }

    .alert-item-head {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
    }
    .alert-name {
      flex: 1;
      color: #e0e8f0;
      font-weight: 500;
    }
    .alert-time {
      color: #5a7090;
      font-size: 11px;
    }
    .alert-item-body {
      margin-top: 4px;
      font-size: 12px;
      color: #a0b0c5;
    }
  }
}

.stream-enter-active, .stream-leave-active {
  transition: all 0.4s ease;
}
.stream-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}
.stream-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.panel-bottom {
  flex: 0 0 auto;
}

/* 滚动条美化 */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: rgba(255,255,255,0.02); }
::-webkit-scrollbar-thumb { background: rgba(79, 195, 247, 0.3); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: rgba(79, 195, 247, 0.5); }
</style>
