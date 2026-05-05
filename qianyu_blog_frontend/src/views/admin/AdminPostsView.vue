<script setup lang="ts">
import { onMounted, ref, computed, nextTick } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { blogApi } from '@/api/blog'
import type { Category, Post, PostPayload, PostStatus } from '@/types/blog'
import { Marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'

/* ------------------------------------------------------------------ */
/* Marked + highlight.js setup                                         */
/* ------------------------------------------------------------------ */
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

/* ------------------------------------------------------------------ */
/* State                                                                */
/* ------------------------------------------------------------------ */
const posts = ref<Post[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const showPreview = ref(false)
const polishing = ref(false)
const polishPreviewVisible = ref(false)
const polishedMarkdown = ref('')
const editingPostId = ref<number | null>(null)
const confirmVisible = ref(false)
const confirmMessage = ref('')
let pendingDeleteId: number | null = null

/* Form fields */
const formTitle = ref('')
const formSummary = ref('')
const formContent = ref('')
const formCategoryId = ref<number>(0)
const formTags = ref('')
const formStatus = ref<PostStatus>('DRAFT')
const formPinned = ref(false)

/* ------------------------------------------------------------------ */
/* Computed                                                             */
/* ------------------------------------------------------------------ */
const renderedHtml = computed(() => {
  if (!formContent.value) return ''
  return marked.parse(formContent.value) as string
})

const renderedPolishedHtml = computed(() => {
  if (!polishedMarkdown.value) return ''
  return marked.parse(polishedMarkdown.value) as string
})

/* ------------------------------------------------------------------ */
/* Data loading                                                         */
/* ------------------------------------------------------------------ */
async function loadPosts() {
  loading.value = true
  error.value = ''
  try {
    posts.value = (await blogApi.adminPosts()).content.filter(p => p.category?.name !== '碎碎念')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载文章失败'
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await blogApi.adminCategories()
  } catch {
    /* silent */
  }
}

/* ------------------------------------------------------------------ */
/* Form helpers                                                         */
/* ------------------------------------------------------------------ */
function resetForm() {
  editingPostId.value = null
  formTitle.value = ''
  formSummary.value = ''
  formContent.value = ''
  formCategoryId.value = 0
  formTags.value = ''
  formStatus.value = 'DRAFT'
  formPinned.value = false
  showPreview.value = false
  polishedMarkdown.value = ''
  polishPreviewVisible.value = false
}

function editPost(post: Post) {
  editingPostId.value = post.id
  formTitle.value = post.title
  formSummary.value = post.summary
  formContent.value = post.content
  formCategoryId.value = post.category.id
  formTags.value = post.tags.join(', ')
  formStatus.value = post.status
  formPinned.value = post.isPinned
  showPreview.value = false
  /* Scroll to the writing canvas */
  nextTick(() => {
    document.querySelector('.writing-canvas')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

/* ------------------------------------------------------------------ */
/* Save                                                                 */
/* ------------------------------------------------------------------ */
async function savePost() {
  if (!formTitle.value.trim()) {
    error.value = '文章标题不能为空，请填写标题后再保存'
    return
  }
  if (!formCategoryId.value) {
    error.value = '请在右侧「发布设置」中选择一个内容类型'
    return
  }

  saving.value = true
  error.value = ''

  const tags = formTags.value
    .split(',')
    .map(t => t.trim())
    .filter(Boolean)

  const payload: PostPayload = {
    title: formTitle.value.trim(),
    summary: formSummary.value.trim() || undefined,
    content: formContent.value,
    categoryId: formCategoryId.value,
    tags,
    isPinned: formPinned.value,
    status: formStatus.value,
  }

  try {
    if (editingPostId.value !== null) {
      await blogApi.updatePost(editingPostId.value, payload)
    } else {
      await blogApi.createPost(payload)
    }
    resetForm()
    await loadPosts()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '文章保存失败，请检查网络后重试'
  } finally {
    saving.value = false
  }
}

async function polishPostContent() {
  error.value = ''
  if (!formContent.value.trim()) {
    error.value = '请先填写正文，再进行 AI 润色'
    return
  }

  polishing.value = true
  try {
    const result = await blogApi.polishPost({
      title: formTitle.value,
      summary: formSummary.value,
      content: formContent.value,
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
  formContent.value = polishedMarkdown.value
  showPreview.value = true
  polishPreviewVisible.value = false
}

/* ------------------------------------------------------------------ */
/* Delete                                                               */
/* ------------------------------------------------------------------ */
function askDelete(id: number) {
  pendingDeleteId = id
  confirmMessage.value = '确认删除这篇文章？删除后不可恢复。'
  confirmVisible.value = true
}

async function confirmDelete() {
  if (pendingDeleteId === null) return
  confirmVisible.value = false
  try {
    await blogApi.deletePost(pendingDeleteId)
    if (editingPostId.value === pendingDeleteId) resetForm()
    await loadPosts()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '文章删除失败，请稍后重试'
  } finally {
    pendingDeleteId = null
  }
}

/* ------------------------------------------------------------------ */
/* Lifecycle                                                            */
/* ------------------------------------------------------------------ */
onMounted(() => {
  loadPosts()
  loadCategories()
})
</script>

<template>
  <AdminLayout>
    <section class="admin-posts animate-fade-in">

      <!-- ============================================================ -->
      <!-- Section 1: Writing Canvas                                     -->
      <!-- ============================================================ -->
      <div class="canvas-grid">
        <!-- Left: Article Card (2 cols) -->
        <div class="card writing-canvas">
          <div class="card-accent accent-blue"></div>
          <span class="card-kicker">Writing Canvas</span>

          <input
            v-model="formTitle"
            class="title-input"
            type="text"
            placeholder="文章标题"
            spellcheck="false"
          />

          <input
            v-model="formSummary"
            class="summary-input"
            type="text"
            placeholder="文章摘要 (可选)"
            spellcheck="false"
          />

          <div class="editor-toolbar">
            <button
              class="btn btn-ghost btn-sm"
              :class="{ active: showPreview }"
              @click="showPreview = !showPreview"
            >
              {{ showPreview ? '返回编辑' : '预览' }}
            </button>
            <button
              class="btn btn-ghost btn-sm"
              :disabled="polishing"
              @click="polishPostContent"
            >
              {{ polishing ? 'AI 润色中...' : 'AI 润色' }}
            </button>
          </div>

          <div v-if="!showPreview" class="editor-wrap">
            <textarea
              v-model="formContent"
              class="content-textarea"
              placeholder="使用 Markdown 书写..."
              spellcheck="false"
            ></textarea>
          </div>

          <div v-else class="preview-wrap markdown-body" v-html="renderedHtml"></div>
        </div>

        <!-- Right: Taxonomy Sidebar -->
        <div class="card taxonomy-sidebar">
          <div class="card-accent accent-warm"></div>
          <span class="card-kicker">Meta</span>
          <h3 class="sidebar-heading">发布设置</h3>

          <div v-if="error" class="alert alert-error">{{ error }}</div>

          <div class="field">
            <label class="field-label">内容类型</label>
            <select v-model="formCategoryId" class="field-select">
              <option :value="0" disabled>请选择分类</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                {{ cat.name }}
              </option>
            </select>
          </div>

          <div class="field">
            <label class="field-label">标签</label>
            <input
              v-model="formTags"
              class="field-input"
              type="text"
              placeholder="以逗号分隔，如：Vue, TypeScript"
            />
          </div>

          <div class="field">
            <label class="field-label">状态</label>
            <select v-model="formStatus" class="field-select">
              <option value="DRAFT">草稿</option>
              <option value="PUBLISHED">发布</option>
            </select>
          </div>

          <div class="field field-inline">
            <label class="field-label-checkbox">
              <input v-model="formPinned" type="checkbox" />
              <span>置顶</span>
            </label>
          </div>

          <div class="sidebar-actions">
            <button
              class="btn btn-primary btn-pill"
              :disabled="saving"
              @click="savePost"
            >
              {{ saving ? '保存中...' : (editingPostId !== null ? '更新文章' : '保存文章') }}
            </button>

            <button
              v-if="editingPostId !== null"
              class="btn btn-ghost btn-pill"
              @click="resetForm"
            >
              取消编辑
            </button>
          </div>
        </div>
      </div>

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
              <div class="polish-dialog-body markdown-body" v-html="renderedPolishedHtml"></div>
              <div class="polish-dialog-actions">
                <button type="button" class="btn btn-secondary" @click="polishPreviewVisible = false">稍后再看</button>
                <button type="button" class="btn btn-primary" @click="applyPolishedContent">替换正文</button>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>

      <!-- ============================================================ -->
      <!-- Section 2: Article Management                                 -->
      <!-- ============================================================ -->
      <div class="card article-list-card">
        <span class="card-kicker">Management</span>
        <h3 class="list-heading">已有文章</h3>

        <div v-if="loading" class="loading-hint">加载中...</div>

        <div v-if="!loading && posts.length === 0" class="empty-state">
          <p>暂无文章</p>
        </div>

        <div v-if="!loading && posts.length > 0" class="table-list">
          <div
            v-for="post in posts"
            :key="post.id"
            class="table-row stagger-item"
            :class="{ 'is-editing': editingPostId === post.id }"
          >
            <div class="table-row-content">
              <div class="table-row-title">
                <span v-if="post.isPinned" class="pinned-icon" title="已置顶">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M16 12V4h1V2H7v2h1v8l-2 2v2h5v6l1 1 1-1v-6h5v-2l-2-2z"/>
                  </svg>
                </span>
                {{ post.title }}
              </div>
              <div class="table-row-meta">
                <span
                  class="badge"
                  :class="post.status === 'PUBLISHED' ? 'badge-published' : 'badge-draft'"
                >
                  {{ post.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                </span>
                <span class="meta-category">{{ post.category.name }}</span>
                <span v-if="post.tags && post.tags.length > 0" class="meta-tags">
                  <span v-for="tag in post.tags" :key="tag" class="tag-chip">{{ tag }}</span>
                </span>
              </div>
            </div>
            <div class="table-row-actions">
              <button class="btn btn-secondary btn-sm" @click="editPost(post)">编辑</button>
              <button class="btn btn-danger btn-sm" @click="askDelete(post.id)">删除</button>
            </div>
          </div>
        </div>
      </div>

    </section>

    <ConfirmModal
      :visible="confirmVisible"
      :message="confirmMessage"
      confirm-text="删除"
      :danger="true"
      @confirm="confirmDelete"
      @cancel="confirmVisible = false"
    />
  </AdminLayout>
</template>

<style scoped>
/* ================================================================== */
/* Page wrapper                                                        */
/* ================================================================== */
.admin-posts {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  gap: 28px;
}

/* ================================================================== */
/* Cards                                                               */
/* ================================================================== */
.card {
  position: relative;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 28px 30px 30px;
  overflow: hidden;
}

.card-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}

.accent-blue {
  background: #9bb8ce;
}

.accent-warm {
  background: #e7dfcf;
}

.card-kicker {
  display: inline-block;
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--secondary);
  margin-bottom: 10px;
}

/* ================================================================== */
/* Canvas grid: 3-column layout                                        */
/* ================================================================== */
.canvas-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

@media (max-width: 900px) {
  .canvas-grid {
    grid-template-columns: 1fr;
  }
}

/* ================================================================== */
/* Writing Canvas (left)                                               */
/* ================================================================== */
.writing-canvas {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.title-input {
  width: 100%;
  border: none;
  background: transparent;
  font-family: var(--font-serif);
  font-size: clamp(2rem, 4vw, 4.5rem);
  font-weight: 700;
  line-height: 1.08;
  color: var(--primary);
  padding: 0;
  outline: none;
}

.title-input::placeholder {
  color: var(--secondary);
  opacity: 0.45;
}

.summary-input {
  width: 100%;
  border: none;
  border-bottom: 1px solid var(--line);
  background: transparent;
  font-family: var(--font-serif);
  font-size: 1.05rem;
  color: var(--secondary);
  padding: 6px 0 10px;
  outline: none;
  transition: border-color 0.25s var(--ease);
}

.summary-input:focus {
  border-color: var(--primary);
}

.summary-input::placeholder {
  color: var(--secondary);
  opacity: 0.45;
}

/* Editor toolbar */
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.editor-toolbar .btn.active {
  background: var(--base);
  color: var(--primary);
}

/* Textarea */
.content-textarea {
  width: 100%;
  min-height: 280px;
  resize: vertical;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--base);
  font-family: var(--font-mono);
  font-size: 0.88rem;
  line-height: 1.8;
  padding: 18px;
  color: var(--primary);
  outline: none;
  transition: border-color 0.25s var(--ease), box-shadow 0.25s var(--ease);
}

.content-textarea:focus {
  border-color: #9bb8ce;
  box-shadow: 0 0 0 3px rgba(155, 184, 206, 0.15);
}

.content-textarea::placeholder {
  color: var(--secondary);
  opacity: 0.45;
}

/* Preview */
.preview-wrap {
  min-height: 280px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--base);
  padding: 18px;
  line-height: 1.8;
  overflow-wrap: break-word;
}

.preview-wrap :deep(pre) {
  background: var(--code-bg);
  border-radius: 8px;
  padding: 14px 16px;
  overflow-x: auto;
  font-family: var(--font-mono);
  font-size: 0.85rem;
  line-height: 1.7;
}

.preview-wrap :deep(code) {
  font-family: var(--font-mono);
  font-size: 0.88em;
}

.preview-wrap :deep(p:not(:last-child)) {
  margin-bottom: 1em;
}

.preview-wrap :deep(h1),
.preview-wrap :deep(h2),
.preview-wrap :deep(h3) {
  margin-top: 1.4em;
  margin-bottom: 0.6em;
}

.preview-wrap :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

/* ================================================================== */
/* Taxonomy Sidebar (right)                                            */
/* ================================================================== */
.taxonomy-sidebar {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.sidebar-heading {
  font-family: var(--font-serif);
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--primary);
  margin: 0;
}

/* Fields */
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--secondary);
  letter-spacing: 0.02em;
}

.field-input,
.field-select {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--base);
  font-size: 0.88rem;
  color: var(--primary);
  outline: none;
  transition: border-color 0.25s var(--ease);
}

.field-input:focus,
.field-select:focus {
  border-color: #e7dfcf;
}

.field-select {
  cursor: pointer;
  appearance: auto;
}

.field-inline {
  flex-direction: row;
  align-items: center;
}

.field-label-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  color: var(--primary);
  cursor: pointer;
  user-select: none;
}

.field-label-checkbox input[type='checkbox'] {
  width: 16px;
  height: 16px;
  accent-color: var(--primary);
  cursor: pointer;
}

/* Sidebar actions */
.sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 6px;
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
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 12px;
  box-shadow: 0 24px 72px rgba(15, 23, 42, 0.24);
  overflow: hidden;
}

.polish-dialog-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px;
  border-bottom: 1px solid var(--line);
}

.polish-dialog-header h3 {
  margin: 6px 0 0;
  font-size: 20px;
  color: var(--primary);
}

.polish-dialog-kicker {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--secondary);
}

