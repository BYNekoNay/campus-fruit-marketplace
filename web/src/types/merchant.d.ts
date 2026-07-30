// 商家管理相关类型声明（全局 ambient declaration）

interface MerchantInfo {
  id: number
  ownerUserId: number
  name: string
  contactName: string
  contactPhone: string
  licenseNumber: string
  status: 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'SUSPENDED'
  rejectReason?: string
  statusLabel: string
  createdAt: string
}

interface MerchantStoreInfo {
  id: number
  merchantId: number
  name: string
  address: string
  latitude: number
  longitude: number
  coordType: string
  phone: string
  businessHours: string
  status: 'PENDING_APPROVAL' | 'ACTIVE' | 'CLOSED' | 'SUSPENDED'
  statusLabel: string
  pickupLeadMinutes: number
  staffList?: StaffInfo[]
  createdAt: string
}

interface StaffInfo {
  id: number
  storeId: number
  userId: number
  role: string
  storeName?: string
  createdAt: string
}

interface CreateMerchantRequest {
  name: string
  contactName: string
  contactPhone: string
  licenseNumber: string
}

interface CreateStoreRequest {
  name: string
  address: string
  latitude: number
  longitude: number
  phone: string
  businessHours: string
}

interface UpdateStoreRequest {
  name: string
  address: string
  latitude: number
  longitude: number
  phone: string
  businessHours: string
}

interface AddStaffRequest {
  userId: number
  role: string
}

interface ReviewActionRequest {
  approved: boolean
  reason?: string
}
