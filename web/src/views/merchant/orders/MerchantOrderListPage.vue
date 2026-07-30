<template>
  <div class="merchant-order-list">
    <div class="merchant-order-list__header">
      <h3>订单管理</h3>
      <!-- 门店选择 -->
      <el-select
        v-model="selectedStoreId"
        placeholder="请选择门店"
        @change="handleStoreChange"
        style="width: 250px"
      >
        <el-option
          v-for="store in stores"
          :key="store.id"
          :label="store.name"
          :value="store.id"
        />
      </el-select>
    </div>

    <!-- 未选择门店 -->
    <el-empty
      v-if="!selectedStoreId"
      description="请先选择门店"
      :image-size="80"
    />

    <template v-else>
      <!-- Tabs -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane
          v-for="tab in tabs"
          :key="tab.value"
          :label="tab.label"
          :name="tab.value"
        />
      </el-tabs>

      <!-- 加载中 -->
      <div v-if="loading" class="merchant-order-list__loading">
        <el-skeleton :rows="8" animated />
      </div>

      <!-- 空状态 -->
      <el-empty
        v-else-if="orders.length === 0"
        description="暂无订单"
        :image-size="80"
      />

      <!-- 订单表格 -->
      <template v-else>
        <el-table
          :data="orders"
          style="width: 100%"
          v-loading="loading"
          row-class-name="merchant-order-list__table"
        >
          <el-table-column prop="orderNo" label="订单编号" min-width="160">
            <template #default="{ row }">
              <span class="merchant-order-list__order-no">{{ row.orderNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="130" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" size="small">
                {{ formatOrderStatus(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }">
              ¥{{ row.totalAmountYuan?.toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="商品" min-width="180">
            <template #default="{ row }">
              <span v-for="(item, idx) in row.items.slice(0, 2)" :key="idx">
                {{ item.fruitVariety }}×{{ item.quantity }}
                <span v-if="idx < Math.min(row.items.length, 2) - 1">, </span>
              </span>
              <span v-if="row.items.length > 2" class="merchant-order-list__more">
                等{{ row.items.length }}件
              </span>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" align="center" fixed="right">
            <template #default="{ row }">
              <div class="merchant-order-list__actions">
                <el-button
                  type="primary"
                  size="small"
                  text
                  @click="$router.push(`/merchant/orders/${row.id}`)"
                >
                  详情
                </el-button>
                <template v-if="row.status === 'PENDING_RESERVATION' || row.status === 'PENDING_STORE_CONFIRMATION'">
                  <el-button
                    type="success"
                    size="small"
                    text
                    :loading="actionLoadingIds.has(row.id)"
                    @click="handleAccept(row)"
                  >
                    接单
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    text
                    :loading="actionLoadingIds.has(row.id)"
                    @click="handleReject(row)"
                  >
                    拒单
                  </el-button>
                </template>
                <template v-if="row.status === 'ACCEPTED'">
                  <el-button
                    type="warning"
                    size="small"
                    text
                    :loading="actionLoadingIds.has(row.id)"
                    @click="handleReady(row)"
                  >
                    备货完成
                  </el-button>
                </template>
                <template v-if="row.status === 'READY_FOR_PICKUP'">
                  <el-button
                    type="success"
                    size="small"
                    text
                    :loading="actionLoadingIds.has(row.id)"
                    @click="showVerifyDialog(row)"
                  >
                    核销
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    text
                    :loading="actionLoadingIds.has(row.id)"
                    @click="handleNoShow(row)"
                  >
                    标记未取
                  </el-button>
                </template>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="merchant-order-list__pagination">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="orderStore.orderTotal"
            layout="prev, pager, next, total"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </template>

    <!-- 拒单对话框 -->
    <el-dialog v-model="rejectDialog.visible" title="拒单" width="400px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button
          type="danger"
          :loading="rejectDialog.loading"
          :disabled="!rejectForm.reason.trim()"
          @click="confirmReject"
        >
          确认拒单
        </el-button>
      </template>
    </el-dialog>

    <!-- 核销对话框 -->
    <el-dialog v-model="verifyDialog.visible" title="核销订单" width="400px">
      <div class="merchant-order-list__verify">
        <p>请输入用户出示的自取码</p>
        <el-input
          v-model="verifyDialog.pickupCode"
          placeholder="请输入6位自取码"
          maxlength="10"
          clearable
          class="merchant-order-list__verify-input"
        >
          <template #prefix>
            <el-icon><Key /></el-icon>
          </template>
        </el-input>
      </div>
      <template #footer>
        <el-button @click="verifyDialog.visible = false">取消</el-button>
        <el-button
          type="success"
          :loading="verifyDialog.loading"
          :disabled="!verifyDialog.pickupCode.trim()"
          @click="confirmVerify"
        >
          确认核销
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Key } from '@element-plus/icons-vue'
import { useOrderStore } from '@/stores/order'
import { useMerchantStore } from '@/stores/merchant'
import {
  formatOrderStatus,
  getStatusTagType,
  formatDateTime,
  ORDER_TABS,
} from '@/utils/order'

const orderStore = useOrderStore()
const merchantStore = useMerchantStore()

const loading = ref(false)
const selectedStoreId = ref<number>(0)
const activeTab = ref('ALL')
const currentPage = ref(1)
const pageSize = ref(10)
const actionLoadingIds = ref(new Set<number>())
const tabs = ORDER_TABS

const rejectDialog = ref({
  visible: false,
  loading: false,
  orderId: 0,
})

const rejectForm = ref({
  reason: '',
})

const verifyDialog = ref({
  visible: false,
  loading: false,
  orderId: 0,
  pickupCode: '',
})

const stores = computed(() => merchantStore.stores || [])
const orders = computed(() => orderStore.orders)

onMounted(async () => {
  // 先获取商家信息，再获取门店列表
  const merchant = await merchantStore.fetchMyMerchant()
  if (merchant?.id) {
    await merchantStore.fetchStores(merchant.id)
    if (stores.value.length > 0) {
      selectedStoreId.value = stores.value[0].id
      await loadOrders()
    }
  }
})

function handleStoreChange() {
  activeTab.value = 'ALL'
  currentPage.value = 1
  loadOrders()
}

function handleTabChange() {
  currentPage.value = 1
  loadOrders()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadOrders()
}

async function loadOrders() {
  if (!selectedStoreId.value) return
  loading.value = true
  try {
    const status = activeTab.value === 'ALL' ? undefined : activeTab.value
    await orderStore.fetchStoreOrders(selectedStoreId.value, status, currentPage.value, pageSize.value)
  } finally {
    loading.value = false
  }
}

async function batchAction(id: number, action: () => Promise<void>, successMsg: string) {
  actionLoadingIds.value.add(id)
  try {
    await action()
    ElMessage.success(successMsg)
    await loadOrders()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    actionLoadingIds.value.delete(id)
  }
}

function handleAccept(row: OrderInfo) {
  ElMessageBox.confirm(`确认接单 ${row.orderNo} 吗？`, '确认接单', {
    confirmButtonText: '确认接单',
    type: 'success',
  }).then(() => {
    batchAction(row.id, () => orderStore.acceptOrder(row.id), '已接单')
  }).catch(() => {})
}

function handleReject(row: OrderInfo) {
  rejectDialog.value = {
    visible: true,
    loading: false,
    orderId: row.id,
  }
  rejectForm.value.reason = ''
}

async function confirmReject() {
  rejectDialog.value.loading = true
  try {
    await orderStore.rejectOrder(rejectDialog.value.orderId, rejectForm.value.reason)
    ElMessage.success('已拒单')
    rejectDialog.value.visible = false
    await loadOrders()
  } catch {
    ElMessage.error('拒单失败')
  } finally {
    rejectDialog.value.loading = false
  }
}

function handleReady(row: OrderInfo) {
  ElMessageBox.confirm(`确认 "${row.orderNo}" 已备货完成？`, '备货完成', {
    confirmButtonText: '确认完成',
    type: 'warning',
  }).then(() => {
    batchAction(row.id, () => orderStore.readyOrder(row.id), '已标记备货完成')
  }).catch(() => {})
}

function showVerifyDialog(row: OrderInfo) {
  verifyDialog.value = {
    visible: true,
    loading: false,
    orderId: row.id,
    pickupCode: '',
  }
}

async function confirmVerify() {
  verifyDialog.value.loading = true
  try {
    await orderStore.completeOrder(verifyDialog.value.orderId, verifyDialog.value.pickupCode)
    ElMessage.success('核销成功')
    verifyDialog.value.visible = false
    await loadOrders()
  } catch {
    ElMessage.error('核销失败，请检查自取码')
  } finally {
    verifyDialog.value.loading = false
  }
}

function handleNoShow(row: OrderInfo) {
  ElMessageBox.confirm(
    `确认将此订单标记为"未取"？`,
    '标记未取',
    {
      confirmButtonText: '确认',
      type: 'warning',
    }
  ).then(() => {
    batchAction(row.id, () => orderStore.markNoShow(row.id), '已标记为未取')
  }).catch(() => {})
}
</script>

<style lang="scss" scoped>
.merchant-order-list {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $spacing-lg;

    h3 {
      margin: 0;
      font-size: 18px;
    }
  }

  &__loading {
    padding: $spacing-xl 0;
  }

  &__order-no {
    font-family: 'Courier New', Courier, monospace;
    font-size: 13px;
    color: $text-secondary;
  }

  &__more {
    color: $text-secondary;
    font-size: 12px;
  }

  &__actions {
    display: flex;
    gap: 2px;
    justify-content: center;
    flex-wrap: wrap;
  }

  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: $spacing-lg;
  }

  &__verify {
    p {
      margin-bottom: $spacing-md;
      color: $text-secondary;
    }
  }

  &__verify-input {
    font-family: 'Courier New', Courier, monospace;
    font-size: 18px;
  }
}
</style>
