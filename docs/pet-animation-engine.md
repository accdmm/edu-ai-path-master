# 桌宠平滑动画引擎实现方案

> 目标：参考 GitHub 桌宠项目的动画与物理实现，把面试页的奶蛙桌宠升级为「60fps 渲染循环 + 状态交叉淡化 + 弹簧物理位移 + 指针交互」的动画引擎。
>
> 实现文件：[NaiwaPet.vue](../frontend/src/components/NaiwaPet.vue)、[AiInterview.vue](../frontend/src/views/AiInterview.vue)

---

## 1. 参考项目与核心结论

### 1.1 Shimeji / Shimeji-Desktop

Shimeji 是 Windows / macOS / Linux 上经典的桌面宠物，动作通过 XML 声明，每个 `Pose` 包含：

| 字段 | 含义 |
| --- | --- |
| `Image` | 当前帧图片 |
| `ImageAnchor` | 帧锚点，保证换帧不漂移 |
| `Velocity` | 每帧位移，如 `-2,0` |
| `Duration` | 当前姿势持续帧数，如 `6` |

其关键思想：

- **动作 = 状态机 + 姿势序列**，移动速度和动画帧绑定；
- **位移按帧积分**，配合重力、地面/墙壁边界形成自然的走动和下落；
- 条件动画（`Condition`）根据鼠标位置、边界等环境状态选择动作；
- 锚点固定，避免换帧时角色位置跳变。

参考：
- <https://github.com/DalekCraft2/Shimeji-Desktop>
- <https://deepwiki.com/zh050707-lab/-shimeji-AI-/4.2-animation-system>
- <https://deepwiki.com/jinile/shimeji/2-system-architecture>

### 1.2 Petdex / Buddy

Petdex 定义 8×9（1536×1872）精灵图集，每个状态一行、每帧 192×208，`pet.json` 描述状态、帧序与每帧时长。Buddy 的 `PetSprite.svelte` 用 `background-position` 切换帧：

```js
function frameToPosition(frame) {
  const xPct = (frame.col / (COLUMNS - 1)) * 100
  const yPct = (frame.row / (ROWS - 1)) * 100
  return `${xPct}% ${yPct}%`
}
```

其状态切换逻辑：

- `$effect` 监听 `state`，启动对应帧序列；
- `setTimeout` 按 `frame.ms` 播下一帧；
- 一次性动作结束后回退到 `fallback` 状态；
- 指针按下/移动/抬起通过 Pointer Events 驱动拖拽。

可借鉴点：**状态 manifest 与渲染解耦、一次性动作回退、Pointer Events 交互**。
不足：`setTimeout` 逐帧调度在后台标签页/主线程繁忙时不稳定，且 `background-position` 每帧触发布局/绘制，不如 `transform` 适合 60fps。

参考：
- <https://github.com/crafter-station/petdex>
- <https://petdex.dev/docs>
- <https://github.com/AG9898/buddy>

### 1.3 Mishi Pet

Mishi 是基于 PixiJS 的 Shimeji 风格网站桌宠：

- **状态转移表**驱动自动行为，循环状态随机持续时长，一次性状态播完再转移；
- 拖拽时根据速度/方向反应，松手后**重力下落 + 弹跳**；
- 物理常量集中在 `constants.js`，状态逻辑按 `states/walk.js`、`states/drag.js` 拆分。

可借鉴点：**状态转移表、重力/弹跳、拖拽动量、状态逻辑拆分**。

参考：<https://www.npmjs.com/package/mishi-pet>

### 1.4 结论

| 问题 | Shimeji | Buddy/Petdex | Mishi | 本实现 |
| --- | --- | --- | --- | --- |
| 帧序列 | Pose + Duration | pet.json + setTimeout | PixiJS ticker | CSS `steps()` + 双图层 |
| 状态切换 | XML 条件动画 | `$effect` 重启动画 | 状态转移表 | Vue props + 交叉淡化队列 |
| 位移 | 每帧 Velocity + 重力 | 无（拖拽窗口） | 重力/弹跳 | rAF + 临界阻尼弹簧 |
| 60fps | 固定帧循环 | setTimeout | PixiJS ticker | requestAnimationFrame + delta time |
| 交互 | 鼠标/边界条件 | Pointer Events 拖拽 | 拖拽/抛掷 | Pointer Events 拖拽 + 点击跳跃 |

