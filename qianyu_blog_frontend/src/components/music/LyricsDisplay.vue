<template>
  <div class="lyrics-display">
    <div v-if="lyrics.length === 0" class="no-lyrics">
      <p>暂无歌词</p>
    </div>
    <div v-else class="lyrics-container" ref="containerRef">
      <div
        v-for="(line, index) in lyrics"
        :key="index"
        :ref="index === currentIndex ? 'currentLineRef' : undefined"
        class="lyric-line"
        :class="{ active: index === currentIndex, passed: index < currentIndex }"
      >
        {{ line.text }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import type { LyricLine } from '@/types/music'

const props = defineProps<{
  lyrics: LyricLine[]
  currentIndex: number
}>()

const containerRef = ref<HTMLElement>()
const currentLineRef = ref<HTMLElement>()

watch(() => props.currentIndex, async () => {
  await nextTick()
  if (currentLineRef.value && containerRef.value) {
    const container = containerRef.value
    const line = currentLineRef.value
    const containerHeight = container.clientHeight
    const lineTop = line.offsetTop
    const lineHeight = line.clientHeight
    const scrollTo = lineTop - containerHeight / 2 + lineHeight / 2
    container.scrollTo({ top: scrollTo, behavior: 'smooth' })
  }
})
</script>

<style scoped>
.lyrics-display {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 400px;
}

.no-lyrics {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(30, 27, 75, 0.3);
  font-family: 'Poppins', sans-serif;
  font-size: 1rem;
  font-weight: 300;
  letter-spacing: 0.05em;
}

.lyrics-container {
  flex: 1;
  overflow-y: auto;
  padding: 3rem 2rem;
  mask-image: linear-gradient(
    to bottom,
    transparent 0%,
    black 15%,
    black 85%,
    transparent 100%
  );
  -webkit-mask-image: linear-gradient(
    to bottom,
    transparent 0%,
    black 15%,
    black 85%,
    transparent 100%
  );
  scrollbar-width: none;
}

.lyrics-container::-webkit-scrollbar {
  display: none;
}

.lyric-line {
  text-align: center;
  padding: 0.625rem 1rem;
  font-family: 'Poppins', sans-serif;
  font-size: 1.05rem;
  font-weight: 300;
  color: rgba(30, 27, 75, 0.2);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  line-height: 1.8;
  cursor: default;
  user-select: none;
  border-radius: 8px;
}

.lyric-line.active {
  color: #1E1B4B;
  font-size: 1.35rem;
  font-weight: 600;
  transform: scale(1.06);
  text-shadow: 0 1px 8px rgba(99, 102, 241, 0.15);
  background: rgba(99, 102, 241, 0.06);
}

.lyric-line.passed {
  color: rgba(30, 27, 75, 0.12);
}
</style>
