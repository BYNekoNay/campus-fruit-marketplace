<template>
  <div class="search-bar" :class="{ 'search-bar--large': large }">
    <el-input
      v-model="keyword"
      :placeholder="placeholder"
      :size="large ? 'large' : 'default'"
      clearable
      @keyup.enter="handleSearch"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
      <template #append>
        <el-button
          :icon="Search"
          :type="large ? 'warning' : 'primary'"
          @click="handleSearch"
        >
          {{ large ? '搜索' : '' }}
        </el-button>
      </template>
    </el-input>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

defineProps<{
  placeholder?: string
  large?: boolean
}>()

const emit = defineEmits<{
  search: [keyword: string]
}>()

const keyword = ref('')

function handleSearch() {
  const trimmed = keyword.value.trim()
  if (trimmed) {
    emit('search', trimmed)
  }
}
</script>

<style lang="scss" scoped>
.search-bar {
  width: 100%;

  &--large {
    .el-input {
      --el-input-bg-color: rgba(255, 255, 255, 0.95);
    }
  }
}
</style>
