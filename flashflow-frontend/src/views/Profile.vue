<template>
  <div>
    <div class="ff-container" style="margin:24px auto">
      <el-row :gutter="20">
        <!-- 左侧用户卡片 -->
        <el-col :xs="24" :sm="8">
          <div style="background:var(--ff-bg-card);border-radius:var(--ff-radius-lg);padding:28px 20px;text-align:center;box-shadow:var(--ff-shadow-sm);margin-bottom:16px">
            <el-avatar :size="72" style="background:var(--ff-primary);font-size:28px;font-weight:700;margin-bottom:14px">
              {{ (userInfo.nickname || 'U')[0].toUpperCase() }}
            </el-avatar>
            <div style="font-weight:700;font-size:17px">{{ userInfo.nickname || '用户' }}</div>
            <div style="color:var(--ff-text-muted);font-size:13px;margin-top:4px">{{ userInfo.phone || '' }}</div>
          </div>

          <div style="background:var(--ff-bg-card);border-radius:var(--ff-radius-lg);padding:12px;box-shadow:var(--ff-shadow-sm)">
            <div class="menu-item" @click="$router.push('/order')">
              <el-icon><List /></el-icon> <span>我的订单</span> <span class="arrow">→</span>
            </div>
            <div class="menu-item" @click="$router.push('/address')">
              <el-icon><Location /></el-icon> <span>收货地址</span> <span class="arrow">→</span>
            </div>
            <div class="menu-item" @click="$router.push('/cart')">
              <el-icon><ShoppingCart /></el-icon> <span>购物车</span> <span class="arrow">→</span>
            </div>
            <div class="menu-item" @click="$router.push('/coupon')">
              <el-icon><Ticket /></el-icon> <span>优惠券</span> <span class="arrow">→</span>
            </div>
            <el-divider style="margin:6px 0" />
            <div class="menu-item" style="color:var(--ff-danger)" @click="handleLogout">
              <el-icon><SwitchButton /></el-icon> <span>退出登录</span>
            </div>
          </div>
        </el-col>

        <!-- 右侧订单 -->
        <el-col :xs="24" :sm="16">
          <div style="background:var(--ff-bg-card);border-radius:var(--ff-radius-lg);padding:0;box-shadow:var(--ff-shadow-sm);overflow:hidden">
            <div style="padding:16px 20px;border-bottom:1px solid var(--ff-border-light)">
              <span style="font-weight:700;font-size:16px">最近订单</span>
            </div>
            <div v-if="orders.length === 0" style="text-align:center;padding:48px 0;color:var(--ff-text-muted)">暂无订单</div>
            <div v-else>
              <div v-for="order in orders" :key="order.id" class="order-row" @click="$router.push('/order')">
                <div>
                  <div style="font-weight:600;font-size:14px">{{ order.orderSn }}</div>
                  <div style="font-size:18px;font-weight:700;color:var(--ff-danger);margin-top:2px">¥{{ order.payAmount }}</div>
                </div>
                <div style="text-align:right">
                  <span class="ff-tag" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
                  <div style="margin-top:6px">
                    <el-button v-if="order.status === 0" size="small" type="primary" @click.stop="$router.push(`/payment/${order.orderSn}`)">去支付</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div style="margin-top:16px;display:flex;gap:10px;flex-wrap:wrap">
            <el-button size="large" @click="$router.push('/seckill')" style="border-radius:10px">限时秒杀</el-button>
            <el-button size="large" @click="$router.push('/shop')" style="border-radius:10px">全部商品</el-button>
            <el-button size="large" @click="$router.push('/address')" style="border-radius:10px">管理地址</el-button>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
.menu-item {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px; border-radius: 8px; cursor: pointer;
  font-size: 14px; color: var(--ff-text); transition: all 0.15s;
}
.menu-item:hover { background: var(--ff-bg); }
.menu-item .arrow { margin-left: auto; font-size: 12px; color: var(--ff-text-muted); }
.order-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 20px; border-bottom: 1px solid var(--ff-border-light); cursor: pointer; transition: background 0.15s;
}
.order-row:hover { background: var(--ff-bg-hover); }
</style>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getOrderPage } from '@/api/order'
import { List, Location, ShoppingCart, SwitchButton, Ticket } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const userInfo = ref<any>({})
const orders = ref<any[]>([])

function statusClass(s: number) { return ['ff-tag-red','ff-tag-orange','ff-tag-green','',''][s] || '' }
function statusLabel(s: number) { return ['待支付','已支付','已发货','已收货','已完成','已取消','退款中','已退款'][s] || '未知' }

function handleLogout() {
  localStorage.removeItem('flashflow_userId')
  userStore.logout()
  router.push('/')
}

onMounted(async () => {
  userInfo.value = { nickname: userStore.username || 'User' }
  try {
    const uid = String(userStore.userId || 0)
    if (uid) {
      const res = await getOrderPage({ page: 1, size: 5 })
      orders.value = res.data.records || []
    }
  } catch {}
})
</script>
