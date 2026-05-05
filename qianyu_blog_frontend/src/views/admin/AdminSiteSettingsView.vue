<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { blogApi } from '@/api/blog'
import type { SiteSettingsPayload } from '@/types/blog'
import { SITE_DEFAULTS, resolveAssetUrl } from '@/constants/siteDefaults'
import AdminLayout from '@/components/AdminLayout.vue'

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

const form = ref<SiteSettingsPayload>({
  siteName: SITE_DEFAULTS.siteName,
  siteSubtitle: '',
  heroBadge: '',
  heroTitle: SITE_DEFAULTS.heroTitle,
  heroDescription: '',
  avatarImageUrl: '',
  heroBackgroundImageUrl: '',
  defaultPostCoverUrl: '',
  githubUrl: '',
  email: '',
  footerText: '',
})

async function loadSettings() {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const settings = await blogApi.getSiteSettings()
    form.value = {
      siteName: settings.siteName || SITE_DEFAULTS.siteName,
      siteSubtitle: settings.siteSubtitle || '',
      heroBadge: settings.heroBadge || '',
      heroTitle: settings.heroTitle || SITE_DEFAULTS.heroTitle,
      heroDescription: settings.heroDescription || '',
      avatarImageUrl: settings.avatarImageUrl || '',
      heroBackgroundImageUrl: settings.heroBackgroundImageUrl || '',
      defaultPostCoverUrl: settings.defaultPostCoverUrl || '',
      githubUrl: settings.githubUrl || '',
      email: settings.email || '',
      footerText: settings.footerText || '',
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '站点配置加载失败，请刷新页面重试'
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  error.value = ''
  success.value = ''
  if (!form.value.siteName.trim()) {
    error.value = '站点名称不能为空，将显示在浏览器标签页和页脚'
    return
  }
  if (!form.value.heroTitle.trim()) {
    error.value = '首页主标题不能为空，将显示在首页 Hero 区域'
    return
  }
  if (!form.value.heroDescription?.trim()) {
    error.value = '首页描述不能为空，将显示在主标题下方'
    return
  }

  saving.value = true
  try {
    await blogApi.saveSiteSettings(form.value)
    success.value = '站点配置已保存'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '站点配置保存失败，请检查网络后重试'
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>

<template>
  <AdminLayout>
  <section class="admin-site-settings animate-fade-in">
    <div v-if="loading" class="loading">
      <span>加载中...</span>
    </div>

    <div v-else class="settings-card">
      <div v-if="error" class="alert alert-error mb-lg">{{ error }}</div>
      <div v-if="success" class="alert alert-success mb-lg">{{ success }}</div>

      <form class="settings-form" @submit.prevent="saveSettings">
        <div class="form-grid">
          <div class="form-group">
            <label class="form-label" for="siteName">站点名称</label>
            <input id="siteName" v-model="form.siteName" type="text" class="form-input" />
          </div>

          <div class="form-group">
            <label class="form-label" for="siteSubtitle">副标题</label>
            <input id="siteSubtitle" v-model="form.siteSubtitle" type="text" class="form-input" />
          </div>

          <div class="form-group">
            <label class="form-label" for="heroBadge">Hero 徽标</label>
            <input id="heroBadge" v-model="form.heroBadge" type="text" class="form-input" placeholder="技术 · 生活 · 思考" />
          </div>

          <div class="form-group">
            <label class="form-label" for="heroTitle">首页主标题</label>
            <input id="heroTitle" v-model="form.heroTitle" type="text" class="form-input" />
          </div>

          <div class="form-group form-group-full">
            <label class="form-label" for="heroDescription">首页描述</label>
            <textarea id="heroDescription" v-model="form.heroDescription" class="form-textarea" rows="3" />
          </div>

          <div class="form-group">
            <label class="form-label" for="avatarImageUrl">头像 URL</label>
            <input id="avatarImageUrl" v-model="form.avatarImageUrl" type="text" class="form-input" placeholder="/api/public/media/{id}/file" />
            <img v-if="form.avatarImageUrl" :src="resolveAssetUrl(form.avatarImageUrl)" alt="头像预览" class="image-preview avatar-preview" />
          </div>

          <div class="form-group">
            <label class="form-label" for="heroBackgroundImageUrl">首页背景图 URL</label>
            <input id="heroBackgroundImageUrl" v-model="form.heroBackgroundImageUrl" type="text" class="form-input" placeholder="/api/public/media/{id}/file" />
            <img v-if="form.heroBackgroundImageUrl" :src="resolveAssetUrl(form.heroBackgroundImageUrl)" alt="背景图预览" class="image-preview bg-preview" />
          </div>

          <div class="form-group">
            <label class="form-label" for="defaultPostCoverUrl">默认文章封面 URL</label>
            <input id="defaultPostCoverUrl" v-model="form.defaultPostCoverUrl" type="text" class="form-input" placeholder="/api/public/media/{id}/file" />
            <img v-if="form.defaultPostCoverUrl" :src="resolveAssetUrl(form.defaultPostCoverUrl)" alt="封面预览" class="image-preview cover-preview" />
          </div>

          <div class="form-group">
            <label class="form-label" for="githubUrl">GitHub 链接</label>
            <input id="githubUrl" v-model="form.githubUrl" type="text" class="form-input" placeholder="https://github.com/your-name" />
          </div>

          <div class="form-group">
            <label class="form-label" for="email">联系邮箱</label>
            <input id="email" v-model="form.email" type="email" class="form-input" />
          </div>

          <div class="form-group form-group-full">
            <label class="form-label" for="footerText">页脚文案</label>
            <input id="footerText" v-model="form.footerText" type="text" class="form-input" />
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="saving">
            {{ saving ? '保存中...' : '保存配置' }}
          </button>
          <button type="button" class="btn btn-secondary" @click="loadSettings">重新加载</button>
          <RouterLink class="btn btn-secondary" to="/admin/media">去媒体中心复制图片 URL</RouterLink>
        </div>
      </form>
    </div>
  </section>
  </AdminLayout>
</template>

<style scoped>
.admin-site-settings {
  max-width: 1000px;
  margin: 0 auto;
}

.settings-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.settings-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.form-group-full {
  grid-column: 1 / -1;
}

.form-actions {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.mb-lg {
  margin-bottom: var(--space-6);
}

.image-preview {
  margin-top: var(--space-2);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  object-fit: cover;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
}

.bg-preview {
  width: 100%;
  max-width: 320px;
  height: 120px;
}

.cover-preview {
  width: 200px;
  height: 120px;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
