<template>
  <div class="cart-page">
    <h2>购物车</h2>

    <!-- 加载中 -->
    <div v-if="loading" class="cart-page__loading">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 空购物车 -->
    <el-empty
      v-else-if="!cartStore.cart || cartStore.cart.items.length === 0"
      description="购物车空空如也，快去选购吧~"
    >
      <el-button type="primary" @click="$router.push('/discovery')">
        去发现水果
      </el-button>
    </el-empty>

    <!-- 购物车内容 -->
    <template v-else>
      <!-- 门店信息 -->
      <div class="cart-page__store">
        <span class="cart-page__store-name">
          <el-icon><Shop /></el-icon>
          {{ cartStore.cart.storeName }}
        </span>
        <el-button type="primary" plain size="small" @click="showSwitchConfirm = true">
          切换门店
        </el-button>
      </div>

      <!-- 商品列表 -->
      <el-table :data="cartStore.cart.items" style="width: 100%" class="cart-page__table">
        <el-table-column prop="fruitVariety" label="水果品种" min-width="150" />
        <el-table-column prop="salesUnit" label="规格" width="120" />
        <el-table-column label="单价(元)" width="100" align="right">
          <template #default="{ row }">
            ¥{{ row.unitPriceYuan?.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="数量" width="140" align="center">
          <template #default="{ row }">
            <el-input-number
              :model-value="row.quantity"
              :min="1"
              :max="99"
              size="small"
              controls-position="right"
              @update:model-value="(val: number | undefined) => handleQuantityChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="100" align="right">
          <template #default="{ row }">
            <span class="cart-page__subtotal">
              ¥{{ (row.unitPriceYuan * row.quantity).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button
              type="danger"
              size="small"
              text
              @click="handleRemove(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 底部合计 -->
      <div class="cart-page__footer">
        <div class="cart-page__total">
          合计：<span class="cart-page__total-price">¥{{ cartStore.cartTotal.toFixed(2) }}</span>
          <span class="cart-page__total-info">
            （共 {{ cartStore.cartItemCount }} 件商品，到店支付）
          </span>
        </div>
        <el-button
          type="primary"
          size="large"
          :loading="creating"
          @click="showOrderConfirm = true"
        >
          立即下单
        </el-button>
      </div>
    </template>

    <!-- 切换门店确认对话框 -->
    <el-dialog
      v-model="showSwitchConfirm"
      title="切换门店"
      width="450px"
      :close-on-click-modal="false"
    >
      <div class="cart-page__switch-dialog">
        <el-alert
          title="警告：切换门店将清空当前购物车"
          type="warning"
          :closable="false"
          show-icon
          class="cart-page__switch-alert"
        />
        <el-descriptions :column="1" border size="small" class="cart-page__switch-info">
          <el-descriptions-item label="当前门店">
            {{ cartStore.cart?.storeName }}
          </el-descriptions-item>
          <el-descriptions-item label="当前商品数">
            {{ cartStore.cartItemCount }} 件
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="showSwitchConfirm = false">取消</el-button>
        <el-button type="primary" @click="handleSwitchStore">
          确认切换（清空购物车）
        </el-button>
      </template>
    </el-dialog>

    <!-- 下单确认对话框 -->
    <el-dialog
      v-model="showOrderConfirm"
      title="确认下单"
      width="480px"
      :close-on-click-modal="false"
    >
      <div class="cart-page__order-confirm">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="门店">
            {{ cartStore.cart?.storeName }}
          </el-descriptions-item>
          <el-descriptions-item label="商品数量">
            {{ cartStore.cartItemCount }} 件
          </el-descriptions-item>
          <el-descriptions-item label="金额合计">
            <span class="cart-page__total-price">¥{{ cartStore.cartTotal.toFixed(2) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">
            到店支付
          </el-descriptions-item>
        </el-descriptions>
        <el-divider />
        <div class="cart-page__order-items">
          <div
            v-for="item in cartStore.cart?.items"
            :key="item.id"
            class="cart-page__order-item"
          >
            <span>{{ item.fruitVariety }} × {{ item.quantity }}</span>
            <span>¥{{ (item.unitPriceYuan * item.quantity).toFixed(2) }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showOrderConfirm = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateOrder">
          确认下单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Shop } from '@element-plus/icons-vue'
import { useOrderStore } from '@/stores/order'

const router = useRouter()
const cartStore = useOrderStore()

const loading = ref(true)
const creating = ref(false)
const showSwitchConfirm = ref(false)
const showOrderConfirm = ref(false)

onMounted(async () => {
  await cartStore.fetchCart()
  loading.value = false
})

function handleQuantityChange(row: CartItem, val: number | undefined) {
  if (!val || val === row.quantity) return
  cartStore.addToCart(row.offerId, val).then(() => {
    ElMessage.success('数量已更新')
  })
}

function handleRemove(itemId: number) {
  ElMessageBox.confirm('确定要删除该商品吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await cartStore.removeFromCart(itemId)
    ElMessage.success('已删除')
  }).catch(() => {})
}

function handleSwitchStore() {
  cartStore.clearCart()
  showSwitchConfirm.value = false
  ElMessage.success('购物车已清空，请选择新门店')
  router.push('/discovery')
}

async function handleCreateOrder() {
  if (!cartStore.cart || cartStore.cart.items.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }

  creating.value = true
  try {
    const order = await cartStore.createOrder()
    if (order) {
      showOrderConfirm.value = false
      ElMessage.success('下单成功')
      router.push(`/orders/${order.id}`)
    }
  } catch {
    ElMessage.error('下单失败，请重试')
  } finally {
    creating.value = false
  }
}
</script>

<style lang="scss" scoped>
.cart-page {
  h2 {
    font-size: 22px;
    margin-bottom: $spacing-lg;
  }

  &__loading {
    padding: $spacing-xl 0;
  }

  &__store {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $spacing-md $spacing-lg;
    background: #fff;
    border-radius: 8px;
    margin-bottom: $spacing-md;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  }

  &__store-name {
    font-size: 16px;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 6px;
    color: $text-color;

    .el-icon {
      color: $primary-color;
    }
  }

  &__table {
    border-radius: 8px;
    overflow: hidden;
    margin-bottom: $spacing-lg;
  }

  &__subtotal {
    font-weight: 500;
    color: $danger-color;
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $spacing-lg;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  }

  &__total {
    font-size: 16px;
    color: $text-color;
  }

  &__total-price {
    font-size: 22px;
    font-weight: bold;
    color: $danger-color;
    margin: 0 4px;
  }

  &__total-info {
    font-size: 13px;
    color: $text-secondary;
    margin-left: $spacing-sm;
  }

  &__switch-dialog {
    .cart-page__switch-alert {
      margin-bottom: $spacing-md;
    }
  }

  &__switch-info {
    margin-bottom: $spacing-md;
  }

  &__order-confirm {
    .cart-page__total-price {
      font-size: 16px;
      font-weight: bold;
      color: $danger-color;
    }
  }

  &__order-items {
    max-height: 200px;
    overflow-y: auto;
  }

  &__order-item {
    display: flex;
    justify-content: space-between;
    padding: $spacing-xs 0;
    font-size: 14px;
    color: $text-secondary;
  }
}
</style>
