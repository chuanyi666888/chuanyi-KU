<template>
  <div class="permission-page">
    <el-card>
      <div class="toolbar">
        <span class="title">权限树</span>
        <el-button :icon="Refresh" @click="fetchData">刷新</el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="permTree"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="permissionName" label="名称" min-width="220" />
        <el-table-column prop="permissionCode" label="权限码" min-width="220" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.permissionType)" size="small">
              {{ typeText(row.permissionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="component" label="组件" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { permissionApi } from '@/api/iam'
import type { PermissionVO } from '@/types/api'

const loading = ref(false)
const permTree = ref<PermissionVO[]>([])

async function fetchData() {
  loading.value = true
  try {
    permTree.value = await permissionApi.tree()
  } finally {
    loading.value = false
  }
}

function typeText(t: number): string {
  return { 1: '菜单', 2: '按钮', 3: '接口' }[t] || '-'
}
function typeTag(t: number): any {
  return { 1: 'primary', 2: 'warning', 3: 'info' }[t] || 'info'
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.permission-page { padding: 0; }
.toolbar {
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  .title { font-size: 16px; font-weight: 600; color: var(--color-text-primary); }
}
</style>
