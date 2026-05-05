<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { blogApi } from '@/api/blog'
import type { MediaAsset } from '@/types/blog'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const assets = ref<MediaAsset[]>([])
const loading = ref(true)
const error = ref('')

const PLACEHOLDER_IMAGES = [
  'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=700&q=82',
  'https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=700&q=82',
  'https://images.unsplash.com/photo-1493246507139-91e8fad9978e?auto=format&fit=crop&w=700&q=82',
  'https://images.unsplash.com/photo-1473893604213-3df9c15611c0?auto=format&fit=crop&w=700&q=82',
]

interface GalleryItem {
  id: number
  url: string
  alt: string
  date: string
}

const galleryItems = computed<GalleryItem[]>(() => {
  if (assets.value.length > 0) {
    return assets.value.map((asset) => ({
      id: asset.id,
      url: `${API_BASE_URL}/api/public/media/${asset.id}/file`,
      alt: asset.altText || asset.displayName || asset.originalFileName,
      date: asset.createdAt?.slice(0, 10) ?? '',
    }))
  }
  return PLACEHOLDER_IMAGES.map((url, index) => ({
    id: index,
    url,
    alt: `示例照片 ${index + 1}`,
    date: '',
  }))
})

onMounted(async () => {
  try {
    assets.value = await blogApi.publicMediaAssets()
  } catch {
    assets.value = []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="light-view">
    <section class="section-header">
      <div class="header-left">
        <span class="kicker">Visual Masonry</span>
        <h1 class="title">光影手记</h1>
      </div>
      <p class="header-desc">
        用镜头记录生活中那些转瞬即逝的光影时刻，每一张照片都承载着一段独特的记忆与情感。
      </p>
    </section>

    <div v-if="loading" class="loading">
      <span>正在加载...</span>
    </div>

    <div v-if="error" class="alert alert-error">
      {{ error }}
    </div>

    <section v-if="!loading" class="masonry-grid">
      <div
        v-for="item in galleryItems"
        :key="item.id"
        class="photo-card"
        :data-date="item.date"
      >
        <img :src="item.url" :alt="item.alt" loading="lazy" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.light-view {
  max-width: 1160px;
  margin: 0 auto;
  padding: 0 var(--space-5);
}

/* ---- Section Header ---- */
.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-8);
  margin-bottom: var(--space-10);
  padding-top: var(--space-10);
}

.header-left {
  flex-shrink: 0;
}

.kicker {
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--primary);
  margin-bottom: var(--space-2);
}

.title {
  font-family: var(--font-serif);
  font-size: 42px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text);
  line-height: 1.15;
  margin: 0;
}

.header-desc {
  max-width: 440px;
  font-size: 15px;
  line-height: 1.75;
  color: var(--secondary);
  margin: 0;
  padding-top: var(--space-2);
}

/* ---- Loading ---- */
.loading {
  text-align: center;
  padding: var(--space-16) 0;
  color: var(--secondary);
}

/* ---- Masonry Grid ---- */
.masonry-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

/* ---- Photo Card ---- */
.photo-card {
  position: relative;
  aspect-ratio: 9 / 16;
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: var(--shadow);
  overflow: hidden;
  cursor: pointer;
}

.photo-card:nth-child(even) {
  margin-top: 42px;
}

.photo-card img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s var(--ease);
}

.photo-card:hover img {
  transform: scale(1.02);
}

/* ---- Date Overlay ---- */
.photo-card::after {
  content: attr(data-date);
  position: absolute;
  inset: auto 0 0 0;
  padding: 28px 16px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  letter-spacing: 0.02em;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.42), transparent);
  opacity: 0;
  transition: opacity 0.35s var(--ease);
  pointer-events: none;
}

.photo-card:hover::after {
  opacity: 1;
}

/* ---- Responsive ---- */
@media (max-width: 900px) {
  .masonry-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .section-header {
    flex-direction: column;
    gap: var(--space-4);
  }

  .title {
    font-size: 34px;
  }
}

@media (max-width: 560px) {
  .masonry-grid {
    grid-template-columns: 1fr;
  }

  .photo-card:nth-child(even) {
    margin-top: 0;
  }

  .title {
    font-size: 28px;
  }

  .section-header {
    padding-top: var(--space-8);
    margin-bottom: var(--space-8);
  }
}
</style>
