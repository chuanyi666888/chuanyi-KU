<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="logo">
        <el-icon :size="28" color="#4fc3f7"><Monitor /></el-icon>
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">智慧粮库</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="appStore.sidebarCollapsed"
          :collapse-transition="false"
          router
          unique-opened
        >
          <template v-for="route in fullMenu" :key="route.path">
            <!-- 一级菜单（无子菜单） -->
            <el-menu-item
              v-if="!route.children || route.children.length === 0"
              :index="route.path"
            >
              <el-icon v-if="route.meta?.icon">
                <component :is="route.meta.icon" />
              </el-icon>
              <template #title>{{ route.meta?.title }}</template>
            </el-menu-item>

            <!-- 一级菜单（有子菜单） -->
            <el-sub-menu v-else :index="route.path">
              <template #title>
                <el-icon v-if="route.meta?.icon">
                  <component :is="route.meta.icon" />
                </el-icon>
                <span>{{ route.meta?.title }}</span>
              </template>
              <el-menu-item
                v-for="child in visibleChildren(route)"
                :key="child.path"
                :index="`${route.path}/${child.path}`"
              >
                <el-icon v-if="child.meta?.icon">
                  <component :is="child.meta.icon" />
                </el-icon>
                <template #title>{{ child.meta?.title }}</template>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-scrollbar>
    </aside>

    <!-- 主区域 -->
    <div class="main">
      <header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta?.title">
              {{ route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" class="avatar">
                {{ avatarText }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Monitor, Fold, Expand, ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { staticRoutes } from '@/router'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const menuRoutes = computed(() => {
  return staticRoutes.filter((r) => {
    if (r.meta?.public) return false
    if (r.path === '/') return false
    const perm = r.meta?.permission as string | undefined
    if (perm && !userStore.hasPermission(perm)) return false
    return true
  })
})

// 根路由下的 dashboard 单独加入菜单
const dashboardRoute = computed(() => staticRoutes.find((r) => r.path === '/'))
const fullMenu = computed(() => {
  const list = [...menuRoutes.value]
  if (dashboardRoute.value) {
    list.unshift(dashboardRoute.value)
  }
  return list
})

const activeMenu = computed(() => route.path)

const avatarText = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || 'U'
  return name.charAt(0).toUpperCase()
})

function visibleChildren(route: any) {
  if (!route.children) return []
  return route.children.filter((c: any) => {
    const perm = c.meta?.permission as string | undefined
    if (perm && !userStore.hasPermission(perm)) return false
    return true
  })
}

async function handleCommand(cmd: string) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', {
      type: 'warning'
    })
    await userStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  width: 100vw;
  height: 100vh;
  background: var(--color-bg-primary);
}

.sidebar {
  width: 220px;
  background: var(--color-bg-secondary);
  border-right: 1px solid var(--color-border);
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
  &.collapsed { width: 64px; }

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    border-bottom: 1px solid var(--color-border);
    .logo-text {
      font-size: 18px;
      font-weight: 600;
      color: var(--color-text-primary);
      letter-spacing: 2px;
    }
  }
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: 60px;
  background: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      color: var(--color-text-secondary);
      &:hover { color: var(--color-primary-light); }
    }
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 6px 12px;
    border-radius: 4px;
    &:hover { background: var(--color-bg-elevated); }
    .avatar {
      background: var(--color-primary);
      color: #fff;
    }
    .username {
      color: var(--color-text-primary);
      font-size: 14px;
    }
  }
}

.content {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

:deep(.el-menu) {
  border-right: none;
}
</style>
