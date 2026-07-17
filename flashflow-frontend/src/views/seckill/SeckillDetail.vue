<template>
  <div>
    <div class="ff-container" style="margin:20px auto">

      <!-- ═══════════ 活动列表模式 ═══════════ -->
      <template v-if="!hasActivityId">
        <div class="ff-section-header" style="margin-bottom:16px">
          <div class="title"><span class="icon"></span> 限时秒杀</div>
        </div>
        <el-row :gutter="16">
          <el-col v-for="act in allActivities" :key="act.id" :xs="24" :sm="12" :md="8" style="margin-bottom:14px">
            <div class="act-card" @click="goToActivity(act)">
              <div class="act-card-top">
                <div class="act-card-icon"></div>
                <div class="act-card-info">
                  <div class="act-card-name">{{ act.name }}</div>
                  <div class="act-card-time">{{ fmt(act.startTime) }} ~ {{ fmt(act.endTime) }}</div>
                </div>
              </div>
              <div class="act-card-bottom">
                <span class="ff-tag" :class="act.status === 2 ? 'ff-tag-red' : act.status === 1 ? 'ff-tag-orange' : 'ff-tag-blue'">
                  {{ ['草稿','即将开始','进行中','已结束'][act.status||0] }}
                </span>
                <el-button :type="act.status===2?'danger':act.status===1?'warning':'info'" size="small" round>
                  {{ act.status===2?'去抢购 →':act.status===1?'即将开始':'已结束' }}
                </el-button>
              </div>
            </div>
          </el-col>
          <el-empty v-if="allActivities.length===0" description="暂无秒杀活动" />
        </el-row>
      </template>

      <!-- ═══════════ 详情模式 ═══════════ -->
      <template v-else>
        <el-breadcrumb separator="→" style="margin-bottom:16px">
          <el-breadcrumb-item :to="{ path: '/seckill' }">秒杀</el-breadcrumb-item>
          <el-breadcrumb-item>{{ activity?.name }}</el-breadcrumb-item>
        </el-breadcrumb>

        <div v-loading="loading" style="background:var(--ff-bg-card);border-radius:var(--ff-radius-lg);padding:28px;box-shadow:var(--ff-shadow-sm)">

          <!-- 倒计时横幅 -->
          <div class="seckill-banner" v-if="countdown">
            <div class="seckill-banner-left">
              <span style="font-size:24px"></span>
              <span style="font-weight:700">限时秒杀</span>
              <span style="color:rgba(255,255,255,0.7);font-size:13px">{{ countdown.statusText }}</span>
            </div>
            <div class="seckill-banner-right">
              <div class="cd-block"><span class="cd-num">{{ countdown.h }}</span><span class="cd-label">时</span></div>
              <span class="cd-sep">:</span>
              <div class="cd-block"><span class="cd-num">{{ countdown.m }}</span><span class="cd-label">分</span></div>
              <span class="cd-sep">:</span>
              <div class="cd-block"><span class="cd-num">{{ countdown.s }}</span><span class="cd-label">秒</span></div>
            </div>
          </div>

          <div v-if="sku" class="detail-layout">
            <!-- 图片 -->
            <div class="gallery" style="flex:1;min-width:300px;max-width:450px">
              <div style="border-radius:var(--ff-radius-lg);overflow:hidden;background:#fafafa;aspect-ratio:1;display:flex;align-items:center;justify-content:center">
                <img :src="sku.skuImage" style="width:100%;height:100%;object-fit:cover" />
              </div>
            </div>

            <!-- 信息 -->
            <div class="info-panel" style="flex:1;min-width:300px">
              <h1 style="font-size:22px;font-weight:700;margin:0 0 16px">{{ sku.skuName }}</h1>

              <div class="price-box">
                <div class="price-row">
                  <span class="price-label">秒杀价</span>
                  <span class="price-value">¥{{ sku.activityPrice }}</span>
                  <span class="price-original">¥{{ sku.originalPrice }}</span>
                </div>
                <!-- 进度条 -->
                <div class="progress-section">
                  <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:4px">
                    <span style="color:var(--ff-text-muted)">已抢 {{ sku.soldCount || 0 }} 件</span>
                    <span style="color:var(--ff-danger);font-weight:600" v-if="(sku.soldCount||0)/(sku.stockLimit||1) > 0.7">🔥 即将售罄</span>
                  </div>
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: Math.min(100, ((sku.soldCount||0)/(sku.stockLimit||1))*100) + '%' }"></div>
                  </div>
                  <div style="display:flex;justify-content:space-between;font-size:12px;margin-top:4px">
                    <span style="color:var(--ff-text-muted)">总库存 {{ sku.stockLimit }}</span>
                    <span style="color:var(--ff-text-muted)">限购 {{ sku.perUserLimit }} 件</span>
                  </div>
                </div>
              </div>

              <!-- SKU 选择 -->
              <div v-if="skuList.length>1" class="sku-section">
                <div class="sku-label">选择规格</div>
                <div class="sku-tags">
                  <span v-for="s in skuList" :key="s.skuId" class="sku-tag" :class="{ active: s.skuId === sku?.skuId }" @click="sku = s">
                    {{ s.skuName }}
                  </span>
                </div>
              </div>

              <!-- 按钮 -->
              <div style="margin-top:24px">
                <el-button v-if="isActive" type="danger" size="large" class="btn-buy" :loading="buying" @click="handleBuy">
                   立即抢购
                </el-button>
                <el-button v-else-if="isUpcoming" size="large" disabled class="btn-disabled">暂未开始</el-button>
                <el-button v-else size="large" disabled class="btn-disabled">已结束</el-button>
              </div>
            </div>
          </div>

          <el-empty v-if="!sku && !loading" description="商品信息不存在" />
        </div>

        <!-- 结果弹窗 -->
        <el-dialog v-model="resultVisible" :title="result.success ? '🎉 抢购成功' : '😅 抢购失败'" width="400px">
          <div style="text-align:center;padding:16px">
            <div style="font-size:48px;margin-bottom:12px">{{ result.success ? '🎉' : '😅' }}</div>
            <p style="font-size:16px;font-weight:600">{{ result.message }}</p>
            <p v-if="result.orderSn" style="color:var(--ff-text-muted);font-size:13px;margin-top:8px">订单号：{{ result.orderSn }}</p>
          </div>
          <template #footer>
            <el-button @click="resultVisible=false">继续浏览</el-button>
            <el-button v-if="result.orderSn" type="primary" @click="router.push(`/payment/${result.orderSn}`)">去支付 →</el-button>
          </template>
        </el-dialog>
      </template>

    </div>
  </div>
