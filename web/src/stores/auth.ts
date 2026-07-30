import { defineStore } from 'pinia'
import { ref, computed, toRaw } from 'vue'
import type { UserInfo, LoginRequest, RegisterRequest, LoginResponse } from '@/types/user'
import {
  saveTokens,
  getAccessToken,
  getRefreshToken,
  clearAuth,
  saveUser,
  getUser,
} from '@/utils/auth'
import request from '@/utils/request'

export const useAuthStore = defineStore('auth', () => {
  // ==================== 状态 ====================
  const token = ref<string | null>(getAccessToken())
  const refreshToken = ref<string | null>(getRefreshToken())
  const user = ref<UserInfo | null>(getUser())

  // ==================== Getters ====================
  const isAuthenticated = computed(() => !!token.value)

  const isAdmin = computed(() =>
    user.value?.roles?.includes('ROLE_ADMIN') ?? false
  )

  const isMerchant = computed(() =>
    user.value?.roles?.includes('ROLE_MERCHANT') ?? false
  )

  const nickname = computed(() => user.value?.nickname ?? '')

  // ==================== Actions ====================

  /**
   * 登录
   */
  async function login(email: string, password: string) {
    const loginData: LoginRequest = { email, password }
    const res: any = await request.post('/auth/login', loginData)
    const data: LoginResponse = res.data
    if (!data) throw new Error(res.message || '登录失败')

    // 保存到 state
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    user.value = data.user

    // 持久化到 localStorage
    saveTokens(data.accessToken, data.refreshToken)
    saveUser(data.user)
  }

  /**
   * 注册
   */
  async function register(email: string, password: string, nickname: string) {
    const registerData: RegisterRequest = { email, password, nickname }
    await request.post('/auth/register', registerData)
  }

  /**
   * 注册后自动登录
   */
  async function registerAndLogin(email: string, password: string, nickname: string) {
    await register(email, password, nickname)
    await login(email, password)
  }

  /**
   * 退出登录
   */
  async function logout() {
    try {
      await request.post('/auth/logout')
    } catch {
      // 即使后端登出失败，前端也要清除状态
    }
    token.value = null
    refreshToken.value = null
    user.value = null
    clearAuth()
  }

  /**
   * 获取当前用户信息
   */
  async function fetchMe() {
    const res: any = await request.get('/auth/me')
    if (res.data) {
      user.value = res.data
      saveUser(res.data)
    }
  }

  /**
   * 从 localStorage 恢复 session（app 启动时调用）
   */
  function restoreSession() {
    token.value = getAccessToken()
    refreshToken.value = getRefreshToken()
    user.value = getUser()

    // 如果有 token 但没有 user，尝试 fetch
    if (token.value && !user.value) {
      fetchMe().catch(() => {
        // 获取失败，清除 token
        token.value = null
        refreshToken.value = null
        clearAuth()
      })
    }
  }

  /**
   * 刷新 accessToken（手动调用）
   */
  async function refreshAccessToken() {
    const currentRefreshToken = refreshToken.value
    if (!currentRefreshToken) {
      throw new Error('无刷新令牌')
    }

    const res: any = await request.post('/auth/refresh', {
      refreshToken: currentRefreshToken,
    })

    const data = res.data
    if (!data?.accessToken) {
      throw new Error('刷新令牌失败')
    }

    token.value = data.accessToken
    if (data.refreshToken) {
      refreshToken.value = data.refreshToken
    }
    saveTokens(data.accessToken, data.refreshToken || currentRefreshToken)
  }

  return {
    // 状态
    token,
    refreshToken,
    user,
    // getters
    isAuthenticated,
    isAdmin,
    isMerchant,
    nickname,
    // actions
    login,
    register,
    registerAndLogin,
    logout,
    fetchMe,
    restoreSession,
    refreshAccessToken,
  }
})
