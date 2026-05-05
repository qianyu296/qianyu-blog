<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'

const props = defineProps<{
  onBack: () => void
}>()

type Board = number[][]
type MoveDirection = 'up' | 'down' | 'left' | 'right'

const rootRef = ref<HTMLDivElement | null>(null)
const board = ref<Board>(createEmptyBoard())
const score = ref(0)
const gameOver = ref(false)
const won = ref(false)

let touchStartX = 0
let touchStartY = 0

const directions = computed(() => [
  { key: 'up' as const, label: '↑' },
  { key: 'left' as const, label: '←' },
  { key: 'down' as const, label: '↓' },
  { key: 'right' as const, label: '→' },
])

function focusHost() {
  rootRef.value?.focus()
}

function createEmptyBoard(): Board {
  return Array.from({ length: 4 }, () => Array.from({ length: 4 }, () => 0))
}

function cloneBoard(source: Board) {
  return source.map((row) => [...row])
}

function addRandomTile(target: Board) {
  const empties: Array<[number, number]> = []
  for (let row = 0; row < 4; row += 1) {
    for (let col = 0; col < 4; col += 1) {
      if (target[row]?.[col] === 0) {
        empties.push([row, col])
      }
    }
  }
  const picked = empties[Math.floor(Math.random() * empties.length)]
  if (picked) {
    const [row, col] = picked
    target[row]![col] = Math.random() < 0.9 ? 2 : 4
  }
}

function compress(line: number[]) {
  const values = line.filter(Boolean)
  const next: number[] = []
  let gained = 0

  for (let index = 0; index < values.length; index += 1) {
    const current = values[index]
    const nextValue = values[index + 1]
    if (current !== undefined && current === nextValue) {
      const merged = current * 2
      next.push(merged)
      gained += merged
      index += 1
      if (merged === 2048) {
        won.value = true
      }
    } else if (current !== undefined) {
      next.push(current)
    }
  }

  while (next.length < 4) {
    next.push(0)
  }

  return { line: next, gained }
}

function getColumn(target: Board, col: number) {
  return target.map((row) => row[col] ?? 0)
}

function setColumn(target: Board, col: number, values: number[]) {
  for (let row = 0; row < 4; row += 1) {
    target[row]![col] = values[row] ?? 0
  }
}

function move(direction: MoveDirection) {
  if (gameOver.value) return

  const nextBoard = cloneBoard(board.value)
  const before = JSON.stringify(nextBoard)
  let gained = 0

  if (direction === 'left' || direction === 'right') {
    for (let row = 0; row < 4; row += 1) {
      const source = [...(nextBoard[row] ?? [])]
      const working = direction === 'right' ? source.reverse() : source
      const result = compress(working)
      const updated = direction === 'right' ? [...result.line].reverse() : result.line
      nextBoard[row] = updated
      gained += result.gained
    }
  } else {
    for (let col = 0; col < 4; col += 1) {
      const source = getColumn(nextBoard, col)
      const working = direction === 'down' ? source.reverse() : source
      const result = compress(working)
      const updated = direction === 'down' ? [...result.line].reverse() : result.line
      setColumn(nextBoard, col, updated)
      gained += result.gained
    }
  }

  if (JSON.stringify(nextBoard) === before) {
    return
  }

  score.value += gained
  addRandomTile(nextBoard)
  board.value = nextBoard
  gameOver.value = !hasMoves(nextBoard)
}

function hasMoves(target: Board) {
  for (let row = 0; row < 4; row += 1) {
    for (let col = 0; col < 4; col += 1) {
      const current = target[row]?.[col] ?? 0
      if (current === 0) return true
      if (target[row]?.[col + 1] === current) return true
      if (target[row + 1]?.[col] === current) return true
    }
  }
  return false
}

function resetGame() {
  won.value = false
  gameOver.value = false
  score.value = 0
  const next = createEmptyBoard()
  addRandomTile(next)
  addRandomTile(next)
  board.value = next
  focusHost()
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key.startsWith('Arrow')) {
    event.preventDefault()
  }
  if (event.key === 'ArrowUp') move('up')
  if (event.key === 'ArrowDown') move('down')
  if (event.key === 'ArrowLeft') move('left')
  if (event.key === 'ArrowRight') move('right')
}

function handleTouchStart(event: TouchEvent) {
  const touch = event.touches[0]
  if (!touch) return
  touchStartX = touch.clientX
  touchStartY = touch.clientY
}

function handleTouchEnd(event: TouchEvent) {
  const touch = event.changedTouches[0]
  if (!touch) return
  const deltaX = touch.clientX - touchStartX
  const deltaY = touch.clientY - touchStartY
  const threshold = 24

  if (Math.abs(deltaX) < threshold && Math.abs(deltaY) < threshold) return
  if (Math.abs(deltaX) > Math.abs(deltaY)) {
    move(deltaX > 0 ? 'right' : 'left')
  } else {
    move(deltaY > 0 ? 'down' : 'up')
  }
}

function tileColor(value: number) {
  const colors: Record<number, string> = {
    0: '#1f2937',
    2: '#e5e7eb',
    4: '#d1d5db',
    8: '#fbbf24',
    16: '#f59e0b',
    32: '#f97316',
    64: '#ef4444',
    128: '#60a5fa',
    256: '#3b82f6',
    512: '#8b5cf6',
    1024: '#7c3aed',
    2048: '#22c55e',
  }
  return colors[value] ?? '#111827'
}

function textColor(value: number) {
  return value <= 4 ? '#111827' : '#ffffff'
}

onMounted(async () => {
  resetGame()
  await nextTick()
  focusHost()
})
</script>

<template>
  <div ref="rootRef" class="game-container" tabindex="0" @keydown="handleKeydown" @pointerdown.capture="focusHost">
    <div class="game-header">
      <button class="btn btn-secondary" @click="props.onBack()">返回</button>
      <h2>2048</h2>
      <span class="score">得分：{{ score }}</span>
    </div>

    <div class="game-2048">
      <div class="grid-2048" @touchstart.passive="handleTouchStart" @touchend.passive="handleTouchEnd">
        <div v-for="(row, rowIndex) in board" :key="rowIndex" class="grid-row">
          <div
            v-for="(cell, colIndex) in row"
            :key="`${rowIndex}-${colIndex}`"
            class="tile"
            :style="{ backgroundColor: tileColor(cell), color: textColor(cell) }"
          >
            {{ cell || '' }}
          </div>
        </div>
      </div>

      <div v-if="won" class="win-overlay">
        <h3>达到 2048 了</h3>
        <p>当前得分：{{ score }}</p>
        <button class="btn btn-primary" @click="resetGame">再来一局</button>
      </div>

      <div v-else-if="gameOver" class="game-over">
        <h3>没有可移动位置</h3>
        <p>最终得分：{{ score }}</p>
        <button class="btn btn-primary" @click="resetGame">重新开始</button>
      </div>
    </div>

    <div class="game-control-pad">
      <button
        v-for="control in directions"
        :key="control.key"
        class="btn btn-secondary btn-sm"
        @click="move(control.key)"
      >
        {{ control.label }}
      </button>
    </div>

    <p class="game-hint">只有当前游戏区域聚焦时，方向键才会生效。</p>
  </div>
</template>
