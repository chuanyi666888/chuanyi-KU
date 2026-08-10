<template>
  <div class="sensor-page">
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
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <div class="toolbar">
        <el-button v-permission="'iot:sensor:create'" type="primary" :icon="Plus" @click="handleAdd">
          新增传感器
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="code" label="传感器编码" min-width="130" />
        <el-table-column prop="name" label="传感器名称" min-width="140" />
        <el-table-column prop="depotName" label="所属粮库" min-width="130" />
        <el-table-column prop="warehouseName" label="所属粮仓" min-width="130" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="sensorTypeTag(row.sensorType)" size="small">
              {{ sensorTypeText(row.sensorType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column label="阈值范围" width="140" align="center">
          <template #default="{ row }">
            {{ formatNum(row.minValue) }} ~ {{ formatNum(row.maxValue) }}
          </template>
        </el-table-column>
        <el-table-column label="最新读数" width="110" align="right">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isAlarm(row) }">{{ formatNum(row.lastValue) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最近上报" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.lastAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'iot:sensor:update'"
              type="primary"
              size="small"
              link
              @click="handleEdit(row as SensorVO)"
            >编辑</el-button>
            <el-button
              v-permission="'iot:sensor:delete'"
              type="danger"
              size="small"
              link
              @click="handleDelete(row as SensorVO)"
            >删除</el-button>
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
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item v-if="!isEdit" label="传感器编码" prop="code">
          <el-input v-model="form.code" placeholder="字母/数字组合" />
        </el-form-item>
        <el-form-item label="传感器名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="所属粮库" prop="depotId">
          <el-select
            v-model="form.depotId"
            placeholder="请选择粮库"
            filterable
            style="width: 100%"
            @change="handleFormDepotChange"
          >
            <el-option
              v-for="d in depotOptions"
              :key="d.id"
              :label="d.name"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="所属粮仓" prop="warehouseId">
          <el-select
            v-model="form.warehouseId"
            placeholder="请选择粮仓"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="w in formWarehouseOptions"
              :key="w.id"
              :label="w.name"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="传感器类型" prop="sensorType">
          <el-select v-model="form.sensorType" placeholder="请选择类型" style="width: 100%">
            <el-option
              v-for="(label, key) in sensorTypeMap"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如 ℃、%、ppm" />
        </el-form-item>
        <el-form-item label="阈值下限" prop="minValue">
          <el-input-number
            v-model="form.minValue"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="阈值上限" prop="maxValue">
          <el-input-number
            v-model="form.maxValue"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { sensorApi } from '@/api/iot'
import { depotApi, warehouseApi } from '@/api/grain'
import { formatDateTime } from '@/utils/format'
import type { SensorVO } from '@/types/iot'
import type { DepotVO, WarehouseVO } from '@/types/grain'

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

function formatNum(val?: number): string {
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

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<SensorVO[]>([])
const total = ref(0)
const depotOptions = ref<DepotVO[]>([])
const warehouseOptions = ref<WarehouseVO[]>([])

const query = reactive({
  page: 1,
  size: 20,
  depotId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  sensorType: undefined as string | undefined,
  status: undefined as number | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const res = await sensorApi.page(query)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

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

// 新增/编辑
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const formWarehouseOptions = ref<WarehouseVO[]>([])

const form = reactive({
  id: undefined as number | undefined,
  code: '',
  name: '',
  depotId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  sensorType: 'TEMP',
  unit: '',
  minValue: undefined as number | undefined,
  maxValue: undefined as number | undefined,
  status: 1,
  remark: ''
})

const rules: FormRules = {
  code: [{ required: true, message: '请输入传感器编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入传感器名称', trigger: 'blur' }],
  depotId: [{ required: true, message: '请选择所属粮库', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择所属粮仓', trigger: 'change' }],
  sensorType: [{ required: true, message: '请选择传感器类型', trigger: 'change' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
}

function resetForm() {
  form.id = undefined
  form.code = ''
  form.name = ''
  form.depotId = undefined
  form.warehouseId = undefined
  form.sensorType = 'TEMP'
  form.unit = ''
  form.minValue = undefined
  form.maxValue = undefined
  form.status = 1
  form.remark = ''
  formWarehouseOptions.value = []
}

async function handleFormDepotChange(depotId: number | undefined) {
  form.warehouseId = undefined
  formWarehouseOptions.value = []
  if (depotId) {
    formWarehouseOptions.value = await warehouseApi.listByDepot(depotId)
  }
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增传感器'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: SensorVO) {
  isEdit.value = true
  dialogTitle.value = '编辑传感器'
  resetForm()
  form.id = row.id
  form.code = row.code
  form.name = row.name
  form.depotId = row.depotId
  form.warehouseId = row.warehouseId
  form.sensorType = row.sensorType
  form.unit = row.unit
  form.minValue = row.minValue
  form.maxValue = row.maxValue
  form.status = row.status
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await sensorApi.update({
          id: form.id,
          name: form.name,
          sensorType: form.sensorType,
          unit: form.unit,
          minValue: form.minValue,
          maxValue: form.maxValue,
          status: form.status,
          remark: form.remark
        })
        ElMessage.success('修改成功')
      } else {
        await sensorApi.create({
          code: form.code,
          name: form.name,
          depotId: form.depotId,
          warehouseId: form.warehouseId,
          sensorType: form.sensorType,
          unit: form.unit,
          minValue: form.minValue,
          maxValue: form.maxValue,
          remark: form.remark
        })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      fetchData()
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row: SensorVO) {
  await ElMessageBox.confirm(`确认删除传感器「${row.name}」？`, '提示', { type: 'warning' })
  await sensorApi.remove(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => {
  fetchDepots()
  fetchData()
})
</script>

<style scoped lang="scss">
.sensor-page { padding: 0; }
.search-card { margin-bottom: 12px; }
.toolbar {
  margin-bottom: 12px;
  display: flex;
  justify-content: flex-start;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.text-danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
