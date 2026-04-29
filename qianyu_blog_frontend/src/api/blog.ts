import type { ApiResponse, Category, CategoryPayload, LoginResponse, PageResponse, Post, PostPayload } from '@/types/blog'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

function getToken() {
  return localStorage.getItem('qianyu_admin_token')
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })
  const payload = await response.json() as ApiResponse<T>
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || '请求失败')
  }
  return payload.data
}

export const blogApi = {
  publicCategories: () => request<Category[]>('/api/public/categories'),
  publicPosts: (categoryId?: number) => {
    const query = categoryId ? `?categoryId=${categoryId}` : ''
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
}
