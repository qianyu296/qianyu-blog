import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { blogApi } from '@/api/blog'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('qianyu_admin_token') ?? '')
  const isAuthenticated = computed(() => Boolean(token.value))

  async function login(username: string, password: string) {
    const response = await blogApi.login(username, password)
    token.value = response.token
    localStorage.setItem('qianyu_admin_token', response.token)
  }

  function logout() {
    token.value = ''
    localStorage.removeItem('qianyu_admin_token')
  }

  return { token, isAuthenticated, login, logout }
})
