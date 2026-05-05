import type { LyricLine } from '@/types/music'

export function parseLrc(content?: string): LyricLine[] {
  if (!content) {
    return []
  }

  const pattern = /\[(\d{2}):(\d{2})(?:\.(\d{1,3}))?\]/g
  const list: LyricLine[] = []

  for (const row of content.split(/\r?\n/)) {
    const text = row.replace(pattern, '').trim() || '...'
    const matches = [...row.matchAll(pattern)]
    for (const match of matches) {
      const minutes = Number(match[1] || 0)
      const seconds = Number(match[2] || 0)
      const fraction = Number((match[3] || '0').padEnd(3, '0'))
      list.push({
        time: minutes * 60 + seconds + fraction / 1000,
        text,
      })
    }
  }

  return list.sort((a, b) => a.time - b.time)
}
