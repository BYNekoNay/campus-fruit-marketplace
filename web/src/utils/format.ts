/**
 * 工具函数：格式化显示
 */

/** 分转元，保留 2 位小数 */
export function centsToYuan(cents: number): string {
  return (cents / 100).toFixed(2)
}

/** 克转为显示文本 */
export function gramsToDisplay(grams: number | null): string {
  if (grams === null || grams === undefined) {
    return '不可比'
  }
  if (grams >= 1000 && grams % 1000 === 0) {
    return `${grams / 1000}kg`
  }
  return `${grams}g`
}

/** 标准价显示 */
export function standardPriceDisplay(price: number | null): string {
  if (price === null || price === undefined) {
    return '不可比'
  }
  return `¥${centsToYuan(price)}/500g`
}

/** 价格变化方向 */
export function priceChangeDirection(
  oldPrice: number,
  newPrice: number
): 'up' | 'down' | 'same' {
  if (newPrice > oldPrice) return 'up'
  if (newPrice < oldPrice) return 'down'
  return 'same'
}

/** 日期时间格式化 "2026-07-29 22:30" */
export function formatDateTime(date: string): string {
  if (!date) return '-'
  const d = new Date(date)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 相对时间 "3小时前" / "2天前" */
export function timeAgo(date: string): string {
  if (!date) return '-'
  const now = Date.now()
  const then = new Date(date).getTime()
  const diff = now - then

  const seconds = Math.floor(diff / 1000)
  if (seconds < 60) return '刚刚'

  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}分钟前`

  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`

  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`

  const months = Math.floor(days / 30)
  if (months < 12) return `${months}个月前`

  const years = Math.floor(months / 12)
  return `${years}年前`
}