.dialog-close {
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--primary);
  border-radius: 8px;
  padding: 8px 14px;
  cursor: pointer;
}

.polish-dialog-body {
  padding: 20px;
  overflow-y: auto;
  background: var(--base);
}

.polish-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px;
  border-top: 1px solid var(--line);
  background: var(--surface);
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

/* ================================================================== */
/* Buttons                                                             */
/* ================================================================== */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 20px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  color: var(--primary);
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.24s var(--ease), color 0.24s var(--ease), border-color 0.24s var(--ease);
  white-space: nowrap;
}

.btn:hover {
  background: var(--base);
}

.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-sm {
  padding: 6px 14px;
  font-size: 0.8rem;
}

.btn-pill {
  border-radius: 100px;
}

.btn-primary {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border-color: var(--btn-primary-bg);
}

.btn-primary:hover,
.btn-primary:focus-visible {
  background: var(--btn-primary-hover-bg);
  color: var(--btn-primary-hover-text);
  border-color: var(--btn-primary-hover-bg);
  opacity: 1;
}

.btn-secondary {
  background: transparent;
  color: var(--secondary);
  border-color: var(--line);
}

.btn-secondary:hover {
  color: var(--primary);
  border-color: var(--primary);
}

.btn-danger {
  background: transparent;
  color: #c0392b;
  border-color: transparent;
}

