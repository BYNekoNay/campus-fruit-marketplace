<template>
  <div class="merchant-order-detail">
    <el-page-header @back="$router.back()">
      <template #content>
        <h3>订单详情</h3>
      </template>
    </el-page-header>

    <!-- 加载中 -->
    <div v-if="loading" class="merchant-order-detail__loading">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- 未找到 -->
    <el-result
      v-else-if="!order"
      icon="warning"
      title="订单不存在"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/merchant/orders')">
          返回订单列表
        </el-button>
      </template>
    </el-result>

    <template v-else>
      <div class="merchant-order-detail__content">
        <!-- 状态卡片 -->
        <el-card shadow="never">
          <div class="merchant-order-detail__status-header">
            <el-tag :type="getStatusTagType(order.status)" size="large">
              {{ formatOrderStatus(order.status) }}
            </el-tag>
            <span class="merchant-order-detail__order-no-label">
              订单编号：<span class="merchant-order-detail__order-no">{{ order.orderNo }}</span>
            </span>
          </div>

          <el-steps
            :active="activeStep"
            :process-status="stepProcessStatus"
            align-center
            style="margin-top: 20px"
          >
            <el-step
              v-for="(step, idx) in steps"
              :key="idx"
              :title="step.title"
              :description="step.description"
            />
          </el-steps>
        </el-card>

        <!-- 订单信息 -->
        <el-card shadow="never">
          <template #header><span>订单信息</span></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="订单编号">
              {{ order.orderNo }}
            </el-descriptions-item>
            <el-descriptions-item label="门店名称">
              {{ order.storeName }}
            </el-descriptions-item>
            <el-descriptions-item label="下单时间">
              {{ formatDateTime(order.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="支付状态">
              {{ order.paymentStatus === 'PAID_AT_PICKUP' ? '已到店支付' : '到店支付' }}
            </el-descriptions-item>
            <el-descriptions-item
              v-if="order.cancelReason"
              label="取消原因"
              :span="2"
            >
              <span style="color: #f56c6c">{{ order.cancelReason }}</span>
            </el-descriptions-item>
            <el-descriptions-item
              v-if="order.pickupCode"
              label="自取码"
            >
              <span class="merchant-order-detail__pickup-code">{{ order.pickupCode }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 商品清单 -->
        <el-card shadow="never">
          <template #header><span>商品清单</span></template>
          <el-table :data="order.items" size="small" style="width: 100%">
            <el-table-column prop="fruitVariety" label="品种" />
            <el-table-column prop="salesUnit" label="规格" width="120" />
            <el-table-column label="单价(元)" width="100" align="right">
              <template #default="{ row }">
                ¥{{ centsToYuan(row.unitPrice) }}
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" align="center" />
            <el-table-column label="小计" width="110" align="right">
              <template #default="{ row }">
                ¥{{ ((row.unitPrice * row.quantity) / 100).toFixed(2) }}
              </template>
            </el-table-column>
          </el-table>
          <div class="merchant-order-detail__amount">
            <span>合计：</span>
            <span class="merchant-order-detail__total-price">
              ¥{{ order.totalAmountYuan?.toFixed(2) }}
            </span>
          </div>
        </el-card>

        <!-- 状态历史 -->
        <el-card shadow="never">
          <template #header><span>状态历史</span></template>
          <el-timeline v-if="order.statusEvents && order.statusEvents.length > 0">
            <el-timeline-item
              v-for="event in order.statusEvents"
              :key="event.id"
              :timestamp="formatDateTime(event.createdAt)"
              placement="top"
            >
              <div class="merchant-order-detail__history">
                <span class="merchant-order-detail__history-status">
                  {{ formatOrderStatus(event.toStatus as OrderStatus) }}
                </span>
                <span v-if="event.operatorType" class="merchant-order-detail__history-operator">
                  ({{ event.operatorType }})
                </span>
                <span v-if="event.note" class="merchant-order-detail__history-note">
                  {{ event.note }}
                </span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无状态变更记录" :image-size="60" />
        </el-card>

        <!-- 操作区 -->
        <div class="merchant-order-detail__actions" v-if="!isFinal">
          <template v-if="order.status === 'PENDING_RESERVATION' || order.status === 'PENDING_STORE_CONFIRMATION'">
            <el-button type="success" :loading="actionLoading" @click="handleAccept">
              接单
            </el-button>
            <el-button type="danger" :loading="actionLoading" @click="handleReject">
              拒单
            </el-button>
          </template>
          <template v-if="order.status === 'ACCEPTED'">
            <el-button type="warning" :loading="actionLoading" @click="handleReady">
              备货完成
            </el-button>
          </template>
          <template v-if="order.status === 'READY_FOR_PICKUP'">
            <el-button type="success" :loading="actionLoading" @click="showVerifyDialog">
              核销
            </el-button>
            <el-button type="danger" :loading="actionLoading" @click="handleNoShow">
              标记未取
            </el-button>
          </template>
        </div>
      </div>
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
      <div class="merchant-order-detail__verify">
        <p>请输入用户出示的自取码</p>
        <el-input
          v-model="verifyDialog.pickupCode"
          placeholder="请输入6位自取码"
          maxlength="10"
          clearable
          class="merchant-order-detail__verify-input"
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Key } from '@element-plus/icons-vue'
import { useOrderStore } from '@/stores/order'
import { centsToYuan } from '@/utils/format'
import {
  formatOrderStatus,
  getStatusTagType,
  isOrderFinalStatus,
  getOrderSteps,
  getActiveStepIndex,
  formatDateTime,
} from '@/utils/order'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()

const loading = ref(true)
const actionLoading = ref(false)

const rejectDialog = ref({
  visible: false,
  loading: false,
})

const rejectForm = ref({ reason: '' })

const verifyDialog = ref({
  visible: false,
  loading: false,
  pickupCode: '',
})

const order = computed(() => orderStore.currentOrder)

const isFinal = computed(() => {
  if (!order.value) return true
  return isOrderFinalStatus(order.value.status)
})

const steps = computed(() => {
  if (!order.value) return []
  return getOrderSteps(order.value.status)
})

const activeStep = computed(() => {
  if (!order.value) return 0
  return getActiveStepIndex(order.value.status)
})

const stepProcessStatus = computed<'process' | 'finish' | 'error' | 'wait'>(() => {
  if (!order.value) return 'process'
  const failedStatuses: OrderStatus[] = ['CANCELLED', 'REJECTED', 'EXPIRED', 'NO_SHOW_PENDING']
  if (failedStatuses.includes(order.value.status)) return 'error'
  if (order.value.status === 'COMPLETED') return 'finish'
  return 'process'
})

onMounted(async () => {
  const id = Number(route.params.id)
  if (id && !isNaN(id)) {
    await orderStore.fetchOrderDetail(id)
  }
  loading.value = false
})

async function refreshOrder() {
  if (order.value) {
    await orderStore.fetchOrderDetail(order.value.id)
  }
}

function handleAccept() {
  if (!order.value) return
  ElMessageBox.confirm(`确认接单 ${order.value.orderNo} 吗？`, '确认接单', {
    confirmButtonText: '确认接单',
    type: 'success',
  }).then(async () => {
    actionLoading.value = true
    try {
      await orderStore.acceptOrder(order.value!.id)
      ElMessage.success('已接单')
      await refreshOrder()
    } catch {
      ElMessage.error('操作失败')
    } finally {
      actionLoading.value = false
    }
  }).catch(() => {})
}

function handleReject() {
  if (!order.value) return
  rejectDialog.value.visible = true
  rejectForm.value.reason = ''
}

async function confirmReject() {
  if (!order.value) return
  rejectDialog.value.loading = true
  try {
    await orderStore.rejectOrder(order.value.id, rejectForm.value.reason)
    ElMessage.success('已拒单')
    rejectDialog.value.visible = false
    await refreshOrder()
  } catch {
    ElMessage.error('拒单失败')
  } finally {
    rejectDialog.value.loading = false
  }
}

function handleReady() {
  if (!order.value) return
  ElMessageBox.confirm(`确认 "${order.value.orderNo}" 已备货完成？`, '备货完成', {
    confirmButtonText: '确认完成',
    type: 'warning',
  }).then(async () => {
    actionLoading.value = true
    try {
      await orderStore.readyOrder(order.value!.id)
      ElMessage.success('已标记备货完成')
      await refreshOrder()
    } catch {
      ElMessage.error('操作失败')
    } finally {
      actionLoading.value = false
    }
  }).catch(() => {})
}

function showVerifyDialog() {
  verifyDialog.value = { visible: true, loading: false, pickupCode: '' }
}

async function confirmVerify() {
  if (!order.value) return
  verifyDialog.value.loading = true
  try {
    await orderStore.completeOrder(order.value.id, verifyDialog.value.pickupCode)
    ElMessage.success('核销成功')
    verifyDialog.value.visible = false
    await refreshOrder()
  } catch {
    ElMessage.error('核销失败，请检查自取码')
  } finally {
    verifyDialog.value.loading = false
  }
}

function handleNoShow() {
  if (!order.value) return
  ElMessageBox.confirm('确认将此订单标记为"未取"？', '标记未取', {
    confirmButtonText: '确认',
    type: 'warning',
  }).then(async () => {
    actionLoading.value = true
    try {
      await orderStore.markNoShow(order.value!.id)
      ElMessage.success('已标记为未取')
      await refreshOrder()
    } catch {
      ElMessage.error('操作失败')
    } finally {
      actionLoading.value = false
    }
  }).catch(() => {})
}
</script>

<style lang="scss" scoped>
.merchant-order-detail {
  &__loading {
    margin-top: $spacing-xl;
  }

  &__content {
    margin-top: $spacing-xl;
    display: flex;
    flex-direction: column;
    gap: $spacing-md;
  }

  &__status-header {
    display: flex;
    align-items: center;
    gap: $spacing-lg;
  }

  &__order-no-label {
    font-size: 14px;
    color: $text-secondary;
  }

  &__order-no {
    font-family: 'Courier New', Courier, monospace;
    font-size: 13px;
    color: $text-color;
  }

  &__pickup-code {
    font-family: 'Courier New', Courier, monospace;
    font-size: 16px;
    font-weight: bold;
    color: $primary-color;
    letter-spacing: 4px;
  }

  &__amount {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: $spacing-sm;
    padding: $spacing-md 0 0;
    font-size: 16px;
  }

  &__total-price {
    font-size: 20px;
    font-weight: bold;
    color: $danger-color;
  }

  &__history {
    display: flex;
    gap: $spacing-sm;
    align-items: center;
    flex-wrap: wrap;
  }

  &__history-status {
    font-weight: 500;
    white-space: nowrap;
  }

  &__history-operator {
    color: $text-secondary;
    font-size: 12px;
  }

  &__history-note {
    color: $text-secondary;
    font-size: 13px;
  }

  &__actions {
    display: flex;
    justify-content: center;
    gap: $spacing-md;
    padding: $spacing-lg;
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
