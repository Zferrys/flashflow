<template>
  <div>
    <h3>商品管理</h3>

    <el-card style="margin-bottom: 16px">
      <el-button type="primary" @click="showSpuDialog()">新增商品</el-button>
    </el-card>

    <el-card>
      <el-table :data="spuList" border stripe v-loading="loading" @expand-change="onExpand">
        <el-table-column type="expand" width="30">
          <template #default="{ row }">
            <div v-loading="skuLoading[row.id!]">
              <div v-if="!skuMap[row.id!] || !skuMap[row.id!].length" style="text-align:center;padding:20px;color:#999">
                暂无 SKU
              </div>
              <el-table v-else :data="skuMap[row.id!]" size="small" border>
                <el-table-column prop="skuName" label="SKU名称" min-width="160" />
                <el-table-column label="图片" width="60">
                  <template #default="{ row: s }">
                    <el-image :src="s.image" style="width:36px;height:36px;border-radius:4px" fit="cover">
                      <template #error><span style="font-size:10px">📦</span></template>
                    </el-image>
                  </template>
                </el-table-column>
                <el-table-column prop="price" label="价格" width="100">
                  <template #default="{ row: s }">¥{{ s.price }}</template>
                </el-table-column>
                <el-table-column prop="stock" label="库存" width="70" />
                <el-table-column label="操作" width="160">
                  <template #default="{ row: s }">
                    <el-button size="small" text @click="editSku(row, s)">编辑</el-button>
                    <el-button size="small" text type="danger" @click="removeSku(s.id!)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-button size="small" type="primary" link @click="showSkuDialog(row)" style="margin-top:8px">
                + 添加 SKU
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="50" />
        <el-table-column label="主图" width="60">
          <template #default="{ row }">
            <el-image :src="row.mainImage" style="width:40px;height:40px;border-radius:4px" fit="cover">
              <template #error><span style="font-size:10px">📦</span></template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="spuName" label="商品名称" min-width="180" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="150" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showSpuDialog(row)">编辑</el-button>
            <el-button size="small" @click="editSku(row)">SKU</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
        layout="total, prev, pager, next" style="margin-top:16px" @current-change="fetchData" />
    </el-card>

    <!-- SPU 对话框 -->
    <el-dialog v-model="spuVisible" :title="editingSpu ? '编辑商品' : '新增商品'" width="550px">
      <el-form :model="spuForm" label-width="100px" ref="spuFormRef">
        <el-form-item label="商品名称" required prop="spuName">
          <el-input v-model="spuForm.spuName" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="主图">
          <div class="flex gap-2 flex-wrap items-start">
            <ImagePicker :modelValue="spuForm.mainImage" @update:modelValue="spuForm.mainImage = $event" />
            <el-upload :show-file-list="false" :http-request="handleSpuUpload" accept="image/*">
              <el-button size="small" type="primary"><el-icon><Upload /></el-icon> 上传新图片</el-button>
            </el-upload>
          </div>
          <el-image v-if="spuForm.mainImage" :src="spuForm.mainImage" style="width:120px;height:120px;margin-top:6px;border-radius:6px;border:1px solid #eee" fit="cover">
            <template #error><span style="font-size:12px;color:#999">加载失败</span></template>
          </el-image>
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="spuForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="spuForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="spuVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSpu" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- SKU 对话框 -->
    <el-dialog v-model="skuVisible" :title="editingSkuItem ? '编辑SKU' : '添加SKU'" width="480px">
      <el-form :model="skuForm" label-width="90px">
        <el-form-item label="SKU名称" required>
          <el-input v-model="skuForm.skuName" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="价格" required>
              <el-input-number v-model="skuForm.price" :precision="2" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存" required>
              <el-input-number v-model="skuForm.stock" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="SKU图片">
          <div class="flex gap-2 flex-wrap items-start">
            <ImagePicker :modelValue="skuForm.image" @update:modelValue="skuForm.image = $event" />
            <el-upload :show-file-list="false" :http-request="handleSkuUpload" accept="image/*">
              <el-button size="small" type="primary"><el-icon><Upload /></el-icon> 上传新图片</el-button>
            </el-upload>
          </div>
          <el-image v-if="skuForm.image" :src="skuForm.image" style="width:80px;height:80px;margin-top:4px;border-radius:4px;border:1px solid #eee" fit="cover" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="skuVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSku" :loading="skuSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  getSpuPage, createSpu, updateSpu,
  getSkuList, createSku, updateSku, deleteSku,
  type ProductSpu, type ProductSku
} from '@/api/product'
import { ElMessage, ElMessageBox } from 'element-plus'
import ImagePicker from '@/components/ImagePicker.vue'
import uploadRequest from '@/api/upload'

