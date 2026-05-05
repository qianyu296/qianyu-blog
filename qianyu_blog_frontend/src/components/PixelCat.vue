<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)
const frame = ref(0)
const isSleeping = ref(false)
const tailFrame = ref(0)

let animTimer: ReturnType<typeof setInterval> | null = null
let idleTimer: ReturnType<typeof setTimeout> | null = null
let tailTimer: ReturnType<typeof setInterval> | null = null

const PX = 5
const W = 16
const H = 18

// 颜色
const C = '#F59E0B'   // 橙色主色
const CD = '#D97706'  // 深橙
const CL = '#FCD34D'  // 浅橙
const CE = '#1E1B4B'  // 深色(眼/嘴)
const CN = '#FB7185'  // 粉(鼻)
const CW = '#FFFFFF'  // 白(高光)
const CT = '#FBBF24'  // 爪子

type Sprite = (string | null)[][]

// 16x18 的精灵图，null = 透明
// 行0-5: 头 (12宽居中)
// 行6-9: 身体 (10宽居中)
// 行10-12: 腿
// 行13: 地面阴影

function getSprite(f: number, tf: number, sleeping: boolean): Sprite {
  const s: Sprite = Array.from({ length: H }, () => Array(W).fill(null) as (string | null)[])

  // === 耳朵 (行0) ===
  s[0]![1] = CL; s[0]![2] = C
  s[0]![13] = C; s[0]![14] = CL

  // === 头顶 (行1) ===
  for (let x = 0; x < 14; x++) s[1]![x + 1] = C

  // === 眼睛行 (行2) ===
  for (let x = 0; x < 14; x++) s[2]![x + 1] = C
  if (sleeping) {
    // 闭眼 - 横线
    s[2]![3] = CE; s[2]![4] = CE
    s[2]![11] = CE; s[2]![12] = CE
  } else {
    // 睁眼
    s[2]![3] = CE; s[2]![4] = CE; s[2]![3] = CW // 高光
    s[2]![3] = CW
    s[2]![4] = CE
    s[2]![11] = CE; s[2]![12] = CE; s[2]![11] = CW
    s[2]![11] = CW
    s[2]![12] = CE
  }

  // === 鼻子行 (行3) ===
  for (let x = 0; x < 14; x++) s[3]![x + 1] = C
  s[3]![7] = CN; s[3]![8] = CN

  // === 嘴巴行 (行4) ===
  for (let x = 0; x < 14; x++) s[4]![x + 1] = C
  if (!sleeping) {
    s[4]![7] = CE; s[4]![8] = CE
  }

  // === 脸颊 (行5) ===
  for (let x = 0; x < 14; x++) s[5]![x + 1] = C

  // === 身体 (行6-9) ===
  for (let y = 6; y <= 9; y++) {
    for (let x = 3; x <= 12; x++) {
      s[y]![x] = C
    }
    // 胸口花纹
    if (y >= 7 && y <= 8) {
      for (let x = 6; x <= 9; x++) s[y]![x] = CL
    }
  }

  // === 尾巴 (行7-8, 右侧延伸) ===
  // 通过 offset 实现尾巴摆动
  const tailOff = tf === 0 ? 0 : tf === 1 ? -1 : 1
  for (let i = 0; i < 4; i++) {
    const ty = 7 + Math.round((i * tailOff) / 3)
    if (ty >= 0 && ty < H) {
      s[ty]![13 + i] = CD
    }
  }

  // === 腿 (行10-12) ===
  const legPairs = [
    { x: 4, walkOff: f === 1 ? -1 : f === 3 ? 1 : 0 },
    { x: 6, walkOff: (f + 2) % 4 === 1 ? -1 : (f + 2) % 4 === 3 ? 1 : 0 },
    { x: 8, walkOff: (f + 2) % 4 === 1 ? -1 : (f + 2) % 4 === 3 ? 1 : 0 },
    { x: 10, walkOff: f === 1 ? -1 : f === 3 ? 1 : 0 },
  ]

  for (const leg of legPairs) {
    for (let ly = 0; ly < 3; ly++) {
      const py = 10 + ly + (ly < 2 ? leg.walkOff : 0)
      if (py >= 0 && py < H) {
        s[py]![leg.x] = ly === 2 ? CT : C
        s[py]![leg.x + 1] = ly === 2 ? CT : C
      }
    }
  }

  // === 地面阴影 (行13-14) ===
  for (let x = 3; x <= 12; x++) {
    s[13]![x] = 'rgba(0,0,0,0.06)'
  }

  return s
}

