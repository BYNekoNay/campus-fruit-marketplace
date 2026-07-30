import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useOfferStore = defineStore('offer', () => {
  // ==================== 状态 ====================
  const fruits = ref<CanonicalFruit[]>([])
  const offers = ref<FruitOffer[]>([])
  const priceHistories = ref<PriceHistory[]>([])
  const loading = ref(false)

  // ==================== 标准水果 Actions ====================

  /** 搜索标准水果（按品种名称搜索） */
  async function fetchFruits(keyword?: string): Promise<CanonicalFruit[]> {
    loading.value = true
    try {
      const params: Record<string, string | number> = {}
      if (keyword) params.keyword = keyword
      const res: any = await request.get('/admin/canonical-fruits', { params })
      fruits.value = res.data?.records ?? res.data ?? []
      return fruits.value
    } catch {
      fruits.value = []
      return []
    } finally {
      loading.value = false
    }
  }

  /** 管理员创建标准水果 */
  async function createFruit(dto: CreateFruitRequest): Promise<CanonicalFruit> {
    const res: any = await request.post('/admin/canonical-fruits', dto)
    const data = res.data
    if (data) {
      fruits.value.unshift(data)
    }
    return data
  }

  /** 管理员更新标准水果 */
  async function updateFruit(
    id: number,
    dto: UpdateFruitRequest
  ): Promise<CanonicalFruit> {
    const res: any = await request.put(`/admin/canonical-fruits/${id}`, dto)
    const data = res.data
    if (data) {
      const idx = fruits.value.findIndex((f) => f.id === id)
      if (idx !== -1) {
        fruits.value[idx] = data
      }
    }
    return data
  }

  /** 管理员切换水果状态（启用/停用） */
  async function toggleFruitStatus(id: number, active: boolean): Promise<void> {
    await request.put(`/admin/canonical-fruits/${id}/status`, { active })
    const idx = fruits.value.findIndex((f) => f.id === id)
    if (idx !== -1) {
      fruits.value[idx].status = active ? 'ACTIVE' : 'INACTIVE'
    }
  }

  // ==================== 报价 Actions ====================

  /** 获取门店报价列表 */
  async function fetchOffers(storeId: number): Promise<FruitOffer[]> {
    loading.value = true
    try {
      const res: any = await request.get(`/merchant/stores/${storeId}/offers`)
      offers.value = res.data ?? []
      return offers.value
    } catch {
      offers.value = []
      return []
    } finally {
      loading.value = false
    }
  }

  /** 创建报价 */
  async function createOffer(dto: CreateOfferRequest): Promise<FruitOffer> {
    const res: any = await request.post('/merchant/offers', dto)
    const data = res.data
    if (data) {
      offers.value.unshift(data)
    }
    return data
  }

  /** 更新报价 */
  async function updateOffer(
    id: number,
    dto: UpdateOfferRequest
  ): Promise<FruitOffer> {
    const res: any = await request.put(`/merchant/offers/${id}`, dto)
    const data = res.data
    if (data) {
      const idx = offers.value.findIndex((o) => o.id === id)
      if (idx !== -1) {
        offers.value[idx] = data
      }
    }
    return data
  }

  /** 确认价格 */
  async function confirmPrice(id: number): Promise<void> {
    await request.put(`/merchant/offers/${id}/confirm-price`)
    const idx = offers.value.findIndex((o) => o.id === id)
    if (idx !== -1) {
      offers.value[idx].lastConfirmedAt = new Date().toISOString()
      offers.value[idx].priceStale = false
    }
  }

  /** 暂停报价 */
  async function pauseOffer(id: number): Promise<void> {
    await request.put(`/merchant/offers/${id}/pause`)
    const idx = offers.value.findIndex((o) => o.id === id)
    if (idx !== -1) {
      offers.value[idx].status = 'PAUSED'
    }
  }

  /** 激活报价 */
  async function activateOffer(id: number): Promise<void> {
    await request.put(`/merchant/offers/${id}/activate`)
    const idx = offers.value.findIndex((o) => o.id === id)
    if (idx !== -1) {
      offers.value[idx].status = 'ACTIVE'
    }
  }

  /** 获取价格历史 */
  async function fetchPriceHistory(offerId: number): Promise<PriceHistory[]> {
    const res: any = await request.get(`/merchant/offers/${offerId}/price-history`)
    priceHistories.value = res.data ?? []
    return priceHistories.value
  }

  return {
    // 状态
    fruits,
    offers,
    priceHistories,
    loading,
    // 水果 actions
    fetchFruits,
    createFruit,
    updateFruit,
    toggleFruitStatus,
    // 报价 actions
    fetchOffers,
    createOffer,
    updateOffer,
    confirmPrice,
    pauseOffer,
    activateOffer,
    fetchPriceHistory,
  }
})
