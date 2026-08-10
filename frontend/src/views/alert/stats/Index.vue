<template>
  <div class="alert-stats-page">
    <!-- 状态卡 -->
    <el-row :gutter="12">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card pending">
          <div class="stat-label">待处理</div>
          <div class="stat-value">{{ stats.pending ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card processing">
          <div class="stat-label">处理中</div>
          <div class="stat-value">{{ stats.processing ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card resolved">
          <div class="stat-label">已解决</div>
          <div class="stat-value">{{ stats.resolved ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card total">
          <div class="stat-label">总告警数</div>
          <div class="stat-value">{{ stats.total ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row">
      <el-col :span="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>近 7 日告警趋势</span>
              <el-button link type="primary" icon="Refresh" @click="loadStats">刷新</el-button>
            </div>
          </template>
          <v-chart class="chart" :option="trendOption" autoresize />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>告警状态分布</span>
            </div>
          </template>
          <v-chart class="chart" :option="statusPieOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>按传感器类型</span>
            </div>
          </template>
          <v-chart class="chart" :option="sensorTypeOption" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>按严重等级</span>
            </div>
          </template>
          <v-chart class="chart" :option="severityOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>按粮库分布</span>
            </div>
          </template>
          <v-chart class="chart" :option="depotOption" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { VChart } from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart, LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
} from 'echarts/components'
import { alertApi } from '@/api/alert'
import { depotApi } from '@/api/grain'
import type { AlertStatsVO } from '@/types/alert'
import type { DepotVO } from '@/types/grain'

use([
  CanvasRenderer,
  BarChart,
  PieChart,
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
])

const stats = ref<AlertStatsVO | null>(null)
const depots = ref<DepotVO[]>([])

const loadStats = async () => {
  try {
    stats.value = await alertApi.stats()
  } catch (e) {
    // ignore
  }
}

// 近 7 日趋势
const trendOption = computed(() => {
  const trend = stats.value?.dailyTrend || {}
  const dates = Object.keys(trend).sort()
  const values = dates.map(d => trend[d] || 0)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: '告警数',
      type: 'line',
      smooth: true,
      data: values,
      areaStyle: { opacity: 0.3 },
      itemStyle: { color: '#f56c6c' },
      lineStyle: { color: '#f56c6c', width: 2 }
    }]
  }
})

// 状态饼图
const statusPieOption = computed(() => {
  const s = stats.value
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '45%'],
      label: { formatter: '{b}: {c}' },
      data: [
        { name: '待处理', value: s?.pending ?? 0, itemStyle: { color: '#f56c6c' } },
        { name: '处理中', value: s?.processing ?? 0, itemStyle: { color: '#e6a23c' } },
        { name: '已解决', value: s?.resolved ?? 0, itemStyle: { color: '#67c23a' } },
        { name: '已忽略', value: s?.ignored ?? 0, itemStyle: { color: '#909399' } }
      ]
    }]
  }
})

// 传感器类型
const sensorTypeOption = computed(() => {
  const m = stats.value?.bySensorType || {}
  const nameMap: Record<string, string> = {
    TEMP: '温度',
    HUMIDITY: '湿度',
    GRAIN_TEMP: '粮温',
    PEST: '虫害',
    GAS: '气体'
  }
  const keys = Object.keys(m)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: keys.map(k => nameMap[k] || k) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: keys.map(k => m[k] || 0),
      itemStyle: { color: '#409eff' },
      barWidth: '40%'
    }]
  }
})

// 严重等级
const severityOption = computed(() => {
  const m = stats.value?.bySeverity || {}
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: '60%',
      center: ['50%', '45%'],
      label: { formatter: '{b}: {c}' },
      data: [
        { name: '低', value: m[1] || 0, itemStyle: { color: '#909399' } },
        { name: '中', value: m[2] || 0, itemStyle: { color: '#e6a23c' } },
        { name: '高', value: m[3] || 0, itemStyle: { color: '#f56c6c' } }
      ]
    }]
  }
})

// 按粮库
const depotOption = computed(() => {
  const m = stats.value?.byDepot || {}
  const names: string[] = []
  const values: number[] = []
  for (const d of depots.value) {
    names.push(d.name)
    values.push(m[d.id] || 0)
  }
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: names },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: { color: '#67c23a' },
      barWidth: '40%'
    }]
  }
})

onMounted(async () => {
  try {
    depots.value = await depotApi.all()
  } catch (e) {
    // ignore
  }
  loadStats()
})
</script>

<style scoped lang="scss">
.alert-stats-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-card {
  border-left: 4px solid var(--el-border-color);
  .stat-label {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
  .stat-value {
    font-size: 26px;
    font-weight: 600;
    margin-top: 4px;
    color: var(--el-text-color-primary);
  }
  &.pending { border-left-color: var(--el-color-danger); .stat-value { color: var(--el-color-danger); } }
  &.processing { border-left-color: var(--el-color-warning); .stat-value { color: var(--el-color-warning); } }
  &.resolved { border-left-color: var(--el-color-success); .stat-value { color: var(--el-color-success); } }
  &.total { border-left-color: var(--el-color-primary); .stat-value { color: var(--el-color-primary); } }
}

.chart-row {
  margin: 0 !important;
}

.chart-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
  }
  :deep(.el-card__body) {
    padding: 14px 16px;
  }
}

.chart {
  height: 280px;
}
</style>
