// API 响应类型

/** Jiangke 框架标准响应格式 */
export interface ApiResponse<T = unknown> {
  errorCode: string
  message: string
  data?: T
  timestamp: string
  traceId: string
}

/** 分页查询参数 */
export interface PageQuery {
  page: number
  pageSize: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}
