<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { blogApi } from '@/api/blog'
import type { Category } from '@/types/blog'
import AdminLayout from '@/components/AdminLayout.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'

const categories = ref<Category[]>([])
const editingId = ref<number>()
const name = ref('')
const slug = ref('')
const description = ref('')
const error = ref('')
const loading = ref(false)
const confirmVisible = ref(false)
const confirmMessage = ref('')
let pendingDeleteId: number | null = null

async function load() {
  loading.value = true
  try {
    categories.value = await blogApi.adminCategories()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = undefined
  name.value = ''
  slug.value = ''
  description.value = ''
}

function edit(category: Category) {
  editingId.value = category.id
  name.value = category.name
  slug.value = category.slug
  description.value = category.description ?? ''
}

async function save() {
  error.value = ''
  if (!name.value.trim()) {
    error.value = '分类名称不能为空，请填写后再保存'
    return
  }
  if (!slug.value.trim()) {
    error.value = '分类标识（Slug）不能为空，用于 URL 路径，如：tech-notes'
    return
  }
  try {
    const payload = { name: name.value, slug: slug.value, description: description.value }
    if (editingId.value) {
      await blogApi.updateCategory(editingId.value, payload)
    } else {
      await blogApi.createCategory(payload)
    }
    resetForm()
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '分类保存失败，请检查网络后重试'
  }
}

function askRemove(id: number) {
  pendingDeleteId = id
  confirmMessage.value = '确认删除这个分类？删除后不可恢复。'
  confirmVisible.value = true
}

async function confirmRemove() {
  if (pendingDeleteId === null) return
  confirmVisible.value = false
  try {
    await blogApi.deleteCategory(pendingDeleteId)
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '分类删除失败，请确认该分类下无文章后重试'
  } finally {
    pendingDeleteId = null
  }
}

onMounted(load)
</script>

<template>
  <AdminLayout>
  <section class="admin-categories animate-fade-in">
    <div class="toolbar">
      <div class="toolbar-left">
        <h1>分类管理</h1>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <span>加载中...</span>
    </div>

    <template v-if="!loading">
      <section class="category-form">
        <h2 class="form-title">{{ editingId ? '编辑分类' : '新增分类' }}</h2>
        <form class="form-grid" @submit.prevent="save">
          <div class="form-group">
            <label class="form-label" for="name">名称</label>
            <input
              id="name"
              v-model="name"
              type="text"
              class="form-input"
              placeholder="分类名称"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label" for="slug">标识</label>
            <input
              id="slug"
              v-model="slug"
              type="text"
              class="form-input"
              placeholder="如: tech"
              required
            />
          </div>
          <div class="form-group form-group-full">
            <label class="form-label" for="desc">描述</label>
            <input
              id="desc"
              v-model="description"
              type="text"
              class="form-input"
              placeholder="分类描述（可选）"
            />
          </div>
        </form>

        <div v-if="error" class="alert alert-error mt-md">
          {{ error }}
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" @click="save">
            {{ editingId ? '保存分类' : '新增分类' }}
          </button>
          <button v-if="editingId" type="button" class="btn btn-secondary" @click="resetForm">
            取消编辑
          </button>
        </div>
      </section>

      <section class="category-list">
        <h2 class="form-title">分类列表</h2>
        <div class="table-list">
          <div
            v-for="category in categories"
            :key="category.id"
            class="table-row stagger-item"
          >
            <div class="table-row-content">
              <div class="table-row-title">{{ category.name }}</div>
              <div class="table-row-meta">
                <code class="slug-code">{{ category.slug }}</code>
                <span v-if="category.description" class="muted">{{ category.description }}</span>
              </div>
            </div>
            <div class="table-row-actions">
              <button class="btn btn-secondary btn-sm" @click="edit(category)">编辑</button>
              <button class="btn btn-danger btn-sm" @click="askRemove(category.id)">删除</button>
            </div>
          </div>

          <div v-if="categories.length === 0" class="empty-state">
            <div class="empty-state-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A2 2 0 013 12V7a4 4 0 014-4z"/>
              </svg>
            </div>
            <p class="empty-state-text">暂无分类</p>
          </div>
        </div>
      </section>
    </template>
  </section>

  <ConfirmModal
    :visible="confirmVisible"
    :message="confirmMessage"
    confirm-text="删除"
    :danger="true"
    @confirm="confirmRemove"
    @cancel="confirmVisible = false"
  />
  </AdminLayout>
</template>

<style scoped>
.admin-categories {
  max-width: 900px;
  margin: 0 auto;
}

.form-title {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--space-4);
}

.category-form {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  margin-bottom: var(--space-6);
}

.category-list {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.form-group-full {
  grid-column: 1 / -1;
}

.slug-code {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 12px;
  background: var(--color-bg);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
}

.table-row-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: 4px;
}

.form-actions {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  margin-top: var(--space-4);
}

.empty-state-icon svg {
  width: 100%;
  height: 100%;
}
</style>
