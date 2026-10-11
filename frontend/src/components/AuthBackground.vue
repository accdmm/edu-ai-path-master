<template>
  <div
    ref="rootEl"
    class="auth-bg"
    :class="[`auth-bg--${variant}`, { 'is-static': isStatic, 'is-paused': isPaused }]"
    aria-hidden="true"
  >
    <!-- A. 流动光晕（Aurora）：纯 CSS，3 个大尺寸径向渐变缓慢漂移 -->
    <template v-if="variant === 'aurora'">
      <span class="blob blob-1"></span>
      <span class="blob blob-2"></span>
      <span class="blob blob-3"></span>
    </template>

    <!-- B. 漂浮几何（考卷元素 ＋ ○ □）：小而慢，只分布在四周 -->
    <template v-else-if="variant === 'shapes'">
      <span
        v-for="(s, i) in shapes"
        :key="i"
        class="shape"
        :class="`shape--${s.kind}`"
        :style="{
          left: s.left,
          top: s.top,
          width: s.size + 'px',
          height: s.size + 'px',
          animationDuration: s.duration + 's',
          animationDelay: s.delay + 's'
        }"
      ></span>
    </template>

    <!-- C. 点阵呼吸 + 两层视差：JS 仅节流改 CSS 变量 -->
    <template v-else>
      <span class="grid"></span>
      <span class="glow"></span>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  // aurora：流动光晕 | shapes：漂浮几何 | grid：点阵呼吸 + 视差
  variant: { type: String, default: 'aurora' }
})

const rootEl = ref(null)
const isStatic = ref(false) // 低端设备 / 系统偏好减少动效 → 静态降级
const isPaused = ref(false) // 页面不可见时暂停动画

// 几何元素位置：避开中央内容列，仅分布左右两侧（第 7 个起在小屏隐藏）
const shapes = [
  { kind: 'plus', left: '10%', top: '14%', size: 16, duration: 24, delay: -2 },
  { kind: 'square', left: '86%', top: '12%', size: 12, duration: 27, delay: -4 },
  { kind: 'ring', left: '16%', top: '40%', size: 12, duration: 28, delay: -9 },
  { kind: 'ring', left: '90%', top: '42%', size: 16, duration: 25, delay: -11 },
  { kind: 'square', left: '8%', top: '70%', size: 14, duration: 26, delay: -5 },
  { kind: 'plus', left: '84%', top: '72%', size: 18, duration: 29, delay: -6 },
  { kind: 'ring', left: '18%', top: '88%', size: 18, duration: 30, delay: -14 },
  { kind: 'square', left: '91%', top: '88%', size: 14, duration: 23, delay: -13 },
  { kind: 'plus', left: '78%', top: '26%', size: 12, duration: 22, delay: -7 },
  { kind: 'ring', left: '20%', top: '8%', size: 12, duration: 31, delay: -3 }
]

const detectStatic = () => {
  const reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const cores = navigator.hardwareConcurrency || 8
  const memory = navigator.deviceMemory || 8
  // 减少动效偏好 / 低端设备（≤2 核或 ≤2GB 内存）→ 静态降级
  isStatic.value = Boolean(reduceMotion || cores <= 2 || memory <= 2)
}

const onVisibilityChange = () => {
  isPaused.value = document.hidden
}

// grid 视差：rAF 级节流 ≈30fps，仅写 CSS 变量（合成层变换，不触发重排）
let lastTs = 0
const onPointerMove = (e) => {
  if (isStatic.value) return
  const now = performance.now()
  if (now - lastTs < 33) return
  lastTs = now
  const el = rootEl.value
  if (!el) return
  const dx = (e.clientX / window.innerWidth - 0.5) * 2
  const dy = (e.clientY / window.innerHeight - 0.5) * 2
  el.style.setProperty('--px', `${(dx * 10).toFixed(1)}px`)
  el.style.setProperty('--py', `${(dy * 10).toFixed(1)}px`)
}

