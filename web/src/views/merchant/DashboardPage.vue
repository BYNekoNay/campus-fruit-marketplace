<template>
  <div class="dashboard-page">
    <h2 class="dashboard-page__title">商家工作台</h2>

    <!-- 未入驻提示 -->
    <el-result
      v-if="!merchant && !loading"
      icon="warning"
      title="您还未入驻"
      sub-title="申请成为商家，开启水果销售之旅"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/merchant/apply')">
          前往入驻
        </el-button>
      </template>
    </el-result>

    <!-- 加载中 -->
    <div v-if="loading" class="dashboard-page__loading">
      <el-skeleton :rows="6" animated />
    </div>

    <!-- 商家信息 -->
    <template v-if="merchant">
      <!-- 审核中提示 -->
      <el-alert
        v-if="merchant.status === 'PENDING_REVIEW'"
        title="您的入驻申请正在审核中，请耐心等待"
        type="warning"
        :closable="false"
        show-icon
        class="mb-lg"
      />

      <!-- 拒绝原因 -->
      <el-alert
        v-if="merchant.status === 'REJECTED'"
        :title="`审核未通过${merchant.rejectReason ? '：' + merchant.rejectReason : ''}`"
        type="error"
        :closable="false"
        show-icon
        class="mb-lg"
      >
        <template #default>
          <p style="margin: 8px 0 0">您可以重新提交申请。</p>
          <el-button
            type="primary"
            size="small"
            style="margin-top: 8px"
            @click="$router.push('/merchant/apply')"
          >
            重新申请
          </el-button>
        </template>
      </el-alert>

      <!-- 已暂停提示 -->
      <el-alert
        v-if="merchant.status === 'SUSPENDED'"
        title="您的商家已被暂停，请联系管理员"
        type="error"
        :closable="false"
        show-icon
        class="mb-lg"
      />

      <!-- 商家信息卡片 -->
      <el-card class="dashboard-page__info-card">
        <template #header>
          <div class="info-card__header">
            <span>商家信息</span>
            <el-tag :type="getStatusType(merchant.status)" size="large">
              {{ merchant.statusLabel }}
            </el-tag>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商家名称">{{ merchant.name }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ merchant.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ merchant.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="营业执照号">{{ merchant.licenseNumber }}</el-descriptions-item>
          <el-descriptions-item label="入驻时间">{{ merchant.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(merchant.status)" size="small">
              {{ merchant.statusLabel }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 统计卡片（仅已通过审核时显示） -->
      <div v-if="merchant.status === 'APPROVED'" class="dashboard-page__stats">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-card__number">{{ stores.length }}</div>
              <div class="stat-card__label">门店总数</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-card__number stat-card__number--success">
                {{ activeStores.length }}
              </div>
              <div class="stat-card__label">营业中</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-card__number stat-card__number--warning">
                {{ pendingStores.length }}
              </div>
              <div class="stat-card__label">待审核</div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useMerchantStore } from '@/stores/merchant'

const merchantStore = useMerchantStore()

const merchant = computed(() => merchantStore.merchant)
const stores = computed(() => merchantStore.stores)
const loading = computed(() => merchantStore.loading)
const activeStores = computed(() => merchantStore.activeStores)

const pendingStores = computed(() =>
  stores.value.filter((s) => s.status === 'PENDING_APPROVAL')
)

function getStatusType(status: string): 'warning' | 'success' | 'danger' | 'info' {
  const map: Record<string, 'warning' | 'success' | 'danger' | 'info'> = {
    PENDING_REVIEW: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    SUSPENDED: 'danger',
  }
  return map[status] || 'info'
}

onMounted(async () => {
  if (!merchantStore.merchant) {
    await merchantStore.fetchMyMerchant()
  }
  if (merchantStore.merchant && merchantStore.merchant.status === 'APPROVED') {
    await merchantStore.fetchStores(merchantStore.merchant.id)
  }
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  &__title {
    font-size: 22px;
    margin-bottom: $spacing-lg;
  }

  &__loading {
    max-width: 800px;
  }

  &__info-card {
    margin-bottom: $spacing-lg;
  }

  &__stats {
    margin-top: $spacing-lg;
  }
}

.info-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-card {
  text-align: center;

  &__number {
    font-size: 32px;
    font-weight: bold;
    color: $primary-color;

    &--success {
      color: $success-color;
    }

    &--warning {
      color: $warning-color;
    }
  }

  &__label {
    color: $text-secondary;
    margin-top: $spacing-sm;
  }
}
</style>
