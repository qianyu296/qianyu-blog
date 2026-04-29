import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'
import PostDetailView from '@/views/PostDetailView.vue'
import AdminLoginView from '@/views/admin/AdminLoginView.vue'
import AdminPostsView from '@/views/admin/AdminPostsView.vue'
import AdminPostEditView from '@/views/admin/AdminPostEditView.vue'
import AdminCategoriesView from '@/views/admin/AdminCategoriesView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/posts/:id', name: 'post-detail', component: PostDetailView },
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
    { path: '/admin/posts', name: 'admin-posts', component: AdminPostsView, meta: { requiresAuth: true } },
    { path: '/admin/posts/new', name: 'admin-post-new', component: AdminPostEditView, meta: { requiresAuth: true } },
    { path: '/admin/posts/:id/edit', name: 'admin-post-edit', component: AdminPostEditView, meta: { requiresAuth: true } },
    { path: '/admin/categories', name: 'admin-categories', component: AdminCategoriesView, meta: { requiresAuth: true } },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }
})

export default router