onMounted(() => {
  detectStatic()
  onVisibilityChange()
  document.addEventListener('visibilitychange', onVisibilityChange)
  if (props.variant === 'grid' && !isStatic.value) {
    window.addEventListener('pointermove', onPointerMove, { passive: true })
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('pointermove', onPointerMove)
})
</script>

<style scoped lang="scss">
/* 背景层：固定铺满视口、隔离渲染、不接收事件、不参与无障碍树 */
.auth-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background: #fff;
  contain: strict;
}

/* ============ A. 流动光晕（Aurora） ============ */
.blob {
  position: absolute;
  display: block;
  border-radius: 50%;
  background: radial-gradient(closest-side, rgba(64, 158, 255, .20), rgba(64, 158, 255, 0) 72%);
  will-change: transform;
  animation: auth-drift 30s ease-in-out infinite alternate;
}

.blob-1 {
  top: -16vmax;
  left: -12vmax;
  width: 48vmax;
  height: 48vmax;
}

.blob-2 {
  right: -14vmax;
  bottom: -20vmax;
  width: 52vmax;
  height: 52vmax;
  background: radial-gradient(closest-side, rgba(121, 187, 255, .24), rgba(121, 187, 255, 0) 72%);
  animation-duration: 38s;
  animation-delay: -8s;
}

.blob-3 {
  top: 34%;
  left: 58%;
  width: 34vmax;
  height: 34vmax;
  background: radial-gradient(closest-side, rgba(64, 158, 255, .13), rgba(64, 158, 255, 0) 72%);
  animation-duration: 26s;
  animation-delay: -14s;
}

@keyframes auth-drift {
  from {
    transform: translate3d(0, 0, 0) scale(1);
  }
  to {
    transform: translate3d(5vmax, 4vmax, 0) scale(1.08);
  }
}

/* ============ B. 漂浮几何（考卷元素） ============ */
.shape {
  position: absolute;
  display: block;
  opacity: .5;
  will-change: transform;
  animation: auth-float 24s ease-in-out infinite alternate;
}

.shape--plus {
  background:
    linear-gradient(#79bbff, #79bbff) center / 100% 2px no-repeat,
    linear-gradient(#79bbff, #79bbff) center / 2px 100% no-repeat;
}

.shape--ring {
  border: 2px solid #a0cfff;
  border-radius: 50%;
}

.shape--square {
  border: 2px solid #a0cfff;
  border-radius: 4px;
}

@keyframes auth-float {
  from {
    transform: translate3d(0, -10px, 0) rotate(-6deg);
  }
  to {
    transform: translate3d(0, 12px, 0) rotate(8deg);
  }
}

/* ============ C. 点阵呼吸 + 两层视差 ============ */
.auth-bg--grid .grid {
  position: absolute;
  inset: -28px;
  background-image: radial-gradient(rgba(64, 158, 255, .22) 1.2px, transparent 1.2px);
  background-size: 22px 22px;
  transform: translate3d(var(--px, 0px), var(--py, 0px), 0);
  animation: auth-breathe 14s ease-in-out infinite alternate;
  will-change: transform, opacity;
}

.auth-bg--grid .glow {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 68vmax;
  height: 68vmax;
  margin: -34vmax 0 0 -34vmax;
  border-radius: 50%;
  background: radial-gradient(closest-side, rgba(64, 158, 255, .16), rgba(64, 158, 255, 0) 70%);
  transform: translate3d(calc(var(--px, 0px) * 2.2), calc(var(--py, 0px) * 2.2), 0);
  will-change: transform;
}

@keyframes auth-breathe {
  from {
    opacity: .55;
  }
  to {
    opacity: 1;
  }
}

/* ============ 降级与暂停 ============ */
.auth-bg.is-static * {
  animation: none !important;
}

.auth-bg.is-static .grid,
.auth-bg.is-static .glow {
  transform: none !important;
}

.auth-bg.is-paused * {
  animation-play-state: paused !important;
}

/* ============ 响应式：小屏减少元素、放慢节奏 ============ */
@media (max-width: 768px) {
  .blob {
    animation-duration: 44s;
  }

  .shape:nth-child(n + 7) {
    display: none;
  }

  .auth-bg--grid .grid {
    background-size: 18px 18px;
  }
}
</style>