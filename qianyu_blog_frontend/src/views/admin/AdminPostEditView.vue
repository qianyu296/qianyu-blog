<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { blogApi } from '@/api/blog'
import type { Category, MediaAsset, PostPayload, PostStatus } from '@/types/blog'
import AdminLayout from '@/components/AdminLayout.vue'
import { Marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'

const route = useRoute()
const router = useRouter()
const categories = ref<Category[]>([])
const mediaAssets = ref<MediaAsset[]>([])
const title = ref('')
const summary = ref('')
const content = ref('')
const coverImageUrl = ref('')
const categoryId = ref<number>()
const status = ref<PostStatus>('DRAFT')
const isPinned = ref(false)
const tagsInput = ref('')
const showPreview = ref(false)
const polishing = ref(false)
const polishPreviewVisible = ref(false)
const polishedMarkdown = ref('')
const error = ref('')
const loading = ref(false)
const postId = computed(() => route.params.id ? Number(route.params.id) : undefined)

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

const renderedPreview = computed(() => {
  if (!content.value) return ''
  return marked.parse(content.value) as string
})

const renderedPolishedPreview = computed(() => {
  if (!polishedMarkdown.value) return ''
  return marked.parse(polishedMarkdown.value) as string
})

const coverPreviewUrl = computed(() => resolveAssetUrl(coverImageUrl.value))

function resolveAssetUrl(url?: string) {
  if (!url) return ''
  return url.startsWith('http')
    ? url
    : `${import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}${url}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [categoryData, mediaData] = await Promise.all([
      blogApi.adminCategories(),
      blogApi.adminMediaAssets(),
    ])
    categories.value = categoryData
    mediaAssets.value = mediaData

    if (postId.value) {
      const post = await blogApi.adminPost(postId.value)
      title.value = post.title
      summary.value = post.summary
      content.value = post.content
      coverImageUrl.value = post.coverImageUrl || ''
      categoryId.value = post.category.id
      status.value = post.status
      isPinned.value = post.isPinned
      tagsInput.value = (post.tags || []).join(', ')
    } else if (categories.value.length > 0) {
      categoryId.value = categories.value[0]?.id
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '页面数据加载失败，请刷新重试'
  } finally {
    loading.value = false
  }
}

function selectCover(asset: MediaAsset) {
  coverImageUrl.value = asset.fileUrl
}

async function submit() {
  error.value = ''
  if (!categoryId.value) {
    error.value = '当前没有可用分类，请先到「内容分区」页面创建一个分类'
    return
  }
  if (!title.value.trim()) {
    error.value = '文章标题不能为空，请填写标题后再保存'
    return
  }
  const tags = tagsInput.value
    .split(',')
    .map(t => t.trim())
    .filter(t => t.length > 0)

  const payload: PostPayload = {
    title: title.value,
    summary: summary.value,
    content: content.value,
    coverImageUrl: coverImageUrl.value.trim() || undefined,
    categoryId: categoryId.value,
    status: status.value,
    isPinned: isPinned.value,
    tags,
  }

  try {
    if (postId.value) {
      await blogApi.updatePost(postId.value, payload)
    } else {
      await blogApi.createPost(payload)
    }
    await router.push('/admin/posts')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '文章保存失败，请检查网络后重试'
  }
}

async function polishContent() {
  error.value = ''
  if (!content.value.trim()) {
    error.value = '请先填写正文，再进行 AI 润色'
    return
  }

  polishing.value = true
  try {
    const result = await blogApi.polishPost({
      title: title.value,
      summary: summary.value,
      content: content.value,
    })
    polishedMarkdown.value = result.content
    polishPreviewVisible.value = true
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'AI 润色失败，请稍后重试'
  } finally {
    polishing.value = false
  }
}

function applyPolishedContent() {
  if (!polishedMarkdown.value) return
  content.value = polishedMarkdown.value
  showPreview.value = true
  polishPreviewVisible.value = false
}

onMounted(load)
</script>

<template>
  <AdminLayout>
  <section class="admin-edit animate-fade-in">
    <div class="toolbar">
      <div class="toolbar-left">
        <h1>{{ postId ? '编辑文章' : '新建文章' }}</h1>
      </div>
      <div class="toolbar-right">
        <button class="btn btn-ghost" @click="showPreview = !showPreview">
          {{ showPreview ? '编辑' : '预览' }}
        </button>
        <RouterLink class="btn btn-secondary" to="/admin/posts">取消</RouterLink>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <span>加载中...</span>
    </div>

    <form v-if="!loading" class="edit-form" @submit.prevent="submit">
      <div class="form-grid">
        <div class="form-main">
          <div class="form-group">
            <label class="form-label" for="title">标题</label>
            <input
              id="title"
              v-model="title"
              type="text"
              class="form-input"
              placeholder="输入文章标题"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label" for="summary">摘要</label>
            <textarea
              id="summary"
              v-model="summary"
              class="form-textarea"
              placeholder="输入文章摘要"
              rows="2"
              required
            />
          </div>

          <div class="form-group">
            <div class="content-header">
              <label class="form-label">内容 (Markdown)</label>
            </div>
            <div v-if="!showPreview">
              <textarea
                v-model="content"
                class="form-textarea content-editor"
                placeholder="支持 Markdown 语法..."
                rows="14"
                required
              />
            </div>
            <div v-else class="content-preview markdown-body" v-html="renderedPreview"></div>
          </div>
        </div>

        <div class="form-sidebar">
          <div class="sidebar-card">
            <h3 class="sidebar-title">发布设置</h3>

            <div class="form-group">
              <label class="form-label" for="category">分类</label>
              <select id="category" v-model="categoryId" class="form-select" required>
                <option value="" disabled>选择分类</option>
                <option v-for="category in categories" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label" for="status">状态</label>
              <select id="status" v-model="status" class="form-select">
                <option value="DRAFT">草稿</option>
                <option value="PUBLISHED">发布</option>
              </select>
            </div>

            <div class="form-group">
              <label class="checkbox-label">
                <input v-model="isPinned" type="checkbox" />
                <span>文章置顶</span>
              </label>
            </div>
          </div>

          <div class="sidebar-card">
            <h3 class="sidebar-title">封面</h3>

            <div class="form-group">
              <label class="form-label" for="coverImageUrl">封面 URL</label>
              <input
                id="coverImageUrl"
                v-model="coverImageUrl"
                type="text"
                class="form-input"
                placeholder="/api/public/media/{id}/file"
              />
              <span class="form-hint">可直接粘贴 URL，或从下方媒体中心资源中选择。</span>
            </div>

            <div v-if="coverPreviewUrl" class="cover-preview">
              <img :src="coverPreviewUrl" :alt="title || '文章封面预览'" />
            </div>

            <div class="cover-actions">
              <button type="button" class="btn btn-secondary btn-sm" @click="coverImageUrl = ''">清空封面</button>
              <RouterLink class="btn btn-secondary btn-sm" to="/admin/media">打开媒体中心</RouterLink>
            </div>

            <div v-if="mediaAssets.length > 0" class="cover-gallery">
              <button
                v-for="asset in mediaAssets.slice(0, 8)"
                :key="asset.id"
                type="button"
                class="cover-option"
                :class="{ active: coverImageUrl === asset.fileUrl }"
                @click="selectCover(asset)"
              >
                <img :src="resolveAssetUrl(asset.fileUrl)" :alt="asset.altText || asset.displayName || asset.originalFileName" />
                <span>{{ asset.displayName || asset.originalFileName }}</span>
              </button>
            </div>
          </div>

          <div class="sidebar-card">
            <h3 class="sidebar-title">标签</h3>
            <div class="form-group">
              <input
                v-model="tagsInput"
                type="text"
                class="form-input"
                placeholder="多个标签用逗号分隔"
              />
              <span class="form-hint">如: Java, Spring, 后端</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="error" class="alert alert-error">
        {{ error }}
      </div>

      <div class="form-actions">
        <button type="button" class="btn btn-secondary btn-lg" :disabled="polishing" @click="polishContent">
          {{ polishing ? 'AI 润色中...' : 'AI 润色' }}
        </button>
        <button type="submit" class="btn btn-primary btn-lg">
          保存文章
        </button>
      </div>
    </form>
    <Teleport to="body">
      <Transition name="polish-modal">
        <div v-if="polishPreviewVisible" class="polish-overlay" @click.self="polishPreviewVisible = false">
          <div class="polish-dialog">
            <div class="polish-dialog-header">
              <div>
                <div class="polish-dialog-kicker">AI 润色结果</div>
                <h3>预览润色后的正文</h3>
              </div>
              <button type="button" class="dialog-close" @click="polishPreviewVisible = false">关闭</button>
            </div>
            <div class="polish-dialog-body markdown-body" v-html="renderedPolishedPreview"></div>
            <div class="polish-dialog-actions">
              <button type="button" class="btn btn-secondary" @click="polishPreviewVisible = false">稍后再看</button>
              <button type="button" class="btn btn-primary" @click="applyPolishedContent">替换正文</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </section>
  </AdminLayout>
</template>

<style scoped>
.admin-edit {
  max-width: 1180px;
  margin: 0 auto;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--space-6);
}

.form-main {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.form-sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.sidebar-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}

.sidebar-title {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
  margin-bottom: var(--space-4);
  color: var(--color-text);
}

.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
}

.content-editor {
  min-height: 320px;
  font-family: 'SF Mono', Monaco, 'Inconsolata', monospace;
  font-size: 14px;
  line-height: 1.6;
}

.content-preview {
  min-height: 320px;
  padding: var(--space-4);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow-y: auto;
}

.form-hint {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: var(--space-1);
  display: block;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  font-size: 14px;
  padding-top: var(--space-2);
}

.checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--color-accent);
}

.cover-preview {
  margin-top: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.cover-preview img {
  width: 100%;
  height: 160px;
  display: block;
  object-fit: cover;
}

.cover-actions {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
  margin-top: var(--space-4);
}

.cover-gallery {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.cover-option {
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  text-align: left;
}

.cover-option.active {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 2px var(--color-accent-subtle);
}

.cover-option img {
  width: 100%;
  height: 86px;
  display: block;
  object-fit: cover;
}

.cover-option span {
  display: block;
  padding: var(--space-2);
  font-size: 12px;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-actions {
  display: flex;
  gap: var(--space-4);
  align-items: center;
}

.polish-overlay {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.42);
  backdrop-filter: blur(8px);
}

.polish-dialog {
  width: min(880px, 100%);
  max-height: min(84vh, 920px);
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: 0 24px 72px rgba(15, 23, 42, 0.24);
  overflow: hidden;
}

.polish-dialog-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5);
  border-bottom: 1px solid var(--color-border);
}

.polish-dialog-header h3 {
  margin: 6px 0 0;
  font-size: 20px;
  color: var(--color-text);
}

.polish-dialog-kicker {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
}

.dialog-close {
  border: 1px solid var(--color-border);
  background: var(--color-surface-muted, var(--color-bg));
  color: var(--color-text);
  border-radius: var(--radius-md);
  padding: 8px 14px;
  cursor: pointer;
}

.polish-dialog-body {
  padding: var(--space-5);
  overflow-y: auto;
  background: var(--color-bg);
}

.polish-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  padding: var(--space-5);
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}

.polish-modal-enter-active,
.polish-modal-leave-active {
  transition: opacity 0.22s ease;
}

.polish-modal-enter-active .polish-dialog,
.polish-modal-leave-active .polish-dialog {
  transition: transform 0.22s ease, opacity 0.22s ease;
}

.polish-modal-enter-from,
.polish-modal-leave-to {
  opacity: 0;
}

.polish-modal-enter-from .polish-dialog,
.polish-modal-leave-to .polish-dialog {
  opacity: 0;
  transform: translateY(10px) scale(0.985);
}

.markdown-body {
  font-size: 15px;
  line-height: 1.7;
  color: var(--color-text);
}

.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  font-weight: 600;
  margin: var(--space-4) 0 var(--space-2);
}

.markdown-body :deep(h2) { font-size: 18px; }
.markdown-body :deep(h3) { font-size: 16px; }

.markdown-body :deep(p) { margin-bottom: var(--space-3); }

.markdown-body :deep(code) {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
  background: var(--color-bg);
  padding: 2px 5px;
  border-radius: var(--radius-sm);
  color: #D63384;
}

.markdown-body :deep(pre) {
  background: #1E1E1E;
  border-radius: var(--radius-md);
  padding: var(--space-4);
  overflow-x: auto;
}

.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
  color: #D4D4D4;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: var(--space-5);
  margin-bottom: var(--space-3);
}

.markdown-body :deep(blockquote) {
  margin: var(--space-3) 0;
  padding: var(--space-3) var(--space-4);
  border-left: 3px solid var(--color-accent);
  background: var(--color-bg);
  color: var(--color-text-secondary);
}

@media (max-width: 960px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-sidebar {
    order: -1;
  }

  .polish-dialog {
    max-height: 88vh;
  }

  .polish-dialog-header,
  .polish-dialog-actions {
    padding: var(--space-4);
  }

  .polish-dialog-actions {
    flex-direction: column-reverse;
  }
}
</style>
