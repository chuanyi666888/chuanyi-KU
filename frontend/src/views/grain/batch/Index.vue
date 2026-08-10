<template>
  <div class="batch-page">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="批次号/品种/产地"
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
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="已封批" :value="0" />
            <el-option label="在储" :value="1" />
            <el-option label="已出清" :value="2" />
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
        <el-button v-permission="'grain:batch:create'" type="primary" :icon="Plus" @click="handleAdd">
          新增批次
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="batchNo" label="批次号" min-width="160" />
        <el-table-column label="粮食品种" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="grainTypeTag(row.grainType)" size="small">
              {{ grainTypeText(row.grainType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="variety" label="品种" min-width="100" />
        <el-table-column prop="grade" label="等级" width="80" align="center" />
        <el-table-column prop="origin" label="产地" min-width="120" />
        <el-table-column prop="productionYear" label="生产年份" width="100" align="center" />
        <el-table-column prop="moisture" label="水分(%)" width="100" align="right" />
        <el-table-column prop="stockTons" label="库存(吨)" width="110" align="right" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="batchStatusTag(row.status)" size="small">
              {{ batchStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'grain:batch:update'"
              type="primary"
              size="small"
              link
              @click="handleEdit(row as BatchVO)"
            >编辑</el-button>
            <el-button
              v-permission="'grain:batch:delete'"
              type="danger"
              size="small"
              link
              :disabled="(row as BatchVO).status === 1"
              @click="handleDelete(row as BatchVO)"
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
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-if="!isEdit" label="批次号" prop="batchNo">
          <el-input v-model="form.batchNo" placeholder="字母/数字组合" />
        </el-form-item>
        <el-form-item label="粮食品种" prop="grainType">
          <el-select v-model="form.grainType" placeholder="请选择品种" style="width: 100%">
            <el-option label="小麦" value="WHEAT" />
            <el-option label="玉米" value="CORN" />
            <el-option label="稻谷" value="RICE" />
            <el-option label="大豆" value="SOYBEAN" />
            <el-option label="高粱" value="SORGHUM" />
          </el-select>
        </el-form-item>
        <el-form-item label="品种">
          <el-input v-model="form.variety" placeholder="如：硬质白麦" />
        </el-form-item>
        <el-form-item label="等级" prop="grade">
          <el-select v-model="form.grade" placeholder="请选择等级" clearable style="width: 100%">
            <el-option label="一等" value="1" />
            <el-option label="二等" value="2" />
            <el-option label="三等" value="3" />
            <el-option label="四等" value="4" />
            <el-option label="五等" value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="产地">
          <el-input v-model="form.origin" />
        </el-form-item>
        <el-form-item label="生产年份" prop="productionYear">
          <el-input-number v-model="form.productionYear" :min="1990" :max="new Date().getFullYear()" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="水分(%)">
          <el-input-number v-model="form.moisture" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="杂质(%)">
          <el-input-number v-model="form.impurity" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="容重(g/L)">
          <el-input-number v-model="form.testWeight" :min="0" :precision="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="已封批" :value="0" />
            <el-option label="在储" :value="1" />
            <el-option label="已出清" :value="2" />
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
import { batchApi } from '@/api/grain'
import { formatDateTime } from '@/utils/format'
import type { BatchVO } from '@/types/grain'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<BatchVO[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  grainType: undefined as string | undefined,
  status: undefined as number | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const res = await batchApi.page(query)
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
  query.keyword = ''
  query.grainType = undefined
  query.status = undefined
  query.page = 1
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

// 批次状态: 0已封批/info, 1在储/success, 2已出清/danger
function batchStatusText(status: number): string {
  const map: Record<number, string> = { 0: '已封批', 1: '在储', 2: '已出清' }
  return map[status] ?? String(status)
}
function batchStatusTag(status: number): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'info',
    1: 'success',
    2: 'danger'
  }
  return map[status] ?? 'info'
}

// 新增/编辑
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  batchNo: '',
  grainType: 'WHEAT',
  variety: '',
  grade: '',
  origin: '',
  productionYear: new Date().getFullYear(),
  moisture: undefined as number | undefined,
  impurity: undefined as number | undefined,
  testWeight: undefined as number | undefined,
  status: 0,
  remark: ''
})

const rules: FormRules = {
  batchNo: [{ required: true, message: '请输入批次号', trigger: 'blur' }],
  grainType: [{ required: true, message: '请选择粮食品种', trigger: 'change' }]
}

function resetForm() {
  form.id = undefined
  form.batchNo = ''
  form.grainType = 'WHEAT'
  form.variety = ''
  form.grade = ''
  form.origin = ''
  form.productionYear = new Date().getFullYear()
  form.moisture = undefined
  form.impurity = undefined
  form.testWeight = undefined
  form.status = 0
  form.remark = ''
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增批次'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: BatchVO) {
  isEdit.value = true
  dialogTitle.value = '编辑批次'
  resetForm()
  form.id = row.id
  form.batchNo = row.batchNo
  form.grainType = row.grainType
  form.variety = row.variety || ''
  form.grade = row.grade || ''
  form.origin = row.origin || ''
  form.productionYear = row.productionYear || new Date().getFullYear()
  form.moisture = row.moisture
  form.impurity = row.impurity
  form.testWeight = row.testWeight
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
        await batchApi.update({
          id: form.id!,
          grainType: form.grainType,
          variety: form.variety,
          grade: form.grade,
          origin: form.origin,
          productionYear: form.productionYear,
          moisture: form.moisture,
          impurity: form.impurity,
          testWeight: form.testWeight,
          status: form.status,
          remark: form.remark
        })
        ElMessage.success('修改成功')
      } else {
        await batchApi.create({
          batchNo: form.batchNo,
          grainType: form.grainType,
          variety: form.variety,
          grade: form.grade,
          origin: form.origin,
          productionYear: form.productionYear,
          moisture: form.moisture,
          impurity: form.impurity,
          testWeight: form.testWeight,
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

async function handleDelete(row: BatchVO) {
  if (row.status === 1) {
    ElMessage.warning('在储批次不可删除')
    return
  }
  await ElMessageBox.confirm(`确认删除批次「${row.batchNo}」？`, '提示', { type: 'warning' })
  await batchApi.remove(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.batch-page { padding: 0; }
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
