<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { blogApi } from '@/api/blog'
import type { Post } from '@/types/blog'

const posts = ref<Post[]>([])
const error = ref('')

async function load() {
  try {
    posts.value = (await blogApi.adminPosts()).content
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  }
}

async function remove(id: number) {
  if (!window.confirm('确认删除这篇文章？')) return
  await blogApi.deletePost(id)
  await load()
}

onMounted(load)
</script>

<template>
  <section class="card">
    <div class="toolbar">
      <h1>文章管理</h1>
      <RouterLink class="button" to="/admin/posts/new">新建文章</RouterLink>
    </div>
    <div class="admin-nav">
      <RouterLink to="/admin/posts">文章</RouterLink>
      <RouterLink to="/admin/categories">分类</RouterLink>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="table-list">
      <div v-for="post in posts" :key="post.id" class="table-row">
        <div>
          <strong>{{ post.title }}</strong>
          <p class="muted">{{ post.category.name }} · {{ post.status }}</p>
        </div>
        <div class="admin-nav">
          <RouterLink class="button secondary" :to="`/admin/posts/${post.id}/edit`">编辑</RouterLink>
          <button class="danger" @click="remove(post.id)">删除</button>
        </div>
      </div>
      <p v-if="posts.length === 0" class="muted">暂无文章</p>
    </div>
  </section>
</template>
