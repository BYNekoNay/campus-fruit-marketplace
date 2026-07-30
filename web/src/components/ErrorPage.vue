<template>
  <div class="error-page" role="alert">
    <div class="error-page__inner">
      <div class="error-page__icon">{{ icon }}</div>
      <h1 class="error-page__code">{{ code }}</h1>
      <h2 class="error-page__title">{{ title }}</h2>
      <p class="error-page__desc">{{ message }}</p>
      <div class="error-page__actions">
        <el-button type="primary" @click="handleRetry" v-if="showRetry">重新加载</el-button>
        <el-button @click="$router.push('/')">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  type?: 'network' | 'server' | 'notfound' | 'forbidden' | 'empty'
  message?: string
  showRetry?: boolean
}>(), {
  type: 'server',
  showRetry: false
})

const emit = defineEmits<{ retry: [] }>()

const config = computed(() => {
  const map = {
    network: { icon: '📡', code: '网络', title: '网络连接失败', desc: '请检查网络后重试', retry: true },
    server: { icon: '🔧', code: '500', title: '服务暂不可用', desc: '服务器开小差了，请稍后再试', retry: true },
    notfound: { icon: '🔍', code: '404', title: '页面不存在', desc: '你访问的页面可能已移除或地址输入有误', retry: false },
    forbidden: { icon: '🔒', code: '403', title: '无访问权限', desc: '你没有权限访问此页面，请联系管理员', retry: false },
    empty: { icon: '📭', code: '暂无', title: '暂无数据', desc: '当前没有可显示的内容', retry: false }
  }
  return map[props.type]
})

const icon = computed(() => config.value.icon)
const code = computed(() => config.value.code)
const title = computed(() => props.message || config.value.title)
const message = computed(() => props.message ? '' : config.value.desc)

function handleRetry() {
  emit('retry')
  window.location.reload()
}
</script>

<style scoped lang="scss">
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 40px 16px;
  text-align: center;

  &__inner {
    max-width: 420px;
  }

  &__icon { font-size: 56px; margin-bottom: 16px; }
  &__code { font-size: 14px; color: #909399; margin: 0 0 8px; font-weight: 400; }
  &__title { font-size: 20px; color: #303133; margin: 0 0 12px; font-weight: 600; }
  &__desc { font-size: 14px; color: #606266; margin: 0 0 24px; line-height: 1.6; }
  &__actions { display: flex; gap: 12px; justify-content: center; }
}
</style>
