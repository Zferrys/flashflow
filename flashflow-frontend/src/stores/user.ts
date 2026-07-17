import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, type LoginData } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('flashflow_token') || '')
  const username = ref(localStorage.getItem('flashflow_username') || '')
  const userId = ref(Number(localStorage.getItem('flashflow_userId') || 0))
  const role = ref(localStorage.getItem('flashflow_role') || '')

  const isLoggedIn = computed(() => !!token.value)

  async function login(loginData: LoginData) {
    const res = await loginApi(loginData)
    token.value = res.data.accessToken
    username.value = res.data.user.username
    userId.value = res.data.user.id
    role.value = res.data.user.roleCode
    localStorage.setItem('flashflow_token', res.data.accessToken)
    localStorage.setItem('flashflow_username', res.data.user.username)
    localStorage.setItem('flashflow_userId', String(res.data.user.id))
    localStorage.setItem('flashflow_role', res.data.user.roleCode)
  }

  function setToken(accessToken: string, refreshToken?: string) {
    token.value = accessToken
    localStorage.setItem('flashflow_token', accessToken)
  }

  function setUserInfo(info: { id: number; username: string; roleCode: string }) {
    username.value = info.username
    userId.value = info.id
    role.value = info.roleCode
    localStorage.setItem('flashflow_username', info.username)
    localStorage.setItem('flashflow_userId', String(info.id))
    localStorage.setItem('flashflow_role', info.roleCode)
  }

  function logout() {
    token.value = ''
    username.value = ''
    userId.value = 0
    role.value = ''
    localStorage.removeItem('flashflow_token')
    localStorage.removeItem('flashflow_username')
    localStorage.removeItem('flashflow_userId')
    localStorage.removeItem('flashflow_role')
  }

  return { token, username, userId, role, isLoggedIn, login, setToken, setUserInfo, logout }
})
