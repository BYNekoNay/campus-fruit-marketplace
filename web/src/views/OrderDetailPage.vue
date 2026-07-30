<template>
  <div class="order-detail-page">
    <el-page-header @back="$router.back()">
      <template #content>
        <h2>订单详情</h2>
      </template>
    </el-page-header>

    <!-- 加载中 -->
    <div v-if="loading" class="order-detail-page__loading">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- 未找到订单 -->
    <el-result
      v-else-if="!order"
      icon="warning"
      title="订单不存在"
      sub-title="请检查订单号是否正确"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/orders')">
          返回订单列表
        </el-button>
      </template>
    </el-result>

    <template v-else>
      <div class="order-detail-page__content">
        <!-- 状态卡片 -->
        <el-card shadow="never" class="order-detail-page__status-card">
          <div class="order-detail-page__status-header">
            <el-tag
              :type="getStatusTagType(order.status)"
              :class="{ 'order-detail-page__blink': order.status === 'READY_FOR_PICKUP' }"
              size="large"
            >
              {{ formatOrderStatus(order.status) }}
            </el-tag>
            <span v-if="isFinal" class="order-detail-page__final-badge">终态</span>
          </div>

          <!-- 进度条 -->
          <el-steps
            :active="activeStep"
            :process-status="stepProcessStatus"
            align-center
            class="order-detail-page__steps"
          >
            <el-step
              v-for="(step, idx) in steps"
              :key="idx"
              :title="step.title"
              :description="step.description"
            />
          </el-steps>
        </el-card>

        <!-- NO_SHOW_PENDING 宽限期 -->
        <el-alert
          v-if="order.status === 'NO_SHOW_PENDING'"
          title="未按时取货"
          type="warning"
          :closable="false"
          show-icon
        >
          <p>该订单已超过取货保留时间，如需申诉请联系门店或提交申诉。</p>
          <el-button type="warning" size="small" @click="handleAppeal">提交申诉</el-button>
        </el-alert>

        <!-- STALE_QUOTE -->
        <el-alert
          v-if="staleQuoteInfo"
          title="价格已变化"
          type="warning"
          :closable="false"
          show-icon
        >
          <p>{{ staleQuoteInfo }}</p>
        </el-alert>

        <!-- 自取码区域 -->
        <el-card
          v-if="order.status === 'READY_FOR_PICKUP' && order.pickupCode"
          shadow="never"
          class="order-detail-page__pickup-card"
        >
          <template #header>
            <span>自取码</span>
          </template>
          <div class="order-detail-page__pickup">
            <div class="order-detail-page__pickup-code">{{ order.pickupCode }}</div>
            <p class="order-detail-page__pickup-hint">请向店员出示此码核销取货</p>
            <div class="order-detail-page__pickup-actions">
              <el-button type="primary" @click="copyPickupCode">
                <el-icon><CopyDocument /></el-icon>
                复制自取码
              </el-button>
              <el-button @click="resendPickupCode" :loading="resending">
                重新发送
              </el-button>
            </div>
            <div class="order-detail-page__pickup-expire" v-if="order.pickupCodeExpiresAt">
              有效期至：{{ formatDateTime(order.pickupCodeExpiresAt) }}
            </div>
            <!-- 倒计时 -->
            <div class="order-detail-page__countdown" v-if="countdownText">
              {{ countdownText }}
            </div>
            <el-button type="warning" size="small" link @click="handleRevokePickupCode" style="margin-top:8px">
              作废并重新生成
            </el-button>
          </div>
        </el-card>

        <!-- 订单信息 -->
        <el-card shadow="never" class="order-detail-page__info-card">
          <template #header>
            <span>订单信息</span>
          </template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="订单编号">
              <span class="order-detail-page__mono">{{ order.orderNo }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="门店名称">
              {{ order.storeName }}
            </el-descriptions-item>
            <el-descriptions-item label="下单时间">
              {{ formatDateTime(order.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="支付方式">
              {{ order.paymentStatus === 'PAID_AT_PICKUP' ? '已到店支付' : '到店支付' }}
            </el-descriptions-item>
            <el-descriptions-item
              v-if="order.cancelReason"
              label="取消原因"
              :span="2"
            >
              <span class="order-detail-page__cancel-reason">{{ order.cancelReason }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 商品清单 -->
        <el-card shadow="never" class="order-detail-page__items-card">
          <template #header>
            <span>商品清单</span>
          </template>
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
                <span class="order-detail-page__item-subtotal">
                  ¥{{ ((row as any).subtotalYuan || (row.unitPrice * row.quantity / 100)).toFixed(2) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
          <div class="order-detail-page__amount-summary">
            <span>合计：</span>
            <span class="order-detail-page__total-price">
              ¥{{ order.totalAmountYuan?.toFixed(2) }}
            </span>
          </div>
        </el-card>

        <!-- 状态历史 -->
        <el-card shadow="never" class="order-detail-page__history-card">
          <template #header>
            <span>状态历史</span>
          </template>
          <el-timeline v-if="order.statusEvents && order.statusEvents.length > 0">
            <el-timeline-item
              v-for="event in order.statusEvents"
              :key="event.id"
              :timestamp="formatDateTime(event.createdAt)"
              placement="top"
            >
              <div class="order-detail-page__history-event">
                <span class="order-detail-page__history-status">
                  {{ formatOrderStatus(event.toStatus as OrderStatus) }}
                </span>
                <span v-if="event.note" class="order-detail-page__history-note">
                  {{ event.note }}
                </span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无状态变更记录" :image-size="60" />
        </el-card>

        <!-- 底部操作 -->
        <div class="order-detail-page__actions" v-if="!isFinal && canCancelOrder(order.status)">
          <el-button
            v-if="order.status === 'READY_FOR_PICKUP'"
            type="success"
            @click="showPickupCode"
          >
            查看自取码
          </el-button>
          <el-button
            v-if="canCancelOrder(order.status)"
            type="danger"
            :loading="cancelling"
            @click="handleCancel"
          >
            取消订单
          </el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { useOrderStore } from '@/stores/order'
import { centsToYuan } from '@/utils/format'
import {
  formatOrderStatus,
  getStatusTagType,
  canCancelOrder,
  isOrderFinalStatus,
  getOrderSteps,
  getActiveStepIndex,
  formatDateTime,
} from '@/utils/order'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()

const props = defineProps<{ id: string }>()

const loading = ref(true)
const cancelling = ref(false)
const resending = ref(false)
const countdownText = ref('')
const staleQuoteInfo = ref('')
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 计算属性
const order = computed(() => orderStore.currentOrder)

const isFinal = computed(() => {
  if (!order.value) return false
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
  const id = Number(props.id)
  if (!id || isNaN(id)) {
    loading.value = false
    return
  }
  await orderStore.fetchOrderDetail(id)
  loading.value = false
  startCountdown()
})

onUnmounted(() => {
  stopCountdown()
})

function startCountdown() {
  if (!order.value?.pickupCodeExpiresAt) return
  stopCountdown()
  countdownTimer = setInterval(() => {
    const expiresAt = new Date(order.value!.pickupCodeExpiresAt!).getTime()
    const now = Date.now()
    const diff = expiresAt - now
    if (diff <= 0) {
      countdownText.value = '自取码已过期'
      stopCountdown()
      return
    }
    const hours = Math.floor(diff / 3600000)
    const minutes = Math.floor((diff % 3600000) / 60000)
    const seconds = Math.floor((diff % 60000) / 1000)
    countdownText.value = `剩余 ${hours}时${minutes}分${seconds}秒`
  }, 1000)
}

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

function copyPickupCode() {
  if (!order.value?.pickupCode) return
  navigator.clipboard.writeText(order.value.pickupCode).then(() => {
    ElMessage.success('已复制自取码')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

async function resendPickupCode() {
  if (!order.value) return
  resending.value = true
  try {
    const result = await orderStore.getPickupCode(order.value.id)
    if (result && order.value) {
      order.value.pickupCode = result.pickupCode
      order.value.pickupCodeExpiresAt = result.expiresAt
      startCountdown()
      ElMessage.success('自取码已重新获取')
    }
  } catch {
    ElMessage.error('获取失败')
  } finally {
    resending.value = false
  }
}

function handleCancel() {
  if (!order.value) return
  ElMessageBox.confirm(`确定要取消订单 ${order.value.orderNo} 吗？`, '取消订单', {
    confirmButtonText: '确定取消',
    cancelButtonText: '暂不取消',
    type: 'warning',
  }).then(async () => {
    cancelling.value = true
    try {
      await orderStore.cancelOrder(order.value!.id)
      ElMessage.success('订单已取消')
    } catch {
      ElMessage.error('取消失败')
    } finally {
      cancelling.value = false
    }
  }).catch(() => {})
}

function showPickupCode() {
  if (!order.value?.pickupCode) return
  ElMessageBox.alert(
    `自取码：${order.value.pickupCode}`,
    '自取码',
    {
      confirmButtonText: '我知道了',
      center: true,
      customClass: 'order-detail-page__pickup-message',
    }
  )
}

async function handleRevokePickupCode() {
  try {
    await ElMessageBox.confirm('作废当前自取码并生成新的？旧码将立即失效。', '重新生成自取码', {
      confirmButtonText: '确认作废并重新生成',
      type: 'warning'
    })
    const result = await orderStore.getPickupCode(Number(route.params.id))
    if (order.value && result) {
      order.value.pickupCode = result.pickupCode
      order.value.pickupCodeExpiresAt = result.expiresAt
    }
    ElMessage.success('已生成新的自取码')
  } catch { /* cancelled */ }
}

function handleAppeal() {
  ElMessageBox.alert('申诉功能：请联系门店说明情况，或在工作时间拨打客服电话。', '申诉渠道')
}
</script>

<style lang="scss" scoped>
.order-detail-page {
  &__loading {
    margin-top: $spacing-xl;
  }

  &__content {
    margin-top: $spacing-xl;
    display: flex;
    flex-direction: column;
    gap: $spacing-md;
  }

  &__status-card {
    .order-detail-page__status-header {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      margin-bottom: $spacing-lg;
    }
  }

  &__final-badge {
    font-size: 12px;
    color: $text-secondary;
    background: #f0f0f0;
    padding: 2px 8px;
    border-radius: 10px;
  }

  &__steps {
    margin: $spacing-lg 0;
  }

  &__blink {
    animation: order-detail-blink 1.5s ease-in-out infinite;
  }

  &__pickup-card {
    border: 2px solid $success-color;
  }

  &__pickup {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: $spacing-md;
  }

  &__pickup-code {
    font-size: 48px;
    font-family: 'Courier New', Courier, monospace;
    font-weight: bold;
    color: $primary-color;
    letter-spacing: 12px;
    background: #f5f7fa;
    padding: $spacing-md $spacing-xl * 2;
    border-radius: 12px;
    border: 3px dashed $primary-color;
  }

  &__pickup-hint {
    color: $text-secondary;
    font-size: 14px;
  }

  &__pickup-actions {
    display: flex;
    gap: $spacing-md;
  }

  &__pickup-expire {
    color: $warning-color;
    font-size: 13px;
  }

  &__countdown {
    color: $danger-color;
    font-size: 14px;
    font-weight: 500;
  }

  &__info-card,
  &__items-card,
  &__history-card {
    // info card
  }

  &__mono {
    font-family: 'Courier New', Courier, monospace;
    font-size: 13px;
  }

  &__cancel-reason {
    color: $danger-color;
  }

  &__item-subtotal {
    font-weight: 500;
    color: $danger-color;
  }

  &__amount-summary {
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

  &__history-event {
    display: flex;
    gap: $spacing-md;
    align-items: center;
  }

  &__history-status {
    font-weight: 500;
    white-space: nowrap;
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
}

@keyframes order-detail-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
</style>

<style lang="scss">
// 全局样式（自取码弹窗）
.order-detail-page__pickup-message {
  .el-message-box__message {
    font-size: 24px;
    font-family: 'Courier New', Courier, monospace;
    font-weight: bold;
    color: var(--el-color-primary);
    letter-spacing: 6px;
    text-align: center;
  }
}
</style>
