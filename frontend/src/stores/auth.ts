import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { apiPost } from '@/shared/api/http'
import type { LoginRequest, LoginResponse } from '@/shared/types/api'

const TOKEN_KEY = 'iids.auth.token'
const USER_KEY = 'iids.auth.user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<LoginResponse | null>(readUser())
  const isAuthenticated = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.displayName ?? user.value?.username ?? 'User')

  async function login(request: LoginRequest) {
    const response = await apiPost<LoginResponse, LoginRequest>('/auth/login', request)
    token.value = response.token
    user.value = response
    localStorage.setItem(TOKEN_KEY, response.token)
    localStorage.setItem(USER_KEY, JSON.stringify(response))
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isAuthenticated, displayName, login, logout }
})

function readUser(): LoginResponse | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as LoginResponse
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}