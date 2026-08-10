<template>
  <div class="user-page">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="用户名/昵称/邮箱/手机号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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
        <el-button v-permission="'system:user:create'" type="primary" :icon="Plus" @click="handleAdd">
          新增用户
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r.id" size="small" class="mr4">
              {{ r.roleName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'system:user:update'"
              type="primary"
              size="small"
              link
              @click="handleEdit(row as UserVO)"
            >编辑</el-button>
            <el-button
              v-permission="'system:user:reset'"
              type="warning"
              size="small"
              link
              @click="handleResetPwd(row as UserVO)"
            >重置密码</el-button>
            <el-button
              v-permission="'system:user:delete'"
              type="danger"
              size="small"
              link
              :disabled="(row as UserVO).username === 'admin'"
              @click="handleDelete(row as UserVO)"
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item v-if="!isEdit" label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="字母开头，3-64 位" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width: 100%">
            <el-option
              v-for="r in roleOptions"
              :key="r.id"
              :label="r.roleName"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
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

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="440px">
      <el-form ref="resetFormRef" :model="resetPwdForm" :rules="resetRules" label-width="100px">
        <el-form-item label="用户">
          <span>{{ resetPwdForm.username }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPwdForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleResetPwdSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { userApi, roleApi } from '@/api/iam'
import { formatDateTime } from '@/utils/format'
import type { UserVO, RoleVO } from '@/types/api'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<UserVO[]>([])
const total = ref(0)
const roleOptions = ref<RoleVO[]>([])

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  status: undefined as number | undefined
})

async function fetchData() {
  loading.value = true
  try {
    const res = await userApi.page(query)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function fetchRoles() {
  roleOptions.value = await roleApi.all()
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
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  roleIds: [] as number[],
  status: 1,
  remark: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]{2,63}$/, message: '字母开头，3-64 位字母数字下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

function resetForm() {
  form.id = undefined
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.email = ''
  form.phone = ''
  form.roleIds = []
  form.status = 1
  form.remark = ''
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: UserVO) {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  resetForm()
  form.id = row.id
  form.username = row.username
  form.nickname = row.nickname
  form.email = row.email || ''
  form.phone = row.phone || ''
  form.roleIds = row.roles?.map((r) => r.id) || []
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
        await userApi.update({
          id: form.id!,
          nickname: form.nickname,
          email: form.email,
          phone: form.phone,
          roleIds: form.roleIds,
          status: form.status,
          remark: form.remark
        })
        ElMessage.success('修改成功')
      } else {
        await userApi.create({
          username: form.username,
          password: form.password,
          nickname: form.nickname,
          email: form.email,
          phone: form.phone,
          roleIds: form.roleIds,
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

// 删除
async function handleDelete(row: UserVO) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await userApi.remove(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

// 重置密码
const resetPwdVisible = ref(false)
const resetFormRef = ref<FormInstance>()
const resetPwdForm = reactive({
  userId: undefined as number | undefined,
  username: '',
  newPassword: ''
})
const resetRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32', trigger: 'blur' }
  ]
}

function handleResetPwd(row: UserVO) {
  resetPwdForm.userId = row.id
  resetPwdForm.username = row.username
  resetPwdForm.newPassword = ''
  resetPwdVisible.value = true
}

async function handleResetPwdSubmit() {
  if (!resetFormRef.value) return
  await resetFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await userApi.resetPassword(resetPwdForm.userId!, resetPwdForm.newPassword)
      ElMessage.success('密码已重置')
      resetPwdVisible.value = false
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  fetchRoles()
  fetchData()
})
</script>

<style scoped lang="scss">
.user-page { padding: 0; }
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
.mr4 { margin-right: 4px; }
</style>
