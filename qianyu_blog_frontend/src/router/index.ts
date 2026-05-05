import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'
import PostDetailView from '@/views/PostDetailView.vue'
import GameCenterView from '@/views/GameCenterView.vue'
import AdminLoginView from '@/views/admin/AdminLoginView.vue'
import AdminCategoriesView from '@/views/admin/AdminCategoriesView.vue'
import AdminAISettingsView from '@/views/admin/AdminAISettingsView.vue'
import ChatView from '@/views/ChatView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/light', name: 'light', component: () => import('@/views/LightView.vue') },
    { path: '/archive', name: 'archive', component: () => import('@/views/ArchiveView.vue') },
    { path: '/posts/:id', name: 'post-detail', component: PostDetailView },
    { path: '/games', name: 'games', component: GameCenterView },
    { path: '/about', name: 'about', component: () => import('@/views/AboutView.vue') },
    { path: '/chat', name: 'chat', component: ChatView },
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
    { path: '/admin', redirect: '/admin/dashboard' },
    { path: '/admin/dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/DashboardView.vue'), meta: { requiresAuth: true } },
    { path: '/admin/posts', name: 'admin-posts', component: () => import('@/views/admin/AdminPostsView.vue'), meta: { requiresAuth: true } },
    { path: '/admin/sparks', name: 'admin-sparks', component: () => import('@/views/admin/AdminSparksView.vue'), meta: { requiresAuth: true } },
    { path: '/admin/categories', name: 'admin-categories', component: AdminCategoriesView, meta: { requiresAuth: true } },
    { path: '/admin/media', name: 'admin-media', component: () => import('@/views/admin/AdminMediaView.vue'), meta: { requiresAuth: true } },
    { path: '/admin/music', name: 'admin-music', component: () => import('@/views/admin/AdminMusicView.vue'), meta: { requiresAuth: true } },
    { path: '/admin/ai-settings', name: 'admin-ai-settings', component: AdminAISettingsView, meta: { requiresAuth: true } },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }
})

export default router
