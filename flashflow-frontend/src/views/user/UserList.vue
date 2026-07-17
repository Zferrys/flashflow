<template>
  <div>
    <h3>用户管理</h3>

    <!-- 搜索栏 -->
    <el-card style="margin-bottom: 16px">
      <el-form :inline="true">
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="邮箱/昵称/手机号" clearable @clear="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="openCreate">新增用户</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="email" label="邮箱（登录账号）" min-width="180" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="editUser(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id!)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px" @closed="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" :disabled="isEdit" placeholder="登录账号" maxlength="100" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="formData.nickname" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" maxlength="20" placeholder="选填" />
        </el-form-item>
        <el-form-item :label="isEdit ? '新密码(留空不修改)' : '密码'" :prop="isEdit ? '' : 'password'">
          <el-input v-model="formData.password" type="password" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="statusValue" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserPage, createUser, updateUser, deleteUser, type CUser } from '@/api/user'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

const list = ref<CUser[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formData = ref<CUser>({})
const formRef = ref<FormInstance>()

const statusValue = ref(1)

const formRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度8-20位', trigger: 'blur' },
  ],
}

function resetForm() {
  formRef.value?.resetFields()
  formData.value = {}
  isEdit.value = false
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserPage({ page: page.value, size: size.value, keyword: keyword.value })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  fetchData()
}

function openCreate() {
  isEdit.value = false
  formData.value = {}
  statusValue.value = 1
  dialogVisible.value = true
}

function editUser(row: CUser) {
  isEdit.value = true
  formData.value = { ...row }
  statusValue.value = row.status ?? 1
  dialogVisible.value = true
}

function handleDelete(id: number) {
  ElMessageBox.confirm('确认删除该用户？', '提示').then(async () => {
    await deleteUser(id)
    ElMessage.success('删除成功')
    fetchData()
  })
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload: any = { ...formData.value, status: statusValue.value }
    delete payload.createTime
    delete payload.updateTime
    delete payload.lastLogin
    if (isEdit.value) {
      await updateUser(payload)
      ElMessage.success('修改成功')
    } else {
      await createUser(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '操作失败')
  } finally {
    saving.value = false
  }
}

onMounted(fetchData)
</script>
