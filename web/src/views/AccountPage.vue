<template>
  <div class="account-page">
    <h2>个人中心</h2>

    <el-row :gutter="20">
      <!-- 左侧：头像和菜单 -->
      <el-col :xs="24" :sm="8" :md="6">
        <el-card class="account-page__profile">
          <div class="account-page__avatar">
            <el-avatar :size="80">
              <el-icon :size="40"><User /></el-icon>
            </el-avatar>
            <h3 class="account-page__nickname">
              {{ authStore.nickname || '未设置昵称' }}
            </h3>
            <p class="account-page__email">{{ authStore.user?.email || '' }}</p>
          </div>
          <el-menu :default-active="activeMenu" router>
            <el-menu-item index="/account">
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </el-menu-item>
            <el-menu-item index="/orders">
              <el-icon><Document /></el-icon>
              <span>我的订单</span>
            </el-menu-item>
            <el-menu-item index="/cart">
              <el-icon><ShoppingCart /></el-icon>
              <span>购物车</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <!-- 右侧：详细信息 -->
      <el-col :xs="24" :sm="16" :md="18">
        <!-- 基本信息 -->
        <el-card class="account-page__section">
          <template #header>
            <h3>基本信息</h3>
          </template>
          <el-form label-width="80px" size="default">
            <el-form-item label="邮箱">
              <el-input
                :model-value="authStore.user?.email"
                disabled
              />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input
                v-model="nicknameForm.nickname"
                placeholder="请输入昵称"
              />
              <el-button
                type="primary"
                size="small"
                :loading="nicknameSaving"
                style="margin-left: 8px"
                @click="handleSaveNickname"
              >
                保存
              </el-button>
            </el-form-item>
            <el-form-item label="角色">
              <el-tag
                v-for="role in authStore.user?.roles"
                :key="role"
                size="small"
                :type="getRoleTagType(role)"
                class="account-page__role-tag"
              >
                {{ getRoleLabel(role) }}
              </el-tag>
            </el-form-item>
            <el-form-item label="状态">
              <el-tag
                :type="authStore.user?.status === 'ACTIVE' ? 'success' : 'danger'"
                size="small"
              >
                {{ authStore.user?.status === 'ACTIVE' ? '正常' : '冻结' }}
              </el-tag>
            </el-form-item>
            <el-form-item label="注册时间">
              <span>{{ formatDate(authStore.user?.createdAt) }}</span>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 修改密码 -->
        <el-card class="account-page__section">
          <template #header>
            <h3>修改密码</h3>
          </template>
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            size="default"
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                show-password
                placeholder="请输入当前密码"
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                placeholder="请输入新密码"
              />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmNewPassword">
              <el-input
                v-model="passwordForm.confirmNewPassword"
                type="password"
                show-password
                placeholder="请再次输入新密码"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="passwordSaving"
                @click="handleChangePassword"
              >
                修改密码
              </el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 退出登录 -->
        <el-card class="account-page__section">
          <el-button type="danger" @click="handleLogout">
            退出登录
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { User, Document, ShoppingCart } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const authStore = useAuthStore()
const activeMenu = ref('/account')

// ==================== 昵称修改 ====================
const nicknameSaving = ref(false)
const nicknameForm = reactive({
  nickname: authStore.nickname,
})

async function handleSaveNickname() {
  if (!nicknameForm.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  nicknameSaving.value = true
  try {
    const res: any = await request.put('/auth/me', {
      nickname: nicknameForm.nickname.trim(),
    })
    // 更新本地 store
    if (res.data) {
      authStore.user = res.data
      ElMessage.success('昵称修改成功')
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '修改失败')
  } finally {
    nicknameSaving.value = false
  }
}

// ==================== 修改密码 ====================
const passwordFormRef = ref<FormInstance>()
const passwordSaving = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmNewPassword: '',
})

const validateConfirmPassword = (
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void
) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmNewPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
    passwordSaving.value = true
    await request.post('/auth/change-password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    resetPasswordForm()
    authStore.logout()
    router.push('/auth/login')
  } catch (error: any) {
    if (error === false) return
    ElMessage.error(error?.response?.data?.message || '密码修改失败')
  } finally {
    passwordSaving.value = false
  }
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmNewPassword = ''
  passwordFormRef.value?.resetFields()
}

// ==================== 退出登录 ====================
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } catch {
    // 用户取消
  }
}

// ==================== 工具函数 ====================
function getRoleLabel(role: string): string {
  const map: Record<string, string> = {
    ROLE_USER: '普通用户',
    ROLE_MERCHANT: '商家',
    ROLE_ADMIN: '管理员',
  }
  return map[role] || role
}

function getRoleTagType(role: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    ROLE_USER: '',
    ROLE_MERCHANT: 'warning',
    ROLE_ADMIN: 'danger',
  }
  return map[role] || 'info'
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style lang="scss" scoped>
.account-page {
  h2 {
    font-size: 22px;
    margin-bottom: $spacing-lg;
  }

  &__profile {
    text-align: center;
    margin-bottom: $spacing-lg;
  }

  &__avatar {
    padding: $spacing-lg 0;
  }

  &__nickname {
    margin-top: $spacing-sm;
    font-size: 18px;
  }

  &__email {
    color: $text-secondary;
    font-size: 13px;
    margin-top: $spacing-xs;
  }

  &__section {
    margin-bottom: $spacing-md;
  }

  &__role-tag {
    margin-right: $spacing-xs;
  }

  .el-menu {
    border-right: none;
  }
}
</style>
