<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { typingWords } from './TypingWords'

const props = defineProps<{
  onBack: () => void
}>()

const currentWord = ref('')
const input = ref('')
const secondsLeft = ref(45)
const score = ref(0)
const finished = ref(false)
const typedChars = ref(0)
const inputRef = ref<HTMLInputElement | null>(null)

let timer: number | null = null

const wordsPerMinute = computed(() => Math.round((typedChars.value / 5 / 45) * 60))

function pickWord() {
  currentWord.value =
    typingWords[Math.floor(Math.random() * typingWords.length)] ?? 'typescript'
}

function resetGame() {
  score.value = 0
  typedChars.value = 0
  secondsLeft.value = 45
  finished.value = false
  input.value = ''
  pickWord()

  if (timer) {
    window.clearInterval(timer)
  }
  timer = window.setInterval(() => {
    secondsLeft.value -= 1
    if (secondsLeft.value <= 0) {
      finishGame()
    }
  }, 1000)

  requestAnimationFrame(() => inputRef.value?.focus())
}

function finishGame() {
  finished.value = true
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
}

function handleInput() {
  if (finished.value) return
  if (input.value === currentWord.value) {
    score.value += 1
    typedChars.value += currentWord.value.length
    input.value = ''
    pickWord()
  }
}

onMounted(resetGame)

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <div class="game-container">
    <div class="game-header">
      <button class="btn btn-secondary" @click="props.onBack()">返回</button>
      <h2>打字挑战</h2>
      <span class="score">命中：{{ score }}</span>
    </div>

    <div class="typing-game">
      <div class="stats-row">
        <div class="stat-card">
          <strong>{{ secondsLeft }}</strong>
          <span>秒</span>
        </div>
        <div class="stat-card">
          <strong>{{ score }}</strong>
          <span>单词</span>
        </div>
        <div class="stat-card">
          <strong>{{ wordsPerMinute }}</strong>
          <span>WPM</span>
        </div>
      </div>

      <div class="word-display">{{ currentWord }}</div>

      <input
        ref="inputRef"
        v-model="input"
        class="typing-input"
        :disabled="finished"
        placeholder="输入上面的单词"
        @input="handleInput"
      />

      <div v-if="finished" class="game-result-card">
        <h3>时间到</h3>
        <p>完成单词：{{ score }}</p>
        <p>估算速度：{{ wordsPerMinute }} WPM</p>
        <button class="btn btn-primary" @click="resetGame">再来一次</button>
      </div>
    </div>

    <p class="game-hint">尽量快地准确输入单词，45 秒内完成越多越好。</p>
  </div>
</template>
