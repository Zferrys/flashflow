<template>
  <div>
    <div class="ff-container" style="margin:20px auto">

      <el-breadcrumb separator="→" style="margin-bottom:16px">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/shop' }">全部商品</el-breadcrumb-item>
        <el-breadcrumb-item v-if="spu">{{ spu.spuName }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div v-loading="loading" style="background:var(--ff-bg-card);border-radius:var(--ff-radius-lg);padding:28px;box-shadow:var(--ff-shadow-sm)">

        <div v-if="sku" class="detail-layout">
          <!-- ══════ 图片区 ══════ -->
          <div class="gallery">
            <div class="gallery-main">
              <img :src="selectedImage" :alt="sku.skuName" />
            </div>
            <div class="gallery-thumbs" v-if="gallery.length > 1">
              <div v-for="(img, i) in gallery" :key="i"
                class="gallery-thumb" :class="{ active: selectedImage === img }"
                @mouseenter="selectedImage = img">
                <img :src="img" loading="lazy" />
              </div>
            </div>
          </div>

          <!-- ══════ 信息区 ══════ -->
          <div class="info-panel">
            <h1 class="product-name">{{ sku.skuName }}</h1>
            <div class="product-spu-name">{{ spu?.spuName }}</div>

            <div class="price-box">
              <div class="price-row">
                <span class="price-label">秒杀价</span>
                <span class="price-value">¥{{ sku.price }}</span>
                <span class="price-original">¥{{ Math.round(sku.price * 1.4) }}</span>
                <span class="price-tag">立省 ¥{{ Math.round(sku.price * 0.4) }}</span>
              </div>
              <div class="stock-info">
                <span>库存 {{ sku.stock || 0 }} 件</span>
                <span v-if="(sku.stock||0) < 20" style="color:var(--ff-danger)">⚠ 库存紧张</span>
              </div>
            </div>

            <!-- SKU 选择 -->
            <div class="sku-section" v-if="skuList.length > 1">
              <div class="sku-label">选择规格</div>
              <div class="sku-tags">
                <span v-for="s in skuList" :key="s.id"
                  class="sku-tag" :class="{ active: selectedSkuId === s.id }"
                  @click="switchSku(s)">
                  {{ s.skuName }}
                </span>
              </div>
            </div>

            <!-- 数量 -->
            <div class="qty-section">
              <span class="sku-label">数量</span>
              <el-input-number v-model="quantity" :min="1" :max="sku.stock || 99" size="large" />
            </div>

            <!-- 按钮 -->
            <div class="action-btns">
              <el-button type="danger" size="large" class="btn-buy" :loading="buying" @click="buyNow">
                <span style="font-size:17px;font-weight:700">立即购买</span>
              </el-button>
              <el-button size="large" class="btn-cart" :loading="carting" @click="addToCart">
                <el-icon style="margin-right:4px"><ShoppingCart /></el-icon> 加入购物车
              </el-button>
            </div>

            <!-- 描述 -->
            <div class="desc-box" v-if="spu?.description">
              <div class="desc-title">商品描述</div>
              <p>{{ spu.description }}</p>
            </div>
          </div>
        </div>

        <el-empty v-if="!sku && !loading" description="商品信息不存在" />
      </div>
    </div>

    <!-- 结果弹窗 -->
    <el-dialog v-model="resultVisible" :title="result.success ? '🎉 下单成功' : '😅 下单失败'" width="400px" :close-on-click-modal="false">
      <div style="text-align:center;padding:16px">
        <div style="font-size:48px;margin-bottom:12px">{{ result.success ? '🎉' : '😅' }}</div>
        <p style="font-size:16px;font-weight:600">{{ result.message }}</p>
        <p v-if="result.orderSn" style="color:var(--ff-text-muted);font-size:13px;margin-top:8px">订单号：{{ result.orderSn }}</p>
      </div>
      <template #footer>
        <el-button @click="resultVisible=false">继续逛逛</el-button>
        <el-button v-if="result.orderSn" type="primary" @click="router.push(`/payment/${result.orderSn}`)">去支付 →</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-layout { display: flex; gap: 40px; flex-wrap: wrap; }
.gallery { flex: 1; min-width: 320px; max-width: 480px; }
.gallery-main {
  border-radius: var(--ff-radius-lg);
  overflow: hidden;
  background: #fafafa;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.gallery-main img { width: 100%; height: 100%; object-fit: cover; }
.gallery-thumbs { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; }
.gallery-thumb {
  width: 60px; height: 60px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
}
.gallery-thumb:hover, .gallery-thumb.active { border-color: var(--ff-primary); }
.gallery-thumb img { width: 100%; height: 100%; object-fit: cover; }

.info-panel { flex: 1; min-width: 320px; }
.product-name { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.product-spu-name { font-size: 13px; color: var(--ff-text-muted); margin-bottom: 20px; }

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
.price-tag { font-size: 12px; background: var(--ff-danger); color: #fff; padding: 2px 8px; border-radius: 4px; }
.stock-info { margin-top: 10px; font-size: 12px; color: var(--ff-text-muted); display: flex; gap: 16px; }

.sku-section { margin-bottom: 20px; }
.sku-label { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.sku-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.sku-tag {
  padding: 8px 16px;
  border: 1px solid var(--ff-border);
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--ff-bg-card);
}
.sku-tag:hover, .sku-tag.active {
  border-color: var(--ff-primary);
  color: var(--ff-primary);
  background: var(--ff-primary-light);
}

.qty-section { margin-bottom: 24px; display: flex; align-items: center; gap: 16px; }

.action-btns { display: flex; gap: 12px; }
.btn-buy { flex: 1; height: 48px; font-size: 16px; border-radius: var(--ff-radius) !important; font-weight: 700 !important; }
.btn-cart { width: 180px; height: 48px; border-radius: var(--ff-radius) !important; font-weight: 600 !important; border-color: var(--ff-border); }

.desc-box { margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--ff-border-light); }
.desc-title { font-weight: 600; margin-bottom: 8px; }
.desc-box p { font-size: 13px; color: var(--ff-text-secondary); line-height: 1.8; margin: 0; }

@media (max-width: 768px) {
  .detail-layout { gap: 20px; }
  .gallery { max-width: 100%; }
  .action-btns { flex-direction: column; }
  .btn-cart { width: 100%; }
}
</style>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getActiveSkus, getSpuDetail, type ProductSku, type ProductSpu } from '@/api/product'
import request from '@/api/request'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { addToCart as apiAddToCart } from '@/api/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const buying = ref(false)
const carting = ref(false)
const sku = ref<ProductSku | null>(null)
const spu = ref<ProductSpu | null>(null)
const skuList = ref<ProductSku[]>([])
const selectedSkuId = ref(0)
const quantity = ref(1)

const result = ref({ success: false, message: '', orderSn: '' })
const resultVisible = ref(false)
const selectedImage = ref('')
const gallery = ref<string[]>([])

function buildGallery() {
  const imgs: string[] = []
  // SPU 多图
  if (spu.value?.images) {
    spu.value.images.split(',').map(s => s.trim()).filter(Boolean).forEach(i => {
      if (!imgs.includes(i)) imgs.push(i)
    })
  }
  // SPU 主图
  if (spu.value?.mainImage && !imgs.includes(spu.value.mainImage)) imgs.push(spu.value.mainImage)
  // 所有 SKU 图
  skuList.value.forEach(s => {
    if (s.image && !imgs.includes(s.image)) imgs.push(s.image)
  })
  gallery.value = imgs
  selectedImage.value = sku.value?.image || gallery.value[0] || ''
}

function switchSku(s: ProductSku) {
  selectedSkuId.value = s.id!
  sku.value = s
  if (s.image) selectedImage.value = s.image
  quantity.value = 1
  window.history.replaceState(null, '', `/shop/${s.id}`)
}

onMounted(async () => {
  try {
    const skuId = Number(route.params.skuId)
    const res = await getActiveSkus()
    const list: ProductSku[] = res.data || []
    sku.value = list.find(s => s.id === skuId) || null
    if (sku.value?.spuId) {
      const spuRes = await getSpuDetail(sku.value.spuId)
      spu.value = spuRes.data
      skuList.value = list.filter(s => s.spuId === sku.value?.spuId)
      selectedSkuId.value = sku.value.id!
      buildGallery()
    } else if (sku.value?.image) {
      gallery.value = [sku.value.image]
      selectedImage.value = sku.value.image
    }
  } finally { loading.value = false }
})

async function addToCart() {
  const uid = String(userStore.userId || 0)
  if (!uid) { ElMessage.warning('请先登录'); router.push('/login'); return }
  carting.value = true
  try {
    await apiAddToCart({ userId: Number(uid), skuId: sku.value!.id!, skuName: sku.value!.skuName, skuImage: sku.value!.image, price: sku.value!.price, quantity: quantity.value })
    ElMessage.success('已加入购物车')
  } catch { ElMessage.error('添加失败') }
  finally { carting.value = false }
}

async function buyNow() {
  const uid = String(userStore.userId || 0)
  if (!uid) { ElMessage.warning('请先登录'); router.push('/login'); return }
  buying.value = true
  try {
    const res = await request.post('/order', {
      remark: '', addressSnapshot: '',
      items: [{ skuId: sku.value!.id, skuName: sku.value!.skuName, quantity: quantity.value, price: sku.value!.price, skuImage: sku.value!.image }]
    })
    result.value = { success: true, message: '下单成功！', orderSn: res.data.orderSn }
    resultVisible.value = true
  } catch (e: any) {
    result.value = { success: false, message: e.response?.data?.msg || '下单失败', orderSn: '' }
    resultVisible.value = true
  } finally { buying.value = false }
}
</script>
