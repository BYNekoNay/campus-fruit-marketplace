/**
 * 订单工具函数
 */
import { centsToYuan, formatDateTime } from './format'

/** 订单状态中文文案 */
export function formatOrderStatus(status: OrderStatus): string {
  const map: Record<OrderStatus, string> = {
    PENDING_RESERVATION: '待预约',
    PENDING_STORE_CONFIRMATION: '待商家确认',
    ACCEPTED: '已接单',
    READY_FOR_PICKUP: '待自取',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REJECTED: '已拒绝',
    EXPIRED: '已过期',
    NO_SHOW_PENDING: '未取待处理',
  }
  return map[status] || status
}

/** 支付状态中文文案 */
export function formatPaymentStatus(status: string): string {
  const map: Record<string, string> = {
    UNPAID: '到店支付',
    PAID_AT_PICKUP: '已到店支付',
  }
  return map[status] || status
}

/** 获取 el-tag 类型 */
export function getStatusTagType(status: OrderStatus): 'info' | 'warning' | 'primary' | 'success' | 'danger' | '' {
  const map: Record<OrderStatus, 'info' | 'warning' | 'primary' | 'success' | 'danger' | ''> = {
    PENDING_RESERVATION: 'info',
    PENDING_STORE_CONFIRMATION: 'warning',
    ACCEPTED: 'primary',
    READY_FOR_PICKUP: 'success',
    COMPLETED: '',
    CANCELLED: 'danger',
    REJECTED: 'danger',
    EXPIRED: 'danger',
    NO_SHOW_PENDING: 'danger',
  }
  return map[status] || 'info'
}

/** 状态是否处于终态（不可操作） */
export function isOrderFinalStatus(status: OrderStatus): boolean {
  return ['COMPLETED', 'CANCELLED', 'REJECTED', 'EXPIRED'].includes(status)
}

/** 订单是否可取消 */
export function canCancelOrder(status: OrderStatus): boolean {
  return ['PENDING_RESERVATION', 'PENDING_STORE_CONFIRMATION', 'ACCEPTED'].includes(status)
}

/** 获取步骤列表用于 el-steps */
export function getOrderSteps(status: OrderStatus): { title: string; description: string }[] {
  const allSteps = [
    { title: '已下单', description: '订单已提交' },
    { title: '商家确认', description: '商家确认接单' },
    { title: '备货完成', description: '商品准备完毕' },
    { title: '已自取', description: '用户已自取' },
  ]

  // 失败状态：显示中断
  const failedStatuses: OrderStatus[] = ['CANCELLED', 'REJECTED', 'EXPIRED', 'NO_SHOW_PENDING']
  if (failedStatuses.includes(status)) {
    const failedIndex = getActiveStepIndex(status)
    return allSteps.map((step, index) => {
      if (index > failedIndex) {
        return { title: step.title, description: '流程中断' }
      }
      return step
    })
  }

  return allSteps
}

/** 获取当前活跃步骤索引 */
export function getActiveStepIndex(status: OrderStatus): number {
  const map: Record<OrderStatus, number> = {
    PENDING_RESERVATION: 0,
    PENDING_STORE_CONFIRMATION: 0,
    ACCEPTED: 1,
    READY_FOR_PICKUP: 2,
    COMPLETED: 3,
    CANCELLED: 99,
    REJECTED: 99,
    EXPIRED: 99,
    NO_SHOW_PENDING: 99,
  }
  return map[status] || 0
}

/** 生成 UUID v4 */
export function generateIdempotencyKey(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

/** 金额分转元（数字） */
export function centsToYuanNum(cents: number): number {
  return cents / 100
}

/** 计算订单项小计（分） */
export function orderItemSubtotal(unitPrice: number, quantity: number): number {
  return unitPrice * quantity
}

/** 状态对应的 Tab 筛选值 */
export const ORDER_TABS: { label: string; value: string }[] = [
  { label: '全部', value: 'ALL' },
  { label: '待确认', value: 'PENDING_CONFIRMATION' },
  { label: '备货中', value: 'PREPARING' },
  { label: '待自取', value: 'READY_FOR_PICKUP' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
]

/** Tab 值映射到后端 status 列表 */
export function tabToStatuses(tab: string): string[] | undefined {
  const map: Record<string, string[] | undefined> = {
    ALL: undefined,
    PENDING_CONFIRMATION: ['PENDING_RESERVATION', 'PENDING_STORE_CONFIRMATION'],
    PREPARING: ['ACCEPTED'],
    READY_FOR_PICKUP: ['READY_FOR_PICKUP'],
    COMPLETED: ['COMPLETED'],
    CANCELLED: ['CANCELLED', 'REJECTED', 'EXPIRED', 'NO_SHOW_PENDING'],
  }
  return map[tab]
}

/** 判断状态是否匹配 tab */
export function orderMatchesTab(status: OrderStatus, tab: string): boolean {
  if (tab === 'ALL') return true
  const statuses = tabToStatuses(tab)
  return statuses ? statuses.includes(status) : true
}

export { formatDateTime }
