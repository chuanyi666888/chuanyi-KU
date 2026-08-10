<template>
  <div class="profile-page">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <div class="user-card">
            <el-avatar :size="80" class="avatar">
              {{ avatarText }}
            </el-avatar>
            <h3>{{ userStore.userInfo?.nickname }}</h3>
            <p>{{ userStore.userInfo?.username }}</p>
            <div class="roles">
              <el-tag v-for="r in userStore.roles" :key="r" size="small" class="role-tag">
                {{ r }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>修改密码</span>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="form.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="form.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSubmit">
                提交
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
          <el-alert type="warning" :closable="false">
            修改密码后需要重新登录
          </el-alert>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const avatarText = computed(() => {
  const name = userStore.userInfo?.nickname || 'U'
  return name.charAt(0).toUpperCase()
})

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authApi.changePassword({
        oldPassword: form.oldPassword,
        newPassword: form.newPassword
      })
      ElMessage.success('密码修改成功，请重新登录')
      await userStore.logout()
      router.replace('/login')
    } catch (e) {
      // 已在拦截器提示
    } finally {
      loading.value = false
    }
  })
}

function resetForm() {
  formRef.value?.resetFields()
}
</script>

<style scoped lang="scss">
.profile-page { padding: 0; }
.user-card {
  text-align: center;
  padding: 20px 0;
  .avatar {
    background: var(--color-primary);
    color: #fff;
    font-size: 32px;
    margin-bottom: 12px;
  }
  h3 { margin: 8px 0 4px; color: var(--color-text-primary); }
  p { margin: 0 0 16px; color: var(--color-text-secondary); font-size: 13px; }
  .roles {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    justify-content: center;
    .role-tag { margin: 0; }
  }
}
</style>
