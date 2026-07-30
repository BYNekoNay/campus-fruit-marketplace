<template>
  <div class="store-detail-page">
    <!-- 返回按钮 -->
    <el-page-header @back="$router.back()">
      <template #content>
        <span v-if="storeInfo">{{ storeInfo.storeName }}</span>
        <span v-else>门店详情</span>
      </template>
    </el-page-header>

    <!-- 加载状态 -->
    <template v-if="store.loading">
      <div class="store-detail-page__content">
        <el-skeleton :rows="8" animated />
      </div>
    </template>

    <!-- 错误状态 -->
    <el-result
      v-else-if="error"
      icon="error"
      title="加载失败"
      :sub-title="error"
    >
      <template #extra>
        <el-button type="primary" @click="fetchData">重新加载</el-button>
      </template>
    </el-result>

    <!-- 门店信息 -->
    <div v-else-if="storeInfo" class="store-detail-page__content">
      <!-- 门店信息卡片 -->
      <el-card class="store-detail-page__info-card">
        <div class="store-detail-page__info-header">
          <div class="store-detail-page__store-name">
            <el-icon :size="28" color="#67C23A"><Shop /></el-icon>
            <h2>{{ storeInfo.storeName }}</h2>
            <el-tag
              :type="statusType[storeInfo.storeStatus] || 'info'"
              size="small"
            >
              {{ statusLabel[storeInfo.storeStatus] || storeInfo.storeStatus }}
            </el-tag>
          </div>
          <el-button
            :type="store.isFavorite(storeId) ? 'danger' : 'default'"
            :icon="StarFilled"
            circle
            @click="toggleFavorite"
          />
        </div>

        <div class="store-detail-page__info-grid">
          <div class="store-detail-page__info-item">
            <el-icon><Location /></el-icon>
            <span>{{ storeInfo.storeAddress }}</span>
          </div>
          <div class="store-detail-page__info-item">
            <el-icon><Phone /></el-icon>
            <span>{{ storeInfo.phone || '暂无' }}</span>
          </div>
          <div class="store-detail-page__info-item">
            <el-icon><Clock /></el-icon>
            <span>{{ storeInfo.businessHours || '暂无营业时间' }}</span>
          </div>
          <div class="store-detail-page__info-item">
            <el-icon><Star /></el-icon>
            <el-rate
              :model-value="storeInfo.avgRating"
              disabled
              show-score
              size="small"
            />
          </div>
        </div>
      </el-card>

      <!-- 门店位置地图 -->
      <el-card class="store-detail-page__map-card">
        <template #header>
          <span>门店位置</span>
        </template>
        <DiscoveryMap
          :stores="[mapStoreData]"
          :center-lat="storeInfo.storeLat"
          :center-lng="storeInfo.storeLng"
        />
      </el-card>

      <!-- 报价列表 -->
      <el-card class="store-detail-page__offers-card">
        <template #header>
          <span>当前报价（{{ offers.length }}）</span>
        </template>

        <el-empty v-if="offers.length === 0" description="暂无报价" />

        <div v-else class="store-detail-page__offers-grid">
          <el-card
            v-for="offer in offers"
            :key="offer.offerId"
            shadow="hover"
            class="store-detail-page__offer-item"
          >
            <div class="store-detail-page__offer-header">
              <h3 class="store-detail-page__offer-variety">
                {{ offer.fruitVariety }}
                <el-tag
                  v-if="offer.priceStale"
                  type="warning"
                  size="small"
                  effect="dark"
                >
                  价格可能过期
                </el-tag>
              </h3>
              <span class="store-detail-page__offer-price">
                ¥{{ offer.unitPrice.toFixed(2) }}
              </span>
            </div>

            <div class="store-detail-page__offer-meta">
              <el-tag size="small">{{ offer.fruitCategory }}</el-tag>
              <el-tag size="small" type="success">{{ offer.fruitGrade }}</el-tag>
              <el-tag size="small" type="warning">{{ offer.fruitOrigin }}</el-tag>
              <span class="store-detail-page__offer-spec">{{ offer.salesUnit }}</span>
            </div>

            <div class="store-detail-page__offer-sub">
              <span v-if="offer.isComparable && offer.standardPricePer500g != null">
                标准价：¥{{ offer.standardPricePer500g.toFixed(2) }}/500g
              </span>
              <span v-else class="store-detail-page__offer-non-comparable">
                不可比报价
              </span>
              <span>
                库存：{{ offer.availableQuantity }}
              </span>
              <span>
                评分：{{ offer.avgRating.toFixed(1) }}
              </span>
            </div>
          </el-card>
        </div>
      </el-card>
    </div>

    <!-- 评价区域 -->
    <el-card class="store-detail-page__section" v-if="reviewStats">
      <template #header><h3>门店评价</h3></template>
      <div class="store-detail-page__rating-stats">
        <div class="rating-main">
          <span class="rating-score">{{ reviewStats.bayesianRating?.toFixed(1) || reviewStats.avgRating?.toFixed(1) }}</span>
          <StarRating :model-value="reviewStats.avgRating || 0" readonly :size="18" />
          <span class="rating-count">{{ reviewStats.totalRatings }} 条评价</span>
        </div>
        <div class="rating-bars" v-if="reviewStats.distribution">
          <div v-for="(count, star) in reviewStats.distribution" :key="star" class="bar-row">
            <span class="bar-label">{{ star }}星</span>
            <div class="bar-track"><div class="bar-fill" :style="{width: barWidth(count, reviewStats.totalRatings)}"></div></div>
            <span class="bar-count">{{ count }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <ReviewCard
      v-for="review in reviews"
      :key="review.id"
      :review="review"
      class="store-detail-page__review"
      @report="handleReport"
    />

    <div class="store-detail-page__review-empty" v-if="reviews.length === 0 && !reviewStore.loading">
      暂无评价，成为第一个评价的人
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useDiscoveryStore } from '@/stores/discovery'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Shop,
  Location,
  Phone,
  Clock,
  Star,
  StarFilled,
} from '@element-plus/icons-vue'
import DiscoveryMap from '@/components/DiscoveryMap.vue'
import StarRating from '@/components/StarRating.vue'
import ReviewCard from '@/components/ReviewCard.vue'
import { useReviewStore } from '@/stores/review'

