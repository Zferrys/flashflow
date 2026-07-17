<template>
  <div>
    <header style="background:#fff;border-bottom:1px solid #eee;padding:0 16px">
      <div style="max-width:1200px;margin:0 auto;display:flex;align-items:center;height:56px;justify-content:space-between">
        <div style="display:flex;align-items:center;gap:16px">
          <span style="font-size:18px;font-weight:bold;color:#409eff;cursor:pointer" @click="router.push('/')">⚡ FlashFlow</span>
          <span style="font-size:16px;font-weight:500">收货地址</span>
        </div>
        <el-button text @click="router.push('/order')">我的订单</el-button>
      </div>
    </header>

    <div style="max-width:700px;margin:24px auto;padding:0 16px">
      <el-card v-loading="loading">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>收货地址 ({{ list.length }})</span>
            <el-button size="small" type="primary" @click="showDialog()">新增地址</el-button>
          </div>
        </template>
        <div v-if="list.length === 0" style="text-align:center;padding:40px;color:#999">
          暂无收货地址
        </div>
        <div v-else>
          <div v-for="item in list" :key="item.id" style="padding:12px 0;border-bottom:1px solid #f0f0f0;display:flex;justify-content:space-between;align-items:center">
            <div>
              <div style="display:flex;align-items:center;gap:8px">
                <span style="font-weight:bold">{{ item.receiverName }}</span>
                <span style="color:#999;font-size:13px">{{ item.receiverPhone }}</span>
                <el-tag v-if="item.isDefault" size="small" type="danger">默认</el-tag>
              </div>
              <div style="color:#666;font-size:13px;margin-top:4px">{{ item.province }}{{ item.city }}{{ item.district }}{{ item.detailAddress }}</div>
            </div>
            <div style="display:flex;gap:8px;flex-shrink:0">
              <el-button v-if="!item.isDefault" size="small" text @click="setDefault(item.id!)">设为默认</el-button>
              <el-button size="small" text @click="showDialog(item)">编辑</el-button>
              <el-button size="small" text type="danger" @click="remove(item.id!)">删除</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑地址' : '新增地址'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="收货人" required>
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="form.receiverPhone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区" required>
          <el-cascader v-model="regionSelected" :options="regionOptions" placeholder="请选择省/市/区" style="width:100%" @change="onRegionChange" clearable filterable />
        </el-form-item>
        <el-form-item label="详细地址" required>
          <el-input v-model="form.detailAddress" placeholder="街道、门牌号等" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.isDefault" :true-value="1" :false-value="0">设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getAddresses, saveAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/cart'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const editing = ref(false)
const form = ref<any>({})
const regionSelected = ref<string[]>([])

// 基础省市数据（企业级应接入完整地区数据API）
const regionOptions = [
  { value: '北京市', label: '北京市', children: [{ value: '东城区', label: '东城区' }, { value: '西城区', label: '西城区' }, { value: '朝阳区', label: '朝阳区' }, { value: '海淀区', label: '海淀区' }, { value: '丰台区', label: '丰台区' }] },
  { value: '上海市', label: '上海市', children: [{ value: '黄浦区', label: '黄浦区' }, { value: '徐汇区', label: '徐汇区' }, { value: '浦东新区', label: '浦东新区' }, { value: '静安区', label: '静安区' }] },
  { value: '广东省', label: '广东省', children: [{ value: '广州市', label: '广州市' }, { value: '深圳市', label: '深圳市' }, { value: '东莞市', label: '东莞市' }, { value: '佛山市', label: '佛山市' }] },
  { value: '浙江省', label: '浙江省', children: [{ value: '杭州市', label: '杭州市' }, { value: '宁波市', label: '宁波市' }, { value: '温州市', label: '温州市' }] },
  { value: '江苏省', label: '江苏省', children: [{ value: '南京市', label: '南京市' }, { value: '苏州市', label: '苏州市' }, { value: '无锡市', label: '无锡市' }] },
  { value: '四川省', label: '四川省', children: [{ value: '成都市', label: '成都市' }, { value: '绵阳市', label: '绵阳市' }] },
  { value: '湖北省', label: '湖北省', children: [{ value: '武汉市', label: '武汉市' }, { value: '宜昌市', label: '宜昌市' }] },
  { value: '山东省', label: '山东省', children: [{ value: '济南市', label: '济南市' }, { value: '青岛市', label: '青岛市' }] },
  { value: '福建省', label: '福建省', children: [{ value: '福州市', label: '福州市' }, { value: '厦门市', label: '厦门市' }] },
  { value: '湖南省', label: '湖南省', children: [{ value: '长沙市', label: '长沙市' }] },
  { value: '河南省', label: '河南省', children: [{ value: '郑州市', label: '郑州市' }] },
]

function onRegionChange(val: string[]) {
  form.value.province = val[0] || ''
  form.value.city = val[1] || ''
  form.value.district = val[2] || ''
}

async function fetchList() {
  const uid = String(userStore.userId || 0)
  if (!userStore.token) return
  loading.value = true
  try {
    const res = await getAddresses()
    list.value = res.data || []
  } finally { loading.value = false }
}

function showDialog(item?: any) {
  editing.value = !!item
  if (item) {
    form.value = { ...item }
    // 回填级联选择器
    const parts = [item.province, item.city, item.district].filter(Boolean)
    regionSelected.value = parts.length > 0 ? parts : []
  } else {
    form.value = { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 }
    regionSelected.value = []
  }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.receiverName || !form.value.receiverPhone || !form.value.detailAddress) {
    ElMessage.warning('请填写完整信息')
    return
  }
  saving.value = true
  try {
    const uid = String(userStore.userId || 0)
    if (!userStore.token) return
    form.value.userId = Number(uid)
    if (editing.value) {
      await updateAddress(form.value)
      ElMessage.success('修改成功')
    } else {
      await saveAddress(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally { saving.value = false }
}

async function remove(id: number) {
  ElMessageBox.confirm('确认删除该地址？').then(async () => {
    await deleteAddress(id)
    ElMessage.success('已删除')
    fetchList()
  })
}

async function setDefault(id: number) {
  const uid = String(userStore.userId || 0)
  if (!userStore.token) return
  await setDefaultAddress(id)
  ElMessage.success('已设为默认')
  fetchList()
}

onMounted(fetchList)
</script>
