<template>
  <div class="register-page">
    <div class="register-page__card">
      <div class="register-page__header">
        <h1 class="register-page__title">注册</h1>
        <p class="register-page__subtitle">创建你的校园水果商城账号</p>
      </div>

      <!-- 服务端错误提示 -->
      <el-alert
        v-if="serverError"
        :title="serverError"
        type="error"
        show-icon
        :closable="true"
        class="register-page__error"
        @close="serverError = ''"
      />

      <el-form
        ref="formRef"
        :model="registerForm"
        :rules="rules"
        size="large"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱地址"
            :prefix-icon="Message"
            clearable
          />
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
            v-model="registerForm.nickname"
            placeholder="请输入昵称"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码（至少8位）"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="register-page__btn"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-page__footer">
        <span>已有账号？</span>
        <router-link to="/auth/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Message, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const serverError = ref('')

const registerForm = reactive({
  email: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = (
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void
) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度在2到20个字符之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function handleRegister() {
  if (!formRef.value) return

  serverError.value = ''

  try {
    await formRef.value.validate()
    loading.value = true

    // 注册并自动登录
    await authStore.registerAndLogin(
      registerForm.email,
      registerForm.password,
      registerForm.nickname
    )
    ElMessage.success('注册成功，欢迎加入！')
    router.push('/')
  } catch (error: any) {
    // 表单校验失败时不显示错误
    if (error === false) return

    const msg =
      error?.response?.data?.message ||
      error?.message ||
      '注册失败，请稍后重试'
    serverError.value = msg
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $primary-light 0%, $primary-color 100%);

  &__card {
    width: 420px;
    padding: $spacing-xxl;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12);
  }

  &__header {
    text-align: center;
    margin-bottom: $spacing-xl;
  }

  &__title {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
  }

  &__subtitle {
    margin-top: $spacing-xs;
    color: $text-secondary;
    font-size: 14px;
  }

  &__error {
    margin-bottom: $spacing-md;
  }

  &__btn {
    width: 100%;
  }

  &__footer {
    text-align: center;
    font-size: 14px;
    color: $text-secondary;

    a {
      color: $primary-color;
      text-decoration: none;
      margin-left: $spacing-xs;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}
</style>
