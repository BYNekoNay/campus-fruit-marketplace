import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useDiscoveryStore = defineStore('discovery', () => {
  // ==================== 状态 ====================
  const searchResults = ref<SearchResult[]>([])
  const searchTotal = ref(0)
  const nearbyStores = ref<NearbyStore[]>([])
  const compareData = ref<CompareResponse | null>(null)
  const categories = ref<CategoryOption[]>([])
  const priceStats = ref<PriceStats | null>(null)
  const favorites = ref<FavoriteStore[]>([])
  const loading = ref(false)

  // ==================== 搜索 ====================

  async function search(params: Partial<SearchRequest>): Promise<SearchResponse> {
    loading.value = true
    try {
      const res: any = await request.post('/api/discovery/search', {
        keyword: params.keyword || '',
        category: params.category || '',
        lat: params.lat ?? null,
        lng: params.lng ?? null,
        radiusKm: params.radiusKm ?? null,
        minPrice: params.minPrice ?? null,
        maxPrice: params.maxPrice ?? null,
        minRating: params.minRating ?? null,
        sortBy: params.sortBy || 'COMPREHENSIVE',
        page: params.page ?? 1,
        size: params.size ?? 20,
      })
      const data: SearchResponse = res.data
      if (data) {
        searchResults.value = data.items ?? []
        searchTotal.value = data.totalCount ?? 0
      }
      return data ?? { totalCount: 0, items: [] }
    } catch {
      searchResults.value = []
      searchTotal.value = 0
      return { totalCount: 0, items: [] }
    } finally {
      loading.value = false
    }
  }

  // ==================== 附近门店 ====================

  async function fetchNearby(
    lat: number,
    lng: number,
    radius?: number
  ): Promise<NearbyStore[]> {
    try {
      const params: Record<string, string | number> = { lat, lng }
      if (radius) params.radiusKm = radius
      const res: any = await request.get('/api/discovery/nearby', { params })
      nearbyStores.value = res.data ?? []
      return nearbyStores.value
    } catch {
      nearbyStores.value = []
      return []
    }
  }

  // ==================== 比价 ====================

  async function compare(offerIds: number[]): Promise<CompareResponse | null> {
    loading.value = true
    try {
      const res: any = await request.post('/api/discovery/compare', { offerIds })
      compareData.value = res.data ?? null
      return compareData.value
    } catch {
      compareData.value = null
      return null
    } finally {
      loading.value = false
    }
  }

  // ==================== 品类列表 ====================

  async function fetchCategories(): Promise<CategoryOption[]> {
    try {
      const res: any = await request.get('/api/discovery/categories')
      categories.value = res.data ?? []
      return categories.value
    } catch {
      categories.value = []
      return []
    }
  }

  // ==================== 价格统计 ====================

  async function fetchPriceStats(fruitId: number): Promise<PriceStats | null> {
    try {
      const res: any = await request.get(`/api/discovery/stats/${fruitId}`)
      priceStats.value = res.data ?? null
      return priceStats.value
    } catch {
      priceStats.value = null
      return null
    }
  }

  // ==================== 收藏 ====================

  async function fetchFavorites(): Promise<FavoriteStore[]> {
    try {
      const res: any = await request.get('/api/favorites')
      favorites.value = res.data ?? []
      return favorites.value
    } catch {
      favorites.value = []
      return []
    }
  }

  async function addFavorite(storeId: number): Promise<boolean> {
    try {
      await request.post(`/api/favorites/${storeId}`)
      // 乐观添加到本地列表
      if (!favorites.value.find((f) => f.storeId === storeId)) {
        favorites.value.push({
          storeId,
          storeName: '',
          addedAt: new Date().toISOString(),
        })
      }
      return true
    } catch {
      return false
    }
  }

  async function removeFavorite(storeId: number): Promise<boolean> {
    try {
      await request.delete(`/api/favorites/${storeId}`)
      favorites.value = favorites.value.filter((f) => f.storeId !== storeId)
      return true
    } catch {
      return false
    }
  }

  function isFavorite(storeId: number): boolean {
    return favorites.value.some((f) => f.storeId === storeId)
  }

  return {
    // 状态
    searchResults,
    searchTotal,
    nearbyStores,
    compareData,
    categories,
    priceStats,
    favorites,
    loading,
    // 搜索
    search,
    // 附近门店
    fetchNearby,
    // 比价
    compare,
    // 品类
    fetchCategories,
    // 价格统计
    fetchPriceStats,
    // 收藏
    fetchFavorites,
    addFavorite,
    removeFavorite,
    isFavorite,
  }
})
