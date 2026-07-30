<template>
  <div class="merchant-detail-page">
    <div class="merchant-detail-page__header">
      <h2>
        <el-button text @click="$router.back()" :icon="ArrowLeft" />
        商家详情
      </h2>
      <div v-if="currentMerchant?.status === 'PENDING_REVIEW'" class="header-actions">
        <el-button type="success" @click="handleApprove">通过审核</el-button>
        <el-button type="danger" @click="handleReject">拒绝</el-button>
      </div>
    </div>

    <div v-if="loading" class="merchant-detail-page__loading">
      <el-skeleton :rows="10" animated />
    </div>

    <template v-if="currentMerchant">
      <!-- 商家基本信息 -->
      <el-card class="mb-lg">
        <template #header>
          <div class="card-header">
            <span>商家基本信息</span>
            <el-tag :type="getStatusType(currentMerchant.status)" size="large">
              {{ currentMerchant.statusLabel }}
            </el-tag>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商家名称">{{ currentMerchant.name }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentMerchant.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentMerchant.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="营业执照号">{{ currentMerchant.licenseNumber }}</el-descriptions-item>
          <el-descriptions-item label="入驻时间">{{ currentMerchant.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentMerchant.status)" size="small">
              {{ currentMerchant.statusLabel }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item
            v-if="currentMerchant.rejectReason"
            label="拒绝原因"
            :span="2"
          >
            <span class="reject-reason">{{ currentMerchant.rejectReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 门店列表 -->
      <el-card>
        <template #header>
          <span>门店列表 ({{ merchantStores.length }})</span>
        </template>
        <el-table
          :data="merchantStores"
          border
          stripe
          empty-text="暂无门店"
        >
          <el-table-column prop="name" label="门店名称" min-width="150" />
          <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
          <el-table-column prop="phone" label="电话" width="130" />
          <el-table-column prop="businessHours" label="营业时间" width="160" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStoreStatusType(row.status)" size="small">
                {{ row.statusLabel }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 拒绝原因弹窗 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝原因"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form :model="rejectForm" :rules="rejectRules" ref="rejectFormRef">
        <el-form-item prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因（必填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="confirmReject">
          确认拒绝
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useAdminMerchantStore } from '@/stores/adminMerchant'

const route = useRoute()
const router = useRouter()
const adminMerchantStore = useAdminMerchantStore()

const rejectDialogVisible = ref(false)
const rejecting = ref(false)
const rejectFormRef = ref<FormInstance>()

const rejectForm = reactive({ reason: '' })
const rejectRules: FormRules = {
  reason: [
    { required: true, message: '请填写拒绝原因', trigger: 'blur' },
  ],
}

const currentMerchant = computed(() => adminMerchantStore.currentMerchant)
const merchantStores = computed(() => adminMerchantStore.merchantStores)
const loading = computed(() => adminMerchantStore.loading)

const merchantId = computed(() => Number(route.params.id) || 0)

function getStatusType(status: string): 'warning' | 'success' | 'danger' | 'info' {
  const map: Record<string, 'warning' | 'success' | 'danger' | 'info'> = {
    PENDING_REVIEW: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    SUSPENDED: 'danger',
  }
  return map[status] || 'info'
}

function getStoreStatusType(status: string): 'warning' | 'success' | 'danger' | 'info' {
  const map: Record<string, 'warning' | 'success' | 'danger' | 'info'> = {
    PENDING_APPROVAL: 'warning',
    ACTIVE: 'success',
    CLOSED: 'info',
    SUSPENDED: 'danger',
  }
  return map[status] || 'info'
}

async function handleApprove() {
  if (!currentMerchant.value) return
  try {
    await adminMerchantStore.approveMerchant(currentMerchant.value.id)
    ElMessage.success('已通过审核')
  } catch (err: any) {
    ElMessage.error(err?.message || '操作失败')
  }
}

function handleReject() {
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!rejectFormRef.value || !currentMerchant.value) return
  await rejectFormRef.value.validate(async (valid) => {
    if (!valid) return
    rejecting.value = true
    try {
      await adminMerchantStore.rejectMerchant(currentMerchant.value!.id, rejectForm.reason)
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
    } catch (err: any) {
      ElMessage.error(err?.message || '操作失败')
    } finally {
    rejecting.value = false
    }
  })
}

onMounted(async () => {
  await adminMerchantStore.fetchMerchantDetail(merchantId.value)
  if (currentMerchant.value) {
    await adminMerchantStore.fetchMerchantStores(currentMerchant.value.id)
  }
})
</script>

<style lang="scss" scoped>
.merchant-detail-page {
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-lg;

    h2 {
      font-size: 22px;
      display: flex;
      align-items: center;
    }
  }

  &__loading {
    max-width: 900px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reject-reason {
  color: $danger-color;
}

.header-actions {
  display: flex;
  gap: $spacing-sm;
}
</style>
