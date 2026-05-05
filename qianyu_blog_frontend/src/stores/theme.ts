import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<'light' | 'dark'>('light')

  function initTheme() {
    const stored = localStorage.getItem('qianyu-theme')
    if (stored === 'dark' || stored === 'light') {
      theme.value = stored
    } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
      theme.value = 'dark'
    }
    applyTheme()
  }

  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    localStorage.setItem('qianyu-theme', theme.value)
    applyTheme()
  }

  function applyTheme() {
    document.documentElement.dataset.theme = theme.value
  }

  return { theme, initTheme, toggleTheme }
})
