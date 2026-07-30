<template>
  <div class="map-picker">
    <div class="map-picker__display" @click="showMapDialog = true">
      <div class="map-picker__map-area">
        <template v-if="latitude !== 0 && longitude !== 0">
          <div class="map-picker__pin">
            <el-icon :size="24" color="#F56C6C"><MapLocation /></el-icon>
          </div>
          <div class="map-picker__coord-label">
            {{ latitude.toFixed(6) }}, {{ longitude.toFixed(6) }}
          </div>
        </template>
        <template v-else>
          <el-icon :size="40" color="#C0C4CC"><MapLocation /></el-icon>
          <p class="map-picker__placeholder">点击此处在地图上选择门店位置</p>
        </template>
      </div>
    </div>

    <!-- 手动输入（降级方案） -->
    <div class="map-picker__manual">
      <el-input
        v-model="localLat"
        placeholder="纬度 (如 39.915)"
        size="default"
        class="map-picker__input"
      >
        <template #prepend>纬度</template>
      </el-input>
      <el-input
        v-model="localLng"
        placeholder="经度 (如 116.404)"
        size="default"
        class="map-picker__input"
      >
        <template #prepend>经度</template>
      </el-input>
    </div>

    <!-- 地图选点弹窗 -->
    <el-dialog
      v-model="showMapDialog"
      title="选择门店位置"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="map-dialog">
        <!--
          TODO: 集成百度地图 JavaScript API GL 或 Leaflet
          百度地图接入步骤：
          1. 注册百度地图开放平台账号 (https://lbsyun.baidu.com/)
          2. 创建应用并获取 AK (Access Key)
          3. 在 index.html 引入 Baidu Map GL JS SDK:
            <script src="//api.map.baidu.com/api?type=webgl&v=1.0&ak=你的AK"></script>
          4. 使用 BMapGL.Map 初始化地图并实现选点功能
          
          当前使用模拟地图界面用于开发和演示。
        -->
        <div class="map-dialog__mock">
          <div
            class="map-dialog__mock-area"
            @click="handleMapClick"
          >
            <!-- 模拟网格线 -->
            <div class="map-dialog__grid">
              <div
                v-for="i in 8"
                :key="'h' + i"
                class="map-dialog__grid-line map-dialog__grid-line--h"
                :style="{ top: (i * 12.5) + '%' }"
              />
              <div
                v-for="i in 8"
                :key="'v' + i"
                class="map-dialog__grid-line map-dialog__grid-line--v"
                :style="{ left: (i * 12.5) + '%' }"
              />
            </div>

            <!-- 模拟一些道路 -->
            <div class="map-dialog__road map-dialog__road--h" style="top: 37%; left: 0; width: 100%;" />
            <div class="map-dialog__road map-dialog__road--v" style="left: 42%; top: 0; height: 100%;" />
            <div class="map-dialog__road map-dialog__road--h" style="top: 68%; left: 0; width: 100%;" />

            <!-- 建筑方块 -->
            <div
              v-for="block in mockBuildings"
              :key="block.id"
              class="map-dialog__building"
              :style="{
                left: block.x + '%',
                top: block.y + '%',
                width: block.w + '%',
                height: block.h + '%',
              }"
            />

            <!-- 水印文字 -->
            <div class="map-dialog__watermark">模拟地图 — 点击选择位置</div>

            <!-- 选点标记 -->
            <div
              v-if="tempLat !== 0"
              class="map-dialog__marker"
              :style="{
                left: ((tempLng + 180) / 360 * 100) + '%',
                top: ((90 - tempLat) / 180 * 100) + '%',
              }"
            >
              <el-icon :size="28" color="#F56C6C"><MapLocation /></el-icon>
            </div>
          </div>
        </div>

        <div class="map-dialog__info" v-if="tempLat !== 0">
          <el-tag>纬度: {{ tempLat.toFixed(6) }}</el-tag>
          <el-tag type="success">经度: {{ tempLng.toFixed(6) }}</el-tag>
        </div>
        <div class="map-dialog__info" v-else>
          <span class="map-dialog__hint">点击地图设置门店位置</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="showMapDialog = false">取消</el-button>
        <el-button type="primary" :disabled="tempLat === 0" @click="confirmMapPick">
          确认选择
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { MapLocation } from '@element-plus/icons-vue'

const props = defineProps<{
  latitude: number
  longitude: number
  address?: string
}>()

const emit = defineEmits<{
  'update:latitude': [value: number]
  'update:longitude': [value: number]
}>()

const showMapDialog = ref(false)
const tempLat = ref(0)
const tempLng = ref(0)
const localLat = ref('')
const localLng = ref('')

