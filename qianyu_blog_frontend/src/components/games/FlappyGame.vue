<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'

const props = defineProps<{
  onBack: () => void
}>()

type Pipe = {
  x: number
  gapY: number
  passed: boolean
}

const rootRef = ref<HTMLDivElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const score = ref(0)
const gameOver = ref(false)
const started = ref(false)

const width = 420
const height = 520
const pipeWidth = 64
const pipeGap = 140

let ctx: CanvasRenderingContext2D | null = null
let animationId: number | null = null
let lastFrame = 0
let bird = { x: 96, y: 220, velocity: 0 }
let pipes: Pipe[] = []
let spawnCooldown = 0

function focusHost() {
  rootRef.value?.focus()
}

function resetState() {
  score.value = 0
  gameOver.value = false
  started.value = false
  bird = { x: 96, y: 220, velocity: 0 }
  pipes = []
  spawnCooldown = 0
}

function flap() {
  if (gameOver.value) return
  started.value = true
  bird.velocity = -8
}

function addPipe() {
  const minY = 120
  const maxY = height - 180
  pipes.push({
    x: width + pipeWidth,
    gapY: minY + Math.random() * (maxY - minY),
    passed: false,
  })
}

function update(delta: number) {
  if (!started.value || gameOver.value) return

  spawnCooldown -= delta
  if (spawnCooldown <= 0) {
    addPipe()
    spawnCooldown = 1500
  }

  bird.velocity += 0.45
  bird.y += bird.velocity

  pipes.forEach((pipe) => {
    pipe.x -= 2.6
    if (!pipe.passed && pipe.x + pipeWidth < bird.x) {
      pipe.passed = true
      score.value += 1
    }
  })

  pipes = pipes.filter((pipe) => pipe.x + pipeWidth > -20)

  if (bird.y < 16 || bird.y > height - 36) {
    gameOver.value = true
  }

  pipes.forEach((pipe) => {
    const withinX = bird.x + 14 > pipe.x && bird.x - 14 < pipe.x + pipeWidth
    const hitsTop = bird.y - 14 < pipe.gapY - pipeGap / 2
    const hitsBottom = bird.y + 14 > pipe.gapY + pipeGap / 2
    if (withinX && (hitsTop || hitsBottom)) {
      gameOver.value = true
    }
  })
}

function draw() {
  if (!ctx) return

  ctx.fillStyle = '#0ea5e9'
  ctx.fillRect(0, 0, width, height)

  ctx.fillStyle = 'rgba(255,255,255,0.18)'
  ctx.fillRect(0, height - 70, width, 70)

  ctx.fillStyle = '#22c55e'
  pipes.forEach((pipe) => {
    const topHeight = pipe.gapY - pipeGap / 2
    const bottomY = pipe.gapY + pipeGap / 2
    ctx!.fillRect(pipe.x, 0, pipeWidth, topHeight)
    ctx!.fillRect(pipe.x, bottomY, pipeWidth, height - bottomY)
  })

  ctx.save()
  ctx.translate(bird.x, bird.y)
  ctx.rotate(Math.max(-0.5, Math.min(1, bird.velocity / 10)))
  ctx.fillStyle = '#fbbf24'
  ctx.beginPath()
  ctx.ellipse(0, 0, 16, 13, 0, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = '#111827'
  ctx.beginPath()
  ctx.arc(5, -4, 2, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = '#f97316'
  ctx.beginPath()
  ctx.moveTo(12, 0)
  ctx.lineTo(22, -4)
  ctx.lineTo(22, 4)
  ctx.closePath()
  ctx.fill()
  ctx.restore()

  ctx.fillStyle = '#ffffff'
  ctx.font = 'bold 32px sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText(String(score.value), width / 2, 52)

  if (!started.value && !gameOver.value) {
    ctx.fillStyle = 'rgba(0,0,0,0.35)'
    ctx.fillRect(0, 0, width, height)
    ctx.fillStyle = '#ffffff'
    ctx.font = 'bold 26px sans-serif'
    ctx.fillText('点击或空格开始', width / 2, height / 2)
  }
}

function loop(timestamp: number) {
  const delta = lastFrame ? timestamp - lastFrame : 16
  lastFrame = timestamp
  update(delta)
  draw()
  animationId = requestAnimationFrame(loop)
}

function resetGame() {
  resetState()
  focusHost()
}

function handleKeydown(event: KeyboardEvent) {
  if (event.code === 'Space' || event.code === 'ArrowUp') {
    event.preventDefault()
    flap()
  }
  if (gameOver.value && event.code === 'Enter') {
    event.preventDefault()
    resetGame()
  }
}

function handlePointer() {
  focusHost()
  if (gameOver.value) {
    resetGame()
    return
  }
  flap()
}

onMounted(async () => {
  ctx = canvasRef.value?.getContext('2d') ?? null
  resetState()
  animationId = requestAnimationFrame(loop)
  await nextTick()
  focusHost()
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
})
</script>

<template>
  <div ref="rootRef" class="game-container" tabindex="0" @keydown="handleKeydown" @pointerdown.capture="focusHost">
    <div class="game-header">
      <button class="btn btn-secondary" @click="props.onBack()">返回</button>
      <h2>像素飞鸟</h2>
      <span class="score">得分：{{ score }}</span>
    </div>

    <div class="canvas-wrapper">
      <canvas
        ref="canvasRef"
        :width="width"
        :height="height"
        @click="handlePointer"
        @touchstart.prevent="handlePointer"
      ></canvas>

      <div v-if="gameOver" class="game-over">
        <h3>撞上障碍了</h3>
        <p>得分：{{ score }}</p>
        <button class="btn btn-primary" @click="resetGame">重新开始</button>
      </div>
    </div>

    <p class="game-hint">仅在当前游戏区域聚焦时，空格和上方向键才会接管。</p>
  </div>
</template>
