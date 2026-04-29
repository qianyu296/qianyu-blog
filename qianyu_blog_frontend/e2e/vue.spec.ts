import { test, expect } from '@playwright/test'
import type { Page, Route } from '@playwright/test'

const categories = [{ id: 1, name: '技术', slug: 'tech', description: '技术文章' }]
const post = {
  id: 1,
  title: '第一篇博客',
  summary: '这是摘要',
  content: '这是文章内容',
  status: 'PUBLISHED',
  category: categories[0],
  createdAt: '2026-04-29T12:00:00',
}

async function success(route: Route, data: unknown) {
  await route.fulfill({ json: { code: 0, message: 'success', data } })
}

async function mockApi(page: Page) {
  await page.route('**/api/public/categories', async route => success(route, categories))
  await page.route('**/api/public/posts', async route => success(route, { content: [post], page: 0, size: 10, totalElements: 1, totalPages: 1 }))
  await page.route('**/api/public/posts/1', async route => success(route, post))
  await page.route('**/api/admin/auth/login', async route => success(route, { token: 'mock-token', tokenType: 'Bearer', expiresInMinutes: 60 }))
  await page.route('**/api/admin/categories', async route => {
    if (route.request().method() === 'POST') {
      await success(route, { ...categories[0], id: 2 })
      return
    }
    await success(route, categories)
  })
  await page.route('**/api/admin/categories/1', async route => {
    const method = route.request().method()
    if (method === 'PUT') {
      await success(route, { ...categories[0], name: '技术更新' })
      return
    }
    if (method === 'DELETE') {
      await success(route, null)
      return
    }
    await route.fallback()
  })
  await page.route('**/api/admin/posts', async route => {
    if (route.request().method() === 'POST') {
      await success(route, { ...post, id: 2 })
      return
    }
    await success(route, { content: [post], page: 0, size: 10, totalElements: 1, totalPages: 1 })
  })
  await page.route('**/api/admin/posts/1', async route => {
    const method = route.request().method()
    if (method === 'GET') {
      await success(route, post)
      return
    }
    if (method === 'PUT') {
      await success(route, { ...post, title: '更新后的博客' })
      return
    }
    if (method === 'DELETE') {
      await success(route, null)
      return
    }
    await route.fallback()
  })
}

test('renders public blog home and opens post detail', async ({ page }) => {
  await mockApi(page)
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '千语博客' })).toBeVisible()
  await expect(page.getByRole('link', { name: '第一篇博客' })).toBeVisible()
  await page.getByRole('link', { name: '第一篇博客' }).click()
  await expect(page.getByRole('heading', { name: '第一篇博客' })).toBeVisible()
  await expect(page.getByText('这是文章内容')).toBeVisible()
})

test('redirects protected admin route to login and logs in', async ({ page }) => {
  await mockApi(page)
  await page.goto('/admin/posts')
  await expect(page.getByRole('heading', { name: '管理员登录' })).toBeVisible()
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/admin\/posts/)
  await expect(page.getByRole('heading', { name: '文章管理' })).toBeVisible()
})

test('manages categories', async ({ page }) => {
  await mockApi(page)
  await page.goto('/admin/login')
  await page.getByRole('button', { name: '登录' }).click()
  await page.getByRole('link', { name: '分类' }).click()
  await expect(page.getByText('tech · 技术文章')).toBeVisible()

  await page.getByLabel('名称').fill('生活')
  await page.getByLabel('标识').fill('life')
  await page.getByLabel('描述').fill('生活记录')
  await page.getByRole('button', { name: '新增分类' }).click()
  await expect(page.getByLabel('名称')).toHaveValue('')

  await page.getByRole('button', { name: '编辑' }).click()
  await page.getByLabel('名称').fill('技术更新')
  await page.getByRole('button', { name: '保存分类' }).click()
  await expect(page.getByLabel('名称')).toHaveValue('')
})

test('manages posts', async ({ page }) => {
  await mockApi(page)
  page.on('dialog', dialog => dialog.accept())
  await page.goto('/admin/login')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByText('第一篇博客')).toBeVisible()

  await page.getByRole('link', { name: '新建文章' }).click()
  await page.getByLabel('标题').fill('新文章')
  await page.getByLabel('摘要').fill('新摘要')
  await page.getByLabel('内容').fill('新内容')
  await page.getByRole('button', { name: '保存' }).click()
  await expect(page).toHaveURL(/\/admin\/posts/)

  await page.getByRole('link', { name: '编辑' }).click()
  await page.getByLabel('标题').fill('更新后的博客')
  await page.getByRole('button', { name: '保存' }).click()
  await expect(page).toHaveURL(/\/admin\/posts/)

  await page.getByRole('button', { name: '删除' }).click()
  await expect(page.getByRole('heading', { name: '文章管理' })).toBeVisible()
})
