<template>
  <el-container class="admin-layout">
    <el-aside :width="sidebarWidth" class="admin-layout__aside">
      <div class="admin-layout__logo">
        <span v-if="!isCollapsed">平台管理</span>
        <span v-else>管</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/admin">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据概览</template>
        </el-menu-item>
        <el-menu-item index="/admin/merchants">
          <el-icon><Checked /></el-icon>
          <template #title>商家审核</template>
        </el-menu-item>
        <el-menu-item index="/admin/fruits">
          <el-icon><Goods /></el-icon>
          <template #title>商品目录</template>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/stores">
          <el-icon><Shop /></el-icon>
          <template #title>门店管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><Document /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/reports">
          <el-icon><Warning /></el-icon>
          <template #title>举报审核</template>
        </el-menu-item>
        <el-menu-item index="/admin/settings">
          <el-icon><Setting /></el-icon>
          <template #title>系统设置</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-layout__header" height="60px">
        <div class="admin-layout__header-left">
          <el-button
            :icon="isCollapsed ? Expand : Fold"
            text
            @click="toggleSidebar"
          />
          <span class="admin-layout__title">平台管理后台</span>
        </div>
        <div class="admin-layout__header-right">
          <router-link to="/">返回首页</router-link>
        </div>
      </el-header>
      <el-main class="admin-layout__main">
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
  DataAnalysis,
  Checked,
  User,
  Shop,
  Goods,
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
.admin-layout {
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
