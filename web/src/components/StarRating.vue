<template>
  <div class="star-rating" :class="{ readonly }" role="radiogroup" :aria-label="`评分 ${currentValue} 星（共 5 星）`">
    <span
      v-for="star in 5"
      :key="star"
      class="star"
      :class="{ active: star <= currentValue, hover: star <= hoverValue }"
      :style="{ fontSize: size + 'px' }"
      @click="!readonly && setRating(star)"
      @mouseenter="!readonly && (hoverValue = star)"
      @mouseleave="!readonly && (hoverValue = 0)"
    >
      {{ star <= (hoverValue || currentValue) ? '★' : '☆' }}
    </span>
    <span v-if="!readonly" class="rating-text">{{ currentValue > 0 ? currentValue + '分' : '评分' }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: number
  readonly?: boolean
  size?: number
}>(), {
  readonly: false,
  size: 24
})

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

const currentValue = ref(props.modelValue || 0)
const hoverValue = ref(0)

watch(() => props.modelValue, (val) => {
  currentValue.value = val || 0
})

function setRating(star: number) {
  currentValue.value = star
  emit('update:modelValue', star)
}
</script>

<style scoped lang="scss">
.star-rating {
  display: inline-flex;
  align-items: center;
  gap: 2px;

  .star {
    cursor: pointer;
    color: #c0c4cc;
    transition: color 0.2s, transform 0.15s;
    user-select: none;

    &.active {
      color: #f7ba2a;
    }

    &.hover {
      color: #f7ba2a;
      transform: scale(1.15);
    }
  }

  &.readonly .star {
    cursor: default;
  }

  .rating-text {
    margin-left: 8px;
    font-size: 14px;
    color: #909399;
  }
}
</style>
