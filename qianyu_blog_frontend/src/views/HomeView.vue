<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { blogApi } from '@/api/blog'
import type { Post } from '@/types/blog'
import { resolveAssetUrl } from '@/constants/siteDefaults'

const posts = ref<Post[]>([])
const latestSpark = ref<Post | null>(null)
const loading = ref(false)
const error = ref('')
const sparkModalVisible = ref(false)

const pinnedPost = computed(() => posts.value.find(post => post.isPinned) ?? null)

const otherPosts = computed(() =>
  posts.value.filter(post => !(pinnedPost.value && post.id === pinnedPost.value.id))
)

function getPostTime(post: Post) {
  return post.createdAt ? new Date(post.createdAt).getTime() : 0
}

const latestPostCard = computed(() => {
  const sorted = [...otherPosts.value].sort((a, b) => getPostTime(b) - getPostTime(a))
  return sorted[0] ?? null
})

const compactPosts = computed(() =>
  otherPosts.value.filter(post => !(latestPostCard.value && post.id === latestPostCard.value.id))
)

function getPostCover(post: Post) {
  return resolveAssetUrl(post.coverImageUrl)
}

function formatDate(dateStr?: string) {
  if (!dateStr) return ''
  return dateStr.slice(0, 10).replace(/-/g, '.')
}

