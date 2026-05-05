<script setup lang="ts">
import AdminLayout from '@/components/AdminLayout.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { blogApi } from '@/api/blog'
import type { MusicTrack } from '@/types/blog'
import { ref, computed, onMounted } from 'vue'

const baseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const activeChannel = ref<'calm' | 'rhythm'>('calm')
const calmTracks = ref<MusicTrack[]>([])
const rhythmTracks = ref<MusicTrack[]>([])
const loading = ref(false)
const uploading = ref(false)
const error = ref('')
const success = ref('')
const confirmVisible = ref(false)
const confirmMessage = ref('')
let pendingDeleteId: number | null = null

const uploadTitle = ref('')
const uploadArtist = ref('')
const uploadAudioFile = ref<File>()
const uploadAudioInputRef = ref<HTMLInputElement | null>(null)

const playingId = ref<number>()

const currentTracks = computed(() =>
  activeChannel.value === 'calm' ? calmTracks.value : rhythmTracks.value,
)

const selectedTrackUrl = computed(() => {
  const all = [...calmTracks.value, ...rhythmTracks.value]
  const track = all.find((t) => t.id === playingId.value)
  if (!track?.fileUrl) return ''
  return resolveAssetUrl(track.fileUrl)
})

function formatTime(seconds?: number) {
  if (!Number.isFinite(seconds)) return '00:00'
  const total = Math.max(0, Math.floor(seconds ?? 0))
  const minutes = Math.floor(total / 60)
  const remain = total % 60
  return `${String(minutes).padStart(2, '0')}:${String(remain).padStart(2, '0')}`
}

function resolveAssetUrl(url: string) {
  if (!url) return ''
  return url.startsWith('http') ? url : `${baseUrl}${url}`
}

function handleUploadAudioChange(event: Event) {
  const input = event.target as HTMLInputElement
  uploadAudioFile.value = input.files?.[0]
}

function resetUploadForm() {
  uploadTitle.value = ''
  uploadArtist.value = ''
  uploadAudioFile.value = undefined
  if (uploadAudioInputRef.value) uploadAudioInputRef.value.value = ''
}

function selectChannel(ch: 'calm' | 'rhythm') {
  activeChannel.value = ch
  playingId.value = undefined
}

async function loadTracks() {
  loading.value = true
  error.value = ''
  try {
    const [calm, rhythm] = await Promise.all([
      blogApi.publicMusicTracks('calm'),
      blogApi.publicMusicTracks('rhythm'),
    ])
    calmTracks.value = calm
    rhythmTracks.value = rhythm
  } catch (err) {
    error.value = err instanceof Error ? err.message : '音乐列表加载失败，请检查网络后刷新页面'
  } finally {
    loading.value = false
  }
}

async function submitUpload() {
  if (!uploadAudioFile.value) {
    error.value = '请先选择一个音频文件（支持 mp3、wav、flac 等格式）'
    success.value = ''
    return
  }

  uploading.value = true
  error.value = ''
  success.value = ''

  try {
    const created = await blogApi.uploadMusic({
      title: uploadTitle.value.trim() || undefined,
      artist: uploadArtist.value.trim() || undefined,
      channel: activeChannel.value,
      audioFile: uploadAudioFile.value,
    })
    if (activeChannel.value === 'rhythm') {
      rhythmTracks.value = [created, ...rhythmTracks.value]
    } else {
      calmTracks.value = [created, ...calmTracks.value]
    }
    resetUploadForm()
    success.value = '音乐上传成功'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '音乐上传失败，请检查文件格式和大小后重试'
  } finally {
    uploading.value = false
  }
}

function askRemove(id: number) {
  pendingDeleteId = id
  confirmMessage.value = '确认删除这首音乐？删除后不可恢复。'
  confirmVisible.value = true
}

async function confirmRemove() {
  if (pendingDeleteId === null) return
  confirmVisible.value = false
  error.value = ''
  success.value = ''
  try {
    await blogApi.deleteMusic(pendingDeleteId)
    calmTracks.value = calmTracks.value.filter((t) => t.id !== pendingDeleteId)
    rhythmTracks.value = rhythmTracks.value.filter((t) => t.id !== pendingDeleteId)
    if (playingId.value === pendingDeleteId) playingId.value = undefined
    success.value = '音乐已删除'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '音乐删除失败，请稍后重试'
  } finally {
    pendingDeleteId = null
  }
}

function togglePlay(track: MusicTrack) {
  if (playingId.value === track.id) {
    playingId.value = undefined
  } else {
    playingId.value = track.id
  }
}

onMounted(loadTracks)
</script>

