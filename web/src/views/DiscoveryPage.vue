<template>
  <div class="discovery-page">
    <!-- 定位状态提示 -->
    <div class="discovery-page__location-bar">
      <div v-if="locationStatus === 'located'" class="discovery-page__location-tag discovery-page__location-tag--active">
        <el-icon><LocationFilled /></el-icon>
        <span>当前位置：{{ locationDistrict }}</span>
        <el-button size="small" text @click="revokeLocation">切换</el-button>
      </div>
      <div v-else-if="locationStatus === 'pending'" class="discovery-page__location-tag">
        <el-icon><LocationInformation /></el-icon>
        <span>正在获取位置...</span>
      </div>
      <div v-else class="discovery-page__location-tag">
        <el-icon><LocationInformation /></el-icon>
        <span>点击授权位置，查看附近门店</span>
        <el-button size="small" text type="primary" @click="requestLocationConsent">授权定位</el-button>
      </div>
    </div>

    <!-- 顶部搜索栏 -->
    <div class="discovery-page__toolbar">
      <div class="discovery-page__search-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索水果名称..."
          clearable
          size="large"
          class="discovery-page__search-input"
          @keyup.enter="doSearch"
          @clear="doSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="searchCategory"
          placeholder="全部品类"
          clearable
          size="large"
          style="width: 140px"
          @change="doSearch"
        >
          <el-option
            v-for="cat in store.categories"
            :key="cat.value"
            :label="cat.label"
            :value="cat.value"
          />
        </el-select>

        <el-select
          v-model="sortBy"
          size="large"
          style="width: 140px"
          @change="doSearch"
        >
          <el-option label="综合推荐" value="COMPREHENSIVE" />
          <el-option label="距离最近" value="DISTANCE" />
          <el-option label="价格最低" value="PRICE_ASC" />
          <el-option label="价格最高" value="PRICE_DESC" />
          <el-option label="评分最高" value="RATING" />
          <el-option label="销量优先" value="SALES" />
        </el-select>

        <el-popover placement="bottom" :width="260" trigger="click">
          <template #reference>
            <el-button size="large" :icon="Coin">
              价格区间
              <template v-if="priceRange[0] > 0 || priceRange[1] < 100">(¥{{ priceRange[0] }}-¥{{ priceRange[1] }})</template>
            </el-button>
          </template>
          <div class="discovery-page__price-range">
            <span class="discovery-page__price-label">¥{{ priceRange[0] }}</span>
            <el-slider
              v-model="priceRange"
              range
              :min="0"
              :max="100"
              :step="1"
            />
            <span class="discovery-page__price-label">¥{{ priceRange[1] }}</span>
          </div>
          <div style="text-align: right; margin-top: 8px">
            <el-button size="small" @click="doSearch">确定</el-button>
          </div>
        </el-popover>
      </div>
    </div>

    <!-- 移动端标签切换 -->
    <div class="discovery-page__mobile-tabs">
      <el-radio-group v-model="mobileTab" size="small">
        <el-radio-button value="list">
          <el-icon><List /></el-icon>
          列表
        </el-radio-button>
        <el-radio-button value="map">
          <el-icon><LocationFilled /></el-icon>
          地图
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 主内容区：双栏布局 -->
    <div class="discovery-page__main" :class="{ 'discovery-page__main--map-active': mobileTab === 'map' }">
      <!-- 左侧列表区 -->
      <div
        class="discovery-page__list"
        :class="{ 'discovery-page__list--hidden-mobile': mobileTab === 'map' }"
        ref="listContainerRef"
      >
        <!-- 加载状态 -->
        <template v-if="store.loading">
          <SearchResultCard
            v-for="i in 5"
            :key="i"
            :result="skeletonItem"
            skeleton
          />
        </template>

        <!-- 空状态 -->
        <el-empty
          v-else-if="store.searchResults.length === 0 && searched"
          description="没有找到相关水果，试试其他关键词"
        />

        <!-- 结果列表 -->
        <template v-else>
          <SearchResultCard
            v-for="item in store.searchResults"
            :key="item.offerId"
            :result="item"
            :selected="selectedOfferIds.has(item.offerId)"
            :disabled="selectedOfferIds.size >= maxCompare && !selectedOfferIds.has(item.offerId)"
            :favorited="store.isFavorite(item.storeId)"
            :highlighted="highlightedStoreId === item.storeId"
            @click-card="goToStore(item.storeId)"
            @click-store="goToStore"
            @toggle-compare="toggleCompare"
            @toggle-favorite="handleFavorite"
          />
        </template>

        <!-- 分页 -->
        <div v-if="store.searchTotal > pageSize" class="discovery-page__pagination">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="store.searchTotal"
            layout="prev, pager, next"
            @current-change="doSearch"
          />
        </div>
      </div>

      <!-- 右侧地图区 -->
      <div
        class="discovery-page__map"
        :class="{ 'discovery-page__map--visible-mobile': mobileTab === 'map' }"
      >
        <DiscoveryMap
          :stores="mapStores"
          :selected-store-id="highlightedStoreId"
          @select-store="highlightStore"
        />
      </div>
    </div>

    <!-- 底部比价栏 -->
    <transition name="slide-up">
      <div v-if="selectedOfferIds.size > 0" class="discovery-page__compare-bar">
        <div class="discovery-page__compare-bar-inner">
          <div class="discovery-page__compare-info">
            <el-icon><Goods /></el-icon>
            <span>已选择 <strong>{{ selectedOfferIds.size }}</strong> / {{ maxCompare }} 个报价进行对比</span>
          </div>
          <div class="discovery-page__compare-actions">
            <el-button @click="clearCompare">清空</el-button>
            <el-button
              type="primary"
              :disabled="selectedOfferIds.size < 2"
              @click="openCompare"
            >
              开始比价（{{ selectedOfferIds.size }}）
            </el-button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 比价弹窗 -->
    <CompareDialog
      v-model="compareDialogVisible"
      :compare-data="store.compareData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDiscoveryStore } from '@/stores/discovery'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import {
  Search,
  Coin,
  List,
  LocationFilled,
  LocationInformation,
  Goods,
} from '@element-plus/icons-vue'
import DiscoveryMap from '@/components/DiscoveryMap.vue'
import SearchResultCard from '@/components/SearchResultCard.vue'
import CompareDialog from '@/components/CompareDialog.vue'

