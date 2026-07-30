<template>
  <div class="apply-page">
    <div class="apply-page__header">
      <h2>商家入驻申请</h2>
      <p class="apply-page__desc">填写以下信息，提交商家入驻申请</p>
    </div>

    <el-card class="apply-page__card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        label-position="right"
        size="large"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="商家名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入商家名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="联系人" prop="contactName">
          <el-input
            v-model="form.contactName"
            placeholder="请输入联系人姓名"
            maxlength="20"
          />
        </el-form-item>

        <el-form-item label="联系电话" prop="contactPhone">
          <el-input
            v-model="form.contactPhone"
            placeholder="请输入联系电话"
            maxlength="11"
          />
        </el-form-item>

        <el-form-item label="营业执照号" prop="licenseNumber">
          <el-input
            v-model="form.licenseNumber"
            placeholder="请输入统一社会信用代码"
            maxlength="18"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
          >
            提交申请
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useMerchantStore } from '@/stores/merchant'

const router = useRouter()
const merchantStore = useMerchantStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive<CreateMerchantRequest>({
  name: '',
  contactName: '',
  contactPhone: '',
  licenseNumber: '',
})

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
    { required: true, message: '请输入商家名称', trigger: 'blur' },
    { min: 2, max: 50, message: '商家名称长度在 2 到 50 个字符', trigger: 'blur' },
  ],
  contactName: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '联系人姓名长度在 2 到 20 个字符', trigger: 'blur' },
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' },
  ],
  licenseNumber: [
    { required: true, message: '请输入营业执照号', trigger: 'blur' },
    { min: 18, max: 18, message: '营业执照号为 18 位统一社会信用代码', trigger: 'blur' },
  ],
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await merchantStore.applyMerchant({ ...form })
      ElMessage.success('申请已提交，请等待审核')
      router.push('/merchant/dashboard')
    } catch (err: any) {
      ElMessage.error(err?.message || '提交失败')
    } finally {
      submitting.value = false
    }
  })
}

function handleReset() {
  formRef.value?.resetFields()
}
</script>

<style lang="scss" scoped>
.apply-page {
  max-width: 640px;
  margin: 0 auto;

  &__header {
    margin-bottom: $spacing-lg;

    h2 {
      font-size: 22px;
      margin-bottom: $spacing-sm;
    }
  }

  &__desc {
    color: $text-secondary;
    font-size: 14px;
  }

  &__card {
    :deep(.el-card__body) {
      padding: $spacing-xl;
    }
  }
}
</style>
