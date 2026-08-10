<template>
  <div class="alert-record-page">
    <!-- 顶部统计条 -->
    <el-row :gutter="12" class="stat-row">
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
          <div class="stat-label">总告警</div>
          <div class="stat-value">{{ stats.total ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="粮库">
          <el-select v-model="query.depotId" placeholder="全部" clearable style="width: 140px" @change="loadWarehouses">
            <el-option v-for="d in depots" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="粮仓">
          <el-select v-model="query.warehouseId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="传感器类型">
          <el-select v-model="query.sensorType" placeholder="全部" clearable style="width: 130px">
            <el-option label="温度" value="TEMP" />
            <el-option label="湿度" value="HUMIDITY" />
            <el-option label="粮温" value="GRAIN_TEMP" />
            <el-option label="虫害" value="PEST" />
            <el-option label="气体" value="GAS" />
          </el-select>
        </el-form-item>
        <el-form-item label="告警类型">
          <el-select v-model="query.alertType" placeholder="全部" clearable style="width: 110px">
            <el-option label="超上限" value="HIGH" />
            <el-option label="低于下限" value="LOW" />
            <el-option label="离线" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重等级">
          <el-select v-model="query.severity" placeholder="全部" clearable style="width: 100px">
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已忽略" value="IGNORED" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 340px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="onSearch">查询</el-button>
          <el-button icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="list"
        v-loading="loading"
        border
        stripe
        :row-class-name="rowClassName"
        @row-click="onRowClick"
      >
        <el-table-column label="告警编号" prop="alertCode" width="170" />
        <el-table-column label="传感器" min-width="160">
          <template #default="{ row }">
            <div class="sensor-cell">
              <el-icon :color="typeColor(row.sensorType)"><Cpu /></el-icon>
              <span>{{ row.sensorName }}</span>
            </div>
            <div class="sensor-code">{{ row.sensorCode }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="alertTypeTag(row.alertType)" size="small">{{ row.alertTypeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="实际值" width="120">
          <template #default="{ row }">
            <span class="actual-value">{{ row.actualValue }}{{ row.unit }}</span>
            <div class="threshold-hint">阈值 {{ row.thresholdValue }}{{ row.unit }}</div>
          </template>
        </el-table-column>
        <el-table-column label="严重等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="severityTag(row.severity)" effect="dark" size="small">
              {{ row.severityName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="粮库/粮仓" min-width="140">
          <template #default="{ row }">
            <div>{{ row.depotName || '-' }}</div>
            <div class="threshold-hint">{{ row.warehouseName || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="触发时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.triggeredAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="primary" size="small" link
              @click.stop="openProcess(row, 'ACK')"
            >确认</el-button>
            <el-button
              v-if="row.status === 'PENDING' || row.status === 'PROCESSING'"
              type="success" size="small" link
              @click.stop="openProcess(row, 'RESOLVE')"
            >解决</el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="warning" size="small" link
              @click.stop="openProcess(row, 'IGNORE')"
            >忽略</el-button>
            <el-button
              v-if="row.status === 'RESOLVED' || row.status === 'IGNORED'"
              size="small" link
              @click.stop="openProcess(row, 'REOPEN')"
            >重开</el-button>
            <el-button size="small" link @click.stop="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 处理弹窗 -->
    <el-dialog v-model="processVisible" :title="processTitle" width="500px">
      <el-form :model="processForm" label-width="80px">
        <el-form-item label="告警编号">
          <span>{{ currentAlert?.alertCode }}</span>
        </el-form-item>
        <el-form-item label="传感器">
          <span>{{ currentAlert?.sensorName }}</span>
        </el-form-item>
        <el-form-item label="当前值">
          <span class="actual-value">
            {{ currentAlert?.actualValue }}{{ currentAlert?.unit }}
            （阈值 {{ currentAlert?.thresholdValue }}{{ currentAlert?.unit }}）
          </span>
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input
            v-model="processForm.comment"
            type="textarea"
            :rows="3"
            :placeholder="commentPlaceholder"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitProcess">确认</el-button>
      </template>
    </el-dialog>

    <!-- 详情 + 流水弹窗 -->
    <el-dialog v-model="detailVisible" title="告警详情与处理流水" width="720px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="告警编号">{{ currentAlert?.alertCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTag(currentAlert?.status)" size="small">{{ currentAlert?.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="传感器">{{ currentAlert?.sensorName }}（{{ currentAlert?.sensorCode }}）</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="alertTypeTag(currentAlert?.alertType)" size="small">{{ currentAlert?.alertTypeName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="实际值">{{ currentAlert?.actualValue }}{{ currentAlert?.unit }}</el-descriptions-item>
        <el-descriptions-item label="阈值">{{ currentAlert?.thresholdValue }}{{ currentAlert?.unit }}</el-descriptions-item>
        <el-descriptions-item label="严重等级">
          <el-tag :type="severityTag(currentAlert?.severity)" effect="dark" size="small">{{ currentAlert?.severityName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="粮库/粮仓">{{ currentAlert?.depotName }} / {{ currentAlert?.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="触发时间">{{ formatDateTime(currentAlert?.triggeredAt) }}</el-descriptions-item>
        <el-descriptions-item label="解决时间">{{ formatDateTime(currentAlert?.resolvedAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentAlert?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="logs-title">处理流水</div>
      <el-timeline v-if="logs.length" class="logs-timeline">
        <el-timeline-item
          v-for="log in logs"
          :key="log.id"
          :timestamp="formatDateTime(log.processAt)"
          :type="logTimelineType(log.action)"
        >
          <div class="log-line">
            <el-tag :type="actionTag(log.action)" size="small">{{ log.actionName }}</el-tag>
            <span class="log-status">{{ log.fromStatus }} → {{ log.toStatus }}</span>
            <span class="log-operator">{{ log.processByName || '系统' }}</span>
          </div>
          <div v-if="log.comment" class="log-comment">{{ log.comment }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无处理记录" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Cpu } from '@element-plus/icons-vue'
import { alertApi } from '@/api/alert'
import { depotApi, warehouseApi } from '@/api/grain'
import { formatDateTime } from '@/utils/format'
import type { AlertRecordVO, AlertProcessLogVO, AlertStatsVO, AlertQuery } from '@/types/alert'
import type { DepotVO, WarehouseVO } from '@/types/grain'

// ===== 状态卡 =====
const stats = ref<Partial<AlertStatsVO>>({})
const loadStats = async () => {
  try {
    stats.value = await alertApi.stats()
  } catch (e) {
    // 权限不足时静默
  }
}

// ===== 筛选 =====
const depots = ref<DepotVO[]>([])
const warehouses = ref<WarehouseVO[]>([])
const dateRange = ref<string[]>([])
const query = reactive<AlertQuery>({
  page: 1,
  size: 20,
  depotId: undefined,
  warehouseId: undefined,
  sensorType: undefined,
  alertType: undefined,
  severity: undefined,
  status: undefined
})

const loadWarehouses = async () => {
  warehouses.value = []
  query.warehouseId = undefined
  if (!query.depotId) return
  try {
    warehouses.value = await warehouseApi.listByDepot(query.depotId)
  } catch (e) {
    // ignore
  }
}

const onSearch = () => {
  query.page = 1
  loadList()
}

const onReset = () => {
  query.depotId = undefined
  query.warehouseId = undefined
  query.sensorType = undefined
  query.alertType = undefined
  query.severity = undefined
  query.status = undefined
  dateRange.value = []
  warehouses.value = []
  onSearch()
}

// ===== 列表 =====
const list = ref<AlertRecordVO[]>([])
const total = ref(0)
const loading = ref(false)

const loadList = async () => {
  loading.value = true
  try {
    if (dateRange.value && dateRange.value.length === 2) {
      query.startTime = dateRange.value[0]
      query.endTime = dateRange.value[1]
    } else {
      query.startTime = undefined
      query.endTime = undefined
    }
    const res = await alertApi.page(query)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const rowClassName = ({ row }: { row: AlertRecordVO }) => {
  if (row.status === 'PENDING' && row.severity === 3) return 'row-critical'
  if (row.status === 'PENDING') return 'row-pending'
  return ''
}

const onRowClick = (row: AlertRecordVO) => {
  viewDetail(row)
}

// ===== 处理 =====
const processVisible = ref(false)
const processForm = reactive<{ action: string; comment: string }>({ action: '', comment: '' })
const currentAlert = ref<AlertRecordVO | null>(null)
const submitting = ref(false)

const processTitle = computed(() => {
  switch (processForm.action) {
    case 'ACK': return '确认告警'
    case 'RESOLVE': return '解决告警'
    case 'IGNORE': return '忽略告警'
    case 'REOPEN': return '重新打开告警'
    default: return '处理告警'
  }
})

const commentPlaceholder = computed(() => {
  switch (processForm.action) {
    case 'ACK': return '请输入确认意见（如：已安排巡检）'
    case 'RESOLVE': return '请输入解决措施（如：通风降温后恢复）'
    case 'IGNORE': return '请输入忽略原因'
    case 'REOPEN': return '请输入重开原因'
    default: return '请输入处理意见'
  }
})

const openProcess = (row: AlertRecordVO, action: string) => {
  currentAlert.value = row
  processForm.action = action
  processForm.comment = ''
  processVisible.value = true
}

const submitProcess = async () => {
  if (!currentAlert.value) return
  submitting.value = true
  try {
    await alertApi.process({
      alertId: currentAlert.value.id,
      action: processForm.action as any,
      comment: processForm.comment
    })
    ElMessage.success('处理成功')
    processVisible.value = false
    loadList()
    loadStats()
  } finally {
    submitting.value = false
  }
}

// ===== 详情 + 流水 =====
const detailVisible = ref(false)
const logs = ref<AlertProcessLogVO[]>([])

const viewDetail = async (row: AlertRecordVO) => {
  currentAlert.value = row
  detailVisible.value = true
  try {
    logs.value = await alertApi.processLogs(row.id)
  } catch (e) {
    logs.value = []
  }
}

// ===== 样式辅助 =====
const typeColor = (t: string) => {
  switch (t) {
    case 'TEMP': return '#f56c6c'
    case 'HUMIDITY': return '#409eff'
    case 'GRAIN_TEMP': return '#e6a23c'
    case 'PEST': return '#67c23a'
    case 'GAS': return '#909399'
    default: return '#909399'
  }
}
const alertTypeTag = (t?: string) => {
  switch (t) {
    case 'HIGH': return 'danger'
    case 'LOW': return 'warning'
    case 'OFFLINE': return 'info'
    default: return ''
  }
}
const severityTag = (s?: number) => {
  switch (s) {
    case 3: return 'danger'
    case 2: return 'warning'
    case 1: return 'info'
    default: return ''
  }
}
const statusTag = (s?: string) => {
  switch (s) {
    case 'PENDING': return 'danger'
    case 'PROCESSING': return 'warning'
    case 'RESOLVED': return 'success'
    case 'IGNORED': return 'info'
    default: return ''
  }
}
const actionTag = (a: string) => {
  switch (a) {
    case 'ACK': return 'primary'
    case 'RESOLVE': return 'success'
    case 'IGNORE': return 'info'
    case 'REOPEN': return 'warning'
    default: return ''
  }
}
const logTimelineType = (a: string) => {
  switch (a) {
    case 'ACK': return 'primary'
    case 'RESOLVE': return 'success'
    case 'IGNORE': return 'info'
    case 'REOPEN': return 'warning'
    default: return ''
  }
}

// ===== WebSocket 实时告警通知 =====
let ws: WebSocket | null = null
const connectWs = () => {
  try {
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    ws = new WebSocket(`${proto}://${location.host}/ws/iot`)
    ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data)
        if (msg.type === 'ALERT') {
          ElMessageBox.alert(
            `传感器 ${msg.sensorName} 触发告警\n实际值 ${msg.actualValue}${msg.unit}（阈值 ${msg.thresholdValue}${msg.unit}）`,
            `新告警：${msg.alertCode}`,
            { type: msg.severity === 3 ? 'error' : 'warning', confirmButtonText: '查看' }
          ).then(() => {
            loadList()
            loadStats()
          }).catch(() => {
            loadList()
            loadStats()
          })
        }
      } catch (e) {
        // ignore non-JSON
      }
    }
    ws.onclose = () => {
      setTimeout(connectWs, 5000)
    }
  } catch (e) {
    // ignore
  }
}

onMounted(async () => {
  try {
    depots.value = await depotApi.all()
  } catch (e) {
    // ignore
  }
  loadList()
  loadStats()
  connectWs()
})

onUnmounted(() => {
  if (ws) ws.close()
})
</script>

<style scoped lang="scss">
.alert-record-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-row {
  margin: 0 !important;
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
}

.filter-card, .table-card {
  :deep(.el-card__body) {
    padding: 14px 16px;
  }
}

.sensor-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--el-text-color-primary);
}
.sensor-code {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.actual-value {
  color: var(--el-color-danger);
  font-weight: 600;
}
.threshold-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.logs-title {
  margin-top: 20px;
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
  border-left: 3px solid var(--el-color-primary);
  padding-left: 8px;
}
.logs-timeline {
  padding-left: 8px;
}
.log-line {
  display: flex;
  align-items: center;
  gap: 10px;
}
.log-status {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.log-operator {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.log-comment {
  margin-top: 4px;
  font-size: 13px;
  color: var(--el-text-color-primary);
}

:deep(.row-critical) {
  background-color: var(--el-color-danger-light-9) !important;
}
:deep(.row-pending) {
  background-color: var(--el-color-warning-light-9) !important;
}
</style>
