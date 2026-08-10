<template>
  <div class="inventory-page">
    <!-- 搜索栏 -->
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
        <el-form-item label="粮仓">
          <el-select
            v-model="query.warehouseId"
            placeholder="全部"
            clearable
            filterable
            style="width: 180px"
            :disabled="!query.depotId"
          >
            <el-option
              v-for="w in warehouseOptions"
              :key="w.id"
              :label="w.name"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="批次ID">
          <el-input
            v-model.number="query.batchId"
            placeholder="批次ID"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="粮食品种">
          <el-select v-model="query.grainType" placeholder="全部" clearable style="width: 140px">
            <el-option label="小麦" value="WHEAT" />
            <el-option label="玉米" value="CORN" />
            <el-option label="稻谷" value="RICE" />
            <el-option label="大豆" value="SOYBEAN" />
            <el-option label="高粱" value="SORGHUM" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片 -->
    <el-card class="stat-card">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">记录数</div>
            <div class="stat-value">{{ total }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">当前库存合计(吨)</div>
            <div class="stat-value primary">{{ formatNumber(sumCurrent, 3) }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">冻结库存合计(吨)</div>
            <div class="stat-value warning">{{ formatNumber(sumFrozen, 3) }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">可用库存合计(吨)</div>
            <div class="stat-value success">{{ formatNumber(sumAvailable, 3) }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table v-loading="loading" :data="tableData" border stripe show-summary :summary-method="getSummaries" style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="depotName" label="所属粮库" min-width="140" />
        <el-table-column prop="warehouseName" label="粮仓" min-width="140" />
        <el-table-column prop="batchNo" label="批次号" min-width="160" />
        <el-table-column label="粮食品种" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="grainTypeTag(row.grainType)" size="small">
              {{ grainTypeText(row.grainType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentTons" label="当前库存(吨)" width="140" align="right" />
        <el-table-column prop="frozenTons" label="冻结库存(吨)" width="140" align="right" />
        <el-table-column prop="availableTons" label="可用库存(吨)" width="140" align="right" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { inventoryApi, depotApi, warehouseApi } from '@/api/grain'
import { formatDateTime, formatNumber } from '@/utils/format'
import type { InventoryVO, DepotVO, WarehouseVO } from '@/types/grain'

const loading = ref(false)
const tableData = ref<InventoryVO[]>([])
const total = ref(0)
const depotOptions = ref<DepotVO[]>([])
const warehouseOptions = ref<WarehouseVO[]>([])

const query = reactive({
  page: 1,
  size: 20,
  depotId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  batchId: undefined as number | undefined,
  grainType: undefined as string | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const res = await inventoryApi.page(query)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function fetchDepots() {
  depotOptions.value = await depotApi.all()
}

async function handleDepotChange(depotId: any) {
  query.warehouseId = undefined
  if (depotId) {
    warehouseOptions.value = await warehouseApi.listByDepot(depotId)
  } else {
    warehouseOptions.value = []
  }
}

function handleSearch() {
  query.page = 1
  fetchData()
}
function handleReset() {
  query.depotId = undefined
  query.warehouseId = undefined
  query.batchId = undefined
  query.grainType = undefined
  query.page = 1
  warehouseOptions.value = []
  fetchData()
}

// 粮食品种映射
function grainTypeText(type: string): string {
  const map: Record<string, string> = {
    WHEAT: '小麦',
    CORN: '玉米',
    RICE: '稻谷',
    SOYBEAN: '大豆',
    SORGHUM: '高粱'
  }
  return map[type] || type
}
function grainTypeTag(type: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    WHEAT: 'warning',
    CORN: '',
    RICE: 'success',
    SOYBEAN: 'info',
    SORGHUM: 'danger'
  }
  return map[type] || 'info'
}

// 统计合计
const sumCurrent = computed(() => tableData.value.reduce((s, r) => s + (r.currentTons || 0), 0))
const sumFrozen = computed(() => tableData.value.reduce((s, r) => s + (r.frozenTons || 0), 0))
const sumAvailable = computed(() => tableData.value.reduce((s, r) => s + (r.availableTons || 0), 0))

function getSummaries({ columns, data }: { columns: any[]; data: InventoryVO[] }) {
  const sums: (string | number)[] = []
  columns.forEach((col, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    const prop = col.property
    if (prop === 'currentTons' || prop === 'frozenTons' || prop === 'availableTons') {
      sums[index] = formatNumber(data.reduce((s, r) => s + ((r as any)[prop] || 0), 0), 3)
    } else {
      sums[index] = ''
    }
  })
  return sums
}

onMounted(() => {
  fetchDepots()
  fetchData()
})
</script>

<style scoped lang="scss">
.inventory-page { padding: 0; }
.search-card { margin-bottom: 12px; }
.stat-card { margin-bottom: 12px; }
.stat-item {
  text-align: center;
  .stat-label {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-bottom: 6px;
  }
  .stat-value {
    font-size: 22px;
    font-weight: 600;
    color: var(--color-text-primary);
    &.primary { color: var(--color-primary); }
    &.success { color: var(--el-color-success); }
    &.warning { color: var(--el-color-warning); }
  }
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
