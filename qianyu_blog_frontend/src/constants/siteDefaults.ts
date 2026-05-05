export const SITE_DEFAULTS = {
  siteName: '千语博客',
  heroTitle: '千语博客',
  heroDescription: '记录学习、开发和生活片段，在代码与文字间寻找平衡',
  heroBadge: '技术 · 生活 · 思考',
  footerText: '用代码和文字持续构建自己的内容站。',
} as const

export function resolveAssetUrl(url?: string | null): string {
  if (!url) return ''
  return url.startsWith('http')
    ? url
    : `${import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}${url}`
}
