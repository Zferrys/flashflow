<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h3 style="margin:0">优惠券管理</h3>
      <el-button type="primary" @click="showDialog()">新增优惠券</el-button>
    </div>

    <el-card>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="券名" min-width="140" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'warning' : 'info'">{{ row.type === 1 ? '满减' : '折扣' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="门槛" width="90">
          <template #default="{ row }">¥{{ row.conditionAmount }}</template>
        </el-table-column>
        <el-table-column label="面额/折扣" width="90">
          <template #default="{ row }">
            {{ row.type === 1 ? '¥' + row.discountAmount : (row.discountRate ? (row.discountRate * 10).toFixed(1) + '折' : '-') }}
          </template>
        </el-table-column>
        <el-table-column label="适用范围" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.scope === 'ALL' ? '全场' : row.scope === 'CATEGORY' ? '分类' : row.scope === 'SKU' ? '单品' : '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="自动发放" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.autoGrant === 'NEW_USER' ? 'success' : 'info'">{{ row.autoGrant === 'NEW_USER' ? '新用户' : row.autoGrant === 'FIRST_ORDER' ? '首单' : '手动' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="剩余/总量" width="90">
          <template #default="{ row }">{{ row.remainCount }}/{{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="每人限领" width="80"><template #default="{ row }">{{ row.perUserLimit }}</template></el-table-column>
        <el-table-column label="有效期" min-width="180">
          <template #default="{ row }">{{ fmt(row.startTime) }} ~ {{ fmt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '有效' : '无效' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="showDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id!)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="editing ? '编辑优惠券' : '新增优惠券'" width="560px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="券名" prop="name">
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减券</el-radio>
            <el-radio :value="2">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="使用门槛" prop="conditionAmount">
          <el-input-number v-model="form.conditionAmount" :min="0" :precision="2" style="width:200px" />
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="优惠金额" prop="discountAmount">
          <el-input-number v-model="form.discountAmount" :min="0.01" :max="9999" :precision="2" style="width:200px" />
          <span style="margin-left:8px;color:#999">元</span>
        </el-form-item>
        <el-form-item v-if="form.type === 2" label="折扣率" prop="discountRate">
          <el-input-number v-model="form.discountRate" :min="0.1" :max="0.99" :precision="2" :step="0.05" style="width:200px" />
          <span style="margin-left:8px;color:#999">{{ form.discountRate ? (form.discountRate * 10).toFixed(1) + '折' : '' }}</span>
        </el-form-item>
        <el-form-item label="适用范围" prop="scope">
          <el-radio-group v-model="form.scope">
            <el-radio value="ALL">全场通用</el-radio>
            <el-radio value="CATEGORY">指定分类</el-radio>
            <el-radio value="SKU">指定商品</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.scope === 'CATEGORY'" label="分类ID">
          <el-input-number v-model="form.scopeValue" :min="1" :max="7" style="width:200px" />
        </el-form-item>
        <el-form-item v-if="form.scope === 'SKU'" label="SKU ID(JSON)">
          <el-input v-model="form.scopeValue" placeholder='如：[7, 17]' maxlength="200" style="width:200px" />
        </el-form-item>
        <el-form-item label="自动发放">
          <el-select v-model="form.autoGrant" style="width:200px">
            <el-option label="手动领取" value="NONE" />
            <el-option label="新用户注册" value="NEW_USER" />
            <el-option label="首单赠送" value="FIRST_ORDER" />
          </el-select>
        </el-form-item>
        <el-form-item label="发行总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" style="width:200px" />
        </el-form-item>
        <el-form-item label="每人限领" prop="perUserLimit">
          <el-input-number v-model="form.perUserLimit" :min="1" :max="10" style="width:200px" />
        </el-form-item>
        <el-form-item label="有效期" prop="dateRange">
          <el-date-picker v-model="form.dateRange" type="datetimerange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="有效" inactive-text="无效" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '@/api/request'

const list = ref<any[]>([])
const loading = ref(false)
const visible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = ref<any>({ type: 1, conditionAmount: 0, discountAmount: 5, discountRate: null, scope: 'ALL', scopeValue: null, autoGrant: 'NONE', totalCount: 100, perUserLimit: 1, status: 1, dateRange: [] })

const rules: FormRules = {
  name: [{ required: true, message: '请输入券名' }],
  type: [{ required: true }],
  totalCount: [{ required: true }],
  dateRange: [{ required: true, message: '请选择有效期', trigger: 'change' }],
}

function fmt(t: string) { return (t || '').replace('T', ' ').substring(0, 16) || '' }

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/promotion/coupon/admin/list')
    list.value = res.data || []
  } catch (e: any) {
    console.error('获取优惠券列表失败:', e)
    ElMessage.error('加载失败，请确认已登录管理员账号')
  } finally { loading.value = false }
}

function showDialog(row?: any) {
  editing.value = !!row
  if (row) {
    form.value = { ...row, dateRange: [row.startTime, row.endTime] }
  } else {
    form.value = { type: 1, conditionAmount: 0, discountAmount: 5, discountRate: null, scope: 'ALL', scopeValue: null, autoGrant: 'NONE', totalCount: 100, perUserLimit: 1, status: 1, dateRange: [] }
  }
  visible.value = true
}

function resetForm() { formRef.value?.resetFields() }

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      ...form.value,
      startTime: form.value.dateRange[0],
      endTime: form.value.dateRange[1],
      remainCount: editing.value ? form.value.remainCount : form.value.totalCount,
      discountRate: form.value.type === 2 ? form.value.discountRate : null,
      scopeValue: form.value.scope === 'ALL' ? null : form.value.scopeValue,
    }
    if (editing.value) {
      await request.put('/promotion/coupon/admin', payload)
      ElMessage.success('修改成功')
    } else {
      await request.post('/promotion/coupon/admin', payload)
      ElMessage.success('创建成功')
    }
    visible.value = false
    fetchList()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除此优惠券？', '提示', { type: 'warning' })
  await request.delete('/promotion/coupon/admin/' + id)
  ElMessage.success('已删除')
  fetchList()
}

onMounted(fetchList)
</script>