const route = useRoute()
const router = useRouter()
const store = useDiscoveryStore()
const authStore = useAuthStore()

// 搜索参数
const searchKeyword = ref('')
const searchCategory = ref('')
const sortBy = ref('COMPREHENSIVE')
const priceRange = ref([0, 100])
const currentPage = ref(1)
const pageSize = 20
const searched = ref(false)

// 对比选择
const selectedOfferIds = ref(new Set<number>())
const maxCompare = 5
const compareDialogVisible = ref(false)

// 地图联动
const highlightedStoreId = ref<number | undefined>(undefined)

// 移动端
const mobileTab = ref<'list' | 'map'>('list')

// 定位状态
const locationStatus = ref<'idle' | 'pending' | 'located' | 'denied'>('idle')
const locationDistrict = ref('')
const currentLat = ref<number | undefined>(undefined)
const currentLng = ref<number | undefined>(undefined)

// Skeleton 占位
const skeletonItem: SearchResult = {
  offerId: 0,
  storeId: 0,
  storeName: '',
  storeLat: 0,
  storeLng: 0,
  distance: null,
  fruitVariety: '加载中...',
  fruitCategory: '',
  fruitGrade: '',
  fruitOrigin: '',
  salesUnit: '',
  unitPrice: 0,
  standardPricePer500g: null,
  isComparable: false,
  availableQuantity: 0,
  avgRating: 0,
  reviewCount: 0,
  priceStale: false,
  storeStatus: '',
  storeAddress: '',
}

