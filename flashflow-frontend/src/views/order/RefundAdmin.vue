<template>
  <div>
    <div style="max-width:1000px;margin:24px auto;padding:0 16px">
      <h3>退款审批</h3>

      <el-card v-loading="loading">
        <el-table :data="list" border stripe>
          <el-table-column prop="id" label="订单ID" width="70" />
          <el-table-column prop="orderSn" label="订单号" width="200" />
          <el-table-column label="退款原因" min-width="200">
            <template #default="{ row }">{{ row.cancelReason || '-' }}</template>
          </el-table-column>
          <el-table-column prop="payAmount" label="金额" width="100">
            <template #default="{ row }">¥{{ row.payAmount }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="approve(row)">通过</el-button>
              <el-button size="small" type="danger" @click="reject(row)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && list.length === 0" description="暂无待审批的退款申请" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<any[]>([])
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/order/refund-pending')
    list.value = res.data || []
  } finally { loading.value = false }
}

async function approve(order: any) {
  try {
    await ElMessageBox.confirm(`确认通过订单 ${order.orderSn} 的退款？`, '审批确认', {
      confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'warning',
    })
  } catch { return }
  try {
    await request.post(`/order/${order.id}/refund-approve`)
    ElMessage.success('退款已通过')
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '操作失败')
  }
}

async function reject(order: any) {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝退款', {
      confirmButtonText: '确认拒绝', cancelButtonText: '取消',
      inputPlaceholder: '拒绝原因',
    })
    if (!value) return
    await request.post(`/order/${order.id}/refund-reject`, null, { params: { reason: value } })
    ElMessage.success('退款已拒绝')
    fetchList()
  } catch { /* cancelled */ }
}

onMounted(fetchList)
</script>
