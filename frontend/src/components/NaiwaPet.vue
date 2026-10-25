<template>
  <div class="naiwa-pet">
    <span ref="shadowRef" class="pet-shadow" aria-hidden="true"></span>
    <div
      ref="bodyRef"
      class="pet-body"
      :class="{ 'is-interactive': interactive, 'is-dragging': dragging }"
      :tabindex="interactive ? 0 : -1"
      :role="interactive ? 'button' : undefined"
      :aria-label="interactive ? '奶蛙桌宠，可拖动或点击互动' : '奶蛙桌宠'"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointercancel="onPointerUp"
      @lostpointercapture="onPointerUp"
      @keydown.enter.prevent="poke"
      @keydown.space.prevent="poke"
    >
      <span class="pet-halo" aria-hidden="true"></span>
      <div class="pet-viewport" aria-hidden="true">
        <div
          v-for="layer in layers"
          :key="layer.key"
          class="pet-layer"
          :class="[`pet-strip-${layer.state}`, { 'is-visible': layer.opacity > 0 }]"
          :style="{ backgroundImage: `url(${ASSETS[layer.state]})`, opacity: layer.opacity }"
        ></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'

// 素材：https://github.com/timerring/codex-pet-naiwa（MIT）；Credits: LynnShaw/naiwa-pet

const props = defineProps({
  state: { type: String, default: 'waiting' },
  interactive: { type: Boolean, default: true }
})

const ASSETS = {
  idle: '/pets/naifrog/idle-strip.png',
  running: '/pets/naifrog/running-strip.png',
  jumping: '/pets/naifrog/jumping-strip.png',
  waiting: '/pets/naifrog/waiting-strip.png',
  failed: '/pets/naifrog/failed-strip.png'
}
const VALID_STATES = Object.keys(ASSETS)
const normalizeState = (state) => (VALID_STATES.includes(state) ? state : 'waiting')

// ---- 60fps 物理参数 ----
const HOME = { x: 0, y: 0 }
const WANDER_X = 14 // 待机时的小范围游走
const WANDER_Y = 4
const DRAG_X = 84 // 允许拖动/交互的范围，保证不破坏通话布局
const DRAG_Y = 38
const POS_STIFFNESS = 70
const POS_DAMPING = 19 // 接近临界阻尼，避免回弹过冲
const MAX_DT = 0.05
const CROSSFADE_MS = 240
const MAX_FRAME_DELTAS = 120

const bodyRef = ref()
const shadowRef = ref()
const dragging = ref(false)
const layers = reactive([
  { id: 0, gen: 0, key: '0-0', state: normalizeState(props.state), opacity: 1 },
  { id: 1, gen: 0, key: '1-0', state: normalizeState(props.state), opacity: 0 }
])

let activeIndex = 0
let currentState = normalizeState(props.state)
let transitioning = false
let pendingState = null
let crossfadeTimer = null

// ---- 运行时状态（不进入 Vue 响应式，避免 60fps 触发组件重渲染）----
const pos = { x: 0, y: 0, vx: 0, vy: 0 }
const target = { x: HOME.x, y: HOME.y }
const scale = { x: 1, y: 1 }
const drag = { active: false, pointerId: null, startX: 0, startY: 0, originX: 0, originY: 0, moved: 0 }
const stats = { fps: 60, avg: 16.7, p95: 16.7, max: 16.7, longFrames: 0 }
let reducedMotion = false
let rafId = 0
let lastTime = 0
let clock = 0
let nextWanderAt = 2.5
let statFrames = 0
let statAccum = 0
const frameDeltas = []
let motionQuery = null

const clamp = (v, min, max) => Math.max(min, Math.min(max, v))

const pushFrameDelta = (ms) => {
  frameDeltas.push(ms)
  if (frameDeltas.length > MAX_FRAME_DELTAS) frameDeltas.shift()
}

