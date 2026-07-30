// 门店与水果相关类型

export interface StoreInfo {
  id: number
  name: string
  description: string
  logo: string
  coverImage: string
  address: string
  phone: string
  businessHours: string
  rating: number
  salesVolume: number
  tags: string[]
  status: StoreStatus
  ownerId: number
  createdAt: string
}

export type StoreStatus = 'open' | 'closed' | 'resting'

export interface FruitOffer {
  id: number
  storeId: number
  name: string
  description: string
  images: string[]
  price: number
  originalPrice: number
  unit: string
  stock: number
  category: FruitCategory
  tags: string[]
  salesVolume: number
  rating: number
  isOnSale: boolean
  createdAt: string
}

export type FruitCategory =
  | 'seasonal'
  | 'tropical'
  | 'berries'
  | 'citrus'
  | 'melon'
  | 'drupe'
  | 'imported'
  | 'other'

// CartItem, OrderInfo, OrderItem, OrderStatus 类型已移至 src/types/order.d.ts
