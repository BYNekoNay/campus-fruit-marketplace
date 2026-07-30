<template>
  <div class="discovery-map" ref="mapContainer">
    <!-- 地图网格（灰色背景模拟地图） -->
    <div class="discovery-map__grid">
      <!-- 网格线 -->
      <svg
        class="discovery-map__grid-svg"
        :viewBox="`0 0 ${gridWidth} ${gridHeight}`"
        preserveAspectRatio="none"
      >
        <defs>
          <pattern
            id="grid-pattern"
            width="40"
            height="40"
            patternUnits="userSpaceOnUse"
          >
            <path
              d="M 40 0 L 0 0 0 40"
              fill="none"
              stroke="#d0d7de"
              stroke-width="0.5"
            />
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#grid-pattern)" />
      </svg>

      <!-- 门店标记点 -->
      <div
        v-for="store in positionedStores"
        :key="store.storeId"
        class="discovery-map__marker"
        :class="{
          'discovery-map__marker--selected': store.storeId === selectedStoreId,
        }"
        :style="{ left: store.x + '%', top: store.y + '%' }"
        @click="$emit('select-store', store.storeId)"
      >
        <div class="discovery-map__marker-dot">
          <el-icon :size="14"><LocationFilled /></el-icon>
        </div>
        <div class="discovery-map__marker-tooltip">
          <div class="discovery-map__marker-name">{{ store.storeName }}</div>
          <div class="discovery-map__marker-distance">
            {{ formatDistance(store.distance) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 图例 -->
    <div class="discovery-map__legend">
      <div class="discovery-map__legend-item">
        <span class="discovery-map__legend-dot discovery-map__legend-dot--store"></span>
        <span>门店位置</span>
      </div>
      <div class="discovery-map__legend-item">
        <span class="discovery-map__legend-dot discovery-map__legend-dot--selected"></span>
        <span>已选中</span>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-if="stores.length === 0"
      description="暂无门店数据"
      :image-size="80"
    />

    <!-- 地图加载失败降级 -->
    <div v-if="mapLoadFailed" class="discovery-map__fallback" role="alert">
      <p>地图暂时不可用</p>
      <p class="discovery-map__fallback-hint">请使用下方门店列表查看和选择门店</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { LocationFilled } from '@element-plus/icons-vue'

interface StoreWithPosition extends NearbyStore {
  x: number
  y: number
}

const props = defineProps<{
  stores: NearbyStore[]
  centerLat?: number
  centerLng?: number
  selectedStoreId?: number
}>()

defineEmits<{
  'select-store': [storeId: number]
}>()

const gridWidth = 600
const gridHeight = 400
const mapContainer = ref<HTMLElement>()
const mapLoadFailed = ref(false)

// 将门店坐标映射为网格百分比位置
const positionedStores = computed<StoreWithPosition[]>(() => {
  if (props.stores.length === 0) return []

  const lats = props.stores.map((s) => s.lat)
  const lngs = props.stores.map((s) => s.lng)

  const minLat = Math.min(...lats)
  const maxLat = Math.max(...lats)
  const minLng = Math.min(...lngs)
  const maxLng = Math.max(...lngs)

  const latRange = maxLat - minLat || 0.01
  const lngRange = maxLng - minLng || 0.01
  const padding = 8 // 百分比边距

  return props.stores.map((s) => {
    // 注意：纬度越大越靠上（北），但 CSS top 值越小越靠上
    const xRatio = (s.lng - minLng) / lngRange
    const yRatio = (maxLat - s.lat) / latRange // 反转Y轴

    return {
      ...s,
      x: padding + xRatio * (100 - 2 * padding),
      y: padding + yRatio * (100 - 2 * padding),
    }
  })
})

function formatDistance(km: number): string {
  if (km < 1) return `${Math.round(km * 1000)}m`
  return `${km.toFixed(1)}km`
}
</script>

<style lang="scss" scoped>
.discovery-map {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 500px;
  background: #f0f3f7;
  border-radius: $border-radius;
  overflow: hidden;

  &__grid {
    position: relative;
    width: 100%;
    height: 100%;
    min-height: 500px;
  }

  &__grid-svg {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
  }

  &__marker {
    position: absolute;
    transform: translate(-50%, -100%);
    cursor: pointer;
    z-index: 10;
    transition: transform 0.2s ease;

    &:hover {
      z-index: 20;
      .discovery-map__marker-dot {
        transform: scale(1.3);
        box-shadow: 0 4px 12px rgba(103, 194, 58, 0.5);
      }
      .discovery-map__marker-tooltip {
        opacity: 1;
        visibility: visible;
      }
    }

    &--selected {
      .discovery-map__marker-dot {
        background: #f56c6c;
        border-color: #c0392b;
        box-shadow: 0 0 0 4px rgba(245, 108, 108, 0.25);
      }
    }
  }

  &__marker-dot {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 50% 50% 50% 0;
    background: $primary-color;
    border: 3px solid #fff;
    box-shadow: 0 2px 8px rgba(103, 194, 58, 0.35);
    color: #fff;
    transform: rotate(-45deg);
    transition: transform 0.2s ease;

    .el-icon {
      transform: rotate(45deg);
    }
  }

  &__marker-tooltip {
    position: absolute;
    bottom: calc(100% + 12px);
    left: 50%;
    transform: translateX(-50%);
    background: #fff;
    border-radius: 6px;
    padding: 6px 10px;
    min-width: 100px;
    text-align: center;
    box-shadow: $box-shadow;
    opacity: 0;
    visibility: hidden;
    transition: opacity 0.2s ease;
    white-space: nowrap;

    &::after {
      content: '';
      position: absolute;
      top: 100%;
      left: 50%;
      transform: translateX(-50%);
      border: 5px solid transparent;
      border-top-color: #fff;
    }
  }

  &__marker-name {
    font-size: 12px;
    font-weight: 600;
    color: $text-primary;
  }

  &__marker-distance {
    font-size: 11px;
    color: $text-secondary;
    margin-top: 2px;
  }

  &__legend {
    position: absolute;
    bottom: 12px;
    left: 12px;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 6px;
    padding: 8px 12px;
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: $text-regular;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  }

  &__legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__legend-dot {
    display: inline-block;
    width: 12px;
    height: 12px;
    border-radius: 3px;

    &--store {
      background: $primary-color;
    }

    &--selected {
      background: #f56c6c;
    }
  }
}
</style>
