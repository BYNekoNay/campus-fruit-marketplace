<template>
  <div class="login-page">
    <div class="login-page__card">
      <div class="login-page__header">
        <h1 class="login-page__title">登录</h1>
        <p class="login-page__subtitle">欢迎回到校园水果商城</p>
      </div>

      <!-- 服务端错误提示 -->
      <el-alert
        v-if="serverError"
        :title="serverError"
        type="error"
        show-icon
        :closable="true"
        class="login-page__error"
        @close="serverError = ''"
      />

      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="email">
          <el-input
            v-model="loginForm.email"
            placeholder="请输入邮箱地址"
            :prefix-icon="Message"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-page__btn"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-page__footer">
        <span>还没有账号？</span>
        <router-link to="/auth/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Message, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const serverError = ref('')

const loginForm = reactive({
  email: '',
  password: '',
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
}

async function handleLogin() {
  if (!formRef.value) return

  serverError.value = ''

  try {
    await formRef.value.validate()
    loading.value = true

    await authStore.login(loginForm.email, loginForm.password)
    ElMessage.success('登录成功')

    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (error: any) {
    // 表单校验失败时不显示错误
    if (error === false) return

    // 提取服务端错误信息
    const msg =
      error?.response?.data?.message ||
      error?.message ||
      '登录失败，请稍后重试'
    serverError.value = msg
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
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
