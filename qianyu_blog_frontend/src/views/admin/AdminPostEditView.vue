<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { blogApi } from '@/api/blog'
import type { Category, PostPayload, PostStatus } from '@/types/blog'

const route = useRoute()
const router = useRouter()
const categories = ref<Category[]>([])
const title = ref('')
const summary = ref('')
const content = ref('')
const categoryId = ref<number>()
const status = ref<PostStatus>('DRAFT')
const error = ref('')
const postId = computed(() => route.params.id ? Number(route.params.id) : undefined)

async function load() {
  categories.value = await blogApi.adminCategories()
  if (postId.value) {
    const post = await blogApi.adminPost(postId.value)
    title.value = post.title
    summary.value = post.summary
    content.value = post.content
    categoryId.value = post.category.id
    status.value = post.status
  } else if (categories.value.length > 0) {
    categoryId.value = categories.value[0]?.id
  }
}

async function submit() {
  error.value = ''
  if (!categoryId.value) {
    error.value = '请先创建分类'
    return
  }
  const payload: PostPayload = { title: title.value, summary: summary.value, content: content.value, categoryId: categoryId.value, status: status.value }
  try {
    if (postId.value) {
      await blogApi.updatePost(postId.value, payload)
    } else {
      await blogApi.createPost(payload)
    }
    await router.push('/admin/posts')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败'
  }
}

onMounted(load)
</script>

<template>
  <section class="card">
    <h1>{{ postId ? '编辑文章' : '新建文章' }}</h1>
    <form class="form" @submit.prevent="submit">
      <label>标题<input v-model="title" required /></label>
      <label>摘要<textarea v-model="summary" required /></label>
      <label>内容<textarea v-model="content" required /></label>
      <label>分类
        <select v-model="categoryId" required>
          <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
        </select>
      </label>
      <label>状态
        <select v-model="status">
          <option value="DRAFT">草稿</option>
          <option value="PUBLISHED">发布</option>
        </select>
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit">保存</button>
    </form>
  </section>
</template>
