<template>
  <el-footer class="app-footer" height="80px">
    <div class="app-footer__container">
      <div class="app-footer__links">
        <a href="#">关于我们</a>
        <a href="#">帮助中心</a>
        <a href="#">用户协议</a>
        <a href="#">隐私政策</a>
        <a href="#">联系我们</a>
      </div>
      <p class="app-footer__copyright">
        &copy; {{ currentYear }} 校园水果商城. All rights reserved.
      </p>
    </div>
  </el-footer>
</template>

<script setup lang="ts">
const currentYear = new Date().getFullYear()
</script>

<style lang="scss" scoped>
.app-footer {
  background-color: $text-primary;
  color: rgba(255, 255, 255, 0.7);
  padding: 0;

  &__container {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    padding: 0 $spacing-lg;
  }

  &__links {
    display: flex;
    gap: $spacing-lg;
    margin-bottom: $spacing-xs;

    a {
      color: rgba(255, 255, 255, 0.7);
      text-decoration: none;
      font-size: 13px;

      &:hover {
        color: #fff;
      }
    }
  }

  &__copyright {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.5);
  }
}
</style>
