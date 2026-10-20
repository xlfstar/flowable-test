import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo, login as loginApi, logout as logoutApi, type LoginParams } from '@/api/auth'
import type { UserInfo } from '@/types'

function parseUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem('user')
    return raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const user = ref<UserInfo | null>(parseUser())

  function setToken(value: string) {
    token.value = value
    localStorage.setItem('token', value)
  }

  function setUser(value: UserInfo) {
    user.value = value
    localStorage.setItem('user', JSON.stringify(value))
  }

  async function login(params: LoginParams) {
    const res = await loginApi(params)
    setToken(res.data.token)
    setUser(res.data.user)
    return res.data
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    setUser(res.data)
    return res.data
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 忽略退出接口异常
    }
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, login, fetchUserInfo, logout, setToken, setUser }
})