.btn-danger:hover {
  background: rgba(192, 57, 43, 0.08);
}

.btn-ghost {
  background: transparent;
  border-color: transparent;
  color: var(--secondary);
}

.btn-ghost:hover {
  color: var(--primary);
  background: var(--base);
}

/* ================================================================== */
/* Article list card                                                   */
/* ================================================================== */
.article-list-card {
  padding-bottom: 20px;
}

.list-heading {
  font-family: var(--font-serif);
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--primary);
  margin: 0 0 18px;
}

/* Alerts */
.alert {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 0.85rem;
  margin-bottom: 16px;
}

.alert-error {
  background: rgba(192, 57, 43, 0.08);
  color: #c0392b;
  border: 1px solid rgba(192, 57, 43, 0.18);
}

/* Loading & empty */
.loading-hint {
  text-align: center;
  padding: 32px 0;
  color: var(--secondary);
  font-size: 0.9rem;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: var(--secondary);
  font-size: 0.92rem;
}

/* ================================================================== */
/* Table list                                                          */
/* ================================================================== */
.table-list {
  display: grid;
  gap: 2px;
}

.table-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  transition: background 0.2s var(--ease);
}

.table-row:hover {
  background: var(--base);
}

.table-row.is-editing {
  background: rgba(155, 184, 206, 0.1);
  outline: 1px solid rgba(155, 184, 206, 0.3);
}

