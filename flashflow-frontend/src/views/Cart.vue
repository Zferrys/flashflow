<template>
  <div>
    <div class="ff-container" style="margin:24px auto">
      <div class="ff-section-header">
        <div class="title"><span class="icon"></span>购物车</div>
        <el-button text @click="router.push('/shop')">继续购物 →</el-button>
      </div>

      <div v-loading="loading">
        <div v-if="list.length === 0" style="text-align:center;padding:60px 0">
          <el-empty description="购物车是空的">
            <el-button type="primary" @click="router.push('/shop')">去逛逛</el-button>
          </el-empty>
        </div>

        <div v-else class="cart-layout">
          <div class="cart-items">
            <!-- 全选菜单栏 -->
            <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid #ebeef5;margin-bottom:8px">
              <el-checkbox v-model="allChecked" @change="toggleAll" :indeterminate="checkedTotal > 0 && checkedTotal < list.length">全选 ({{ checkedTotal }}/{{ list.length }})</el-checkbox>
              <el-button text type="danger" size="small" @click="clearChecked" :disabled="checkedTotal === 0">删除选中</el-button>
            </div>
            <div v-for="item in list" :key="item.id" class="cart-item">
              <el-checkbox v-model="item._checked" @change="toggle(item)" />
              <el-image :src="item.skuImage" style="width:80px;height:80px;border-radius:8px;flex-shrink:0" fit="cover" lazy>
                <template #error><div style="width:80px;height:80px;background:#f3f4f6;border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:24px">📦</div></template>
              </el-image>

              <div style="flex:1;min-width:0">
                <div style="font-weight:600;font-size:14px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ item.skuName }}</div>
                <div style="font-size:12px;color:var(--ff-text-muted);margin:2px 0">单价 ¥{{ item.price }}</div>
                <div style="font-size:18px;color:var(--ff-danger);font-weight:700">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
              </div>

              <div style="display:flex;align-items:center;gap:8px">
                <el-input-number v-model="item.quantity" :min="1" :max="99" size="small" controls-position="right" style="width:100px" @change="(v: number) => updateQty(item.id!, v)" />
                <el-button text type="danger" size="small" @click="remove(item.id!)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>

            <div style="display:flex;justify-content:space-between;align-items:center;padding:12px 0">
              <div>
                已选 <span style="color:var(--ff-danger);font-weight:700;font-size:16px">{{ checkedTotal }}</span> 件
                <el-button text type="danger" size="small" @click="clearChecked" style="margin-left:8px">清空已选</el-button>
              </div>
            </div>
          </div>

          <div class="cart-summary">
            <div style="background:var(--ff-bg-card);border-radius:var(--ff-radius-lg);padding:24px;box-shadow:var(--ff-shadow-sm);position:sticky;top:80px">
              <h4 style="margin:0 0 20px;font-size:16px;font-weight:700">订单摘要</h4>
              <div style="display:flex;justify-content:space-between;margin-bottom:8px;font-size:14px">
                <span style="color:var(--ff-text-secondary)">商品 ({{ checkedTotal }} 件)</span>
                <span style="font-weight:600">¥{{ totalAmount.toFixed(2) }}</span>
              </div>
              <div style="display:flex;justify-content:space-between;margin-bottom:16px;font-size:14px">
                <span style="color:var(--ff-text-secondary)">运费</span>
                <span style="color:var(--ff-success);font-weight:500">免运费</span>
              </div>
              <el-divider />
              <div style="display:flex;justify-content:space-between;align-items:baseline;margin-bottom:20px">
                <span style="font-size:14px;color:var(--ff-text-secondary)">应付</span>
                <span style="font-size:28px;font-weight:800;color:var(--ff-danger)">¥{{ totalAmount.toFixed(2) }}</span>
              </div>
              <el-button type="danger" size="large" class="checkout-btn" :disabled="checkedTotal === 0" :loading="checking" @click="checkout">
                结算 ({{ checkedTotal }})
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-layout { display: flex; gap: 24px; align-items: flex-start; flex-wrap: wrap; }
.cart-items { flex: 1; min-width: 300px; display: flex; flex-direction: column; gap: 10px; }
.cart-item {
  display: flex; gap: 14px; align-items: center;
  background: var(--ff-bg-card); padding: 16px;
  border-radius: var(--ff-radius); border: 1px solid var(--ff-border-light);
}
.cart-summary { width: 320px; flex-shrink: 0; }
.checkout-btn { width: 100%; height: 48px; font-size: 16px; font-weight: 700; border-radius: var(--ff-radius) !important; }
@media (max-width: 768px) { .cart-summary { width: 100%; } }
</style>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCart, updateCartQuantity, removeFromCart, clearCheckedCart, toggleChecked, getAddresses, type CartItem } from '@/api/cart'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const checking = ref(false)
const list = ref<(CartItem & { _checked: boolean })[]>([])
const allChecked = ref(false)

