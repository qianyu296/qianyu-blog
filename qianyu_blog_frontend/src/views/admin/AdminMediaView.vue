<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { blogApi } from '@/api/blog'
import type { MediaAsset } from '@/types/blog'

const baseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const assets = ref<MediaAsset[]>([])
const loading = ref(false)
const uploading = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const uploadDialogVisible = ref(false)
const pendingUploadFile = ref<File | null>(null)
const pendingUploadPreviewUrl = ref('')
const uploadDescription = ref('')
const lightboxAsset = ref<MediaAsset | null>(null)
const confirmVisible = ref(false)
const confirmMessage = ref('')
let pendingDeleteId: number | null = null

function resolveAssetUrl(url?: string) {
  if (!url) return ''
  return url.startsWith('http') ? url : `${baseUrl}${url}`
}

function formatDate(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

async function loadAssets() {
  loading.value = true
  try {
    assets.value = await blogApi.adminMediaAssets()
  } catch {
    /* handled silently */
  } finally {
    loading.value = false
  }
}

function openUploadDialog() {
  uploadDialogVisible.value = true
}

function resetPendingUpload() {
  if (pendingUploadPreviewUrl.value) {
    URL.revokeObjectURL(pendingUploadPreviewUrl.value)
  }
  pendingUploadFile.value = null
  pendingUploadPreviewUrl.value = ''
  uploadDescription.value = ''
  if (fileInputRef.value) fileInputRef.value.value = ''
}

function triggerUpload() {
  fileInputRef.value?.click()
}

function closeUploadDialog() {
  uploadDialogVisible.value = false
  resetPendingUpload()
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (pendingUploadPreviewUrl.value) {
    URL.revokeObjectURL(pendingUploadPreviewUrl.value)
  }
  pendingUploadFile.value = file
  pendingUploadPreviewUrl.value = URL.createObjectURL(file)
}

async function submitUpload() {
  const file = pendingUploadFile.value
  if (!file) return

  const description = uploadDescription.value.trim()

  uploading.value = true
  try {
    const created = await blogApi.uploadMediaAsset({
      file,
      displayName: description || undefined,
      altText: description || undefined,
    })
    assets.value.unshift(created)
    closeUploadDialog()
  } catch {
    /* handled silently */
  } finally {
    uploading.value = false
  }
}

function askRemove(id: number, event: MouseEvent) {
  event.stopPropagation()
  pendingDeleteId = id
  confirmMessage.value = '确认删除这张图片？删除后不可恢复。'
  confirmVisible.value = true
}

async function confirmRemove() {
  if (pendingDeleteId === null) return
  confirmVisible.value = false
  try {
    await blogApi.deleteMediaAsset(pendingDeleteId)
    assets.value = assets.value.filter((a) => a.id !== pendingDeleteId)
    if (lightboxAsset.value?.id === pendingDeleteId) lightboxAsset.value = null
  } catch {
    /* handled silently */
  } finally {
    pendingDeleteId = null
  }
}

function openLightbox(asset: MediaAsset) {
  lightboxAsset.value = asset
}

function closeLightbox() {
  lightboxAsset.value = null
}

function onOverlayClick(event: MouseEvent) {
  if (event.target === event.currentTarget) closeLightbox()
}

function onKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape') return
  if (uploadDialogVisible.value) {
    closeUploadDialog()
    return
  }
  closeLightbox()
}

onMounted(() => {
  loadAssets()
  document.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
  resetPendingUpload()
})
</script>

