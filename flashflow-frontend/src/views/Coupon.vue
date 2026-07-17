<template>
  <div class="coupon-page">
    <!-- ====== 可领取优惠券 ====== -->
    <div class="section">
      <div class="section-title">
        <span class="title-icon"></span>
        领券中心
        <span class="badge">{{ availableList.length }}</span>
      </div>
      <div class="coupon-grid" v-loading="loading">
        <div
          v-for="c in availableList"
          :key="c.id"
          class="coupon-card"
          :class="{ owned: myCouponIds.has(c.id), 'type-discount': c.type === 2 }"
        >
          <!-- 左侧：面额 -->
          <div class="coupon-left">
            <div class="coupon-value">
              <span class="symbol">¥</span>
              <span class="amount" v-if="c.type === 1">{{ c.discountAmount }}</span>
              <span class="amount" v-else>{{ (c.discountRate * 10).toFixed(1) }}</span>
              <span class="suffix" v-if="c.type === 2">折</span>
            </div>
            <div class="coupon-condition" v-if="c.conditionAmount > 0">满{{ c.conditionAmount }}可用</div>
            <div class="coupon-condition" v-else>无门槛</div>
          </div>
          <!-- 中间虚线分隔 -->
          <div class="coupon-divider"></div>
          <!-- 右侧：信息 -->
          <div class="coupon-right">
            <div class="coupon-name">{{ c.name }}</div>
            <div class="coupon-scope" v-if="c.scope !== 'ALL'">{{ scopeLabel(c) }}</div>
            <div class="coupon-scope" v-else>全场通用</div>
            <div class="coupon-time">{{ formatRange(c) }}</div>
            <div class="coupon-stock">剩余 {{ c.remainCount }}/{{ c.totalCount }}</div>
          </div>
          <!-- 操作按钮 -->
          <div class="coupon-action">
            <button v-if="!myCouponIds.has(c.id)" class="btn-claim" @click="doClaim(c.id!)">立即领取</button>
            <div v-else class="tag-owned">已拥有</div>
          </div>
        </div>
        <el-empty v-if="!loading && availableList.length === 0" description="暂无可领优惠券" />
      </div>
    </div>

    <!-- ====== 我的优惠券 ====== -->
    <div class="section">
      <div class="section-title">
        <span class="title-icon my"></span>
        我的优惠券
        <span class="badge">{{ myList.length }}</span>
        <span class="sub-hint" v-if="myList.length">下单时自动计算最优优惠</span>
      </div>
      <div class="coupon-grid" v-loading="myLoading">
        <div
          v-for="uc in myList"
          :key="uc.id"
          class="coupon-card my-coupon"
          :class="{ used: uc.used, 'type-discount': uc.type === 2 }"
        >
          <!-- 左侧 -->
          <div class="coupon-left">
            <div class="coupon-value">
              <span class="symbol">¥</span>
              <span class="amount" v-if="uc.type === 1">{{ uc.discountAmount }}</span>
              <span class="amount" v-else>{{ (uc.discountRate * 10).toFixed(1) }}</span>
              <span class="suffix" v-if="uc.type === 2">折</span>
            </div>
            <div class="coupon-condition" v-if="uc.conditionAmount > 0">满{{ uc.conditionAmount }}可用</div>
            <div class="coupon-condition" v-else>无门槛</div>
          </div>
          <div class="coupon-divider"></div>
          <!-- 右侧 -->
          <div class="coupon-right">
            <div class="coupon-name">{{ uc.couponName }}</div>
            <div class="coupon-scope" v-if="uc.scope !== 'ALL'">{{ scopeLabel(uc) }}</div>
            <div class="coupon-scope" v-else>全场通用</div>
            <div class="coupon-time">{{ formatRange(uc) }}</div>
          </div>
          <!-- 状态 -->
          <div class="coupon-action">
            <span v-if="uc.used" class="tag-used">已使用</span>
            <span v-else class="tag-available">可使用</span>
            <div class="coupon-get-time">领取于 {{ fmtDate(uc.getTime) }}</div>
          </div>
        </div>
        <el-empty v-if="!myLoading && myList.length === 0" description="还没有优惠券，去领券中心看看吧">
          <el-button type="primary" @click="window.scrollTo({top:0,behavior:'smooth'})">去领券</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getAvailableCoupons, getMyCoupons, claimCoupon } from '@/api/coupon'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const myLoading = ref(false)
const availableList = ref<any[]>([])
const myList = ref<any[]>([])
const myCouponIds = reactive(new Set<number>())

// ===== 格式化 =====
function fmtDate(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 10)
}
function formatRange(c: any) {
  const s = fmtDate(c.startTime)
  const e = fmtDate(c.endTime)
  return `${s.substring(5)} ~ ${e.substring(5)}`
}
const catMap: Record<string,string> = {'1':'手机','2':'笔记本','3':'音频','4':'穿戴','5':'平板','6':'游戏','7':'配件'}
function scopeLabel(c: any) {
  if (c.scope === 'CATEGORY') return '仅限' + (catMap[c.scopeValue] || '分类')
  if (c.scope === 'SKU') return '指定商品可用'
  return '全场通用'
}

