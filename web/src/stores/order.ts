import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { generateIdempotencyKey, centsToYuanNum } from '@/utils/order'

export const useOrderStore = defineStore('order', () => {
  // ==================== 状态 ====================
  const cart = ref<CartInfo | null>(null)
  const orders = ref<OrderInfo[]>([])
  const currentOrder = ref<OrderInfo | null>(null)
  const orderTotal = ref(0)
  const loading = ref(false)

  // ==================== Getters ====================
  const cartItemCount = computed(() => cart.value?.items?.length ?? 0)

  const cartTotal = computed(() => {
    if (!cart.value) return 0
    return centsToYuanNum(cart.value.totalAmount)
  })

  // ==================== 购物车 Actions ====================

  /** 获取购物车 */
  async function fetchCart(): Promise<CartInfo | null> {
    loading.value = true
    try {
      const res: any = await request.get('/cart')
      cart.value = res.data ?? null
      // 计算 unitPriceYuan
      if (cart.value?.items) {
        cart.value.items.forEach((item) => {
          item.unitPriceYuan = centsToYuanNum(item.unitPrice)
        })
        cart.value.totalAmountYuan = centsToYuanNum(cart.value.totalAmount)
      }
      return cart.value
    } catch {
      cart.value = null
      return null
    } finally {
      loading.value = false
    }
  }

  /** 添加到购物车 */
  async function addToCart(offerId: number, quantity: number): Promise<void> {
    const params: AddToCartRequest = { offerId, quantity }
    await request.post('/cart/items', params)
    // 重新拉取购物车
    await fetchCart()
  }

  /** 从购物车移除 */
  async function removeFromCart(itemId: number): Promise<void> {
    await request.delete(`/cart/items/${itemId}`)
    await fetchCart()
  }

  /** 清空购物车 */
  async function clearCart(): Promise<void> {
    await request.delete('/cart')
    cart.value = null
  }

  // ==================== 订单 Actions ====================

  /** 创建订单 */
  async function createOrder(): Promise<OrderInfo | null> {
    loading.value = true
    try {
      const idempotencyKey = generateIdempotencyKey()
      const res: any = await request.post('/orders', { idempotencyKey })
      const data: OrderInfo = res.data
      if (data) {
        data.totalAmountYuan = centsToYuanNum(data.totalAmount)
        if (data.items) {
          data.items.forEach((item) => {
            (item as any).subtotal = item.unitPrice * item.quantity
            ;(item as any).subtotalYuan = centsToYuanNum(item.unitPrice * item.quantity)
          })
        }
      }
      currentOrder.value = data
      // 创建订单后清空购物车
      cart.value = null
      return data
    } catch {
      return null
    } finally {
      loading.value = false
    }
  }

  /** 获取订单列表 */
  async function fetchOrders(page: number = 1, size: number = 10): Promise<void> {
    loading.value = true
    try {
      const res: any = await request.get('/orders', {
        params: { page, size },
      })
      const data: OrderPage = res.data
      orders.value = (data.records || []).map((order: OrderInfo) => ({
        ...order,
        totalAmountYuan: centsToYuanNum(order.totalAmount),
      }))
      orderTotal.value = data.total || 0
    } catch {
      orders.value = []
      orderTotal.value = 0
    } finally {
      loading.value = false
    }
  }

  /** 获取订单详情 */
  async function fetchOrderDetail(id: number): Promise<OrderInfo | null> {
    loading.value = true
    try {
      const res: any = await request.get(`/orders/${id}`)
      const data: OrderInfo = res.data
      if (data) {
        data.totalAmountYuan = centsToYuanNum(data.totalAmount)
        if (data.items) {
          data.items.forEach((item) => {
            (item as any).subtotal = item.unitPrice * item.quantity
            ;(item as any).subtotalYuan = centsToYuanNum(item.unitPrice * item.quantity)
          })
        }
      }
      currentOrder.value = data
      return data
    } catch {
      currentOrder.value = null
      return null
    } finally {
      loading.value = false
    }
  }

  /** 取消订单 */
  async function cancelOrder(id: number): Promise<void> {
    await request.put(`/orders/${id}/cancel`)
    // 刷新当前订单
    await fetchOrderDetail(id)
    // 同时更新列表
    const idx = orders.value.findIndex((o) => o.id === id)
    if (idx !== -1 && currentOrder.value) {
      orders.value[idx] = currentOrder.value
    }
  }

  /** 获取自取码 */
  async function getPickupCode(id: number): Promise<{ pickupCode: string; expiresAt: string } | null> {
    const res: any = await request.get(`/orders/${id}/pickup-code`)
    return res.data ?? null
  }

  // ==================== 商家端 Actions ====================

  /** 获取门店订单列表 */
  async function fetchStoreOrders(
    storeId: number,
    status?: string,
    page: number = 1,
    size: number = 10
  ): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, string | number> = { storeId, page, size }
      if (status) params.status = status
      const res: any = await request.get('/store/orders', { params })
      const data: OrderPage = res.data
      orders.value = (data.records || []).map((order: OrderInfo) => ({
        ...order,
        totalAmountYuan: centsToYuanNum(order.totalAmount),
      }))
      orderTotal.value = data.total || 0
    } catch {
      orders.value = []
      orderTotal.value = 0
    } finally {
      loading.value = false
    }
  }

  /** 接单 */
  async function acceptOrder(id: number): Promise<void> {
    loading.value = true
    try {
      await request.put(`/store/orders/${id}/accept`)
    } finally {
      loading.value = false
    }
  }

  /** 拒单 */
  async function rejectOrder(id: number, reason: string): Promise<void> {
    loading.value = true
    try {
      await request.put(`/store/orders/${id}/reject`, { reason })
    } finally {
      loading.value = false
    }
  }

  /** 备货完成 */
  async function readyOrder(id: number): Promise<void> {
    loading.value = true
    try {
      await request.put(`/store/orders/${id}/ready`)
    } finally {
      loading.value = false
    }
  }

  /** 核销（完成） */
  async function completeOrder(id: number, pickupCode: string): Promise<void> {
    loading.value = true
    try {
      await request.put(`/store/orders/${id}/complete`, { pickupCode })
    } finally {
      loading.value = false
    }
  }

  /** 标记未取 */
  async function markNoShow(id: number): Promise<void> {
    loading.value = true
    try {
      await request.put(`/store/orders/${id}/no-show`)
    } finally {
      loading.value = false
    }
  }

  return {
    // 状态
    cart,
    orders,
    currentOrder,
    orderTotal,
    loading,
    // Getters
    cartItemCount,
    cartTotal,
    // 购物车
    fetchCart,
    addToCart,
    removeFromCart,
    clearCart,
    // 订单
    createOrder,
    fetchOrders,
    fetchOrderDetail,
    cancelOrder,
    getPickupCode,
    // 商家端
    fetchStoreOrders,
    acceptOrder,
    rejectOrder,
    readyOrder,
    completeOrder,
    markNoShow,
  }
})
