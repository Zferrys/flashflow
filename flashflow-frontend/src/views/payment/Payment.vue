<template>
  <div>
    <!-- 消费者首页导航 -->
    <header style="background:#fff;border-bottom:1px solid #eee;padding:0 16px">
      <div style="max-width:1200px;margin:0 auto;display:flex;align-items:center;height:56px;justify-content:space-between">
        <span style="font-size:18px;font-weight:bold;color:#409eff;cursor:pointer" @click="router.push('/')">⚡ FlashFlow</span>
        <el-button text @click="router.push('/order')">我的订单</el-button>
      </div>
    </header>

    <div style="max-width:600px;margin:40px auto;padding:0 16px">
      <el-card v-loading="loading">
        <template #header><span>💳 支付中心</span></template>

        <!-- 支付中 -->
        <div v-if="paying" style="text-align:center;padding:60px">
          <el-icon class="is-loading" :size="48" color="#409eff"><Loading /></el-icon>
          <p style="margin-top:16px;color:#999">正在处理支付...</p>
        </div>

        <!-- 支付成功 -->
        <div v-else-if="success" style="text-align:center;padding:60px">
          <div style="font-size:64px;margin-bottom:16px">✅</div>
          <h2 style="margin:0 0 8px">支付成功！</h2>
          <p style="color:#999;margin-bottom:24px">订单 {{ orderSn }} 已支付完成</p>
          <div style="display:flex;gap:12px;justify-content:center">
            <el-button @click="router.push('/order')">查看订单</el-button>
            <el-button type="primary" @click="router.push('/')">返回首页</el-button>
          </div>
        </div>

        <!-- 订单确认 -->
        <div v-else-if="order" style="padding:0">
          <div style="text-align:center;margin-bottom:24px">
            <div style="font-size:48px;margin-bottom:8px">🛒</div>
            <h3 style="margin:0">订单确认</h3>
          </div>

          <!-- 商品清单 -->
          <div v-if="items.length" style="margin-bottom:20px">
            <div style="font-weight:bold;margin-bottom:8px">商品清单</div>
            <div v-for="item in items" :key="item.skuId" style="display:flex;gap:12px;padding:12px 0;border-bottom:1px solid #f0f0f0;align-items:center">
              <el-image :src="item.skuImage || ''" style="width:60px;height:60px;border-radius:4px;flex-shrink:0" fit="cover">
                <template #error><div style="width:60px;height:60px;background:#f5f7fa;border-radius:4px"></div></template>
              </el-image>
              <div style="flex:1">
                <div style="font-weight:bold">{{ item.skuName }}</div>
                <div style="color:#999;font-size:12px">x{{ item.quantity }}</div>
              </div>
              <div style="color:#f56c6c;font-weight:bold">¥{{ item.subTotal || (item.skuPrice * item.quantity) }}</div>
            </div>
          </div>

          <!-- 金额信息 -->
          <el-descriptions :column="1" border style="margin-bottom:20px">
            <el-descriptions-item label="订单编号">{{ order.orderSn }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">
              <span style="color:#f56c6c;font-size:20px;font-weight:bold">¥{{ order.totalAmount }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="支付方式">模拟支付（开发环境）</el-descriptions-item>
          </el-descriptions>

          <el-button type="danger" size="large" style="width:100%;height:48px;font-size:16px" @click="handlePay" :loading="paying">
            确认支付 ¥{{ order.totalAmount }}
          </el-button>
          <div style="margin-top:12px;text-align:center">
            <el-button text @click="router.push('/order')">稍后支付</el-button>
          </div>
        </div>

        <el-empty v-else-if="!loading" description="订单不存在" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderBySn, getOrderItems, mockPay } from '@/api/order'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const orderSn = ref(route.params.orderSn as string)
const loading = ref(true)
const paying = ref(false)
const success = ref(false)
const order = ref<any>(null)
const items = ref<any[]>([])

async function fetchOrder() {
  loading.value = true
  try {
    const res = await getOrderBySn(orderSn.value)
    order.value = res.data
    if (order.value?.id) {
      const itemRes = await getOrderItems(order.value.id)
      items.value = itemRes.data || []
    }
  } catch { order.value = null }
  finally { loading.value = false }
}

async function handlePay() {
  paying.value = true
  try {
    await mockPay(orderSn.value, order.value?.payAmount || order.value?.totalAmount)
    success.value = true
    ElMessage.success('支付成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '支付失败，请重试')
  } finally {
    paying.value = false
  }
}

onMounted(fetchOrder)
</script>