const route = useRoute()
const store = useDiscoveryStore()
const reviewStore = useReviewStore()

const storeId = computed(() => Number(route.params.id))
const storeInfo = ref<SearchResult | null>(null)
const offers = ref<SearchResult[]>([])
const error = ref('')

const statusType: Record<string, string> = {
  OPEN: 'success',
  CLOSED: 'danger',
  RESTING: 'warning',
}

const statusLabel: Record<string, string> = {
  OPEN: '营业中',
  CLOSED: '已关闭',
  RESTING: '休息中',
}

const mapStoreData = computed<NearbyStore>(() => ({
  storeId: storeInfo.value?.storeId ?? 0,
  storeName: storeInfo.value?.storeName ?? '',
  address: storeInfo.value?.storeAddress ?? '',
  lat: storeInfo.value?.storeLat ?? 0,
  lng: storeInfo.value?.storeLng ?? 0,
  distance: 0,
  phone: '',
  avgRating: storeInfo.value?.avgRating ?? 0,
}))

async function fetchData() {
  error.value = ''
  try {
    // 搜索该门店的所有报价
    const res = await store.search({
      page: 1,
      size: 50,
      sortBy: 'COMPREHENSIVE',
    })

    // 过滤当前门店的报价
    const storeOffers = res.items.filter((item) => item.storeId === storeId.value)
    offers.value = storeOffers

    if (storeOffers.length > 0) {
      storeInfo.value = storeOffers[0] // 取第一个作为门店信息
    }
  } catch {
    error.value = '加载门店信息失败'
  }
}