// SPU
const spuList = ref<ProductSpu[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const spuVisible = ref(false)
const editingSpu = ref(false)
const saving = ref(false)
const spuForm = ref<any>({ spuName: '', mainImage: '', description: '', status: 1 })

// SKU
const skuMap = ref<Record<number, ProductSku[]>>({})
const skuLoading = ref<Record<number, boolean>>({})
const skuVisible = ref(false)
const skuSaving = ref(false)
const editingSkuItem = ref(false)
const skuForm = ref<any>({ spuId: 0, skuName: '', price: 0, stock: 0, image: '' })
const currentSpu = ref<ProductSpu | null>(null)

async function fetchSpuList(spuId: number) {
  try {
    const res = await getSkuList(spuId)
    skuMap.value[spuId] = res.data || []
  } catch { skuMap.value[spuId] = [] }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getSpuPage({ page: page.value, size: size.value })
    spuList.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function onExpand(row: ProductSpu, expanded: boolean[]) {
  if (expanded.length && row.id) fetchSpuList(row.id)
}

function showSpuDialog(row?: ProductSpu) {
  editingSpu.value = !!row
  spuForm.value = row ? { ...row } : { spuName: '', mainImage: '', description: '', status: 1 }
  spuVisible.value = true
}

async function saveSpu() {
  saving.value = true
  try {
    if (editingSpu.value) {
      await updateSpu(spuForm.value)
      ElMessage.success('修改成功')
    } else {
      await createSpu(spuForm.value)
      ElMessage.success('创建成功')
    }
    spuVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

async function showSkuDialog(row: ProductSpu) {
  currentSpu.value = row
  editingSkuItem.value = false
  skuForm.value = { spuId: row.id, skuName: '', price: 0, stock: 0, image: '/assets/products/iphone15-pro.jpg' }
  skuVisible.value = true
}

function editSku(row: ProductSpu, sku?: ProductSku) {
  currentSpu.value = row
  editingSkuItem.value = !!sku
  skuForm.value = sku ? { ...sku } : { spuId: row.id, skuName: '', price: 0, stock: 0, image: '' }
  skuVisible.value = true
}

async function saveSku() {
  skuSaving.value = true
  try {
    if (editingSkuItem.value) {
      await updateSku(skuForm.value)
      ElMessage.success('修改成功')
    } else {
      await createSku(skuForm.value)
      ElMessage.success('添加成功')
    }
    skuVisible.value = false
    if (currentSpu.value?.id) fetchSpuList(currentSpu.value.id)
  } finally { skuSaving.value = false }
}

async function removeSku(id: number) {
  ElMessageBox.confirm('确认删除？').then(async () => {
    await deleteSku(id)
    ElMessage.success('删除成功')
    if (currentSpu.value?.id) fetchSpuList(currentSpu.value.id)
    fetchData()
  })
}

async function handleSpuUpload(opts: any) {
  try { const url = await uploadRequest(opts.file); if (url) { spuForm.value.mainImage = url; ElMessage.success('上传成功') } }
  catch { ElMessage.error('上传失败') }
}
async function handleSkuUpload(opts: any) {
  try { const url = await uploadRequest(opts.file); if (url) { skuForm.value.image = url; ElMessage.success('上传成功') } }
  catch { ElMessage.error('上传失败') }
}

onMounted(fetchData)
</script>
