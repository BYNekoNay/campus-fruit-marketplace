<template>
  <el-dialog
    v-model="visible"
    title="水果价格对比"
    width="95%"
    top="2vh"
    :close-on-click-modal="false"
    destroy-on-close
    class="compare-dialog"
  >
    <template v-if="compareData">
      <!-- 统计面板 -->
      <div class="compare-dialog__stats">
        <div class="compare-dialog__stat-item">
          <div class="compare-dialog__stat-label">最低价</div>
          <div class="compare-dialog__stat-value compare-dialog__stat-value--low">
            ¥{{ compareData.stats.minPrice.toFixed(2) }}
          </div>
        </div>
        <div class="compare-dialog__stat-item">
          <div class="compare-dialog__stat-label">最高价</div>
          <div class="compare-dialog__stat-value compare-dialog__stat-value--high">
            ¥{{ compareData.stats.maxPrice.toFixed(2) }}
          </div>
        </div>
        <div class="compare-dialog__stat-item">
          <div class="compare-dialog__stat-label">中位价</div>
          <div class="compare-dialog__stat-value">
            ¥{{ compareData.stats.medianPrice.toFixed(2) }}
          </div>
        </div>
        <div class="compare-dialog__stat-item">
          <div class="compare-dialog__stat-label">平均价</div>
          <div class="compare-dialog__stat-value">
            ¥{{ compareData.stats.avgPrice.toFixed(2) }}
          </div>
        </div>
        <div class="compare-dialog__stat-item">
          <div class="compare-dialog__stat-label">参与门店 / 样本</div>
          <div class="compare-dialog__stat-value">
            {{ compareData.stats.storeCount }} / {{ compareData.stats.sampleCount }}
          </div>
        </div>
      </div>

      <!-- 样本不足警告 -->
      <el-alert
        v-if="compareData.stats.sampleInsufficient"
        title="样本不足"
        type="warning"
        description="当前参与比价的样本数量较少，价格统计可能不够准确，建议增加更多门店进行对比。"
        show-icon
        :closable="false"
        class="compare-dialog__alert"
      />

      <!-- 比价表格 -->
      <el-table
        :data="compareData.offers"
        stripe
        border
        style="width: 100%"
        :row-class-name="tableRowClassName"
      >
        <el-table-column prop="storeName" label="门店名称" min-width="140" />
        <el-table-column prop="storeAddress" label="地址" min-width="160" />
        <el-table-column label="距离" width="100" align="center">
          <template #default="{ row }">
            {{ row.distance != null ? `${row.distance.toFixed(1)}km` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="fruitVariety" label="规格" width="120" />
        <el-table-column prop="salesUnit" label="销售单位" width="120" />
        <el-table-column label="单价" width="100" align="right" sortable>
          <template #default="{ row }">
            <span
              :class="{
                'compare-dialog__price--lowest': row.unitPrice === compareData!.stats.minPrice,
              }"
            >
              ¥{{ row.unitPrice.toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="标准价/500g" width="120" align="right">
          <template #default="{ row }">
            <template v-if="row.isComparable && row.standardPricePer500g != null">
              ¥{{ row.standardPricePer500g.toFixed(2) }}
            </template>
            <el-tag v-else type="info" size="small">不可比</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="库存状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.stockStatus === '充足' ? 'success' : row.stockStatus === '紧张' ? 'warning' : 'danger'"
              size="small"
            >
              {{ row.stockStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="100" align="center">
          <template #default="{ row }">
            <el-rate
              :model-value="row.avgRating"
              disabled
              size="small"
              show-score
              score-template="{value}"
            />
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 加载中 -->
    <div v-else class="compare-dialog__loading">
      <el-skeleton :rows="6" animated />
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: boolean
  compareData: CompareResponse | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = ref(props.modelValue)

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

function tableRowClassName({ row }: { row: CompareItem }): string {
  if (!row.isComparable) return 'row--non-comparable'
  if (
    props.compareData &&
    row.unitPrice === props.compareData.stats.minPrice
  ) {
    return 'row--lowest-price'
  }
  return ''
}
</script>

<style lang="scss" scoped>
.compare-dialog {
  &__stats {
    display: flex;
    gap: $spacing-md;
    margin-bottom: $spacing-lg;
    flex-wrap: wrap;
  }

  &__stat-item {
    flex: 1;
    min-width: 120px;
    background: $bg-color;
    border-radius: $border-radius;
    padding: $spacing-md;
    text-align: center;
  }

  &__stat-label {
    font-size: $font-size-sm;
    color: $text-secondary;
    margin-bottom: 4px;
  }

  &__stat-value {
    font-size: 20px;
    font-weight: 700;
    color: $text-primary;

    &--low {
      color: $success-color;
    }

    &--high {
      color: $danger-color;
    }
  }

  &__alert {
    margin-bottom: $spacing-lg;
  }

  &__loading {
    padding: $spacing-xl;
  }

  &__price--lowest {
    color: $success-color;
    font-weight: 700;
  }
}
</style>

<style lang="scss">
// 全局样式：表格行高亮
.compare-dialog {
  .row--lowest-price {
    background-color: rgba(103, 194, 58, 0.08) !important;
  }

  .row--non-comparable {
    background-color: #fafafa !important;
    color: $text-secondary;
  }
}
</style>
