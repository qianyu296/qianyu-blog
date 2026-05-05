<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useMusicStore } from '@/stores/music'

const musicStore = useMusicStore()

type MusicMode = 'calm' | 'rhythm'

const STORAGE_KEY = 'qianyu-music-mode'

const mode = ref<MusicMode>((localStorage.getItem(STORAGE_KEY) as MusicMode) || 'calm')
const isHovered = ref(false)

// Keep mode in sync with store channel
watch(() => musicStore.channel, (ch) => {
  mode.value = ch as MusicMode
})
const showVolume = ref(false)
const isMuted = ref(false)
const prevVolume = ref(0.74)
const volumeBtnEl = ref<HTMLElement | null>(null)
const volumePopupStyle = ref({})
let volumeCloseTimer: number | null = null

const modeLabel = computed(() => (mode.value === 'calm' ? '沉浸留白' : '创作律动'))

const isExpanded = computed(() => isHovered.value || showVolume.value || musicStore.isPlaying)

const songName = computed(() => musicStore.currentTrack?.title || '未知曲目')

const volumeIcon = computed(() => {
  if (isMuted.value || musicStore.volume === 0) return 'muted'
  if (musicStore.volume < 0.5) return 'low'
  return 'high'
})

function togglePlay() {
  musicStore.toggle()
}

function toggleMode() {
  const next = mode.value === 'calm' ? 'rhythm' : 'calm'
  musicStore.switchChannel(next)
}

function toggleMute() {
  if (isMuted.value) {
    isMuted.value = false
    musicStore.setVolume(prevVolume.value)
  } else {
    prevVolume.value = musicStore.volume
    isMuted.value = true
    musicStore.setVolume(0)
  }
}

function onVolumeInput(e: Event) {
  const val = Number((e.target as HTMLInputElement).value)
  musicStore.setVolume(val)
  isMuted.value = val === 0
}

function openVolume() {
  if (volumeCloseTimer !== null) {
    window.clearTimeout(volumeCloseTimer)
    volumeCloseTimer = null
  }
  if (volumeBtnEl.value) {
    const rect = volumeBtnEl.value.getBoundingClientRect()
    volumePopupStyle.value = {
      position: 'fixed',
      left: rect.left + rect.width / 2 - 18 + 'px',
      bottom: window.innerHeight - rect.top + 10 + 'px',
      zIndex: 9999,
    }
  }
  showVolume.value = true
}

function closeVolume() {
  if (volumeCloseTimer !== null) {
    window.clearTimeout(volumeCloseTimer)
  }
  volumeCloseTimer = window.setTimeout(() => {
    showVolume.value = false
    volumeCloseTimer = null
  }, 120)
}

function onMouseEnter() {
  isHovered.value = true
}

function onMouseLeave() {
  if (!musicStore.isPlaying) {
    isHovered.value = false
  }
}

// Sync hover collapse when playback stops
watch(
  () => musicStore.isPlaying,
  (playing) => {
    if (!playing && !isHovered.value) {
      // will collapse naturally via isExpanded
    }
  },
)

// Set CSS variable for mood color
watch(
  mode,
  (m) => {
    document.documentElement.style.setProperty(
      '--mood-color',
      m === 'rhythm' ? '#5A8BAE' : '#8c8c8c',
    )
  },
  { immediate: true },
)

watch(showVolume, (visible) => {
  if (visible) {
    isHovered.value = true
  }
})

onMounted(() => {
  musicStore.loadTracks()
})

onUnmounted(() => {
  if (volumeCloseTimer !== null) {
    window.clearTimeout(volumeCloseTimer)
  }
})
</script>

