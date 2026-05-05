import { ref, computed, watch, nextTick, type Ref } from 'vue'
import type { LyricLine } from '@/types/music'
import { parseLrc } from '@/utils/lyricsParser'

export function useLyrics(currentTime: Ref<number>) {
  const lyricLines = ref<LyricLine[]>([])
  const currentLineElement = ref<HTMLElement | null>(null)

  const activeLyricIndex = computed(() => {
    if (lyricLines.value.length === 0) return -1

    let index = -1
    for (let i = 0; i < lyricLines.value.length; i += 1) {
      if ((lyricLines.value[i]?.time ?? Infinity) <= currentTime.value) {
        index = i
        continue
      }
      break
    }
    return index
  })

  function loadLyrics(content?: string) {
    lyricLines.value = parseLrc(content)
  }

  watch(activeLyricIndex, () => {
    currentLineElement.value?.scrollIntoView({ block: 'center', behavior: 'smooth' })
  })

  return {
    lyricLines,
    currentLineElement,
    activeLyricIndex,
    loadLyrics,
  }
}
