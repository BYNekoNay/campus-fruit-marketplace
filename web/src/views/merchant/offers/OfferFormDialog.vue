<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑报价' : '新建报价'"
    width="560px"
    :close-on-click-modal="true"
    destroy-on-close
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="110px"
      @keydown.esc="visible = false"
    >
      <el-form-item label="标准水果" prop="canonicalFruitId">
        <el-select
          v-model="form.canonicalFruitId"
          filterable
          remote
          reserve-keyword
          :remote-method="searchFruits"
          :loading="fruitSearchLoading"
          placeholder="输入品种名称搜索"
          clearable
          @change="onFruitSelected"
        >
          <el-option
            v-for="f in fruitOptions"
            :key="f.id"
            :label="`${f.category} - ${f.variety}（${f.grade}）`"
            :value="f.id"
          />
        </el-select>
        <div v-if="selectedFruit" class="offer-form__fruit-info">
          <el-tag size="small">{{ selectedFruit.category }}</el-tag>
          <el-tag size="small" type="success">{{ selectedFruit.grade }}</el-tag>
          <span>产地：{{ selectedFruit.origin }}</span>
        </div>
      </el-form-item>

      <el-form-item label="销售单位" prop="salesUnit">
        <el-input v-model="form.salesUnit" placeholder="如：500g盒装、1kg散装、个" maxlength="30" show-word-limit />
      </el-form-item>

      <el-form-item label="净重(克)" prop="netWeightGrams">
        <el-input-number
          v-model="form.netWeightGrams"
          :min="0"
          :max="999999"
          placeholder="如：500"
          controls-position="right"
          style="width: 100%"
        />
        <span class="offer-form__hint">按个/盒可不填，将标记为"不可比"</span>
      </el-form-item>

      <el-form-item label="单价(元)" prop="unitPrice">
        <el-input-number
          v-model="form.unitPriceYuan"
          :min="0.01"
          :max="999999"
          :precision="2"
          placeholder="如：12.50"
          controls-position="right"
          style="width: 100%"
        />
        <span v-if="isEdit && editOffer?.standardPricePer500g" class="offer-form__preview">
          当前标准价：{{ centsToYuan(editOffer.standardPricePer500g) }} 元/500g
        </span>
      </el-form-item>

      <el-form-item label="库存数量" prop="stockQuantity">
        <el-input-number
          v-model="form.stockQuantity"
          :min="0"
          :max="99999"
          :step="1"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="质量说明" prop="qualityDesc">
        <el-input
          v-model="form.qualityDesc"
          type="textarea"
          :rows="3"
          placeholder="选填：描述水果的新鲜度、大小、口感等"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEdit ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, toRaw } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useOfferStore } from '@/stores/offer'
import { centsToYuan } from '@/utils/format'

const props = defineProps<{
  modelValue: boolean
  storeId: number
  editOffer?: FruitOffer | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'submit', dto: CreateOfferRequest): void
}>()

const offerStore = useOfferStore()

const visible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const fruitSearchLoading = ref(false)
const fruitOptions = ref<CanonicalFruit[]>([])
const selectedFruit = ref<CanonicalFruit | null>(null)

const form = reactive({
  canonicalFruitId: 0,
  salesUnit: '',
  netWeightGrams: null as number | null,
  unitPriceYuan: 0,
  stockQuantity: 0,
  qualityDesc: '',
})

const isEdit = ref(false)

const rules: FormRules = {
  canonicalFruitId: [
    { required: true, message: '请选择标准水果', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (value === 0) {
          callback(new Error('请选择标准水果'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
  salesUnit: [
    { required: true, message: '请输入销售单位', trigger: 'blur' },
    { max: 30, message: '销售单位最多 30 个字符', trigger: 'blur' },
  ],
  unitPriceYuan: [
    { required: true, message: '请输入单价', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value === undefined || value === null || value <= 0) {
          callback(new Error('单价必须大于 0'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  stockQuantity: [
    { required: true, message: '请输入库存数量', trigger: 'blur' },
  ],
}

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val) {
      if (props.editOffer) {
        isEdit.value = true
        form.canonicalFruitId = props.editOffer.canonicalFruitId
        form.salesUnit = props.editOffer.salesUnit
        form.netWeightGrams = props.editOffer.netWeightGrams
        form.unitPriceYuan = parseFloat(centsToYuan(props.editOffer.unitPrice))
        form.stockQuantity = props.editOffer.stockQuantity
        form.qualityDesc = props.editOffer.qualityDesc || ''
        // 回填选中水果信息
        selectedFruit.value = {
          id: props.editOffer.canonicalFruitId,
          category: props.editOffer.fruitCategory,
          variety: props.editOffer.fruitVariety,
          grade: props.editOffer.fruitGrade,
          origin: '',
          defaultUnit: '',
          comparisonGroupId: null,
          version: 0,
          status: 'ACTIVE',
          createdAt: '',
        }
        fruitOptions.value = [selectedFruit.value]
      } else {
        isEdit.value = false
        resetForm()
      }
    }
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

function resetForm() {
  form.canonicalFruitId = 0
  form.salesUnit = ''
  form.netWeightGrams = null
  form.unitPriceYuan = 0
  form.stockQuantity = 0
  form.qualityDesc = ''
  selectedFruit.value = null
  fruitOptions.value = []
  formRef.value?.clearValidate()
}

function handleClosed() {
  resetForm()
}

async function searchFruits(query: string) {
  if (!query || query.length < 1) {
    fruitOptions.value = []
    return
  }
  fruitSearchLoading.value = true
  try {
    const data = await offerStore.fetchFruits(query)
    fruitOptions.value = data.filter((f) => f.status === 'ACTIVE')
  } finally {
    fruitSearchLoading.value = false
  }
}

function onFruitSelected(id: number) {
  if (id) {
    selectedFruit.value = fruitOptions.value.find((f) => f.id === id) || null
  } else {
    selectedFruit.value = null
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const dto: CreateOfferRequest = {
        storeId: props.storeId,
        canonicalFruitId: form.canonicalFruitId,
        salesUnit: form.salesUnit,
        netWeightGrams: form.netWeightGrams,
        unitPrice: Math.round(form.unitPriceYuan * 100), // 元转分
        stockQuantity: form.stockQuantity,
        qualityDesc: form.qualityDesc,
      }
      emit('submit', dto)
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      visible.value = false
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.offer-form {
  &__fruit-info {
    margin-top: 6px;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: $text-secondary;
  }

  &__hint {
    display: block;
    font-size: 12px;
    color: $text-secondary;
    margin-top: 4px;
  }

  &__preview {
    display: block;
    font-size: 12px;
    color: $primary-color;
    margin-top: 4px;
  }
}
</style>
