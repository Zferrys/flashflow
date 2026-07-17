<template>
  <el-container style="height: 100vh">
    <!-- 移动端遮罩 -->
    <div v-if="sidebarVisible && isMobile" style="position:fixed;inset:0;background:rgba(0,0,0,0.4);z-index:999" @click="sidebarVisible=false" />

    <!-- 侧边栏 -->
    <el-aside :width="sidebarVisible ? '220px' : '0'" class="admin-sidebar" :class="{ collapsed: !sidebarVisible }">
      <div class="sidebar-header">
        <span class="sidebar-logo">⚡</span>
        <span class="sidebar-title">FlashFlow</span>
        <span class="sidebar-badge">Admin</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="transparent"
        text-color="rgba(255,255,255,0.55)"
        active-text-color="#f59e0b"
        @select="isMobile && (sidebarVisible=false)"
      >
        <el-menu-item index="/admin/dashboard"><el-icon><Odometer /></el-icon><span>控制台</span></el-menu-item>
        <el-menu-item index="/admin/user"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item index="/admin/activity"><el-icon><Timer /></el-icon><span>活动管理</span></el-menu-item>
        <el-menu-item index="/admin/order"><el-icon><List /></el-icon><span>订单管理</span></el-menu-item>
        <el-menu-item index="/admin/product"><el-icon><Goods /></el-icon><span>商品管理</span></el-menu-item>
        <el-menu-item index="/admin/coupon"><el-icon><Ticket /></el-icon><span>优惠券管理</span></el-menu-item>
        <el-menu-item index="/admin/refund"><el-icon><Money /></el-icon><span>退款审批</span></el-menu-item>
        <div class="sidebar-divider"></div>
        <el-menu-item index="/">
          <el-icon><ShoppingCart /></el-icon><span>返回商城</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部导航 -->
      <el-header class="admin-header">
        <div class="header-left">
          <el-button text class="menu-trigger" @click="sidebarVisible=!sidebarVisible">
            <el-icon :size="20"><Fold v-if="sidebarVisible" /><Expand v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-badge">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.username || 'admin' }}
            </span>
            <template #dropdown>
              <el-dropdown-item @click="router.push('/')">🏠 返回商城</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">🚪 退出登录</el-dropdown-item>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="admin-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style>
/* ── Transition ── */
.fade-slide-enter-active { transition: all 0.25s ease-out; }
.fade-slide-leave-active { transition: all 0.15s ease-in; }
.fade-slide-enter-from { opacity: 0; transform: translateY(10px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-6px); }

/* ── Sidebar ── */
.admin-sidebar {
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%) !important;
  transition: width 0.25s ease !important;
  overflow: hidden; position: relative; z-index: 1000;
}
.sidebar-header {
  height: 64px; display: flex; align-items: center; gap: 8px;
  padding: 0 20px; border-bottom: 1px solid rgba(255,255,255,0.06);
  white-space: nowrap;
}
.sidebar-logo { font-size: 22px; }
.sidebar-title { font-size: 18px; font-weight: 800; color: #fff; }
.sidebar-badge {
  font-size: 10px; font-weight: 600; color: #f59e0b;
  background: rgba(245,158,11,0.15); padding: 2px 6px; border-radius: 4px;
  letter-spacing: 1px; text-transform: uppercase;
}
.sidebar-divider {
  height: 1px; background: rgba(255,255,255,0.06); margin: 8px 16px;
}

/* Sidebar Menu */
.admin-sidebar .el-menu {
  border-right: none !important;
  background: transparent !important;
}
.admin-sidebar .el-menu-item {
  margin: 2px 8px; border-radius: 8px;
  border-left: none !important;
  transition: all 0.2s; height: 44px; line-height: 44px;
  font-size: 14px;
}
.admin-sidebar .el-menu-item:hover {
  background: rgba(255,255,255,0.06) !important;
  color: rgba(255,255,255,0.85) !important;
}
.admin-sidebar .el-menu-item.is-active {
  background: rgba(245,158,11,0.12) !important;
  color: #f59e0b !important;
  font-weight: 600;
}

/* ── Header ── */
.admin-header {
  background: #fff; border-bottom: 1px solid var(--ff-border);
  display: flex; align-items: center; justify-content: space-between;
  height: 60px; padding: 0 20px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.menu-trigger { font-size: 18px; padding: 6px; color: #64748b; }
.menu-trigger:hover { color: #0f172a; }
.header-right { display: flex; align-items: center; gap: 12px; }
.user-badge {
  cursor: pointer; display: flex; align-items: center; gap: 6px;
  background: #f1f5f9; padding: 6px 14px; border-radius: 20px;
  font-size: 13px; color: #334155; font-weight: 500;
  transition: background 0.2s;
}
.user-badge:hover { background: #e2e8f0; }

/* ── Main ── */
.admin-main { background: #f1f5f9; padding: 24px; min-height: calc(100vh - 60px); }
</style>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Fold, Expand, Money } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const sidebarVisible = ref(true)
const isMobile = ref(false)

const pageTitle = computed(() => (route.meta.title as string) || 'FlashFlow')

let resizeHandler: (() => void) | null = null
onMounted(() => {
  resizeHandler = () => { isMobile.value = window.innerWidth < 768; if (isMobile.value) sidebarVisible.value = false }
  resizeHandler()
  window.addEventListener('resize', resizeHandler)
})
onUnmounted(() => {
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})

function handleLogout() { userStore.logout(); router.push('/login') }
</script>
