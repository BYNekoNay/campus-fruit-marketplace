// 购物车 & 订单 类型定义

interface CartItem {
  id: number
  offerId: number
  canonicalFruitId: number
  fruitVariety: string
  salesUnit: string
  unitPrice: number        // 分
  unitPriceYuan: number    // 元(计算属性)
  quantity: number
  addedAt: string
}

interface CartInfo {
  cartId: number
  storeId: number
  storeName: string
  items: CartItem[]
  totalAmount: number      // 分
  totalAmountYuan: number  // 元
}

interface OrderItem {
  id: number
  offerId: number
  fruitVariety: string
  salesUnit: string
  unitPrice: number
  quantity: number
}

interface OrderInfo {
  id: number
  orderNo: string
  storeId: number
  storeName: string
  status: OrderStatus
  statusLabel: string
  items: OrderItem[]
  totalAmount: number
  totalAmountYuan: number
  pickupCode?: string       // 仅 READY_FOR_PICKUP 状态
  pickupCodeExpiresAt?: string
  paymentStatus: 'UNPAID' | 'PAID_AT_PICKUP'
  cancelReason?: string
  createdAt: string
  statusEvents: OrderStatusEvent[]
}

type OrderStatus = 'PENDING_RESERVATION' | 'PENDING_STORE_CONFIRMATION' | 'ACCEPTED' | 'READY_FOR_PICKUP' | 'COMPLETED' | 'CANCELLED' | 'REJECTED' | 'EXPIRED' | 'NO_SHOW_PENDING'

interface OrderStatusEvent {
  id: number
  fromStatus: string
  toStatus: string
  operatorType: string
  note: string
  createdAt: string
}

interface AddToCartRequest {
  offerId: number
  quantity: number
}

interface CreateOrderRequest {
  idempotencyKey: string    // 前端生成 UUID
}

interface SwitchCartConfirmInfo {
  currentStoreName: string
  currentItems: CartItem[]
  newStoreId: number
  newStoreName: string
}

// 分页订单列表
interface OrderPage {
  records: OrderInfo[]
  total: number
  size: number
  current: number
}

// 商家端订单列表请求参数
interface StoreOrderQuery {
  storeId: number
  status?: string
  page?: number
  size?: number
}