// 地图标记点数据
const mapStores = computed<NearbyStore[]>(() => {
  return store.searchResults.map((item) => ({
    storeId: item.storeId,
    storeName: item.storeName,
    address: item.storeAddress,
    lat: item.storeLat,
    lng: item.storeLng,
    distance: item.distance ?? 0,
    phone: '',
    avgRating: item.avgRating,
  }))
})

// 搜索
async function doSearch() {
  currentPage.value = 1
  await store.search({
    keyword: searchKeyword.value || undefined,
    category: searchCategory.value || undefined,
    sortBy: sortBy.value as SearchRequest['sortBy'],
    minPrice: priceRange.value[0] > 0 ? priceRange.value[0] : undefined,
    maxPrice: priceRange.value[1] < 100 ? priceRange.value[1] : undefined,
    lat: currentLat.value,
    lng: currentLng.value,
    page: currentPage.value,
    size: pageSize,
  })
  searched.value = true
  highlightedStoreId.value = undefined
}

// 对比切换
function toggleCompare(offerId: number) {
  if (selectedOfferIds.value.has(offerId)) {
    selectedOfferIds.value.delete(offerId)
  } else {
    if (selectedOfferIds.value.size >= maxCompare) {
      ElMessage.warning(`最多选择 ${maxCompare} 个报价进行对比`)
      return
    }
    selectedOfferIds.value.add(offerId)
    ElMessage.success(`已选择 ${selectedOfferIds.value.size} 个报价`)
  }
}

function clearCompare() {
  selectedOfferIds.value = new Set()
}

async function openCompare() {
  const ids = Array.from(selectedOfferIds.value)
  compareDialogVisible.value = true
  await store.compare(ids)
}

// 收藏切换
async function handleFavorite(storeId: number) {
  if (store.isFavorite(storeId)) {
    const ok = await store.removeFavorite(storeId)
    if (ok) ElMessage.success('已取消收藏')
  } else {
    const ok = await store.addFavorite(storeId)
    if (ok) ElMessage.success('已收藏')
  }
}

// 地图高亮
function highlightStore(storeId: number) {
  highlightedStoreId.value = storeId
  mobileTab.value = 'list' // 移动端切回列表
  // 滚动到对应卡片
  nextTick(() => {
    const index = store.searchResults.findIndex((r) => r.storeId === storeId)
    if (index !== -1) {
      const cards = document.querySelectorAll('.search-result-card')
      if (cards[index]) {
        cards[index].scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    }
  })
}

// 跳转门店详情
function goToStore(storeId: number) {
  router.push({ name: 'StoreDetail', params: { id: storeId } })
}

// ==================== 定位相关 ====================

/**
 * 检查服务端定位 consent 状态，若已授权则触发浏览器定位。
 */
async function checkAndRequestLocation() {
  if (!authStore.isAuthenticated) {
    locationStatus.value = 'idle'
    return
  }

  try {
    const res: any = await request.get('/me/consent/LOCATION')
    const status = res?.data?.status

    if (status === 'GRANTED') {
      locationStatus.value = 'pending'
      await requestBrowserLocation()
    } else {
      locationStatus.value = 'idle'
    }
  } catch {
    // 服务端不可达或未认证，回退到静默模式
    locationStatus.value = 'idle'
  }
}

/**
 * 请求浏览器定位。
 * 前端不发送精确定位到日志/tracking，仅用于当次搜索请求。
 */
async function requestBrowserLocation() {
  if (!('geolocation' in navigator)) {
    locationStatus.value = 'idle'
    return
  }

  try {
    locationStatus.value = 'pending'
    const position = await new Promise<GeolocationPosition>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: false,
        timeout: 8000,
        maximumAge: 300000,
      })
    })

    const { latitude, longitude } = position.coords
    currentLat.value = latitude
    currentLng.value = longitude
    locationStatus.value = 'located'

    // 仅用于展示模糊区域，不存储精确坐标
    locationDistrict.value = formatDistrict(latitude, longitude)
  } catch (err: any) {
    if (err?.code === err?.PERMISSION_DENIED) {
      locationStatus.value = 'denied'
    } else {
      locationStatus.value = 'idle'
    }
    // 清除坐标
    currentLat.value = undefined
    currentLng.value = undefined
  }
}

