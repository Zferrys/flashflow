import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  // 消费者页面（使用统一 ConsumerLayout）
  {
    path: '/',
    component: () => import('@/layout/ConsumerLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: 'FlashFlow 首页', public: true },
      },
      {
        path: 'shop',
        name: 'Shop',
        component: () => import('@/views/product/Shop.vue'),
        meta: { title: '商城', public: true },
      },
      {
        path: 'shop/:skuId',
        name: 'ShopDetail',
        component: () => import('@/views/product/ShopDetail.vue'),
        meta: { title: '商品详情', public: true },
      },
      {
        path: 'seckill',
        name: 'Seckill',
        component: () => import('@/views/seckill/SeckillDetail.vue'),
        meta: { title: '秒杀', public: true },
      },
      {
        path: 'seckill/:activityId/:skuId?',
        name: 'SeckillDetail',
        component: () => import('@/views/seckill/SeckillDetail.vue'),
        meta: { title: '秒杀详情', public: true },
      },
      {
        path: 'order',
        name: 'MyOrders',
        component: () => import('@/views/order/MyOrders.vue'),
        meta: { title: '我的订单' },
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/Cart.vue'),
        meta: { title: '购物车' },
      },
      {
        path: 'payment/:orderSn',
        name: 'Payment',
        component: () => import('@/views/payment/Payment.vue'),
        meta: { title: '支付中心', public: true },
      },
      {
        path: 'address',
        name: 'Address',
        component: () => import('@/views/Address.vue'),
        meta: { title: '收货地址' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心' },
      },
      {
        path: 'coupon',
        name: 'Coupon',
        component: () => import('@/views/Coupon.vue'),
        meta: { title: '领券中心' },
      },
    ],
  },
  // 退款页
  {
    path: '/refund/:orderSn',
    name: 'Refund',
    component: () => import('@/views/order/Refund.vue'),
    meta: { title: '申请退款' },
  },
  // 登录页（独立）
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true },
  },
  // 注册页
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', public: true },
  },
  // 404
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在', public: true },
  },
  // 管理后台
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '控制台', icon: 'Odometer' },
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', icon: 'User' },
      },
      {
        path: 'activity',
        name: 'ActivityList',
        component: () => import('@/views/activity/ActivityList.vue'),
        meta: { title: '活动管理', icon: 'Timer' },
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理', icon: 'List' },
      },
      {
        path: 'product',
        name: 'ProductList',
        component: () => import('@/views/product/ProductList.vue'),
        meta: { title: '商品管理', icon: 'Goods' },
      },
      {
        path: 'coupon',
        name: 'CouponAdmin',
        component: () => import('@/views/coupon/CouponAdmin.vue'),
        meta: { title: '优惠券管理', icon: 'Ticket' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：未登录跳转登录页，非管理员禁止访问 /admin
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  document.title = (to.meta.title as string) || 'FlashFlow'

  // 公开页面直接放行
  if (to.meta.public) {
    return next()
  }

  // 未登录 → 跳转登录页
  if (!userStore.token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 管理后台 → 仅管理员可访问
  if (to.path.startsWith('/admin') && userStore.role !== 'ROLE_ADMIN') {
    return next('/')
  }

  next()
})

export default router
