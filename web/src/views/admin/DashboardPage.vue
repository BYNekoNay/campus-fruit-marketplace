<template>
  <div class="admin-dashboard">
    <h2>平台数据概览</h2>

    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6" v-for="stat in stats" :key="stat.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{color: stat.color}">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
          <div class="stat-trend" v-if="stat.trend">
            <span :class="stat.trend > 0 ? 'trend-up' : 'trend-down'">
              {{ stat.trend > 0 ? '↑' : '↓' }} {{ Math.abs(stat.trend) }}%
            </span>
            较上周
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:20px">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header><span>近7天订单趋势</span></template>
          <div class="chart-placeholder">
            <div v-for="(day, i) in orderTrend" :key="i" class="chart-bar">
              <div class="bar" :style="{height: (day.count / maxOrder * 100) + '%'}"></div>
              <span class="bar-label">{{ day.date }}</span>
              <span class="bar-value">{{ day.count }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header><span>待办事项</span></template>
          <el-timeline>
            <el-timeline-item
              v-for="item in todos"
              :key="item.id"
              :timestamp="item.time"
              :type="item.type"
            >
              <router-link :to="item.link">{{ item.text }}</router-link>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="todos.length === 0" description="暂无待办" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'

const stats = ref([
  { label: '平台用户', value: 0, color: '#409EFF', trend: 0 },
  { label: '认证商家', value: 0, color: '#67C23A', trend: 0 },
  { label: '活跃门店', value: 0, color: '#E6A23C', trend: 0 },
  { label: '今日订单', value: 0, color: '#F56C6C', trend: 0 },
])

const orderTrend = ref([
  { date: '周一', count: 0 }, { date: '周二', count: 0 },
  { date: '周三', count: 0 }, { date: '周四', count: 0 },
  { date: '周五', count: 0 }, { date: '周六', count: 0 },
  { date: '周日', count: 0 },
])

const todos = ref([
  { id: 1, text: '审核商家入驻申请', time: '刚刚', link: '/admin/merchants', type: 'warning' as const },
  { id: 2, text: '处理评价举报', time: '10分钟前', link: '/admin/reports', type: 'danger' as const },
])

const maxOrder = computed(() => Math.max(1, ...orderTrend.value.map(d => d.count)))

onMounted(async () => {
  try {
    const [users, orders] = await Promise.all([
      request.get('/api/admin/users?size=1').catch(() => ({ totalCount: 0 })),
      request.get('/api/admin/orders/stats').catch(() => ({})),
    ])
    stats.value[0].value = (users as any).totalCount ?? 0
    stats.value[3].value = (orders as any).todayCount ?? 0
  } catch { /* gracefully handle */ }
})
</script>

<style scoped lang="scss">
.admin-dashboard {
  padding: 20px;
  h2 { margin-bottom: 20px; }
}

.stat-cards {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
  .stat-value {
    font-size: 28px;
    font-weight: 700;
    margin: 8px 0;
  }
  .stat-label {
    font-size: 13px;
    color: #909399;
  }
  .stat-trend {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
  }
}

.trend-up { color: #f56c6c; }
.trend-down { color: #67c23a; }

.chart-placeholder {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 200px;
  padding: 0 10px;

  .chart-bar {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    flex: 1;
    max-width: 60px;

    .bar {
      width: 32px;
      background: linear-gradient(to top, #409EFF, #79bbff);
      border-radius: 4px 4px 0 0;
      min-height: 4px;
      transition: height 0.5s;
    }
    .bar-label { font-size: 11px; color: #909399; }
    .bar-value { font-size: 11px; color: #606266; font-weight: 500; }
  }
}
</style>
