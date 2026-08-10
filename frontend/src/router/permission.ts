import type { Router } from 'vue-router'
import NProgress from 'nprogress'
import { useUserStore } from '@/stores/user'

const APP_TITLE = import.meta.env.VITE_APP_TITLE || '智慧粮库平台'

export function setupGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    NProgress.start()
    document.title = to.meta?.title ? `${to.meta.title} - ${APP_TITLE}` : APP_TITLE

    const userStore = useUserStore()

    // 公共路由直接放行
    if (to.meta?.public) {
      // 已登录用户访问登录页，跳转首页
      if (to.name === 'Login' && userStore.isLogin) {
        next({ path: '/' })
        return
      }
      next()
      return
    }

    // 未登录
    if (!userStore.isLogin) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }

    // 已登录但未拉取用户信息
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserInfo()
        await userStore.fetchMenuTree()
      } catch (e) {
        userStore.reset()
        next({ name: 'Login', query: { redirect: to.fullPath } })
        return
      }
    }

    // 权限校验
    const requiredPerm = to.meta?.permission as string | undefined
    if (requiredPerm && !userStore.hasPermission(requiredPerm)) {
      next({ name: 'Dashboard' })
      return
    }

    next()
  })

  router.afterEach(() => {
    NProgress.done()
  })
}