<template>
  <AdminLayout>
    <section class="admin-music animate-fade-in">
      <div v-if="error" class="alert alert-error mb-lg">{{ error }}</div>
      <Transition name="fade">
        <div v-if="success" class="alert alert-success mb-lg">{{ success }}</div>
      </Transition>

      <!-- Channel panels -->
      <div class="channel-grid">
        <article
          class="channel-panel"
          :class="{ 'is-active': activeChannel === 'calm' }"
          @click="selectChannel('calm')"
        >
          <span class="card-kicker">Channel 01</span>
          <h2>沉浸留白</h2>
          <div class="wave-bars">
            <span class="wave-bar" style="height: 18px; animation-delay: 0s;"></span>
            <span class="wave-bar" style="height: 36px; animation-delay: 0.12s;"></span>
            <span class="wave-bar" style="height: 25px; animation-delay: 0.24s;"></span>
            <span class="wave-bar" style="height: 44px; animation-delay: 0.36s;"></span>
            <span class="wave-bar" style="height: 22px; animation-delay: 0.48s;"></span>
          </div>
          <p class="channel-desc">Ambient, lo-fi, cinematic -- music for focus and quiet moments.</p>
          <span class="channel-count">{{ calmTracks.length }} 首曲目</span>
        </article>

        <article
          class="channel-panel"
          :class="{ 'is-active': activeChannel === 'rhythm' }"
          @click="selectChannel('rhythm')"
        >
          <span class="card-kicker">Channel 02</span>
          <h2>创作律动</h2>
          <div class="wave-bars">
            <span class="wave-bar" style="height: 18px; animation-delay: 0s;"></span>
            <span class="wave-bar" style="height: 36px; animation-delay: 0.12s;"></span>
            <span class="wave-bar" style="height: 25px; animation-delay: 0.24s;"></span>
            <span class="wave-bar" style="height: 44px; animation-delay: 0.36s;"></span>
            <span class="wave-bar" style="height: 22px; animation-delay: 0.48s;"></span>
          </div>
          <p class="channel-desc">Beats, electronic, experimental -- energy for building and creating.</p>
          <span class="channel-count">{{ rhythmTracks.length }} 首曲目</span>
        </article>
      </div>

      <!-- Audio preview -->
      <div v-if="playingId" class="card preview-panel">
        <div class="section-header">
          <h3 class="card-subtitle">播放预览</h3>
          <span class="muted">{{ [...calmTracks, ...rhythmTracks].find(t => t.id === playingId)?.title }}</span>
        </div>
        <audio class="audio-player" controls autoplay :src="selectedTrackUrl || undefined"></audio>
      </div>

      <!-- Track list + Upload -->
      <section class="card music-manage-section">
        <div class="section-header">
          <h2 class="card-title">{{ activeChannel === 'calm' ? '沉浸留白' : '创作律动' }} · 曲目列表</h2>
          <span class="muted">{{ loading ? '加载中...' : `${currentTracks.length} 首` }}</span>
        </div>

        <!-- Track list -->
        <div v-if="currentTracks.length === 0 && !loading" class="empty-state">
          <p class="empty-state-text">该频道暂无曲目</p>
        </div>

        <div v-else class="track-list">
          <div
            v-for="track in currentTracks"
            :key="track.id"
            class="track-item"
            :class="{ active: track.id === playingId }"
          >
            <button class="play-btn" @click="togglePlay(track)">
              <svg v-if="track.id === playingId" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                <rect x="6" y="4" width="4" height="16" rx="1" /><rect x="14" y="4" width="4" height="16" rx="1" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                <path d="M8 5.14v14.72a1 1 0 001.5.86l11-7.36a1 1 0 000-1.72l-11-7.36A1 1 0 008 5.14z" />
              </svg>
            </button>
            <div class="track-info">
              <strong>{{ track.title }}</strong>
              <span class="muted">{{ track.artist || '未知歌手' }}</span>
            </div>
            <span class="track-meta muted">{{ formatTime(track.durationSeconds) }}</span>
            <button class="btn btn-danger btn-sm" @click="askRemove(track.id)">删除</button>
          </div>
        </div>

        <!-- Upload form -->
        <div class="upload-divider"></div>
        <h3 class="upload-heading">上传到「{{ activeChannel === 'calm' ? '沉浸留白' : '创作律动' }}」</h3>

        <div class="upload-form">
          <div class="upload-fields">
            <div class="form-group">
              <label class="form-label" for="uploadTitle">标题</label>
              <input
                id="uploadTitle"
                v-model="uploadTitle"
                type="text"
                class="form-input"
                placeholder="可留空，默认使用文件名"
              />
            </div>

            <div class="form-group">
              <label class="form-label" for="uploadArtist">歌手</label>
              <input
                id="uploadArtist"
                v-model="uploadArtist"
                type="text"
                class="form-input"
                placeholder="可选"
              />
            </div>

            <div class="form-group">
              <label class="form-label">音频文件</label>
              <div class="file-picker" @click="uploadAudioInputRef?.click()">
                <svg class="file-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M9 18V5l12-2v13" />
                  <circle cx="6" cy="18" r="3" />
                  <circle cx="18" cy="16" r="3" />
                </svg>
                <span>{{ uploadAudioFile ? uploadAudioFile.name : '选择音频文件' }}</span>
              </div>
              <input
                ref="uploadAudioInputRef"
                type="file"
                accept="audio/*"
                hidden
                @change="handleUploadAudioChange"
              />
            </div>
          </div>

          <div class="form-actions">
            <button class="btn btn-primary" :disabled="uploading" @click="submitUpload">
              {{ uploading ? '上传中...' : '上传音乐' }}
            </button>
          </div>
        </div>
      </section>

    </section>

    <ConfirmModal
      :visible="confirmVisible"
      :message="confirmMessage"
      confirm-text="删除"
      :danger="true"
      @confirm="confirmRemove"
      @cancel="confirmVisible = false"
    />
  </AdminLayout>
