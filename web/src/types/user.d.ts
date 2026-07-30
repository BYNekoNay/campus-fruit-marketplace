// 用户相关类型

export interface UserInfo {
  id: number
  email: string
  nickname: string
  status: 'ACTIVE' | 'FROZEN' | 'DELETED'
  roles: string[]
  createdAt: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

export interface RegisterRequest {
  email: string
  password: string
  nickname: string
}
