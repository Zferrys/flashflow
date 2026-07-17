<template>
  <div>
    <h3>订单管理</h3>

    <el-card>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="orderSn" label="订单号" width="180" />
        <el-table-column prop="userId" label="用户ID" width="70" />
        <el-table-column prop="totalAmount" label="总金额" width="90">
          <template #default="{ row }"> ¥{{ row.totalAmount }} </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payType" label="支付方式" width="80">
          <template #default="{ row }">{{ row.payType === 1 ? '支付宝' : row.payType === 2 ? '微信' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="paymentTime" label="支付时间" width="150" />
        <el-table-column prop="createTime" label="创建时间" width="150" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 16px"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="700px">
      <template v-if="currentOrder">
        <el-descriptions :column="2" border style="margin-bottom: 16px">
          <el-descriptions-item label="订单号" :span="2">{{ currentOrder.orderSn }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ currentOrder.userId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(currentOrder.status)" size="small">{{ statusLabel(currentOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">¥{{ currentOrder.payAmount }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ currentOrder.payType === 1 ? '支付宝' : currentOrder.payType === 2 ? '微信' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ currentOrder.paymentTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-bottom: 8px">商品明细</h4>
        <el-table :data="orderItems" border size="small" v-loading="itemsLoading" style="margin-bottom: 16px">
          <el-table-column prop="skuName" label="商品名称" min-width="160" />
          <el-table-column prop="skuPrice" label="单价" width="80">
            <template #default="{ row }">¥{{ row.skuPrice }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="60" />
          <el-table-column prop="subTotal" label="小计" width="80">
            <template #default="{ row }">¥{{ row.subTotal }}</template>
          </el-table-column>
        </el-table>

        <h4 style="margin-bottom: 8px">状态变更记录</h4>
        <el-table :data="orderEvents" border size="small" v-loading="eventsLoading">
          <el-table-column prop="fromStatus" label="原状态" width="100">
            <template #default="{ row }">{{ row.fromStatus != null ? statusLabel(row.fromStatus) : '-' }}</template>
          </el-table-column>
          <el-table-column prop="toStatus" label="目标状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.toStatus)" size="small">{{ statusLabel(row.toStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="eventTime" label="变更时间" width="160" />
          <el-table-column prop="operatorType" label="操作人" width="80">
            <template #default="{ row }">{{ ['系统', '用户', '管理员'][row.operatorType] || '系统' }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrderPage } from '@/api/order'
import request from '@/api/request'

const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const detailVisible = ref(false)
const currentOrder = ref<any>(null)
const orderItems = ref<any[]>([])
const orderEvents = ref<any[]>([])
const itemsLoading = ref(false)
const eventsLoading = ref(false)

function statusType(s: number) {
  return ['warning', 'success', 'primary', 'primary', 'success', 'danger', 'warning', 'info'][s] as any
}
function statusLabel(s: number) {
  return ['待支付', '已支付', '已发货', '已收货', '已完成', '已取消', '退款中', '已退款'][s] || '未知'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getOrderPage({ page: page.value, size: size.value })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function showDetail(row: any) {
  currentOrder.value = row
  detailVisible.value = true

  itemsLoading.value = true
  eventsLoading.value = true
  try {
    const [itemsRes, eventsRes] = await Promise.all([
      request.get(`/order/${row.id}/items`),
      request.get(`/order/${row.id}/events`),
    ])
    orderItems.value = itemsRes.data || []
    orderEvents.value = eventsRes.data || []
  } catch {
    orderItems.value = []
    orderEvents.value = []
  } finally {
    itemsLoading.value = false
    eventsLoading.value = false
  }
}

onMounted(fetchData)
</script>
