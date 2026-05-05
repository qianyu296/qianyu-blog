<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { blogApi } from '@/api/blog'
import type { Post, Category, MediaAsset } from '@/types/blog'

const baseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/* ── reactive state ── */
const categories = ref<Category[]>([])
const posts = ref<Post[]>([])
const mediaAssets = ref<MediaAsset[]>([])
const loading = ref(true)

const sparkBox = ref(localStorage.getItem('qianyu-admin-spark') ?? '')
const sparkCount = computed(() => sparkBox.value.length)

const musicMode = ref<string>(localStorage.getItem('qianyu-music-mode') ?? 'calm')
const publishing = ref(false)
const toast = ref('')

/* ── derived metrics ── */
const draftCount = computed(() => posts.value.filter(p => p.status === 'DRAFT').length)
const photoCount = computed(() => mediaAssets.value.length)
const moodLabel = computed(() => musicMode.value === 'rhythm' ? '02' : '01')

const recentMedia = computed(() => {
  return [...mediaAssets.value]
    .sort((a, b) => {
      const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0
      const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0
      return tb - ta
    })
    .slice(0, 2)
})

/* ── spark persistence ── */
function onSparkInput() {
  localStorage.setItem('qianyu-admin-spark', sparkBox.value)
}

/* ── publish spark ── */
async function publishSpark() {
  if (!sparkBox.value.trim()) return
  publishing.value = true
  try {
    /* 1. find or create "碎碎念" category */
    const allCats = await blogApi.adminCategories()
    let suipiCategory = allCats.find(c => c.name === '碎碎念')
    if (!suipiCategory) {
      suipiCategory = await blogApi.createCategory({
        name: '碎碎念',
        slug: 'suipisuiNian',
        description: '生活碎碎念与灵感记录',
      })
    }

    /* 2. create post */
    await blogApi.createPost({
      title: '碎碎念',
      content: sparkBox.value,
      categoryId: suipiCategory.id,
      status: 'PUBLISHED',
      tags: ['碎碎念'],
      isPinned: false,
    })

    /* 3. success */
    sparkBox.value = ''
    localStorage.removeItem('qianyu-admin-spark')
    showToast('已发布到前台')
    /* refresh posts count */
    const page = await blogApi.adminPosts()
    posts.value = page.content
  } catch (err) {
    showToast(err instanceof Error ? err.message : '碎碎念发布失败，请检查网络后重试')
  } finally {
    publishing.value = false
  }
}

/* ── atmosphere toggle ── */
function setMusicMode(mode: string) {
  musicMode.value = mode
  localStorage.setItem('qianyu-music-mode', mode)
}

/* ── toast ── */
let toastTimer: ReturnType<typeof setTimeout> | null = null
function showToast(msg: string) {
  toast.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 2400)
}

