import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types/api'
import { authApi, type LoginParams } from '@/api/auth'
import { permissionApi } from '@/api/iam'
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from '@/api/request'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getAccessToken())
  const refreshToken = ref<string>(getRefreshToken())
  const userInfo = ref<UserInfo | null>(null)
  const permissions = ref<string[]>([])
  const roles = ref<string[]>([])
  const menuTree = ref<any[]>([])

  const isLogin = computed(() => !!token.value)

  async function login(params: LoginParams) {
    const res = await authApi.login(params)
    token.value = res.accessToken
    refreshToken.value = res.refreshToken
    userInfo.value = res.userInfo
    roles.value = res.userInfo.roles || []
    permissions.value = res.userInfo.permissions || []
    setTokens(res.accessToken, res.refreshToken)
    await fetchMenuTree()
    return res
  }

  async function fetchUserInfo() {
    const info = await authApi.userInfo()
    userInfo.value = info
    roles.value = info.roles || []
    permissions.value = info.permissions || []
    return info
  }

  async function fetchMenuTree() {
    try {
      menuTree.value = await permissionApi.menuTree()
    } catch (e) {
      menuTree.value = []
    }
  }

  function hasPermission(code: string): boolean {
    if (roles.value.includes('ROLE_ADMIN')) return true
    return permissions.value.includes(code)
  }

  function hasAnyPermission(codes: string[]): boolean {
    if (roles.value.includes('ROLE_ADMIN')) return true
    if (!codes || codes.length === 0) return true
    return codes.some((c) => permissions.value.includes(c))
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch (e) {
      // 忽略
    }
    reset()
  }

  function reset() {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    permissions.value = []
    roles.value = []
    menuTree.value = []
    clearTokens()
  }

  return {
    token,
    refreshToken,
    userInfo,
    permissions,
    roles,
    menuTree,
    isLogin,
    login,
    fetchUserInfo,
    fetchMenuTree,
    hasPermission,
    hasAnyPermission,
    logout,
    reset
  }
})
