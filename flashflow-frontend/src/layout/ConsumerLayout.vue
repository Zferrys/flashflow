<template>
  <div class="consumer-app">
    <!-- 统一导航 -->
    <header class="consumer-header">
      <div class="header-inner">
        <div class="header-left">
          <span class="brand-logo" @click="router.push('/')">⚡ FlashFlow</span>
          <nav class="header-nav">
            <a :class="['nav-link', { active: route.path === '/shop' }]" @click="router.push('/shop')">全部商品</a>
            <a :class="['nav-link', { active: route.path.startsWith('/seckill') }]" @click="router.push('/seckill')">
              限时秒杀
              <span class="nav-badge">HOT</span>
            </a>
            <a :class="['nav-link', { active: route.path === '/coupon' }]" @click="router.push('/coupon')">优惠券</a>
          </nav>
        </div>
        <div class="header-right">
          <el-badge :value="cartCount" :hidden="cartCount === 0" class="cart-badge">
            <a class="icon-btn" @click="router.push('/cart')">
              <el-icon :size="20"><ShoppingCart /></el-icon>
              <span class="hide-mobile">购物车</span>
            </a>
          </el-badge>
          <template v-if="userStore.token">
            <a class="icon-btn hide-mobile" @click="router.push('/order')">我的订单</a>
            <el-dropdown trigger="click">
              <span class="user-pill">
                <el-icon><UserFilled /></el-icon>
                <span class="hide-phone">{{ userStore.username || '用户' }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-item @click="router.push('/profile')">👤 个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/order')">📦 我的订单</el-dropdown-item>
                <el-dropdown-item @click="router.push('/address')">📍 收货地址</el-dropdown-item>
                <el-dropdown-item @click="router.push('/coupon')">🎫 我的优惠券</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">🚪 退出登录</el-dropdown-item>
              </template>
            </el-dropdown>
          </template>
          <el-button v-else type="warning" size="small" round @click="router.push('/login')">登录</el-button>
        </div>
      </div>
    </header>

    <!-- 页面内容 -->
    <main class="consumer-main">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 统一底部 -->
    <footer class="consumer-footer">
      <div class="footer-grid">
        <div class="footer-brand">
          <div class="footer-brand-name">⚡ FlashFlow</div>
          <div class="footer-brand-desc">高并发闪购平台 · 企业级微服务架构</div>
        </div>
        <div class="footer-links">
          <div class="footer-col">
            <div class="footer-col-title">购物</div>
            <a class="footer-link" @click="router.push('/shop')">全部商品</a>
            <a class="footer-link" @click="router.push('/seckill')">限时秒杀</a>
            <a class="footer-link" @click="router.push('/coupon')">领优惠券</a>
          </div>
          <div class="footer-col">
            <div class="footer-col-title">我的</div>
            <a class="footer-link" @click="router.push('/order')">我的订单</a>
            <a class="footer-link" @click="router.push('/address')">收货地址</a>
            <a class="footer-link" @click="router.push('/profile')">个人中心</a>
          </div>
        </div>
      </div>
      <div class="footer-bottom">© 2026 FlashFlow. All rights reserved.</div>
    </footer>
  </div>
</template>

<style>
/* ── App Container ── */
.consumer-app { min-height: 100vh; background: var(--ff-bg); display: flex; flex-direction: column; }
.consumer-main { flex: 1; }

/* ── Header ── */
.consumer-header {
  background: rgba(255,255,255,0.92); backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--ff-border); position: sticky; top: 0; z-index: 1000;
}
.header-inner {
  max-width: 1280px; margin: 0 auto; padding: 0 20px;
  display: flex; align-items: center; height: 60px; justify-content: space-between;
}
.header-left { display: flex; align-items: center; gap: 32px; }
.brand-logo { font-size: 20px; font-weight: 800; color: var(--ff-text); cursor: pointer; }
.header-nav { display: flex; gap: 4px; }
.nav-link {
  padding: 6px 14px; font-size: 14px; font-weight: 500; color: var(--ff-text-secondary);
  cursor: pointer; border-radius: 8px; transition: all var(--ff-transition); text-decoration: none;
  display: flex; align-items: center; gap: 6px;
}
.nav-link:hover { color: var(--ff-text); background: var(--ff-bg-hover); }
.nav-link.active { color: var(--ff-accent-dark); font-weight: 600; }
.nav-badge {
  font-size: 10px; font-weight: 700; color: #fff; background: var(--ff-accent);
  padding: 1px 5px; border-radius: 3px; letter-spacing: 0.5px;
}
.header-right { display: flex; align-items: center; gap: 4px; }
.icon-btn {
  display: flex; align-items: center; gap: 4px; padding: 6px 10px;
  font-size: 14px; color: var(--ff-text-secondary); cursor: pointer;
  border-radius: 8px; transition: all var(--ff-transition); text-decoration: none;
}
.icon-btn:hover { color: var(--ff-text); background: var(--ff-bg-hover); }
.cart-badge { display: flex; align-items: center; }
.user-pill {
  cursor: pointer; display: flex; align-items: center; gap: 5px;
  background: var(--ff-primary-light); padding: 5px 14px; border-radius: 20px;
  font-size: 13px; color: var(--ff-primary); font-weight: 500;
  transition: background var(--ff-transition);
}
.user-pill:hover { background: var(--ff-primary-200); }

/* ── Footer ── */
.consumer-footer { background: #0f172a; margin-top: auto; padding: 40px 20px 24px; }
.footer-grid {
  max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between;
  flex-wrap: wrap; gap: 40px;
}
.footer-brand-name { font-size: 22px; font-weight: 800; color: #fff; margin-bottom: 6px; }
.footer-brand-desc { font-size: 13px; color: rgba(255,255,255,0.35); }
.footer-links { display: flex; gap: 48px; }
.footer-col { display: flex; flex-direction: column; gap: 8px; }
.footer-col-title { color: rgba(255,255,255,0.7); font-weight: 600; font-size: 14px; margin-bottom: 4px; }
.footer-link { font-size: 13px; color: rgba(255,255,255,0.35); cursor: pointer; text-decoration: none; transition: color 0.2s; }
.footer-link:hover { color: var(--ff-accent); }
.footer-bottom {
  max-width: 1200px; margin: 24px auto 0; padding-top: 16px;
  border-top: 1px solid rgba(255,255,255,0.06); text-align: center;
  font-size: 12px; color: rgba(255,255,255,0.2);
}

/* ── Transitions ── */
.fade-slide-enter-active { transition: all 0.25s ease-out; }
.fade-slide-leave-active { transition: all 0.2s ease-in; }
.fade-slide-enter-from { opacity: 0; transform: translateY(12px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-8px); }

/* ── Responsive ── */
@media (max-width: 768px) {
  .header-nav { gap: 0; }
  .nav-link { padding: 6px 8px; font-size: 13px; }
  .footer-links { gap: 24px; }
}
</style>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCart } from '@/api/cart'
import { ShoppingCart, UserFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartCount = ref(0)

function handleLogout() { userStore.logout(); router.push('/') }

// 每次路由变化重新获取购物车数量
watch(() => route.path, async () => {
  const uid = localStorage.getItem('flashflow_userId') || userStore.userId
  if (uid) {
    try { const r = await getCart(Number(uid)); cartCount.value = (r.data || []).length } catch {}
  }
}, { immediate: true })
</script>
