<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { blogApi } from '@/api/blog'
import type { AiChatMessage } from '@/types/blog'

type ImagePreset = {
  id: string
  title: string
  ratio: string
  size?: string
  description: string
}

type ChatUiMessage = AiChatMessage & {
  id: number
  pendingImage?: boolean
  imagePrompt?: string
  startedAt?: number
  finishedAt?: number
  generationSeconds?: number
  presetId?: string
  presetTitle?: string
  presetRatio?: string
  requestedSize?: string
}

type ImageHistoryItem = {
  id: number
  prompt: string
  imageUrl: string
  finishedAt: number
  generationSeconds: number
  presetId: string
  presetTitle: string
  presetRatio: string
  requestedSize?: string
}

const IMAGE_TIMEOUT_MS = 3 * 60 * 1000
const HISTORY_STORAGE_KEY = 'qianyu_ai_image_history'
const HISTORY_LIMIT = 24

const imagePresets: ImagePreset[] = [
  { id: 'auto', title: '自动判断', ratio: 'Auto', description: '按提示词内容自动推断最合适的尺寸。' },
  { id: 'square-standard', title: '标准正方形', ratio: '1:1', size: '1024x1024', description: '头像、社交媒体配图、通用方图。' },
  { id: 'landscape-standard', title: '标准横版', ratio: '3:2', size: '1536x1024', description: '电脑壁纸、横版海报、PPT 配图。' },
  { id: 'portrait-standard', title: '标准竖版', ratio: '2:3', size: '1024x1536', description: '手机壁纸、小红书 / 抖音竖屏图。' },
  { id: 'square-2k', title: '2K 正方形', ratio: '1:1', size: '2048x2048', description: '高清头像、印刷、高质量素材。' },
  { id: 'landscape-2k', title: '2K 横版', ratio: '16:9', size: '2048x1152', description: '视频封面、Banner、横版大图。' },
  { id: 'portrait-2k', title: '2K 竖版', ratio: '9:16', size: '1152x2048', description: '短视频封面、抖音图、竖版海报。' },
]

const presetMap = new Map(imagePresets.map(preset => [preset.id, preset]))
const defaultPreset = imagePresets[0]!

const messages = ref<ChatUiMessage[]>([])
const imageHistory = ref<ImageHistoryItem[]>([])
const inputMessage = ref('')
const isLoading = ref(false)
const error = ref('')
const messagesContainer = ref<HTMLElement>()
const currentAssistantContent = ref('')
const isGeneratingImage = ref(false)
const imagePrompt = ref('')
const showImageDialog = ref(false)
const previewImageUrl = ref('')
const clockTick = ref(Date.now())
const selectedPresetId = ref<ImagePreset['id']>('auto')
const expandedHistoryId = ref<number | null>(null)

let nextMessageId = 1
let clockTimer: number | null = null

const selectedPreset = computed<ImagePreset>(() => presetMap.get(selectedPresetId.value) ?? defaultPreset)

function createMessage(message: Omit<ChatUiMessage, 'id'>): ChatUiMessage {
  return { ...message, id: nextMessageId++ }
}

function restoreHistory() {
  const raw = localStorage.getItem(HISTORY_STORAGE_KEY)
  if (!raw) return

  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return

    imageHistory.value = parsed
      .filter((item): item is ImageHistoryItem =>
        item
        && typeof item.id === 'number'
        && typeof item.prompt === 'string'
        && typeof item.imageUrl === 'string'
        && typeof item.finishedAt === 'number'
        && typeof item.generationSeconds === 'number'
        && typeof item.presetId === 'string'
        && typeof item.presetTitle === 'string'
        && typeof item.presetRatio === 'string',
      )
      .slice(0, HISTORY_LIMIT)

    // Sync nextMessageId with restored history to avoid id collisions
    const maxId = imageHistory.value.reduce((max, item) => Math.max(max, item.id), 0)
    if (maxId >= nextMessageId) {
      nextMessageId = maxId + 1
    }
  } catch {
    imageHistory.value = []
  }
}

