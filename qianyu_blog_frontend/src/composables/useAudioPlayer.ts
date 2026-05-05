import { ref, computed, watch, type Ref } from 'vue'
import type { AudioPlayerState } from '@/types/music'

export function useAudioPlayer(audioRef: Ref<HTMLAudioElement | null>) {
  const currentTime = ref(0)
  const duration = ref(0)
  const volume = ref(0.74)
  const isPlaying = ref(false)
  const loading = ref(false)

  const progress = computed(() =>
    duration.value > 0 ? Math.min((currentTime.value / duration.value) * 100, 100) : 0,
  )

  function syncVolume() {
    if (audioRef.value) {
      audioRef.value.volume = volume.value
    }
  }

  function onTimeUpdate() {
    currentTime.value = audioRef.value?.currentTime ?? 0
  }

  function onLoadedMetadata() {
    duration.value = audioRef.value?.duration ?? 0
  }

  function onSeek(event: Event) {
    const input = event.target as HTMLInputElement
    const next = Number(input.value)
    currentTime.value = next
    if (audioRef.value) {
      audioRef.value.currentTime = next
    }
  }

  function seekTo(time: number) {
    currentTime.value = time
    if (audioRef.value) {
      audioRef.value.currentTime = time
    }
  }

  function togglePlayback() {
    if (!audioRef.value) return
    if (audioRef.value.paused) {
      audioRef.value.play().catch(() => undefined)
      return
    }
    audioRef.value.pause()
  }

  watch(volume, syncVolume)

  return {
    currentTime,
    duration,
    volume,
    isPlaying,
    loading,
    progress,
    syncVolume,
    onTimeUpdate,
    onLoadedMetadata,
    onSeek,
    seekTo,
    togglePlayback,
  }
}
