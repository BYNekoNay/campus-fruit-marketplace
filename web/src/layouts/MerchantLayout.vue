<template>
  <el-container class="merchant-layout">
    <el-aside :width="sidebarWidth" class="merchant-layout__aside">
      <div class="merchant-layout__logo">
        <span v-if="!isCollapsed">商家中心</span>
        <span v-else>商</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/merchant/dashboard">
          <el-icon><Monitor /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>
        <el-menu-item index="/merchant/stores">
          <el-icon><Shop /></el-icon>
          <template #title>门店管理</template>
        </el-menu-item>
        <el-menu-item index="/merchant/products">
          <el-icon><Goods /></el-icon>
          <template #title>商品管理</template>
        </el-menu-item>
        <el-menu-item index="/merchant/offers">
          <el-icon><PriceTag /></el-icon>
          <template #title>商品报价</template>
        </el-menu-item>
        <el-menu-item index="/merchant/orders">
          <el-icon><Document /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>
        <el-menu-item index="/merchant/settings">
          <el-icon><Setting /></el-icon>
          <template #title>门店设置</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="merchant-layout__header" height="60px">
        <div class="merchant-layout__header-left">
          <el-button
            :icon="isCollapsed ? Expand : Fold"
            text
            @click="toggleSidebar"
          />
          <span class="merchant-layout__title">商家管理中心</span>
        </div>
        <div class="merchant-layout__header-right">
          <router-link to="/">返回首页</router-link>
        </div>
      </el-header>
      <el-main class="merchant-layout__main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import {
  Monitor,
  Shop,
  Goods,
  PriceTag,
  Document,
  Setting,
  Expand,
  Fold,
} from '@element-plus/icons-vue'

const route = useRoute()
const appStore = useAppStore()

const isCollapsed = computed(() => appStore.sidebarCollapsed)
const sidebarWidth = computed(() => (isCollapsed.value ? '64px' : '220px'))
const activeMenu = computed(() => route.path)

function toggleSidebar() {
  appStore.toggleSidebar()
}
</script>

<style lang="scss" scoped>
.merchant-layout {
  height: 100vh;

  &__aside {
    background-color: #304156;
    overflow: hidden;

    .el-menu {
      border-right: none;
    }
  }

  &__logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #fff;
    border-bottom: 1px solid $border-color;
    padding: 0 $spacing-lg;
  }

  &__header-left {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }

  &__title {
    font-size: 16px;
    font-weight: 500;
  }

  &__header-right {
    a {
      color: $primary-color;
      text-decoration: none;
      font-size: 14px;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  &__main {
    background-color: $bg-color;
    padding: $spacing-lg;
  }
}
</style>
