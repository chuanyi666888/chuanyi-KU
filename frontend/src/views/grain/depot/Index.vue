<template>
  <div class="depot-page">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="编码/名称/管理员"
            clearable
            @keyup.enter="handleSearch"
          />
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
        <el-button v-permission="'grain:depot:create'" type="primary" :icon="Plus" @click="handleAdd">
          新增粮库
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="code" label="粮库编码" min-width="120" />
        <el-table-column prop="name" label="粮库名称" min-width="160" />
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="capacityTons" label="库容(吨)" width="110" align="right" />
        <el-table-column prop="manager" label="负责人" width="100" />
        <el-table-column prop="phone" label="联系电话" min-width="120" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'grain:depot:update'"
              type="primary"
              size="small"
              link
              @click="handleEdit(row as DepotVO)"
            >编辑</el-button>
            <el-button
              v-permission="'grain:depot:delete'"
              type="danger"
              size="small"
              link
              @click="handleDelete(row as DepotVO)"
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-if="!isEdit" label="粮库编码" prop="code">
          <el-input v-model="form.code" placeholder="字母/数字组合" />
        </el-form-item>
        <el-form-item label="粮库名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="省">
          <el-input v-model="form.province" placeholder="省" />
        </el-form-item>
        <el-form-item label="市">
          <el-input v-model="form.city" placeholder="市" />
        </el-form-item>
        <el-form-item label="区/县">
          <el-input v-model="form.district" placeholder="区/县" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="form.address" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="库容(吨)" prop="capacityTons">
          <el-input-number v-model="form.capacityTons" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.manager" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" />
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
import { depotApi } from '@/api/grain'
import { formatDateTime } from '@/utils/format'
import type { DepotVO } from '@/types/grain'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<DepotVO[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  status: undefined as number | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const res = await depotApi.page(query)
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
  query.status = undefined
  query.page = 1
  fetchData()
}

// 新增/编辑
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  code: '',
  name: '',
  address: '',
  province: '',
  city: '',
  district: '',
  capacityTons: undefined as number | undefined,
  manager: '',
  phone: '',
  status: 1,
  remark: ''
})

const rules: FormRules = {
  code: [{ required: true, message: '请输入粮库编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入粮库名称', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

function resetForm() {
  form.id = undefined
  form.code = ''
  form.name = ''
  form.address = ''
  form.province = ''
  form.city = ''
  form.district = ''
  form.capacityTons = undefined
  form.manager = ''
  form.phone = ''
  form.status = 1
  form.remark = ''
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增粮库'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: DepotVO) {
  isEdit.value = true
  dialogTitle.value = '编辑粮库'
  resetForm()
  form.id = row.id
  form.code = row.code
  form.name = row.name
  form.address = row.address || ''
  form.province = row.province || ''
  form.city = row.city || ''
  form.district = row.district || ''
  form.capacityTons = row.capacityTons
  form.manager = row.manager || ''
  form.phone = row.phone || ''
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
        await depotApi.update({
          id: form.id!,
          name: form.name,
          address: form.address,
          province: form.province,
          city: form.city,
          district: form.district,
          capacityTons: form.capacityTons,
          manager: form.manager,
          phone: form.phone,
          status: form.status,
          remark: form.remark
        })
        ElMessage.success('修改成功')
      } else {
        await depotApi.create({
          code: form.code,
          name: form.name,
          address: form.address,
          province: form.province,
          city: form.city,
          district: form.district,
          capacityTons: form.capacityTons,
          manager: form.manager,
          phone: form.phone,
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

async function handleDelete(row: DepotVO) {
  await ElMessageBox.confirm(`确认删除粮库「${row.name}」？`, '提示', { type: 'warning' })
  await depotApi.remove(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.depot-page { padding: 0; }
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
