<script setup lang="ts">
import { ref } from 'vue'
import { SnakeGame, Game2048, TypingGame, MemoryGame, BreakoutGame, FlappyGame } from '@/components/GameComponents'

type GameId = 'snake' | '2048' | 'typing' | 'memory' | 'breakout' | 'flappy'

const currentGame = ref<GameId | null>(null)

function startGame(game: GameId) {
  currentGame.value = game
}

function backToCenter() {
  currentGame.value = null
}

const games = [
  { id: 'snake' as const, name: '贪吃蛇', icon: '🐍', description: '经典贪吃蛇，越吃越长', bg: 'linear-gradient(135deg, #22C55E 0%, #16A34A 100%)' },
  { id: '2048' as const, name: '2048', icon: '🔢', description: '滑动方块，合成 2048', bg: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)' },
  { id: 'typing' as const, name: '打字挑战', icon: '⌨️', description: '测试你的输入速度', bg: 'linear-gradient(135deg, #3B82F6 0%, #2563EB 100%)' },
  { id: 'memory' as const, name: '记忆翻牌', icon: '🃏', description: '翻牌配对，考验记忆力', bg: 'linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%)' },
  { id: 'breakout' as const, name: '打砖块', icon: '🧱', description: '反弹小球，击碎砖块', bg: 'linear-gradient(135deg, #EC4899 0%, #DB2777 100%)' },
  { id: 'flappy' as const, name: '像素飞鸟', icon: '🐤', description: '控制小鸟穿过障碍', bg: 'linear-gradient(135deg, #14B8A6 0%, #0D9488 100%)' },
]
</script>

<template>
  <div class="game-center">
    <template v-if="!currentGame">
      <header class="center-header animate-slide-down">
        <h1>游戏中心</h1>
        <p class="subtitle">挑一个小游戏，顺手放松一下</p>
      </header>

      <div class="games-grid">
        <button
          v-for="(game, index) in games"
          :key="game.id"
          class="game-card stagger-item"
          :style="{ animationDelay: `${index * 80}ms` }"
          @click="startGame(game.id)"
        >
          <div class="game-icon" :style="{ background: game.bg }">{{ game.icon }}</div>
          <h3>{{ game.name }}</h3>
          <p>{{ game.description }}</p>
          <span class="play-btn">开始游戏</span>
        </button>
      </div>
    </template>

    <template v-else-if="currentGame === 'snake'"><SnakeGame :on-back="backToCenter" /></template>
    <template v-else-if="currentGame === '2048'"><Game2048 :on-back="backToCenter" /></template>
    <template v-else-if="currentGame === 'typing'"><TypingGame :on-back="backToCenter" /></template>
    <template v-else-if="currentGame === 'memory'"><MemoryGame :on-back="backToCenter" /></template>
    <template v-else-if="currentGame === 'breakout'"><BreakoutGame :on-back="backToCenter" /></template>
    <template v-else-if="currentGame === 'flappy'"><FlappyGame :on-back="backToCenter" /></template>
  </div>
</template>

<style scoped>
.game-center { max-width: 900px; margin: 0 auto; }
.center-header { text-align: center; margin-bottom: var(--space-10); }
.center-header h1 { font-family: var(--font-heading); font-size: 36px; font-weight: 700; letter-spacing: -0.03em; margin-bottom: var(--space-2); }
.subtitle { color: var(--color-text-secondary); font-size: 16px; }
.games-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: var(--space-5); }
.game-card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-xl); padding: var(--space-6); text-align: center; cursor: pointer; transition: all var(--duration-normal) var(--ease-out); position: relative; overflow: hidden; }
.game-card:hover { border-color: var(--color-accent); transform: translateY(-4px); box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1); }
.game-card:hover .play-btn { opacity: 1; transform: translateY(0); }
.game-icon { width: 64px; height: 64px; border-radius: var(--radius-lg); display: flex; align-items: center; justify-content: center; font-size: 28px; margin: 0 auto var(--space-4); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); }
.game-card h3 { font-family: var(--font-heading); font-size: 18px; font-weight: 600; margin-bottom: var(--space-2); }
.game-card p { font-size: 13px; color: var(--color-text-muted); margin-bottom: var(--space-4); }
.play-btn { display: inline-block; padding: var(--space-2) var(--space-4); background: var(--color-accent); color: white; font-size: 13px; font-weight: 500; border-radius: var(--radius-full); opacity: 0.8; transform: translateY(4px); transition: all var(--duration-fast) var(--ease-out); }

@media (max-width: 640px) {
  .games-grid { grid-template-columns: repeat(2, 1fr); }
  .game-icon { width: 48px; height: 48px; font-size: 22px; }
  .game-card h3 { font-size: 15px; }
  .game-card p { font-size: 12px; }
}
</style>