function pushHistory(item: ImageHistoryItem) {
  imageHistory.value = [item, ...imageHistory.value.filter(history => history.id !== item.id)].slice(0, HISTORY_LIMIT)
  expandedHistoryId.value = item.id
}

function clearHistory() {
  imageHistory.value = []
  expandedHistoryId.value = null
}

function removeFromHistory(id: number) {
  imageHistory.value = imageHistory.value.filter(item => item.id !== id)
}

function reuseHistory(item: ImageHistoryItem) {
  inputMessage.value = item.prompt
  if (presetMap.has(item.presetId)) {
    selectedPresetId.value = item.presetId
  }
}

function toggleHistory(itemId: number) {
  expandedHistoryId.value = expandedHistoryId.value === itemId ? null : itemId
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

function openPreview(url: string) {
  previewImageUrl.value = url
}

function closePreview() {
  previewImageUrl.value = ''
}

function downloadImage(url: string, index: number) {
  const link = document.createElement('a')
  link.href = url
  link.download = `ai-image-${Date.now()}-${index + 1}.png`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function formatElapsed(seconds: number) {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return mins > 0 ? `${mins}分 ${String(secs).padStart(2, '0')}秒` : `${secs}秒`
}

function getElapsedLabel(message: ChatUiMessage) {
  if (!message.startedAt) return '0秒'
  const endAt = message.pendingImage ? clockTick.value : (message.finishedAt ?? clockTick.value)
  const seconds = Math.max(0, Math.floor((endAt - message.startedAt) / 1000))
  return formatElapsed(seconds)
}

function formatFinishedAt(timestamp?: number) {
  if (!timestamp) return ''
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(timestamp)
}

async function sendMessage() {
  const text = inputMessage.value.trim()
  if (!text || isLoading.value) return

  messages.value.push(createMessage({ role: 'user', content: text }))
  messages.value.push(createMessage({ role: 'assistant', content: '' }))
  const assistantIndex = messages.value.length - 1

  inputMessage.value = ''
  isLoading.value = true
  error.value = ''
  currentAssistantContent.value = ''
  scrollToBottom()

  try {
    const token = localStorage.getItem('qianyu_admin_token')
    const response = await fetch(`${blogApi.aiChatStreamUrl}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ message: text }),
    })

    if (!response.ok) {
      const errorData = await response.json()
      throw new Error(errorData.message || '请求失败')
    }

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()

    if (!reader) {
      throw new Error('无法读取响应流')
    }

    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (!line.startsWith('data: ')) continue

        const data = line.slice(6)
        if (data === '[DONE]') {
          break
        }

        try {
          const parsed = JSON.parse(data)
          if (parsed.content) {
            currentAssistantContent.value += parsed.content
            if (messages.value[assistantIndex]) {
              messages.value[assistantIndex].content = currentAssistantContent.value
            }
            scrollToBottom()
          }
          if (parsed.error) {
            throw new Error(parsed.error)
          }
        } catch (exception) {
          if (exception instanceof Error && exception.message !== '') {
            throw exception
          }
        }
      }
    }
  } catch (exception) {
    error.value = exception instanceof Error ? exception.message : '发送失败'
    messages.value.splice(assistantIndex, 1)
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

async function generateImage() {
  const prompt = imagePrompt.value.trim()
  if (!prompt) return

  const startedAt = Date.now()
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => controller.abort(), IMAGE_TIMEOUT_MS)
  const preset = selectedPreset.value ?? defaultPreset

  isGeneratingImage.value = true
  showImageDialog.value = false
  error.value = ''

  messages.value.push(createMessage({ role: 'user', content: `生成图片：${prompt}` }))
  messages.value.push(
    createMessage({
      role: 'assistant',
      content: '图像生成中',
      pendingImage: true,
      imagePrompt: prompt,
      startedAt,
      presetId: preset.id,
      presetTitle: preset.title,
      presetRatio: preset.ratio,
      requestedSize: preset.size,
    }),
  )
  const assistantIndex = messages.value.length - 1
  scrollToBottom()

  try {
    const result = await blogApi.generateImage({ prompt, size: preset.size }, controller.signal)
    const finishedAt = Date.now()
    const generationSeconds = Math.max(1, Math.round((finishedAt - startedAt) / 1000))

    if (messages.value[assistantIndex]) {
      const updatedMessage: ChatUiMessage = {
        ...messages.value[assistantIndex],
        content: '已生成 1 张图片',
        imageUrl: result.url,
        pendingImage: false,
        finishedAt,
        generationSeconds,
      }
      messages.value[assistantIndex] = updatedMessage

      pushHistory({
        id: updatedMessage.id,
        prompt,
        imageUrl: result.url,
        finishedAt,
        generationSeconds,
        presetId: preset.id,
        presetTitle: preset.title,
        presetRatio: preset.ratio,
        requestedSize: preset.size,
      })
    }
  } catch (exception) {
    const finishedAt = Date.now()
    const messageText = exception instanceof DOMException && exception.name === 'AbortError'
      ? '生成失败，请再生成一次吧'
      : (exception instanceof Error ? exception.message : '图片生成失败')

    if (messages.value[assistantIndex]) {
      messages.value[assistantIndex] = {
        ...messages.value[assistantIndex],
        content: messageText,
        pendingImage: false,
        finishedAt,
      }
    }
  } finally {
    window.clearTimeout(timeoutId)
    isGeneratingImage.value = false
    imagePrompt.value = ''
    scrollToBottom()
  }
}

function openImageDialog() {
  if (!inputMessage.value.trim()) {
    error.value = '请先输入图片描述'
    return
  }
  imagePrompt.value = inputMessage.value.trim()
  showImageDialog.value = true
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

watch(imageHistory, (value) => {
  localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(value))
  if (expandedHistoryId.value !== null && !value.some(item => item.id === expandedHistoryId.value)) {
    expandedHistoryId.value = null
  }
}, { deep: true })

onMounted(() => {
  restoreHistory()
  scrollToBottom()
  clockTimer = window.setInterval(() => {
    clockTick.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (clockTimer !== null) {
    window.clearInterval(clockTimer)
  }
})
</script>

<template>
  <div class="chat-layout animate-fade-in">
    <section class="chat-page">
      <header class="chat-header">
        <h1>AI构建未来</h1>
        <p class="subtitle">可使用模型为gpt-5.4以及gpt-image-2</p>
      </header>

      <div class="chat-container">
        <div ref="messagesContainer" class="messages">
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </svg>
            </div>
            <p>开始和 AI 对话</p>
            <span>输入问题，或者输入提示词后点击图片按钮直接生成图片。</span>
          </div>

          <div v-for="(msg, index) in messages" :key="msg.id" :class="['message', msg.role]">
            <div class="message-avatar">
              <svg v-if="msg.role === 'user'" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
              </svg>
              <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.8 7.2-6.4 6.4-3.2-3.2 1.4-1.4 1.8 1.8 5-5 1.4 1.4z" />
              </svg>
            </div>

            <div class="message-content">
              <div v-if="msg.pendingImage" class="image-loading-card">
                <div class="loading-card-head">
                  <div class="loading-badge">
                    <span class="loading-pulse-dot"></span>
                    <span>图像生成中</span>
                  </div>

                  <div class="loading-elapsed">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="9"></circle>
                      <path d="M12 7v5l3 3"></path>
                    </svg>
                    <span>{{ getElapsedLabel(msg) }}</span>
                  </div>
                </div>

                <div class="loading-meta">
                  <span class="meta-chip meta-chip-soft">{{ msg.presetTitle ?? '自动判断' }}</span>
                  <span class="meta-chip meta-chip-soft">{{ msg.presetRatio ?? 'Auto' }}</span>
                  <span v-if="msg.requestedSize" class="meta-chip meta-chip-soft">{{ msg.requestedSize }}</span>
                </div>

                <p class="loading-title">正在为你渲染这张图</p>
                <p v-if="msg.imagePrompt" class="loading-prompt">{{ msg.imagePrompt }}</p>

                <div class="loading-bar" aria-hidden="true">
                  <span class="loading-bar-fill"></span>
                </div>

                <div class="loading-sparkles" aria-hidden="true">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>

              <template v-else>
                <div v-if="msg.content" class="message-bubble">{{ msg.content }}</div>

                <div v-if="msg.imageUrl" class="image-result-card">
                  <button class="image-preview-surface" type="button" @click="openPreview(msg.imageUrl)">
                    <img :src="msg.imageUrl" class="message-image" alt="AI 生成图片" />
                    <span class="image-hover-overlay">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="11" cy="11" r="7"></circle>
                        <path d="M21 21l-4.35-4.35"></path>
                      </svg>
                      <span>点击预览</span>
                    </span>
                  </button>

                  <div class="image-toolbar">
                    <div class="image-meta">
                      <span class="image-meta-label">结果</span>
                      <strong>图片已生成</strong>
                      <div class="image-meta-chips">
                        <span v-if="msg.presetTitle" class="meta-chip">{{ msg.presetTitle }}</span>
                        <span v-if="msg.presetRatio" class="meta-chip">{{ msg.presetRatio }}</span>
                        <span v-if="msg.requestedSize" class="meta-chip">{{ msg.requestedSize }}</span>
                        <span v-if="msg.generationSeconds" class="meta-chip">耗时 {{ formatElapsed(msg.generationSeconds) }}</span>
                        <span v-if="msg.finishedAt" class="meta-chip">完成于 {{ formatFinishedAt(msg.finishedAt) }}</span>
                      </div>
                    </div>

                    <div class="image-actions">
                      <button class="toolbar-btn" type="button" @click="openPreview(msg.imageUrl)">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6z"></path>
                          <circle cx="12" cy="12" r="3"></circle>
                        </svg>
                        <span>预览</span>
                      </button>

                      <button class="toolbar-btn toolbar-btn-primary" type="button" @click="downloadImage(msg.imageUrl, index)">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M12 3v12"></path>
                          <path d="M7 10l5 5 5-5"></path>
                          <path d="M5 21h14"></path>
                        </svg>
                        <span>下载</span>
                      </button>
                    </div>
                  </div>

                  <div v-if="msg.imagePrompt" class="image-prompt-panel">
                    <span class="image-prompt-label">提示词</span>
                    <p>{{ msg.imagePrompt }}</p>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <div v-if="error" class="chat-error">{{ error }}</div>

        <div class="chat-input-area">
          <textarea
            v-model="inputMessage"
            class="chat-input"
            placeholder="输入消息或图片提示词..."
            rows="1"
            :disabled="isLoading || isGeneratingImage"
            @keydown="handleKeydown"
          />

          <button
            class="image-btn"
            :disabled="!inputMessage.trim() || isLoading || isGeneratingImage"
            title="生成图片"
            @click="openImageDialog"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
          </button>

          <button
            class="send-btn"
            :disabled="!inputMessage.trim() || isLoading || isGeneratingImage"
            @click="sendMessage"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 2L11 13" />
              <path d="M22 2l-7 20-4-9-9-4 20-7z" />
            </svg>
          </button>
        </div>
      </div>
    </section>

    <aside class="history-panel">
      <div class="history-header">
        <div>
          <p class="history-eyebrow">History</p>
          <h2>生图记录</h2>
        </div>
        <button class="history-clear-btn" type="button" :disabled="imageHistory.length === 0" @click="clearHistory">
          清空
        </button>
      </div>

      <div v-if="imageHistory.length === 0" class="history-empty">
        <div class="history-empty-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <rect x="3" y="3" width="18" height="18" rx="3"></rect>
            <path d="M8 13l2.5-2.5L14 14l2.5-2.5L20 15"></path>
            <circle cx="8.5" cy="8.5" r="1.25"></circle>
          </svg>
        </div>
        <p>还没有生图记录</p>
        <span>生成成功后，这里会保留最近的图片、比例和提示词。</span>
      </div>

      <div v-else class="history-list">
        <article
          v-for="(item, index) in imageHistory"
          :key="item.id"
          class="history-entry"
          :class="{ expanded: expandedHistoryId === item.id }"
        >
          <button
            class="history-row"
            type="button"
            :aria-expanded="expandedHistoryId === item.id"
            @click="toggleHistory(item.id)"
          >
            <div class="history-row-main">
              <p class="history-row-title" :title="item.prompt">{{ item.prompt }}</p>
              <div class="history-row-meta">
                <span>{{ item.presetRatio }}</span>
                <span v-if="item.requestedSize">{{ item.requestedSize }}</span>
                <span>{{ formatElapsed(item.generationSeconds) }}</span>
              </div>
            </div>

            <div class="history-row-side">
              <span class="history-row-time">{{ formatFinishedAt(item.finishedAt) }}</span>
              <span class="history-row-chevron" :class="{ expanded: expandedHistoryId === item.id }" aria-hidden="true">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M6 9l6 6 6-6"></path>
                </svg>
              </span>
            </div>
          </button>

          <div v-if="expandedHistoryId === item.id" class="history-details">
            <button class="history-preview image-preview-surface" type="button" @click="openPreview(item.imageUrl)">
              <img :src="item.imageUrl" :alt="item.prompt" class="history-details-image" />
              <span class="image-hover-overlay">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="7"></circle>
                  <path d="M21 21l-4.35-4.35"></path>
                </svg>
                <span>点击查看图片</span>
              </span>
            </button>

            <div class="history-details-meta">
              <span class="meta-chip">{{ item.presetTitle }}</span>
              <span class="meta-chip">{{ item.presetRatio }}</span>
              <span v-if="item.requestedSize" class="meta-chip">{{ item.requestedSize }}</span>
              <span class="meta-chip">{{ formatFinishedAt(item.finishedAt) }}</span>
            </div>

            <p class="history-details-prompt">{{ item.prompt }}</p>

            <div class="history-card-actions">
              <button class="toolbar-btn" type="button" @click="openPreview(item.imageUrl)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
                <span>预览</span>
              </button>
              <button class="toolbar-btn" type="button" @click="reuseHistory(item)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 12a9 9 0 1 1-3-6.7"></path>
                  <path d="M21 3v6h-6"></path>
                </svg>
                <span>复用预设</span>
              </button>
              <button class="toolbar-btn toolbar-btn-primary" type="button" @click="downloadImage(item.imageUrl, index)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 3v12"></path>
                  <path d="M7 10l5 5 5-5"></path>
                  <path d="M5 21h14"></path>
                </svg>
                <span>下载</span>
              </button>
              <button class="toolbar-btn toolbar-btn-danger" type="button" @click="removeFromHistory(item.id)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 6h18"></path>
                  <path d="M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2"></path>
                  <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"></path>
                </svg>
                <span>删除</span>
              </button>
            </div>
          </div>
        </article>
      </div>
    </aside>

    <div v-if="showImageDialog" class="dialog-overlay" @click.self="showImageDialog = false">
      <div class="dialog image-dialog">
        <h3>确认生成图片</h3>
        <p class="dialog-prompt">{{ imagePrompt }}</p>

        <div class="preset-section">
          <div class="preset-section-head">
            <span>尺寸与比例</span>
            <small>支持标准和 2K 预设，未选择时可用自动判断。</small>
          </div>

          <div class="preset-grid">
            <button
              v-for="presetItem in imagePresets"
              :key="presetItem.id"
              type="button"
              class="preset-card"
              :class="{ active: selectedPresetId === presetItem.id }"
              @click="selectedPresetId = presetItem.id"
            >
              <div class="preset-card-top">
                <strong>{{ presetItem.title }}</strong>
                <span>{{ presetItem.ratio }}</span>
              </div>
              <div class="preset-card-size">{{ presetItem.size ?? '由模型决定' }}</div>
              <p>{{ presetItem.description }}</p>
            </button>
          </div>
        </div>

        <div class="dialog-actions">
          <button class="btn btn-secondary" @click="showImageDialog = false">取消</button>
          <button class="btn btn-primary" :disabled="isGeneratingImage" @click="generateImage">
            {{ isGeneratingImage ? '生成中...' : '确认生成' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="previewImageUrl" class="preview-overlay" @click.self="closePreview">
      <div class="preview-dialog">
        <div class="preview-toolbar">
          <div class="preview-title">图片预览</div>
          <div class="preview-actions">
            <button class="toolbar-btn" type="button" @click="downloadImage(previewImageUrl, 0)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 3v12"></path>
                <path d="M7 10l5 5 5-5"></path>
                <path d="M5 21h14"></path>
              </svg>
              <span>下载</span>
            </button>
            <button class="toolbar-btn" type="button" @click="closePreview">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6L6 18"></path>
                <path d="M6 6l12 12"></path>
              </svg>
              <span>关闭</span>
            </button>
          </div>
        </div>
        <img :src="previewImageUrl" class="preview-image" alt="预览图片" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-layout {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: var(--space-6);
  align-items: start;
}

.chat-page {
  min-width: 0;
}

.chat-header {
  text-align: center;
  margin-bottom: var(--space-6);
}

.chat-header h1 {
  font-family: var(--font-heading);
  font-size: 32px;
  font-weight: 700;
  margin-bottom: var(--space-2);
}

.subtitle {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.chat-container,
.history-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
}

.chat-container {
  height: calc(100vh - 180px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  text-align: center;
}

.empty-icon,
.history-empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: var(--color-bg);
  border-radius: 50%;
  margin-bottom: var(--space-4);
}

.empty-state p,
.history-empty p {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: var(--space-1);
}

.empty-state span,
.history-empty span {
  font-size: 13px;
}

.message {
  display: flex;
  gap: var(--space-3);
  max-width: 85%;
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message.assistant {
  align-self: flex-start;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message.user .message-avatar {
  background: var(--color-accent);
  color: white;
}

.message.assistant .message-avatar {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}

.message-bubble {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message.user .message-bubble {
  background: var(--color-accent);
  color: white;
  border-bottom-right-radius: var(--radius-sm);
}

.message.assistant .message-bubble {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-bottom-left-radius: var(--radius-sm);
}

.image-loading-card,
.image-result-card {
  width: min(460px, 72vw);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.98));
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.image-loading-card {
  position: relative;
  overflow: hidden;
  padding: var(--space-4);
}

.image-loading-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent 0%, rgba(255, 255, 255, 0.65) 36%, transparent 72%);
  transform: translateX(-100%);
  animation: loading-sheen 2.2s linear infinite;
}

.loading-card-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-3);
}

.loading-badge,
.loading-elapsed {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  height: 32px;
  padding: 0 var(--space-3);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
}

.loading-badge {
  background: rgba(37, 99, 235, 0.1);
  color: var(--color-accent);
}

.loading-elapsed {
  background: rgba(15, 23, 42, 0.06);
  color: var(--color-text-secondary);
}

.loading-meta {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.loading-pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
  animation: pulse-dot 1.2s ease-in-out infinite;
}

.loading-title {
  position: relative;
  z-index: 1;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--space-2);
}

.loading-prompt {
  position: relative;
  z-index: 1;
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.7;
  margin-bottom: var(--space-4);
}

.loading-bar {
  position: relative;
  z-index: 1;
  height: 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  overflow: hidden;
}

.loading-bar-fill {
  display: block;
  width: 38%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2563eb, #8b5cf6, #06b6d4);
  animation: loading-run 1.8s cubic-bezier(0.16, 1, 0.3, 1) infinite;
}

.loading-sparkles {
  position: relative;
  z-index: 1;
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.loading-sparkles span {
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: radial-gradient(circle at 30% 30%, rgba(139, 92, 246, 0.4), rgba(37, 99, 235, 0.08));
  animation: float-spark 2.6s ease-in-out infinite;
}

.loading-sparkles span:nth-child(2) {
  animation-delay: 0.25s;
}

.loading-sparkles span:nth-child(3) {
  animation-delay: 0.5s;
}

.image-result-card {
  overflow: hidden;
}

.image-preview-surface {
  position: relative;
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: var(--color-bg);
  cursor: zoom-in;
}

.message-image {
  display: block;
  width: 100%;
  height: auto;
  max-height: 720px;
  background: var(--color-bg);
  object-fit: contain;
}

.image-hover-overlay {
  position: absolute;
  inset: auto var(--space-4) var(--space-4) var(--space-4);
  height: 40px;
  border-radius: var(--radius-md);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  background: rgba(17, 24, 39, 0.72);
  color: white;
  font-size: 13px;
  opacity: 0;
  transform: translateY(8px);
  transition: opacity var(--duration-normal) var(--ease-out), transform var(--duration-normal) var(--ease-out);
}

.image-preview-surface:hover .image-hover-overlay {
  opacity: 1;
  transform: translateY(0);
}

.image-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
}

.image-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.image-meta-label,
.image-prompt-label,
.history-eyebrow {
  font-size: 12px;
  color: var(--color-text-muted);
  text-transform: uppercase;
}

.image-meta strong {
  font-size: 14px;
  font-weight: 600;
}

.image-meta-chips,
.history-card-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 var(--space-2);
  border-radius: var(--radius-full);
  background: rgba(15, 23, 42, 0.06);
  color: var(--color-text-secondary);
  font-size: 12px;
}

.meta-chip-soft {
  background: var(--color-surface-elevated);
}

.image-actions,
.preview-actions,
.history-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.toolbar-btn,
.history-clear-btn {
  flex-shrink: 0;
  min-width: 88px;
  height: 36px;
  padding: 0 var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  white-space: nowrap;
  transition: all var(--duration-fast) var(--ease-out);
}

.toolbar-btn span,
.history-clear-btn span {
  white-space: nowrap;
}

.toolbar-btn:hover,
.history-clear-btn:hover:not(:disabled) {
  background: var(--color-accent-light);
  border-color: var(--color-accent);
  color: var(--color-accent);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.12);
}

.toolbar-btn-primary {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: white;
}

.toolbar-btn-primary:hover {
  color: white;
  background: var(--color-accent-hover);
  border-color: var(--color-accent-hover);
}

.toolbar-btn-danger {
  color: var(--color-danger);
  border-color: rgba(220, 38, 38, 0.15);
}

.toolbar-btn-danger:hover {
  background: var(--color-danger-light);
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.image-prompt-panel {
  padding: 0 var(--space-4) var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.image-prompt-panel p {
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: rgba(15, 23, 42, 0.04);
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}

.chat-error {
  padding: var(--space-3) var(--space-4);
  margin: 0 var(--space-4);
  background: var(--color-danger-light);
  color: var(--color-danger);
  border-radius: var(--radius-md);
  font-size: 14px;
}

.chat-input-area {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4);
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}

.chat-input {
  flex: 1;
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  background: var(--color-bg);
  color: var(--color-text);
}

.chat-input:focus {
  outline: none;
  border-color: var(--color-accent);
}

.chat-input:disabled {
  opacity: 0.6;
}

.send-btn,
.image-btn {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.send-btn {
  background: var(--color-accent);
  border: none;
  color: white;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
}

.image-btn {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
}

.image-btn:hover:not(:disabled) {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.send-btn:disabled,
.image-btn:disabled,
.history-clear-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.history-panel {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  max-height: calc(100vh - 140px);
  padding: var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.history-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
}

.history-header h2 {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
}

.history-empty {
  flex: 1;
  min-height: 280px;
  padding: var(--space-6);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  text-align: center;
}

.history-list {
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-right: var(--space-1);
}

.history-entry {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.history-entry.expanded {
  border-color: rgba(37, 99, 235, 0.28);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.history-row {
  width: 100%;
  padding: 12px 14px;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  text-align: left;
  cursor: pointer;
}

.history-row:hover {
  background: var(--color-accent-subtle);
}

.history-row-main {
  min-width: 0;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
}

.history-row-title {
  margin: 0;
  color: var(--color-text);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-row-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  color: var(--color-text-muted);
  font-size: 11px;
}

.history-row-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.history-row-time {
  color: var(--color-text-muted);
  font-size: 11px;
}

.history-row-chevron {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.06);
  color: var(--color-text-secondary);
  transition: transform var(--duration-fast) var(--ease-out), background var(--duration-fast) var(--ease-out);
}

.history-row-chevron.expanded {
  transform: rotate(180deg);
  background: rgba(37, 99, 235, 0.12);
  color: var(--color-accent);
}

.history-details {
  padding: 0 14px 14px;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.history-preview {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.history-details-image {
  display: block;
  width: 100%;
  height: auto;
  max-height: 260px;
  background: var(--color-bg);
  object-fit: contain;
}

.history-details-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.history-details-prompt {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.dialog-overlay,
.preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.56);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog,
.preview-dialog {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
}

.dialog {
  padding: var(--space-8);
  width: min(960px, 92vw);
}

.dialog h3 {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin-bottom: var(--space-4);
}

.dialog-prompt {
  font-size: 14px;
  color: var(--color-text-secondary);
  padding: var(--space-3);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-5);
  word-break: break-word;
}

.preset-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.preset-section-head {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  align-items: baseline;
}

.preset-section-head span {
  font-size: 14px;
  font-weight: 600;
}

.preset-section-head small {
  color: var(--color-text-muted);
  font-size: 12px;
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
}

.preset-card {
  text-align: left;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  background: var(--color-surface);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  transition: all var(--duration-fast) var(--ease-out);
}

.preset-card:hover {
  border-color: var(--color-accent);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.08);
}

.preset-card.active {
  border-color: var(--color-accent);
  background: var(--color-accent-light);
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.15);
}

.preset-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.preset-card-top strong {
  font-size: 14px;
  font-weight: 600;
}

.preset-card-top span {
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 700;
}

.preset-card-size {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.preset-card p {
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.dialog-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
  margin-top: var(--space-5);
}

.btn {
  padding: var(--space-2) var(--space-5);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all var(--duration-fast) var(--ease-out);
}

.btn-secondary {
  background: var(--color-bg);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover {
  background: var(--color-surface);
}

.btn-primary {
  background: var(--color-accent);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.preview-dialog {
  width: min(94vw, 1100px);
  max-height: 92vh;
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.preview-title {
  font-size: 15px;
  font-weight: 600;
}

.preview-image {
  display: block;
  max-width: 100%;
  max-height: calc(92vh - 84px);
  object-fit: contain;
  border-radius: var(--radius-lg);
  background: var(--color-bg);
}

@keyframes loading-sheen {
  from {
    transform: translateX(-100%);
  }

  to {
    transform: translateX(120%);
  }
}

@keyframes pulse-dot {
  0%,
  100% {
    transform: scale(0.85);
    opacity: 0.65;
  }

  50% {
    transform: scale(1.15);
    opacity: 1;
  }
}

@keyframes loading-run {
  0% {
    transform: translateX(-110%);
  }

  100% {
    transform: translateX(320%);
  }
}

@keyframes float-spark {
  0%,
  100% {
    transform: translateY(0) scale(0.96);
    opacity: 0.55;
  }

  50% {
    transform: translateY(-8px) scale(1.06);
    opacity: 1;
  }
}

@media (max-width: 1080px) {
  .chat-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .history-panel {
    position: static;
    max-height: none;
  }
}

@media (max-width: 760px) {
  .preset-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .chat-container {
    height: calc(100vh - 120px);
  }

  .chat-header h1 {
    font-size: 24px;
  }

  .message {
    max-width: 95%;
  }

  .image-loading-card,
  .image-result-card {
    width: min(100%, 100vw - 96px);
  }

  .loading-card-head,
  .image-toolbar,
  .preview-toolbar,
  .history-header,
  .preset-section-head {
    flex-direction: column;
    align-items: stretch;
  }

  .image-actions,
  .preview-actions,
  .history-card-actions {
    width: 100%;
  }

  .toolbar-btn,
  .history-clear-btn {
    justify-content: center;
  }

  .preset-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .preview-dialog,
  .dialog {
    width: 96vw;
    padding: var(--space-3);
  }
}
</style>
