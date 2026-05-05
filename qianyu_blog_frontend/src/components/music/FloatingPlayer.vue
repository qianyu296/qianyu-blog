<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMusicStore } from '@/stores/music'

const route = useRoute()
const router = useRouter()
const music = useMusicStore()

const visible = computed(() => {
  return music.currentTrack && route.path !== '/music'
})

const progressPercent = computed(() => music.progress)

function formatTime(seconds: number): string {
  if (!isFinite(seconds)) return '0:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

function goToMusicPage() {
  router.push('/music')
}
</script>

<template>
  <Transition name="float">
    <div v-if="visible" class="floating-player">
      <div class="float-progress" :style="{ width: progressPercent + '%' }"></div>

      <div class="float-body">
        <div class="float-info" @click="goToMusicPage">
          <div class="float-title">{{ music.currentTrack?.title }}</div>
          <div class="float-artist">{{ music.currentTrack?.artist || '未知艺术家' }}</div>
        </div>

        <div class="float-controls">
          <button class="float-btn" :disabled="!music.canPrev" @click="music.goPrev()">
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/></svg>
          </button>
          <button class="float-btn float-play" @click="music.toggle()">
            <svg v-if="!music.isPlaying" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
            <svg v-else viewBox="0 0 24 24" fill="currentColor"><path d="M6 4h4v16H6zm8 0h4v16h-4z"/></svg>
          </button>
          <button class="float-btn" :disabled="!music.canNext" @click="music.goNext()">
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M16 18h2V6h-2zm-11-7l8.5-6v12z"/></svg>
          </button>
        </div>

        <div class="float-time">{{ formatTime(music.currentTime) }}</div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.floating-player {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 340px;
  background: color-mix(in srgb, var(--surface) 84%, transparent);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(99, 102, 241, 0.1);
  border-radius: 18px;
  box-shadow:
    0 12px 40px rgba(99, 102, 241, 0.1),
    0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  z-index: 900;
}

.float-progress {
  height: 3px;
  background: linear-gradient(90deg, #4338CA, #22C55E);
  transition: width 0.3s linear;
  border-radius: 0 3px 0 0;
}

.float-body {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}

.float-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.float-info:hover .float-title {
  color: var(--color-accent);
}

.float-title {
  font-family: 'Poppins', sans-serif;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.2s;
}

.float-artist {
  font-family: 'Poppins', sans-serif;
  font-size: 0.7rem;
  font-weight: 300;
  color: var(--color-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.float-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.float-btn {
  background: none;
  border: none;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-accent);
  border-radius: 50%;
  transition: all 0.2s;
}

.float-btn:hover:not(:disabled) {
  background: var(--color-accent-subtle);
}

.float-btn:disabled {
  opacity: 0.2;
  cursor: not-allowed;
}

.float-btn svg {
  width: 18px;
  height: 18px;
}

.float-play {
  width: 36px;
  height: 36px;
  background: linear-gradient(145deg, #22C55E, #16a34a);
  color: white;
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.25);
}

.float-play:hover:not(:disabled) {
  background: linear-gradient(145deg, #22d367, #22c55e);
  box-shadow: 0 6px 16px rgba(34, 197, 94, 0.35);
  transform: scale(1.05);
}

.float-play svg {
  width: 18px;
  height: 18px;
}

.float-time {
  font-family: 'Poppins', sans-serif;
  font-size: 0.7rem;
  font-weight: 400;
  color: var(--color-text-muted);
  min-width: 36px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

/* Transition */
.float-enter-active {
  animation: float-in 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

.float-leave-active {
  animation: float-in 0.25s ease reverse;
}

@keyframes float-in {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 480px) {
  .floating-player {
    left: 16px;
    right: 16px;
    bottom: 16px;
    width: auto;
  }

  .float-time {
    display: none;
  }
}
</style>
