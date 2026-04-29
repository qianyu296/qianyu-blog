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
  status: PostStatus
  category: Category
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
  summary: string
  content: string
  categoryId: number
  status: PostStatus
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresInMinutes: number
}