async function toggleFavorite() {
  if (store.isFavorite(storeId.value)) {
    const ok = await store.removeFavorite(storeId.value)
    if (ok) ElMessage.success('已取消收藏')
  } else {
    const ok = await store.addFavorite(storeId.value)
    if (ok) ElMessage.success('已收藏')
  }
}

// 评价相关
const reviews = computed(() => reviewStore.storeReviews)
const reviewStats = computed(() => reviewStore.statistics)

async function fetchReviews() {
  if (storeId.value) {
    await reviewStore.fetchStoreReviews(storeId.value)
  }
}

function barWidth(count: number, total: number): string {
  if (total === 0) return '0%'
  return (count / total * 100).toFixed(0) + '%'
}

function handleReport(reviewId: number) {
  ElMessageBox.prompt('请描述举报原因', '举报评价', {
    confirmButtonText: '提交',
    cancelButtonText: '取消',
  }).then(({ value }) => {
    if (value) {
      reviewStore.submitReport(reviewId, value).then(() => ElMessage.success('举报已提交')).catch(() => ElMessage.error('举报失败'))
    }
  }).catch(() => {})
}

watch(storeId, () => {
  fetchData()
})

onMounted(() => {
  store.fetchFavorites()
  fetchData()
  fetchReviews()
})
</script>

<style lang="scss" scoped>
.store-detail-page {
  padding-bottom: $spacing-xl;

  &__content {
    margin-top: $spacing-xl;
    display: flex;
    flex-direction: column;
    gap: $spacing-lg;
  }

  &__info-card {
    .el-card__body {
      padding: $spacing-lg;
    }
  }

  &__info-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-lg;
  }

  &__store-name {
    display: flex;
    align-items: center;
    gap: $spacing-sm;

    h2 {
      font-size: 22px;
      margin: 0;
    }
  }

  &__info-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: $spacing-md;

    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }
  }

  &__info-item {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: 14px;
    color: $text-regular;
  }

  &__map-card {
    height: 400px;
  }

  &__offers-card {
    .el-card__body {
      padding: $spacing-md;
    }
  }

  &__offers-grid {
    display: flex;
    flex-direction: column;
    gap: $spacing-sm;
  }

  &__offer-item {
    border-left: 3px solid $primary-color;
  }

  &__offer-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-sm;
  }

  &__offer-variety {
    font-size: 16px;
    font-weight: 600;
    margin: 0;
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }

  &__offer-price {
    font-size: 22px;
    font-weight: 700;
    color: $danger-color;
  }

  &__offer-meta {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
    margin-bottom: $spacing-sm;
  }

  &__offer-spec {
    font-size: $font-size-sm;
    color: $text-secondary;
    margin-left: auto;
  }

  &__offer-sub {
    display: flex;
    gap: $spacing-lg;
    font-size: $font-size-sm;
    color: $text-secondary;
  }

  &__offer-non-comparable {
    color: $danger-color;
  }
}

.store-detail-page__section {
  margin-top: 16px;
}

.store-detail-page__rating-stats {
  display: flex;
  gap: 32px;
  align-items: flex-start;

  .rating-main {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;

    .rating-score {
      font-size: 36px;
      font-weight: 700;
      color: #f7ba2a;
    }

    .rating-count {
      font-size: 13px;
      color: #909399;
    }
  }

  .rating-bars {
    flex: 1;
    .bar-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;
      font-size: 12px;

      .bar-label { width: 28px; color: #606266; }
      .bar-count { width: 24px; color: #909399; text-align: right; }
      .bar-track {
        flex: 1;
        height: 8px;
        background: #f0f0f0;
        border-radius: 4px;
        .bar-fill {
          height: 100%;
          background: #f7ba2a;
          border-radius: 4px;
        }
      }
    }
  }
}

.store-detail-page__review {
  margin-top: 12px;
}

.store-detail-page__review-empty {
  text-align: center;
  padding: 40px;
  color: #909399;
  font-size: 14px;
}
</style>