function draw() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, canvas.width, canvas.height)

  const sprite = getSprite(frame.value, tailFrame.value, isSleeping.value)

  for (let y = 0; y < H; y++) {
    for (let x = 0; x < W; x++) {
      const color = sprite[y]?.[x]
      if (color) {
        ctx.fillStyle = color
        ctx.fillRect(x * PX, y * PX, PX, PX)
      }
    }
  }
}

function startWalking() {
  isSleeping.value = false
  frame.value = 0
  if (animTimer) clearInterval(animTimer)
  animTimer = setInterval(() => {
    frame.value = (frame.value + 1) % 4
    nextTick(draw)
  }, 220)
  scheduleIdle()
}

function startSleeping() {
  isSleeping.value = true
  frame.value = 0
  if (animTimer) { clearInterval(animTimer); animTimer = null }
  draw()
  scheduleWake()
}

function scheduleIdle() {
  if (idleTimer) clearTimeout(idleTimer)
  idleTimer = setTimeout(() => startSleeping(), 8000 + Math.random() * 6000)
}

function scheduleWake() {
  if (idleTimer) clearTimeout(idleTimer)
  idleTimer = setTimeout(() => startWalking(), 5000 + Math.random() * 4000)
}

watch([frame, tailFrame, isSleeping], draw)

onMounted(() => {
  draw()
  startWalking()
  tailTimer = setInterval(() => {
    tailFrame.value = (tailFrame.value + 1) % 3
    nextTick(draw)
  }, 350)
})

onUnmounted(() => {
  if (animTimer) clearInterval(animTimer)
  if (idleTimer) clearTimeout(idleTimer)
  if (tailTimer) clearInterval(tailTimer)
})
</script>

<template>
  <div class="pixel-cat-wrapper" :class="{ sleeping: isSleeping }" @click="isSleeping ? startWalking() : startSleeping()">
    <canvas
      ref="canvasRef"
      class="cat-canvas"
      :width="W * PX"
      :height="H * PX"
    ></canvas>
    <!-- zzz -->
    <div v-if="isSleeping" class="zzz">
      <span class="z z1">z</span>
      <span class="z z2">z</span>
      <span class="z z3">z</span>
    </div>
    <!-- tooltip -->
    <div class="cat-tooltip">{{ isSleeping ? '点击叫醒' : '点击睡觉' }}</div>
  </div>
</template>

<style scoped>
.pixel-cat-wrapper {
  position: fixed;
  bottom: 20px;
  left: 20px;
  z-index: 999;
  cursor: pointer;
  transition: transform 0.2s;
}

.pixel-cat-wrapper:hover {
  transform: scale(1.08);
}

.pixel-cat-wrapper:hover .cat-tooltip {
  opacity: 1;
  transform: translateY(-4px);
}

.cat-tooltip {
  position: absolute;
  bottom: 100%;
  left: 0;
  padding: 4px 10px;
  background: rgba(30, 27, 75, 0.85);
  color: white;
  font-size: 12px;
  border-radius: 8px;
  white-space: nowrap;
  opacity: 0;
  transform: translateY(4px);
  transition: all 0.2s;
  pointer-events: none;
  margin-bottom: 6px;
}

.cat-canvas {
  image-rendering: pixelated;
  image-rendering: crisp-edges;
  display: block;
}

/* zzz */
.zzz {
  position: absolute;
  top: -8px;
  left: 60px;
}

.z {
  position: absolute;
  font-family: 'Courier New', monospace;
  font-weight: 900;
  color: #F59E0B;
  opacity: 0;
  animation: float-z 2s ease-in-out infinite;
}

.z1 { font-size: 14px; left: 0; top: 0; animation-delay: 0s; }
.z2 { font-size: 11px; left: 10px; top: -12px; animation-delay: 0.5s; }
.z3 { font-size: 16px; left: 20px; top: -26px; animation-delay: 1s; }

@keyframes float-z {
  0% { opacity: 0; transform: translateY(6px) scale(0.8); }
  30% { opacity: 1; }
  100% { opacity: 0; transform: translateY(-16px) scale(1.2); }
}

@media (max-width: 720px) {
  .pixel-cat-wrapper {
    bottom: 12px;
    left: 12px;
  }

  .cat-tooltip {
    display: none;
  }
}
</style>
