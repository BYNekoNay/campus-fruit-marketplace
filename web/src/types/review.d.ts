export {}

declare global {
  interface ReviewInfo {
    id: number
    userId: number
    orderId: number
    storeId: number
    storeName?: string
    rating: number
    content: string
    tags: string[]
    status: 'ACTIVE' | 'HIDDEN' | 'DELETED'
    version: number
    merchantReply?: {
      id: number
      content: string
      createdAt: string
    }
    createdAt: string
  }

  interface ReviewStatistics {
    avgRating: number
    bayesianRating: number
    totalRatings: number
    distribution: Record<string, number>
  }

  interface SubmitReviewRequest {
    storeId: number
    orderId: number
    rating: number
    content: string
    tags: string[]
  }

  interface UpdateReviewRequest {
    rating: number
    content: string
    tags: string[]
  }

  interface ReviewReportInfo {
    id: number
    reviewId: number
    reporterId: number
    reason: string
    status: 'PENDING' | 'DISMISSED' | 'ACCEPTED'
    review?: ReviewInfo
    createdAt: string
  }

  interface MerchantReplyRequest {
    content: string
  }
}
