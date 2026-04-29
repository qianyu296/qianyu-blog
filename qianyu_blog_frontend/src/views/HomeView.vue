<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { blogApi } from '@/api/blog'
import type { Category, Post } from '@/types/blog'

const categories = ref<Category[]>([])
const posts = ref<Post[]>([])
const selectedCategoryId = ref<number | undefined>()
const loading = ref(false)
const error = ref('')

async function loadPosts() {
  loading.value = true
  error.value = ''
  try {
    const page = await blogApi.publicPosts(selectedCategoryId.value)
    posts.value = page.content
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载文章失败'
  } finally {
    loading.value = false
  }
}

async function selectCategory(id?: number) {
  selectedCategoryId.value = id
  await loadPosts()
}

onMounted(async () => {
  try {
    categories.value = await blogApi.publicCategories()
  } catch {
    categories.value = []
  }
  await loadPosts()
})
</script>

<template>
  <section class="grid">
    <div class="card">
      <p class="muted">记录学习、开发和生活片段</p>
      <h1>千语博客</h1>
      <div class="toolbar">
        <div class="admin-nav">
          <button class="secondary" @click="selectCategory()">全部</button>
          <button v-for="category in categories" :key="category.id" class="secondary" @click="selectCategory(category.id)">
            {{ category.name }}
          </button>
        </div>
      </div>
    </div>

    <p v-if="loading">正在加载...</p>
    <p v-if="error" class="error">{{ error }}</p>

    <div class="post-list">
      <article v-for="post in posts" :key="post.id" class="card post-item">
        <h2><RouterLink :to="`/posts/${post.id}`">{{ post.title }}</RouterLink></h2>
        <p class="muted">{{ post.category.name }} · {{ post.createdAt?.slice(0, 10) }}</p>
        <p>{{ post.summary }}</p>
      </article>
      <div v-if="!loading && posts.length === 0" class="card muted">暂无文章</div>
    </div>
  </section>
</template>