/**
 * 向服务端发起定位授权请求。
 */
async function requestLocationConsent() {
  if (!authStore.isAuthenticated) {
    ElMessage.info('请先登录后再授权定位')
    return
  }

  try {
    await request.put('/me/consent/LOCATION/grant')
    ElMessage.success('定位授权成功')
    await requestBrowserLocation()
  } catch {
    ElMessage.error('授权失败，请稍后重试')
  }
}

/**
 * 撤销定位授权。
 */
async function revokeLocation() {
  try {
    await request.put('/me/consent/LOCATION/revoke')
    currentLat.value = undefined
    currentLng.value = undefined
    locationDistrict.value = ''
    locationStatus.value = 'idle'
    ElMessage.success('已撤销定位授权')
  } catch {
    ElMessage.error('撤销失败')
  }
}

/**
 * 模糊格式化坐标为区域名（不存储精确值）。
 * 仅用于前端展示。
 */
function formatDistrict(_lat: number, _lng: number): string {
  // 不显示精确坐标，暂时显示为"附近区域"
  // 后续可接入逆地理编码服务显示模糊区名
  return '附近区域'
}

// 监听 URL query 参数变化
watch(
  () => route.query.keyword,
  (val) => {
    if (val && typeof val === 'string') {
      searchKeyword.value = val
      doSearch()
    }
  },
  { immediate: true }
)

onMounted(() => {
  store.fetchCategories()
  store.fetchFavorites()
  checkAndRequestLocation()
})
</script>

<style lang="scss" scoped>
.discovery-page {
  padding-bottom: 80px;

  &__location-bar {
    margin-bottom: $spacing-sm;
  }

  &__location-tag {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    background: #f5f7fa;
    border-radius: 20px;
    font-size: $font-size-sm;
    color: $text-secondary;

    &--active {
      background: rgba(103, 194, 58, 0.08);
      color: $primary-color;
    }
  }

  &__toolbar {
    background: #fff;
    border-radius: $border-radius;
    padding: $spacing-md;
    margin-bottom: $spacing-lg;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  }

  &__search-row {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    flex-wrap: wrap;
  }

  &__search-input {
    flex: 1;
    min-width: 200px;
  }

  &__price-range {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }

  &__price-label {
    font-size: $font-size-sm;
    color: $text-secondary;
    white-space: nowrap;
    min-width: 40px;
  }

  &__mobile-tabs {
    display: none;
    margin-bottom: $spacing-md;
    justify-content: center;

    @media (max-width: 768px) {
      display: flex;
    }
  }

  &__main {
    display: flex;
    gap: $spacing-md;
    min-height: 500px;
  }

  &__list {
    flex: 3;
    min-width: 0;
    overflow-y: auto;
    max-height: calc(100vh - 260px);

    &--hidden-mobile {
      @media (max-width: 768px) {
        display: none;
      }
    }
  }

  &__map {
    flex: 2;
    min-width: 300px;
    position: sticky;
    top: 76px;
    height: calc(100vh - 260px);
    min-height: 500px;

    @media (max-width: 768px) {
      display: none;
      position: relative;
      top: 0;
      height: calc(100vh - 200px);
    }

    &--visible-mobile {
      @media (max-width: 768px) {
        display: block;
      }
    }
  }

  &__main--map-active {
    .discovery-page__list {
      @media (max-width: 768px) {
        display: none;
      }
    }
  }

  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: $spacing-lg;
  }

  &__compare-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    background: #fff;
    border-top: 2px solid $primary-color;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.1);
    z-index: 100;
    padding: $spacing-sm 0;
  }

  &__compare-bar-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 $spacing-lg;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__compare-info {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: 14px;
    color: $text-regular;

    strong {
      color: $primary-color;
    }
  }

  &__compare-actions {
    display: flex;
    gap: $spacing-sm;
  }
}

// 过渡动画
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
</style>
