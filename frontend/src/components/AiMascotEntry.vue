<template>
  <!-- 首页悬浮「AI 入口」小图标：奶龙挥旗循环动画，点击进入 AI 面试官 -->
  <div class="ai-mascot-entry">
    <el-tooltip content="AI 面试官 · 奶龙陪你模拟面试" placement="left" :show-after="300">
      <div class="mascot-bob">
        <button
          ref="fabRef"
          type="button"
          class="mascot-fab"
          aria-label="进入 AI 面试官"
          @click="goAiInterview"
        >
          <video
            ref="videoRef"
            class="mascot-video"
            src="/pets/naifrog/flag.mp4"
            :autoplay="!reducedMotion"
            muted
            loop
            playsinline
            preload="auto"
            aria-hidden="true"
          ></video>
          <span class="mascot-badge" aria-hidden="true">AI</span>
        </button>
      </div>
    </el-tooltip>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const videoRef = ref(null)

// 尊重系统「减少动态效果」偏好：不自动播放，仅展示首帧静态图
const reducedMotion =
  typeof window !== 'undefined' &&
  window.matchMedia &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

const goAiInterview = () => {
  if (userStore.token) {
    router.push('/ai-interview')
  } else {
    router.push({ path: '/login', query: { redirect: '/ai-interview' } })
  }
}

// 标签页切后台时暂停循环播放，回来再续上（省电 / 省 CPU）
const onVisibilityChange = () => {
  const video = videoRef.value
  if (!video) return
  if (document.hidden) {
    video.pause()
  } else if (!reducedMotion) {
    video.play().catch(() => {})
  }
}

onMounted(() => {
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.ai-mascot-entry {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 1200; /* 高于页面内容，低于弹窗遮罩（el-overlay 2001） */
}

/* 外层负责呼吸浮动，内层负责 hover 缩放，避免 transform 相互覆盖 */
.mascot-bob {
  animation: mascot-bob 3.4s ease-in-out infinite;
}

.mascot-fab {
  position: relative;
  display: block;
  width: 84px;
  height: 84px;
  padding: 0;
  border: 3px solid #fff;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 8px 24px rgba(31, 41, 55, 0.22);
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.mascot-fab:hover {
  transform: translateY(-4px) scale(1.06);
  box-shadow: 0 14px 32px rgba(31, 41, 55, 0.28);
}

.mascot-fab:active {
  transform: scale(0.97);
}

.mascot-fab:focus-visible {
  outline: 2px solid #409eff;
  outline-offset: 3px;
}

.mascot-video {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 素材为 1:1 正方形内容，圆形裁切后奶龙与旗帜完整保留 */
  display: block;
  pointer-events: none;
}

/* 右上角 AI 角标：一眼看出这是 AI 功能入口 */
.mascot-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 24px;
  height: 20px;
  padding: 0 5px;
  border: 1.5px solid #fff;
  border-radius: 999px;
  background: linear-gradient(135deg, #409eff, #667eea);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 17px;
  text-align: center;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.45);
}

@keyframes mascot-bob {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

@media (max-width: 640px) {
  .ai-mascot-entry {
    right: 16px;
    bottom: 16px;
  }

  .mascot-fab {
    width: 64px;
    height: 64px;
    border-width: 2px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mascot-bob {
    animation: none;
  }

  .mascot-fab {
    transition: none;
  }
}
</style>
