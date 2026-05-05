<script setup lang="ts">
import { ref, watch } from 'vue'
import { useAudioPlayer } from '@/composables/useAudioPlayer'
import { useLyrics } from '@/composables/useLyrics'
import { usePlaylist } from '@/composables/usePlaylist'
import { useMusicStore } from '@/stores/music'
import PlayerControls from './PlayerControls.vue'
import LyricsDisplay from './LyricsDisplay.vue'
import PlaylistPanel from './PlaylistPanel.vue'
import type { MusicTrack } from '@/types/music'

interface Props {
  tracks: MusicTrack[]
}

const props = defineProps<Props>()
const musicStore = useMusicStore()

const showPlaylist = ref(false)
const audioRef = ref<HTMLAudioElement | null>(null)

// Audio player composable
const {
  currentTime,
  duration,
  volume,
  isPlaying,
  progress,
  syncVolume,
  onTimeUpdate,
  onLoadedMetadata,
  onSeek,
  seekTo,
  togglePlayback,
} = useAudioPlayer(audioRef)

// Playlist composable
const {
  tracks: playlistTracks,
  selectedTrackId,
  selectedTrack,
  selectedIndex,
  selectedUrl,
  canPrev,
  canNext,
  selectTrack,
  playAdjacent,
} = usePlaylist(audioRef, syncVolume)

// Lyrics composable
const {
  lyricLines,
  currentLineElement,
  activeLyricIndex,
  loadLyrics,
} = useLyrics(currentTime)

// Initialize playlist with props
watch(() => props.tracks, (newTracks) => {
  playlistTracks.value = newTracks
  if (newTracks.length > 0 && !selectedTrackId.value && newTracks[0]) {
    selectTrack(newTracks[0].id, false)
  }
}, { immediate: true })

// Sync from store: if the floating player changed the track, pick it up here
watch(() => musicStore.currentTrackId, (storeId) => {
  if (storeId && storeId !== selectedTrackId.value) {
    selectTrack(storeId, musicStore.isPlaying)
  }
})

// Sync local state to store (for floating player to pick up)
watch(selectedTrackId, (id) => {
  if (id !== undefined) musicStore.currentTrackId = id
})

watch(isPlaying, (playing) => {
  musicStore.isPlaying = playing
})

watch(currentTime, (t) => {
  musicStore.currentTime = t
})

watch(duration, (d) => {
  musicStore.duration = d
})

// Load lyrics when track changes
watch(selectedTrack, (track) => {
  if (track?.lyrics) {
    loadLyrics(track.lyrics)
  } else {
    loadLyrics('')
  }
})

// Update playing state
watch(() => audioRef.value, (audio) => {
  if (!audio) return

  const updatePlayingState = () => {
    isPlaying.value = !audio.paused
  }

  audio.addEventListener('play', updatePlayingState)
  audio.addEventListener('pause', updatePlayingState)
  audio.addEventListener('ended', () => {
    if (canNext.value) {
      playAdjacent(1)
    }
  })
}, { immediate: true })

function handleVolumeChange(value: number) {
  volume.value = value
}
</script>

<template>
  <div class="music-player">
    <!-- Hidden Audio Element -->
    <audio
      ref="audioRef"
      :src="selectedUrl"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoadedMetadata"
    ></audio>

    <div class="player-grid">
      <!-- Album Art Section -->
      <section class="album-section">
        <div class="album-art">
          <div class="album-placeholder">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
            </svg>
          </div>
        </div>

        <div class="track-info">
          <h1>{{ selectedTrack?.title || '还没有选中曲目' }}</h1>
          <p v-if="selectedTrack?.artist">{{ selectedTrack.artist }}</p>
        </div>

        <button
          class="playlist-toggle"
          type="button"
          @click="showPlaylist = !showPlaylist"
        >
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z"/>
          </svg>
          播放列表
        </button>
      </section>

      <!-- Lyrics Section -->
      <LyricsDisplay
        v-if="lyricLines.length > 0"
        :lyrics="lyricLines"
        :current-index="activeLyricIndex"
      />

      <!-- Controls Section -->
      <section class="controls-section">
        <PlayerControls
          :is-playing="isPlaying"
          :current-time="currentTime"
          :duration="duration"
          :volume="volume"
          :is-muted="false"
          :can-prev="canPrev"
          :can-next="canNext"
          @toggle-play="togglePlayback"
          @previous="() => playAdjacent(-1)"
          @next="() => playAdjacent(1)"
          @seek="(time: number) => seekTo(time)"
          @volume-change="handleVolumeChange"
          @toggle-mute="() => {}"
        />
      </section>
    </div>

    <!-- Playlist Panel -->
    <PlaylistPanel
      :is-open="showPlaylist"
      :tracks="playlistTracks"
      :current-track-id="selectedTrackId"
      @close="showPlaylist = false"
      @select-track="selectTrack"
    />
  </div>
