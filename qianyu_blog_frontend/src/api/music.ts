import axios from 'axios'
import type { MusicTrack } from '@/types/music'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export interface MusicTrackResponse {
  id: number
  title: string
  artist: string | null
  fileUrl: string
  coverUrl: string | null
  lyricsContent: string | null
  durationSeconds: number | null
  channel: string
}

export const musicApi = {
  async list(channel?: string): Promise<MusicTrack[]> {
    const params = channel ? `?channel=${encodeURIComponent(channel)}` : ''
    const response = await axios.get<{ data: MusicTrackResponse[] }>(
      `${API_BASE_URL}/api/public/music${params}`
    )
    return response.data.data.map((track: MusicTrackResponse) => ({
      id: track.id,
      title: track.title,
      artist: track.artist,
      audioUrl: `${API_BASE_URL}${track.fileUrl}`,
      coverUrl: track.coverUrl ? `${API_BASE_URL}${track.coverUrl}` : null,
      lyrics: track.lyricsContent,
      durationSeconds: track.durationSeconds,
      channel: track.channel
    }))
  }
}