async function loadPosts() {
  loading.value = true
  error.value = ''

  try {
    const [page, categories] = await Promise.all([
      blogApi.publicPosts(),
      blogApi.publicCategories(),
    ])

    posts.value = page.content

    const sparkCategory = categories.find(category => category.name === '碎碎念')
    if (sparkCategory) {
      const sparkPage = await blogApi.publicPosts(sparkCategory.id)
      latestSpark.value = sparkPage.content[0] ?? null
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载文章失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadPosts)
</script>

<template>
  <div class="home">
    <header class="hero">
      <div class="hero-left">
        <div class="hero-kicker">Notes, Code, Light</div>
        <h1 class="hero-title">把日常写成可回看的光。</h1>
      </div>
      <p class="hero-copy">
        这里收纳技术笔记、城市散步、深夜想法与还没整理完的照片。文字负责记住思考，影像负责保存当时的温度。
      </p>
    </header>

    <div v-if="loading" class="loading-state">
      <span>正在加载...</span>
    </div>

    <div v-if="error" class="alert alert-error">
      {{ error }}
    </div>

    <section v-if="!loading" class="bento-grid">
      <article v-if="pinnedPost" class="card feature-card">
        <RouterLink
          :to="`/posts/${pinnedPost.id}`"
          class="card-body feature-body"
        >
          <div class="feature-text">
            <div class="card-meta">
              {{ pinnedPost.category?.name }} / {{ formatDate(pinnedPost.createdAt) }}
            </div>
            <h2>{{ pinnedPost.title }}</h2>
            <p>{{ pinnedPost.summary }}</p>
          </div>
          <div v-if="pinnedPost.tags && pinnedPost.tags.length > 0" class="tag-row">
            <span v-for="tag in pinnedPost.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
        </RouterLink>
        <div v-if="getPostCover(pinnedPost)" class="feature-media">
          <img :src="getPostCover(pinnedPost)" :alt="pinnedPost.title" />
        </div>
      </article>

      <article v-else class="card feature-card">
        <div class="card-body feature-body">
          <div class="feature-text">
            <div class="card-meta">技术长文 / 2026.05.03</div>
            <h2>给个人博客做一次轻量重构</h2>
            <p>从信息密度、阅读路径和视觉节奏三个角度，把一个普通页面改成更适合长期写作的杂志式入口。</p>
          </div>
          <div class="tag-row">
            <span class="tag">CSS Grid</span>
            <span class="tag">Design System</span>
            <span class="tag">Frontend</span>
          </div>
        </div>
        <div class="feature-media">
          <img
            src="https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&fit=crop&w=900&q=82"
            alt="桌面上的笔记本电脑与写作工作区"
          />
        </div>
      </article>

      <article class="card text-card" @click="latestSpark && (sparkModalVisible = true)">
        <div class="card-body">
          <div class="card-meta">碎碎念 / {{ formatDate(latestSpark?.createdAt) || '今日' }}</div>
          <blockquote class="spark-clamp">
            {{ latestSpark?.content || '慢一点，很多答案会自己浮上来。' }}
          </blockquote>
          <span v-if="latestSpark?.content && latestSpark.content.length > 80" class="spark-more">展开全文</span>
        </div>
      </article>

      <RouterLink
        v-if="latestPostCard"
        :to="`/posts/${latestPostCard.id}`"
        class="card compact-card post-card-link latest-card"
      >
        <div class="card-body">
          <div>
            <div class="card-meta">最新文章 / {{ formatDate(latestPostCard.createdAt) }}</div>
            <h2>{{ latestPostCard.title }}</h2>
            <p>{{ latestPostCard.summary }}</p>
          </div>
          <div v-if="latestPostCard.category?.name" class="tag-row">
            <span class="tag">{{ latestPostCard.category.name }}</span>
          </div>
        </div>
      </RouterLink>

      <template v-for="post in compactPosts" :key="post.id">
        <RouterLink
          :to="`/posts/${post.id}`"
          class="card compact-card post-card-link"
        >
          <div class="card-body">
            <div>
              <div class="card-meta">
                {{ post.category?.name }} / {{ formatDate(post.createdAt) }}
              </div>
              <h2>{{ post.title }}</h2>
              <p>{{ post.summary }}</p>
            </div>
          </div>
        </RouterLink>
      </template>

      <template v-if="compactPosts.length < 1">
        <article class="card compact-card">
          <div class="card-body">
            <div>
              <div class="card-meta">阅读 / 12 min</div>
              <h2>把复杂系统拆到能讲清楚</h2>
              <p>写技术文章不是复述实现，而是把混乱的决策路径整理成可复用的判断。</p>
            </div>
          </div>
        </article>
      </template>

      <template v-if="compactPosts.length < 2">
        <article class="card compact-card">
          <div class="card-body">
            <div>
              <div class="card-meta">生活 / 夜行</div>
              <h2>便利店门口的雨</h2>
              <p>玻璃上映着车灯，咖啡有一点烫，城市短暂地安静下来。</p>
            </div>
          </div>
        </article>
      </template>
    </section>

    <section v-if="!loading && posts.length === 0 && !error" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V9a2 2 0 012-2h2a2 2 0 012 2v9a2 2 0 01-2 2h-2z" />
        </svg>
      </div>
      <p>暂无文章</p>
    </section>

    <Teleport to="body">
      <Transition name="spark-modal">
        <div v-if="sparkModalVisible && latestSpark" class="spark-overlay" @click.self="sparkModalVisible = false">
          <div class="spark-dialog">
            <div class="spark-dialog-accent"></div>
            <div class="spark-dialog-meta">碎碎念 / {{ formatDate(latestSpark.createdAt) }}</div>
            <p class="spark-dialog-content">{{ latestSpark.content }}</p>
            <button class="spark-dialog-close" @click="sparkModalVisible = false">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.home {
  width: min(1160px, calc(100% - 32px));
  margin: 0 auto;
  padding: 44px 0 72px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(280px, 0.95fr);
  align-items: end;
  gap: 36px;
  padding: 42px 0 34px;
}

.hero-kicker {
  color: var(--secondary, #8c8c8c);
  font-size: 0.82rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-family: var(--font-sans, Inter, "PingFang SC", sans-serif);
}

.hero-title {
  max-width: 760px;
  margin: 16px 0 0;
  font-family: var(--font-serif, "Noto Serif SC", "Songti SC", serif);
  font-size: clamp(3rem, 8vw, 7.2rem);
  line-height: 0.98;
  font-weight: 700;
  letter-spacing: 0;
  color: var(--primary, #2c2c2c);
}

.hero-copy {
  margin: 0;
  color: #5f5f5b;
  font-family: var(--font-sans, Inter, "PingFang SC", sans-serif);
  font-size: clamp(1rem, 1.5vw, 1.18rem);
  line-height: 1.9;
}

.bento-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
  margin-top: 20px;
}

.card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--radius, 8px);
  background: var(--surface, #ffffff);
  box-shadow: var(--shadow, 0 4px 24px rgba(0, 0, 0, 0.03));
  transition: transform 0.3s var(--ease, cubic-bezier(0.25, 0.8, 0.25, 1)),
    box-shadow 0.3s var(--ease, cubic-bezier(0.25, 0.8, 0.25, 1)),
    border-color 0.3s var(--ease, cubic-bezier(0.25, 0.8, 0.25, 1));
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover, 0 18px 42px rgba(34, 34, 34, 0.08));
  border-color: color-mix(in srgb, var(--primary) 10%, transparent);
}

.card-body {
  padding: 30px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 28px;
  font-family: var(--font-sans, Inter, "PingFang SC", sans-serif);
}

.card-meta {
  color: var(--secondary, #8c8c8c);
  font-size: 0.82rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.card h2 {
  margin: 12px 0 14px;
  font-family: var(--font-serif, "Noto Serif SC", "Songti SC", serif);
  font-size: clamp(1.65rem, 2.4vw, 2.65rem);
  font-weight: 500;
  line-height: 1.16;
  letter-spacing: 0;
  color: var(--primary, #2c2c2c);
}

.card p {
  margin: 0;
  color: #62625e;
  line-height: 1.9;
  font-size: 0.95rem;
}

.feature-card {
  grid-column: span 2;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 0.78fr);
  min-height: 360px;
}

.feature-body {
  text-decoration: none;
  color: inherit;
  cursor: pointer;
}

.feature-body:hover h2 {
  color: var(--secondary, #8c8c8c);
}

.feature-media {
  min-height: 100%;
  background: var(--code-bg);
  overflow: hidden;
}

.feature-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s var(--ease, cubic-bezier(0.25, 0.8, 0.25, 1));
}

.feature-card:hover .feature-media img {
  transform: scale(1.03);
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  border-radius: 999px;
  background: var(--code-bg);
  color: var(--secondary);
  font-size: 0.82rem;
  font-weight: 600;
  padding: 6px 10px;
  font-family: var(--font-sans, Inter, "PingFang SC", sans-serif);
}

.text-card {
  min-height: 360px;
  cursor: pointer;
}

.text-card .card-body {
  height: 100%;
  justify-content: flex-start;
  padding-top: 42px;
}

.text-card blockquote {
  margin: 0;
  font-family: var(--font-serif, "Noto Serif SC", "Songti SC", serif);
  font-size: clamp(1.4rem, 2.4vw, 2rem);
  font-weight: 400;
  line-height: 1.6;
  letter-spacing: 0;
  color: var(--primary, #2c2c2c);
}

.spark-clamp {
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.spark-more {
  display: inline-block;
  margin-top: 8px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--secondary, #8c8c8c);
  letter-spacing: 0.04em;
  transition: color 0.2s ease;
}

.text-card:hover .spark-more {
  color: var(--primary, #2c2c2c);
}

.spark-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.spark-dialog {
  position: relative;
  width: min(520px, 90vw);
  max-height: 80vh;
  overflow-y: auto;
  background: var(--surface, #ffffff);
  border-radius: 16px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.12), 0 4px 16px rgba(0, 0, 0, 0.06);
  padding: 36px 32px 32px;
}

.spark-dialog-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #e7dfcf;
  border-radius: 16px 16px 0 0;
}

.spark-dialog-meta {
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--secondary, #8c8c8c);
  margin-bottom: 20px;
}

.spark-dialog-content {
  margin: 0;
  font-family: var(--font-serif, "Noto Serif SC", "Songti SC", serif);
  font-size: clamp(1.1rem, 2vw, 1.35rem);
  font-weight: 400;
  line-height: 1.8;
  color: var(--primary, #2c2c2c);
  white-space: pre-wrap;
  word-break: break-word;
}

.spark-dialog-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  display: grid;
  place-items: center;
  cursor: pointer;
  color: var(--secondary, #8c8c8c);
  transition: background 0.2s ease, color 0.2s ease;
}

.spark-dialog-close:hover {
  background: var(--base, #f9f9f6);
  color: var(--primary, #2c2c2c);
}

.spark-modal-enter-active,
.spark-modal-leave-active {
  transition: opacity 0.28s ease;
}

.spark-modal-enter-active .spark-dialog,
.spark-modal-leave-active .spark-dialog {
  transition: transform 0.28s var(--ease, cubic-bezier(0.25, 0.8, 0.25, 1)), opacity 0.28s ease;
}

.spark-modal-enter-from,
.spark-modal-leave-to {
  opacity: 0;
}

.spark-modal-enter-from .spark-dialog,
.spark-modal-leave-to .spark-dialog {
  opacity: 0;
  transform: translateY(16px) scale(0.97);
}

.compact-card {
  min-height: 220px;
}

.latest-card {
  background: color-mix(in srgb, var(--surface) 88%, var(--code-bg));
}

.compact-card .card-body {
  height: 100%;
}

.post-card-link {
  text-decoration: none;
  color: inherit;
  display: block;
}

.post-card-link:hover h2 {
  color: var(--secondary, #8c8c8c);
}

.loading-state {
  text-align: center;
  padding: 60px 0;
  color: var(--secondary, #8c8c8c);
  font-family: var(--font-sans, Inter, "PingFang SC", sans-serif);
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: var(--secondary, #8c8c8c);
  font-family: var(--font-sans, Inter, "PingFang SC", sans-serif);
}

.empty-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
}

.empty-icon svg {
  width: 100%;
  height: 100%;
}

@media (max-width: 900px) {
  .home {
    width: min(100% - 24px, 1160px);
    padding-top: 24px;
  }

  .hero {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .bento-grid {
    grid-template-columns: 1fr;
  }

  .feature-card {
    grid-column: span 1;
    grid-template-columns: 1fr;
  }

  .feature-media {
    min-height: auto;
    aspect-ratio: 16 / 10;
  }

  .text-card {
    min-height: 280px;
  }

  .text-card blockquote {
    font-size: clamp(1.6rem, 5vw, 2.4rem);
  }
}

@media (max-width: 560px) {
  .home {
    width: min(100% - 16px, 1160px);
  }

  .hero-title {
    font-size: clamp(2.7rem, 17vw, 4.7rem);
  }

  .card-body {
    padding: 22px;
  }
}
</style>
