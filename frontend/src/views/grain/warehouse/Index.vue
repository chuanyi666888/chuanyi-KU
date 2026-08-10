<template>
  <div class="warehouse-page">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="所属粮库">
          <el-select
            v-model="query.depotId"
            placeholder="全部"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option
              v-for="d in depotOptions"
              :key="d.id"
              :label="d.name"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="编码/名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="停用" :value="0" />
            <el-option label="空仓" :value="1" />
            <el-option label="在储" :value="2" />
            <el-option label="满仓" :value="3" />
            <el-option label="维修中" :value="4" />
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
        <el-button v-permission="'grain:warehouse:create'" type="primary" :icon="Plus" @click="handleAdd">
          新增粮仓
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="code" label="粮仓编码" min-width="120" />
        <el-table-column prop="name" label="粮仓名称" min-width="140" />
        <el-table-column prop="depotName" label="所属粮库" min-width="140" />
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="warehouseTypeTag(row.type)" size="small">
              {{ warehouseTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="capacityTons" label="库容(吨)" width="110" align="right" />
        <el-table-column prop="currentTons" label="当前(吨)" width="110" align="right" />
        <el-table-column label="使用率" width="160">
          <template #default="{ row }">
            <el-progress
              :percentage="Number((row.usageRate ? row.usageRate * 100 : 0).toFixed(1))"
              :status="usageStatus(row.usageRate)"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="warehouseStatusTag(row.status)" size="small">
              {{ warehouseStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'grain:warehouse:update'"
              type="primary"
              size="small"
              link
              @click="handleEdit(row as WarehouseVO)"
            >编辑</el-button>
            <el-button
              v-permission="'grain:warehouse:delete'"
              type="danger"
              size="small"
              link
              @click="handleDelete(row as WarehouseVO)"
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
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属粮库" prop="depotId">
          <el-select
            v-model="form.depotId"
            placeholder="请选择粮库"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="d in depotOptions"
              :key="d.id"
              :label="d.name"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="粮仓编码" prop="code">
          <el-input v-model="form.code" placeholder="字母/数字组合" />
        </el-form-item>
        <el-form-item label="粮仓名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="粮仓类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="平房仓" value="FLAT" />
            <el-option label="立筒仓" value="SILO" />
            <el-option label="浅圆仓" value="DOME" />
          </el-select>
        </el-form-item>
        <el-form-item label="库容(吨)" prop="capacityTons">
          <el-input-number v-model="form.capacityTons" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="停用" :value="0" />
            <el-option label="空仓" :value="1" />
            <el-option label="在储" :value="2" />
            <el-option label="满仓" :value="3" />
            <el-option label="维修中" :value="4" />
          </el-select>
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
import { warehouseApi, depotApi } from '@/api/grain'
import { formatDateTime } from '@/utils/format'
import type { WarehouseVO, DepotVO } from '@/types/grain'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<WarehouseVO[]>([])
const total = ref(0)
const depotOptions = ref<DepotVO[]>([])

const query = reactive({
  page: 1,
  size: 20,
  depotId: undefined as number | undefined,
  keyword: '',
  status: undefined as number | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const res = await warehouseApi.page(query)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function fetchDepots() {
  depotOptions.value = await depotApi.all()
}

function handleSearch() {
  query.page = 1
  fetchData()
}
function handleReset() {
  query.depotId = undefined
  query.keyword = ''
  query.status = undefined
  query.page = 1
  fetchData()
}

// 粮仓类型映射
function warehouseTypeText(type: string): string {
  const map: Record<string, string> = { FLAT: '平房仓', SILO: '立筒仓', DOME: '浅圆仓' }
  return map[type] || type
}
function warehouseTypeTag(type: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    FLAT: '',
    SILO: 'success',
    DOME: 'warning'
  }
  return map[type] || 'info'
}

// 粮仓状态映射: 0停用/danger, 1空仓/info, 2在储/warning, 3满仓/success, 4维修中/danger
function warehouseStatusText(status: number): string {
  const map: Record<number, string> = { 0: '停用', 1: '空仓', 2: '在储', 3: '满仓', 4: '维修中' }
  return map[status] ?? String(status)
}
function warehouseStatusTag(status: number): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'danger',
    1: 'info',
    2: 'warning',
    3: 'success',
    4: 'danger'
  }
  return map[status] ?? 'info'
}

function usageStatus(rate?: number): '' | 'success' | 'warning' | 'exception' {
  if (!rate) return ''
  if (rate >= 0.9) return 'exception'
  if (rate >= 0.7) return 'warning'
  return 'success'
}

// 新增/编辑
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  depotId: undefined as number | undefined,
  code: '',
  name: '',
  type: 'FLAT',
  capacityTons: undefined as number | undefined,
  status: 1,
  remark: ''
})

const rules: FormRules = {
  depotId: [{ required: true, message: '请选择所属粮库', trigger: 'change' }],
  code: [{ required: true, message: '请输入粮仓编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入粮仓名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择粮仓类型', trigger: 'change' }]
}

function resetForm() {
  form.id = undefined
  form.depotId = undefined
  form.code = ''
  form.name = ''
  form.type = 'FLAT'
  form.capacityTons = undefined
  form.status = 1
  form.remark = ''
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增粮仓'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: WarehouseVO) {
  isEdit.value = true
  dialogTitle.value = '编辑粮仓'
  resetForm()
  form.id = row.id
  form.depotId = row.depotId
  form.code = row.code
  form.name = row.name
  form.type = row.type
  form.capacityTons = row.capacityTons
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
        await warehouseApi.update({
          id: form.id!,
          depotId: form.depotId,
          name: form.name,
          type: form.type,
          capacityTons: form.capacityTons,
          status: form.status,
          remark: form.remark
        })
        ElMessage.success('修改成功')
      } else {
        await warehouseApi.create({
          depotId: form.depotId,
          code: form.code,
          name: form.name,
          type: form.type,
          capacityTons: form.capacityTons,
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

async function handleDelete(row: WarehouseVO) {
  await ElMessageBox.confirm(`确认删除粮仓「${row.name}」？`, '提示', { type: 'warning' })
  await warehouseApi.remove(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => {
  fetchDepots()
  fetchData()
})
</script>

<style scoped lang="scss">
.warehouse-page { padding: 0; }
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
</style>
