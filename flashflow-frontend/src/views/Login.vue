<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">⚡</div>
        <h1 class="brand-title">FlashFlow</h1>
        <p class="brand-sub">高并发闪购平台 · 企业级微服务架构</p>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-dot"></span> Redis 分片库存 · 5000+ QPS
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span> Saga 分布式事务 · 数据最终一致
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span> Sentinel 三层限流 · 削峰填谷
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-form-side">
      <div class="login-card">
        <div class="login-header">
          <h2 class="login-title">{{ isAdmin ? '管理员登录' : '欢迎回来' }}</h2>
          <p class="login-desc">{{ isAdmin ? '后台管理系统' : '登录您的账户继续购物' }}</p>
        </div>

        <div class="login-tabs">
          <button :class="['tab-btn', { active: !isAdmin }]" @click="switchTab(false)">用户登录</button>
          <button :class="['tab-btn', { active: isAdmin }]" @click="switchTab(true)">管理员</button>
        </div>

        <!-- 用户登录 -->
        <el-form v-if="!isAdmin" ref="userFormRef" :model="userForm" :rules="userRules" label-width="0" size="large" @keyup.enter="handleUserLogin">
          <el-form-item prop="account">
            <el-input v-model="userForm.account" placeholder="请输入邮箱" maxlength="100">
              <template #prefix><span class="input-prefix">📧</span></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password>
              <template #prefix><span class="input-prefix">🔒</span></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="warning" class="login-btn" :loading="userLoading" @click="handleUserLogin">登 录</el-button>
          </el-form-item>
        </el-form>

        <!-- 管理员登录 -->
        <el-form v-else ref="adminFormRef" :model="adminForm" :rules="adminRules" label-width="0" size="large" @keyup.enter="handleAdminLogin">
          <el-form-item prop="account">
            <el-input v-model="adminForm.account" placeholder="请输入管理员用户名">
              <template #prefix><span class="input-prefix">👤</span></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="adminForm.password" type="password" placeholder="请输入密码" show-password>
              <template #prefix><span class="input-prefix">🔐</span></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-btn admin-btn" :loading="adminLoading" @click="handleAdminLogin">管理员登录</el-button>
          </el-form-item>
        </el-form>

        <div v-if="!isAdmin" class="login-footer">
          还没有账号？<a href="/register" @click.prevent="router.push('/register')">立即注册 →</a>
        </div>
        <div v-else class="login-footer login-footer-admin">
          <span>请使用管理员账号登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh; display: flex;
  background: #f8fafc;
}
/* ── 左侧品牌区 ── */
.login-brand {
  flex: 1; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(160deg, #0f172a 0%, #1e293b 40%, #334155 100%);
  padding: 60px; position: relative; overflow: hidden;
}
.login-brand::before {
  content: ''; position: absolute;
  top: -50%; right: -30%;
  width: 600px; height: 600px;
  background: radial-gradient(circle, rgba(245,158,11,0.08) 0%, transparent 70%);
  border-radius: 50%;
}
.brand-content { position: relative; z-index: 1; max-width: 420px; }
.brand-logo { font-size: 48px; margin-bottom: 16px; }
.brand-title {
  font-size: 42px; font-weight: 900; color: #fff;
  letter-spacing: -1px; margin-bottom: 8px;
}
.brand-sub { font-size: 15px; color: #94a3b8; margin-bottom: 40px; line-height: 1.6; }
.brand-features { display: flex; flex-direction: column; gap: 14px; }
.feature-item {
  display: flex; align-items: center; gap: 10px;
  font-size: 13px; color: #cbd5e1;
}
.feature-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--ff-accent); flex-shrink: 0;
}
/* ── 右侧登录区 ── */
.login-form-side {
  width: 480px; display: flex; align-items: center; justify-content: center;
  padding: 40px;
}
.login-card { width: 100%; max-width: 380px; }
.login-header { margin-bottom: 32px; }
.login-title { font-size: 28px; font-weight: 800; color: #0f172a; margin-bottom: 6px; }
.login-desc { font-size: 14px; color: #94a3b8; }
/* Tabs */
.login-tabs { display: flex; margin-bottom: 28px; background: #f1f5f9; border-radius: 10px; padding: 4px; }
.tab-btn {
  flex: 1; padding: 10px 0; border: none; background: transparent;
  font-size: 14px; font-weight: 500; color: #64748b; cursor: pointer;
  border-radius: 8px; transition: all 0.2s;
}
.tab-btn.active { background: #fff; color: #0f172a; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.tab-btn:hover:not(.active) { color: #334155; }
/* Form */
.input-prefix { font-size: 16px; opacity: 0.7; }
.login-btn {
  width: 100%; height: 48px; font-size: 16px; font-weight: 600;
  border-radius: 10px !important; letter-spacing: 2px;
}
.admin-btn { background: #1e293b !important; border-color: #1e293b !important; }
.admin-btn:hover { background: #334155 !important; }
.login-footer { text-align: center; margin-top: 20px; font-size: 13px; color: #94a3b8; }
.login-footer a { color: #f59e0b; font-weight: 500; }
.login-footer-admin { font-size: 12px; color: #94a3b8; }
/* Responsive */
@media (max-width: 768px) {
  .login-brand { display: none; }
  .login-form-side { width: 100%; padding: 20px; }
}
</style>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isAdmin = ref(false)
const userFormRef = ref()
const adminFormRef = ref()

const userLoading = ref(false)
const adminLoading = ref(false)

const userForm = reactive({ account: '', password: '' })
const adminForm = reactive({ account: '', password: '' })

const validateEmail = (_rule: any, value: string, callback: any) => {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) callback(new Error('请输入正确的邮箱地址'))
  else callback()
}
const userRules = {
  account: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const adminRules = {
  account: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function switchTab(v: boolean) { isAdmin.value = v }

function getRedirect() {
  const redirect = route.query.redirect as string
  // 防止开放重定向攻击，只允许站内路径
  if (redirect && (redirect.startsWith('/') && !redirect.startsWith('//'))) return redirect
  return null
}

async function handleUserLogin() {
  const valid = await userFormRef.value.validate().catch(() => false)
  if (!valid) return
  userLoading.value = true
  try {
    const res = await request.post('/auth/user/login', { account: userForm.account, password: userForm.password })
    const data = res.data
    userStore.setToken(data.accessToken, data.refreshToken)
    if (data.refreshToken) {
      localStorage.setItem('flashflow_refreshToken', data.refreshToken)
    }
    // 登录后调用 /me 获取完整用户信息
    try {
      const meRes = await request.get('/auth/me')
      if (meRes.data) {
        userStore.setUserInfo({
          id: meRes.data.userId || meRes.data.id,
          username: meRes.data.username,
          roleCode: meRes.data.roleCode || 'ROLE_USER'
        })
      }
    } catch {
      // 获取用户信息失败不影响登录
      userStore.setUserInfo({ id: 0, username: userForm.account, roleCode: 'ROLE_USER' })
    }
    ElMessage.success('登录成功')
    router.push(getRedirect() || '/')
  } catch { /* handled in interceptor */ }
  finally { userLoading.value = false }
}

async function handleAdminLogin() {
  const valid = await adminFormRef.value.validate().catch(() => false)
  if (!valid) return
  adminLoading.value = true
  try {
    await userStore.login(adminForm)
    ElMessage.success('登录成功')
    router.push(getRedirect() || '/admin/dashboard')
  } catch { /* handled in interceptor */ }
  finally { adminLoading.value = false }
}
</script>
