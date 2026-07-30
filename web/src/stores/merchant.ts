import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

export const useMerchantStore = defineStore('merchant', () => {
  // ==================== 状态 ====================
  const merchant = ref<MerchantInfo | null>(null)
  const stores = ref<MerchantStoreInfo[]>([])
  const loading = ref(false)

  // ==================== Getters ====================
  const hasMerchant = computed(() => merchant.value !== null && merchant.value.id > 0)

  const isApproved = computed(
    () => merchant.value?.status === 'APPROVED'
  )

  const activeStores = computed(() =>
    stores.value.filter((s) => s.status === 'ACTIVE')
  )

  const pendingApprovalStores = computed(() =>
    stores.value.filter((s) => s.status === 'PENDING_APPROVAL')
  )

  // ==================== Actions ====================

  /** 获取我的商家入驻信息 */
  async function fetchMyMerchant(): Promise<MerchantInfo | null> {
    loading.value = true
    try {
      const res: any = await request.get('/merchant/my')
      if (res.data) {
        merchant.value = res.data
        return res.data
      }
      merchant.value = null
      return null
    } catch {
      merchant.value = null
      return null
    } finally {
      loading.value = false
    }
  }

  /** 申请入驻 */
  async function applyMerchant(dto: CreateMerchantRequest): Promise<MerchantInfo> {
    const res: any = await request.post('/merchant/apply', dto)
    const data = res.data
    if (data) {
      merchant.value = data
    }
    return data
  }

  /** 获取门店列表 */
  async function fetchStores(merchantId: number): Promise<MerchantStoreInfo[]> {
    loading.value = true
    try {
      const res: any = await request.get(`/merchant/${merchantId}/stores`)
      stores.value = res.data ?? res ?? []
      return stores.value
    } catch {
      stores.value = []
      return []
    } finally {
      loading.value = false
    }
  }

  /** 创建门店 */
  async function createStore(
    merchantId: number,
    dto: CreateStoreRequest
  ): Promise<MerchantStoreInfo> {
    const res: any = await request.post(`/merchant/${merchantId}/stores`, dto)
    const data = res.data
    if (data) {
      stores.value.push(data)
    }
    return data
  }

  /** 更新门店 */
  async function updateStore(
    storeId: number,
    dto: UpdateStoreRequest
  ): Promise<MerchantStoreInfo> {
    const res: any = await request.put(`/stores/${storeId}`, dto)
    const data = res.data
    if (data) {
      const idx = stores.value.findIndex((s) => s.id === storeId)
      if (idx !== -1) {
        stores.value[idx] = data
      }
    }
    return data
  }

  /** 添加员工 */
  async function addStaff(
    storeId: number,
    dto: AddStaffRequest
  ): Promise<StaffInfo> {
    const res: any = await request.post(`/stores/${storeId}/staff`, dto)
    const data = res.data
    // 更新对应门店的 staffList
    if (data) {
      const store = stores.value.find((s) => s.id === storeId)
      if (store) {
        if (!store.staffList) {
          store.staffList = []
        }
        store.staffList.push(data)
      }
    }
    return data
  }

  /** 移除员工 */
  async function removeStaff(storeId: number, userId: number): Promise<void> {
    await request.delete(`/stores/${storeId}/staff/${userId}`)
    const store = stores.value.find((s) => s.id === storeId)
    if (store?.staffList) {
      store.staffList = store.staffList.filter((s) => s.userId !== userId)
    }
  }

  /** 暂停门店 */
  async function suspendStore(storeId: number): Promise<void> {
    await request.put(`/stores/${storeId}/suspend`)
    const store = stores.value.find((s) => s.id === storeId)
    if (store) {
      store.status = 'SUSPENDED'
      store.statusLabel = '已暂停'
    }
  }

  /** 激活门店 */
  async function activateStore(storeId: number): Promise<void> {
    await request.put(`/stores/${storeId}/activate`)
    const store = stores.value.find((s) => s.id === storeId)
    if (store) {
      store.status = 'ACTIVE'
      store.statusLabel = '营业中'
    }
  }

  /** 重置状态 */
  function reset() {
    merchant.value = null
    stores.value = []
    loading.value = false
  }

  return {
    // 状态
    merchant,
    stores,
    loading,
    // getters
    hasMerchant,
    isApproved,
    activeStores,
    pendingApprovalStores,
    // actions
    fetchMyMerchant,
    applyMerchant,
    fetchStores,
    createStore,
    updateStore,
    addStaff,
    removeStaff,
    suspendStore,
    activateStore,
    reset,
  }
})
