<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { blogApi } from '@/api/blog'
import type { Post, Category } from '@/types/blog'

const posts = ref<Post[]>([])
const category = ref<Category | null>(null)
const loading = ref(false)
const error = ref('')
const success = ref('')
const confirmVisible = ref(false)
const confirmMessage = ref('')
let pendingDeleteId: number | null = null

const sortedPosts = computed(() =>
  [...posts.value].sort((a, b) => {
    const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0
    return tb - ta
  }),
)

function formatDate(dateStr?: string) {
  if (!dateStr) return ''
  return dateStr.slice(0, 16).replace('T', ' ')
}

async function loadSparks() {
  loading.value = true
  error.value = ''
  try {
    const cats = await blogApi.adminCategories()
    category.value = cats.find(c => c.name === '碎碎念') ?? null
    if (category.value) {
      const page = await blogApi.publicPosts(category.value.id)
      posts.value = page.content
    } else {
      posts.value = []
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载碎碎念失败'
  } finally {
    loading.value = false
  }
}

function askDelete(id: number) {
  pendingDeleteId = id
  confirmMessage.value = '确认删除这条碎碎念？删除后不可恢复。'
  confirmVisible.value = true
}

async function confirmDelete() {
  if (pendingDeleteId === null) return
  confirmVisible.value = false
  error.value = ''
  success.value = ''
  try {
    await blogApi.deletePost(pendingDeleteId)
    posts.value = posts.value.filter(p => p.id !== pendingDeleteId)
    success.value = '已删除'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除失败，请稍后重试'
  } finally {
    pendingDeleteId = null
  }
}

onMounted(loadSparks)
</script>

<template>
  <AdminLayout>
    <section class="sparks-page animate-fade-in">
      <div class="card">
        <div class="card-accent"></div>
        <span class="card-kicker">Sparks</span>
        <div class="card-header">
          <h2 class="card-heading">碎碎念管理</h2>
          <button class="btn btn-ghost btn-sm" @click="loadSparks">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="23 4 23 10 17 10" />
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
            </svg>
            刷新
          </button>
        </div>

        <div v-if="error" class="alert alert-error">{{ error }}</div>
        <Transition name="fade">
          <div v-if="success" class="alert alert-success">{{ success }}</div>
        </Transition>

        <div v-if="loading" class="loading-hint">加载中...</div>

        <div v-if="!loading && sortedPosts.length === 0" class="empty-state">
          <p>暂无碎碎念，去总览页的灵感捕手发布一条吧</p>
        </div>

        <div v-if="!loading && sortedPosts.length > 0" class="spark-list">
          <div v-for="post in sortedPosts" :key="post.id" class="spark-item">
            <div class="spark-content">{{ post.content }}</div>
            <div class="spark-footer">
              <span class="spark-time">{{ formatDate(post.createdAt) }}</span>
              <button class="btn btn-danger btn-sm" @click="askDelete(post.id)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <ConfirmModal
      :visible="confirmVisible"
      :message="confirmMessage"
      confirm-text="删除"
      :danger="true"
      @confirm="confirmDelete"
      @cancel="confirmVisible = false"
    />
  </AdminLayout>
</template>

<style scoped>
.sparks-page {
  max-width: 800px;
  margin: 0 auto;
}

.card {
  position: relative;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 28px 30px 30px;
  overflow: hidden;
}

.card-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #e7dfcf;
}

.card-kicker {
  display: inline-block;
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--secondary);
  margin-bottom: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.card-heading {
  font-family: var(--font-serif);
  font-size: 1.35rem;
  font-weight: 700;
  margin: 0;
  color: var(--primary);
}

.alert {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 0.85rem;
  margin-bottom: 16px;
}

.alert-error {
  background: rgba(192, 57, 43, 0.08);
  color: #c0392b;
  border: 1px solid rgba(192, 57, 43, 0.18);
}

.alert-success {
  background: rgba(46, 139, 87, 0.08);
  color: #2e8b57;
  border: 1px solid rgba(46, 139, 87, 0.18);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.loading-hint {
  text-align: center;
  padding: 32px 0;
  color: var(--secondary);
  font-size: 0.9rem;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: var(--secondary);
  font-size: 0.92rem;
}

.spark-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.spark-item {
  padding: 18px 20px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--base);
  transition: background 0.2s var(--ease), border-color 0.2s var(--ease);
}

.spark-item:hover,
.spark-item:focus-within {
  background: var(--color-surface-hover);
  border-color: color-mix(in srgb, var(--color-accent) 18%, transparent);
}

.spark-content {
  font-size: 0.95rem;
  line-height: 1.7;
  color: var(--primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.spark-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.spark-time {
  font-size: 0.78rem;
  color: var(--secondary);
}

/* Buttons */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 20px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  color: var(--primary);
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.24s var(--ease), color 0.24s var(--ease);
  white-space: nowrap;
}

.btn:hover {
  background: var(--base);
}

.btn-sm {
  padding: 6px 14px;
  font-size: 0.8rem;
}

.btn-ghost {
  background: transparent;
  border-color: transparent;
  color: var(--secondary);
}

.btn-ghost:hover {
  color: var(--primary);
  background: var(--base);
}

.btn-danger {
  background: transparent;
  color: #c0392b;
  border-color: transparent;
}

.btn-danger:hover {
  background: rgba(192, 57, 43, 0.08);
}
</style>
