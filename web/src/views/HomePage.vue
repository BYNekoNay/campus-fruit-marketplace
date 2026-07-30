<template>
  <div class="home-page">
    <!-- 搜索区域 -->
    <section class="home-page__hero">
      <div class="home-page__hero-content">
        <h1 class="home-page__title">
          <template v-if="authStore.isAuthenticated">
            {{ greeting }}，{{ authStore.nickname }}
          </template>
          <template v-else>
            新鲜水果，直达校园
          </template>
        </h1>
        <p class="home-page__subtitle">
          <template v-if="!authStore.isAuthenticated">
            发现身边的优质水果门店，下单即达
          </template>
          <template v-else>
            今天想买点什么水果呢？
          </template>
        </p>
        <SearchBar
          class="home-page__search"
          placeholder="搜索水果、门店..."
          @search="handleSearch"
        />
      </div>
    </section>

    <!-- 未登录引导 -->
    <section v-if="!authStore.isAuthenticated" class="home-page__section">
      <el-card class="home-page__welcome-card">
        <div class="home-page__welcome-content">
          <el-icon :size="48" color="#67C23A"><Apple /></el-icon>
          <div>
            <h3>开启你的校园水果之旅</h3>
            <p>登录即可享受专属优惠和便捷下单体验</p>
          </div>
          <div class="home-page__welcome-actions">
            <el-button type="primary" @click="$router.push('/auth/login')">
              立即登录
            </el-button>
            <el-button @click="$router.push('/auth/register')">
              注册账号
            </el-button>
          </div>
        </div>
      </el-card>
    </section>

    <!-- 热门水果分类快捷入口 -->
    <section class="home-page__section">
      <h2 class="home-page__section-title">热门分类</h2>
      <div class="home-page__categories">
        <div
          v-for="cat in fruitCategories"
          :key="cat.value"
          class="home-page__category-item"
          @click="$router.push({ name: 'Discovery', query: { keyword: cat.label } })"
        >
          <div class="home-page__category-icon">
            <el-icon :size="28" :color="cat.color"><Apple /></el-icon>
          </div>
          <span class="home-page__category-name">{{ cat.label }}</span>
        </div>
      </div>
    </section>

    <!-- 附近门店快捷卡片（横向滚动） -->
    <section class="home-page__section">
      <div class="home-page__section-header">
        <h2 class="home-page__section-title">附近门店</h2>
        <el-button text type="primary" @click="$router.push('/discovery')">
          查看更多
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <!-- 加载中 -->
      <el-skeleton v-if="nearbyLoading" :rows="2" animated>
        <template #template>
          <div class="home-page__nearby-skeleton">
            <el-skeleton-item
              v-for="i in 3"
              :key="i"
              variant="rect"
              style="width: 260px; height: 140px; border-radius: 8px"
            />
          </div>
        </template>
      </el-skeleton>

      <!-- 附近门店横向滚动 -->
      <div v-else-if="discoveryStore.nearbyStores.length > 0" class="home-page__nearby-scroll">
        <el-card
          v-for="store in discoveryStore.nearbyStores"
          :key="store.storeId"
          shadow="hover"
          class="home-page__nearby-card"
          @click="$router.push({ name: 'StoreDetail', params: { id: store.storeId } })"
        >
          <div class="home-page__nearby-card-header">
            <el-avatar :size="40" shape="square">
              <el-icon :size="20"><Shop /></el-icon>
            </el-avatar>
            <div>
              <div class="home-page__nearby-card-name">{{ store.storeName }}</div>
              <el-rate
                :model-value="store.avgRating"
                disabled
                size="small"
                show-score
                score-template="{value}"
              />
            </div>
          </div>
          <div class="home-page__nearby-card-info">
            <div class="home-page__nearby-card-distance">
              <el-icon :size="14"><Location /></el-icon>
              {{ formatDistance(store.distance) }}
            </div>
            <div class="home-page__nearby-card-address">{{ store.address }}</div>
          </div>
        </el-card>
      </div>

      <!-- 无附近门店 -->
      <el-empty
        v-else
        description="暂无附近门店"
        :image-size="60"
      />
    </section>

    <!-- 热门水果推荐 -->
    <section class="home-page__section">
      <h2 class="home-page__section-title">热门水果推荐</h2>
      <el-row :gutter="16">
        <el-col
          v-for="i in 8"
          :key="i"
          :xs="12"
          :sm="8"
          :md="6"
          :lg="6"
        >
          <el-card class="fruit-card" shadow="hover" @click="$router.push('/discovery')">
            <div class="fruit-card__image">
              <el-icon :size="48" color="#67C23A"><Apple /></el-icon>
            </div>
            <div class="fruit-card__info">
              <h3 class="fruit-card__name">新鲜水果 {{ i }}</h3>
              <div class="fruit-card__price">
                <span class="fruit-card__price-current">¥9.9</span>
                <span class="fruit-card__price-original">¥19.9</span>
              </div>
              <span class="fruit-card__tag">热销</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDiscoveryStore } from '@/stores/discovery'
