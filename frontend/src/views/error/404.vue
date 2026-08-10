<template>
  <div class="not-found">
    <div class="content">
      <h1>404</h1>
      <p>页面不存在</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped lang="scss">
.not-found {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-primary);
  .content {
    text-align: center;
    h1 {
      font-size: 120px;
      margin: 0;
      color: var(--color-primary);
      text-shadow: 0 0 40px rgba(30, 136, 229, 0.5);
    }
    p {
      color: var(--color-text-secondary);
      margin: 16px 0 24px;
    }
  }
}
</style>
