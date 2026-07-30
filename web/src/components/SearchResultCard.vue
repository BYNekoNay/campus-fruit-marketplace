<template>
  <el-card
    class="search-result-card"
    role="article"
    :aria-label="result ? `${result.fruitVariety} ${result.salesUnit} ¥${result.unitPrice?.toFixed(2)} ${result.storeName}` : '搜索结果'"
    :class="{
      'search-result-card--highlighted': highlighted,
      'search-result-card--skeleton': skeleton,
    }"
    shadow="hover"
    @click="$emit('click-card', result)"
  >
    <template v-if="skeleton">
      <div class="search-result-card__skeleton">
        <el-skeleton :rows="3" animated />
      </div>
    </template>

    <template v-else>
      <div class="search-result-card__main">
        <!-- 左侧：水果信息 -->
        <div class="search-result-card__info">
          <div class="search-result-card__title-row">
            <h3 class="search-result-card__name">{{ result.fruitVariety }}</h3>
            <!-- 价格陈旧警告 -->
            <el-tag
              v-if="result.priceStale"
              type="warning"
              size="small"
              effect="dark"
            >
              价格可能过期
            </el-tag>
            <!-- 冷启动新店标签 -->
            <el-tag
              v-if="result.coldStart"
              type="success"
              size="small"
              effect="plain"
            >
              新店推荐
            </el-tag>
            <!-- 排序解释 -->
            <span v-if="result.rankingReason" class="search-result-card__ranking-reason">
              {{ result.rankingReason }}
            </span>
          </div>
          <div class="search-result-card__meta">
            <el-tag size="small" type="info">{{ result.fruitCategory }}</el-tag>
            <el-tag size="small" type="info">{{ result.fruitGrade }}</el-tag>
            <el-tag size="small" type="info">{{ result.fruitOrigin }}</el-tag>
          </div>
          <div class="search-result-card__spec">
            {{ result.salesUnit }}
            <template v-if="!result.isComparable">
              <span class="search-result-card__non-comparable">按个/盒</span>
            </template>
          </div>
        </div>

        <!-- 右侧：价格信息 -->
        <div class="search-result-card__price">
          <div class="search-result-card__price-main">
            <span class="search-result-card__price-symbol">¥</span>
            <span class="search-result-card__price-value">{{ formatPrice(result.unitPrice) }}</span>
          </div>
          <div class="search-result-card__price-standard" v-if="result.isComparable && result.standardPricePer500g != null">
            ¥{{ result.standardPricePer500g.toFixed(2) }}/500g
          </div>
          <div v-else-if="!result.isComparable" class="search-result-card__price-note">
            不可比报价
          </div>
        </div>
      </div>

      <!-- 底部：门店信息 + 操作 -->
      <div class="search-result-card__footer">
        <div class="search-result-card__store">
          <div class="search-result-card__store-info" @click.stop="$emit('click-store', result.storeId)">
            <el-icon :size="16"><Shop /></el-icon>
            <span class="search-result-card__store-name">{{ result.storeName }}</span>
          </div>
          <div class="search-result-card__store-meta">
            <span v-if="result.distance != null" class="search-result-card__distance">
              <el-icon :size="14"><Location /></el-icon>
              {{ formatDistance(result.distance) }}
            </span>
            <span class="search-result-card__rating">
              <el-rate
                :model-value="result.avgRating"
                disabled
                show-score
                score-template="{value}"
                size="small"
              />
              <span class="search-result-card__review-count">({{ result.reviewCount }})</span>
            </span>
          </div>
        </div>

        <div class="search-result-card__actions" @click.stop>
          <!-- 对比复选框 -->
          <el-checkbox
            :model-value="selected"
            :disabled="disabled"
            @change="$emit('toggle-compare', result.offerId)"
          >
            对比
          </el-checkbox>

          <!-- 收藏心形 -->
          <el-button
            :icon="StarFilled"
            circle
            size="small"
            :type="favorited ? 'danger' : 'default'"
            :class="{ 'search-result-card__fav--active': favorited }"
            @click="$emit('toggle-favorite', result.storeId)"
          />
        </div>
      </div>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import { Shop, Location, StarFilled } from '@element-plus/icons-vue'

defineProps<{
  result: SearchResult
  selected?: boolean
  disabled?: boolean
  favorited?: boolean
  highlighted?: boolean
  skeleton?: boolean
}>()

defineEmits<{
  'click-card': [result: SearchResult]
  'click-store': [storeId: number]
  'toggle-compare': [offerId: number]
  'toggle-favorite': [storeId: number]
}>()

function formatPrice(price: number): string {
  return price.toFixed(2)
}

function formatDistance(km: number): string {
  if (km < 1) return `${Math.round(km * 1000)}m`
  return `${km.toFixed(1)}km`
}
</script>

<style lang="scss" scoped>
.search-result-card {
  margin-bottom: $spacing-sm;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    border-color: $primary-color;
  }

  &--highlighted {
    border-color: $primary-color;
    background-color: rgba(103, 194, 58, 0.04);
    box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.2);
  }

  &--skeleton {
    cursor: default;
  }

  &__main {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: $spacing-sm;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__title-row {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin-bottom: 4px;
  }

  &__name {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $text-primary;
    margin: 0;
  }

  &__meta {
    display: flex;
    gap: 4px;
    margin-bottom: 4px;
    flex-wrap: wrap;
  }

  &__spec {
    font-size: $font-size-sm;
    color: $text-secondary;
  }

  &__non-comparable {
    color: $danger-color;
    font-size: $font-size-sm;
    margin-left: $spacing-xs;
  }

  &__price {
    text-align: right;
    flex-shrink: 0;
    margin-left: $spacing-md;
  }

  &__price-main {
    line-height: 1.2;
  }

  &__price-symbol {
    font-size: 14px;
    color: $danger-color;
    font-weight: 600;
  }

  &__price-value {
    font-size: 22px;
    color: $danger-color;
    font-weight: 700;
  }

  &__price-standard {
    font-size: $font-size-sm;
    color: $text-secondary;
    margin-top: 2px;
  }

  &__price-note {
    font-size: $font-size-sm;
    color: $danger-color;
    margin-top: 2px;
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: $spacing-sm;
    border-top: 1px solid $border-light;
  }

  &__store {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__store-info {
    display: flex;
    align-items: center;
    gap: 4px;
    color: $primary-color;
    padding: 2px 6px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 13px;

    &:hover {
      background-color: rgba(103, 194, 58, 0.08);
    }
  }

  &__store-name {
    font-weight: 500;
  }

  &__store-meta {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    font-size: $font-size-sm;
    color: $text-secondary;
  }

  &__distance {
    display: flex;
    align-items: center;
    gap: 2px;
  }

  &__rating {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  &__review-count {
    font-size: $font-size-sm;
    color: $text-placeholder;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }

  &__fav--active {
    :deep(.el-icon) {
      color: #f56c6c;
    }
  }

  &__ranking-reason {
    font-size: 12px;
    color: #67c23a;
    margin-left: 8px;
    font-weight: 500;
  }
}
</style>