const updateStats = (dt) => {
  const ms = dt * 1000
  statFrames += 1
  statAccum += dt
  if (ms > 25) stats.longFrames += 1
  pushFrameDelta(ms)
  if (statAccum < 1) return
  const sorted = [...frameDeltas].sort((a, b) => a - b)
  stats.fps = statFrames / statAccum
  stats.avg = (statAccum * 1000) / statFrames
  stats.p95 = sorted[Math.min(sorted.length - 1, Math.floor(sorted.length * 0.95))] || 0
  stats.max = sorted[sorted.length - 1] || 0
  statFrames = 0
  statAccum = 0
  window.__naiwaPetStats = { ...stats }
}

// ---- 状态切换：双层精灵交叉淡化，避免从第 0 帧硬切 ----
const doubleRaf = (fn) => {
  requestAnimationFrame(() => requestAnimationFrame(fn))
}

const beginTransition = (nextState) => {
  const next = normalizeState(nextState)
  if (next === currentState) return
  if (transitioning) {
    pendingState = next
    return
  }
  const from = layers[activeIndex]
  const toIndex = 1 - activeIndex
  const to = layers[toIndex]
  // 换 state/gen/key 会在下一帧重新挂载该层，让精灵动画从第 0 帧开始
  to.state = next
  to.gen += 1
  to.key = `${to.id}-${to.gen}`
  to.opacity = 0
  transitioning = true
  nextTick(() => {
    doubleRaf(() => {
      to.opacity = 1
      from.opacity = 0
      if (crossfadeTimer) clearTimeout(crossfadeTimer)
      crossfadeTimer = setTimeout(() => {
        crossfadeTimer = null
        activeIndex = toIndex
        currentState = next
        transitioning = false
        if (pendingState && pendingState !== currentState) {
          const queued = pendingState
          pendingState = null
          beginTransition(queued)
        }
      }, CROSSFADE_MS)
    })
  })
}

watch(
  () => props.state,
  (next) => beginTransition(next)
)

// ---- rAF 物理循环 ----
const updateTarget = () => {
  if (drag.active) return
  const state = currentState
  if (state === 'running') {
    // 思考时轻微来回踱步
    target.x = reducedMotion ? HOME.x : Math.sin(clock * 2.4) * 10
    target.y = -1
    return
  }
  if (state === 'jumping') {
    target.x = HOME.x
    target.y = HOME.y
    return
  }
  if (state === 'failed') {
    target.x = reducedMotion ? HOME.x : Math.sin(clock * 9) * 1.2
    target.y = 1.5
    return
  }
  if (!reducedMotion && clock >= nextWanderAt) {
    target.x = (Math.random() * 2 - 1) * WANDER_X
    target.y = (Math.random() * 2 - 1) * WANDER_Y
    nextWanderAt = clock + 2.5 + Math.random() * 2.5
  }
}

const integrate = (dt) => {
  const ax = POS_STIFFNESS * (target.x - pos.x) - POS_DAMPING * pos.vx
  const ay = POS_STIFFNESS * (target.y - pos.y) - POS_DAMPING * pos.vy
  pos.vx += ax * dt
  pos.vy += ay * dt
  pos.x += pos.vx * dt
  pos.y += pos.vy * dt

  const limitX = DRAG_X
  const limitY = DRAG_Y
  if (pos.x > limitX) {
    pos.x = limitX
    pos.vx = 0
  } else if (pos.x < -limitX) {
    pos.x = -limitX
    pos.vx = 0
  }
  if (pos.y > limitY) {
    pos.y = limitY
    pos.vy = 0
  } else if (pos.y < -limitY) {
    pos.y = -limitY
    pos.vy = 0
  }
}

