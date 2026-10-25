<template>
  <!-- 首页悬浮「AI 入口」小图标：奶龙挥旗循环动画 + 旋转渐变光环，点击进入 AI 面试官 -->
  <div class="ai-mascot-entry">
    <el-tooltip content="AI 面试官 · 奶龙陪你模拟面试" placement="left" :show-after="300">
      <div class="mascot-float">
        <div class="mascot-hover">
          <span class="mascot-ring" aria-hidden="true"></span>
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
          </button>
        </div>
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

/* 三层结构：外层呼吸浮动 / 中层 hover 缩放 / 内层旋转渐变光环 + 圆形视频按钮
   分层避免 transform 相互覆盖 */
.mascot-float {
  animation: mascot-bob 3.4s ease-in-out infinite;
}

.mascot-hover {
  position: relative;
  width: 84px;
  height: 84px;
  transition: transform 0.25s ease;
}

.mascot-hover:hover {
  transform: scale(1.08);
}

/* 旋转渐变光环：conic 渐变跟随元素整体旋转，白色圆按钮盖住中心只露环 */
.mascot-ring {
  position: absolute;
  inset: -5px;
  border-radius: 50%;
  background: conic-gradient(
    from 0deg,
    #409eff,
    #667eea,
    #13c2c2,
    #ff6b6b,
    #ffa500,
    #409eff
  );
  box-shadow: 0 0 16px 2px rgba(102, 126, 234, 0.45);
  animation: mascot-ring-spin 4.5s linear infinite;
}

.mascot-fab {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 6px 18px rgba(31, 41, 55, 0.18);
  cursor: pointer;
  overflow: hidden;
  transition: box-shadow 0.25s ease;
}

.mascot-hover:hover .mascot-fab {
  box-shadow: 0 10px 26px rgba(31, 41, 55, 0.26);
}

.mascot-fab:active {
  transform: scale(0.97);
}

.mascot-fab:focus-visible {
  outline: 2px solid #409eff;
  outline-offset: 4px;
}

.mascot-video {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 素材为 1:1 正方形内容，圆形裁切后奶龙与旗帜完整保留 */
  display: block;
  pointer-events: none;
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

@keyframes mascot-ring-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .ai-mascot-entry {
    right: 16px;
    bottom: 16px;
  }

  .mascot-hover {
    width: 64px;
    height: 64px;
  }

  .mascot-ring {
    inset: -4px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mascot-float,
  .mascot-ring {
    animation: none;
  }

  .mascot-hover {
    transition: none;
  }
}
</style>
