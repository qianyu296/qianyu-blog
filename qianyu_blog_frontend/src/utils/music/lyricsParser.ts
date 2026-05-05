import type { LyricLine } from '@/types/music'

export function parseLyrics(lrcText: string): LyricLine[] {
  if (!lrcText) return []

  const lines = lrcText.split('\n')
  const lyrics: LyricLine[] = []

  const timeRegex = /\[(\d{2}):(\d{2})\.(\d{2,3})\]/g

  for (const line of lines) {
    const matches = [...line.matchAll(timeRegex)]
    if (matches.length === 0) continue

    const text = line.replace(timeRegex, '').trim()
    if (!text) continue

    for (const match of matches) {
      const minutes = parseInt(match[1] ?? '0', 10)
      const seconds = parseInt(match[2] ?? '0', 10)
      const ms = match[3]
      if (!ms) continue
      const milliseconds = parseInt(ms.padEnd(3, '0'), 10)
      const time = minutes * 60 + seconds + milliseconds / 1000

      lyrics.push({ time, text })
    }
  }

  return lyrics.sort((a, b) => a.time - b.time)
}

export function getCurrentLyricIndex(lyrics: LyricLine[], currentTime: number): number {
  if (lyrics.length === 0) return -1

  for (let i = lyrics.length - 1; i >= 0; i--) {
    const lyric = lyrics[i]
    if (lyric && currentTime >= lyric.time) {
      return i
    }
  }

  return -1
}
