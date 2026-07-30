<template>
  <div class="store-list-page">
    <div class="store-list-page__header">
      <h2>门店管理</h2>
      <el-button type="primary" :icon="Plus" @click="$router.push('/merchant/stores/create')">
        新建门店
      </el-button>
    </div>

    <el-card>
      <el-table
        v-loading="loading"
        :data="stores"
        border
        stripe
        empty-text="暂无门店，点击右上角新建门店"
      >
        <el-table-column prop="name" label="门店名称" min-width="150" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="businessHours" label="营业时间" width="160" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :icon="Edit"
              @click="$router.push(`/merchant/stores/${row.id}/edit`)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              :icon="User"
              @click="$router.push(`/merchant/stores/${row.id}/staff`)"
            >
              员工
            </el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              size="small"
              type="warning"
              @click="handleSuspend(row)"
            >
              暂停
            </el-button>
            <el-button
              v-if="row.status === 'SUSPENDED'"
              size="small"
              type="success"
              @click="handleActivate(row)"
            >
              激活
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Plus, Edit, User } from '@element-plus/icons-vue'
import { useMerchantStore } from '@/stores/merchant'

const merchantStore = useMerchantStore()

const stores = computed(() => merchantStore.stores)
const loading = computed(() => merchantStore.loading)

function getStatusType(status: string): 'warning' | 'success' | 'danger' | 'info' {
  const map: Record<string, 'warning' | 'success' | 'danger' | 'info'> = {
    PENDING_APPROVAL: 'warning',
    ACTIVE: 'success',
    CLOSED: 'info',
    SUSPENDED: 'danger',
  }
  return map[status] || 'info'
}

async function handleSuspend(row: MerchantStoreInfo) {
  try {
    await ElMessageBox.confirm(
      `确认暂停门店「${row.name}」？暂停后用户将无法下单。`,
      '暂停确认',
      { confirmButtonText: '确认暂停', cancelButtonText: '取消', type: 'warning' }
    )
    await merchantStore.suspendStore(row.id)
    ElMessage.success('门店已暂停')
  } catch {
    // 用户取消
  }
}

async function handleActivate(row: MerchantStoreInfo) {
  try {
    await ElMessageBox.confirm(
      `确认激活门店「${row.name}」？`,
      '激活确认',
      { confirmButtonText: '确认激活', cancelButtonText: '取消', type: 'info' }
    )
    await merchantStore.activateStore(row.id)
    ElMessage.success('门店已激活')
  } catch {
    // 用户取消
  }
}

onMounted(async () => {
  if (!merchantStore.merchant) {
    await merchantStore.fetchMyMerchant()
  }
  if (merchantStore.merchant) {
    await merchantStore.fetchStores(merchantStore.merchant.id)
  }
})
</script>

<style lang="scss" scoped>
.store-list-page {
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-lg;

    h2 {
      font-size: 22px;
    }
  }
}
</style>