const checkedTotal = computed(() => list.value.filter(i => i._checked).length)
const totalAmount = computed(() => list.value.filter(i => i._checked).reduce((s, i) => s + i.price * i.quantity, 0))

// 全选/取消全选
function toggleAll() {
  const newVal = !allChecked.value
  allChecked.value = newVal
  list.value.forEach(i => { i._checked = newVal; toggle(i) })
}

async function fetchCart() {
  const uid = userStore.userId
  if (!uid) return
  loading.value = true
  try {
    const res = await getCart()
    list.value = (res.data || []).map((i: CartItem) => ({ ...i, _checked: i.checked === 1 }))
  } finally { loading.value = false }
}

async function toggle(item: CartItem & { _checked: boolean }) {
  await toggleChecked(item.id!, item._checked ? 1 : 0)
}

async function updateQty(id: number, qty: number) {
  await updateCartQuantity(id, qty)
}

async function remove(id: number) {
  try {
    await ElMessageBox.confirm('确定要移除吗？', '提示', { confirmButtonText: '移除', type: 'warning' })
    await removeFromCart(id)
    list.value = list.value.filter(i => i.id !== id)
    ElMessage.success('已移除')
  } catch { /* cancelled */ }
}

async function clearChecked() {
  const uid = userStore.userId || 0
  if (!uid) return
  await clearCheckedCart()
  list.value = list.value.filter(i => !i._checked)
}

async function checkout() {
  checking.value = true
  const uid = userStore.userId || 0
  if (!uid) { router.push('/login'); checking.value = false; return }
  const checked = list.value.filter(i => i._checked)
  if (!checked.length) { checking.value = false; return }

  // Get address
  let addressSnapshot = ''
  try {
    const addrRes = await getAddresses()
    const addrs = addrRes.data || []
    if (addrs.length > 0) {
      const d = addrs.find((a: any) => a.isDefault) || addrs[0]
      addressSnapshot = `${d.province}${d.city}${d.district} ${d.detailAddress} (${d.receiverName} ${d.receiverPhone})`
    }
  } catch {}

  const total = checked.reduce((s, i) => s + i.price * i.quantity, 0)
  try {
    await ElMessageBox.confirm(
      `共 ${checked.length} 件商品，合计 ¥${total}\n收货：${addressSnapshot || '未设置'}\n确认下单？`,
      '订单确认', { confirmButtonText: '确认下单', cancelButtonText: '取消', type: 'info' }
    )
  } catch { checking.value = false; return }

  try {
    const res = await request.post('/order', {
      addressSnapshot,
      items: checked.map(i => ({ skuId: i.skuId, skuName: i.skuName, quantity: i.quantity, price: i.price, skuImage: i.skuImage }))
    })
    await clearCheckedCart()
    router.push(`/payment/${res.data.orderSn}`)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || '下单失败')
  } finally { checking.value = false }
}

onMounted(fetchCart)
</script>
