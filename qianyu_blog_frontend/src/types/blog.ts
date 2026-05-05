export type PostStatus = 'DRAFT' | 'PUBLISHED'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Category {
  id: number
  name: string
  slug: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface Post {
  id: number
  title: string
  summary: string
  content: string
  coverImageUrl?: string
  status: PostStatus
  isPinned: boolean
  category: Category
  tags: string[]
  createdAt?: string
  updatedAt?: string
}

export interface CategoryPayload {
  name: string
  slug: string
  description?: string
}

export interface PostPayload {
  title: string
  summary?: string
  content: string
  coverImageUrl?: string
  categoryId: number
  tags: string[]
  isPinned: boolean
  status: PostStatus
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresInMinutes: number
}

export interface AiSettings {
  id: number
  chatApiKey: string
  imageApiKey: string
  apiEndpoint: string
  imageApiEndpoint?: string
  modelName: string
  imageModelName?: string
  enabled: boolean
  maxTokens?: number
}

export interface AiChatMessage {
  role: 'user' | 'assistant'
  content: string
  imageUrl?: string
}

export interface AiSettingsPayload {
  chatApiKey: string
  imageApiKey: string
  apiEndpoint: string
  imageApiEndpoint?: string
  modelName: string
  imageModelName?: string
  enabled: boolean
  maxTokens?: number
}

export interface MusicTrack {
  id: number
  title: string
  artist?: string
  originalFileName: string
  contentType: string
  fileSize: number
  durationSeconds?: number
  lyricsContent?: string
  fileUrl: string
  channel?: string
  createdAt?: string
  updatedAt?: string
}

export interface SiteSettings {
  id: number
  siteName: string
  siteSubtitle?: string
  heroBadge?: string
  heroTitle: string
  heroDescription: string
  avatarImageUrl?: string
  heroBackgroundImageUrl?: string
  defaultPostCoverUrl?: string
  githubUrl?: string
  email?: string
  footerText?: string
  createdAt?: string
  updatedAt?: string
}

export interface SiteSettingsPayload {
  siteName: string
  siteSubtitle?: string
  heroBadge?: string
  heroTitle: string
  heroDescription: string
  avatarImageUrl?: string
  heroBackgroundImageUrl?: string
  defaultPostCoverUrl?: string
  githubUrl?: string
  email?: string
  footerText?: string
}

export interface MediaAsset {
  id: number
  originalFileName: string
  contentType: string
  fileSize: number
  displayName?: string
  altText?: string
  fileUrl: string
  createdAt?: string
  updatedAt?: string
}
