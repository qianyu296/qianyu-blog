<template>
  <div class="playlist-panel" :class="{ open: isOpen }">
    <div class="panel-header">
      <h3>播放列表 ({{ tracks.length }})</h3>
      <button class="close-btn" @click="$emit('close')">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
        </svg>
      </button>
    </div>

    <div class="panel-actions">
      <button class="action-btn" @click="$emit('add-files')">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
        </svg>
        添加音乐
      </button>
      <button class="action-btn" :disabled="tracks.length === 0" @click="$emit('clear')">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
        </svg>
        清空列表
      </button>
    </div>

    <div class="track-list">
      <div
        v-for="(track, index) in tracks"
        :key="track.id"
        class="track-item"
        :class="{ active: track.id === currentTrackId }"
        @click="$emit('select', track.id)"
      >
        <div class="track-index">{{ index + 1 }}</div>
        <div class="track-info">
          <div class="track-title">{{ track.title }}</div>
          <div class="track-artist">{{ track.artist || '未知艺术家' }}</div>
        </div>
        <div class="track-duration">{{ formatDuration(track.durationSeconds) }}</div>
        <button class="remove-btn" @click.stop="$emit('remove', index)">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { MusicTrack } from '@/types/music'

defineProps<{
  isOpen: boolean
  tracks: MusicTrack[]
  currentTrackId?: number
}>()

defineEmits<{
  'close': []
  'add-files': []
  'clear': []
  'select': [trackId: number]
  'remove': [index: number]
}>()

function formatDuration(seconds: number | null): string {
  if (!seconds) return '--:--'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}
</script>

<style scoped>
.playlist-panel {
  position: fixed;
  right: -420px;
  top: 0;
  bottom: 0;
  width: 400px;
  background: rgba(240, 244, 255, 0.92);
  backdrop-filter: blur(40px);
  transition: right 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  display: flex;
  flex-direction: column;
  z-index: 1000;
  border-left: 1px solid rgba(99, 102, 241, 0.1);
  box-shadow: -16px 0 48px rgba(99, 102, 241, 0.08);
}

.playlist-panel.open {
  right: 0;
}

/* Header */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem 1.5rem 1.25rem;
  border-bottom: 1px solid rgba(99, 102, 241, 0.08);
}

.panel-header h3 {
  margin: 0;
  font-family: 'Righteous', cursive;
  font-size: 1.2rem;
  font-weight: 400;
  color: var(--color-text);
  letter-spacing: 0.02em;
}

.close-btn {
  background: var(--color-accent-subtle);
  border: 1px solid color-mix(in srgb, var(--color-accent) 16%, transparent);
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: color-mix(in srgb, var(--color-accent) 18%, transparent);
  color: var(--color-accent);
}

.close-btn svg {
  width: 20px;
  height: 20px;
}

/* Actions */
.panel-actions {
  display: flex;
  gap: 0.5rem;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid rgba(99, 102, 241, 0.08);
}

.action-btn {
  flex: 1;
  padding: 0.625rem 0.875rem;
  background: var(--color-accent-subtle);
  border: 1px solid color-mix(in srgb, var(--color-accent) 16%, transparent);
  border-radius: 10px;
  color: var(--color-accent);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-family: 'Poppins', sans-serif;
  font-size: 0.8rem;
  font-weight: 400;
  transition: all 0.2s ease;
}

.action-btn:hover:not(:disabled) {
  background: color-mix(in srgb, var(--color-accent) 18%, transparent);
  border-color: color-mix(in srgb, var(--color-accent) 28%, transparent);
  color: var(--color-accent-hover);
}

.action-btn:disabled {
  opacity: 0.25;
  cursor: not-allowed;
}

.action-btn svg {
  width: 16px;
  height: 16px;
}

/* Track List */
.track-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem;
  scrollbar-width: thin;
  scrollbar-color: rgba(99, 102, 241, 0.12) transparent;
}

.track-list::-webkit-scrollbar {
  width: 4px;
}

.track-list::-webkit-scrollbar-thumb {
  background: rgba(99, 102, 241, 0.12);
  border-radius: 999px;
}

.track-item {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--color-text-secondary);
  border: 1px solid transparent;
}

.track-item:hover {
  background: var(--color-accent-subtle);
  border-color: color-mix(in srgb, var(--color-accent) 16%, transparent);
  color: var(--color-text);
}

.track-item.active {
  background: var(--color-success-light);
  border-color: color-mix(in srgb, var(--color-success) 28%, transparent);
  color: var(--color-text);
}

.track-item.active .track-index {
  color: #22C55E;
}

.track-index {
  font-family: 'Poppins', sans-serif;
  font-size: 0.8rem;
  font-weight: 400;
  min-width: 24px;
  text-align: center;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

.track-info {
  flex: 1;
  min-width: 0;
}

.track-title {
  font-family: 'Poppins', sans-serif;
  font-weight: 500;
  font-size: 0.9rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.track-artist {
  font-family: 'Poppins', sans-serif;
  font-size: 0.75rem;
  font-weight: 300;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.track-item.active .track-artist {
  color: var(--color-text-secondary);
}

.track-duration {
  font-family: 'Poppins', sans-serif;
  font-size: 0.75rem;
  font-weight: 300;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

.remove-btn {
  background: none;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  opacity: 0;
  transition: all 0.2s ease;
}

.track-item:hover .remove-btn {
  opacity: 1;
}

.remove-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.remove-btn svg {
  width: 16px;
  height: 16px;
}
</style>
