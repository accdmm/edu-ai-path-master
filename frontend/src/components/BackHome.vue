<template>
  <el-tooltip content="返回首页" placement="left">
    <el-button
      class="back-home-fab"
      circle
      size="large"
      type="primary"
      @click="goHome"
    >
      <el-icon><HomeFilled /></el-icon>
    </el-button>
  </el-tooltip>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
const goHome = () => router.push('/')
</script>

<style scoped>
.back-home-fab {
  position: fixed;
  right: 24px;
  bottom: 80px;
  z-index: 2000;
  width: 44px;
  height: 44px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}
</style>
