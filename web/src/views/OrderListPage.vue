<template>
  <div class="order-list-page">
    <h2>我的订单</h2>

    <!-- 加载中 -->
    <div v-if="loading && orders.length === 0" class="order-list-page__loading">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" v-else>
      <el-tab-pane
        v-for="tab in tabs"
        :key="tab.value"
        :label="tab.label"
        :name="tab.value"
      />
    </el-tabs>

    <!-- 空状态 -->
    <el-empty v-if="!loading && orders.length === 0" description="暂无订单">
      <el-button type="primary" @click="$router.push('/discovery')">
        去选购水果
      </el-button>
    </el-empty>

    <!-- 订单列表 -->
    <template v-else>
      <el-table
        :data="filteredOrders"
        style="width: 100%"
        :row-class-name="tableRowClassName"
        v-loading="loading"
        class="order-list-page__table"
      >
        <el-table-column prop="orderNo" label="订单编号" min-width="160">
          <template #default="{ row }">
            <span class="order-list-page__order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="storeName" label="门店" min-width="120" />
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">
            <span class="order-list-page__amount">¥{{ row.totalAmountYuan?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag
              :type="getStatusTagType(row.status)"
              :class="{ 'order-list-page__blink': row.status === 'READY_FOR_PICKUP' }"
              size="small"
            >
              {{ formatOrderStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <div class="order-list-page__actions">
              <el-button
                type="primary"
                size="small"
                text
                @click="$router.push(`/orders/${row.id}`)"
              >
                查看详情
              </el-button>
              <el-button
                v-if="canCancelOrder(row.status)"
                type="danger"
                size="small"
                text
                :loading="cancellingIds.has(row.id)"
                @click="handleCancel(row)"
              >
                取消订单
              </el-button>
              <el-button
                v-if="row.status === 'READY_FOR_PICKUP' && row.pickupCode"
                type="success"
                size="small"
                text
                @click="showPickupCode(row)"
              >
                自取码
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="order-list-page__pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="orderStore.orderTotal"
          layout="prev, pager, next, total"
          @current-change="handlePageChange"
        />
      </div>
    </template>

    <!-- 自取码弹窗 -->
    <el-dialog
      v-model="pickupDialog.visible"
      title="自取码"
      width="360px"
      :close-on-click-modal="false"
    >
      <div class="order-list-page__pickup">
        <div class="order-list-page__pickup-code">
          {{ pickupDialog.code }}
        </div>
        <p class="order-list-page__pickup-hint">
          请向店员出示此码核销
        </p>
        <div class="order-list-page__pickup-expire" v-if="pickupDialog.expiresAt">
          有效期至：{{ formatDateTime(pickupDialog.expiresAt) }}
        </div>
        <el-button type="primary" @click="copyPickupCode">
          <el-icon><CopyDocument /></el-icon>
          复制自取码
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { useOrderStore } from '@/stores/order'
import {
  formatOrderStatus,
  getStatusTagType,
  canCancelOrder,
  orderMatchesTab,
  formatDateTime,
  ORDER_TABS,
} from '@/utils/order'

const orderStore = useOrderStore()

const loading = ref(true)
const currentPage = ref(1)
const pageSize = ref(10)
const activeTab = ref('ALL')
const cancellingIds = ref(new Set<number>())
const tabs = ORDER_TABS

const orders = computed(() => orderStore.orders)

const filteredOrders = computed(() => {
  if (activeTab.value === 'ALL') return orders.value
  return orders.value.filter((o) => orderMatchesTab(o.status, activeTab.value))
})

const pickupDialog = ref({
  visible: false,
  code: '',
  expiresAt: '',
})

onMounted(async () => {
  await loadOrders()
})

async function loadOrders() {
  loading.value = true
  await orderStore.fetchOrders(currentPage.value, pageSize.value)
  loading.value = false
}

function handleTabChange() {
  currentPage.value = 1
  // Tab 切换时前端筛选，不重新请求
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadOrders()
}

function tableRowClassName({ row }: { row: OrderInfo }) {
  if (row.status === 'READY_FOR_PICKUP') return 'order-list-page__row-highlight'
  return ''
}

function handleCancel(row: OrderInfo) {
  ElMessageBox.confirm(`确定要取消订单 ${row.orderNo} 吗？`, '取消订单', {
    confirmButtonText: '确定取消',
    cancelButtonText: '暂不取消',
    type: 'warning',
  }).then(async () => {
    cancellingIds.value.add(row.id)
    try {
      await orderStore.cancelOrder(row.id)
      ElMessage.success('订单已取消')
    } catch {
      ElMessage.error('取消失败')
    } finally {
      cancellingIds.value.delete(row.id)
    }
  }).catch(() => {})
}

async function showPickupCode(row: OrderInfo) {
  if (row.pickupCode) {
    pickupDialog.value = {
      visible: true,
      code: row.pickupCode,
      expiresAt: row.pickupCodeExpiresAt || '',
    }
  } else {
    try {
      const result = await orderStore.getPickupCode(row.id)
      if (result) {
        pickupDialog.value = {
          visible: true,
          code: result.pickupCode,
          expiresAt: result.expiresAt || '',
        }
      }
    } catch {
      ElMessage.error('获取自取码失败')
    }
  }
}

function copyPickupCode() {
  navigator.clipboard.writeText(pickupDialog.value.code).then(() => {
    ElMessage.success('已复制自取码')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}
</script>

<style lang="scss" scoped>
.order-list-page {
  h2 {
    font-size: 22px;
    margin-bottom: $spacing-lg;
  }

  &__loading {
    padding: $spacing-xl 0;
  }

  &__table {
    border-radius: 8px;
    overflow: hidden;
  }

  &__order-no {
    font-family: 'Courier New', Courier, monospace;
    font-size: 13px;
    color: $text-secondary;
  }

  &__amount {
    font-weight: 500;
    color: $danger-color;
  }

  &__actions {
    display: flex;
    gap: 4px;
    justify-content: center;
    flex-wrap: wrap;
  }

  &__blink {
    animation: order-blink 1.5s ease-in-out infinite;
  }

  &__row-highlight {
    background-color: #f0f9eb !important;
  }

  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: $spacing-lg;
  }

  &__pickup {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: $spacing-md;
  }

  &__pickup-code {
    font-size: 42px;
    font-family: 'Courier New', Courier, monospace;
    font-weight: bold;
    color: $primary-color;
    letter-spacing: 10px;
    background: #f5f7fa;
    padding: $spacing-md $spacing-xl;
    border-radius: 8px;
    border: 2px dashed $primary-color;
  }

  &__pickup-hint {
    color: $text-secondary;
    font-size: 14px;
  }

  &__pickup-expire {
    color: $warning-color;
    font-size: 13px;
  }
}

@keyframes order-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
</style>