import SearchBar from '@/components/SearchBar.vue'
import request from '@/utils/request'
import {
  Apple,
  Shop,
  ArrowRight,
  Location,
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const discoveryStore = useDiscoveryStore()

const nearbyLoading = ref(false)

// 热门水果分类
const fruitCategories = [
  { label: '当季水果', value: 'seasonal', color: '#67C23A' },
  { label: '热带水果', value: 'tropical', color: '#E6A23C' },
  { label: '浆果类', value: 'berries', color: '#409EFF' },
  { label: '柑橘类', value: 'citrus', color: '#F56C6C' },
  { label: '进口水果', value: 'imported', color: '#909399' },
  { label: '瓜类', value: 'melon', color: '#67C23A' },
]

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

function handleSearch(keyword: string) {
  router.push({ name: 'Discovery', query: { keyword } })
}

function formatDistance(km: number): string {
  if (km < 1) return `${Math.round(km * 1000)}m`
  return `${km.toFixed(1)}km`
}

// 获取用户位置并加载附近门店
async function loadNearbyStores() {
  if (!authStore.isAuthenticated) return

  // 先检查服务端授权状态
  try {
    const consentRes: any = await request.get('/me/consent/LOCATION')
    const consentStatus = consentRes?.data?.status
    if (consentStatus !== 'GRANTED') {
      // 用户未授权定位，不自动请求浏览器定位
      return
    }
  } catch {
    // 服务端不可达，回退
    return
  }

  if (!('geolocation' in navigator)) return

  try {
    nearbyLoading.value = true
    const position = await new Promise<GeolocationPosition>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: false,
        timeout: 5000,
        maximumAge: 300000, // 5分钟缓存
      })
    })

    await discoveryStore.fetchNearby(
      position.coords.latitude,
      position.coords.longitude
    )
  } catch {
    // 用户拒绝定位或获取失败，静默处理
  } finally {
    nearbyLoading.value = false
  }
}

onMounted(() => {
  discoveryStore.fetchFavorites()
  loadNearbyStores()
})
</script>

<style lang="scss" scoped>
.home-page {
  &__hero {
    background: linear-gradient(135deg, $primary-color 0%, $primary-light 100%);
    border-radius: $border-radius;
    padding: $spacing-xxl $spacing-lg;
    margin-bottom: $spacing-xl;
    text-align: center;
    color: #fff;
  }

  &__hero-content {
    max-width: 600px;
    margin: 0 auto;
  }

  &__title {
    font-size: 32px;
    margin-bottom: $spacing-sm;
  }

  &__subtitle {
    font-size: 16px;
    opacity: 0.9;
    margin-bottom: $spacing-xl;
  }

  &__search {
    width: 100%;
  }

  &__section {
    margin-bottom: $spacing-xl;
  }

  &__section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-lg;
  }

  &__section-title {
    font-size: 20px;
    font-weight: 600;
    margin-bottom: $spacing-lg;
    padding-left: $spacing-sm;
    border-left: 3px solid $primary-color;

    .home-page__section-header & {
      margin-bottom: 0;
    }
  }

  &__welcome-card {
    margin-bottom: $spacing-lg;
  }

  &__welcome-content {
    display: flex;
    align-items: center;
    gap: $spacing-lg;
    flex-wrap: wrap;

    h3 {
      font-size: 18px;
      margin-bottom: $spacing-xs;
    }

    p {
      color: $text-secondary;
      font-size: 14px;
    }
  }

  &__welcome-actions {
    margin-left: auto;
    display: flex;
    gap: $spacing-sm;
  }

  &__categories {
    display: flex;
    gap: $spacing-md;
    flex-wrap: wrap;
  }

  &__category-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: $spacing-xs;
    cursor: pointer;
    padding: $spacing-md;
    border-radius: $border-radius;
    background: $bg-color;
    transition: all $transition-fast;
    min-width: 80px;

    &:hover {
      background: rgba(103, 194, 58, 0.08);
      transform: translateY(-2px);
    }
  }

  &__category-icon {
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff;
    border-radius: 12px;
  }

  &__category-name {
    font-size: $font-size-sm;
    color: $text-regular;
    font-weight: 500;
  }

  &__nearby-scroll {
    display: flex;
    gap: $spacing-md;
    overflow-x: auto;
    padding-bottom: $spacing-sm;
    scroll-snap-type: x mandatory;
    -webkit-overflow-scrolling: touch;

    &::-webkit-scrollbar {
      height: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: $border-color;
      border-radius: 2px;
    }
  }

  &__nearby-card {
    min-width: 260px;
    max-width: 300px;
    cursor: pointer;
    scroll-snap-align: start;
    flex-shrink: 0;
    transition: transform $transition-fast;

    &:hover {
      transform: translateY(-4px);
    }
  }

  &__nearby-card-header {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin-bottom: $spacing-sm;
  }

  &__nearby-card-name {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 2px;
    color: $text-primary;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 160px;
  }

  &__nearby-card-info {
    font-size: $font-size-sm;
    color: $text-secondary;
  }

  &__nearby-card-distance {
    display: flex;
    align-items: center;
    gap: 4px;
    color: $primary-color;
    font-weight: 500;
    margin-bottom: 4px;
  }

  &__nearby-card-address {
    font-size: $font-size-sm;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 240px;
  }

  &__nearby-skeleton {
    display: flex;
    gap: $spacing-md;
    overflow: hidden;
  }
}

.fruit-card {
  margin-bottom: $spacing-md;
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-4px);
  }

  &__image {
    height: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: $bg-color;
    border-radius: $border-radius;
    margin-bottom: $spacing-sm;
  }

  &__name {
    font-size: 14px;
    margin-bottom: $spacing-xs;
  }

  &__price {
    display: flex;
    align-items: baseline;
    gap: $spacing-xs;
  }

  &__price-current {
    color: $danger-color;
    font-size: 18px;
    font-weight: bold;
  }

  &__price-original {
    color: #999;
    font-size: 12px;
    text-decoration: line-through;
  }

  &__tag {
    display: inline-block;
    font-size: 11px;
    color: #fff;
    background: $danger-color;
    padding: 1px 6px;
    border-radius: 4px;
    margin-top: $spacing-xs;
  }
}
</style>
