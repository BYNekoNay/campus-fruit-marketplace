<template>
  <el-card class="review-card" shadow="hover">
    <div class="review-header">
      <div class="reviewer-info">
        <el-avatar :size="40" icon="UserFilled" />
        <div class="reviewer-meta">
          <div class="reviewer-name">{{ review.userId ? '用户' + review.userId : '匿名用户' }}</div>
          <div class="review-time">{{ formatTime(review.createdAt) }}</div>
        </div>
      </div>
      <div class="review-actions">
        <el-tag v-if="review.orderId" type="success" size="small" effect="dark">已购</el-tag>
        <StarRating :model-value="review.rating" readonly :size="16" />
        <el-dropdown trigger="click" v-if="!hideActions">
          <el-button link type="primary" @click.stop>···</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$emit('report', review.id)">举报</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="review-tags" v-if="review.tags?.length">
      <el-tag v-for="tag in review.tags" :key="tag" size="small" type="warning" effect="plain" style="margin-right:4px">
        {{ tag }}
      </el-tag>
      <el-tag v-if="review.version > 1" size="small" type="info">已修改</el-tag>
    </div>

    <div class="review-content" v-if="review.content">
      {{ review.content }}
    </div>

    <div class="merchant-reply" v-if="review.merchantReply">
      <div class="reply-label">
        <el-icon><Shop /></el-icon> 商家回复
      </div>
      <div class="reply-content">{{ review.merchantReply.content }}</div>
      <div class="reply-time">{{ formatTime(review.merchantReply.createdAt) }}</div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import StarRating from '@/components/StarRating.vue'

defineProps<{
  review: ReviewInfo
  hideActions?: boolean
}>()

defineEmits<{
  report: [reviewId: number]
}>()

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 60) return `${Math.max(1, diffMin)}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}小时前`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 30) return `${diffDay}天前`
  return d.toLocaleDateString('zh-CN')
}
</script>

<style scoped lang="scss">
.review-card {
  margin-bottom: 12px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.reviewer-meta {
  .reviewer-name {
    font-weight: 500;
    font-size: 14px;
  }
  .review-time {
    font-size: 12px;
    color: #909399;
    margin-top: 2px;
  }
}

.review-tags {
  margin: 12px 0;
}

.review-content {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
}

.merchant-reply {
  margin-top: 16px;
  padding: 12px 16px;
  background: #f0f9eb;
  border-radius: 8px;
  border-left: 3px solid #67c23a;

  .reply-label {
    font-size: 12px;
    color: #67c23a;
    font-weight: 500;
    margin-bottom: 6px;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .reply-content {
    font-size: 14px;
    color: #606266;
    line-height: 1.6;
  }

  .reply-time {
    font-size: 12px;
    color: #c0c4cc;
    margin-top: 6px;
  }
}
</style>
