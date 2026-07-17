<template>
  <div style="min-height:100vh">
    <!-- HERO -->
    <div style="background:linear-gradient(135deg,#0f172a,#1e293b,#1e3a5f);padding:80px 0 90px;position:relative;overflow:hidden">
      <div style="position:absolute;inset:0;pointer-events:none;background:radial-gradient(ellipse 700px 500px at 70% 30%,rgba(59,130,246,0.1),transparent),radial-gradient(ellipse 500px 400px at 30% 80%,rgba(249,115,22,0.06),transparent)"></div>
      <div style="position:absolute;top:0;left:0;right:0;bottom:0;background:url('data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2240%22 height=%2240%22><circle cx=%222%22 cy=%222%22 r=%221%22 fill=%22rgba(255,255,255,0.03)%22/></svg>');opacity:0.5"></div>
      <div style="max-width:1280px;margin:0 auto;padding:0 24px;position:relative;z-index:1;display:flex;gap:60px;align-items:center">
        <div style="flex:1">
          <div style="display:inline-flex;align-items:center;gap:6px;background:rgba(59,130,246,0.12);border:1px solid rgba(59,130,246,0.2);padding:5px 14px;border-radius:20px;color:#93c5fd;font-size:12px;font-weight:500;margin-bottom:24px">
            <span style="width:6px;height:6px;background:#22c55e;border-radius:50%;display:inline-block"></span>
            周年庆活动火热进行中
          </div>
          <h1 style="font-size:clamp(32px,5vw,54px);font-weight:800;color:#fff;line-height:1.1;margin:0 0 8px">限时秒杀 <span style="background:linear-gradient(135deg,#f97316,#ef4444);-webkit-background-clip:text;-webkit-text-fill-color:transparent">低至5折</span></h1>
          <p style="font-size:16px;color:rgba(255,255,255,0.45);margin:0 0 32px;max-width:520px">精选全球好物，每日准时开抢。Apple、Samsung、Sony 等一线品牌限时限量特惠。</p>
          <div style="display:flex;gap:12px;flex-wrap:wrap">
            <el-button type="danger" size="large" round style="font-size:15px;font-weight:700;padding:14px 36px;height:auto" @click="$router.push('/seckill')">立即抢购</el-button>
            <el-button size="large" round style="font-size:15px;padding:14px 36px;height:auto;color:#fff;border-color:rgba(255,255,255,0.25);background:transparent" @click="$router.push('/shop')">浏览商品</el-button>
          </div>
        </div>
        <div style="display:none;lg:flex;gap:16px;flex-shrink:0" class="hide-mobile">
          <div style="width:180px;height:240px;background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.08);border-radius:20px;padding:24px;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;color:#fff">
            <div style="width:64px;height:64px;border-radius:16px;background:rgba(59,130,246,0.2);display:flex;align-items:center;justify-content:center;font-size:28px;margin-bottom:12px">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#60a5fa" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2"/><line x1="12" y1="18" x2="12" y2="18.01"/></svg>
            </div>
            <div style="font-weight:600;font-size:14px">iPhone 16 Pro</div>
            <div style="font-size:20px;font-weight:800;color:#f97316;margin-top:6px">¥7,999</div>
            <div style="font-size:11px;color:rgba(255,255,255,0.35);margin-top:2px">省 ¥2,000</div>
          </div>
        </div>
      </div>
    </div>

    <div class="ff-container">
      <!-- 分类 -->
      <div class="ff-section" style="margin-top:-24px;position:relative;z-index:2">
        <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:10px;background:var(--ff-bg-card);border-radius:14px;padding:18px;box-shadow:0 1px 3px rgba(0,0,0,0.05)">
          <div v-for="cat in categories" :key="cat.id" style="display:flex;flex-direction:column;align-items:center;gap:8px;padding:14px 8px;border-radius:10px;cursor:pointer;transition:all 0.2s" @click="$router.push(`/shop?cat=${cat.id}`)" @mouseenter="(e:any) => e.currentTarget.style.background='#f3f4f6'" @mouseleave="(e:any) => e.currentTarget.style.background=''">
            <div style="width:44px;height:44px;border-radius:10px;background:#eff6ff;display:flex;align-items:center;justify-content:center;font-size:20px;font-weight:700;color:#3b82f6">{{ cat.label }}</div>
            <span style="font-size:12px;font-weight:500;color:#6b7280">{{ cat.name }}</span>
          </div>
        </div>
      </div>

      <!-- 秒杀 -->
      <div class="ff-section" v-if="flashActivities.length">
        <div class="ff-section-header">
          <div class="title"><span class="icon"></span>限时秒杀<span class="countdown" v-if="countdownText"><span class="countdown-time">{{ countdownText }}</span></span></div>
          <div class="more" @click="$router.push('/seckill')">全部活动 →</div>
        </div>
        <el-row :gutter="14">
          <el-col v-for="act in flashActivities.slice(0,3)" :key="act.id" :xs="24" :sm="12" :md="8" style="margin-bottom:14px">
            <div class="act-card" @click="$router.push(`/seckill/${act.id}`)">
              <div style="display:flex;align-items:center;gap:14px">
                <div style="width:56px;height:56px;border-radius:14px;background:linear-gradient(135deg,#fef2f2,#fff7ed);display:flex;align-items:center;justify-content:center;font-size:26px;flex-shrink:0">
                  <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2"><polygon points="13,2 3,14 12,14 11,22 21,10 12,10"/></svg>
                </div>
                <div style="flex:1;min-width:0">
                  <div style="font-weight:600;font-size:15px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ act.name }}</div>
                  <div style="font-size:12px;color:#9ca3af;margin-top:2px">{{ fmt(act.startTime) }} ~ {{ fmt(act.endTime) }}</div>
                </div>
              </div>
              <div style="display:flex;justify-content:space-between;align-items:center;margin-top:14px">
                <span class="ff-tag ff-tag-red">进行中</span>
                <el-button size="small" type="danger" round>抢购</el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 推荐 -->
      <div class="ff-section" v-if="skuList.length">
        <div class="ff-section-header">
          <div class="title"><span class="icon"></span>热门推荐 <span style="font-size:12px;color:#9ca3af;font-weight:400;margin-left:4px">{{ skuList.length }}件</span></div>
          <div class="more" @click="$router.push('/shop')">浏览全部 →</div>
        </div>

        <el-row :gutter="14" v-loading="loading">
          <el-col v-for="sku in skuList.slice(0,10)" :key="sku.id" :xs="12" :sm="8" :md="6" :lg="4.8" style="margin-bottom:14px">
            <div class="ff-product-card" @click="$router.push(`/shop/${sku.id}`)">
              <div class="img-wrap">
                <img :src="sku.image" :alt="sku.skuName" loading="lazy" />
                <div v-if="sku.price < 5000" class="badge">优惠</div>
                <!-- 图片加载失败时显示 -->
                <div class="img-fallback" v-if="false">
                  <span>{{ (sku.skuName||'')[0] }}</span>
                </div>
              </div>
              <div class="info">
                <div class="name">{{ sku.skuName }}</div>
                <div class="desc">{{ sku.specs || '' }}</div>
                <div class="price">¥{{ sku.price }}<s v-if="sku.price < 8000">¥{{ Math.round(sku.price * 1.35) }}</s></div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<style scoped>