---

## 2. 实现架构

```
AiInterview.vue（业务状态机）
  ├─ waiting   AI 等待用户输入
  ├─ running   AI 思考 / 请求中
  ├─ jumping   用户正在输入（捧腹大笑）
  └─ failed    本轮回答较差（3 秒后回 waiting）
        │  props.state
        ▼
NaiwaPet.vue（渲染 + 物理）
  ├─ 双层精灵交叉淡化（220ms）
  ├─ requestAnimationFrame 60fps 循环
  ├─ 临界阻尼弹簧（位置/回位）
  ├─ squash & stretch（由垂直速度驱动）
  ├─ 待机游走 / 思考踱步 / 失败微抖
  └─ 点击跳跃、拖拽跟随、松手弹回
```

业务状态机仍由 `AiInterview.vue` 控制，`NaiwaPet.vue` 只负责「怎么动、怎么切」，两者解耦，后续可以把组件复用到其他页面。

---

## 3. 关键实现

### 3.1 60fps 主循环与 delta time

不使用 `setInterval` / 逐帧 `setTimeout`，统一用 rAF；每帧计算 `dt` 并限制上限，避免标签页恢复时出现大跳。

```js
const MAX_DT = 0.05

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
  clock += dt
  updateTarget()
  integrate(dt)
  render(dt)
  updateStats(dt)
  rafId = requestAnimationFrame(tick)
}
```

页面隐藏时停止 rAF，恢复时重置 `lastTime`：

```js
const onVisibilityChange = () => {
  if (document.hidden) stopLoop()
  else startLoop()
}
```

### 3.2 临界阻尼弹簧（位置平滑）

位置不是直接赋值，而是通过弹簧积分，保证「起步有加速、停下有余量但不明显过冲」：

```js
const POS_STIFFNESS = 70
const POS_DAMPING = 19 // 接近 2√stiffness，临界阻尼

const integrate = (dt) => {
  const ax = POS_STIFFNESS * (target.x - pos.x) - POS_DAMPING * pos.vx
  const ay = POS_STIFFNESS * (target.y - pos.y) - POS_DAMPING * pos.vy
  pos.vx += ax * dt
  pos.vy += ay * dt
  pos.x += pos.vx * dt
  pos.y += pos.vy * dt
  // 边界钳制，防止拖出通话区
  // ...
}
```

拖拽松手时目标回到 `HOME`，位置由弹簧自然回位，不会瞬移：

```js
target.x = HOME.x
target.y = HOME.y
```

### 3.3 squash & stretch（物理形变）

由垂直速度推导拉伸量，向上运动略微拉长、下落略微压扁，并指数平滑：

```js
const stretch = clamp(-pos.vy / 1600, -0.05, 0.07)
const targetScaleY = 1 + stretch
const targetScaleX = 1 - stretch * 0.6
const k = 1 - Math.exp(-14 * dt)
scale.x += (targetScaleX - scale.x) * k
scale.y += (targetScaleY - scale.y) * k
```

最终只写一个 `transform`，交给合成层：

```js
body.style.transform =
  `translate3d(${pos.x}px, ${pos.y + bob}px, 0) ` +
  `scale(${scale.x}, ${scale.y}) rotate(${rotation}deg)`
```

- 不用 `left/top`，避免每帧触发布局；
- `transform-origin: 50% 100%` 让压扁/拉伸围绕脚底，角色不会悬空；
- `will-change: transform` + `backface-visibility: hidden` 提升合成稳定性。

### 3.4 双层交叉淡化（状态切换无跳变）

如果直接换精灵图，会从新状态第 0 帧硬切，出现「跳一下」。这里使用两层精灵：

- 当前层可见、另一层隐藏；
- 切换时把新状态挂到隐藏层，重挂载让它从第 0 帧开始；
- 新层 opacity `0 → 1`，旧层 `1 → 0`，220ms `cubic-bezier(0.4, 0, 0.2, 1)`；
- 过渡结束后交换 active 层；
- 过渡期间再次切换则进入队列，避免图层互相覆盖。

