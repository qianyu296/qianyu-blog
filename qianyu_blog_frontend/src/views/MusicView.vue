<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { musicApi } from '@/api/music'
import { useMusicStore } from '@/stores/music'
import type { MusicTrack } from '@/types/music'

const musicStore = useMusicStore()
const tracks = ref<MusicTrack[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const currentMode = ref(localStorage.getItem('qianyu-music-mode') || 'calm')

const moodColor = computed(() => currentMode.value === 'rhythm' ? '#5A8BAE' : '#8C8C8C')

function switchMode(mode: string) {
  currentMode.value = mode
  localStorage.setItem('qianyu-music-mode', mode)
}

onMounted(async () => {
  try {
    loading.value = true
    error.value = null
    tracks.value = await musicApi.list()
    musicStore.tracks = tracks.value
    musicStore.loaded = true
    if (tracks.value.length > 0 && !musicStore.currentTrackId && tracks.value[0]) {
      musicStore.selectTrack(tracks.value[0].id, false)
    }
  } catch (err) {
    console.error('Failed to load music tracks:', err)
    error.value = '加载音乐失败，请稍后重试'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="music-page">
    <header class="section-head">
      <div>
        <div class="section-kicker">Capsule UI</div>
        <h2>声音频道</h2>
      </div>
      <p class="section-note">播放器以悬浮胶囊常驻页面，初始只露出播放按钮；展开后显示当前频道、动态声波和情绪切换。</p>
    </header>

    <div v-if="loading" class="loading">
      <span>加载中...</span>
    </div>

    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <div v-else class="music-layout" :style="{ '--mood-color': moodColor }">
      <section class="music-panel">
        <div class="section-kicker">Now Tuning</div>
        <h2>给阅读留一层很轻的背景。</h2>
        <p>沉浸留白适合慢读与整理，创作律动适合技术拆解和连续写作。频道切换会先渐隐再渐入，避免打断页面原本的呼吸感。</p>

        <div class="channel-board" aria-label="音乐频道说明">
          <article
            class="channel-card"
            :class="{ 'is-current': currentMode === 'calm' }"
            @click="switchMode('calm')"
          >
            <div class="card-meta">Calm</div>
            <h3>沉浸留白</h3>
            <p>低存在感环境音，适合浏览摄影、随笔和归档页。</p>
          </article>
          <article
            class="channel-card"
            :class="{ 'is-current': currentMode === 'rhythm' }"
            @click="switchMode('rhythm')"
          >
            <div class="card-meta">Rhythm</div>
            <h3>创作律动</h3>
            <p>更清晰的拍点，适合阅读技术文章与工作流笔记。</p>
          </article>
        </div>

        <div v-if="tracks.length > 0" class="track-list-section">
          <div class="card-meta" style="margin-top: 34px;">Tracks</div>
          <div class="track-grid">
            <div
              v-for="track in tracks"
              :key="track.id"
              class="track-item"
              :class="{ 'is-playing': musicStore.currentTrackId === track.id }"
              @click="musicStore.selectTrack(track.id, true)"
            >
              <div class="track-info">
                <strong>{{ track.title }}</strong>
                <span class="muted">{{ track.artist || '未知歌手' }}</span>
              </div>
              <button
                class="track-play-btn"
                @click.stop="musicStore.currentTrackId === track.id ? musicStore.toggle() : musicStore.selectTrack(track.id, true)"
              >
                <svg v-if="musicStore.currentTrackId === track.id && musicStore.isPlaying" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M6 4h4v16H6V4zm8 0h4v16h-4V4z"/>
                </svg>
                <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M8 5v14l11-7z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </section>

      <aside class="music-stack">
        <article class="music-note">
          <div class="card-meta">Memory</div>
          <h3>记住上次的节奏</h3>
          <p>频道选择会写入 localStorage。再次回到页面时，胶囊会沿用上次选择，点击播放即可从熟悉的情绪开始。</p>
        </article>
        <article class="music-note">
          <div class="card-meta">Reading Depth</div>
          <h3>阅读深度感应</h3>
          <p>当读者进入偏技术的分析区域时，页面会给出一个克制的小提示，引导切换到更适合持续阅读的节奏。</p>
        </article>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.music-page {
  animation: fadeIn 0.42s var(--ease) both;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 24px;
  margin: 22px 0 26px;
}

.section-head h2 {
  margin: 8px 0 0;
  font-family: var(--font-serif);
  font-size: clamp(2.1rem, 4vw, 4.4rem);
  line-height: 1;
}

.section-note {
  max-width: 420px;
  margin: 0;
  color: var(--secondary);
  line-height: 1.8;
}

.music-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 24px;
  align-items: stretch;
}

.music-panel {
  position: relative;
  overflow: hidden;
  min-height: 430px;
  border: 1px solid rgba(44, 44, 44, 0.06);
  border-radius: var(--radius);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(240, 244, 248, 0.74)),
    var(--surface);
  box-shadow: var(--shadow);
  padding: 34px;
}

.music-panel::before {
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  content: "";
  background: var(--mood-color, #8c8c8c);
  opacity: 0.85;
  transition: background 0.35s var(--ease);
}

.music-panel h2 {
  max-width: 720px;
  margin: 14px 0 18px;
  font-family: var(--font-serif);
  font-size: clamp(2.25rem, 5vw, 5.5rem);
  line-height: 1.02;
}

.music-panel p {
  max-width: 620px;
  margin: 0;
  color: #62625e;
  line-height: 1.9;
}

.channel-board {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 34px;
}

.channel-card {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: color-mix(in srgb, var(--surface) 76%, transparent);
  padding: 18px;
  cursor: pointer;
  transition: transform 0.3s var(--ease), border-color 0.3s var(--ease), background 0.3s var(--ease);
}

.channel-card.is-current {
  border-color: color-mix(in srgb, var(--mood-color, #8c8c8c) 42%, transparent);
  background: color-mix(in srgb, var(--surface) 92%, transparent);
}

.channel-card:hover {
  transform: translateY(-3px);
}

.channel-card h3 {
  margin: 0 0 8px;
  font-family: var(--font-serif);
  font-size: 1.35rem;
}

.channel-card p {
  font-size: 0.95rem;
  color: var(--secondary);
  line-height: 1.6;
}

.track-list-section {
  margin-top: 20px;
}

.track-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.track-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: color-mix(in srgb, var(--surface) 68%, transparent);
  cursor: pointer;
  transition: all 0.24s var(--ease);
}

.track-item:hover {
  background: color-mix(in srgb, var(--surface) 84%, transparent);
  border-color: rgba(44, 44, 44, 0.12);
}

.track-item.is-playing {
  border-color: color-mix(in srgb, var(--mood-color, #8c8c8c) 42%, transparent);
  background: color-mix(in srgb, var(--surface) 92%, transparent);
}

.track-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.track-info strong {
  font-size: 0.95rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.track-info .muted {
  font-size: 0.82rem;
}

.track-play-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: var(--surface);
  display: grid;
  place-items: center;
  color: var(--primary);
  transition: all 0.24s var(--ease);
  flex-shrink: 0;
}

.track-play-btn:hover {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.music-stack {
  display: grid;
  gap: 24px;
}

.music-note {
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: var(--shadow);
  padding: 26px;
}

.music-note h3 {
  margin: 10px 0 12px;
  font-family: var(--font-serif);
  font-size: 1.65rem;
  line-height: 1.2;
}

.music-note p {
  margin: 0;
  color: #62625e;
  line-height: 1.85;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 900px) {
  .section-head {
    display: block;
  }

  .section-note {
    margin-top: 14px;
  }

  .music-layout {
    grid-template-columns: 1fr;
  }

  .channel-board {
    grid-template-columns: 1fr;
  }
}
</style>