</template>

<style scoped>
.music-player {
  position: relative;
  min-height: 100vh;
  padding: 3rem 2rem;
}

.player-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(320px, 1fr) 1fr;
  gap: 2.5rem;
  align-items: start;
}

/* Album Section */
.album-section {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  align-items: center;
}

.album-art {
  width: 100%;
  max-width: 400px;
  aspect-ratio: 1;
  border-radius: 28px;
  overflow: hidden;
  background: linear-gradient(145deg, rgba(99, 102, 241, 0.12), rgba(139, 92, 246, 0.08));
  box-shadow:
    0 20px 48px rgba(99, 102, 241, 0.12),
    0 0 0 1px rgba(99, 102, 241, 0.08);
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.4s ease;
}

.album-art:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow:
    0 28px 64px rgba(99, 102, 241, 0.18),
    0 0 0 1px rgba(99, 102, 241, 0.12);
}

.album-art img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.album-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(99, 102, 241, 0.2);
  background: linear-gradient(145deg, rgba(99, 102, 241, 0.08), rgba(139, 92, 246, 0.05));
}

.album-placeholder svg {
  width: 100px;
  height: 100px;
  animation: pulse-icon 3s ease-in-out infinite;
}

@keyframes pulse-icon {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.05); }
}

/* Track Info */
.track-info {
  text-align: center;
}

.track-info h1 {
  font-family: 'Righteous', cursive;
  font-size: 2rem;
  font-weight: 400;
  letter-spacing: 0.01em;
  color: var(--color-text);
  margin: 0 0 0.5rem;
}

.track-info p {
  font-family: 'Poppins', sans-serif;
  font-size: 1rem;
  font-weight: 300;
  color: var(--color-text-secondary);
  margin: 0;
  letter-spacing: 0.03em;
}

/* Playlist Toggle */
.playlist-toggle {
  padding: 0.875rem 2rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--surface) 76%, transparent);
  border: 1px solid var(--color-accent-subtle);
  backdrop-filter: blur(16px);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.625rem;
  font-family: 'Poppins', sans-serif;
  font-weight: 500;
  font-size: 0.9rem;
  color: var(--color-accent);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  letter-spacing: 0.02em;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.08);
}

.playlist-toggle:hover {
  background: color-mix(in srgb, var(--surface) 92%, transparent);
  border-color: color-mix(in srgb, var(--color-accent) 24%, transparent);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.12);
}

.playlist-toggle svg {
  width: 18px;
  height: 18px;
}

/* Controls Section */
.controls-section {
  grid-column: 1 / -1;
  padding: 2rem;
  border-radius: 24px;
  background: color-mix(in srgb, var(--surface) 72%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-accent) 12%, transparent);
  backdrop-filter: blur(24px);
  box-shadow:
    0 8px 32px rgba(99, 102, 241, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

@media (max-width: 1180px) {
  .player-grid {
    grid-template-columns: 1fr;
    max-width: 600px;
  }

  .album-art {
    max-width: 340px;
  }
}

@media (max-width: 720px) {
  .music-player {
    padding: 2rem 1rem;
  }

  .player-grid {
    gap: 1.5rem;
  }

  .track-info h1 {
    font-size: 1.5rem;
  }

  .album-art {
    max-width: 280px;
    border-radius: 20px;
  }

  .controls-section {
    padding: 1.25rem;
    border-radius: 18px;
  }
}
</style>
