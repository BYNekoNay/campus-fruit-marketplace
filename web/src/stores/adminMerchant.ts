import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

export const useAdminMerchantStore = defineStore('adminMerchant', () => {
  // ==================== 状态 ====================
  const merchants = ref<MerchantInfo[]>([])
  const currentMerchant = ref<MerchantInfo | null>(null)
  const merchantStores = ref<MerchantStoreInfo[]>([])
  const loading = ref(false)
  const pagination = ref({ page: 1, pageSize: 10, total: 0 })

  // ==================== Getters ====================
  const pendingMerchants = computed(() =>
    merchants.value.filter((m) => m.status === 'PENDING_REVIEW')
  )
  const approvedMerchants = computed(() =>
    merchants.value.filter((m) => m.status === 'APPROVED')
  )
  const rejectedMerchants = computed(() =>
    merchants.value.filter((m) => m.status === 'REJECTED')
  )

  // ==================== Actions ====================

  /** 获取商家列表（支持分页和状态筛选） */
  async function fetchMerchants(params: {
    page: number
    pageSize: number
    status?: string
  }): Promise<void> {
    loading.value = true
    try {
      const res: any = await request.get('/admin/merchants', { params })
      if (res.data) {
        merchants.value = res.data.records ?? res.data
        pagination.value.total = res.data.total ?? 0
      } else {
        merchants.value = res ?? []
      }
    } catch {
      merchants.value = []
    } finally {
      loading.value = false
    }
  }

  /** 获取商家详情 */
  async function fetchMerchantDetail(id: number): Promise<MerchantInfo | null> {
    loading.value = true
    try {
      const res: any = await request.get(`/admin/merchants/${id}`)
      currentMerchant.value = res.data ?? res
      return currentMerchant.value
    } catch {
      currentMerchant.value = null
      return null
    } finally {
      loading.value = false
    }
  }

  /** 获取商家门店列表 */
  async function fetchMerchantStores(merchantId: number): Promise<MerchantStoreInfo[]> {
    try {
      const res: any = await request.get(`/merchant/${merchantId}/stores`)
      merchantStores.value = res.data ?? res ?? []
      return merchantStores.value
    } catch {
      merchantStores.value = []
      return []
    }
  }

  /** 审核通过 */
  async function approveMerchant(id: number): Promise<void> {
    await request.post(`/admin/merchants/${id}/review`, { approved: true, reason: '' })
    // 更新列表中的状态
    const idx = merchants.value.findIndex((m) => m.id === id)
    if (idx !== -1) {
      merchants.value[idx].status = 'APPROVED'
      merchants.value[idx].statusLabel = '已通过'
    }
    if (currentMerchant.value?.id === id) {
      currentMerchant.value.status = 'APPROVED'
      currentMerchant.value.statusLabel = '已通过'
    }
  }

  /** 审核拒绝 */
  async function rejectMerchant(id: number, reason: string): Promise<void> {
    await request.post(`/admin/merchants/${id}/review`, { approved: false, reason })
    // 更新列表中的状态
    const idx = merchants.value.findIndex((m) => m.id === id)
    if (idx !== -1) {
      merchants.value[idx].status = 'REJECTED'
      merchants.value[idx].statusLabel = '已拒绝'
      merchants.value[idx].rejectReason = reason
    }
    if (currentMerchant.value?.id === id) {
      currentMerchant.value.status = 'REJECTED'
      currentMerchant.value.statusLabel = '已拒绝'
      currentMerchant.value.rejectReason = reason
    }
  }

  /** 暂停商家 */
  async function suspendMerchant(id: number): Promise<void> {
    await request.put(`/admin/merchants/${id}/suspend`)
    const idx = merchants.value.findIndex((m) => m.id === id)
    if (idx !== -1) {
      merchants.value[idx].status = 'SUSPENDED'
      merchants.value[idx].statusLabel = '已暂停'
    }
    if (currentMerchant.value?.id === id) {
      currentMerchant.value.status = 'SUSPENDED'
      currentMerchant.value.statusLabel = '已暂停'
    }
  }

  /** 重置 */
  function reset() {
    merchants.value = []
    currentMerchant.value = null
    merchantStores.value = []
    loading.value = false
    pagination.value = { page: 1, pageSize: 10, total: 0 }
  }

  return {
    merchants,
    currentMerchant,
    merchantStores,
    loading,
    pagination,
    pendingMerchants,
    approvedMerchants,
    rejectedMerchants,
    fetchMerchants,
    fetchMerchantDetail,
    fetchMerchantStores,
    approveMerchant,
    rejectMerchant,
    suspendMerchant,
    reset,
  }
})
