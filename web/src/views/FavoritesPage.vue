<template>
  <div class="favorites-page">
    <h2>我的收藏</h2>
    <el-empty v-if="!loading && stores.length === 0" description="暂无收藏的门店，去发现页逛逛吧">
      <el-button type="primary" @click="$router.push('/discovery')">去发现</el-button>
    </el-empty>

    <el-row :gutter="12" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" v-for="store in stores" :key="store.storeId">
        <el-card class="fav-card" shadow="hover" @click="$router.push(`/stores/${store.storeId}`)" role="button" tabindex="0" :aria-label="`查看 ${store.storeName} 详情`">
          <div class="fav-card__header">
            <h3>{{ store.storeName }}</h3>
            <el-tag v-if="store.status === 'OPEN'" type="success" size="small">营业中</el-tag>
            <el-tag v-else type="danger" size="small">已停业</el-tag>
          </div>
          <div class="fav-card__body">
            <p v-if="store.address"><el-icon><Location /></el-icon> {{ store.address }}</p>
            <div class="fav-card__meta">
              <span v-if="store.avgRating">⭐ {{ store.avgRating.toFixed(1) }}</span>
              <span v-if="store.distance">{{ store.distance.toFixed(1) }}km</span>
            </div>
          </div>
          <el-button type="danger" link size="small" @click.stop="handleRemove(store.storeId)" aria-label="取消收藏">取消收藏</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import { useDiscoveryStore } from '@/stores/discovery'

const store = useDiscoveryStore()
const stores = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    await store.fetchFavorites()
    stores.value = store.favorites || []
  } finally {
    loading.value = false
  }
})

async function handleRemove(storeId: number) {
  await store.removeFavorite(storeId)
  stores.value = stores.value.filter(s => s.storeId !== storeId)
  ElMessage.success('已取消收藏')
}
</script>

<style scoped lang="scss">
.favorites-page {
  padding: 20px;
  max-width: 960px;
  margin: 0 auto;
  h2 { margin-bottom: 20px; }
}

.fav-card {
  margin-bottom: 12px;
  cursor: pointer;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    h3 { margin: 0; font-size: 16px; }
  }

  &__body {
    margin: 12px 0;
    p { font-size: 13px; color: #606266; margin: 4px 0; display: flex; align-items: center; gap: 4px; }
  }

  &__meta {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: #909399;
    margin-top: 8px;
  }
}
</style>
