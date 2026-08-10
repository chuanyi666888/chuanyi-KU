<template>
  <div class="audit-page">
    <el-card class="search-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="操作人">
          <el-input v-model="query.operator" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="query.module" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="操作">
          <el-select v-model="query.operation" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="op in operations" :key="op" :label="op" :value="op" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            @change="handleTimeChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="operation" label="操作" width="100">
          <template #default="{ row }">
            <el-tag :type="opTagType(row.operation)" size="small">{{ row.operation }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="target" label="目标" min-width="120" show-overflow-tooltip />
        <el-table-column prop="method" label="方法" width="80" />
        <el-table-column prop="requestUrl" label="URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="requestIp" label="IP" width="140" />
        <el-table-column label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.result === 1 ? 'success' : 'danger'" size="small">
              {{ row.result === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时(ms)" width="100" align="right" />
        <el-table-column prop="createdAt" label="时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="详情" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="showDetail(row as AuditLog)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[20, 50, 100, 200]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="审计日志详情" width="700px">
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item label="操作人">{{ currentLog.operator || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentLog.operation }}</el-descriptions-item>
        <el-descriptions-item label="目标">{{ currentLog.target || '-' }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ currentLog.method || '-' }}</el-descriptions-item>
        <el-descriptions-item label="URL">{{ currentLog.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ currentLog.requestIp }}</el-descriptions-item>
        <el-descriptions-item label="结果">{{ currentLog.result === 1 ? '成功' : '失败' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog.costMs }} ms</el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatDateTime(currentLog.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2">
          {{ currentLog.errorMsg || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="json-pre">{{ currentLog.requestBody }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { auditApi } from '@/api/audit'
import { formatDateTime } from '@/utils/format'
import type { AuditLog } from '@/types/api'

const loading = ref(false)
const tableData = ref<AuditLog[]>([])
const total = ref(0)

const operations = ['LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'DELETE', 'ASSIGN', 'RESET']
const timeRange = ref<[string, string] | null>(null)

const query = reactive({
  page: 1,
  size: 20,
  operator: '',
  module: '',
  operation: '',
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined
})

function handleTimeChange(val: [string, string] | null) {
  if (val) {
    query.startTime = val[0]
    query.endTime = val[1]
  } else {
    query.startTime = undefined
    query.endTime = undefined
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await auditApi.page(query)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  fetchData()
}
function handleReset() {
  query.operator = ''
  query.module = ''
  query.operation = ''
  query.startTime = undefined
  query.endTime = undefined
  timeRange.value = null
  query.page = 1
  fetchData()
}

function opTagType(op: string): any {
  if (op === 'LOGIN' || op === 'LOGOUT') return 'info'
  if (op === 'CREATE') return 'success'
  if (op === 'UPDATE' || op === 'ASSIGN' || op === 'RESET') return 'warning'
  if (op === 'DELETE') return 'danger'
  return ''
}

// 详情
const detailVisible = ref(false)
const currentLog = ref<AuditLog | null>(null)
function showDetail(row: AuditLog) {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.audit-page { padding: 0; }
.search-card { margin-bottom: 12px; }
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.json-pre {
  background: var(--color-bg-secondary);
  padding: 8px;
  border-radius: 4px;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
