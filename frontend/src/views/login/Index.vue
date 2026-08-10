<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="grid-overlay"></div>
      <div class="glow-orb orb-1"></div>
      <div class="glow-orb orb-2"></div>
    </div>

    <div class="login-card">
      <div class="login-header">
        <div class="logo">
          <el-icon :size="40" color="#4fc3f7"><Monitor /></el-icon>
        </div>
        <h1 class="title">智慧粮库</h1>
        <p class="subtitle">国产化综合运维与业务管理平台</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="tips">
        <el-divider><span class="demo-accounts">演示账号</span></el-divider>
        <div class="account-list">
          <div class="account-item" @click="fillAccount('admin')">
            <el-tag type="danger" size="small">管理员</el-tag>
            <span>admin / Admin@123456</span>
          </div>
          <div class="account-item" @click="fillAccount('keeper01')">
            <el-tag type="warning" size="small">库管员</el-tag>
            <span>keeper01 / Admin@123456</span>
          </div>
          <div class="account-item" @click="fillAccount('auditor01')">
            <el-tag type="info" size="small">审计员</el-tag>
            <span>auditor01 / Admin@123456</span>
          </div>
        </div>
      </div>
    </div>

    <div class="footer">
      © 2026 智慧粮库国产化综合运维与业务管理平台 · 适配银河麒麟 V11
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Monitor } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32', trigger: 'blur' }
  ]
}

function fillAccount(username: string) {
  form.username = username
  form.password = 'Admin@123456'
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({
        username: form.username,
        password: form.password
      })
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.replace(redirect)
    } catch (e) {
      // 错误已在拦截器中提示
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: var(--color-bg-primary);
}

.login-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(30, 136, 229, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(30, 136, 229, 0.05) 1px, transparent 1px);
  background-size: 40px 40px;
}

.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  &.orb-1 {
    width: 500px;
    height: 500px;
    background: var(--color-primary);
    top: -100px;
    left: -100px;
  }
  &.orb-2 {
    width: 400px;
    height: 400px;
    background: var(--color-primary-light);
    bottom: -100px;
    right: -100px;
  }
}

.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 40px;
  background: rgba(13, 33, 55, 0.85);
  backdrop-filter: blur(20px);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
  .logo { margin-bottom: 12px; }
  .title {
    font-size: 28px;
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0 0 8px 0;
    letter-spacing: 4px;
  }
  .subtitle {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin: 0;
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 8px;
}

.tips {
  margin-top: 20px;
  .demo-accounts {
    font-size: 12px;
    color: var(--color-text-tertiary);
  }
  .account-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .account-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: var(--color-text-secondary);
    cursor: pointer;
    padding: 6px 8px;
    border-radius: 4px;
    transition: background 0.2s;
    &:hover { background: var(--color-bg-elevated); }
  }
}

.footer {
  position: absolute;
  bottom: 20px;
  width: 100%;
  text-align: center;
  font-size: 12px;
  color: var(--color-text-tertiary);
  z-index: 1;
}

:deep(.el-divider__text) {
  background-color: var(--color-bg-card);
}
</style>
