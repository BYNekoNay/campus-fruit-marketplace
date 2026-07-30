import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useReviewStore = defineStore('review', () => {
  const storeReviews = ref<ReviewInfo[]>([])
  const myReviews = ref<ReviewInfo[]>([])
  const statistics = ref<ReviewStatistics | null>(null)
  const reports = ref<ReviewReportInfo[]>([])
  const loading = ref(false)

  async function submitReview(dto: SubmitReviewRequest) {
    loading.value = true
    try {
      const res = await request.post<ReviewInfo>('/api/reviews', dto)
      return res
    } finally {
      loading.value = false
    }
  }

  async function updateReview(id: number, dto: { rating: number; content: string; tags: string[] }) {
    const res = await request.put<ReviewInfo>(`/api/reviews/${id}`, dto)
    return res
  }

  async function fetchMyReviews() {
    loading.value = true
    try {
      const res = await request.get<ReviewInfo[]>('/api/reviews/my')
      myReviews.value = Array.isArray(res) ? res : (res as any).data ?? []
    } finally {
      loading.value = false
    }
  }

  async function fetchStoreReviews(storeId: number, page = 1, size = 10) {
    loading.value = true
    try {
      const res: any = await request.get(`/api/stores/${storeId}/reviews`, { params: { page, size } })
      storeReviews.value = res.items ?? res.data?.items ?? []
      statistics.value = res.statistics ?? res.data?.statistics ?? null
    } finally {
      loading.value = false
    }
  }

  async function addMerchantReply(reviewId: number, content: string) {
    await request.post(`/api/reviews/${reviewId}/reply`, { content })
  }

  async function submitReport(reviewId: number, reason: string) {
    await request.post('/api/reports', { reviewId, reason })
  }

  async function fetchPendingReports() {
    loading.value = true
    try {
      const res = await request.get<ReviewReportInfo[]>('/api/admin/reports')
      reports.value = Array.isArray(res) ? res : (res as any).data ?? []
    } finally {
      loading.value = false
    }
  }

  async function reviewReport(reportId: number, action: 'DISMISS' | 'ACCEPT', comment?: string) {
    await request.put(`/api/admin/reports/${reportId}/review`, { action, comment })
    await fetchPendingReports()
  }

  return {
    storeReviews,
    myReviews,
    statistics,
    reports,
    loading,
    submitReview,
    updateReview,
    fetchMyReviews,
    fetchStoreReviews,
    addMerchantReply,
    submitReport,
    fetchPendingReports,
    reviewReport
  }
})
