<template>
  <div>
    <div style="max-width:800px;margin:24px auto;padding:0 16px">
      <!-- 状态切换 -->
      <el-card style="margin-bottom:16px">
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <el-button :type="statusFilter === null ? 'primary' : 'default'" size="small" @click="statusFilter=null;fetchOrders()">全部</el-button>
          <el-button :type="statusFilter === 0 ? 'primary' : 'default'" size="small" @click="statusFilter=0;fetchOrders()">待支付</el-button>
          <el-button :type="statusFilter === 1 ? 'primary' : 'default'" size="small" @click="statusFilter=1;fetchOrders()">已支付</el-button>
          <el-button :type="statusFilter === 2 ? 'primary' : 'default'" size="small" @click="statusFilter=2;fetchOrders()">已发货</el-button>
          <el-button :type="statusFilter === 6 ? 'primary' : 'default'" size="small" @click="statusFilter=6;fetchOrders()">退款中</el-button>
          <el-button :type="statusFilter === 7 ? 'primary' : 'default'" size="small" @click="statusFilter=7;fetchOrders()">已退款</el-button>
        </div>
      </el-card>

      <!-- 订单列表 -->
      <div v-loading="loading">
        <el-card v-for="order in list" :key="order.id" style="margin-bottom:12px" shadow="hover">
          <div style="display:flex;justify-content:space-between;margin-bottom:8px">
            <span style="color:#999;font-size:13px">订单号: {{ order.orderSn }}</span>
            <el-tag :type="statusType(order.status)" size="small">{{ statusLabel(order.status) }}</el-tag>
          </div>
          <div v-if="orderItems[order.id]?.length" style="margin-bottom:12px">
            <div v-for="item in orderItems[order.id]" :key="item.id" style="display:flex;gap:12px;padding:8px 0;align-items:center;border-bottom:1px solid #f5f5f5">
              <el-image :src="item.skuImage || ''" style="width:64px;height:64px;border-radius:4px;flex-shrink:0" fit="cover" lazy>
                <template #error><div style="width:64px;height:64px;background:#f5f7fa;border-radius:4px"></div></template>
              </el-image>
              <div style="flex:1">
                <div style="font-weight:bold">{{ item.skuName }}</div>
                <div style="color:#999;font-size:12px">x{{ item.quantity }}</div>
              </div>
              <div style="color:#f56c6c;font-weight:bold">¥{{ item.skuPrice }}</div>
            </div>
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center;padding-top:8px;border-top:1px solid #eee">
            <span style="font-weight:bold">合计: <span style="color:#f56c6c;font-size:18px">¥{{ order.payAmount }}</span></span>
            <div style="display:flex;gap:8px;flex-wrap:wrap">
              <el-button size="small" type="primary" @click="router.push(`/payment/${order.orderSn}`)" v-if="order.status === 0">去支付</el-button>
              <el-button size="small" @click="handleCancel(order)" v-if="order.status === 0">取消订单</el-button>
              <el-button size="small" type="success" @click="handleDeliver(order.id!)" v-if="order.status === 2">确认收货</el-button>
              <el-button size="small" type="danger" plain @click="router.push(`/refund/${order.orderSn}`)" v-if="order.status === 1 || order.status === 2">申请退款</el-button>
              <el-button size="small" type="warning" plain @click="router.push(`/refund/${order.orderSn}`)" v-if="order.status === 6">查看退款进度</el-button>
              <el-button size="small" text @click="showDetail(order)">详情</el-button>
            </div>
          </div>
        </el-card>

        <el-empty v-if="!loading && list.length === 0" description="暂无订单" />

        <!-- 分页 -->
        <div v-if="total > 0" style="display:flex;justify-content:center;margin-top:20px">
          <el-pagination
            v-model:current-page="page"
            :page-size="size"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="fetchOrders"
          />
        </div>
      </div>
    </div>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <template v-if="currentOrder">
        <el-descriptions :column="2" border style="margin-bottom:16px">
          <el-descriptions-item label="订单号" :span="2">{{ currentOrder.orderSn }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(currentOrder.status)" size="small">{{ statusLabel(currentOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ currentOrder.paymentTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin:0 0 8px">状态变更</h4>
        <el-timeline v-if="events.length">
          <el-timeline-item v-for="e in events" :key="e.id" :timestamp="e.eventTime">
            {{ statusLabel(e.toStatus) }}
            <span v-if="e.fromStatus != null">(从 {{ statusLabel(e.fromStatus) }})</span>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getOrderPage, getOrderItems, getOrderEvents, confirmDeliver, cancelOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const list = ref<any[]>([])
const orderItems = ref<Record<number, any[]>>({})
const loading = ref(false)
const statusFilter = ref<number | null>(null)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const detailVisible = ref(false)
const currentOrder = ref<any>(null)
const events = ref<any[]>([])

function statusType(s: number) {
  return ['danger', 'warning', 'success', '', 'info', 'info', 'warning', 'danger'][s] as any
}
function statusLabel(s: number) {
  return ['待支付', '已支付', '已发货', '已收货', '已完成', '已取消', '退款中', '已退款'][s] || '未知'
}

async function fetchOrders() {
  loading.value = true
  try {
    if (!userStore.token) return
    const params: any = { page: page.value, size: size.value }
    if (statusFilter.value !== null) params.status = statusFilter.value
    const res = await getOrderPage(params)
    list.value = res.data.records || []
    total.value = res.data.total || 0
    // Load items for each order
    list.value.forEach(o => {
      if (o.id) {
        getOrderItems(o.id).then(r => { orderItems.value[o.id] = r.data || [] })
      }
    })
  } finally { loading.value = false }
}

async function handleDeliver(orderId: number) {
  try {
    await confirmDeliver(orderId)
    ElMessage.success('确认收货成功')
    fetchOrders()
  } catch { ElMessage.error('操作失败') }
}

async function handleCancel(order: any) {
  try {
    await ElMessageBox.confirm('确定要取消此订单吗？', '取消订单', {
      confirmButtonText: '确定取消', cancelButtonText: '再想想', type: 'warning',
    })
    await cancelOrder(order.id!)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch { /* cancelled */ }
}

async function showDetail(order: any) {
  currentOrder.value = order
  detailVisible.value = true
  try {
    const res = await getOrderEvents(order.id)
    events.value = res.data || []
  } catch { events.value = [] }
}

onMounted(() => {
  if (!userStore.token) {
    router.push('/login')
    return
  }
  fetchOrders()
})
</script>
