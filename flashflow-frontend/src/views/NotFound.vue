<template>
  <div class="not-found-page">
    <div class="not-found-card">
      <div class="not-found-code">404</div>
      <div class="not-found-title">页面找不到了</div>
      <p class="not-found-desc">您访问的页面不存在或已被移除</p>
      <div class="not-found-actions">
        <el-button type="warning" size="large" round @click="router.push('/')">返回首页</el-button>
        <el-button size="large" round @click="router.go(-1)">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.not-found-page {
  min-height: 60vh; display: flex; align-items: center; justify-content: center;
  background: var(--ff-bg); padding: 40px 20px;
}
.not-found-card { text-align: center; max-width: 480px; }
.not-found-code {
  font-size: 120px; font-weight: 900; color: #1e293b;
  line-height: 1; margin-bottom: 8px;
  -webkit-text-stroke: 2px var(--ff-accent);
}
.not-found-title { font-size: 20px; font-weight: 600; color: var(--ff-text); margin-bottom: 8px; }
.not-found-desc { color: var(--ff-text-muted); margin: 0 0 32px; font-size: 14px; }
.not-found-actions { display: flex; gap: 12px; justify-content: center; }
</style>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>
