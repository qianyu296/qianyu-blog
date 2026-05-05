import type {
  AiSettings,
  AiSettingsPayload,
  ApiResponse,
  Category,
  CategoryPayload,
  LoginResponse,
  MediaAsset,
  MusicTrack,
  PageResponse,
  Post,
  PostPayload,
  SiteSettings,
  SiteSettingsPayload,
} from '@/types/blog'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

function getToken() {
  return localStorage.getItem('qianyu_admin_token')
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (!(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }

  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error
    }
    throw new Error('无法连接到后端服务，请确认后端已经启动且接口地址可访问')
  }

  const contentType = response.headers.get('content-type') ?? ''
  const payload = contentType.includes('application/json')
    ? await response.json() as ApiResponse<T>
    : null

  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      localStorage.removeItem('qianyu_admin_token')
      const isAdminPage = window.location.pathname.startsWith('/admin')
      if (isAdminPage && !window.location.pathname.startsWith('/admin/login')) {
        window.location.href = '/admin/login'
      }
    }
    throw new Error(payload?.message || `请求失败（HTTP ${response.status}）`)
  }

  if (!payload) {
    throw new Error('服务端返回了无法识别的响应')
  }

  if (payload.code !== 0) {
    throw new Error(payload.message || '请求失败')
  }

  return payload.data
}

export const blogApi = {
  publicSiteSettings: () => request<SiteSettings>('/api/public/site/settings'),
  publicCategories: () => request<Category[]>('/api/public/categories'),
  publicTags: () => request<string[]>('/api/public/tags'),
  publicPosts: (categoryId?: number, tag?: string) => {
    const params = new URLSearchParams()
    if (categoryId) params.set('categoryId', String(categoryId))
    if (tag) params.set('tag', tag)
    const query = params.toString() ? `?${params.toString()}` : ''
    return request<PageResponse<Post>>(`/api/public/posts${query}`)
  },
  publicPost: (id: number) => request<Post>(`/api/public/posts/${id}`),
  login: (username: string, password: string) => request<LoginResponse>('/api/admin/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  }),
  adminCategories: () => request<Category[]>('/api/admin/categories'),
  createCategory: (payload: CategoryPayload) => request<Category>('/api/admin/categories', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
  updateCategory: (id: number, payload: CategoryPayload) => request<Category>(`/api/admin/categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }),
  deleteCategory: (id: number) => request<void>(`/api/admin/categories/${id}`, { method: 'DELETE' }),
  adminPosts: () => request<PageResponse<Post>>('/api/admin/posts'),
  adminPost: (id: number) => request<Post>(`/api/admin/posts/${id}`),
  createPost: (payload: PostPayload) => request<Post>('/api/admin/posts', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
  updatePost: (id: number, payload: PostPayload) => request<Post>(`/api/admin/posts/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  }),
  deletePost: (id: number) => request<void>(`/api/admin/posts/${id}`, { method: 'DELETE' }),
  aiChat: (message: string) => request<{ reply: string; model: string }>('/api/ai/chat', {
    method: 'POST',
    body: JSON.stringify({ message }),
  }),
  aiChatStreamUrl: `${API_BASE_URL}/api/ai/chat/stream`,
  getAiSettings: () => request<AiSettings>('/api/admin/ai/settings'),
  saveAiSettings: (payload: AiSettingsPayload) => request<AiSettings>('/api/admin/ai/settings', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
  polishPost: (payload: { title?: string; summary?: string; content: string }) => request<{ content: string }>('/api/admin/ai/polish', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
  getSiteSettings: () => request<SiteSettings>('/api/admin/site-settings'),
  saveSiteSettings: (payload: SiteSettingsPayload) => request<SiteSettings>('/api/admin/site-settings', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
  publicMediaAssets: () => request<MediaAsset[]>('/api/public/media'),
  adminMediaAssets: () => request<MediaAsset[]>('/api/admin/media'),
  uploadMediaAsset: (payload: { file: File; displayName?: string; altText?: string }) => {
    const formData = new FormData()
    formData.set('file', payload.file)
    if (payload.displayName) formData.set('displayName', payload.displayName)
    if (payload.altText) formData.set('altText', payload.altText)
    return request<MediaAsset>('/api/admin/media', {
      method: 'POST',
      body: formData,
    })
  },
  deleteMediaAsset: (id: number) => request<void>(`/api/admin/media/${id}`, {
    method: 'DELETE',
  }),
  publicMusicTracks: (channel?: string) => {
    const params = channel ? `?channel=${encodeURIComponent(channel)}` : ''
    return request<MusicTrack[]>(`/api/public/music${params}`)
  },
  uploadMusic: (payload: { title?: string; artist?: string; channel?: string; audioFile: File; lyricFile?: File }) => {
    const formData = new FormData()
    if (payload.title) formData.set('title', payload.title)
    if (payload.artist) formData.set('artist', payload.artist)
    if (payload.channel) formData.set('channel', payload.channel)
    formData.set('audioFile', payload.audioFile)
    if (payload.lyricFile) formData.set('lyricFile', payload.lyricFile)
    return request<MusicTrack>('/api/admin/music', {
      method: 'POST',
      body: formData,
    })
  },
  updateMusic: (id: number, payload: {
    title?: string
    artist?: string
    lyricsContent?: string
    clearLyrics?: boolean
    audioFile?: File
    lyricFile?: File
  }) => {
    const formData = new FormData()
    if (payload.title !== undefined) formData.set('title', payload.title)
    if (payload.artist !== undefined) formData.set('artist', payload.artist)
    if (payload.lyricsContent !== undefined) formData.set('lyricsContent', payload.lyricsContent)
    if (payload.clearLyrics !== undefined) formData.set('clearLyrics', String(payload.clearLyrics))
    if (payload.audioFile) formData.set('audioFile', payload.audioFile)
    if (payload.lyricFile) formData.set('lyricFile', payload.lyricFile)
    return request<MusicTrack>(`/api/admin/music/${id}`, {
      method: 'PUT',
      body: formData,
    })
  },
  deleteMusic: (id: number) => request<void>(`/api/admin/music/${id}`, {
    method: 'DELETE',
  }),
  generateImage: (payload: { prompt: string; size?: string }, signal?: AbortSignal) => request<{ url: string }>('/api/ai/image', {
    method: 'POST',
    body: JSON.stringify(payload),
    signal,
  }),
}
