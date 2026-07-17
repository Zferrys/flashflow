<template>
  <div>
    <div style="max-width:600px;margin:24px auto;padding:0 16px">
      <el-card>
        <template #header>
          <div style="display:flex;align-items:center;gap:8px">
            <el-button text @click="router.back()">← 返回</el-button>
            <span style="font-weight:600">申请退款</span>
          </div>
        </template>

        <div v-loading="loading">
          <div v-if="order" style="margin-bottom:20px">
            <div style="display:flex;gap:12px;align-items:center;padding:12px;background:#f9f9f9;border-radius:8px;margin-bottom:16px">
              <el-image :src="order.skuImage || ''" style="width:60px;height:60px;border-radius:6px;flex-shrink:0" fit="cover" lazy>
                <template #error><div style="width:60px;height:60px;background:#eee;border-radius:6px"></div></template>
              </el-image>
              <div style="flex:1">
                <div style="font-weight:600;font-size:14px">{{ order.skuName || '商品' }}</div>
                <div style="color:#999;font-size:12px">订单号: {{ order.orderSn }}</div>
                <div style="color:#f56c6c;font-weight:bold;margin-top:4px">¥{{ order.payAmount }}</div>
              </div>
            </div>

            <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
              <el-form-item prop="reason">
                <el-input
                  v-model="form.reason"
                  type="textarea"
                  :rows="4"
                  placeholder="请填写退款原因，这将帮助客服尽快处理您的请求"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
            </el-form>

            <div style="color:#909399;font-size:13px;margin-bottom:16px;line-height:1.6">
              <div>• 退款将原路返回至您的支付账户</div>
              <div>• 退款处理周期一般为 1-3 个工作日</div>
            </div>

            <el-button type="danger" style="width:100%;height:44px;font-size:15px;border-radius:8px" :loading="submitting" @click="handleSubmit">
              提交退款申请
            </el-button>
          </div>
          <el-empty v-else-if="!loading" description="订单信息不存在" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderBySn } from '@/api/order'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const orderSn = route.params.orderSn as string

const loading = ref(true)
const submitting = ref(false)
const order = ref<any>(null)
const formRef = ref()

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
    // 只有已支付订单可以退款
    if (order.value.status !== 1 && order.value.status !== 2) {
      ElMessage.warning('当前订单状态不支持退款')
      router.back()
    }
  } catch { ElMessage.error('加载订单信息失败') }
  finally { loading.value = false }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await ElMessageBox.confirm('确认提交退款申请？退款金额将原路返回。', '退款确认', {
      confirmButtonText: '确认退款', cancelButtonText: '再想想', type: 'warning',
    })
  } catch { return }

  submitting.value = true
  try {
    await request.post('/payment/refund', {
      orderSn,
      amount: order.value.payAmount,
      reason: form.reason,
    })
    ElMessage.success('退款申请已提交，请等待处理')
    router.push('/order')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '退款申请失败')
  } finally { submitting.value = false }
}
</script>