```js
const beginTransition = (nextState) => {
  if (next === currentState) return
  if (transitioning) { pendingState = next; return }

  const from = layers[activeIndex]
  const to = layers[1 - activeIndex]
  to.state = next
  to.gen += 1
  to.key = `${to.id}-${to.gen}` // 重挂载 → 精灵从第 0 帧开始
  to.opacity = 0
  transitioning = true

  nextTick(() => {
    doubleRaf(() => {
      to.opacity = 1
      from.opacity = 0
      setTimeout(() => {
        activeIndex = 1 - activeIndex
        currentState = next
        transitioning = false
        // 处理排队状态
      }, CROSSFADE_MS)
    })
  })
}
```

### 3.5 CSS 精灵逐帧动画

每条精灵条包含 N 帧，元素宽度为 `N × 100%`，用 `translateX` + `steps(N, jump-none)` 精确走帧：

```css
.pet-strip-waiting { width: 600%; animation: pet-strip-6 1.2s steps(6, jump-none) infinite; }
.pet-strip-jumping { width: 500%; animation: pet-strip-5 0.8s steps(5, jump-none) infinite; }
.pet-strip-failed  { width: 800%; animation: pet-strip-8 1.4s steps(8, jump-none) infinite; }

@keyframes pet-strip-6 {
  from { transform: translateX(0); }
  to   { transform: translateX(-83.3333%); } /* (6-1)/6 */
}
@keyframes pet-strip-5 {
  from { transform: translateX(0); }
  to   { transform: translateX(-80%); }      /* (5-1)/5 */
}
@keyframes pet-strip-8 {
  from { transform: translateX(0); }
  to   { transform: translateX(-87.5%); }    /* (8-1)/8 */
}
```

隐藏层通过 `animation-play-state: paused` 暂停，避免后台动画消耗合成资源。

### 3.6 指针交互

- `pointerdown` 捕获指针，记录起点与当前位置；
- `pointermove` 更新弹簧目标，桌宠平滑跟随手指/鼠标；
- `pointerup` 目标回到 HOME，弹簧自然回位；
- 位移小于 6px 视为点击，给一个向上冲量实现「点击跳一下」；
- `touch-action: none` 防止移动端拖拽时页面滚动；
- 阴影独立于身体，按高度缩放/淡出，产生离地感。

```js
const onPointerUp = (event) => {
  drag.active = false
  target.x = HOME.x
  target.y = HOME.y
  if (drag.moved < 6) poke()
}

const poke = () => {
  if (reducedMotion) return
  pos.vy = -260 // 向上冲量，弹簧负责回落
}
```

### 3.7 无障碍与系统偏好

- 监听 `prefers-reduced-motion: reduce`：关闭游走、呼吸、跳跃，暂停精灵动画；
- 桌宠可聚焦，`Enter` / `Space` 触发互动；
- `aria-label` 说明可拖动/点击；
- 页面隐藏时暂停 rAF，避免后台耗电。

### 3.8 FPS 统计

组件每秒计算一次：

- `fps = frames / elapsed`
- `avg`、`p95`、`max` 帧间隔
- `longFrames`（>25ms 的帧数）

结果暴露到 `window.__naiwaPetStats`，便于自动化验证：

```js
window.__naiwaPetStats
// 实测示例：{ fps: 60.01, avg: 16.66, p95: 16.80, longFrames: 1, ... }
```

---

## 4. 业务状态映射

| 业务场景 | `petState` | 动画 | 额外运动 |
| --- | --- | --- | --- |
| AI 等待输入 | `waiting` | 6 帧等待，1.2s / 5fps | 小范围随机游走 ±14px |
| AI 思考/请求中 | `running` | 6 帧执行任务，1.0s / 6fps | 左右轻微踱步 ±10px |
| 用户正在输入 | `jumping` | 5 帧捧腹大笑，0.8s / 6.25fps | 回到中位，物理跳跃由交互触发 |
| 本轮回答较差 | `failed` | 8 帧失败，1.4s / 5.7fps | 微抖，3 秒后回 waiting |
| 页面无会话 | `idle` | 6 帧待机，1.2s / 5fps | 游走/呼吸 |

