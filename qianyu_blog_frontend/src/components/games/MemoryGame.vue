<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

const props = defineProps<{
  onBack: () => void
}>()

type Card = {
  id: number
  emoji: string
  flipped: boolean
  matched: boolean
}

const icons = ['🎮', '🎯', '🚀', '🧩', '🎨', '🎲', '⚡', '🪐']
const cards = ref<Card[]>([])
const opened = ref<number[]>([])
const moves = ref(0)
const matchedPairs = ref(0)
const locked = ref(false)

const totalPairs = computed(() => icons.length)
const cleared = computed(() => matchedPairs.value === totalPairs.value)

function shuffle<T>(items: T[]) {
  const copied = [...items]
  for (let index = copied.length - 1; index > 0; index -= 1) {
    const target = Math.floor(Math.random() * (index + 1))
    ;[copied[index], copied[target]] = [copied[target]!, copied[index]!]
  }
  return copied
}

function resetGame() {
  cards.value = shuffle([...icons, ...icons]).map((emoji, index) => ({
    id: index,
    emoji,
    flipped: false,
    matched: false,
  }))
  opened.value = []
  moves.value = 0
  matchedPairs.value = 0
  locked.value = false
}

function flipCard(index: number) {
  const card = cards.value[index]
  if (!card || card.flipped || card.matched || locked.value || opened.value.length >= 2) {
    return
  }

  card.flipped = true
  opened.value.push(index)

  if (opened.value.length < 2) {
    return
  }

  moves.value += 1
  const [firstIndex, secondIndex] = opened.value
  const first = firstIndex !== undefined ? cards.value[firstIndex] : undefined
  const second = secondIndex !== undefined ? cards.value[secondIndex] : undefined

  if (first && second && first.emoji === second.emoji) {
    first.matched = true
    second.matched = true
    matchedPairs.value += 1
    opened.value = []
    return
  }

  locked.value = true
  window.setTimeout(() => {
    if (first) first.flipped = false
    if (second) second.flipped = false
    opened.value = []
    locked.value = false
  }, 700)
}

onMounted(resetGame)
</script>

<template>
  <div class="game-container">
    <div class="game-header">
      <button class="btn btn-secondary" @click="props.onBack()">返回</button>
      <h2>记忆翻牌</h2>
      <span class="score">步数：{{ moves }} / 配对：{{ matchedPairs }}</span>
    </div>

    <div class="memory-grid">
      <button
        v-for="(card, index) in cards"
        :key="card.id"
        class="memory-card"
        :class="{ flipped: card.flipped || card.matched, matched: card.matched }"
        @click="flipCard(index)"
      >
        <span>{{ card.flipped || card.matched ? card.emoji : '?' }}</span>
      </button>
    </div>

    <div v-if="cleared" class="game-result-card">
      <h3>全部配对完成</h3>
      <p>共用了 {{ moves }} 步</p>
      <button class="btn btn-primary" @click="resetGame">重新洗牌</button>
    </div>

    <p class="game-hint">翻出两张相同卡片即可配对，尽量减少步数。</p>
  </div>
</template>
