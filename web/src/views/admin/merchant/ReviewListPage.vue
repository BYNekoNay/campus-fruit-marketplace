<template>
  <div class="review-list-page">
    <h2 class="review-list-page__title">商家审核</h2>

    <el-card>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="ALL" />
        <el-tab-pane :label="`待审核 (${pendingTotal})`" name="PENDING_REVIEW" />
        <el-tab-pane :label="`已通过 (${approvedTotal})`" name="APPROVED" />
        <el-tab-pane :label="`已拒绝 (${rejectedTotal})`" name="REJECTED" />
      </el-tabs>

      <el-table
        v-loading="loading"
        :data="merchants"
        border
        stripe
        empty-text="暂无数据"
      >
        <el-table-column prop="name" label="商家名称" min-width="160" />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="licenseNumber" label="营业执照号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="申请时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              @click="$router.push(`/admin/merchants/${row.id}`)"
            >
              详情
            </el-button>
            <template v-if="row.status === 'PENDING_REVIEW'">
              <el-button
                size="small"
                type="success"
                @click="handleApprove(row)"
              >
                通过
              </el-button>
              <el-button
                size="small"
                type="danger"
                @click="handleReject(row)"
              >
                拒绝
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="review-list-page__pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @change="fetchData"
        />
      </div>
    </el-card>

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
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAdminMerchantStore } from '@/stores/adminMerchant'

const adminMerchantStore = useAdminMerchantStore()

const activeTab = ref('ALL')
const rejectDialogVisible = ref(false)
const rejecting = ref(false)
const rejectTargetId = ref(0)
const rejectFormRef = ref<FormInstance>()

const rejectForm = reactive({ reason: '' })
const rejectRules: FormRules = {
  reason: [
    { required: true, message: '请填写拒绝原因', trigger: 'blur' },
  ],
}

const merchants = computed(() => adminMerchantStore.merchants)
const loading = computed(() => adminMerchantStore.loading)
const pagination = computed(() => adminMerchantStore.pagination)

const pendingTotal = ref(0)
const approvedTotal = ref(0)
const rejectedTotal = ref(0)

function getStatusType(status: string): 'warning' | 'success' | 'danger' | 'info' {
  const map: Record<string, 'warning' | 'success' | 'danger' | 'info'> = {
    PENDING_REVIEW: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    SUSPENDED: 'danger',
  }
  return map[status] || 'info'
}

function handleTabChange(tab: string) {
  activeTab.value = tab
  pagination.value.page = 1
  fetchData()
}

function buildParams() {
  const params: { page: number; pageSize: number; status?: string } = {
    page: pagination.value.page,
    pageSize: pagination.value.pageSize,
  }
  if (activeTab.value !== 'ALL') {
    params.status = activeTab.value
  }
  return params
}

async function fetchData() {
  await adminMerchantStore.fetchMerchants(buildParams())
  // 更新各状态计数（简化方案：从当前页推算；实际应调用独立统计接口）
  const all = adminMerchantStore.merchants
  pendingTotal.value = all.filter((m) => m.status === 'PENDING_REVIEW').length
  approvedTotal.value = all.filter((m) => m.status === 'APPROVED').length
  rejectedTotal.value = all.filter((m) => m.status === 'REJECTED').length
}

async function handleApprove(row: MerchantInfo) {
  try {
    await adminMerchantStore.approveMerchant(row.id)
    ElMessage.success('已通过审核')
  } catch (err: any) {
    ElMessage.error(err?.message || '操作失败')
  }
}

function handleReject(row: MerchantInfo) {
  rejectTargetId.value = row.id
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!rejectFormRef.value) return
  await rejectFormRef.value.validate(async (valid) => {
    if (!valid) return
    rejecting.value = true
    try {
      await adminMerchantStore.rejectMerchant(rejectTargetId.value, rejectForm.reason)
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
    } catch (err: any) {
      ElMessage.error(err?.message || '操作失败')
    } finally {
      rejecting.value = false
    }
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.review-list-page {
  &__title {
    font-size: 22px;
    margin-bottom: $spacing-lg;
  }

  &__pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: $spacing-lg;
  }
}
</style>
