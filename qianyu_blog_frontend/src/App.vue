<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<template>
  <div class="app-shell">
    <header class="site-header">
      <RouterLink class="brand" to="/">千语博客</RouterLink>
      <nav>
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/admin/posts">后台</RouterLink>
        <button v-if="auth.isAuthenticated" class="link-button" @click="auth.logout()">退出</button>
      </nav>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>
