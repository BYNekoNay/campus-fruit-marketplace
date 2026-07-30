<template>
  <el-container class="default-layout">
    <AppHeader />
    <el-main class="default-layout__main">
      <router-view />
    </el-main>
    <AppFooter />
  </el-container>
</template>

<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
</script>

<style lang="scss" scoped>
.default-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: $bg-color;

  &__main {
    flex: 1;
    max-width: 1200px;
    width: 100%;
    margin: 0 auto;
    padding: $spacing-lg;
  }
}
</style>
