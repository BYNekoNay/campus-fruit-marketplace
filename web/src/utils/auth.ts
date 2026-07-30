import type { UserInfo } from '@/types/user'

const ACCESS_TOKEN_KEY = 'fruit_market_access_token'
const REFRESH_TOKEN_KEY = 'fruit_market_refresh_token'
const USER_KEY = 'fruit_market_user'

// Access Token
export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(token: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

// Refresh Token
export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

// 保存完整的 token 信息
export function saveTokens(accessToken: string, refreshToken: string): void {
  setAccessToken(accessToken)
  setRefreshToken(refreshToken)
}

// 向后兼容：getToken 返回 accessToken
export function getToken(): string | null {
  return getAccessToken()
}

// 清除所有认证数据
export function clearAuth(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

// 用户信息（localStorage 缓存）
export function getUser(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    return null
  }
}

export function saveUser(user: UserInfo): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

// 角色判断
export function hasRole(role: string): boolean {
  const user = getUser()
  if (!user || !user.roles) return false
  return user.roles.includes(role)
}

export function hasAnyRole(...roles: string[]): boolean {
  return roles.some((r) => hasRole(r))
}

export function isAdmin(): boolean {
  return hasRole('ROLE_ADMIN')
}

export function isMerchant(): boolean {
  return hasRole('ROLE_MERCHANT')
}
