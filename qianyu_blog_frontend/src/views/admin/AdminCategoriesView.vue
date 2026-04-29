<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { blogApi } from '@/api/blog'
import type { Category } from '@/types/blog'

const categories = ref<Category[]>([])
const editingId = ref<number>()
const name = ref('')
const slug = ref('')
const description = ref('')
const error = ref('')

async function load() {
  categories.value = await blogApi.adminCategories()
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
    error.value = err instanceof Error ? err.message : '保存失败'
  }
}

async function remove(id: number) {
  if (!window.confirm('确认删除这个分类？')) return
  try {
    await blogApi.deleteCategory(id)
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除失败'
  }
}

onMounted(load)
</script>

<template>
  <section class="card">
    <h1>分类管理</h1>
    <div class="admin-nav">
      <RouterLink to="/admin/posts">文章</RouterLink>
      <RouterLink to="/admin/categories">分类</RouterLink>
    </div>
    <form class="form" @submit.prevent="save">
      <label>名称<input v-model="name" required /></label>
      <label>标识<input v-model="slug" required /></label>
      <label>描述<input v-model="description" /></label>
      <p v-if="error" class="error">{{ error }}</p>
      <div class="actions">
        <button type="submit">{{ editingId ? '保存分类' : '新增分类' }}</button>
        <button v-if="editingId" type="button" class="secondary" @click="resetForm">取消编辑</button>
      </div>
    </form>
    <div class="table-list" style="margin-top: 20px;">
      <div v-for="category in categories" :key="category.id" class="table-row">
        <div>
          <strong>{{ category.name }}</strong>
          <p class="muted">{{ category.slug }} · {{ category.description }}</p>
        </div>
        <div class="actions">
          <button class="secondary" @click="edit(category)">编辑</button>
          <button class="danger" @click="remove(category.id)">删除</button>
        </div>
      </div>
    </div>
  </section>
</template>
