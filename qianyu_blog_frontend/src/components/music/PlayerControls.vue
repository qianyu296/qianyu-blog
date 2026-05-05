<template>
  <div class="player-controls">
    <div class="main-controls">
      <button class="control-btn" :disabled="!canPrev" @click="$emit('previous')">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/>
        </svg>
      </button>
      <button class="control-btn play-btn" @click="$emit('toggle-play')">
        <svg v-if="!isPlaying" viewBox="0 0 24 24" fill="currentColor">
          <path d="M8 5v14l11-7z"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="currentColor">
          <path d="M6 4h4v16H6zm8 0h4v16h-4z"/>
        </svg>
      </button>
      <button class="control-btn" :disabled="!canNext" @click="$emit('next')">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M16 18h2V6h-2zm-11-7l8.5-6v12z"/>
        </svg>
      </button>
    </div>

    <div class="progress-section">
      <span class="time">{{ formatTime(currentTime) }}</span>
      <input type="range" class="progress-bar" :value="currentTime" :max="duration" :step="0.1" @input="handleSeek" />
      <span class="time">{{ formatTime(duration) }}</span>
    </div>

    <div class="volume-section">
      <button class="control-btn" @click="$emit('toggle-mute')">
        <svg v-if="!isMuted && volume > 0.5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"/>
        </svg>
        <svg v-else-if="!isMuted && volume > 0" viewBox="0 0 24 24" fill="currentColor">
          <path d="M7 9v6h4l5 5V4l-5 5H7z"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="currentColor">
          <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zM19 12c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/>
        </svg>
      </button>
      <input type="range" class="volume-bar" :value="volume" min="0" max="1" step="0.01" @input="handleVolumeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  isPlaying: boolean
  currentTime: number
  duration: number
  volume: number
  isMuted: boolean
  canPrev: boolean
  canNext: boolean
}>()

const emit = defineEmits<{
  'toggle-play': []
  'previous': []
  'next': []
  'seek': [time: number]
  'volume-change': [volume: number]
  'toggle-mute': []
}>()

function formatTime(seconds: number): string {
  if (!isFinite(seconds)) return '0:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

function handleSeek(event: Event) {
  const input = event.target as HTMLInputElement
  emit('seek', parseFloat(input.value))
}

function handleVolumeChange(event: Event) {
  const input = event.target as HTMLInputElement
  emit('volume-change', parseFloat(input.value))
}
</script>

<style scoped>
.player-controls {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Main Controls */
.main-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1.5rem;
}

.control-btn {
  background: var(--color-accent-subtle);
  border: 1px solid color-mix(in srgb, var(--color-accent) 16%, transparent);
  border-radius: 50%;
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  color: var(--color-accent);
}

.control-btn:hover:not(:disabled) {
  background: color-mix(in srgb, var(--color-accent) 18%, transparent);
  border-color: color-mix(in srgb, var(--color-accent) 28%, transparent);
  transform: scale(1.08);
  color: var(--color-accent-hover);
}

.control-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.control-btn:disabled {
  opacity: 0.2;
  cursor: not-allowed;
  color: var(--color-text-muted);
}

.play-btn {
  width: 68px;
  height: 68px;
  background: linear-gradient(145deg, #22C55E, #16a34a);
  border: none;
  color: white;
  box-shadow:
    0 8px 24px rgba(34, 197, 94, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

.play-btn:hover:not(:disabled) {
  background: linear-gradient(145deg, #22d367, #22c55e);
  box-shadow:
    0 12px 32px rgba(34, 197, 94, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.play-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.control-btn svg {
  width: 22px;
  height: 22px;
}

.play-btn svg {
  width: 30px;
  height: 30px;
}

/* Progress Section */
.progress-section {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.time {
  font-family: 'Poppins', sans-serif;
  font-size: 0.8rem;
  font-weight: 400;
  color: var(--color-text-muted);
  min-width: 42px;
  text-align: center;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.04em;
}

.progress-bar {
  flex: 1;
  height: 5px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.1);
  outline: none;
  cursor: pointer;
  -webkit-appearance: none;
  position: relative;
}

.progress-bar::-webkit-slider-runnable-track {
  height: 5px;
  border-radius: 999px;
}

.progress-bar::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #4338CA;
  cursor: pointer;
  margin-top: -5.5px;
  box-shadow: 0 2px 8px rgba(67, 56, 202, 0.3);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.progress-bar::-webkit-slider-thumb:hover {
  transform: scale(1.3);
  box-shadow: 0 2px 12px rgba(34, 197, 94, 0.35);
  background: #22C55E;
}

.progress-bar::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #4338CA;
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 8px rgba(67, 56, 202, 0.3);
}

/* Volume Section */
.volume-section {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  justify-content: flex-end;
}

.volume-section .control-btn {
  width: 36px;
  height: 36px;
  background: transparent;
  border: none;
}

.volume-section .control-btn svg {
  width: 20px;
  height: 20px;
}

.volume-bar {
  width: 110px;
  height: 4px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.1);
  outline: none;
  cursor: pointer;
  -webkit-appearance: none;
}

.volume-bar::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #6366F1;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(99, 102, 241, 0.3);
  transition: transform 0.15s ease, background 0.15s ease;
}

.volume-bar::-webkit-slider-thumb:hover {
  transform: scale(1.2);
  background: #4338CA;
}

.volume-bar::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #6366F1;
  cursor: pointer;
  border: none;
}

@media (max-width: 720px) {
  .main-controls {
    gap: 1rem;
  }

  .control-btn {
    width: 46px;
    height: 46px;
  }

  .play-btn {
    width: 60px;
    height: 60px;
  }

  .play-btn svg {
    width: 26px;
    height: 26px;
  }

  .volume-bar {
    width: 80px;
  }
}
</style>
