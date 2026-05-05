<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'

const props = defineProps<{
  onBack: () => void
}>()

type Point = { x: number; y: number }
type Direction = 'up' | 'down' | 'left' | 'right'

const rootRef = ref<HTMLDivElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const score = ref(0)
const gameOver = ref(false)

const gridSize = 20
const cellSize = 20
const canvasSize = gridSize * cellSize

let ctx: CanvasRenderingContext2D | null = null
let timer: number | null = null
let snake: Point[] = []
let food: Point = { x: 0, y: 0 }
let direction: Direction = 'right'
let nextDirection: Direction = 'right'

const controls = computed(() => [
  { key: 'up' as const, label: '↑' },
  { key: 'left' as const, label: '←' },
  { key: 'down' as const, label: '↓' },
  { key: 'right' as const, label: '→' },
])

function focusHost() {
  rootRef.value?.focus()
}

function isOpposite(a: Direction, b: Direction) {
  return (
    (a === 'up' && b === 'down') ||
    (a === 'down' && b === 'up') ||
    (a === 'left' && b === 'right') ||
    (a === 'right' && b === 'left')
  )
}

function setDirection(next: Direction) {
  if (!isOpposite(direction, next)) {
    nextDirection = next
  }
}

function randomFood() {
  const occupied = new Set(snake.map((segment) => `${segment.x},${segment.y}`))
  const emptyCells: Point[] = []

  for (let x = 0; x < gridSize; x += 1) {
    for (let y = 0; y < gridSize; y += 1) {
      if (!occupied.has(`${x},${y}`)) {
        emptyCells.push({ x, y })
      }
    }
  }

  return emptyCells[Math.floor(Math.random() * emptyCells.length)] ?? { x: 5, y: 5 }
}

function resetGame() {
  snake = [
    { x: 8, y: 10 },
    { x: 7, y: 10 },
    { x: 6, y: 10 },
  ]
  direction = 'right'
  nextDirection = 'right'
  food = randomFood()
  score.value = 0
  gameOver.value = false
  draw()

  if (timer) {
    window.clearInterval(timer)
  }
  timer = window.setInterval(tick, 120)
  focusHost()
}

function nextHead(head: Point, move: Direction): Point {
  if (move === 'up') return { x: head.x, y: head.y - 1 }
  if (move === 'down') return { x: head.x, y: head.y + 1 }
  if (move === 'left') return { x: head.x - 1, y: head.y }
  return { x: head.x + 1, y: head.y }
}

function tick() {
  direction = nextDirection
  const head = snake[0]
  if (!head) return

  const newHead = nextHead(head, direction)
  const hitsWall =
    newHead.x < 0 ||
    newHead.x >= gridSize ||
    newHead.y < 0 ||
    newHead.y >= gridSize
  const hitsSelf = snake.some((segment) => segment.x === newHead.x && segment.y === newHead.y)

  if (hitsWall || hitsSelf) {
    gameOver.value = true
    if (timer) {
      window.clearInterval(timer)
      timer = null
    }
    draw()
    return
  }

  snake = [newHead, ...snake]

  if (newHead.x === food.x && newHead.y === food.y) {
    score.value += 10
    food = randomFood()
  } else {
    snake.pop()
  }

  draw()
}

function drawGrid() {
  if (!ctx) return
  ctx.strokeStyle = 'rgba(255,255,255,0.05)'
  ctx.lineWidth = 1
  for (let index = 0; index <= gridSize; index += 1) {
    const pos = index * cellSize
    ctx.beginPath()
    ctx.moveTo(pos, 0)
    ctx.lineTo(pos, canvasSize)
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(0, pos)
    ctx.lineTo(canvasSize, pos)
    ctx.stroke()
  }
}

function draw() {
  if (!ctx) return

  ctx.fillStyle = '#111827'
  ctx.fillRect(0, 0, canvasSize, canvasSize)
  drawGrid()

  ctx.fillStyle = '#ef4444'
  ctx.beginPath()
  ctx.arc(
    food.x * cellSize + cellSize / 2,
    food.y * cellSize + cellSize / 2,
    cellSize / 2 - 3,
    0,
    Math.PI * 2,
  )
  ctx.fill()

  snake.forEach((segment, index) => {
    ctx!.fillStyle = index === 0 ? '#22c55e' : '#4ade80'
    ctx!.fillRect(
      segment.x * cellSize + 1,
      segment.y * cellSize + 1,
      cellSize - 2,
      cellSize - 2,
    )
  })
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key.startsWith('Arrow')) {
    event.preventDefault()
  }
  if (event.key === 'ArrowUp' || event.key.toLowerCase() === 'w') setDirection('up')
  if (event.key === 'ArrowDown' || event.key.toLowerCase() === 's') setDirection('down')
  if (event.key === 'ArrowLeft' || event.key.toLowerCase() === 'a') setDirection('left')
  if (event.key === 'ArrowRight' || event.key.toLowerCase() === 'd') setDirection('right')
}

onMounted(async () => {
  ctx = canvasRef.value?.getContext('2d') ?? null
  resetGame()
  await nextTick()
  focusHost()
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <div ref="rootRef" class="game-container" tabindex="0" @keydown="handleKeydown" @pointerdown.capture="focusHost">
    <div class="game-header">
      <button class="btn btn-secondary" @click="props.onBack()">返回</button>
      <h2>贪吃蛇</h2>
      <span class="score">得分：{{ score }}</span>
    </div>

    <div class="canvas-wrapper">
      <canvas ref="canvasRef" :width="canvasSize" :height="canvasSize"></canvas>
      <div v-if="gameOver" class="game-over">
        <h3>游戏结束</h3>
        <p>得分：{{ score }}</p>
        <button class="btn btn-primary" @click="resetGame">重新开始</button>
      </div>
    </div>

    <div class="game-control-pad">
      <button
        v-for="control in controls"
        :key="control.key"
        class="btn btn-secondary btn-sm"
        @click="setDirection(control.key)"
      >
        {{ control.label }}
      </button>
    </div>

    <p class="game-hint">仅在当前游戏区域聚焦时响应键盘。支持方向键和 WASD。</p>
  </div>
</template>