// ===== 操作 =====
async function doClaim(couponId: number) {
  if (!userStore.token) { router.push('/login'); return }
  try {
    await claimCoupon(couponId)
    ElMessage.success('领取成功')
    myCouponIds.add(couponId)
    fetchMy()
  } catch { ElMessage.warning('领取失败') }
}

async function fetchAvailable() {
  if (!userStore.token) return
  loading.value = true
  try {
    const res = await getAvailableCoupons()
    availableList.value = res.data || []
  } finally { loading.value = false }
}

async function fetchMy() {
  if (!userStore.token) return
  myLoading.value = true
  try {
    const res = await getMyCoupons()
    myList.value = res.data || []
    // 标记已拥有的券ID
    myList.value.forEach((uc: any) => myCouponIds.add(uc.couponId))
  } finally { myLoading.value = false }
}

onMounted(() => { fetchAvailable(); fetchMy() })
</script>

<style scoped>
.coupon-page { max-width: 960px; margin: 24px auto; padding: 0 16px; }

/* ===== 分区标题 ===== */
.section { margin-bottom: 32px; }
.section-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 18px; font-weight: 700; color: #1f2937; margin-bottom: 16px;
}
.title-icon { width: 4px; height: 20px; border-radius: 2px; background: #f59e0b; display: inline-block; }
.title-icon.my { background: #10b981; }
.badge {
  background: #fef3c7; color: #d97706; font-size: 12px; padding: 1px 8px; border-radius: 10px; font-weight: 600;
}
.sub-hint { font-size: 12px; color: #9ca3af; font-weight: 400; margin-left: auto; }

/* ===== 优惠券网格 ===== */
.coupon-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(420px, 1fr)); gap: 12px; }
@media (max-width: 480px) { .coupon-grid { grid-template-columns: 1fr; } }

/* ===== 优惠券卡片 ===== */
.coupon-card {
  display: flex; align-items: stretch; background: #fff;
  border-radius: 10px; overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  transition: transform .15s, box-shadow .15s; cursor: default;
  position: relative;
}
.coupon-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,.1); }

/* 左侧面额区 */
.coupon-left {
  width: 130px; min-width: 130px; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  background: linear-gradient(135deg, #f97316, #ef4444); color: #fff;
  padding: 16px 8px;
}
.type-discount .coupon-left { background: linear-gradient(135deg, #8b5cf6, #6366f1); }
.coupon-value { display: flex; align-items: baseline; }
.coupon-value .symbol { font-size: 14px; font-weight: 500; margin-right: 2px; }
.coupon-value .amount { font-size: 32px; font-weight: 800; line-height: 1; }
.coupon-value .suffix { font-size: 14px; margin-left: 2px; }
.coupon-condition { font-size: 11px; opacity: .8; margin-top: 4px; }

/* 虚线分隔 */
.coupon-divider {
  width: 1px; align-self: stretch;
  background: repeating-linear-gradient(to bottom, #e5e7eb 0, #e5e7eb 4px, transparent 4px, transparent 8px);
}

/* 右侧信息区 */
.coupon-right { flex: 1; padding: 14px 16px; display: flex; flex-direction: column; justify-content: center; gap: 4px; min-width: 0; }
.coupon-name { font-size: 14px; font-weight: 600; color: #1f2937; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.coupon-scope { font-size: 11px; color: #f59e0b; background: #fffbeb; padding: 1px 6px; border-radius: 4px; display: inline-block; width: fit-content; }
.coupon-time { font-size: 11px; color: #9ca3af; }
.coupon-stock { font-size: 11px; color: #d1d5db; }

/* 操作按钮区 */
.coupon-action { width: 90px; min-width: 90px; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 12px 8px; }
.btn-claim {
  background: #f97316; color: #fff; border: none; padding: 8px 18px; border-radius: 20px;
  font-size: 13px; font-weight: 600; cursor: pointer; transition: all .15s; white-space: nowrap;
}
.btn-claim:hover { background: #ea580c; }
.tag-owned { color: #f97316; font-size: 13px; font-weight: 600; }
.tag-used { color: #9ca3af; font-size: 13px; }
.tag-available { color: #10b981; font-size: 13px; font-weight: 600; }
.coupon-get-time { font-size: 10px; color: #d1d5db; margin-top: 4px; }

/* 已拥有/已使用 - 淡化 */
.coupon-card.owned .coupon-left { opacity: .55; filter: grayscale(30%); }
.coupon-card.used .coupon-left { opacity: .4; filter: grayscale(60%); }
.coupon-card.used .coupon-right { opacity: .6; }
</style>
