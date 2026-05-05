export interface LyricLine {
  time: number
  text: string
}

export interface MusicTrack {
  id: number
  title: string
  artist: string | null
  audioUrl: string
  coverUrl?: string | null
  lyrics?: string | null
  durationSeconds: number | null
  channel?: string
}

export interface AudioPlayerState {
  currentTime: number
  duration: number
  volume: number
  isPlaying: boolean
  loading: boolean
}

export interface PlaylistState {
  tracks: MusicTrack[]
  selectedTrackId: number | undefined
  selectedIndex: number
}
