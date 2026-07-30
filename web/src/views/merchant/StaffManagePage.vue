<template>
  <div class="staff-manage-page">
    <div class="staff-manage-page__header">
      <h2>员工管理</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card class="mb-lg">
      <template #header>
        <span>门店信息</span>
      </template>
      <el-descriptions v-if="currentStore" :column="2" border size="small">
        <el-descriptions-item label="门店名称">{{ currentStore.name }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentStore.status)" size="small">
            {{ currentStore.statusLabel }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="地址">{{ currentStore.address }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ currentStore.phone }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 添加员工 -->
    <el-card class="mb-lg">
      <template #header>
        <span>添加员工</span>
      </template>
      <el-form :model="addForm" inline @submit.prevent="handleAddStaff">
        <el-form-item label="用户ID">
          <el-input
            v-model.number="addForm.userId"
            placeholder="输入用户ID"
            type="number"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="addForm.role" placeholder="选择角色" style="width: 150px">
            <el-option label="管理员" value="STORE_ADMIN" />
            <el-option label="店员" value="STORE_STAFF" />
            <el-option label="配送员" value="DELIVERY" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="adding"
            :disabled="!addForm.userId || !addForm.role"
            @click="handleAddStaff"
          >
            添加
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 员工列表 -->
    <el-card>
      <template #header>
        <span>员工列表 ({{ staffList.length }})</span>
      </template>
      <el-table
        :data="staffList"
        border
        stripe
        empty-text="暂无员工"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="120" />
        <el-table-column label="角色" width="130">
          <template #default="{ row }">
            <el-tag size="small">{{ getRoleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="添加时间" min-width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              title="确认移除该员工？"
              confirm-button-text="移除"
              cancel-button-text="取消"
              @confirm="handleRemoveStaff(row)"
            >
              <template #reference>
                <el-button size="small" type="danger" :icon="Delete" :loading="removing === row.userId">
                  移除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { useMerchantStore } from '@/stores/merchant'

const route = useRoute()
const merchantStore = useMerchantStore()

const storeId = computed(() => Number(route.params.id) || 0)
const adding = ref(false)
const removing = ref<number | null>(null)

const addForm = reactive<AddStaffRequest>({
  userId: 0,
  role: 'STORE_STAFF',
})

const currentStore = computed<MerchantStoreInfo | undefined>(() =>
  merchantStore.stores.find((s) => s.id === storeId.value)
)

const staffList = computed<StaffInfo[]>(() =>
  currentStore.value?.staffList || []
)

function getStatusType(status: string): 'warning' | 'success' | 'danger' | 'info' {
  const map: Record<string, 'warning' | 'success' | 'danger' | 'info'> = {
    PENDING_APPROVAL: 'warning',
    ACTIVE: 'success',
    CLOSED: 'info',
    SUSPENDED: 'danger',
  }
  return map[status] || 'info'
}

const roleLabels: Record<string, string> = {
  STORE_ADMIN: '管理员',
  STORE_STAFF: '店员',
  DELIVERY: '配送员',
  STORE_OWNER: '店长',
}

function getRoleLabel(role: string): string {
  return roleLabels[role] || role
}

async function handleAddStaff() {
  if (!addForm.userId || !addForm.role) {
    ElMessage.warning('请填写用户ID和角色')
    return
  }
  adding.value = true
  try {
    await merchantStore.addStaff(storeId.value, {
      userId: addForm.userId,
      role: addForm.role,
    })
    ElMessage.success('添加成功')
    addForm.userId = 0
    addForm.role = 'STORE_STAFF'
  } catch (err: any) {
    ElMessage.error(err?.message || '添加失败')
  } finally {
    adding.value = false
  }
}

async function handleRemoveStaff(row: StaffInfo) {
  removing.value = row.userId
  try {
    await merchantStore.removeStaff(storeId.value, row.userId)
    ElMessage.success('已移除')
  } catch (err: any) {
    ElMessage.error(err?.message || '移除失败')
  } finally {
    removing.value = null
  }
}

onMounted(async () => {
  if (!merchantStore.merchant) {
    await merchantStore.fetchMyMerchant()
  }
  if (merchantStore.merchant) {
    await merchantStore.fetchStores(merchantStore.merchant.id)
  }
})
</script>

<style lang="scss" scoped>
.staff-manage-page {
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-lg;

    h2 {
      font-size: 22px;
    }
  }
}
</style>
