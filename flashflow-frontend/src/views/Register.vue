<template>
  <div class="register-page">
    <el-card class="register-card">
      <div class="register-header">
        <h1 class="register-logo">⚡ FlashFlow</h1>
        <p class="register-sub">创建您的账号，开启闪购之旅</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large" @keyup.enter="handleRegister">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱（登录账号）" maxlength="100">
            <template #prefix><span class="input-emoji">📧</span></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="verifyCode">
          <div style="display:flex;gap:8px;width:100%">
            <el-input v-model="form.verifyCode" placeholder="邮箱验证码" maxlength="6" style="flex:1">
              <template #prefix><span class="input-emoji">🔑</span></template>
            </el-input>
            <el-button :disabled="codeSending || countdown > 0" :loading="codeSending" @click="sendVerifyCode" style="min-width:110px">
              {{ countdown > 0 ? `${countdown}s后重发` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号（选填）" maxlength="11">
            <template #prefix><span class="input-emoji">📱</span></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（选填）">
            <template #prefix><span class="input-emoji">👤</span></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（8-20位，含大小写+数字+特殊字符）" show-password>
            <template #prefix><span class="input-emoji">🔒</span></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password>
            <template #prefix><span class="input-emoji">🔒</span></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="warning" class="register-btn" :loading="loading" @click="handleRegister">注 册</el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        已有账号？<a href="/login" @click.prevent="router.push('/login')">立即登录 →</a>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh; display: flex; justify-content: center; align-items: center;
  background: #f8fafc; padding: 20px;
}
.register-card {
  width: 420px; max-width: 100%; padding: 32px 28px; border-radius: 16px;
}
.register-header { text-align: center; margin-bottom: 28px; }
.register-logo { font-size: 28px; font-weight: 800; color: #0f172a; margin-bottom: 6px; }
.register-sub { font-size: 14px; color: #94a3b8; }
.input-emoji { font-size: 16px; }
.register-btn {
  width: 100%; height: 48px; font-size: 16px; font-weight: 600;
  border-radius: 10px !important; letter-spacing: 2px;
}
.register-footer { text-align: center; margin-top: 16px; font-size: 13px; color: #94a3b8; }
.register-footer a { color: #f59e0b; font-weight: 500; }
</style>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)

const form = reactive({
  email: '',
  verifyCode: '',
  phone: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const validateEmail = (_rule: any, value: string, callback: any) => {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) callback(new Error('请输入正确的邮箱地址'))
  else callback()
}
const validatePhone = (_rule: any, value: string, callback: any) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) callback(new Error('请输入正确的手机号'))
  else callback()
}
const validatePassword = (_rule: any, value: string, callback: any) => {
  if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*]).{8,20}$/.test(value)) {
    callback(new Error('密码需包含大小写字母、数字和特殊字符'))
  } else callback()
}
const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' },
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
  ],
  phone: [
    { validator: validatePhone, trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function sendVerifyCode() {
  const email = form.email
  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.warning('请先输入正确的邮箱地址')
    return
  }
  codeSending.value = true
  try {
    await request.post('/auth/user/send-code', null, { params: { email } })
    ElMessage.success('验证码已发送至您的邮箱')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '发送失败')
  } finally { codeSending.value = false }
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await request.post('/auth/user/register', {
      email: form.email,
      verifyCode: form.verifyCode,
      phone: form.phone || undefined,
      password: form.password,
      nickname: form.nickname || undefined,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || e?.msg || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>
