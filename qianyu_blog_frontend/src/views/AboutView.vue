<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { blogApi } from '@/api/blog'
import type { SiteSettings } from '@/types/blog'
import portraitUrl from '../../photo/my.jpg'

const settings = ref<SiteSettings | null>(null)
const fallback = {
  siteName: '千语',
  heroTitle: '写作是给时间留下索引。',
  heroDescription: '每一次落笔，都是在时间的河流里打下一颗钉子。这里记录技术思考、生活碎片与阅读笔记——不求每篇都深刻，但求每篇都真实。',
}

const title = ref(fallback.heroTitle)
const description = ref(fallback.heroDescription)
const siteName = ref(fallback.siteName)

onMounted(async () => {
  try {
    const data = await blogApi.publicSiteSettings()
    settings.value = data
    if (data.siteName) siteName.value = data.siteName
    if (data.heroTitle) title.value = data.heroTitle
    if (data.heroDescription) description.value = data.heroDescription
  } catch {
    // use fallback values
  }
})
</script>

<template>
  <section class="about-page">
    <header class="section-header">
      <span class="kicker">About</span>
      <h1 class="section-title">关于这里</h1>
    </header>

    <div class="about-grid">
      <div class="portrait-wrap">
        <div class="portrait">
          <img :src="portraitUrl" :alt="siteName" />
        </div>
      </div>

      <div class="about-copy">
        <h2 class="copy-title">{{ title }}</h2>
        <p class="copy-lead">
          {{ description }}
        </p>
        <p class="copy-body">
          这个空间由 {{ siteName }} 打理，用代码构建骨架，用文字填充血肉。如果你也在寻找一种安静的表达方式，欢迎常来坐坐。
        </p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.about-page {
  width: 100%;
  max-width: 1160px;
  margin: 0 auto;
  padding: 80px 24px 100px;
}

.section-header {
  text-align: center;
  margin-bottom: 52px;
}

.kicker {
  display: inline-block;
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--primary);
  margin-bottom: 8px;
}

.section-title {
  font-family: var(--font-display, serif);
  font-size: clamp(1.8rem, 4.5vw, 3.2rem);
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
  line-height: 1.2;
}

.about-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.82fr) minmax(0, 1.18fr);
  gap: 28px;
  align-items: start;
}

/* Portrait */
.portrait-wrap {
  position: relative;
}

.portrait {
  overflow: hidden;
  border-radius: var(--radius, 12px);
  box-shadow: var(--shadow, 0 8px 30px rgba(0, 0, 0, 0.08));
}

.portrait img {
  display: block;
  width: 100%;
  aspect-ratio: 4 / 5;
  object-fit: cover;
}

/* Text card */
.about-copy {
  padding: 34px;
  border-radius: var(--radius, 12px);
  background: var(--surface, #fff);
  box-shadow: var(--shadow, 0 8px 30px rgba(0, 0, 0, 0.08));
}

.copy-title {
  font-family: var(--font-display, serif);
  font-size: clamp(2.2rem, 5vw, 5rem);
  font-weight: 700;
  line-height: 1.15;
  color: var(--color-text);
  margin: 0 0 24px;
}

.copy-lead {
  font-size: 1.05rem;
  line-height: 1.85;
  color: var(--color-text-secondary);
  margin: 0 0 18px;
}

.copy-body {
  font-size: 0.95rem;
  line-height: 1.8;
  color: var(--color-text-muted);
  margin: 0;
}

/* Responsive */
@media (max-width: 900px) {
  .about-grid {
    grid-template-columns: 1fr;
  }

  .portrait {
    max-width: 400px;
    margin: 0 auto;
  }
}
</style>
