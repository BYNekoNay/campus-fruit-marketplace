<template>
  <div class="review-submit-page">
    <el-card class="submit-card">
      <template #header>
        <div class="card-header">
          <span>提交评价</span>
        </div>
      </template>

      <div class="order-summary" v-if="orderInfo">
        <h4>订单摘要</h4>
        <div class="order-items">
          <el-tag v-for="item in orderInfo.items" :key="item.id" type="info" style="margin:4px">
            {{ item.fruitVariety }} ×{{ item.quantity }}
          </el-tag>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" style="margin-top:24px">
        <el-form-item label="评分" prop="rating">
          <StarRating v-model="form.rating" />
        </el-form-item>

        <el-form-item label="标签" prop="tags">
          <el-checkbox-group v-model="form.tags">
            <el-checkbox label="新鲜" />
            <el-checkbox label="实惠" />
            <el-checkbox label="包装好" />
            <el-checkbox label="服务好" />
            <el-checkbox label="分量足" />
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="评价" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="分享你的购物体验..."
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交评价</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StarRating from '@/components/StarRating.vue'
import { useReviewStore } from '@/stores/review'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const reviewStore = useReviewStore()
const formRef = ref()
const submitting = ref(false)
const orderInfo = ref<any>(null)

const orderId = Number(route.query.orderId)
const storeId = Number(route.query.storeId)

const form = reactive({
  rating: 0,
  tags: [] as string[],
  content: ''
})

const rules = {
  rating: [{ required: true, message: '请评星', trigger: 'change', validator: (_r: any, v: number, cb: any) => v > 0 ? cb() : cb(new Error('请评星')) }],
  content: [{ required: true, message: '请输入评价内容', trigger: 'blur' }]
}

onMounted(async () => {
  if (orderId) {
    try {
      const res = await request.get(`/api/orders/${orderId}`)
      orderInfo.value = res
    } catch { /* ignore */ }
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await reviewStore.submitReview({
      storeId: storeId,
      orderId: orderId,
      rating: form.rating,
      content: form.content,
      tags: form.tags
    })
    ElMessage.success('评价提交成功')
    router.push(`/orders/${orderId}`)
  } catch {
    ElMessage.error('评价提交失败，可能已评价过或暂无资格')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.review-submit-page {
  max-width: 680px;
  margin: 24px auto;
  padding: 0 16px;
}

.submit-card {
  .card-header {
    font-size: 18px;
    font-weight: 600;
  }
}

.order-summary {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 8px;

  h4 {
    margin: 0 0 8px;
    font-size: 14px;
    color: #606266;
  }
}
</style>
