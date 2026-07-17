<template>
  <div>
    <div style="max-width:600px;margin:24px auto;padding:0 16px">
      <el-card>
        <template #header>
          <div style="display:flex;align-items:center;gap:8px">
            <el-button text @click="router.back()">← 返回</el-button>
            <span style="font-weight:600">{{ isRefunding ? '退款进度' : isRefunded ? '退款完成' : '申请退款' }}</span>
          </div>
        </template>

        <div v-loading="loading">
          <div v-if="order" style="margin-bottom:20px">
            <div style="display:flex;gap:12px;align-items:center;padding:12px;background:#f9f9f9;border-radius:8px;margin-bottom:16px">
              <el-image :src="orderImage || ''" style="width:60px;height:60px;border-radius:6px;flex-shrink:0" fit="cover" lazy>
                <template #error><div style="width:60px;height:60px;background:#eee;border-radius:6px"></div></template>
              </el-image>
              <div style="flex:1">
                <div style="font-weight:600;font-size:14px">{{ order.skuName || '商品' }}</div>
                <div style="color:#999;font-size:12px">订单号: {{ order.orderSn }}</div>
                <div style="color:#f56c6c;font-weight:bold;margin-top:4px">¥{{ order.payAmount }}</div>
              </div>
            </div>

            <!-- 退款审核中 -->
            <div v-if="isRefunding" style="text-align:center;padding:20px 0">
              <el-icon :size="48" color="#e6a23c"><Clock /></el-icon>
              <h3 style="color:#e6a23c;margin:12px 0 4px">退款审核中</h3>
              <p style="color:#909399;font-size:14px">您的退款申请已提交，管理员正在审核中</p>
              <p style="color:#c0c4cc;font-size:13px;margin-top:8px">退款原因：{{ order.cancelReason || '未填写' }}</p>
            </div>

            <!-- 已退款 -->
            <div v-else-if="isRefunded" style="text-align:center;padding:20px 0">
              <el-icon :size="48" color="#67c23a"><CircleCheck /></el-icon>
              <h3 style="color:#67c23a;margin:12px 0 4px">退款已完成</h3>
              <p style="color:#909399;font-size:14px">退款已原路返回至您的支付账户</p>
            </div>

            <!-- 申请退款表单 -->
            <template v-else>
              <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
                <el-form-item prop="reason">
                  <el-input
                    v-model="form.reason"
                    type="textarea"
                    :rows="4"
                    placeholder="请填写退款原因"
                    maxlength="500"
                    show-word-limit
                  />
                </el-form-item>
              </el-form>
              <div style="color:#909399;font-size:13px;margin-bottom:16px;line-height:1.6">
                <div>• 退款申请提交后需管理员审核</div>
                <div>• 审核通过后将原路返回至您的支付账户</div>
              </div>
              <el-button type="danger" style="width:100%;height:44px;font-size:15px;border-radius:8px" :loading="submitting" @click="handleSubmit">
                提交退款申请
              </el-button>
            </template>
          </div>
          <el-empty v-else-if="!loading" description="订单信息不存在" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderBySn, getOrderItems } from '@/api/order'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, CircleCheck } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const orderSn = route.params.orderSn as string

const loading = ref(true)
const submitting = ref(false)
const order = ref<any>(null)
const orderImage = ref('') // 从订单商品明细取首图
const formRef = ref()
const isRefunding = computed(() => order.value?.status === 6)
const isRefunded = computed(() => order.value?.status === 7)

const form = reactive({ reason: '' })
const rules = {
  reason: [
    { required: true, message: '请填写退款原因', trigger: 'blur' },
    { min: 5, message: '退款原因至少5个字', trigger: 'blur' },
  ],
}

onMounted(async () => {
  try {
    if (!orderSn) { ElMessage.error('参数错误'); return }
    const res = await getOrderBySn(orderSn)
    order.value = res.data
    if (!order.value) { ElMessage.error('订单不存在'); return }
    // 只有已支付/已发货/退款中订单可以进入退款页
    if (order.value.status !== 1 && order.value.status !== 2 && order.value.status !== 6) {
      ElMessage.warning('当前订单状态不支持退款')
      router.back()
    }
    // 获取订单商品明细，取首张商品图片
    if (order.value.id) {
      try {
        const itemsRes = await getOrderItems(order.value.id)
        const items = itemsRes.data || []
        if (items.length > 0 && items[0].skuImage) {
          orderImage.value = items[0].skuImage
        }
      } catch { /* 图片加载失败不影响退款流程 */ }
    }
  } catch { ElMessage.error('加载订单信息失败') }
  finally { loading.value = false }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await ElMessageBox.confirm('确认提交退款申请？管理员审核通过后将原路退款。', '退款确认', {
      confirmButtonText: '提交申请', cancelButtonText: '再想想', type: 'warning',
    })
  } catch { return }

  submitting.value = true
  try {
    await request.post(`/order/${order.value.id}/refund-request`, null, {
      params: { reason: form.reason }
    })
    ElMessage.success('退款申请已提交，请等待处理')
    router.push('/order')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '退款申请失败')
  } finally { submitting.value = false }
}
</script>
