<template>
  <div>
    <h3 style="margin:0 0 20px;font-size:20px">控制台</h3>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" v-for="card in statCards" :key="card.label">
        <div class="stat-icon" :style="{ background: card.bg }"></div>
        <div class="stat-body">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value">
            <el-skeleton :loading="loading" :rows="1" animated>{{ card.value }}</el-skeleton>
          </div>
        </div>
      </div>
    </div>

    <!-- 秒杀活动 -->
    <div style="background:var(--ff-bg-card);border-radius:var(--ff-radius-lg);padding:20px;box-shadow:var(--ff-shadow-sm);margin-bottom:20px">
      <div style="font-weight:700;font-size:16px;margin-bottom:16px">进行中的秒杀活动</div>
      <div v-if="activeActivities.length === 0" style="text-align:center;padding:32px 0;color:var(--ff-text-muted)">
        当前没有进行中的秒杀活动
      </div>
      <div v-else style="display:flex;gap:14px;flex-wrap:wrap">
        <div v-for="act in activeActivities" :key="act.id" class="flash-card" @click="$router.push('/seckill')">
          <div style="display:flex;align-items:center;gap:10px">
            <div style="font-size:28px;color:#f59e0b">⚡</div>
            <div>
              <div style="font-weight:600;font-size:15px">{{ act.name }}</div>
              <div style="font-size:12px;color:var(--ff-text-muted)">{{ fmt(act.startTime) }} ~ {{ fmt(act.endTime) }}</div>
            </div>
          </div>
          <span class="ff-tag ff-tag-red">进行中</span>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div style="display:flex;gap:10px;flex-wrap:wrap">
      <el-button type="primary" size="large" @click="$router.push('/admin/activity')" style="border-radius:10px">创建秒杀活动</el-button>
      <el-button type="success" size="large" @click="$router.push('/admin/user')" style="border-radius:10px">管理用户</el-button>
      <el-button type="warning" size="large" @click="$router.push('/admin/order')" style="border-radius:10px">查看订单</el-button>
      <el-button size="large" @click="$router.push('/admin/product')" style="border-radius:10px">商品管理</el-button>
    </div>
  </div>
</template>

<style scoped>
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card {
  background: var(--ff-bg-card); border-radius: var(--ff-radius-lg);
  padding: 22px 20px; display: flex; align-items: center; gap: 16px;
  box-shadow: var(--ff-shadow-sm); transition: box-shadow 0.2s;
}
.stat-card:hover { box-shadow: var(--ff-shadow-md); }
.stat-icon {
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; flex-shrink: 0;
}
.stat-label { font-size: 13px; color: var(--ff-text-muted); margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 800; color: var(--ff-text); }
.flash-card {
  background: var(--ff-bg); border-radius: var(--ff-radius); padding: 16px;
  min-width: 240px; cursor: pointer; display: flex; justify-content: space-between;
  align-items: center; transition: all 0.2s;
}
.flash-card:hover { background: #eff6ff; }

@media (max-width: 768px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px) { .stats-grid { grid-template-columns: 1fr; } }
</style>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getUserCount } from '@/api/auth'
import { getOrderStats } from '@/api/order'
import { getActivityCount, getFlashNow, type PromotionActivity } from '@/api/activity'

const loading = ref(true)
const activeActivities = ref<PromotionActivity[]>([])

const statCards = reactive([
  { label: '用户总数', value: '--', bg: '#eff6ff' },
  { label: '订单总数', value: '--', bg: '#ecfdf5' },
  { label: '进行中活动', value: '--', bg: '#fffbeb' },
  { label: '支付成功率', value: '--', bg: '#fef2f2' },
])

function fmt(t: string) { return t?.replace('T',' ').substring(5,16) || '' }

onMounted(async () => {
  try {
    const [userR, orderR, actR, flashR] = await Promise.allSettled([
      getUserCount(), getOrderStats(), getActivityCount(), getFlashNow()
    ])
    if (userR.status === 'fulfilled' && userR.value) statCards[0].value = String(userR.value.data || '-')
    if (orderR.status === 'fulfilled' && orderR.value?.data) {
      statCards[1].value = String(orderR.value.data.totalOrders || '-')
      statCards[3].value = orderR.value.data.payRate || '-'
    }
    if (actR.status === 'fulfilled' && actR.value) statCards[2].value = String(actR.value.data || '-')
    if (flashR.status === 'fulfilled' && flashR.value?.data) {
      const list = flashR.value.data
      if (Array.isArray(list)) activeActivities.value = list.filter((a: PromotionActivity) => a.status === 2)
    }
  } finally { loading.value = false }
})
</script>