<template>
  <AdminLayout>
    <section class="admin-media animate-fade-in">
      <div class="media-top-bar">
        <h1 class="media-title">光影手记</h1>
        <button class="btn btn-primary" :disabled="uploading" @click="openUploadDialog">
          {{ uploading ? '上传中...' : '新增图片' }}
        </button>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          hidden
          @change="handleFileChange"
        />
      </div>

      <div v-if="loading" class="media-loading">加载中...</div>

      <div v-else-if="assets.length === 0" class="empty-state">
        <div class="empty-state-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M4 6a2 2 0 012-2h12a2 2 0 012 2v12a2 2 0 01-2 2H6a2 2 0 01-2-2V6z" />
            <path d="M8 14l2.5-3 2 2.5 3.5-4.5L20 14" />
            <circle cx="9" cy="9" r="1" />
          </svg>
        </div>
        <p class="empty-state-text">还没有图片资源</p>
      </div>

      <div v-else class="media-grid">
        <figure
          v-for="asset in assets"
          :key="asset.id"
          class="library-item"
        >
          <img
            :src="resolveAssetUrl(asset.fileUrl)"
            :alt="asset.altText || asset.displayName || asset.originalFileName"
            @click="openLightbox(asset)"
          />
          <figcaption>
            {{ formatDate(asset.createdAt) }}
            <template v-if="asset.displayName"> · {{ asset.displayName }}</template>
          </figcaption>
          <button
            class="delete-btn"
            title="删除"
            @click="askRemove(asset.id, $event)"
          >
            <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="4" y1="4" x2="12" y2="12" />
              <line x1="12" y1="4" x2="4" y2="12" />
            </svg>
          </button>
        </figure>
      </div>

      <teleport to="body">
        <div
          v-if="lightboxAsset"
          class="lightbox-overlay"
          @click="onOverlayClick"
        >
          <img
            class="lightbox-image"
            :src="resolveAssetUrl(lightboxAsset.fileUrl)"
            :alt="lightboxAsset.altText || lightboxAsset.displayName || lightboxAsset.originalFileName"
          />
        </div>
      </teleport>

      <teleport to="body">
        <div
          v-if="uploadDialogVisible"
          class="upload-dialog-overlay"
          @click.self="closeUploadDialog"
        >
          <div class="upload-dialog">
            <div class="upload-dialog-head">
              <h2>新增图片</h2>
              <button class="upload-dialog-close" type="button" aria-label="关闭" @click="closeUploadDialog">
                <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                  <line x1="4" y1="4" x2="12" y2="12" />
                  <line x1="12" y1="4" x2="4" y2="12" />
                </svg>
              </button>
            </div>

            <div class="upload-dialog-body">
              <div class="form-group">
                <label class="form-label">图片文件</label>
                <button class="btn btn-secondary upload-picker" type="button" @click="triggerUpload">
                  {{ pendingUploadFile ? '重新选择图片' : '选择图片' }}
                </button>
                <span v-if="pendingUploadFile" class="upload-file-name">{{ pendingUploadFile.name }}</span>
                <span v-else class="form-hint">支持常见图片格式，先选图再填写描述</span>
              </div>

              <div v-if="pendingUploadPreviewUrl" class="upload-preview">
                <img :src="pendingUploadPreviewUrl" :alt="uploadDescription || pendingUploadFile?.name || '图片预览'" />
              </div>

              <div class="form-group">
                <label class="form-label" for="uploadDescription">图片描述</label>
                <textarea
                  id="uploadDescription"
                  v-model="uploadDescription"
                  class="form-textarea upload-description"
                  rows="4"
                  placeholder="填写这张图片的描述"
                />
                <span class="form-hint">会同时作为图片标题和替代文本使用</span>
              </div>
            </div>

            <div class="upload-dialog-actions">
              <button class="btn btn-secondary" type="button" :disabled="uploading" @click="closeUploadDialog">
                取消
              </button>
              <button class="btn btn-primary" type="button" :disabled="uploading || !pendingUploadFile" @click="submitUpload">
                {{ uploading ? '上传中...' : '开始上传' }}
              </button>
            </div>
          </div>
        </div>
      </teleport>

      <ConfirmModal
        :visible="confirmVisible"
        :message="confirmMessage"
        confirm-text="删除"
        :danger="true"
        @confirm="confirmRemove"
        @cancel="confirmVisible = false"
      />
    </section>
  </AdminLayout>
</template>

<style scoped>
.admin-media {
  max-width: 1200px;
  margin: 0 auto;
}

.media-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.media-title {
  font-family: var(--font-serif);
  font-size: 1.6rem;
  font-weight: 700;
  margin: 0;
}

.media-loading {
  text-align: center;
  padding: 48px 0;
  color: var(--secondary);
  font-size: 0.95rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: var(--secondary);
}

.empty-state-icon {
  width: 56px;
  height: 56px;
  margin-bottom: 16px;
  opacity: 0.45;
}

.empty-state-icon svg {
  width: 100%;
  height: 100%;
}

.empty-state-text {
  font-size: 0.95rem;
  margin: 0;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.upload-dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 120;
  background: rgba(0, 0, 0, 0.48);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.upload-dialog {
  width: min(100%, 520px);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 16px;
  box-shadow: var(--shadow-hover);
  overflow: hidden;
}

.upload-dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 14px;
}

.upload-dialog-head h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 1.2rem;
  font-weight: 700;
}

.upload-dialog-close {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: transparent;
  color: var(--secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s var(--ease), color 0.2s var(--ease);
}

.upload-dialog-close:hover {
  background: var(--color-surface-hover);
  color: var(--primary);
}

.upload-dialog-close svg {
  width: 14px;
  height: 14px;
}

.upload-dialog-body {
  padding: 0 22px 22px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-picker {
  align-self: flex-start;
}

.upload-file-name {
  font-size: 13px;
  color: var(--color-text-secondary);
  word-break: break-all;
}

.upload-preview {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--color-bg);
}

.upload-preview img {
  display: block;
  width: 100%;
  max-height: 280px;
  object-fit: contain;
}

.upload-description {
  min-height: 108px;
}

.upload-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 22px 22px;
  border-top: 1px solid var(--line);
}

.library-item {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  background: var(--surface);
  box-shadow: var(--shadow);
  margin: 0;
}

.library-item img {
  aspect-ratio: 9/16;
  width: 100%;
  object-fit: cover;
  display: block;
  cursor: pointer;
}

.library-item figcaption {
  padding: 12px;
  color: var(--secondary);
  font-size: 0.84rem;
}

.delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(220, 38, 38, 0.8);
  color: white;
  opacity: 0;
  transition: opacity 0.2s;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.delete-btn svg {
  width: 14px;
  height: 14px;
}

.library-item:hover .delete-btn {
  opacity: 1;
}

/* Lightbox */
.lightbox-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
}

.lightbox-image {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
}

/* Responsive */
@media (max-width: 1100px) {
  .media-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .media-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .media-grid {
    grid-template-columns: 1fr;
  }

  .media-top-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
