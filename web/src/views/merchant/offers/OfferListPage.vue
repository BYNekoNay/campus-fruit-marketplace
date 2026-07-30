<template>
  <div class="offer-list-page">
    <div class="offer-list-page__header">
      <h2>商品报价</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建报价</el-button>
    </div>

    <el-card>
      <!-- 门店选择 -->
      <div class="offer-list-page__store-select">
        <span class="offer-list-page__label">选择门店：</span>
        <el-select
          v-model="selectedStoreId"
          placeholder="请选择门店"
          clearable
          @change="handleStoreChange"
        >
          <el-option
            v-for="store in stores"
            :key="store.id"
            :label="store.name"
            :value="store.id"
          />
        </el-select>
      </div>

      <el-table
        v-if="selectedStoreId"
        v-loading="loading"
        :data="offers"
        border
        stripe
        empty-text="暂无报价，点击右上角新建报价"
      >
        <el-table-column label="水果品种" min-width="160">
          <template #default="{ row }">
            <div>
              <span class="offer__variety">{{ row.fruitVariety }}</span>
              <el-tag size="small" style="margin-left: 4px">{{ row.fruitGrade }}</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="规格" width="80">
          <template #default="{ row }">
            {{ row.fruitCategory }}
          </template>
        </el-table-column>

        <el-table-column label="销售单位" min-width="120">
          <template #default="{ row }">
            {{ row.salesUnit }}
          </template>
        </el-table-column>

        <el-table-column label="单价" width="100" align="right">
          <template #default="{ row }">
            <span class="offer__price">¥{{ centsToYuan(row.unitPrice) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="标准价/500g" width="140" align="right">
          <template #default="{ row }">
            <template v-if="row.isComparable">
              ¥{{ centsToYuan(row.standardPricePer500g || 0) }}
            </template>
            <span v-else class="offer__incomparable">按个/盒</span>
          </template>
        </el-table-column>

        <el-table-column label="库存" width="80" align="right">
          <template #default="{ row }">
            {{ row.stockQuantity }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <div class="offer__status-cell">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
              <el-tag
                v-if="row.priceStale"
                type="warning"
                size="small"
                effect="dark"
                class="offer__stale-tag"
              >
                价格可能已变化
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button
              size="small"
              type="warning"
              @click="handleConfirmPrice(row)"
            >
              确认价格
            </el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              size="small"
              type="info"
              @click="handlePause(row)"
            >
              暂停
            </el-button>
            <el-button
              v-if="row.status === 'PAUSED'"
              size="small"
              type="success"
              @click="handleActivate(row)"
            >
              激活
            </el-button>
            <el-button size="small" :icon="Clock" text @click="openHistory(row)">
              历史
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!selectedStoreId" description="请先选择门店查看报价" />
    </el-card>

    <!-- 创建/编辑弹窗 -->
    <OfferFormDialog
      v-model="offerDialogVisible"
      :store-id="selectedStoreId"
      :edit-offer="editOffer"
      @submit="handleOfferSubmit"
    />

    <!-- 价格历史弹窗 -->
    <PriceHistoryDialog
      v-model="historyDialogVisible"
      :offer-id="historyOfferId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Plus, Edit, Clock } from '@element-plus/icons-vue'
import { useOfferStore } from '@/stores/offer'
import { useMerchantStore } from '@/stores/merchant'
import { centsToYuan } from '@/utils/format'
import OfferFormDialog from './OfferFormDialog.vue'
import PriceHistoryDialog from './PriceHistoryDialog.vue'

const offerStore = useOfferStore()
const merchantStore = useMerchantStore()

const selectedStoreId = ref(0)
const loading = computed(() => offerStore.loading)
const stores = computed(() => merchantStore.activeStores)

const offerDialogVisible = ref(false)
const editOffer = ref<FruitOffer | null>(null)

const historyDialogVisible = ref(false)
const historyOfferId = ref(0)

const offers = computed(() => offerStore.offers)

function getStatusType(status: string): 'success' | 'info' | 'danger' {
  const map: Record<string, 'success' | 'info' | 'danger'> = {
    ACTIVE: 'success',
    PAUSED: 'info',
    EXPIRED: 'danger',
  }
  return map[status] || 'info'
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    ACTIVE: '在售',
    PAUSED: '已暂停',
    EXPIRED: '已过期',
  }
  return map[status] || status
}

async function handleStoreChange(storeId: number) {
  if (storeId) {
    await offerStore.fetchOffers(storeId)
  }
}

function openCreate() {
  if (!selectedStoreId.value) {
    ElMessage.warning('请先选择门店')
    return
  }
  editOffer.value = null
  offerDialogVisible.value = true
}

function openEdit(row: FruitOffer) {
  editOffer.value = row
  offerDialogVisible.value = true
}

async function handleOfferSubmit(dto: CreateOfferRequest) {
  if (editOffer.value) {
    await offerStore.updateOffer(editOffer.value.id, {
      salesUnit: dto.salesUnit,
      netWeightGrams: dto.netWeightGrams,
      unitPrice: dto.unitPrice,
      stockQuantity: dto.stockQuantity,
      qualityDesc: dto.qualityDesc,
    })
  } else {
    await offerStore.createOffer(dto)
  }
}

async function handleConfirmPrice(row: FruitOffer) {
  try {
    await ElMessageBox.confirm(
      `确认「${row.fruitVariety}」当前价格 ¥${centsToYuan(row.unitPrice)} / ${row.salesUnit} 无误？`,
      '确认价格',
      { confirmButtonText: '确认无误', cancelButtonText: '取消', type: 'info' }
    )
    await offerStore.confirmPrice(row.id)
    ElMessage.success('价格已确认')
  } catch {
    // 用户取消
  }
}

async function handlePause(row: FruitOffer) {
  try {
    await ElMessageBox.confirm(
      `确认暂停「${row.fruitVariety}」报价？暂停后用户将无法下单。`,
      '暂停确认',
      { confirmButtonText: '确认暂停', cancelButtonText: '取消', type: 'warning' }
    )
    await offerStore.pauseOffer(row.id)
    ElMessage.success('已暂停')
  } catch {
    // 用户取消
  }
}

async function handleActivate(row: FruitOffer) {
  await offerStore.activateOffer(row.id)
  ElMessage.success('已激活')
}

function openHistory(row: FruitOffer) {
  historyOfferId.value = row.id
  historyDialogVisible.value = true
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
.offer-list-page {
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-lg;

    h2 {
      font-size: 22px;
    }
  }

  &__store-select {
    display: flex;
    align-items: center;
    margin-bottom: $spacing-md;
    gap: $spacing-sm;
  }

  &__label {
    font-size: 14px;
    color: $text-regular;
  }
}

.offer {
  &__variety {
    font-weight: 500;
  }

  &__price {
    font-weight: bold;
    color: #F56C6C;
  }

  &__incomparable {
    color: #F56C6C;
    font-size: 12px;
  }

  &__status-cell {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__stale-tag {
    align-self: flex-start;
  }
}
</style>