.table-row-content {
  min-width: 0;
  flex: 1;
}

.table-row-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  font-size: 0.95rem;
  color: var(--primary);
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pinned-icon {
  color: #d4a843;
  display: inline-flex;
  flex-shrink: 0;
}

.table-row-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.badge {
  display: inline-block;
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  padding: 2px 8px;
  border-radius: 100px;
}

.badge-published {
  background: rgba(46, 139, 87, 0.1);
  color: #2e8b57;
}

.badge-draft {
  background: rgba(212, 168, 67, 0.1);
  color: #b8942e;
}

.meta-category {
  font-size: 0.8rem;
  color: var(--secondary);
}

.meta-tags {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.tag-chip {
  font-size: 0.68rem;
  padding: 1px 7px;
  background: rgba(155, 184, 206, 0.12);
  color: #6d93ad;
  border-radius: 4px;
}

.table-row-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

/* ================================================================== */
/* Stagger animation                                                   */
/* ================================================================== */
.stagger-item {
  animation: fadeSlideUp 0.35s var(--ease) both;
}

.stagger-item:nth-child(1) { animation-delay: 0s; }
.stagger-item:nth-child(2) { animation-delay: 0.04s; }
.stagger-item:nth-child(3) { animation-delay: 0.08s; }
.stagger-item:nth-child(4) { animation-delay: 0.12s; }
.stagger-item:nth-child(5) { animation-delay: 0.16s; }
.stagger-item:nth-child(6) { animation-delay: 0.2s; }
.stagger-item:nth-child(7) { animation-delay: 0.24s; }
.stagger-item:nth-child(8) { animation-delay: 0.28s; }

@keyframes fadeSlideUp {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ================================================================== */
/* Responsive                                                          */
/* ================================================================== */
@media (max-width: 900px) {
  .admin-posts {
    gap: 20px;
  }

  .card {
    padding: 22px 18px 24px;
  }

  .title-input {
    font-size: clamp(1.6rem, 5vw, 2.5rem);
  }

  .table-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .table-row-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