</template>

<style scoped>
.admin-music {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  gap: 24px;
}

.mb-lg { margin-bottom: 8px; }

/* Alerts */
.alert {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 0.85rem;
}

.alert-error {
  background: rgba(192, 57, 43, 0.08);
  color: #c0392b;
  border: 1px solid rgba(192, 57, 43, 0.18);
}

.alert-success {
  background: rgba(46, 139, 87, 0.08);
  color: #2e8b57;
  border: 1px solid rgba(46, 139, 87, 0.18);
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* Channel grid */
.channel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

@media (max-width: 820px) {
  .channel-grid { grid-template-columns: 1fr; }
}

.channel-panel {
  position: relative;
  background: var(--surface);
  border: 2px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 24px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.25s var(--ease), box-shadow 0.25s var(--ease);
}

.channel-panel:hover {
  border-color: #b8ccd9;
}

.channel-panel.is-active {
  border-color: #9bb8ce;
  box-shadow: 0 0 0 3px rgba(155, 184, 206, 0.18), var(--shadow);
}

.channel-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #9bb8ce;
}

.card-kicker {
  display: inline-block;
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--secondary);
  margin-bottom: 6px;
}

.channel-panel h2 {
  font-family: var(--font-serif);
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--primary);
  margin: 0 0 12px;
}

.channel-desc {
  font-size: 0.82rem;
  color: var(--secondary);
  margin: 0 0 8px;
  line-height: 1.6;
}

.channel-count {
  font-size: 0.75rem;
  font-weight: 600;
  color: #506d80;
  background: var(--code-bg);
  border-radius: 999px;
  padding: 3px 10px;
}

/* Wave bars */
.wave-bars {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 44px;
  margin-bottom: 12px;
}

.wave-bar {
  width: 4px;
  background: #9bb8ce;
  border-radius: 2px;
  animation: waveAnim 1.2s ease-in-out infinite alternate;
}

@keyframes waveAnim {
  0% { height: 12px; }
  50% { height: 38px; }
  100% { height: 18px; }
}

/* Track item */
.track-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.track-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  transition: background 0.2s var(--ease);
}

.track-item:hover { background: var(--base); }
.track-item.active { background: rgba(155, 184, 206, 0.12); }

.play-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--secondary);
  border-radius: 50%;
  flex-shrink: 0;
  transition: color 0.2s, background 0.2s;
}

.play-btn:hover {
  color: var(--primary);
  background: var(--base);
}

.track-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.track-info strong {
  font-size: 0.88rem;
  font-weight: 500;
  color: var(--primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.track-meta {
  font-size: 0.75rem;
  flex-shrink: 0;
}

.muted { color: var(--secondary); }

/* Preview panel */
.preview-panel {
  padding: 18px 24px;
}

.audio-player {
  width: 100%;
  height: 40px;
  margin-top: 8px;
}

/* Music manage section */
.music-manage-section {
  padding: 28px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.card-title {
  font-family: var(--font-serif);
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--primary);
  margin: 0;
}

.card-subtitle {
  font-family: var(--font-serif);
  font-size: 1rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0;
}

.empty-state {
  text-align: center;
  padding: 24px 0;
}

.empty-state-text {
  font-size: 0.88rem;
  color: var(--secondary);
  margin: 0;
}

/* Upload form */
.upload-divider {
  height: 1px;
  background: var(--line);
  margin: 24px 0;
}

.upload-heading {
  font-family: var(--font-serif);
  font-size: 1rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0 0 16px;
}

.upload-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.upload-fields {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--secondary);
}

.form-input {
  padding: 9px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--base);
  font-size: 0.88rem;
  color: var(--primary);
  outline: none;
  transition: border-color 0.25s var(--ease);
}

.form-input:focus { border-color: #9bb8ce; }

.file-picker {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border: 2px dashed var(--line);
  border-radius: 10px;
  cursor: pointer;
  color: var(--secondary);
  font-size: 0.85rem;
  transition: border-color 0.2s, color 0.2s;
}

.file-picker:hover {
  border-color: #9bb8ce;
  color: var(--primary);
}

.file-icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

/* Buttons */
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
  transition: background 0.24s var(--ease), color 0.24s var(--ease);
  white-space: nowrap;
}

.btn:hover { background: var(--base); }
.btn:disabled { opacity: 0.55; cursor: not-allowed; }

.btn-sm { padding: 6px 14px; font-size: 0.8rem; }

.btn-primary {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border-color: var(--btn-primary-bg);
}

.btn-primary:hover { background: var(--btn-primary-hover-bg); color: var(--btn-primary-hover-text); }

.btn-danger {
  background: transparent;
  color: #c0392b;
  border-color: transparent;
}

.btn-danger:hover { background: rgba(192, 57, 43, 0.08); }

.card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}
</style>