.countdown { display:inline-flex;align-items:center;gap:8px;margin-left:12px }
.countdown-time { font-size:13px;font-weight:700;color:#ef4444;background:#fef2f2;padding:2px 10px;border-radius:4px }
.act-card { background:var(--ff-bg-card);border-radius:12px;padding:20px;cursor:pointer;border:1px solid #f3f4f6;transition:all 0.2s }
.act-card:hover { box-shadow:0 4px 12px rgba(0,0,0,0.06);border-color:#e5e7eb }

@media (max-width:768px) {
  .ff-section-header .title { font-size:16px }
}
</style>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getFlashNow, type PromotionActivity } from '@/api/activity'
import { getActiveSkus } from '@/api/product'

const loading = ref(false)
const flashActivities = ref<PromotionActivity[]>([])
const skuList = ref<any[]>([])

const categories = [
  { id:1, name:'手机', label:'P' },
  { id:2, name:'笔记本', label:'L' },
  { id:3, name:'音频', label:'A' },
  { id:4, name:'手表', label:'W' },
  { id:5, name:'平板', label:'T' },
  { id:6, name:'游戏', label:'G' },
  { id:7, name:'配件', label:'Ac' },
]

const countdownText = computed(() => {
  const act = flashActivities.value[0]
  if (!act?.endTime) return ''
  const diff = new Date(act.endTime).getTime() - Date.now()
  if (diff <= 0) return '已结束'
  const h = Math.floor(diff/3600000), m = Math.floor((diff%3600000)/60000), s = Math.floor((diff%60000)/1000)
  return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
})

function fmt(t: string) { return t?.replace('T',' ').substring(5,16) || '' }

onMounted(async () => {
  loading.value = true
  try {
    const [flashR, skuR] = await Promise.allSettled([getFlashNow(), getActiveSkus()])
    if (flashR.status === 'fulfilled' && flashR.value?.data) {
      flashActivities.value = (flashR.value.data as any[]).filter((a:any) => a.status === 2)
    }
    if (skuR.status === 'fulfilled' && skuR.value?.data) {
      skuList.value = skuR.value.data as any[]
    }
  } finally { loading.value = false }
})
</script>