// 模拟建筑方块数据
const mockBuildings = Array.from({ length: 15 }, (_, i) => ({
  id: i + 1,
  x: 5 + (i % 5) * 18 + Math.random() * 5,
  y: 5 + Math.floor(i / 5) * 22 + Math.random() * 5,
  w: 10 + Math.random() * 10,
  h: 10 + Math.random() * 15,
}))

// 初始化本地输入框
watch(
  () => [props.latitude, props.longitude],
  ([lat, lng]) => {
    localLat.value = lat && lat !== 0 ? String(lat) : ''
    localLng.value = lng && lng !== 0 ? String(lng) : ''
  },
  { immediate: true }
)

// 手动输入双向绑定到 props
watch(localLat, (val) => {
  const num = parseFloat(val)
  if (!isNaN(num) && num >= -90 && num <= 90) {
    emit('update:latitude', num)
  }
})

watch(localLng, (val) => {
  const num = parseFloat(val)
  if (!isNaN(num) && num >= -180 && num <= 180) {
    emit('update:longitude', num)
  }
})

function handleMapClick(e: MouseEvent) {
  const target = e.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  // 计算点击位置相对于地图区域的比例
  const xRatio = (e.clientX - rect.left) / rect.width
  const yRatio = (e.clientY - rect.top) / rect.height

  // 模拟: 假设地图范围大致覆盖中国区域 (73-135 经度, 18-54 纬度)
  tempLng.value = 73.0 + xRatio * (135.0 - 73.0)
  tempLat.value = 54.0 - yRatio * (54.0 - 18.0)
}

function confirmMapPick() {
  emit('update:latitude', parseFloat(tempLat.value.toFixed(6)))
  emit('update:longitude', parseFloat(tempLng.value.toFixed(6)))
  localLat.value = String(tempLat.value.toFixed(6))
  localLng.value = String(tempLng.value.toFixed(6))
  showMapDialog.value = false
}

// 打开地图时，用当前坐标初始化临时值
watch(showMapDialog, (open) => {
  if (open) {
    if (props.latitude !== 0 && props.longitude !== 0) {
      tempLat.value = props.latitude
      tempLng.value = props.longitude
    } else {
      tempLat.value = 0
      tempLng.value = 0
    }
  }
})
</script>

<style lang="scss" scoped>
.map-picker {
  width: 100%;

  &__display {
    cursor: pointer;
    margin-bottom: $spacing-sm;
  }

  &__map-area {
    width: 100%;
    height: 150px;
    background-color: #f0f2f5;
    border: 2px dashed $border-color;
    border-radius: $border-radius;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    transition: border-color $transition-fast;
    position: relative;
    overflow: hidden;

    &:hover {
      border-color: $primary-color;
    }
  }

  &__pin {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -80%);
  }

  &__coord-label {
    margin-top: 40px;
    font-size: 14px;
    font-weight: 500;
    color: $text-regular;
    font-family: monospace;
  }

  &__placeholder {
    color: $text-secondary;
    font-size: 13px;
    margin-top: $spacing-sm;
  }

  &__manual {
    display: flex;
    gap: $spacing-sm;
  }

  &__input {
    flex: 1;
  }
}

.map-dialog {
  &__mock {
    border-radius: $border-radius;
    overflow: hidden;
  }

  &__mock-area {
    width: 100%;
    height: 350px;
    background-color: #e8f0e8;
    position: relative;
    overflow: hidden;
    cursor: crosshair;
  }

  &__grid {
    position: absolute;
    inset: 0;
    pointer-events: none;
  }

  &__grid-line {
    position: absolute;
    background-color: rgba(0, 0, 0, 0.04);

    &--h {
      left: 0;
      width: 100%;
      height: 1px;
    }

    &--v {
      top: 0;
      width: 1px;
      height: 100%;
    }
  }

  &__road {
    position: absolute;
    background-color: #d4d4c8;

    &--h {
      height: 3px;
    }

    &--v {
      width: 3px;
    }
  }

  &__building {
    position: absolute;
    background-color: #ccc;
    border-radius: 2px;
    box-shadow: 0 0 2px rgba(0, 0, 0, 0.1);
  }

  &__watermark {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    color: rgba(0, 0, 0, 0.08);
    font-size: 28px;
    font-weight: bold;
    pointer-events: none;
    white-space: nowrap;
  }

  &__marker {
    position: absolute;
    transform: translate(-50%, -100%);
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
    z-index: 10;
    pointer-events: none;
  }

  &__info {
    display: flex;
    justify-content: center;
    gap: $spacing-md;
    margin-top: $spacing-md;
    padding: $spacing-sm;
  }

  &__hint {
    color: $text-secondary;
    font-size: 13px;
  }
}
</style>
