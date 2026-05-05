import { ref, computed, watch, nextTick, type Ref } from 'vue'
import type { MusicTrack } from '@/types/music'

export function usePlaylist(
  audioRef: Ref<HTMLAudioElement | null>,
  syncVolume: () => void,
) {
  const tracks = ref<MusicTrack[]>([])
  const selectedTrackId = ref<number>()
  const pendingAutoplay = ref(false)

  const selectedTrack = computed(() =>
    tracks.value.find((track) => track.id === selectedTrackId.value),
  )

  const selectedIndex = computed(() =>
    tracks.value.findIndex((track) => track.id === selectedTrackId.value),
  )

  const selectedUrl = computed(() => {
    const audioUrl = selectedTrack.value?.audioUrl
    if (!audioUrl) return ''
    return audioUrl.startsWith('http')
      ? audioUrl
      : `${import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}${audioUrl}`
  })

  const canPrev = computed(() => selectedIndex.value > 0)
  const canNext = computed(() => selectedIndex.value >= 0 && selectedIndex.value < tracks.value.length - 1)

  function selectTrack(trackId: number, autoplay = false) {
    pendingAutoplay.value = autoplay
    selectedTrackId.value = trackId
  }

  function playAdjacent(offset: 1 | -1) {
    if (selectedIndex.value < 0) return
    const next = tracks.value[selectedIndex.value + offset]
    if (!next) return
    selectTrack(next.id, true)
  }

  watch(selectedTrack, async () => {
    await nextTick()
    audioRef.value?.load()
    syncVolume()

    if (pendingAutoplay.value && audioRef.value) {
      pendingAutoplay.value = false
      audioRef.value.play().catch(() => undefined)
      return
    }

    pendingAutoplay.value = false
  }, { immediate: true })

  return {
    tracks,
    selectedTrackId,
    selectedTrack,
    selectedIndex,
    selectedUrl,
    canPrev,
    canNext,
    selectTrack,
    playAdjacent,
  }
}