const render = (dt) => {
  if (!bodyRef.value) return
  const bob = reducedMotion || drag.active ? 0 : Math.sin(clock * 1.7) * 1.8
  // 由垂直速度驱动 squash & stretch：向上运动拉长、下落压扁
  const stretch = clamp(-pos.vy / 1600, -0.05, 0.07)
  const targetScaleY = 1 + stretch
  const targetScaleX = 1 - stretch * 0.6
  const k = 1 - Math.exp(-14 * dt)
  scale.x += (targetScaleX - scale.x) * k
  scale.y += (targetScaleY - scale.y) * k
  const rotation = reducedMotion ? 0 : clamp(pos.vx * 0.02, -3.5, 3.5)
  bodyRef.value.style.transform =
    `translate3d(${pos.x.toFixed(2)}px, ${(pos.y + bob).toFixed(2)}px, 0) ` +
    `scale(${scale.x.toFixed(4)}, ${scale.y.toFixed(4)}) rotate(${rotation.toFixed(2)}deg)`
  if (shadowRef.value) {
    const lift = clamp(-(pos.y + bob) / 70, -0.25, 0.7)
    const shadowScaleX = 1 + lift * 0.12
    const shadowScaleY = 1 - lift * 0.25
    shadowRef.value.style.transform =
      `translateX(-50%) translateX(${pos.x.toFixed(1)}px) ` +
      `scale(${shadowScaleX.toFixed(3)}, ${shadowScaleY.toFixed(3)})`
    shadowRef.value.style.opacity = String(clamp(1 - lift * 0.3, 0.6, 1))
  }
}

const tick = (now) => {
  if (!lastTime) {
    lastTime = now
    render(0)
    rafId = requestAnimationFrame(tick)
    return
  }
  let dt = (now - lastTime) / 1000
  lastTime = now
  if (dt > MAX_DT) dt = MAX_DT
  if (dt < 0) dt = 0
  clock += dt
  updateTarget()
  integrate(dt)
  render(dt)
  updateStats(dt)
  rafId = requestAnimationFrame(tick)
}

const startLoop = () => {
  if (rafId) return
  lastTime = 0
  rafId = requestAnimationFrame(tick)
}
const stopLoop = () => {
  if (rafId) cancelAnimationFrame(rafId)
  rafId = 0
}

const onVisibilityChange = () => {
  if (document.hidden) stopLoop()
  else startLoop()
}

// ---- 交互：点击跳跃、拖动跟随、松手弹簧回位 ----
const poke = () => {
  if (reducedMotion) return
  pos.vy = -260
  target.x = HOME.x
  target.y = HOME.y
}

const onPointerDown = (event) => {
  if (!props.interactive || !event.isPrimary || event.button !== 0) return
  event.preventDefault()
  drag.active = true
  dragging.value = true
  drag.pointerId = event.pointerId
  drag.startX = event.clientX
  drag.startY = event.clientY
  drag.originX = pos.x
  drag.originY = pos.y
  drag.moved = 0
  target.x = pos.x
  target.y = pos.y
  try {
    bodyRef.value.setPointerCapture(event.pointerId)
  } catch (e) { /* 部分旧浏览器不支持 pointer capture */ }
}

const onPointerMove = (event) => {
  if (!drag.active || event.pointerId !== drag.pointerId) return
  const dx = event.clientX - drag.startX
  const dy = event.clientY - drag.startY
  drag.moved = Math.max(drag.moved, Math.abs(dx) + Math.abs(dy))
  target.x = clamp(drag.originX + dx, -DRAG_X, DRAG_X)
  target.y = clamp(drag.originY + dy, -DRAG_Y, DRAG_Y)
}

const onPointerUp = (event) => {
  if (!drag.active) return
  if (event && drag.pointerId !== null && event.pointerId !== drag.pointerId) return
  drag.active = false
  dragging.value = false
  if (drag.pointerId !== null && bodyRef.value && bodyRef.value.hasPointerCapture(drag.pointerId)) {
    try { bodyRef.value.releasePointerCapture(drag.pointerId) } catch (e) { /* ignore */ }
  }
  target.x = HOME.x
  target.y = HOME.y
  if (drag.moved < 6) poke()
  drag.pointerId = null
}

const onMotionPreferenceChange = () => {
  reducedMotion = !!(motionQuery && motionQuery.matches)
  if (reducedMotion) {
    target.x = HOME.x
    target.y = HOME.y
    pos.x = HOME.x
    pos.y = HOME.y
    pos.vx = 0
    pos.vy = 0
    scale.x = 1
    scale.y = 1
    if (bodyRef.value) bodyRef.value.style.transform = 'translate3d(0, 0, 0)'
    if (shadowRef.value) {
      shadowRef.value.style.transform = 'translateX(-50%)'
      shadowRef.value.style.opacity = '1'
    }
  }
}

