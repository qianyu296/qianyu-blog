<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const themeStore = useThemeStore()
const sidebarPinned = ref(false)
const zenMode = ref(false)

function togglePin() {
  sidebarPinned.value = !sidebarPinned.value
  localStorage.setItem('qianyu-admin-sidebar-pinned', String(sidebarPinned.value))
}

function toggleZen() {
  zenMode.value = !zenMode.value
  document.body.classList.toggle('zen-mode', zenMode.value)
}

function logout() {
  auth.logout()
  router.push('/')
}

onMounted(() => {
  if (localStorage.getItem('qianyu-admin-sidebar-pinned') === 'true') {
    sidebarPinned.value = true
  }
})
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar" :class="{ 'is-pinned': sidebarPinned }">
      <div class="sidebar-head">
        <div class="logo-mark">千</div>
        <div class="sidebar-title">千语工作台</div>
      </div>

      <nav class="admin-nav-stack">
        <RouterLink class="admin-nav-item" to="/admin/dashboard" :class="{ 'is-active': route.path === '/admin/dashboard' || route.path === '/admin' }">
          <svg class="admin-nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M4 11.2 12 4l8 7.2V20a1 1 0 0 1-1 1h-5v-6h-4v6H5a1 1 0 0 1-1-1v-8.8Z"/></svg>
          <span class="nav-label">总览</span>
        </RouterLink>
        <RouterLink class="admin-nav-item" to="/admin/posts" :class="{ 'is-active': route.path.startsWith('/admin/posts') }">
          <svg class="admin-nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M5 18.6V21h2.4L18.7 9.7l-2.4-2.4L5 18.6ZM20.4 8a1.2 1.2 0 0 0 0-1.7l-1.7-1.7a1.2 1.2 0 0 0-1.7 0l-1.3 1.3 2.4 2.4L20.4 8Z"/></svg>
          <span class="nav-label">写作</span>
        </RouterLink>
        <RouterLink class="admin-nav-item" to="/admin/sparks" :class="{ 'is-active': route.path === '/admin/sparks' }">
          <svg class="admin-nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/></svg>
          <span class="nav-label">碎碎念</span>
        </RouterLink>
        <RouterLink class="admin-nav-item" to="/admin/categories" :class="{ 'is-active': route.path === '/admin/categories' }">
          <svg class="admin-nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z"/></svg>
          <span class="nav-label">内容分区</span>
        </RouterLink>
        <RouterLink class="admin-nav-item" to="/admin/media" :class="{ 'is-active': route.path === '/admin/media' }">
          <svg class="admin-nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M5 5h14a2 2 0 012 2v10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2zm1 11h12l-3.8-5-3 3.8-2-2.5L6 16zm11.5-6.8a1.7 1.7 0 100-3.4 1.7 1.7 0 000 3.4z"/></svg>
          <span class="nav-label">光影</span>
        </RouterLink>
        <RouterLink class="admin-nav-item" to="/admin/music" :class="{ 'is-active': route.path === '/admin/music' }">
          <svg class="admin-nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M18 4v11.2A3.3 3.3 0 1116 12.1V7H9v10.2A3.3 3.3 0 117 14.1V4h11z"/></svg>
          <span class="nav-label">音乐</span>
        </RouterLink>
        <RouterLink class="admin-nav-item" to="/admin/ai-settings" :class="{ 'is-active': route.path === '/admin/ai-settings' }">
          <svg class="admin-nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 2a2 2 0 012 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 017 7h1a1 1 0 011 1v3a1 1 0 01-1 1h-1.27A7 7 0 0114 23h-4a7 7 0 01-6.73-4H2a1 1 0 01-1-1v-3a1 1 0 011-1h1a7 7 0 017-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 012-2z"/></svg>
          <span class="nav-label">AI 设置</span>
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <button class="pin-button" @click="togglePin">
          <svg class="admin-nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M14 3 21 10l-2.1 2.1-1.4-1.4-3.2 3.2.3 3.9-1.4 1.4-3.8-3.8L5 20l-1-1 4.6-4.4-3.8-3.8 1.4-1.4 3.9.3 3.2-3.2L11.9 5.1 14 3z"/></svg>
          <span class="pin-label">固定导航</span>
        </button>
        <button class="pin-button" @click="logout">
          <svg class="admin-nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M16 13v-2H7V8l-5 4 5 4v-3h9zm3-10H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2V5a2 2 0 00-2-2z"/></svg>
          <span class="pin-label">返回前台</span>
        </button>
      </div>
    </aside>

    <main class="admin-workspace">
      <header class="admin-header">
        <div>
          <div class="admin-logo">千语工作台</div>
          <div class="header-subtitle">写作、光影和氛围音乐的统一管理画布</div>
        </div>
        <div class="header-actions">
          <button class="btn btn-ghost" @click="toggleZen">
            <svg class="admin-nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 4a8 8 0 100 16 8 8 0 000-16zm0 2a6 6 0 010 12V6z"/></svg>
            <span>{{ zenMode ? '退出禅定' : '禅定模式' }}</span>
          </button>
          <button class="btn btn-ghost theme-toggle-btn" @click="themeStore.toggleTheme()" :aria-label="themeStore.theme === 'dark' ? '切换到浅色' : '切换到暗色'">
            <svg v-if="themeStore.theme === 'dark'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
            </svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          </button>
          <RouterLink class="btn btn-primary" to="/">查看前台</RouterLink>
        </div>
      </header>

      <slot />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 76% 10%, color-mix(in srgb, var(--surface) 86%, transparent), transparent 28rem),
    var(--base);
}

