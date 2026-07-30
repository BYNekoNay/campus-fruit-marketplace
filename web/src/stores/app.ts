import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const loading = ref(false)
  const theme = ref<'light' | 'dark'>('light')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function startLoading() {
    loading.value = true
  }

  function stopLoading() {
    loading.value = false
  }

  function setLoading(val: boolean) {
    loading.value = val
  }

  function setTheme(val: 'light' | 'dark') {
    theme.value = val
    document.documentElement.setAttribute('data-theme', val)
  }

  return {
    sidebarCollapsed,
    loading,
    theme,
    toggleSidebar,
    startLoading,
    stopLoading,
    setLoading,
    setTheme,
  }
})
