<template>
  <div class="report-list-page">
    <h2>举报审核管理</h2>
    <el-table :data="reviewStore.reports" v-loading="reviewStore.loading" stripe>
      <el-table-column label="评价摘要" min-width="200">
        <template #default="{ row }">
          <div class="review-cell">
            <StarRating :model-value="row.review?.rating || 0" readonly :size="14" />
            <span class="review-text">{{ row.review?.content?.substring(0, 60) || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="举报原因" width="180" />
      <el-table-column label="举报时间" width="160">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'ACCEPTED' ? 'danger' : 'info'" size="small">
            {{ row.status === 'PENDING' ? '待审核' : row.status === 'ACCEPTED' ? '已采纳' : '已驳回' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" v-if="true">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button size="small" type="danger" @click="handleReview(row, 'ACCEPT')">采纳</el-button>
            <el-button size="small" @click="handleReview(row, 'DISMISS')">驳回</el-button>
          </template>
          <span v-else style="color:#909399">已处理</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import StarRating from '@/components/StarRating.vue'
import { useReviewStore } from '@/stores/review'

const reviewStore = useReviewStore()

onMounted(() => {
  reviewStore.fetchPendingReports()
})

async function handleReview(row: any, action: 'ACCEPT' | 'DISMISS') {
  const title = action === 'ACCEPT' ? '采纳举报（隐藏评价）' : '驳回举报'
  const msg = action === 'ACCEPT' ? '采纳后将隐藏该评价，确定？' : '驳回后评价保持可见，确定？'
  try {
    await ElMessageBox.confirm(msg, title, {
      confirmButtonText: action === 'ACCEPT' ? '采纳' : '驳回',
      cancelButtonText: '取消',
      type: action === 'ACCEPT' ? 'warning' : 'info'
    })
    await reviewStore.reviewReport(row.id, action, '')
  } catch { /* cancelled */ }
}
</script>

<style scoped lang="scss">
.report-list-page {
  padding: 20px;
  h2 { margin-bottom: 16px; }
}

.review-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-text {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}
</style>
