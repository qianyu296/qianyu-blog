<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { blogApi } from '@/api/blog'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import MusicCapsule from '@/components/music/MusicCapsule.vue'

const auth = useAuthStore()
const route = useRoute()
const themeStore = useThemeStore()
const siteName = ref('千语博客')
const footerText = ref('')
const isScrolled = ref(false)

const isAdminRoute = computed(() => route.path.startsWith('/admin'))

function updateScroll() {
  isScrolled.value = window.scrollY > 12
}

onMounted(async () => {
  themeStore.initTheme()
  window.addEventListener('scroll', updateScroll, { passive: true })
  try {
    const settings = await blogApi.publicSiteSettings()
    siteName.value = settings.siteName || '千语博客'
    footerText.value = settings.footerText || ''
  } catch {
    // use defaults
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', updateScroll)
})
</script>

<template>
  <!-- Public layout -->
  <div v-if="!isAdminRoute" class="app-shell">
    <nav class="site-nav" :class="{ 'is-scrolled': isScrolled }">
      <div class="nav-inner">
        <RouterLink class="brand" to="/">{{ siteName }}</RouterLink>
        <div class="nav-actions" role="navigation">
          <RouterLink class="nav-link" to="/" :class="{ 'is-active': route.path === '/' }">首页</RouterLink>
          <RouterLink class="nav-link" to="/light" :class="{ 'is-active': route.path === '/light' }">光影</RouterLink>
          <RouterLink class="nav-link" to="/archive" :class="{ 'is-active': route.path === '/archive' }">文章</RouterLink>
          <RouterLink class="nav-link" to="/about" :class="{ 'is-active': route.path === '/about' }">关于</RouterLink>
          <RouterLink class="nav-link" to="/chat" :class="{ 'is-active': route.path === '/chat' }">AI</RouterLink>
          <RouterLink class="nav-link" to="/admin" :class="{ 'is-active': route.path.startsWith('/admin') }">后台</RouterLink>
          <button class="theme-toggle" @click="themeStore.toggleTheme()" :aria-label="themeStore.theme === 'dark' ? '切换到浅色' : '切换到暗色'">
            <svg v-if="themeStore.theme === 'dark'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
            </svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          </button>
        </div>
      </div>
    </nav>

    <main>
      <RouterView v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </RouterView>
    </main>

    <footer class="site-footer">
      <span>{{ footerText || `© ${new Date().getFullYear()} 千语博客 · Written in quiet light.` }}</span>
    </footer>
  </div>

  <!-- Admin layout -->
  <RouterView v-else />

  <!-- Music capsule: always visible -->
  <MusicCapsule />
</template>

<style scoped>
.site-nav {
  position: sticky;
  top: 0;
  z-index: 20;
  transition: background 0.3s var(--ease), box-shadow 0.3s var(--ease), border-color 0.3s var(--ease);
  border-bottom: 1px solid transparent;
}

.site-nav.is-scrolled {
  background: color-mix(in srgb, var(--surface) 70%, transparent);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-color: var(--line);
  box-shadow: var(--shadow);
}

/* Page transition */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.4s var(--ease), transform 0.4s var(--ease);
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* Theme toggle */
.theme-toggle {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--primary);
  transition: background 0.2s var(--ease), color 0.2s var(--ease);
}

.theme-toggle:hover {
  background: var(--code-bg);
}
</style>
