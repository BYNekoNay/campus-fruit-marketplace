// 标准水果目录 & 门店报价管理 类型声明

interface CanonicalFruit {
  id: number
  category: string      // 品类 如"柑橘类"
  variety: string       // 品种 如"赣南脐橙"
  grade: string         // 等级 如"一级"
  origin: string        // 产地
  defaultUnit: string   // 默认计量单位
  comparisonGroupId: number | null
  version: number
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
}

interface FruitOffer {
  id: number
  storeId: number
  canonicalFruitId: number
  fruitCategory: string
  fruitVariety: string
  fruitGrade: string
  salesUnit: string          // 销售单位 如"500g盒装"
  netWeightGrams: number | null  // 净重克数
  unitPrice: number          // 单价(分)
  standardPricePer500g: number | null  // 标准价(每500g/元)
  isComparable: boolean      // 是否可比
  stockQuantity: number
  availableQuantity: number
  reservedQuantity: number
  status: 'ACTIVE' | 'PAUSED' | 'EXPIRED'
  priceStale: boolean        // 价格陈旧标记
  lastConfirmedAt: string | null
  qualityDesc: string
  createdAt: string
}

interface PriceHistory {
  id: number
  offerId: number
  unitPrice: number
  netWeightGrams: number | null
  salesUnit: string
  changedAt: string
}

interface CreateOfferRequest {
  storeId: number
  canonicalFruitId: number
  salesUnit: string
  netWeightGrams: number | null
  unitPrice: number
  stockQuantity: number
  qualityDesc: string
}

interface CreateFruitRequest {
  category: string
  variety: string
  grade: string
  origin: string
  defaultUnit: string
}

interface UpdateFruitRequest {
  category: string
  variety: string
  grade: string
  origin: string
  defaultUnit: string
}

interface UpdateOfferRequest {
  salesUnit: string
  netWeightGrams: number | null
  unitPrice: number
  stockQuantity: number
  qualityDesc: string
}
