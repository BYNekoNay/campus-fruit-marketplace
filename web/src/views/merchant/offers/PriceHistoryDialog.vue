<template>
  <el-dialog
    v-model="visible"
    title="价格历史"
    width="500px"
    :close-on-click-modal="true"
    @keydown.esc="visible = false"
  >
    <div v-loading="loading" class="price-history">
      <el-empty v-if="!loading && histories.length === 0" description="暂无价格变更记录" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="(item, index) in histories"
          :key="item.id"
          :timestamp="formatDateTime(item.changedAt)"
          placement="top"
        >
          <div class="price-history__item">
            <div class="price-history__price-row">
              <span class="price-history__new-price">
                ¥{{ centsToYuan(item.unitPrice) }}
              </span>
              <span class="price-history__spec">{{ item.salesUnit }}</span>
              <span
                v-if="index < histories.length - 1"
                :class="[
                  'price-history__arrow',
                  `price-history__arrow--${getDirection(index)}`
                ]"
              >
                {{ getArrowSymbol(index) }}
              </span>
            </div>
            <div class="price-history__time-ago">
              {{ timeAgo(item.changedAt) }}
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useOfferStore } from '@/stores/offer'
import { centsToYuan, formatDateTime, timeAgo, priceChangeDirection } from '@/utils/format'

const props = defineProps<{
  modelValue: boolean
  offerId: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
}>()

const offerStore = useOfferStore()

const visible = ref(false)
const loading = ref(false)
const histories = ref<PriceHistory[]>([])

watch(
  () => props.modelValue,
  async (val) => {
    visible.value = val
    if (val) {
      loading.value = true
      try {
        histories.value = await offerStore.fetchPriceHistory(props.offerId)
      } finally {
        loading.value = false
      }
    }
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

function getDirection(index: number): string {
  if (index >= histories.value.length - 1) return 'same'
  const current = histories.value[index]
  const prev = histories.value[index + 1]
  return priceChangeDirection(prev.unitPrice, current.unitPrice)
}

function getArrowSymbol(index: number): string {
  const dir = getDirection(index)
  if (dir === 'up') return '↑'
  if (dir === 'down') return '↓'
  return '→'
}
</script>

<style lang="scss" scoped>
.price-history {
  min-height: 100px;

  &__item {
    .price-history__price-row {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
    }

    .price-history__new-price {
      font-weight: bold;
      color: $text-primary;
    }

    .price-history__spec {
      font-size: 12px;
      color: $text-secondary;
    }

    .price-history__arrow {
      font-size: 16px;
      font-weight: bold;
      margin-left: 4px;

      &--up {
        color: #F56C6C;
      }

      &--down {
        color: #67C23A;
      }

      &--same {
        color: #909399;
      }
    }

    .price-history__time-ago {
      font-size: 12px;
      color: $text-placeholder;
      margin-top: 2px;
    }
  }
}
</style>