<template>
  <div
    class="music-capsule"
    :class="{ 'is-expanded': isExpanded, 'is-playing': musicStore.isPlaying }"
    @mouseenter="onMouseEnter"
    @mouseleave="onMouseLeave"
  >
    <button
      class="play-btn"
      :aria-label="musicStore.isPlaying ? '暂停' : '播放'"
      @click="togglePlay"
    >
      <span v-if="!musicStore.isPlaying" class="icon-play" />
      <span v-else class="icon-pause" />
    </button>

    <div class="capsule-info">
      <span class="song-name">{{ songName }}</span>
      <div class="visualizer" aria-hidden="true">
        <span class="bar bar-1" />
        <span class="bar bar-2" />
        <span class="bar bar-3" />
      </div>
      <button
        class="next-btn"
        aria-label="下一首"
        :disabled="musicStore.tracks.length === 0"
        @click.stop="musicStore.goNext(true)"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M16 18h2V6h-2zm-11-7l8.5-6v12z" />
        </svg>
      </button>
      <button
        ref="volumeBtnEl"
        class="volume-btn"
        :aria-label="isMuted ? '取消静音' : '静音'"
        @click.stop="toggleMute"
        @mouseenter="openVolume"
        @mouseleave="closeVolume"
      >
        <svg v-if="volumeIcon === 'high'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
          <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
          <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
        </svg>
        <svg v-else-if="volumeIcon === 'low'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
          <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
        </svg>
        <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
          <line x1="23" y1="9" x2="17" y2="15" />
          <line x1="17" y1="9" x2="23" y2="15" />
        </svg>
      </button>
      <Teleport to="body">
        <Transition name="vol-pop">
          <div
            v-if="showVolume"
            class="volume-slider-wrap"
            :style="volumePopupStyle"
            @click.stop
            @mouseenter="openVolume"
            @mouseleave="closeVolume"
          >
            <input
              type="range"
              class="volume-slider"
              min="0"
              max="1"
              step="0.01"
              :value="musicStore.volume"
              @input="onVolumeInput"
            />
          </div>
        </Transition>
      </Teleport>
      <button class="mode-btn" @click.stop="toggleMode">
        {{ modeLabel }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.music-capsule {
  --mood-color: #8c8c8c;
  --text: var(--color-text, #1a1a1a);

  position: fixed;
  right: max(18px, calc((100vw - 1160px) / 2));
  bottom: 22px;
  z-index: 30;

  display: grid;
  grid-template-columns: 50px minmax(0, 1fr);
  align-items: center;

  width: 50px;
  overflow: hidden;
  border-radius: 25px;

  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  background: color-mix(in srgb, var(--surface, #ffffff) 60%, transparent);
  box-shadow: var(--shadow, 0 4px 20px rgba(0, 0, 0, 0.08));

  transition: width var(--ease, 0.35s cubic-bezier(0.4, 0, 0.2, 1));
}

.music-capsule::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: var(--mood-color);
  border-radius: 1px;
}

.music-capsule.is-expanded {
  width: 322px;
}

/* Play button */
.play-btn {
  width: 50px;
  height: 50px;
  border: none;
  background: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

/* Play triangle (CSS border trick) */
.icon-play {
  display: block;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 7px 0 7px 12px;
  border-color: transparent transparent transparent var(--text, #1a1a1a);
  margin-left: 2px;
}

/* Pause bars */
.icon-pause {
  display: flex;
  gap: 3px;
  align-items: center;
}

.icon-pause::before,
.icon-pause::after {
  content: '';
  display: block;
  width: 3px;
  height: 13px;
  background: var(--text, #1a1a1a);
  border-radius: 1.5px;
}

/* Expanded info */
.capsule-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding-right: 14px;
  opacity: 0;
  transition: opacity var(--ease, 0.35s cubic-bezier(0.4, 0, 0.2, 1));
}

.is-expanded .capsule-info {
  opacity: 1;
}

.song-name {
  flex: 1;
  min-width: 0;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--text, #1a1a1a);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Visualizer */
.visualizer {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 14px;
}

.bar {
  display: block;
  width: 3px;
  background: var(--mood-color);
  border-radius: 1px;
  height: 4px;
  transition: height 0.2s ease;
}

/* Animated bars when playing */
.is-playing .bar {
  animation: soundBar 0.8s ease-in-out infinite alternate;
}

.is-playing .bar-1 {
  animation-delay: 0s;
}

.is-playing .bar-2 {
  animation-delay: 0.2s;
}

.is-playing .bar-3 {
  animation-delay: 0.4s;
}

@keyframes soundBar {
  0% {
    height: 4px;
  }
  50% {
    height: 12px;
  }
  100% {
    height: 6px;
  }
}

/* Mode button */
.mode-btn {
  flex-shrink: 0;
  border: none;
  background: none;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--mood-color);
  cursor: pointer;
  padding: 2px 0;
  white-space: nowrap;
  transition: color var(--ease, 0.35s);
}

.mode-btn:hover {
  color: var(--text);
}

/* Volume button */
.next-btn,
.volume-btn {
  position: relative;
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--mood-color);
  transition: color var(--ease, 0.35s);
  border-radius: 50%;
}

.next-btn:hover,
.volume-btn:hover {
  color: var(--text);
  background: var(--color-surface-hover, rgba(0, 0, 0, 0.04));
}

.next-btn:disabled {
  opacity: 0.32;
  cursor: not-allowed;
}

/* Volume slider popup */
.volume-slider-wrap {
  width: 36px;
  height: 120px;
  background: color-mix(in srgb, var(--surface, #ffffff) 85%, transparent);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 0;
  pointer-events: auto;
}

.volume-slider {
  width: 100px;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: var(--line, #e0e0e0);
  border-radius: 2px;
  outline: none;
  cursor: pointer;
  padding: 0;
  transform: rotate(-90deg);
  transform-origin: center center;
}

.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--mood-color);
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.18);
  transition: transform 0.15s ease;
}

.volume-slider::-webkit-slider-thumb:hover {
  transform: scale(1.2);
}

.volume-slider::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--mood-color);
  cursor: pointer;
  border: none;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.18);
}

.vol-pop-enter-active,
.vol-pop-leave-active {
  transition: opacity 0.2s ease;
}

.vol-pop-enter-from,
.vol-pop-leave-to {
  opacity: 0;
}
</style>
