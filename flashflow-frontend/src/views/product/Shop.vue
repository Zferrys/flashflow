<template>
  <div>
    <div class="ff-container" style="margin:24px auto">
      <!-- ═══════════ 分类标签 ═══════════ -->
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:20px">
        <span class="ff-cat-pill" :class="{ active: activeCat === 0 }" @click="activeCat=0;page=1">全部</span>
        <span v-for="cat in categories" :key="cat.id" class="ff-cat-pill" :class="{ active: activeCat === cat.id }" @click="activeCat=cat.id;page=1">
          <span style="display:inline-flex;align-items:center;justify-content:center;width:18px;height:18px;border-radius:4px;background:rgba(59,130,246,0.15);font-size:10px;font-weight:700;color:#3b82f6;margin-right:4px">{{ cat.icon }}</span>{{ cat.name }}
        </span>
      </div>

      <!-- ═══════════ 排序 + 搜索 ═══════════ -->
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;flex-wrap:wrap;gap:10px">
        <div style="display:flex;align-items:center;gap:6px">
          <span style="font-size:13px;color:var(--ff-text-muted)">{{ filteredList.length }} 件商品</span>
          <el-divider direction="vertical" />
          <el-button size="small" :type="sortBy === 'default' ? 'primary' : ''" text @click="sortBy='default'">默认</el-button>
          <el-button size="small" :type="sortBy === 'price-asc' ? 'primary' : ''" text @click="sortBy='price-asc'">价格↑</el-button>
          <el-button size="small" :type="sortBy === 'price-desc' ? 'primary' : ''" text @click="sortBy='price-desc'">价格↓</el-button>
        </div>
        <el-input v-model="keyword" placeholder="搜索商品..." :prefix-icon="Search" clearable style="width:220px" size="default" @input="page=1" />
      </div>

      <!-- ═══════════ 骨架屏 ═══════════ -->
      <el-row :gutter="14" v-if="loading">
        <el-col v-for="i in 8" :key="i" :xs="12" :sm="8" :md="6" style="margin-bottom:14px">
          <div class="ff-product-card">
            <div class="img-wrap"><div class="ff-skeleton" style="width:100%;height:100%"></div></div>
            <div class="info">
              <div class="ff-skeleton" style="height:16px;width:70%"></div>
              <div class="ff-skeleton" style="height:12px;width:40%;margin-top:6px"></div>
              <div class="ff-skeleton" style="height:20px;width:35%;margin-top:6px"></div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- ═══════════ 商品网格 ═══════════ -->
      <el-row :gutter="14" v-if="!loading">
        <el-col v-for="spu in paginatedList" :key="spu.id" :xs="12" :sm="8" :md="6" style="margin-bottom:14px">
          <div class="ff-product-card" @click="goShop(spu)">
            <div class="img-wrap">
              <img :src="spu.mainImage" :alt="spu.spuName" loading="lazy" />
              <div v-if="minPriceNum(spu) < 5000" class="badge">优惠</div>
            </div>
            <div class="info">
              <div class="name">{{ spu.spuName }}</div>
              <div class="desc">{{ formatCat(spu.categoryId) }} | {{ spu.description || spu.spuName }}</div>
              <div class="price">¥{{ minPrice(spu) }}<s v-if="minPriceNum(spu) < 8000">¥{{ Math.round(minPriceNum(spu) * 1.35) }}</s></div>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && paginatedList.length === 0" description="没有找到匹配的商品" style="padding:60px 0" />

      <!-- ═══════════ 分页 ═══════════ -->
      <div style="display:flex;justify-content:center;margin-top:24px" v-if="totalPages > 1">
        <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total"
          layout="prev, pager, next, total" background size="default" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.el-divider--vertical { height: 14px; margin: 0 4px; }
</style>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getActiveSpus, getSkuList, type ProductSpu, type ProductSku } from '@/api/product'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const spuList = ref<ProductSpu[]>([])
const skuCache = ref<Record<number, ProductSku[]>>({})
const loading = ref(true)
const keyword = ref('')
const activeCat = ref(0)
const sortBy = ref('default')
const page = ref(1)
const pageSize = 20

const categories = [
  { id: 1, name: '手机', icon: 'M' },
  { id: 2, name: '笔记本', icon: 'L' },
  { id: 3, name: '音频', icon: 'A' },
  { id: 4, name: '手表', icon: 'W' },
  { id: 5, name: '平板', icon: 'T' },
  { id: 6, name: '游戏', icon: 'G' },
  { id: 7, name: '配件', icon: 'Ac' },
]

const formatCat = (id?: number) => categories.find(c => c.id === id)?.name || ''

// Filter
const filteredList = computed(() => {
  let list = spuList.value
  if (activeCat.value > 0) list = list.filter(s => s.categoryId === activeCat.value)
  if (keyword.value) {
    const k = keyword.value.toLowerCase()
    list = list.filter(s => s.spuName?.toLowerCase().includes(k))
  }
  // Sort
  if (sortBy.value === 'price-asc') {
    list = [...list].sort((a, b) => minPriceNum(a) - minPriceNum(b))
  } else if (sortBy.value === 'price-desc') {
    list = [...list].sort((a, b) => minPriceNum(b) - minPriceNum(a))
  }
  return list
})

const total = computed(() => filteredList.value.length)
const totalPages = computed(() => Math.ceil(total.value / pageSize))
const paginatedList = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredList.value.slice(start, start + pageSize)
})

function minPrice(spu: ProductSpu) {
  const skus = skuCache.value[spu.id!]
  if (!skus?.length) return '—'
  return Math.min(...skus.filter(s => s.status === 1).map(s => s.price))
}
function minPriceNum(spu: ProductSpu) {
  const skus = skuCache.value[spu.id!]
  if (!skus?.length) return Infinity
  return Math.min(...skus.filter(s => s.status === 1).map(s => s.price))
}

async function goShop(spu: ProductSpu) {
  const skus = skuCache.value[spu.id!]
  if (skus?.length) router.push(`/shop/${skus[0].id}`)
}

onMounted(async () => {
  try {
    const res = await getActiveSpus()
    spuList.value = res.data || []
    await Promise.all(spuList.value.map(spu => {
      if (!spu.id) return
      return getSkuList(spu.id).then(r => { skuCache.value[spu.id!] = r.data || [] })
    }))
  } finally { loading.value = false }
})
</script>
