<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { blogApi } from '@/api/blog'
import type { Post, SiteSettings } from '@/types/blog'
import { Marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'

const route = useRoute()
const router = useRouter()
const post = ref<Post>()
const siteSettings = ref<SiteSettings>()
const error = ref('')
const readProgress = ref(0)
const showBackTop = ref(false)

const marked = new Marked(
  markedHighlight({
    langPrefix: 'hljs language-',
    highlight(code: string, lang: string) {
      const language = hljs.getLanguage(lang) ? lang : 'plaintext'
      return hljs.highlight(code, { language }).value
    }
  })
)
marked.setOptions({ breaks: true, gfm: true })

const renderedContent = computed(() => {
  if (!post.value?.content) return ''
  return marked.parse(post.value.content) as string
})

const readingTime = computed(() => {
  if (!post.value?.content) return 0
  const words = post.value.content.replace(/<[^>]+>/g, '').length
  return Math.ceil(words / 400)
})

function handleScroll() {
  const scrollTop = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  readProgress.value = docHeight > 0 ? (scrollTop / docHeight) * 100 : 0
  showBackTop.value = scrollTop > 400
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function resolveAssetUrl(url?: string) {
  if (!url) return ''
  return url.startsWith('http')
    ? url
    : `${import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}${url}`
}

const coverImageUrl = computed(() =>
  resolveAssetUrl(post.value?.coverImageUrl || siteSettings.value?.defaultPostCoverUrl),
)

onMounted(async () => {
  try {
    siteSettings.value = await blogApi.publicSiteSettings()
    post.value = await blogApi.publicPost(Number(route.params.id))
  } catch (err) {
    error.value = err instanceof Error ? err.message : '文章加载失败'
  }
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <article class="article animate-fade-in">
    <div class="progress-bar" :style="{ width: `${readProgress}%` }"></div>

    <div v-if="error" class="alert alert-error">
      {{ error }}
    </div>

    <div v-if="!post && !error" class="loading">
      <span>加载中...</span>
    </div>

    <template v-if="post">
      <header class="article-header">
        <div class="article-meta">
          <span v-if="post.isPinned" class="pinned-badge">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor">
              <path d="M16 12V4h1V2H7v2h1v8l-2 2v2h5v6l1 1 1-1v-6h5v-2l-2-2z"/>
            </svg>
            置顶
          </span>
          <span class="badge badge-published">{{ post.category.name }}</span>
          <span class="muted">{{ post.createdAt?.slice(0, 10) }}</span>
          <span class="read-time">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
            </svg>
            {{ readingTime }} 分钟阅读
          </span>
        </div>

        <h1 class="article-title">{{ post.title }}</h1>
        <p class="article-summary">{{ post.summary }}</p>

        <div v-if="post.tags && post.tags.length > 0" class="article-tags">
          <span v-for="tag in post.tags" :key="tag" class="article-tag">{{ tag }}</span>
        </div>
      </header>

      <div v-if="coverImageUrl" class="article-cover">
        <img :src="coverImageUrl" :alt="post.title" />
      </div>

      <div class="article-content markdown-body" v-html="renderedContent"></div>

      <footer class="article-footer">
        <div class="footer-divider"></div>
        <div class="footer-content">
          <RouterLink to="/" class="btn btn-secondary">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 12H5M12 19l-7-7 7-7"/>
            </svg>
            返回首页
          </RouterLink>
          <div v-if="post.tags && post.tags.length > 0" class="footer-tags">
            <span class="footer-tag-label">标签：</span>
            <span v-for="tag in post.tags" :key="tag" class="footer-tag">{{ tag }}</span>
          </div>
        </div>
      </footer>
    </template>

    <Transition name="fade">
      <button v-if="showBackTop" class="back-top" @click="scrollToTop" aria-label="返回顶部">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M18 15l-6-6-6 6"/>
        </svg>
      </button>
    </Transition>

    <Transition name="fade">
      <button v-if="showBackTop" class="back-nav" @click="router.back()" aria-label="返回">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
      </button>
    </Transition>
  </article>
</template>

<style scoped>
.article {
  max-width: var(--content-width);
  margin: 0 auto;
  position: relative;
}

.progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  background: var(--color-accent);
  z-index: 1000;
  transition: width 0.1s ease-out;
}

.article-header {
  margin-bottom: var(--space-12);
  padding-bottom: var(--space-8);
  border-bottom: 1px solid var(--color-border);
}

.article-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  flex-wrap: wrap;
}

.article-title {
  font-family: var(--font-heading);
  font-size: 42px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.15;
  margin-bottom: var(--space-5);
  color: var(--color-text);
}

.article-summary {
  font-size: 18px;
  line-height: 1.7;
  color: var(--color-text-secondary);
  margin-bottom: var(--space-5);
  padding: var(--space-5);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  border-left: 4px solid var(--color-accent);
}

.read-time {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-muted);
  padding: 4px 10px;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border);
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.article-tag {
  font-size: 13px;
  padding: 6px 14px;
  background: var(--color-accent-subtle);
  color: var(--color-accent);
  border-radius: var(--radius-full);
  font-weight: 500;
  border: 1px solid var(--color-accent-subtle);
  transition: all var(--duration-fast) var(--ease-out);
}

.article-tag:hover {
  background: var(--color-accent);
  color: white;
  transform: translateY(-1px);
}

.article-content {
  font-size: 17px;
  line-height: 1.85;
  color: var(--color-text);
}

.article-cover {
  margin-bottom: var(--space-10);
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid var(--color-border);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
}

.article-cover img {
  display: block;
  width: 100%;
  max-height: 420px;
  object-fit: cover;
}

.article-content :deep(h2) {
  font-family: var(--font-heading);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin-top: var(--space-12);
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-3);
  border-bottom: 2px solid var(--color-border);
  color: var(--color-text);
}

