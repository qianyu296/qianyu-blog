<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { blogApi } from '@/api/blog'
import type { AiSettingsPayload } from '@/types/blog'
import AdminLayout from '@/components/AdminLayout.vue'

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

const form = ref<AiSettingsPayload>({
  chatApiKey: '',
  imageApiKey: '',
  apiEndpoint: '',
  imageApiEndpoint: '',
  modelName: '',
  imageModelName: '',
  enabled: true,
  maxTokens: 4096,
})

async function loadSettings() {
  loading.value = true
  error.value = ''
  success.value = ''

  try {
    const settings = await blogApi.getAiSettings()
    if (settings) {
      form.value = {
        chatApiKey: '',
        imageApiKey: '',
        apiEndpoint: settings.apiEndpoint || '',
        imageApiEndpoint: settings.imageApiEndpoint || '',
        modelName: settings.modelName || '',
        imageModelName: settings.imageModelName || '',
        enabled: settings.enabled ?? true,
        maxTokens: settings.maxTokens ?? 4096,
      }
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  error.value = ''
  success.value = ''

  if (!form.value.apiEndpoint.trim()) {
    error.value = '聊天 API 端点不能为空，格式如：https://api.openai.com/v1/chat/completions'
    return
  }

  if (!form.value.modelName.trim()) {
    error.value = '聊天模型名称不能为空，如：gpt-4o、gpt-3.5-turbo'
    return
  }

  saving.value = true
  try {
    await blogApi.saveAiSettings(form.value)
    success.value = '保存成功'
    form.value.chatApiKey = ''
    form.value.imageApiKey = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'AI 配置保存失败，请检查网络后重试'
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>

<template>
  <AdminLayout>
  <section class="admin-ai-settings animate-fade-in">
    <div v-if="loading" class="loading">
      <span>加载中...</span>
    </div>

    <div v-else class="settings-card">
      <div v-if="error" class="alert alert-error mb-lg">{{ error }}</div>
      <div v-if="success" class="alert alert-success mb-lg">{{ success }}</div>

      <form class="settings-form" @submit.prevent="saveSettings">
        <div class="form-group">
          <label class="form-label" for="chatApiKey">聊天 API Key</label>
          <input
            id="chatApiKey"
            v-model="form.chatApiKey"
            type="password"
            class="form-input"
            placeholder="留空则保持当前聊天 key 不变"
          />
          <span class="form-hint">只有输入新 key 时才会覆盖服务端配置。</span>
        </div>

        <div class="form-group">
          <label class="form-label" for="imageApiKey">绘图 API Key</label>
          <input
            id="imageApiKey"
            v-model="form.imageApiKey"
            type="password"
            class="form-input"
            placeholder="留空则保持当前绘图 key 不变"
          />
          <span class="form-hint">如果此前没有单独配置过绘图 key，首次保存时会默认继承聊天 key。</span>
        </div>

        <div class="form-group">
          <label class="form-label" for="apiEndpoint">聊天 API 端点</label>
          <input
            id="apiEndpoint"
            v-model="form.apiEndpoint"
            type="text"
            class="form-input"
            placeholder="https://api.openai.com/v1/chat/completions"
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="imageApiEndpoint">绘图 API 端点</label>
          <input
            id="imageApiEndpoint"
            v-model="form.imageApiEndpoint"
            type="text"
            class="form-input"
            placeholder="留空时沿用聊天 API 端点"
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="modelName">聊天模型名称</label>
          <input
            id="modelName"
            v-model="form.modelName"
            type="text"
            class="form-input"
            placeholder="gpt-4o-mini"
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="imageModelName">绘图模型名称</label>
          <input
            id="imageModelName"
            v-model="form.imageModelName"
            type="text"
            class="form-input"
            placeholder="gpt-image-1 或 dall-e-3"
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="maxTokens">最大回复 Token 数</label>
          <input
            id="maxTokens"
            v-model.number="form.maxTokens"
            type="number"
            class="form-input"
            min="256"
            max="128000"
            placeholder="4096"
          />
          <span class="form-hint">控制聊天接口单次回复的最大长度。</span>
        </div>

        <label class="checkbox-label">
          <input v-model="form.enabled" type="checkbox" />
          <span>启用 AI 功能</span>
        </label>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="saving">
            {{ saving ? '保存中...' : '保存设置' }}
          </button>
          <button type="button" class="btn btn-secondary" @click="loadSettings">重新加载</button>
        </div>
      </form>
    </div>
  </section>
  </AdminLayout>
</template>

<style scoped>
.admin-ai-settings {
  max-width: 900px;
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

.mb-lg {
  margin-bottom: var(--space-6);
}
</style>
