<template>
  <div class="store-form-page">
    <div class="store-form-page__header">
      <h2>{{ isEdit ? '编辑门店' : '新建门店' }}</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card class="store-form-page__card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="right"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="门店名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入门店名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="地址" prop="address">
          <el-input
            v-model="form.address"
            placeholder="请输入详细地址"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <!-- 地图选点 -->
        <el-form-item label="门店坐标" prop="coordinate">
          <MapPicker
            v-model:latitude="form.latitude"
            v-model:longitude="form.longitude"
            :address="form.address"
          />
        </el-form-item>

        <el-form-item label="联系电话" prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入门店联系电话"
            maxlength="11"
          />
        </el-form-item>

        <el-form-item label="营业时间" prop="businessHours">
          <el-input
            v-model="form.businessHours"
            placeholder="例如：08:00-22:00"
            maxlength="50"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '创建门店' }}
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useMerchantStore } from '@/stores/merchant'
import MapPicker from './MapPicker.vue'

const route = useRoute()
const router = useRouter()
const merchantStore = useMerchantStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const isEdit = computed(() => !!route.params.id && route.params.id !== 'create')
const storeId = computed(() => Number(route.params.id) || 0)

const form = reactive<CreateStoreRequest>({
  name: '',
  address: '',
  latitude: 0,
  longitude: 0,
  phone: '',
  businessHours: '',
})

const validateCoordinate = (_rule: any, _value: any, callback: any) => {
  if (form.latitude === 0 && form.longitude === 0) {
    callback(new Error('请在地图上选择门店位置'))
  } else if (form.latitude < -90 || form.latitude > 90) {
    callback(new Error('纬度范围: -90 ~ 90'))
  } else if (form.longitude < -180 || form.longitude > 180) {
    callback(new Error('经度范围: -180 ~ 180'))
  } else {
    callback()
  }
}

const validatePhone = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入联系电话'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号码'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  name: [
    { required: true, message: '请输入门店名称', trigger: 'blur' },
    { min: 2, max: 50, message: '门店名称长度在 2 到 50 个字符', trigger: 'blur' },
  ],
  address: [
    { required: true, message: '请输入地址', trigger: 'blur' },
    { min: 5, max: 100, message: '地址长度在 5 到 100 个字符', trigger: 'blur' },
  ],
  coordinate: [
    { validator: validateCoordinate, trigger: 'change' },
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' },
  ],
  businessHours: [
    { required: true, message: '请输入营业时间', trigger: 'blur' },
  ],
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (!merchantStore.merchant) {
        await merchantStore.fetchMyMerchant()
      }
      if (!merchantStore.merchant) {
        ElMessage.error('未找到商家信息')
        return
      }
      if (isEdit.value) {
        await merchantStore.updateStore(storeId.value, { ...form })
        ElMessage.success('门店信息已更新')
      } else {
        await merchantStore.createStore(merchantStore.merchant.id, { ...form })
        ElMessage.success('门店创建成功')
      }
      router.push('/merchant/stores')
    } catch (err: any) {
      ElMessage.error(err?.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

onMounted(async () => {
  // 编辑模式：加载已有门店数据
  if (isEdit.value && storeId.value) {
    if (!merchantStore.merchant) {
      await merchantStore.fetchMyMerchant()
    }
    if (merchantStore.merchant) {
      const existing = merchantStore.stores.find((s) => s.id === storeId.value)
      if (existing) {
        form.name = existing.name
        form.address = existing.address
        form.latitude = existing.latitude
        form.longitude = existing.longitude
        form.phone = existing.phone
        form.businessHours = existing.businessHours
      } else {
        // 门店不在 store 中，先加载
        await merchantStore.fetchStores(merchantStore.merchant.id)
        const found = merchantStore.stores.find((s) => s.id === storeId.value)
        if (found) {
          form.name = found.name
          form.address = found.address
          form.latitude = found.latitude
          form.longitude = found.longitude
          form.phone = found.phone
          form.businessHours = found.businessHours
        }
      }
    }
  }
})
</script>

<style lang="scss" scoped>
.store-form-page {
  max-width: 800px;
  margin: 0 auto;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-lg;

    h2 {
      font-size: 22px;
    }
  }

  &__card {
    :deep(.el-card__body) {
      padding: $spacing-xl;
    }
  }
}
</style>
