<template>
  <div class="role-page">
    <el-card class="search-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="角色编码/名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <div class="toolbar">
        <el-button v-permission="'system:role:create'" type="primary" :icon="Plus" @click="handleAdd">
          新增角色
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="roleCode" label="角色编码" min-width="160" />
        <el-table-column prop="roleName" label="角色名称" min-width="120" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="userCount" label="用户数" width="90" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内置" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.builtin === 1" type="info" size="small">内置</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:role:assign'" type="primary" size="small" link @click="handleAssign(row as RoleVO)">
              分配权限
            </el-button>
            <el-button v-permission="'system:role:update'" type="warning" size="small" link @click="handleEdit(row as RoleVO)">
              编辑
            </el-button>
            <el-button
              v-permission="'system:role:delete'"
              type="danger"
              size="small"
              link
              :disabled="(row as RoleVO).builtin === 1"
              @click="handleDelete(row as RoleVO)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item v-if="!isEdit" label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="ROLE_ 开头大写" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="assignVisible" title="分配权限" width="600px">
      <div class="assign-header">
        <span>角色：<b>{{ currentRole?.roleName }}</b></span>
        <el-checkbox v-model="expandAll" @change="handleExpandAll">展开/折叠</el-checkbox>
      </div>
      <el-tree
        ref="treeRef"
        :data="permTree"
        show-checkbox
        node-key="id"
        :default-expand-all="expandAll"
        :props="{ label: 'permissionName', children: 'children' }"
        style="max-height: 400px; overflow: auto"
      />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAssignSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { roleApi, permissionApi } from '@/api/iam'
import { formatDateTime } from '@/utils/format'
import type { RoleVO, PermissionVO } from '@/types/api'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<RoleVO[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 20,
  keyword: ''
})

async function fetchData() {
  loading.value = true
  try {
    const res = await roleApi.page(query)
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
  roleCode: '',
  roleName: '',
  description: '',
  status: 1
})

const rules: FormRules = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^ROLE_[A-Z][A-Z0-9_]{2,63}$/, message: 'ROLE_ 开头大写字母数字下划线', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增角色'
  form.id = undefined
  form.roleCode = ''
  form.roleName = ''
  form.description = ''
  form.status = 1
  dialogVisible.value = true
}

function handleEdit(row: RoleVO) {
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  form.id = row.id
  form.roleCode = row.roleCode
  form.roleName = row.roleName
  form.description = row.description || ''
  form.status = row.status
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await roleApi.update({
          id: form.id!,
          roleName: form.roleName,
          description: form.description,
          status: form.status
        })
        ElMessage.success('修改成功')
      } else {
        await roleApi.create({
          roleCode: form.roleCode,
          roleName: form.roleName,
          description: form.description
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

async function handleDelete(row: RoleVO) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await roleApi.remove(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

// 分配权限
const assignVisible = ref(false)
const currentRole = ref<RoleVO | null>(null)
const permTree = ref<PermissionVO[]>([])
const treeRef = ref()
const expandAll = ref(true)

async function handleAssign(row: RoleVO) {
  currentRole.value = row
  assignVisible.value = true
  if (permTree.value.length === 0) {
    permTree.value = await permissionApi.tree()
  }
  const ids = await roleApi.permissionIds(row.id)
  await nextTick()
  treeRef.value?.setCheckedKeys(ids, false)
}

function handleExpandAll(val: any) {
  // el-tree 没有 setExpandAll API，简单刷新即可
}

async function handleAssignSubmit() {
  if (!currentRole.value) return
  submitting.value = true
  try {
    const checked = treeRef.value.getCheckedKeys()
    const halfChecked = treeRef.value.getHalfCheckedKeys()
    const all = [...checked, ...halfChecked]
    await roleApi.assignPermissions(currentRole.value.id, all)
    ElMessage.success('权限分配成功')
    assignVisible.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.role-page { padding: 0; }
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
.assign-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  color: var(--color-text-secondary);
}
</style>
