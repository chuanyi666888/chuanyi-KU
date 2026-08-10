<template>
  <div class="outbound-page">
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
          >
            <el-option
              v-for="d in depotOptions"
              :key="d.id"
              :label="d.name"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待审" :value="0" />
            <el-option label="已审" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="单号/批次号"
            clearable
            @keyup.enter="handleSearch"
          />
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
        <el-button v-permission="'grain:outbound:create'" type="primary" :icon="Plus" @click="handleAdd">
          新增出库单
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="orderNo" label="出库单号" min-width="160" />
        <el-table-column prop="batchNo" label="批次号" min-width="140" />
        <el-table-column prop="depotName" label="粮库" min-width="120" />
        <el-table-column prop="warehouseName" label="粮仓" min-width="120" />
        <el-table-column prop="weightTons" label="重量(吨)" width="100" align="right" />
        <el-table-column label="出库类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="outboundTypeTag(row.outboundType)" size="small">
              {{ outboundTypeText(row.outboundType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="customer" label="客户" min-width="140" show-overflow-tooltip />
        <el-table-column prop="vehicleNo" label="车牌号" width="110" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.status)" size="small">
              {{ orderStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="outboundAt" label="出库时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.outboundAt) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              v-permission="'grain:outbound:audit'"
              type="warning"
              size="small"
              link
              @click="handleAudit(row as OutboundOrderVO)"
            >审核</el-button>
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

    <!-- 新增弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增出库单"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="批次号" prop="batchNo">
          <el-input v-model="form.batchNo" placeholder="请输入批次号" />
        </el-form-item>
        <el-form-item label="所属粮库" prop="depotId">
          <el-select
            v-model="form.depotId"
            placeholder="请选择粮库"
            filterable
            style="width: 100%"
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
        <el-form-item label="所属粮仓" prop="warehouseId">
          <el-select
            v-model="form.warehouseId"
            placeholder="请选择粮仓"
            filterable
            style="width: 100%"
            :disabled="!form.depotId"
          >
            <el-option
              v-for="w in warehouseOptions"
              :key="w.id"
              :label="w.name"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="重量(吨)" prop="weightTons">
          <el-input-number v-model="form.weightTons" :min="0" :precision="3" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="出库类型" prop="outboundType">
          <el-select v-model="form.outboundType" placeholder="请选择类型" style="width: 100%">
            <el-option label="销售" value="SALE" />
            <el-option label="调拨" value="TRANSFER" />
            <el-option label="损耗" value="LOSS" />
            <el-option label="移库" value="RELOCATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户">
          <el-input v-model="form.customer" />
        </el-form-item>
        <el-form-item label="车牌号">
          <el-input v-model="form.vehicleNo" />
        </el-form-item>
        <el-form-item label="出库时间" prop="outboundAt">
          <el-date-picker
            v-model="form.outboundAt"
            type="datetime"
            placeholder="选择出库时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
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

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="出库单审核" width="480px" :close-on-click-modal="false">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="100px">
        <el-form-item label="出库单号">
          <span>{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="批次号">
          <span>{{ currentOrder?.batchNo }}</span>
        </el-form-item>
        <el-form-item label="重量(吨)">
          <span>{{ currentOrder?.weightTons }}</span>
        </el-form-item>
        <el-form-item label="审核结果" prop="auditResult">
          <el-radio-group v-model="auditForm.auditResult">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAuditSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { outboundApi, depotApi, warehouseApi } from '@/api/grain'
import { formatDateTime } from '@/utils/format'
import type { OutboundOrderVO, DepotVO, WarehouseVO } from '@/types/grain'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<OutboundOrderVO[]>([])
const total = ref(0)
const depotOptions = ref<DepotVO[]>([])
const warehouseOptions = ref<WarehouseVO[]>([])

const query = reactive({
  page: 1,
  size: 20,
  depotId: undefined as number | undefined,
  status: undefined as number | undefined,
  keyword: ''
})

async function fetchData() {
  loading.value = true
  try {
    const res = await outboundApi.page(query)
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
  query.status = undefined
  query.keyword = ''
  query.page = 1
  fetchData()
}

// 出库类型映射: SALE销售/TRANSFER调拨/LOSS损耗/RELOCATE移库
function outboundTypeText(type: string): string {
  const map: Record<string, string> = {
    SALE: '销售',
    TRANSFER: '调拨',
    LOSS: '损耗',
    RELOCATE: '移库'
  }
  return map[type] || type
}
function outboundTypeTag(type: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    SALE: 'success',
    TRANSFER: 'warning',
    LOSS: 'danger',
    RELOCATE: 'info'
  }
  return map[type] || 'info'
}

// 状态映射: 0待审/warning, 1已审/success, 2已驳回/danger
function orderStatusText(status: number): string {
  const map: Record<number, string> = { 0: '待审', 1: '已审', 2: '已驳回' }
  return map[status] ?? String(status)
}
function orderStatusTag(status: number): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return map[status] ?? 'info'
}

// 新增
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  batchNo: '',
  depotId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  weightTons: undefined as number | undefined,
  outboundType: 'SALE',
  customer: '',
  vehicleNo: '',
  outboundAt: '',
  remark: ''
})

const rules: FormRules = {
  batchNo: [{ required: true, message: '请输入批次号', trigger: 'blur' }],
  depotId: [{ required: true, message: '请选择粮库', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择粮仓', trigger: 'change' }],
  weightTons: [{ required: true, message: '请输入重量', trigger: 'blur' }],
  outboundType: [{ required: true, message: '请选择出库类型', trigger: 'change' }],
  outboundAt: [{ required: true, message: '请选择出库时间', trigger: 'change' }]
}

async function handleDepotChange(depotId: number) {
  form.warehouseId = undefined
  if (depotId) {
    warehouseOptions.value = await warehouseApi.listByDepot(depotId)
  } else {
    warehouseOptions.value = []
  }
}

function resetForm() {
  form.batchNo = ''
  form.depotId = undefined
  form.warehouseId = undefined
  form.weightTons = undefined
  form.outboundType = 'SALE'
  form.customer = ''
  form.vehicleNo = ''
  form.outboundAt = ''
  form.remark = ''
  warehouseOptions.value = []
}

function handleAdd() {
  resetForm()
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await outboundApi.create({
        batchNo: form.batchNo,
        depotId: form.depotId,
        warehouseId: form.warehouseId,
        weightTons: form.weightTons,
        outboundType: form.outboundType,
        customer: form.customer,
        vehicleNo: form.vehicleNo,
        outboundAt: form.outboundAt,
        remark: form.remark
      })
      ElMessage.success('新增成功')
      dialogVisible.value = false
      fetchData()
    } finally {
      submitting.value = false
    }
  })
}

// 审核
const auditVisible = ref(false)
const auditFormRef = ref<FormInstance>()
const currentOrder = ref<OutboundOrderVO | null>(null)
const auditForm = reactive({
  id: undefined as number | undefined,
  auditResult: 1,
  auditRemark: ''
})
const auditRules: FormRules = {
  auditResult: [{ required: true, message: '请选择审核结果', trigger: 'change' }]
}

function handleAudit(row: OutboundOrderVO) {
  currentOrder.value = row
  auditForm.id = row.id
  auditForm.auditResult = 1
  auditForm.auditRemark = ''
  auditVisible.value = true
}

async function handleAuditSubmit() {
  if (!auditFormRef.value) return
  await auditFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await outboundApi.audit({
        id: auditForm.id!,
        auditResult: auditForm.auditResult,
        auditRemark: auditForm.auditRemark
      })
      ElMessage.success('审核完成')
      auditVisible.value = false
      fetchData()
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  fetchDepots()
  fetchData()
})
</script>

<style scoped lang="scss">
.outbound-page { padding: 0; }
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
