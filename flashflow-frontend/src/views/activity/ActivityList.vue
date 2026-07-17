<template>
  <div>
    <h3>活动管理</h3>

    <el-card style="margin-bottom: 16px">
      <el-button type="primary" @click="openCreateDialog">创建活动</el-button>
    </el-card>

    <el-card>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="活动名称" min-width="140" />
        <el-table-column label="商品" min-width="240">
          <template #default="{ row }">
            <div v-if="skuMap[row.id] && skuMap[row.id].length" style="display: flex; gap: 6px; flex-wrap: wrap">
              <el-popover
                v-for="s in skuMap[row.id].slice(0, 3)"
                :key="s.skuId"
                placement="top"
                :width="220"
                trigger="hover"
              >
                <template #reference>
                  <el-tag
                    size="small"
                    style="cursor: pointer"
                    @click="editSku(row, s)"
                  >
                    <el-image
                      :src="s.skuImage"
                      style="width: 18px; height: 18px; vertical-align: middle; margin-right: 4px; border-radius: 2px"
                      fit="cover"
                    >
                      <template #error><span style="font-size:8px">📦</span></template>
                    </el-image>
                    {{ s.skuName }}
                  </el-tag>
                </template>
                <div style="text-align: center">
                  <el-image :src="s.skuImage" style="width: 180px; height: 180px; border-radius: 4px" fit="cover">
                    <template #error><div style="padding:20px;color:#999">暂无图片</div></template>
                  </el-image>
                  <div style="margin-top: 8px; font-weight: bold">{{ s.skuName }}</div>
                  <div><span style="color:#f56c6c;font-size:18px;font-weight:bold">¥{{ s.activityPrice }}</span>
                    <span style="color:#ccc;text-decoration:line-through;margin-left:6px">¥{{ s.originalPrice }}</span>
                  </div>
                  <div style="color:#999;font-size:12px">库存 {{ s.stockLimit }} | 限购 {{ s.perUserLimit }}</div>
                </div>
              </el-popover>
              <el-tag size="small" v-if="skuMap[row.id].length > 3">+{{ skuMap[row.id].length - 3 }}</el-tag>
              <el-button size="small" circle text @click="addSkuDialog(row)">+</el-button>
            </div>
            <div v-else style="display:flex;gap:4px;align-items:center">
              <span style="color:#ccc;font-size:12px">未配置商品</span>
              <el-button size="small" text @click="addSkuDialog(row)">添加</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="activityType" label="类型" width="70">
          <template #default="{ row }">
            <el-tag size="small">{{ typeMap[row.activityType] || row.activityType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="85">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" min-width="240">
          <template #default="{ row }">{{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="goSeckill(row.id)" v-if="row.status === 2">
              去秒杀
            </el-button>
            <!-- 草稿(0): 可编辑+发布+删除 -->
            <el-button size="small" @click="openEditDialog(row)" v-if="row.status === 0">
              编辑
            </el-button>
            <el-button size="small" type="success" @click="handlePublish(row)" v-if="row.status === 0 || row.status === 1">
              {{ row.status === 1 ? '重新发布' : '发布' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" v-if="row.status === 0">
              删除
            </el-button>
            <!-- 进行中(2) 或 待预热(1): 可关闭 -->
            <el-button size="small" type="warning" @click="handleClose(row)" v-if="row.status === 1 || row.status === 2">
              关闭
            </el-button>
            <el-button size="small" @click="addSkuDialog(row)">
              商品
            </el-button>
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

    <!-- 创建/修改活动对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingActivity ? '修改活动' : '创建活动'" width="500px">
      <el-form :model="formData" label-width="100px" ref="activityFormRef">
        <el-form-item label="活动名称" required prop="name">
          <el-input v-model="formData.name" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="活动类型" required>
          <el-select v-model="formData.activityType" style="width: 100%">
            <el-option label="秒杀" value="FLASH_SALE" />
            <el-option label="预售" value="PRE_SALE" />
            <el-option label="拼团" value="GROUP_BUY" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker v-model="formData.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker v-model="formData.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveActivity" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑商品对话框 -->
    <el-dialog v-model="skuDialogVisible" :title="editingSkuId ? '编辑商品' : '添加商品'" width="520px">
      <el-form :model="skuForm" label-width="100px">
        <el-form-item label="商品名称" required>
          <el-input v-model="skuForm.skuName" placeholder="例: iPhone 15 Pro 256GB" />
        </el-form-item>
        <el-form-item label="活动图片" required>
          <div style="display:flex;gap:8px;align-items:flex-start;flex-wrap:wrap">
            <ImagePicker :modelValue="skuForm.skuImage" @update:modelValue="onSkuImagePicked" />
            <el-upload
              :show-file-list="false"
              :http-request="handleSkuUpload"
              accept="image/*"
            >
              <el-button size="small" type="primary">
                <el-icon><Upload /></el-icon> 上传新图片
              </el-button>
            </el-upload>
          </div>
          <el-image v-if="skuForm.skuImage" :src="skuForm.skuImage" style="width:100px;height:100px;margin-top:6px;border-radius:6px;border:1px solid #eee" fit="cover">
            <template #error><span style="font-size:12px;color:#999">图片加载失败</span></template>
          </el-image>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="原价" required>
              <el-input-number v-model="skuForm.originalPrice" :precision="2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="秒杀价" required>
              <el-input-number v-model="skuForm.activityPrice" :precision="2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="库存" required>
              <el-input-number v-model="skuForm.stockLimit" :min="1" :max="99999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="限购" required>
              <el-input-number v-model="skuForm.perUserLimit" :min="1" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="SKU ID">
          <el-input v-model="skuForm.skuId" placeholder="留空自动生成" disabled />
          <div style="color:#999;font-size:12px">系统自动分配</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="skuDialogVisible = false">取消</el-button>
        <el-button v-if="editingSkuId" type="danger" @click="handleDeleteSku" :loading="deleting">删除</el-button>
        <el-button type="primary" @click="handleSaveSku" :loading="skuSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getActivityPage, getActivitySkuList, createActivity, updateActivity,
  publishActivity, closeActivity, addSku, updateSku, deleteSku,
  type PromotionActivity, type PromotionSku
} from '@/api/activity'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import ImagePicker from '@/components/ImagePicker.vue'
import uploadRequest from '@/api/upload'

const router = useRouter()
const list = ref<PromotionActivity[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

// 活动表单
const dialogVisible = ref(false)
const editingActivity = ref(false)
const formData = ref<any>({})
const saving = ref(false)

function openCreateDialog() {
  editingActivity.value = false
  const now = new Date()
  const start = new Date(now.getTime() + 3600000) // 1小时后
  const end = new Date(now.getTime() + 4 * 3600000) // 4小时后
  const fmt = (d: Date) => {
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }
  formData.value = {
    activityType: 'FLASH_SALE',
    startTime: fmt(start),
    endTime: fmt(end),
  }
  dialogVisible.value = true
}

// SKU 表单
const skuDialogVisible = ref(false)
const skuForm = ref<any>({})
const skuSaving = ref(false)
const deleting = ref(false)
const editingSkuId = ref(0)
const currentActivityForSku = ref<PromotionActivity | null>(null)

// SKU 数据缓存
const skuMap = ref<Record<number, PromotionSku[]>>({})

const typeMap: Record<string, string> = {
  FLASH_SALE: '秒杀',
  PRE_SALE: '预售',
  GROUP_BUY: '拼团',
}

function formatTime(t: string) {
  return t?.replace('T', ' ').substring(0, 16) || ''
}

function statusType(s: number) {
  return ['info', 'warning', 'success', 'danger', 'danger'][s] as any
}
function statusLabel(s: number) {
  return ['草稿', '待预热', '进行中', '已结束', '已关闭'][s] || '未知'
}

function goSeckill(activityId: number) {
  const skus = skuMap.value[activityId]
  if (skus && skus.length) {
    router.push(`/seckill/${activityId}/${skus[0].skuId}`)
  } else {
    ElMessage.info('请先为该活动添加商品')
  }
}

// 加载 SKU 列表
async function fetchSkuList(activityId: number) {
  try {
    const res = await getActivitySkuList(activityId)
    skuMap.value[activityId] = res.data || []
  } catch {
    skuMap.value[activityId] = []
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getActivityPage({ page: page.value, size: size.value })
    list.value = res.data.records
    total.value = res.data.total
    list.value.forEach(a => { if (a.id) fetchSkuList(a.id) })
  } finally {
    loading.value = false
  }
}

// 保存活动（创建/修改）
async function handleSaveActivity() {
  saving.value = true
  try {
    if (editingActivity.value && formData.value.id) {
      await updateActivity(formData.value)
      ElMessage.success('修改成功')
    } else {
      await createActivity(formData.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

function openEditDialog(row: PromotionActivity) {
  editingActivity.value = true
  formData.value = { ...row }
  dialogVisible.value = true
}

function handlePublish(row: PromotionActivity) {
  ElMessageBox.confirm('发布后将预热 Redis，确认？', '提示').then(async () => {
    await publishActivity(row.id!)
    ElMessage.success('发布成功')
    fetchData()
  })
}

function handleClose(row: PromotionActivity) {
  ElMessageBox.confirm('确认关闭该活动？', '提示').then(async () => {
    await closeActivity(row.id!)
    ElMessage.success('已关闭')
    fetchData()
  })
}

function handleDelete(row: PromotionActivity) {
  ElMessageBox.confirm('删除后不可恢复，确认？', '警告', { type: 'warning' }).then(async () => {
    await request.delete('/promotion/activity/' + row.id)
    ElMessage.success('已删除')
    fetchData()
  })
}

// SKU 管理
function addSkuDialog(row: PromotionActivity) {
  currentActivityForSku.value = row
  editingSkuId.value = 0
  skuForm.value = {
    activityId: row.id,
    skuId: Date.now() % 10000 + 1000, // 生成临时 SKU ID
    skuName: '',
    skuImage: '/assets/products/iphone15.jpg',
    originalPrice: 0,
    activityPrice: 0,
    stockLimit: 100,
    perUserLimit: 1,
    soldCount: 0,
    sort: 1,
  }
  skuDialogVisible.value = true
}

function editSku(row: PromotionActivity, sku: PromotionSku) {
  currentActivityForSku.value = row
  editingSkuId.value = sku.id!
  skuForm.value = { ...sku }
  skuDialogVisible.value = true
}

async function handleSaveSku() {
  const actId = currentActivityForSku.value?.id
  const editSkuId = editingSkuId.value
  if (!actId) return
  skuSaving.value = true
  try {
    if (editSkuId) {
      await updateSku(actId, { ...skuForm.value, id: editSkuId, activityId: actId })
      ElMessage.success('修改成功')
    } else {
      await addSku(actId, skuForm.value)
      ElMessage.success('添加成功')
    }
    skuDialogVisible.value = false
    fetchSkuList(actId)
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    skuSaving.value = false
  }
}

async function handleDeleteSku() {
  const actId = currentActivityForSku.value?.id
  const skuId = editingSkuId.value
  if (!actId || !skuId) return
  ElMessageBox.confirm('确认删除该商品？', '提示').then(async () => {
    deleting.value = true
    try {
      await deleteSku(actId, skuId)
      ElMessage.success('删除成功')
      skuDialogVisible.value = false
      fetchSkuList(actId)
    } finally {
      deleting.value = false
    }
  })
}

function onSkuImagePicked(url: string) {
  skuForm.value.skuImage = url
}
async function handleSkuUpload(opts: any) {
  try {
    const url = await uploadRequest(opts.file)
    if (url) { skuForm.value.skuImage = url; ElMessage.success('图片上传成功') }
  } catch { ElMessage.error('图片上传失败') }
}

onMounted(fetchData)
</script>