> 说明：渲染循环是 60fps；精灵图本身只有 5–8 张原画，所以「内容帧率」是 5–6fps，位置、形变、透明度过渡是 60fps。这是低帧数美术素材的正常表现，不是掉帧。若需要内容也达到 60fps，需要离线生成补间帧（光流/RIFE），属于另一项资产处理工作。

---

## 5. 效果验证标准

| 指标 | 目标 | 本次实测 |
| --- | --- | --- |
| 渲染帧率 | 59–61fps | **60.01fps** |
| 平均帧间隔 | ≈16.7ms | **16.66ms** |
| p95 帧间隔 | <20ms | **16.80ms** |
| 长帧（>25ms） | 稳定后为 0 | **1（启动首帧）** |
| 待机运动 | transform 持续变化、无瞬移 | 两次采样 transform 不同 |
| 打字切换 | 可见层为 `pet-strip-jumping`，旧层 opacity 0 | 通过 |
| 点击反馈 | 向上跳跃、阴影缩放/淡出 | transform 负向位移，shadow opacity 0.9865 |
| 状态切换 | 220ms 交叉淡化，无第 0 帧硬切 | 双层结构 + 队列，通过 |
| Console | 无 error | 通过 |
| 跨系统 | 同一套标准 API，无 OS 分支 | Chromium/Windows 实测；Safari 14.1+ 支持 `steps(jump-none)`、Pointer Events、rAF |

### 复测方法

```js
// 控制台
window.__naiwaPetStats
```

或在自动化里：

```js
const stats = await page.evaluate(() => window.__naiwaPetStats)
expect(stats.fps).toBeGreaterThan(58)
expect(stats.p95).toBeLessThan(20)
```

---

## 6. 跨平台说明

- 使用标准 Web API：`requestAnimationFrame`、Pointer Events、CSS `transform` / `steps()` / `prefers-reduced-motion`；
- 不依赖 Windows/macOS 专有接口，浏览器内表现一致；
- Windows Chrome/Edge、macOS Chrome/Safari 14.1+ 均支持；
- 后台标签页会被浏览器降频，代码在 `visibilitychange` 时暂停并在恢复时重置 `lastTime`，避免「恢复后瞬移」；
- 本文实现的是**网页内嵌桌宠**。如果需要 OS 级「始终置顶、透明窗口、跨应用悬浮」，需要再用 Electron/Tauri 包一层窗口；动画引擎本身可原样复用（它只依赖 DOM + rAF）。

---

## 7. 文件与后续可扩展点

- 组件：[frontend/src/components/NaiwaPet.vue](../frontend/src/components/NaiwaPet.vue)
- 接入页：[frontend/src/views/AiInterview.vue](../frontend/src/views/AiInterview.vue)
- 精灵素材：`frontend/public/pets/naifrog/*-strip.png`（来自 codex-pet-naiwa，MIT；Credits: LynnShaw/naiwa-pet）

可扩展：
1. 状态转移配置化（类似 Shimeji XML / Mishi 转移表），支持自动游走、坐下、打招呼；
2. 拖拽抛掷的动量与重力弹跳；
3. 视线跟随鼠标（多方向精灵）；
4. 离线补间帧生成，提升内容帧率；
5. Electron/Tauri 桌面悬浮窗封装。

---

## 8. 参考来源

- [DalekCraft2/Shimeji-Desktop](https://github.com/DalekCraft2/Shimeji-Desktop)
- [Shimeji Animation System（DeepWiki）](https://deepwiki.com/zh050707-lab/-shimeji-AI-/4.2-animation-system)
- [jinile/shimeji System Architecture（DeepWiki）](https://deepwiki.com/jinile/shimeji/2-system-architecture)
- [crafter-station/petdex](https://github.com/crafter-station/petdex)
- [Petdex Docs](https://petdex.dev/docs)
- [AG9898/buddy](https://github.com/AG9898/buddy)
- [mishi-pet（npm）](https://www.npmjs.com/package/mishi-pet)
- [timerring/codex-pet-naiwa](https://github.com/timerring/codex-pet-naiwa)