.admin-sidebar {
  position: relative;
  z-index: 4;
  width: 80px;
  height: 100vh;
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 18px;
  padding: 26px 16px;
  border-right: 1px solid var(--line);
  background: color-mix(in srgb, var(--surface) 82%, transparent);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  transition: width 0.32s var(--ease), opacity 0.28s var(--ease);
  overflow: hidden;
}

.admin-sidebar:hover,
.admin-sidebar.is-pinned {
  width: 216px;
}

.admin-sidebar:hover ~ .admin-workspace .admin-header,
.admin-sidebar.is-pinned ~ .admin-workspace .admin-header {
  left: 216px;
}

.sidebar-head {
  min-height: 46px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-mark {
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  display: grid;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--base);
  font-family: var(--font-serif);
  font-size: 1.15rem;
  font-weight: 700;
}

.sidebar-title,
.nav-label,
.pin-label {
  overflow: hidden;
  opacity: 0;
  transform: translateX(-6px);
  transition: opacity 0.22s var(--ease), transform 0.22s var(--ease);
  white-space: nowrap;
}

.admin-sidebar:hover .sidebar-title,
.admin-sidebar:hover .nav-label,
.admin-sidebar:hover .pin-label,
.admin-sidebar.is-pinned .sidebar-title,
.admin-sidebar.is-pinned .nav-label,
.admin-sidebar.is-pinned .pin-label {
  opacity: 1;
  transform: translateX(0);
}

.sidebar-title {
  font-family: var(--font-serif);
  font-size: 1.2rem;
  font-weight: 700;
}

.admin-nav-stack {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.admin-nav-item {
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 13px;
  border: 0;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--secondary);
  padding: 0 13px;
  transition: background 0.24s var(--ease), color 0.24s var(--ease);
  cursor: pointer;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
}

.admin-nav-item:hover,
.admin-nav-item.is-active,
.admin-nav-item.router-link-active {
  background: var(--base);
  color: var(--primary);
}

.admin-nav-icon {
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  color: currentColor;
}

.sidebar-footer {
  margin-top: auto;
  display: grid;
  gap: 8px;
}

.pin-button {
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--secondary);
  padding: 0 13px;
  transition: color 0.24s var(--ease), background 0.24s var(--ease);
  cursor: pointer;
  font-size: 13px;
}

.pin-button:hover,
.admin-sidebar.is-pinned .pin-button {
  background: var(--code-bg);
  color: var(--primary);
}

.admin-workspace {
  min-width: 0;
  flex: 1;
  height: 100vh;
  overflow-y: auto;
  padding: 90px clamp(22px, 4vw, 48px) 48px;
}

.admin-header {
  position: fixed;
  top: 0;
  left: 80px;
  right: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px clamp(22px, 4vw, 48px) 14px;
  background: var(--base);
  transition: left 0.32s var(--ease);
}

.admin-header::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -12px;
  height: 12px;
  background: linear-gradient(180deg, var(--base), transparent);
  pointer-events: none;
}

.admin-logo {
  font-family: var(--font-serif);
  font-size: clamp(1.6rem, 3vw, 2.5rem);
  font-weight: 700;
}

.header-subtitle {
  margin-top: 6px;
  color: var(--secondary);
  font-size: 0.92rem;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* Zen mode */
:global(body.zen-mode) .admin-sidebar,
:global(body.zen-mode) .admin-header {
  opacity: 0;
  pointer-events: none;
}

:global(body.zen-mode) .admin-workspace {
  padding-inline: clamp(28px, 10vw, 150px);
}

@media (max-width: 820px) {
  .admin-layout {
    display: block;
    height: auto;
    min-height: 100vh;
    overflow: auto;
  }

  .admin-sidebar {
    position: sticky;
    top: 0;
    width: 100%;
    height: auto;
    flex-direction: row;
    align-items: center;
    overflow-x: auto;
    padding: 12px;
  }

  .admin-sidebar:hover,
  .admin-sidebar.is-pinned {
    width: 100%;
  }

  .sidebar-head,
  .sidebar-footer {
    display: none;
  }

  .admin-nav-stack {
    display: flex;
    margin: 0;
  }

  .admin-nav-item {
    flex: 0 0 auto;
  }

  .nav-label {
    opacity: 1;
    transform: none;
  }

  .admin-workspace {
    height: auto;
    overflow: visible;
    padding: 24px 16px 42px;
  }

  .admin-header {
    position: static;
    left: auto;
    right: auto;
    margin: 0 0 24px;
    padding: 0;
    background: transparent;
  }

  .admin-header::after {
    display: none;
  }
}
</style>