/* ── data loading ── */
onMounted(async () => {
  loading.value = true
  try {
    const [catResult, postPage, mediaResult] = await Promise.all([
      blogApi.adminCategories(),
      blogApi.adminPosts(),
      blogApi.adminMediaAssets(),
    ])
    categories.value = catResult
    posts.value = postPage.content
    mediaAssets.value = mediaResult
  } catch (err) {
    showToast(err instanceof Error ? err.message : '仪表盘数据加载失败，请刷新页面重试')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <AdminLayout>
    <div class="dashboard-grid animate-fade-in">

      <!-- Card 1: 灵感捕手 (span 2 cols) -->
      <div class="dash-card spark-card">
        <div class="card-top-line warm-line" />
        <div class="card-body">
          <span class="card-kicker">Sparks</span>
          <h2 class="card-heading">灵感捕手</h2>
          <textarea
            v-model="sparkBox"
            class="spark-textarea"
            placeholder="记录当下的转场灵感、技术碎碎念或一句生活观察..."
            @input="onSparkInput"
          />
          <div class="spark-footer">
            <span class="spark-meta">{{ sparkCount }} 字符 · 自动暂存</span>
            <button
              class="btn btn-primary"
              :disabled="publishing || !sparkBox.trim()"
              @click="publishSpark"
            >
              {{ publishing ? '发布中...' : '发布到前台' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Card 2: 状态统计 (column 3) -->
      <div class="dash-card stats-card">
        <div class="card-top-line accent-line" />
        <div class="card-body">
          <span class="card-kicker">Overview</span>
          <h3 class="card-heading">状态统计</h3>
          <div class="metric-row">
            <span class="metric-label">Drafts</span>
            <span class="metric-value">{{ draftCount }}</span>
            <span class="metric-tag">篇草稿</span>
          </div>
          <div class="metric-row">
            <span class="metric-label">Photos</span>
            <span class="metric-value">{{ photoCount }}</span>
            <span class="metric-tag">张影像</span>
          </div>
          <div class="metric-row">
            <span class="metric-label">Mood</span>
            <span class="metric-value">{{ moodLabel }}</span>
            <span class="metric-tag">{{ musicMode === 'rhythm' ? '律动' : '留白' }}</span>
          </div>
        </div>
      </div>

      <!-- Card 3: 最近的光影 (row 2, col 1) -->
      <div class="dash-card media-card">
        <div class="card-top-line warm-line" />
        <div class="card-body">
          <span class="card-kicker">Visual Check</span>
          <h3 class="card-heading">最近的光影</h3>
          <div v-if="recentMedia.length" class="media-row">
            <div
              v-for="asset in recentMedia"
              :key="asset.id"
              class="media-item"
            >
              <img
                :src="`${baseUrl}/api/public/media/${asset.id}/file`"
                :alt="asset.altText || asset.displayName || asset.originalFileName"
                class="media-img"
              />
              <span class="media-caption">{{ asset.displayName || asset.originalFileName }}</span>
            </div>
          </div>
          <div v-else class="empty-hint">暂无光影素材</div>
        </div>
      </div>

      <!-- Card 4: 氛围中控 (row 2, col 2) -->
      <div class="dash-card atmosphere-card">
        <div class="card-top-line accent-line" />
        <div class="card-body">
          <span class="card-kicker">Atmosphere</span>
          <h3 class="card-heading">氛围中控</h3>
          <div class="toggle-row">
            <span class="toggle-label">沉浸留白</span>
            <button
              class="toggle-switch"
              :class="{ checked: musicMode === 'calm' }"
              role="switch"
              :aria-checked="musicMode === 'calm'"
              @click="setMusicMode('calm')"
            >
              <span class="toggle-knob" />
            </button>
          </div>
          <div class="toggle-row">
            <span class="toggle-label">创作律动</span>
            <button
              class="toggle-switch"
              :class="{ checked: musicMode === 'rhythm' }"
              role="switch"
              :aria-checked="musicMode === 'rhythm'"
              @click="setMusicMode('rhythm')"
            >
              <span class="toggle-knob" />
            </button>
          </div>
        </div>
      </div>

      <!-- Card 5: 内容分区 (row 2, col 3) -->
      <div class="dash-card queue-card">
        <div class="card-top-line" />
        <div class="card-body">
          <span class="card-kicker">Queue</span>
          <h3 class="card-heading">内容分区</h3>
          <div v-if="categories.length" class="tag-list">
            <span
              v-for="cat in categories"
              :key="cat.id"
              class="cat-tag"
            >
              {{ cat.name }}
            </span>
          </div>
          <div v-else class="empty-hint">暂无分区</div>
        </div>
      </div>
    </div>

    <!-- toast -->
    <Transition name="toast-fade">
      <div v-if="toast" class="dash-toast">{{ toast }}</div>
    </Transition>
  </AdminLayout>
</template>

<style scoped>
/* ── Grid layout ── */
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

/* ── Card base ── */
.dash-card {
  background: var(--surface);
  border-radius: var(--radius);
  border: 1px solid var(--line);
  box-shadow: var(--shadow);
  overflow: hidden;
  transition: box-shadow 0.3s var(--ease), transform 0.3s var(--ease);
}

.dash-card:hover {
  box-shadow: var(--shadow-hover);
  transform: translateY(-2px);
}

.card-top-line {
  height: 3px;
  width: 100%;
}

.card-top-line.warm-line {
  background: #e7dfcf;
}

.card-top-line.accent-line {
  background: #9bb8ce;
}

.card-body {
  padding: 24px;
}

/* ── Kicker & heading ── */
.card-kicker {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--secondary);
  margin-bottom: 4px;
}

.card-heading {
  font-family: var(--font-serif);
  font-size: 1.35rem;
  font-weight: 700;
  margin: 0 0 16px;
  color: var(--primary);
}

h3.card-heading {
  font-size: 1.1rem;
}

/* ── Card 1: 灵感捕手 ── */
.spark-card {
  grid-column: span 2;
}

.spark-textarea {
  width: 100%;
  min-height: 140px;
  resize: vertical;
  border: 1px solid var(--line);
  border-radius: calc(var(--radius) - 2px);
  background: var(--base);
  padding: 14px 16px;
  font-size: 0.95rem;
  line-height: 1.7;
  color: var(--primary);
  font-family: inherit;
  transition: border-color 0.24s var(--ease), box-shadow 0.24s var(--ease);
}

.spark-textarea::placeholder {
  color: var(--secondary);
  opacity: 0.65;
}

.spark-textarea:focus {
  outline: none;
  border-color: #9bb8ce;
  box-shadow: 0 0 0 3px rgba(155, 184, 206, 0.18);
}

.spark-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  gap: 12px;
}

.spark-meta {
  font-size: 0.82rem;
  color: var(--secondary);
}

/* ── Card 2: 状态统计 ── */
.metric-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
}

.metric-row:last-child {
  border-bottom: none;
}

.metric-label {
  font-size: 0.82rem;
  color: var(--secondary);
  flex: 0 0 64px;
}

.metric-value {
  font-family: var(--font-serif);
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--primary);
  line-height: 1;
}

.metric-tag {
  font-size: 0.72rem;
  font-weight: 600;
  color: #506d80;
  background: var(--code-bg);
  border-radius: 999px;
  padding: 3px 10px;
}

/* ── Card 3: 最近的光影 ── */
.media-row {
  display: flex;
  gap: 14px;
  justify-content: center;
}

.media-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex: 1;
  max-width: 160px;
  min-width: 0;
}