.article-content :deep(h3) {
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 600;
  margin-top: var(--space-10);
  margin-bottom: var(--space-4);
  color: var(--color-text);
}

.article-content :deep(h4) {
  font-family: var(--font-heading);
  font-size: 17px;
  font-weight: 600;
  margin-top: var(--space-8);
  margin-bottom: var(--space-3);
  color: var(--color-text);
}

.article-content :deep(p) {
  margin-bottom: var(--space-5);
}

.article-content :deep(ul),
.article-content :deep(ol) {
  margin-bottom: var(--space-5);
  padding-left: var(--space-6);
}

.article-content :deep(li) {
  margin-bottom: var(--space-2);
}

.article-content :deep(li)::marker {
  color: var(--color-accent);
}

.article-content :deep(code) {
  font-family: 'SF Mono', Monaco, 'Inconsolata', monospace;
  font-size: 14px;
  background: var(--color-bg);
  padding: 3px 8px;
  border-radius: var(--radius-sm);
  color: var(--color-accent);
  font-weight: 500;
}

.article-content :deep(pre) {
  position: relative;
  background: #1E293B;
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  overflow-x: auto;
  margin: var(--space-6) 0;
  border: 1px solid #334155;
}

.article-content :deep(pre)::before {
  content: '';
  position: absolute;
  top: 12px;
  left: 16px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #EF4444;
  box-shadow: 20px 0 0 #F59E0B, 40px 0 0 #22C55E;
}

.article-content :deep(pre code) {
  background: none;
  padding: var(--space-4) 0 0 0;
  color: #E5E7EB;
  font-size: 14px;
  line-height: 1.7;
  font-weight: 400;
  display: block;
  padding-top: var(--space-6);
}

.article-content :deep(blockquote) {
  margin: var(--space-6) 0;
  padding: var(--space-5) var(--space-6);
  border-radius: 0 var(--radius-lg) var(--radius-lg) 0;
  background: var(--color-bg);
  border-left: 4px solid var(--color-accent);
  color: var(--color-text);
}

.article-content :deep(blockquote p) {
  margin-bottom: 0;
  font-style: italic;
  color: var(--color-text-secondary);
}

.article-content :deep(a) {
  color: var(--color-accent);
  text-decoration: none;
  border-bottom: 2px solid var(--color-accent-subtle);
  transition: all var(--duration-fast) var(--ease-out);
}

.article-content :deep(a:hover) {
  border-bottom-color: var(--color-accent);
}

.article-content :deep(hr) {
  border: none;
  height: 1px;
  background: var(--color-border);
  margin: var(--space-12) 0;
}

.article-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: var(--space-6) 0;
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.article-content :deep(th),
.article-content :deep(td) {
  padding: var(--space-3) var(--space-4);
  text-align: left;
  border-bottom: 1px solid var(--color-border);
}

.article-content :deep(th) {
  background: var(--color-bg);
  font-weight: 600;
  color: var(--color-text);
}

.article-content :deep(tr:last-child td) {
  border-bottom: none;
}

.article-content :deep(img) {
  max-width: 100%;
  border-radius: var(--radius-lg);
  margin: var(--space-6) 0;
}

.article-footer {
  margin-top: var(--space-16);
  padding-top: var(--space-8);
}

.footer-divider {
  height: 1px;
  background: var(--color-border);
  margin-bottom: var(--space-8);
}

.footer-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.footer-tags {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.footer-tag-label {
  font-size: 13px;
  color: var(--color-text-muted);
}

.footer-tag {
  font-size: 12px;
  padding: 4px 10px;
  background: var(--color-bg);
  color: var(--color-text-secondary);
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border);
}

.back-top {
  position: fixed;
  bottom: var(--space-8);
  right: var(--space-8);
  width: 44px;
  height: 44px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  color: var(--color-text);
}

.back-top:hover {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: white;
  transform: translateY(-2px);
}

.back-nav {
  position: fixed;
  bottom: var(--space-8);
  left: var(--space-8);
  width: 44px;
  height: 44px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  color: var(--color-text);
}

.back-nav:hover {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: white;
  transform: translateY(-2px);
}

.fade-enter-active,
.fade-leave-active {
  transition: all var(--duration-normal) var(--ease-out);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@media (max-width: 640px) {
  .article-title {
    font-size: 28px;
  }

  .article-summary {
    font-size: 16px;
    padding: var(--space-4);
  }

  .article-content {
    font-size: 16px;
    line-height: 1.8;
  }

  .article-content :deep(h2) {
    font-size: 22px;
  }

  .article-content :deep(pre) {
    margin: var(--space-4) calc(-1 * var(--space-4));
    border-radius: 0;
  }

  .back-top {
    bottom: var(--space-4);
    right: var(--space-4);
    width: 40px;
    height: 40px;
  }

  .back-nav {
    bottom: var(--space-4);
    left: var(--space-4);
    width: 40px;
    height: 40px;
  }

  .footer-content {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
