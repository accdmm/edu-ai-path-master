<template>
  <div class="auth-bg" :class="{ 'is-static': isStatic, 'is-paused': isPaused }" aria-hidden="true">
    <span
      v-for="(s, i) in shapes"
      :key="i"
      class="shape"
      :class="[`shape--${s.kind}`, { 'is-mobile-hidden': !s.mobile }]"
      :style="{
        left: s.left,
        top: s.top,
        width: s.size + 'px',
        height: s.size + 'px',
        animationDuration: s.duration + 's',
        animationDelay: s.delay + 's'
      }"
    ></span>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const isStatic = ref(false) // 低端设备 / 系统偏好减少动效 → 静态降级
const isPaused = ref(false) // 页面不可见时暂停动画

// 漂浮几何（考卷元素 ＋ ○ □）：位置贴左右两侧，避开中央内容列
// mobile: true 的元素在小屏保留（贴最外侧），其余小屏隐藏
const shapes = [
  { kind: 'plus', left: '9%', top: '14%', size: 18, duration: 24, delay: -2, mobile: true },
  { kind: 'square', left: '87%', top: '12%', size: 14, duration: 27, delay: -4, mobile: true },
  { kind: 'ring', left: '15%', top: '38%', size: 14, duration: 28, delay: -9, mobile: false },
  { kind: 'ring', left: '91%', top: '40%', size: 18, duration: 25, delay: -11, mobile: true },
  { kind: 'square', left: '7%', top: '66%', size: 16, duration: 26, delay: -5, mobile: true },
  { kind: 'plus', left: '85%', top: '70%', size: 20, duration: 29, delay: -6, mobile: false },
  { kind: 'ring', left: '12%', top: '84%', size: 20, duration: 30, delay: -14, mobile: false },
  { kind: 'square', left: '92%', top: '86%', size: 16, duration: 23, delay: -13, mobile: true },
  { kind: 'plus', left: '80%', top: '24%', size: 14, duration: 22, delay: -7, mobile: false },
  { kind: 'ring', left: '20%', top: '8%', size: 13, duration: 31, delay: -3, mobile: false },
  { kind: 'plus', left: '24%', top: '58%', size: 12, duration: 33, delay: -10, mobile: false },
  { kind: 'ring', left: '76%', top: '56%', size: 13, duration: 34, delay: -8, mobile: false },
  { kind: 'square', left: '22%', top: '24%', size: 11, duration: 35, delay: -12, mobile: false },
  { kind: 'square', left: '78%', top: '88%', size: 12, duration: 32, delay: -15, mobile: false }
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

onMounted(() => {
  detectStatic()
  onVisibilityChange()
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
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

/* 漂浮几何：小小只、慢慢漂，只有 transform 动画（走合成层） */
.shape {
  position: absolute;
  display: block;
  opacity: .55;
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

/* ============ 降级与暂停 ============ */
.auth-bg.is-static * {
  animation: none !important;
}

.auth-bg.is-paused * {
  animation-play-state: paused !important;
}

/* ============ 响应式：小屏只保留最外侧元素、进一步降低存在感 ============ */
@media (max-width: 768px) {
  .shape {
    opacity: .4;
  }

  .shape.is-mobile-hidden {
    display: none;
  }
}
</style>