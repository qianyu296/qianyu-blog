<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { blogApi } from '@/api/blog'
import type { Post } from '@/types/blog'

const router = useRouter()
const posts = ref<Post[]>([])
const loading = ref(false)
const error = ref('')

const demoPosts: Post[] = [
  {
    id: 1,
    title: 'Vue 3 Composition API 深度实践',
    summary: '从 Options API 到 Composition API 的思维转变，以及在大型项目中的实战经验总结。涵盖 ref、reactive、computed 的最佳使用场景。',
    content: '',
    status: 'PUBLISHED',
    isPinned: false,
    category: { id: 1, name: '前端开发', slug: 'frontend' },
    tags: ['Vue', 'TypeScript'],
    createdAt: '2026.04.28',
  },
  {
    id: 2,
    title: 'Spring Boot 微服务架构设计思考',
    summary: '在实际业务中落地微服务架构的踩坑记录，包括服务拆分原则、通信方式选型以及分布式事务的处理策略。',
    content: '',
    status: 'PUBLISHED',
    isPinned: false,
    category: { id: 2, name: '后端架构', slug: 'backend' },
    tags: ['Java', '微服务'],
    createdAt: '2026.04.15',
  },
  {
    id: 3,
    title: '写代码之外：关于创造力的碎片笔记',
    summary: '编程之外的思考——关于阅读、写作和创造力之间的微妙联系。技术人如何保持对世界的好奇心。',
    content: '',
    status: 'PUBLISHED',
    isPinned: false,
    category: { id: 3, name: '随笔', slug: 'essay' },
    tags: ['思考', '生活'],
    createdAt: '2026.03.22',
  },
  {
    id: 4,
    title: '从零搭建个人博客系统的完整记录',
    summary: '记录搭建这个博客的全过程：技术选型、架构设计、部署方案，以及在这个过程中做出的每一个取舍。',
    content: '',
    status: 'PUBLISHED',
    isPinned: false,
    category: { id: 4, name: '项目实战', slug: 'project' },
    tags: ['博客', '全栈'],
    createdAt: '2026.03.08',
  },
]

function formatDate(dateStr?: string): string {
  if (!dateStr) return ''
  const raw = dateStr.slice(0, 10)
  return raw.replace(/-/g, '.')
}

function estimateReadTime(post: Post): string {
  const text = post.content || post.summary || ''
  const charCount = text.replace(/<[^>]+>/g, '').length
  const minutes = Math.max(1, Math.ceil(charCount / 400))
  return `${minutes} min read`
}

function getFirstTag(post: Post): string {
  if (post.tags && post.tags.length > 0) return post.tags[0] ?? ''
  return post.category?.name ?? ''
}

function navigateToPost(post: Post) {
  router.push(`/posts/${post.id}`)
}

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const page = await blogApi.publicPosts()
    posts.value = page.content
  } catch {
    posts.value = demoPosts
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="archive animate-fade-in">
    <header class="archive-header">
      <div class="header-left">
        <span class="section-kicker">Archive</span>
        <h1 class="archive-title">近期文章</h1>
      </div>
      <p class="header-desc">
        技术与生活混排，保留思考发生时的上下文。
      </p>
    </header>

    <div v-if="loading" class="loading">
      <span>正在加载...</span>
    </div>

    <div v-if="error" class="alert alert-error">
      {{ error }}
    </div>

    <div v-if="!loading" class="archive-grid">
      <article
        v-for="post in posts"
        :key="post.id"
        class="card archive-card"
        role="link"
        tabindex="0"
        @click="navigateToPost(post)"
        @keydown.enter="navigateToPost(post)"
      >
        <div class="card-body">
          <div class="card-top">
            <span class="card-meta">
              {{ post.category?.name }} / {{ formatDate(post.createdAt) }}
            </span>
            <h2>{{ post.title }}</h2>
            <p class="card-desc">{{ post.summary }}</p>
          </div>

          <div class="article-footer">
            <span class="read-time">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <path d="M12 6v6l4 2" />
              </svg>
              {{ estimateReadTime(post) }}
            </span>
            <span class="footer-tag">{{ getFirstTag(post) }}</span>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.archive {
  max-width: 1160px;
  margin: 0 auto;
}

.archive-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 48px;
  padding-bottom: 32px;
  border-bottom: 1px solid var(--line);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.archive-title {
  font-family: var(--font-serif);
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 700;
  line-height: 1.1;
  color: var(--primary);
  margin: 0;
}

.header-desc {
  color: var(--secondary);
  font-size: 0.95rem;
  line-height: 1.6;
  max-width: 340px;
  text-align: right;
}

.archive-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.archive-card {
  min-height: 280px;
  cursor: pointer;
}

.archive-card .card-body {
  height: 100%;
}

.card-top {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.archive-card h2 {
  font-family: var(--font-serif);
  font-size: clamp(1.15rem, 1.8vw, 1.45rem);
  font-weight: 600;
  line-height: 1.3;
  color: var(--primary);
  margin: 0;
  transition: color 0.25s var(--ease);
}

.archive-card:hover h2 {
  color: var(--color-accent);
}

.card-desc {
  font-size: 0.92rem;
  line-height: 1.7;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}

.article-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid var(--line);
  margin-top: auto;
}

.read-time {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.82rem;
  color: var(--secondary);
  font-weight: 500;
}

.read-time svg {
  flex-shrink: 0;
}

.footer-tag {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  background: #f5f5f0;
  color: #777771;
  font-size: 0.78rem;
  font-weight: 600;
}

@media (max-width: 900px) {
  .archive-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-desc {
    text-align: left;
    max-width: none;
  }

  .archive-grid {
    grid-template-columns: 1fr;
  }
}
</style>
