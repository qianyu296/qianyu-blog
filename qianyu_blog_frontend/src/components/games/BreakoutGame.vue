<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'

const props = defineProps<{
  onBack: () => void
}>()

type Brick = {
  x: number
  y: number
  width: number
  height: number
  visible: boolean
  color: string
}

const rootRef = ref<HTMLDivElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const score = ref(0)
const lives = ref(3)
const gameOver = ref(false)
const won = ref(false)

const width = 480
const height = 360

let ctx: CanvasRenderingContext2D | null = null
let frameId: number | null = null
let rightPressed = false
let leftPressed = false

let paddle = { x: 190, y: 330, width: 100, height: 12, speed: 6 }
let ball = { x: width / 2, y: height - 40, dx: 3.2, dy: -3.2, radius: 7 }
let bricks: Brick[] = []

function focusHost() {
  rootRef.value?.focus()
}

function createBricks() {
  const result: Brick[] = []
  const rows = 5
  const cols = 6
  const brickWidth = 64
  const brickHeight = 18
  const offsetX = 22
  const offsetY = 42
  const gap = 10
  const colors = ['#ef4444', '#f97316', '#fbbf24', '#22c55e', '#3b82f6']

  for (let row = 0; row < rows; row += 1) {
    for (let col = 0; col < cols; col += 1) {
      result.push({
        x: offsetX + col * (brickWidth + gap),
        y: offsetY + row * (brickHeight + gap),
        width: brickWidth,
        height: brickHeight,
        visible: true,
        color: colors[row] ?? '#3b82f6',
      })
    }
  }

  return result
}

function resetBall() {
  ball = { x: width / 2, y: height - 40, dx: 3.2, dy: -3.2, radius: 7 }
  paddle.x = 190
}

function resetGame() {
  score.value = 0
  lives.value = 3
  gameOver.value = false
  won.value = false
  bricks = createBricks()
  resetBall()
  if (frameId) cancelAnimationFrame(frameId)
  loop()
  focusHost()
}

function draw() {
  if (!ctx) return

  ctx.clearRect(0, 0, width, height)
  ctx.fillStyle = '#111827'
  ctx.fillRect(0, 0, width, height)

  bricks.forEach((brick) => {
    if (!brick.visible) return
    ctx!.fillStyle = brick.color
    ctx!.fillRect(brick.x, brick.y, brick.width, brick.height)
  })

  ctx.fillStyle = '#3b82f6'
  ctx.fillRect(paddle.x, paddle.y, paddle.width, paddle.height)

  ctx.fillStyle = '#ffffff'
  ctx.beginPath()
  ctx.arc(ball.x, ball.y, ball.radius, 0, Math.PI * 2)
  ctx.fill()
}

function stepPaddle() {
  if (leftPressed) {
    paddle.x = Math.max(0, paddle.x - paddle.speed)
  }
  if (rightPressed) {
    paddle.x = Math.min(width - paddle.width, paddle.x + paddle.speed)
  }
}

function stepBall() {
  ball.x += ball.dx
  ball.y += ball.dy

  if (ball.x <= ball.radius || ball.x >= width - ball.radius) {
    ball.dx *= -1
  }

  if (ball.y <= ball.radius) {
    ball.dy *= -1
  }

  const paddleHit =
    ball.y + ball.radius >= paddle.y &&
    ball.y + ball.radius <= paddle.y + paddle.height &&
    ball.x >= paddle.x &&
    ball.x <= paddle.x + paddle.width

  if (paddleHit && ball.dy > 0) {
    const hitPoint = (ball.x - (paddle.x + paddle.width / 2)) / (paddle.width / 2)
    ball.dx = hitPoint * 5
    ball.dy = -Math.abs(ball.dy)
  }

  if (ball.y - ball.radius > height) {
    lives.value -= 1
    if (lives.value <= 0) {
      gameOver.value = true
      return
    }
    resetBall()
  }
}

function hitBricks() {
  bricks.forEach((brick) => {
    if (!brick.visible) return
    const collided =
      ball.x + ball.radius > brick.x &&
      ball.x - ball.radius < brick.x + brick.width &&
      ball.y + ball.radius > brick.y &&
      ball.y - ball.radius < brick.y + brick.height

    if (collided) {
      brick.visible = false
      score.value += 10
      ball.dy *= -1
    }
  })

  if (bricks.every((brick) => !brick.visible)) {
    won.value = true
  }
}

function loop() {
  if (gameOver.value || won.value) {
    draw()
    return
  }

  stepPaddle()
  stepBall()
  hitBricks()
  draw()
  frameId = requestAnimationFrame(loop)
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
    event.preventDefault()
  }
  if (event.key === 'ArrowLeft') leftPressed = true
  if (event.key === 'ArrowRight') rightPressed = true
}

function handleKeyup(event: KeyboardEvent) {
  if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
    event.preventDefault()
  }
  if (event.key === 'ArrowLeft') leftPressed = false
  if (event.key === 'ArrowRight') rightPressed = false
}

function handlePointerMove(event: MouseEvent | TouchEvent) {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const clientX =
    event instanceof MouseEvent ? event.clientX : (event.touches[0]?.clientX ?? rect.left)
  const nextX = clientX - rect.left - paddle.width / 2
  paddle.x = Math.max(0, Math.min(width - paddle.width, nextX))
}

onMounted(async () => {
  ctx = canvasRef.value?.getContext('2d') ?? null
  resetGame()
  await nextTick()
  focusHost()
})

onUnmounted(() => {
  if (frameId) cancelAnimationFrame(frameId)
})
</script>

<template>
  <div
    ref="rootRef"
    class="game-container"
    tabindex="0"
    @keydown="handleKeydown"
    @keyup="handleKeyup"
    @pointerdown.capture="focusHost"
  >
    <div class="game-header">
      <button class="btn btn-secondary" @click="props.onBack()">返回</button>
      <h2>打砖块</h2>
      <span class="score">得分：{{ score }} / 生命：{{ lives }}</span>
    </div>

    <div class="canvas-wrapper">
      <canvas
        ref="canvasRef"
        :width="width"
        :height="height"
        @mousemove="handlePointerMove"
        @touchmove.prevent="handlePointerMove"
      ></canvas>

      <div v-if="won" class="win-overlay">
        <h3>通关了</h3>
        <p>得分：{{ score }}</p>
        <button class="btn btn-primary" @click="resetGame">再来一局</button>
      </div>

      <div v-else-if="gameOver" class="game-over">
        <h3>游戏结束</h3>
        <p>得分：{{ score }}</p>
        <button class="btn btn-primary" @click="resetGame">重新开始</button>
      </div>
    </div>

    <p class="game-hint">仅在当前游戏区域聚焦时，左右键才会接管控制。</p>
  </div>
</template>
