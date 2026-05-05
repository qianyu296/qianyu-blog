import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { musicApi } from '@/api/music'
import type { MusicTrack } from '@/types/music'

export const useMusicStore = defineStore('music', () => {
  const audio = new Audio()
  audio.preload = 'auto'

  const tracks = ref<MusicTrack[]>([])
  const currentTrackId = ref<number>()
  const isPlaying = ref(false)
  const currentTime = ref(0)
  const duration = ref(0)
  const volume = ref(0.74)
  const loaded = ref(false)
  const channel = ref<string>(localStorage.getItem('qianyu-music-mode') || 'calm')

  const currentTrack = computed(() =>
    tracks.value.find((t) => t.id === currentTrackId.value),
  )

  const currentIndex = computed(() =>
    tracks.value.findIndex((t) => t.id === currentTrackId.value),
  )

  const progress = computed(() =>
    duration.value > 0 ? Math.min((currentTime.value / duration.value) * 100, 100) : 0,
  )

  const hasMultipleTracks = computed(() => tracks.value.length > 1)
  const canPrev = computed(() => hasMultipleTracks.value && currentIndex.value >= 0)
  const canNext = computed(() => hasMultipleTracks.value && currentIndex.value >= 0)

  // Audio event listeners
  audio.addEventListener('timeupdate', () => {
    currentTime.value = audio.currentTime
  })

  audio.addEventListener('loadedmetadata', () => {
    duration.value = audio.duration
  })

  audio.addEventListener('play', () => {
    isPlaying.value = true
  })

  audio.addEventListener('pause', () => {
    isPlaying.value = false
  })

  audio.addEventListener('ended', () => {
    if (tracks.value.length === 0) {
      isPlaying.value = false
      return
    }
    goNext(true)
  })

  async function loadTracks(ch?: string) {
    const targetChannel = ch || channel.value
    try {
      tracks.value = await musicApi.list(targetChannel)
      loaded.value = true
      if (tracks.value.length > 0 && tracks.value[0]) {
        selectTrack(tracks.value[0].id, false)
      } else {
        currentTrackId.value = undefined
      }
    } catch {
      // silently fail
    }
  }

  function switchChannel(newChannel: string) {
    channel.value = newChannel
    localStorage.setItem('qianyu-music-mode', newChannel)
    audio.pause()
    isPlaying.value = false
    currentTrackId.value = undefined
    loaded.value = false
    loadTracks(newChannel)
  }

  function selectTrack(trackId: number, autoplay = false) {
    currentTrackId.value = trackId
    const track = currentTrack.value
    if (!track?.audioUrl) return
    audio.src = track.audioUrl
    audio.volume = volume.value
    audio.load()
    if (autoplay) {
      audio.play().catch((err) => {
        console.warn('[music] play failed:', err)
      })
    }
  }

  function play() {
    if (!currentTrackId.value && tracks.value.length > 0 && tracks.value[0]) {
      selectTrack(tracks.value[0].id, true)
      return
    }
    audio.play().catch((err) => {
      console.warn('[music] play failed:', err)
    })
  }

  function pause() {
    audio.pause()
  }

  function toggle() {
    if (isPlaying.value) {
      pause()
    } else {
      play()
    }
  }

  function goPrev(wrap = false) {
    if (tracks.value.length === 0 || currentIndex.value < 0) return
    if (currentIndex.value > 0) {
      const prev = tracks.value[currentIndex.value - 1]
      if (prev) selectTrack(prev.id, true)
      return
    }
    if (wrap) {
      const prev = tracks.value[tracks.value.length - 1]
      if (prev) selectTrack(prev.id, true)
    }
  }

  function goNext(wrap = false) {
    if (tracks.value.length === 0 || currentIndex.value < 0) return
    if (currentIndex.value < tracks.value.length - 1) {
      const next = tracks.value[currentIndex.value + 1]
      if (next) selectTrack(next.id, true)
      return
    }
    if (wrap) {
      const next = tracks.value[0]
      if (next) selectTrack(next.id, true)
    }
  }

  function seekTo(time: number) {
    audio.currentTime = time
    currentTime.value = time
  }

  function setVolume(v: number) {
    volume.value = v
    audio.volume = v
  }

  return {
    tracks,
    currentTrackId,
    currentTrack,
    currentIndex,
    isPlaying,
    currentTime,
    duration,
    volume,
    progress,
    canPrev,
    canNext,
    loaded,
    channel,
    loadTracks,
    switchChannel,
    selectTrack,
    play,
    pause,
    toggle,
    goPrev,
    goNext,
    seekTo,
    setVolume,
  }
})
