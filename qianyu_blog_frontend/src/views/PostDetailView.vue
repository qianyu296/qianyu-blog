<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { blogApi } from '@/api/blog'
import type { Post } from '@/types/blog'

const route = useRoute()
const post = ref<Post>()
const error = ref('')

onMounted(async () => {
  try {
    post.value = await blogApi.publicPost(Number(route.params.id))
  } catch (err) {
    error.value = err instanceof Error ? err.message : '文章加载失败'
  }
})
</script>

<template>
  <article class="card">
    <p v-if="error" class="error">{{ error }}</p>
    <template v-if="post">
      <p class="muted">{{ post.category.name }} · {{ post.createdAt?.slice(0, 10) }}</p>
      <h1>{{ post.title }}</h1>
      <p class="muted">{{ post.summary }}</p>
      <div class="content">{{ post.content }}</div>
    </template>
  </article>
</template>

<style scoped>
.content {
  margin-top: 24px;
  white-space: pre-wrap;
  line-height: 1.8;
}
</style>
