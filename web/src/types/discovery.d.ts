// 发现页 & 搜索 & 比价 类型声明

interface SearchRequest {
  keyword?: string
  category?: string
  lat?: number
  lng?: number
  radiusKm?: number
  minPrice?: number
  maxPrice?: number
  minRating?: number
  sortBy: 'COMPREHENSIVE' | 'DISTANCE' | 'PRICE_ASC' | 'PRICE_DESC' | 'RATING'
  page: number
  size: number
}

interface SearchResult {
  offerId: number
  storeId: number
  storeName: string
  storeLat: number
  storeLng: number
  distance: number | null       // 直线距离(km)
  fruitVariety: string
  fruitCategory: string
  fruitGrade: string
  fruitOrigin: string
  salesUnit: string
  unitPrice: number             // 元
  standardPricePer500g: number | null
  isComparable: boolean
  availableQuantity: number
  avgRating: number
  reviewCount: number
  priceStale: boolean
  coldStart?: boolean           // 冷启动标记
  rankingScore?: number         // 综合排序分
  rankingReason?: string        // 排序原因解释
  storeStatus: string
  storeAddress: string
  phone?: string
  businessHours?: string
}

interface SearchResponse {
  totalCount: number
  items: SearchResult[]
}

interface CompareItem {
  storeName: string
  storeAddress: string
  distance: number | null
  fruitVariety: string
  salesUnit: string
  unitPrice: number
  standardPricePer500g: number | null
  isComparable: boolean
  stockStatus: string
  avgRating: number
}

interface CompareResponse {
  offers: CompareItem[]
  stats: {
    minPrice: number
    maxPrice: number
    medianPrice: number
    avgPrice: number
    storeCount: number
    sampleCount: number
    sampleInsufficient: boolean
  }
}

interface NearbyStore {
  storeId: number
  storeName: string
  address: string
  lat: number
  lng: number
  distance: number
  phone: string
  avgRating: number
}

interface FavoriteStore {
  storeId: number
  storeName: string
  addedAt: string
}

interface CategoryOption {
  label: string
  value: string
}

interface PriceStats {
  fruitId: number
  fruitVariety: string
  minPrice: number
  maxPrice: number
  avgPrice: number
  medianPrice: number
  sampleCount: number
  sampleInsufficient: boolean
  updatedAt: string
}