.media-img {
  width: 100%;
  aspect-ratio: 9 / 16;
  border-radius: 10px;
  object-fit: cover;
  background: var(--code-bg);
}

.media-caption {
  font-size: 0.72rem;
  color: var(--secondary);
  text-align: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.empty-hint {
  font-size: 0.85rem;
  color: var(--secondary);
  opacity: 0.6;
  padding: 12px 0;
}

/* ── Card 4: 氛围中控 ── */
.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
}

.toggle-row:last-child {
  border-bottom: none;
}

.toggle-label {
  font-size: 0.92rem;
  color: var(--primary);
  font-weight: 500;
}

.toggle-switch {
  position: relative;
  width: 48px;
  height: 28px;
  border: none;
  border-radius: 999px;
  background: var(--line);
  cursor: pointer;
  transition: background 0.28s var(--ease);
  flex-shrink: 0;
}

.toggle-switch.checked {
  background: #9bb8ce;
}

.toggle-knob {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
  transition: transform 0.28s var(--ease);
}

.toggle-switch.checked .toggle-knob {
  transform: translateX(20px);
}

/* ── Card 5: 内容分区 ── */
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cat-tag {
  background: var(--code-bg);
  color: #506d80;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 0.82rem;
  font-weight: 600;
  transition: background 0.2s var(--ease), color 0.2s var(--ease);
}

.cat-tag:hover {
  background: #dce8f0;
  color: #3a5568;
}

/* ── Toast ── */
.dash-toast {
  position: fixed;
  bottom: 36px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  background: var(--primary);
  color: var(--base);
  padding: 10px 28px;
  border-radius: 999px;
  font-size: 0.88rem;
  font-weight: 500;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.18);
  pointer-events: none;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.32s var(--ease), transform 0.32s var(--ease);
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(12px);
}

/* ── Responsive ── */
@media (max-width: 1100px) {
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .spark-card {
    grid-column: span 2;
  }
}

@media (max-width: 820px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .spark-card {
    grid-column: span 1;
  }

  .spark-textarea {
    min-height: 100px;
  }

  .media-row {
    flex-direction: row;
  }

  .media-img {
    max-height: 160px;
  }
}
</style>