</template>

<style scoped>
/* Countdown Banner */
.seckill-banner {
  background: linear-gradient(135deg, #1e1b4b, #312e81);
  border-radius: var(--ff-radius-lg);
  padding: 20px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}
.seckill-banner-left { display: flex; align-items: center; gap: 10px; }
.cd-block { display: flex; flex-direction: column; align-items: center; background: rgba(0,0,0,0.3); border-radius: 8px; padding: 6px 12px; min-width: 48px; }
.cd-num { font-size: 24px; font-weight: 800; line-height: 1; }
.cd-label { font-size: 11px; color: rgba(255,255,255,0.6); }
.cd-sep { font-size: 20px; font-weight: 700; }
.seckill-banner-right { display: flex; align-items: center; gap: 6px; }

/* Progress */
.progress-section { margin-top: 14px; }
.progress-bar { height: 10px; background: #fee2e2; border-radius: 5px; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, var(--ff-danger), #f97316); border-radius: 5px; transition: width 0.6s ease; }

/* Buttons */
.btn-buy { width: 100%; height: 52px; font-size: 18px; font-weight: 700; border-radius: var(--ff-radius) !important; }
.btn-disabled { width: 100%; height: 52px; font-size: 18px; border-radius: var(--ff-radius) !important; background: #e5e7eb; color: #9ca3af; border: none; }

/* Activity Card */
.act-card { background: var(--ff-bg-card); border-radius: var(--ff-radius); padding: 20px; cursor: pointer; border: 1px solid var(--ff-border); transition: all 0.2s; }
.act-card:hover { box-shadow: var(--ff-shadow-md); border-color: var(--ff-primary-100); }
.act-card-top { display: flex; gap: 12px; align-items: center; margin-bottom: 14px; }
.act-card-icon { font-size: 32px; width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; background: #fef2f2; border-radius: 10px; }
.act-card-name { font-weight: 600; font-size: 15px; }
.act-card-time { font-size: 12px; color: var(--ff-text-muted); margin-top: 2px; }
.act-card-bottom { display: flex; justify-content: space-between; align-items: center; }

/* Shared */
.detail-layout { display: flex; gap: 40px; flex-wrap: wrap; }
.gallery { flex: 1; min-width: 300px; max-width: 450px; }
.price-box {
  background: linear-gradient(135deg, #fef2f2, #fff7ed);
  padding: 20px;
  border-radius: var(--ff-radius);
  margin-bottom: 20px;
}
.price-row { display: flex; align-items: baseline; gap: 10px; }
.price-label { font-size: 13px; color: var(--ff-text-muted); }
.price-value { font-size: 36px; font-weight: 800; color: var(--ff-danger); }
.price-original { font-size: 15px; color: var(--ff-text-muted); text-decoration: line-through; }

.sku-section { margin-bottom: 20px; }
.sku-label { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.sku-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.sku-tag {
  padding: 8px 16px; border: 1px solid var(--ff-border); border-radius: 8px;
  font-size: 13px; cursor: pointer; transition: all 0.2s; background: var(--ff-bg-card);
}
.sku-tag:hover, .sku-tag.active { border-color: var(--ff-primary); color: var(--ff-primary); background: var(--ff-primary-light); }

@media (max-width: 768px) {
  .seckill-banner { flex-direction: column; align-items: flex-start; }
  .cd-block { min-width: 40px; padding: 4px 8px; }
  .cd-num { font-size: 20px; }
}
</style>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getActivityById, getActivitySkuList, getActivityPage, flashSale, type PromotionActivity, type PromotionSku } from '@/api/activity'
import { ElMessage } from 'element-plus'
import { addToCart } from '@/api/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const hasActivityId = computed(() => !!route.params.activityId)
const loading = ref(false)
const buying = ref(false)
const activity = ref<PromotionActivity | null>(null)
const sku = ref<PromotionSku | null>(null)
const skuList = ref<PromotionSku[]>([])
const allActivities = ref<PromotionActivity[]>([])
const result = ref({ success: false, message: '', orderSn: '' })
const resultVisible = ref(false)

const isActive = computed(() => {
  if (!activity.value) return false
  const now = Date.now()
  return now >= new Date(activity.value.startTime).getTime() && now < new Date(activity.value.endTime).getTime()
})
const isUpcoming = computed(() => {
  if (!activity.value) return false
  return Date.now() < new Date(activity.value.startTime).getTime()
})

// Countdown
const countdown = ref({ h: '00', m: '00', s: '00', statusText: '' } as any)
let cdTimer: any = null

function updateCountdown() {
  const act = activity.value
  if (!act) return
  const now = Date.now()
  const start = new Date(act.startTime).getTime()
  const end = new Date(act.endTime).getTime()

  let target: number, text: string
  if (now < start) { target = start; text = '距开始' }
  else if (now < end) { target = end; text = '距结束' }
  else { countdown.value = null; return }

  const diff = target - now
  if (diff <= 0) { countdown.value = null; return }
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  countdown.value = { h: String(h).padStart(2,'0'), m: String(m).padStart(2,'0'), s: String(s).padStart(2,'0'), statusText: text }
}

function fmt(t: string) { return t?.replace('T',' ').substring(5,16) || '' }

async function fetchActivities() {
  try {
    const res = await getActivityPage({ page: 1, size: 20 })
    allActivities.value = res.data.records || []
  } catch { allActivities.value = [] }
}

async function fetchDetail() {
  const aid = Number(route.params.activityId)
  if (!aid) return
  loading.value = true
  try {
    const [actR, skuR] = await Promise.all([
      getActivityById(aid),
      getActivitySkuList(aid),
    ])
    activity.value = actR.data
    skuList.value = skuR.data || []
    const sid = Number(route.params.skuId)
    sku.value = sid ? skuList.value.find(s => s.skuId === sid) || skuList.value[0] : skuList.value[0]
    updateCountdown()
    cdTimer = setInterval(updateCountdown, 1000)
  } finally { loading.value = false }
}

// 页面隐藏时暂停倒计时，节省资源
function onVisibilityChange() {
  if (document.hidden) {
    if (cdTimer) { clearInterval(cdTimer); cdTimer = null }
  } else {
    updateCountdown()
    if (!cdTimer) cdTimer = setInterval(updateCountdown, 1000)
  }
}

async function goToActivity(act: PromotionActivity) {
  try {
    const r = await getActivitySkuList(act.id!)
    if ((r.data||[]).length) router.push(`/seckill/${act.id}/${r.data[0].skuId}`)
    else ElMessage.info('该活动暂无商品')
  } catch { router.push(`/seckill/${act.id}`) }
}

async function handleBuy() {
  if (!sku.value || !activity.value) return
  const uid = userStore.userId  // 使用 store 替代 localStorage
  if (!uid) { ElMessage.warning('请先登录'); router.push('/login'); return }
  buying.value = true
  try {
    const res = await flashSale({ activityId: activity.value.id!, skuId: sku.value.skuId, userId: uid, quantity: 1 })
    result.value = res.data
    resultVisible.value = true
  } catch (e: any) {
    // 优先显示服务器返回的错误信息
    const serverMsg = e?.response?.data?.msg || e?.message || ''
    result.value = {
      success: false,
      message: serverMsg || '😅 抢购失败，请稍后再试',
      orderSn: ''
    }
    resultVisible.value = true
  } finally { buying.value = false }
}

// 监听路由参数变化（同一组件复用时不 remount）
watch(() => route.params.activityId, (newId) => {
  if (newId) fetchDetail()
  else { activity.value = null; skuList.value = []; fetchActivities() }
})

onMounted(() => {
  if (hasActivityId.value) fetchDetail()
  else fetchActivities()
  document.addEventListener('visibilitychange', onVisibilityChange)
})
onUnmounted(() => {
  if (cdTimer) clearInterval(cdTimer)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>
