<template>
  <div class="fruit-list-page">
    <div class="fruit-list-page__header">
      <h2>商品目录</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建水果</el-button>
    </div>

    <el-card>
      <!-- 搜索区域 -->
      <div class="fruit-list-page__search">
        <el-input
          v-model="keyword"
          placeholder="输入品种名称搜索"
          clearable
          :prefix-icon="Search"
          style="width: 300px"
          @change="handleSearch"
          @clear="handleSearch"
        />
      </div>

      <el-table
        v-loading="loading"
        :data="pagedList"
        border
        stripe
        empty-text="暂无水果目录"
      >
        <el-table-column prop="category" label="品类" width="100" />
        <el-table-column prop="variety" label="品种" min-width="140" />
        <el-table-column prop="grade" label="等级" width="80" />
        <el-table-column prop="origin" label="产地" min-width="120" show-overflow-tooltip />
        <el-table-column prop="defaultUnit" label="默认单位" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              size="small"
              type="warning"
              @click="handleDeactivate(row)"
            >
              停用
            </el-button>
            <el-button
              v-else
              size="small"
              type="success"
              @click="handleActivate(row)"
            >
              启用
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="fruit-list-page__pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>

    <!-- 弹窗 -->
    <FruitFormDialog
      v-model="dialogVisible"
      :edit-data="editData"
      @submit="handleFormSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Plus, Edit, Search } from '@element-plus/icons-vue'
import { useOfferStore } from '@/stores/offer'
import { formatDateTime } from '@/utils/format'
import FruitFormDialog from './FruitFormDialog.vue'

const offerStore = useOfferStore()

const keyword = ref('')
const loading = computed(() => offerStore.loading)
const list = computed(() => offerStore.fruits)

const dialogVisible = ref(false)
const editData = ref<CanonicalFruit | null>(null)

const pagination = ref({ page: 1, pageSize: 10 })

const total = computed(() => list.value.length)

const pagedList = computed(() => {
  const start = (pagination.value.page - 1) * pagination.value.pageSize
  return list.value.slice(start, start + pagination.value.pageSize)
})

async function fetchData() {
  await offerStore.fetchFruits(keyword.value || undefined)
}

function handleSearch() {
  pagination.value.page = 1
  fetchData()
}

function openCreate() {
  editData.value = null
  dialogVisible.value = true
}

function openEdit(row: CanonicalFruit) {
  editData.value = row
  dialogVisible.value = true
}

async function handleFormSubmit(dto: CreateFruitRequest) {
  if (editData.value) {
    await offerStore.updateFruit(editData.value.id, dto)
  } else {
    await offerStore.createFruit(dto)
  }
}

async function handleDeactivate(row: CanonicalFruit) {
  try {
    await ElMessageBox.confirm(
      `确认停用「${row.variety}」？停用后商家将无法为该水果创建报价。`,
      '停用确认',
      { confirmButtonText: '确认停用', cancelButtonText: '取消', type: 'warning' }
    )
    await offerStore.toggleFruitStatus(row.id, false)
    ElMessage.success('已停用')
  } catch {
    // 用户取消
  }
}

async function handleActivate(row: CanonicalFruit) {
  await offerStore.toggleFruitStatus(row.id, true)
  ElMessage.success('已启用')
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.fruit-list-page {
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-lg;

    h2 {
      font-size: 22px;
    }
  }

  &__search {
    margin-bottom: $spacing-md;
  }

  &__pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: $spacing-lg;
  }
}
</style>
