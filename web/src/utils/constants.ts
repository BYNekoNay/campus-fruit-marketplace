// 应用常量

export const APP_NAME = '校园水果商城'

export const APP_DESCRIPTION = '新鲜水果，直达校园'

// 分页默认值
export const DEFAULT_PAGE_SIZE = 10
export const DEFAULT_PAGE = 1

// 上传文件大小限制 (MB)
export const MAX_UPLOAD_SIZE = 5

// 订单状态
export const ORDER_STATUS = {
  PENDING_PAYMENT: '待付款',
  PENDING_DELIVERY: '待发货',
  PENDING_RECEIPT: '待收货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
} as const

// 支付方式
export const PAYMENT_METHODS = {
  WECHAT: '微信支付',
  ALIPAY: '支付宝',
} as const

// 用户角色
export const USER_ROLES = {
  USER: 'user',
  MERCHANT: 'merchant',
  ADMIN: 'admin',
} as const
