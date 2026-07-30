<template>
  <el-header class="app-header" height="60px">
    <div class="app-header__container">
      <!-- Logo -->
      <div class="app-header__logo" @click="$router.push('/')">
        <el-icon :size="24" color="#67C23A"><Apple /></el-icon>
        <span class="app-header__logo-text">校园水果商城</span>
      </div>

      <!-- 导航菜单 + 搜索 -->
      <div class="app-header__center">
        <!-- 全局搜索框 -->
        <div class="app-header__search-box">
          <el-input
            v-model="headerSearchKeyword"
            placeholder="搜索水果..."
            clearable
            size="small"
            class="app-header__search-input"
            @keyup.enter="handleHeaderSearch"
            @clear="handleHeaderSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <el-menu
          mode="horizontal"
          :default-active="activeMenu"
          class="app-header__nav"
          background-color="transparent"
          router
        >
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/discovery">
            <el-icon><Search /></el-icon>
            <span>发现</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 右侧操作 -->
      <div class="app-header__actions">
        <!-- 购物车：始终显示 -->
        <el-badge :value="0" :hidden="true" class="app-header__cart">
          <el-button
            :icon="ShoppingCart"
            circle
            text
            @click="handleCartClick"
          />
        </el-badge>

        <!-- 已登录状态 -->
        <template v-if="authStore.isAuthenticated">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="app-header__user">
              <el-avatar :size="36">
                <el-icon :size="20"><User /></el-icon>
              </el-avatar>
              <span class="app-header__username">{{ authStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="account">
                  <el-icon><User /></el-icon>
                  <span>个人中心</span>
                </el-dropdown-item>
                <el-dropdown-item command="orders">
                  <el-icon><Document /></el-icon>
                  <span>我的订单</span>
                </el-dropdown-item>
                <el-dropdown-item command="favorites">
                  <el-icon><Star /></el-icon>
                  <span>我的收藏</span>
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="authStore.isMerchant || authStore.isAdmin"
                  command="merchant"
                >
                  <el-icon><Shop /></el-icon>
                  <span>商家管理</span>
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="authStore.isAdmin"
                  command="admin"
                >
                  <el-icon><Setting /></el-icon>
                  <span>平台管理</span>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

        <!-- 未登录状态 -->
        <template v-else>
          <el-button text @click="$router.push('/auth/login')">登录</el-button>
          <el-button type="primary" size="small" @click="$router.push('/auth/register')">
            注册
          </el-button>
        </template>
      </div>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import {
  Apple,
  HomeFilled,
  Search,
  ShoppingCart,
  User,
  ArrowDown,
  Document,
  Shop,
  Setting,
  SwitchButton,
  Star,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const headerSearchKeyword = ref('')

function handleHeaderSearch() {
  const keyword = headerSearchKeyword.value.trim()
  if (keyword) {
    router.push({ name: 'Discovery', query: { keyword } })
  }
}

const activeMenu = computed(() => {
  if (route.path.startsWith('/discovery')) return '/discovery'
  return '/'
})

function handleCartClick() {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login', query: { redirect: '/cart' } })
    return
  }
  router.push('/cart')
}

function handleCommand(command: string) {
  switch (command) {
    case 'account':
      router.push('/account')
      break
    case 'orders':
      router.push('/orders')
      break
    case 'favorites':
      router.push('/favorites')
      break
    case 'merchant':
      router.push('/merchant')
      break
    case 'admin':
      router.push('/admin')
      break
    case 'logout':
      authStore.logout()
      ElMessage.success('已退出登录')
      router.push('/')
      break
  }
}
</script>

<style lang="scss" scoped>
.app-header {
  background: #fff;
  border-bottom: 1px solid $border-color;
  padding: 0;

  &__container {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    height: 100%;
    padding: 0 $spacing-lg;
  }

  &__logo {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    cursor: pointer;
    user-select: none;
    flex-shrink: 0;
  }

  &__logo-text {
    font-size: 18px;
    font-weight: 700;
    color: $primary-color;
  }

  &__center {
    flex: 1;
    display: flex;
    align-items: center;
    gap: $spacing-md;
    margin: 0 $spacing-md;
  }

  &__search-box {
    flex: 1;
    max-width: 320px;
  }

  &__search-input {
    :deep(.el-input__wrapper) {
      border-radius: 20px;
      background: $bg-color;
    }
  }

  &__nav {
    border-bottom: none;
    flex-shrink: 0;

    .el-menu-item {
      border-bottom: none;
    }
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    flex-shrink: 0;
  }

  &__cart {
    margin-right: $spacing-sm;
  }

  &__user {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    cursor: pointer;
    padding: $spacing-xs $spacing-sm;
    border-radius: $border-radius;

    &:hover {
      background-color: $bg-color;
    }
  }

  &__username {
    font-size: 14px;
    color: $text-primary;
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