onMounted(() => {
  // 预加载各状态精灵条，首次切换不产生解码空白
  Object.values(ASSETS).forEach((src) => {
    const img = new Image()
    img.src = src
  })
  Object.assign(window, { __naiwaPetStats: { ...stats } })
  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  onMotionPreferenceChange()
  if (motionQuery.addEventListener) motionQuery.addEventListener('change', onMotionPreferenceChange)
  else if (motionQuery.addListener) motionQuery.addListener(onMotionPreferenceChange)
  document.addEventListener('visibilitychange', onVisibilityChange)
  startLoop()
})

onBeforeUnmount(() => {
  stopLoop()
  if (crossfadeTimer) clearTimeout(crossfadeTimer)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  if (motionQuery) {
    if (motionQuery.removeEventListener) motionQuery.removeEventListener('change', onMotionPreferenceChange)
    else if (motionQuery.removeListener) motionQuery.removeListener(onMotionPreferenceChange)
  }
  if (window.__naiwaPetStats) delete window.__naiwaPetStats
})
</script>

<style scoped>
.naiwa-pet {
  position: relative;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
.pet-body {
  position: absolute;
  inset: 0;
  transform-origin: 50% 100%;
  will-change: transform;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  user-select: none;
  -webkit-user-select: none;
  touch-action: none;
  pointer-events: none;
  outline: none;
}
.pet-body.is-interactive {
  pointer-events: auto;
  cursor: grab;
}
.pet-body.is-interactive.is-dragging { cursor: grabbing; }
.pet-body:focus-visible {
  outline: 2px solid #8a7cf5;
  outline-offset: 4px;
  border-radius: 12px;
}
.pet-halo {
  position: absolute;
  left: 50%;
  top: 50%;
  z-index: 0;
  width: 264px;
  height: 264px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(138, 124, 245, 0.26) 0%, rgba(110, 231, 216, 0.16) 46%, rgba(255, 159, 154, 0) 70%);
  pointer-events: none;
}
.pet-viewport {
  position: absolute;
  inset: 0;
  z-index: 1;
  overflow: hidden;
}
.pet-layer {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background-repeat: no-repeat;
  background-size: 100% 100%;
  opacity: 0;
  transition: opacity 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform, opacity;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  animation-play-state: paused;
}
.pet-layer.is-visible { animation-play-state: running; }
.pet-strip-idle { width: 600%; animation: pet-strip-6 1.2s steps(6, jump-none) infinite; }
.pet-strip-running { width: 600%; animation: pet-strip-6 1s steps(6, jump-none) infinite; }
.pet-strip-waiting { width: 600%; animation: pet-strip-6 1.2s steps(6, jump-none) infinite; }
.pet-strip-jumping { width: 500%; animation: pet-strip-5 0.8s steps(5, jump-none) infinite; }
.pet-strip-failed { width: 800%; animation: pet-strip-8 1.4s steps(8, jump-none) infinite; }
.pet-shadow {
  position: absolute;
  left: 50%;
  bottom: 2px;
  z-index: 0;
  width: 72%;
  height: 14px;
  transform: translateX(-50%);
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(92, 72, 46, 0.24), rgba(92, 72, 46, 0) 72%);
  filter: blur(2px);
  pointer-events: none;
}
@keyframes pet-strip-6 {
  from { transform: translateX(0); }
  to { transform: translateX(-83.3333%); }
}
@keyframes pet-strip-5 {
  from { transform: translateX(0); }
  to { transform: translateX(-80%); }
}
@keyframes pet-strip-8 {
  from { transform: translateX(0); }
  to { transform: translateX(-87.5%); }
}
@media (max-width: 640px) {
  .pet-halo { width: 208px; height: 208px; }
}
@media (prefers-reduced-motion: reduce) {
  .pet-layer { animation: none; }
  .pet-layer { transition-duration: 0.01s; }
}
</style>
