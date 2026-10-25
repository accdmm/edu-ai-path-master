<template>
  <div class="ai-interview">
    <!-- 网络异常提示：不阻塞页面，恢复后自动消失 -->
    <el-alert
      v-if="!isOnline"
      title="网络已断开，请检查网络连接"
      description="当前面试进度已保留，网络恢复后可继续作答"
      type="error"
      :closable="false"
      show-icon
      class="offline-banner"
    />
    <!-- 配置页 -->
    <div v-if="stage === 'config'" class="interview-config">
      <section class="config-hero">
        <div class="config-hero-copy">
          <div class="hero-kicker">
            <span class="ai-badge">AI</span>
            <span>智能模拟面试</span>
          </div>
          <h2>像真实面试一样<br /><em>被追问，也被看见</em></h2>
          <p>选择技术方向与难度，AI 面试官会实时提问、逐轮追问，并在结束后生成评分、优势分析与参考答案。</p>
          <div class="hero-chips">
            <span class="hero-chip">实时追问</span>
            <span class="hero-chip">逐题点评</span>
            <span class="hero-chip">薄弱点诊断</span>
          </div>
          <div class="hero-meta">
            <span class="credit-pill"><el-icon><Coin /></el-icon>每场 10 积分</span>
            <span class="credit-pill is-soft">当前积分 {{ credits }}</span>
          </div>
        </div>
        <div class="hero-flow" aria-hidden="true">
          <span><i>1</i>选择方向</span>
          <span><i>2</i>实时追问</span>
          <span><i>3</i>报告复盘</span>
        </div>
        <div class="config-hero-pet" aria-hidden="true">
          <NaiwaPet state="waiting" :interactive="false" />
        </div>
      </section>

      <section class="config-panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">INTERVIEW SETUP</span>
            <h3>面试设置</h3>
          </div>
          <span class="panel-note"><el-icon><Timer /></el-icon>约 15–30 分钟</span>
        </div>

        <el-form :model="config" :rules="rules" ref="configRef" label-position="top" class="config-form">
          <el-form-item label="技术方向" prop="direction">
            <el-select v-model="config.direction" placeholder="选择技术方向" size="large">
              <el-option label="Java" value="java" />
              <el-option label="前端" value="frontend" />
              <el-option label="大数据" value="bigdata" />
              <el-option label="算法" value="algorithm" />
              <el-option label="运维" value="devops" />
              <el-option label="测试" value="testing" />
            </el-select>
          </el-form-item>

          <el-form-item label="难度" prop="difficulty">
            <el-radio-group v-model="config.difficulty" class="segmented">
              <el-radio-button value="easy">简单</el-radio-button>
              <el-radio-button value="medium">中等</el-radio-button>
              <el-radio-button value="hard">困难</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="面试轮数" prop="maxRounds">
            <el-radio-group v-model="config.maxRounds" class="segmented">
              <el-radio-button :value="3">3 轮</el-radio-button>
              <el-radio-button :value="4">4 轮</el-radio-button>
              <el-radio-button :value="6">6 轮</el-radio-button>
              <el-radio-button :value="8">8 轮</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="个性化推荐">
            <div class="switch-row">
              <el-switch v-model="config.personalized" active-text="开" inactive-text="关" />
              <span class="form-tip">结合你的答题诊断，优先考察薄弱知识点</span>
            </div>
          </el-form-item>

          <div class="video-tip">
            <el-icon><VideoCamera /></el-icon>
            <span>面试页支持开启摄像头做本地模拟预览，不会上传或录制</span>
          </div>
        </el-form>

        <div class="config-summary">
          <span class="summary-label">本次配置</span>
          <div class="summary-tags">
            <span>{{ directionLabel }}</span>
            <i></i>
            <span>{{ difficultyLabel }}</span>
            <i></i>
            <span>{{ config.maxRounds }} 轮</span>
            <i></i>
            <span>{{ config.personalized ? '个性化' : '标准模式' }}</span>
          </div>
        </div>

        <div class="config-actions">
          <el-button class="start-btn" type="primary" :loading="starting" @click="handleStart">
            开始面试
            <el-icon class="start-arrow"><ArrowRight /></el-icon>
          </el-button>
          <el-button class="ghost-btn" text @click="$router.go(-1)">返回</el-button>
        </div>
      </section>
    </div>

    <!-- 面试对话页 -->
    <div v-else-if="stage === 'chatting'" class="interview-chat">
      <div class="chat-header">
        <div class="chat-title">
          <span class="ai-badge">AI</span>
          <div>
            <h3>AI 面试官</h3>
            <span class="chat-sub">第 {{ round }} / {{ maxRounds }} 轮 · {{ directionLabel }} · {{ difficultyLabel }}</span>
          </div>
        </div>
        <div class="header-actions">
          <div class="mode-switch" :title="textMode ? '当前：视频 + 文字混合模式' : '当前：纯视频通话模式'">
            <el-icon class="mode-icon"><ChatDotRound /></el-icon>
            <span class="mode-label">文字回答</span>
            <el-switch v-model="textMode" size="small" />
          </div>
          <span class="call-live"><i class="live-dot"></i>面试中</span>
        </div>
      </div>

      <!-- 视频通话台：AI 面试官使用奶蛙桌宠形象（模拟），用户摄像头为右侧竖屏小窗 -->
      <div class="call-stage" :class="{ 'text-open': textMode }">
        <span class="ai-orb-caption">{{ aiLoading ? 'AI 面试官正在提问…' : 'AI 面试官在线' }}</span>
        <div class="ai-pet-wrap">
          <NaiwaPet :state="petState" />
        </div>

        <div class="user-cam" :class="{ 'cam-on': cameraOn }">
          <video
            v-show="cameraOn"
            ref="videoRef"
            autoplay
            muted
            playsinline
            class="cam-video"
          ></video>
          <div v-if="!cameraOn" class="cam-off" :title="cameraHint">
            <el-icon><VideoCameraFilled /></el-icon>
            <span>摄像头未开启</span>
          </div>
          <span class="cam-label">我</span>
        </div>

        <!-- 纯视频模式：仅显示当前问题字幕，不展示对话记录与输入区 -->
        <transition name="subtitle-fade">
          <div v-if="!textMode && lastAiMessage" class="call-subtitle">
            <div class="subtitle-head">
              <span class="subtitle-tag">AI 面试官</span>
              <span v-if="aiLoading" class="subtitle-status">正在组织问题…</span>
            </div>
            <p class="subtitle-text">{{ lastAiMessage }}</p>
            <button v-if="awaitingAnswer" class="subtitle-action" @click="openTextMode">
              <el-icon><ChatDotRound /></el-icon>文字作答
            </button>
          </div>
        </transition>

        <div class="call-timer">{{ elapsedText }}</div>

        <div class="call-controls">
          <button
            class="ctrl-btn"
            :class="{ 'ctrl-muted': micMuted }"
            :title="micMuted ? '取消静音（模拟）' : '静音（模拟）'"
            @click="toggleMic"
          >
            <el-icon><Mute v-if="micMuted" /><Microphone v-else /></el-icon>
          </button>
          <button class="ctrl-btn ctrl-hangup" title="结束面试" :disabled="finishing" @click="handleHangup">
            <el-icon><PhoneFilled /></el-icon>
          </button>
          <button
            class="ctrl-btn"
            :class="{ 'ctrl-active': cameraOn }"
            :title="cameraOn ? '关闭视频' : '开启视频'"
            @click="toggleCamera"
          >
            <el-icon><VideoCameraFilled v-if="cameraOn" /><VideoCamera v-else /></el-icon>
          </button>
        </div>
      </div>

      <!-- 降级提示：AI 超时/失败时使用备用题目或点评 -->
      <el-alert
        v-if="degradedHint"
        :title="degradedHint"
        type="warning"
        :closable="false"
        show-icon
        class="degraded-alert"
      />

      <!-- 文字回答面板：默认收起，开启后展开；通话区高度自适应，切换不跳动 -->
      <div class="text-panel" :class="{ open: textMode }" :inert="!textMode" @input="handleTyping">
        <div class="text-panel-inner">
          <div class="chat-body" ref="chatBodyRef">
            <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
              <div class="msg-avatar" :class="m.role">
                <el-icon v-if="m.role === 'ai'"><Microphone /></el-icon>
                <el-icon v-else><User /></el-icon>
              </div>
              <div class="msg-bubble" :class="m.role">{{ m.content }}</div>
            </div>
            <div v-if="aiLoading" class="msg-row ai">
              <div class="msg-avatar ai"><el-icon><Microphone /></el-icon></div>
              <div class="msg-bubble ai typing">面试官正在思考…</div>
            </div>
          </div>

          <div class="chat-input">
            <el-input
              ref="answerInputRef"
              v-model="answerText"
              type="textarea"
              :rows="2"
              placeholder="输入你的回答，回车发送（Shift+回车换行）"
              :disabled="aiLoading"
              @keydown.enter.exact.prevent="handleSend"
            />
            <div class="input-actions">
              <el-button type="primary" :loading="aiLoading" :disabled="!answerText.trim()" @click="handleSend">发送</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 报告生成中 -->
    <div v-else-if="stage === 'waiting'" class="interview-waiting">
      <section class="waiting-card">
        <el-icon class="waiting-icon"><Loading /></el-icon>
        <h3>面试已结束，正在生成面试报告…</h3>
        <p>AI 正在整理你的表现评分、每题参考答案与改进建议</p>
        <p class="waiting-tip">通常需要 30-60 秒，完成后自动展示，请勿关闭页面</p>
        <el-alert
          v-if="pollHint"
          :title="pollHint"
          type="warning"
          :closable="false"
          show-icon
          class="poll-alert"
        />
        <el-button v-if="reportFailed" type="warning" @click="handleRetryReport">重新生成报告</el-button>
      </section>
    </div>

    <!-- 面试结果页 -->
    <div v-else-if="stage === 'finished'" class="interview-result">
      <section class="result-hero" :class="`tone-${scoreTone}`">
        <div class="result-score">
          <svg class="score-ring" viewBox="0 0 120 120" aria-hidden="true">
            <defs>
              <linearGradient id="scoreGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#8a7cf5" />
                <stop offset="55%" stop-color="#6ee7d8" />
                <stop offset="100%" stop-color="#ffb36b" />
              </linearGradient>
            </defs>
            <circle class="score-ring-bg" cx="60" cy="60" r="52" />
            <circle
              class="score-ring-value"
              cx="60"
              cy="60"
              r="52"
              :style="{ '--score-offset': `${scoreOffset}px` }"
              :stroke-dasharray="326.73"
              stroke="url(#scoreGradient)"
            />
          </svg>
          <div class="score-center">
            <strong>{{ scoreValue }}</strong>
            <span>/ 100</span>
          </div>
        </div>

        <div class="result-hero-copy">
          <div class="result-kicker"><el-icon><TrophyBase /></el-icon> INTERVIEW REPORT</div>
          <h2>本次面试结果</h2>
          <p class="result-summary-text">{{ fmtAi(result.summary) }}</p>
          <div class="result-meta">
            <span>{{ directionLabel }}</span>
            <i></i>
            <span>{{ difficultyLabel }}</span>
            <i></i>
            <span>{{ maxRounds }} 轮</span>
          </div>
        </div>

        <div class="result-hero-pet" aria-hidden="true">
          <video
            class="result-laugh-video"
            src="/pets/naifrog/laugh.mp4"
            autoplay
            muted
            loop
            playsinline
            preload="auto"
          ></video>
        </div>
      </section>

      <el-alert
        v-if="result.degraded"
        title="本次报告使用了降级评分"
        description="部分 AI 请求超时或不可用，分数与建议仅供参考，建议重新面试以获取完整评价"
        type="warning"
        :closable="false"
        show-icon
        class="degraded-report"
      />

      <div
        class="result-grid"
        :class="{ 'is-single': !(result.strengths && result.strengths.length) || !(result.improvements && result.improvements.length) }"
      >
        <section class="insight-card strengths" v-if="result.strengths && result.strengths.length">
          <div class="insight-head">
            <span class="insight-icon"><el-icon><CircleCheckFilled /></el-icon></span>
            <div><h3>优势表现</h3><p>做得好的地方</p></div>
          </div>
          <ul class="insight-list">
            <li v-for="(s, i) in result.strengths" :key="i">
              <span class="dot"></span>
              <span>{{ fmtAi(s) }}</span>
            </li>
          </ul>
        </section>

        <section class="insight-card improvements" v-if="result.improvements && result.improvements.length">
          <div class="insight-head">
            <span class="insight-icon"><el-icon><WarningFilled /></el-icon></span>
            <div><h3>改进建议</h3><p>下一步可以加强</p></div>
          </div>
          <ul class="insight-list">
            <li v-for="(s, i) in result.improvements" :key="i">
              <span class="dot"></span>
              <span>{{ fmtAi(s) }}</span>
            </li>
          </ul>
        </section>
      </div>

      <section class="insight-card diagnosis-card" v-if="result.diagnosis && result.diagnosis.length">
        <div class="insight-head">
          <span class="insight-icon"><el-icon><Aim /></el-icon></span>
          <div><h3>薄弱知识点</h3><p>来自你的答题诊断</p></div>
        </div>
        <div class="diag-grid">
          <div class="diag-item" v-for="d in result.diagnosis" :key="d.categoryName">
            <div class="diag-top">
              <span>{{ d.categoryName }}</span>
              <strong :class="d.correctRate < 50 ? 'low' : (d.correctRate < 80 ? 'mid' : 'high')">{{ d.correctRate }}%</strong>
            </div>
            <div class="diag-track">
              <i
                :class="d.correctRate < 50 ? 'low' : (d.correctRate < 80 ? 'mid' : 'high')"
                :style="{ width: `${Math.max(0, Math.min(100, d.correctRate))}%` }"
              ></i>
            </div>
          </div>
        </div>
      </section>

      <section class="insight-card qa-section" v-if="result.qaReview && result.qaReview.length">
        <div class="insight-head qa-head">
          <span class="insight-icon"><el-icon><ChatDotRound /></el-icon></span>
          <div><h3>题目回顾与参考答案</h3><p>逐题复盘你的回答</p></div>
          <span class="qa-count">{{ result.qaReview.length }} 道</span>
        </div>
        <div class="qa-list">
          <article class="qa-card" v-for="(qa, i) in result.qaReview" :key="i">
            <div class="qa-top">
              <span class="qa-index">Q{{ i + 1 }}</span>
              <span class="qa-score" :class="qaScoreTone(qa.score)">单题 {{ qa.score }} 分</span>
            </div>
            <div class="qa-line">
              <span class="qa-label">题目</span>
              <p>{{ fmtAi(qa.question) }}</p>
            </div>
            <div class="qa-line answer">
              <span class="qa-label">你的回答</span>
              <p>{{ fmtAi(qa.userAnswer) || '（未作答）' }}</p>
            </div>
            <div class="qa-line ideal">
              <span class="qa-label">参考答案</span>
              <p>{{ fmtAi(qa.idealAnswer) || '（暂无）' }}</p>
            </div>
          </article>
        </div>
      </section>

      <div class="result-actions">
        <el-button class="start-btn" type="primary" @click="resetAll">
          再面一次
          <el-icon class="start-arrow"><ArrowRight /></el-icon>
        </el-button>
        <el-button class="ghost-btn" @click="goLearningPath">生成学习路径</el-button>
        <el-button class="ghost-btn" @click="$router.push('/interview-questions')">查看真题</el-button>
      </div>
    </div>
  </div>
  <BackHome />
  </template>

<script>
import { ref, reactive, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Microphone, Mute, User, Loading, VideoCameraFilled, VideoCamera, PhoneFilled, ChatDotRound, Coin, Timer, ArrowRight, CircleCheckFilled, WarningFilled, Aim, TrophyBase } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getActiveCredit } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { normalizeAiText as fmtAi } from '@/utils/aiText'
import NaiwaPet from '@/components/NaiwaPet.vue'

export default {
  name: 'AiInterview',
  components: { Microphone, Mute, User, Loading, VideoCameraFilled, VideoCamera, PhoneFilled, ChatDotRound, Coin, Timer, ArrowRight, CircleCheckFilled, WarningFilled, Aim, TrophyBase, NaiwaPet },
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    const stage = ref('config')
    const starting = ref(false)
    const aiLoading = ref(false)
    const finishing = ref(false)
    const chatBodyRef = ref()
    const configRef = ref()
    const credits = ref(0)

    // ---- 视频模拟（仅本地预览，不上传） ----
    const videoRef = ref()
    const cameraOn = ref(false)
    const cameraHint = ref('开启视频，模拟真实面试场景')
    let cameraStream = null

    // 麦克风静音（模拟：当前为文字作答，不影响文本输入）
    const micMuted = ref(false)

    // ---- 异常降级与网络状态 ----
    const isOnline = ref(navigator.onLine)
    const degradedHint = ref('')
    const pollHint = ref('')
    const elapsedSeconds = ref(0)
    let elapsedTimer = null
    let pollFailures = 0
    let degradedTimer = null

    const AI_INTERVIEW_COST = 10

    const elapsedText = computed(() => {
      const m = String(Math.floor(elapsedSeconds.value / 60)).padStart(2, '0')
      const s = String(elapsedSeconds.value % 60).padStart(2, '0')
      return `${m}:${s}`
    })

    const startTimer = () => {
      stopTimer()
      elapsedSeconds.value = 0
      elapsedTimer = setInterval(() => { elapsedSeconds.value++ }, 1000)
    }
    const stopTimer = () => {
      if (elapsedTimer) { clearInterval(elapsedTimer); elapsedTimer = null }
    }

    const showDegraded = (flag, scene) => {
      if (!flag) return
      degradedHint.value = scene === 'start'
        ? '首题由备用题库生成（AI 响应超时），不影响继续面试'
        : '本轮 AI 响应超时或不可用，已使用备用点评/题目，不影响继续面试'
      if (degradedTimer) clearTimeout(degradedTimer)
      degradedTimer = setTimeout(() => { degradedHint.value = '' }, 10000)
    }

    const handleOnline = () => { isOnline.value = true; ElMessage.success('网络已恢复') }
    const handleOffline = () => { isOnline.value = false }

    const startCamera = async () => {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        cameraHint.value = '当前浏览器不支持摄像头，已使用模拟画面'
        ElMessage.warning('当前环境不支持摄像头，可继续文字面试')
        return
      }
      cameraHint.value = '正在请求摄像头权限…'
      let timedOut = false
      try {
        const streamPromise = navigator.mediaDevices.getUserMedia({ video: true, audio: false })
        // 权限弹窗长时间无响应时超时降级，避免界面卡在“正在请求权限”
        cameraStream = await Promise.race([
          streamPromise,
          new Promise((resolve, reject) => setTimeout(() => {
            timedOut = true
            reject(new Error('CAMERA_TIMEOUT'))
          }, 8000))
        ])
        // 超时后迟到的视频流立即释放，避免占用摄像头
        streamPromise.then(stream => {
          if (timedOut) stream.getTracks().forEach(track => track.stop())
        }).catch(() => {})
        cameraOn.value = true
        cameraHint.value = ''
        await nextTick()
        if (videoRef.value) {
          videoRef.value.srcObject = cameraStream
          videoRef.value.play().catch(() => {})
        }
        ElMessage.success('视频已开启（本地模拟预览）')
      } catch (e) {
        cameraOn.value = false
        if (e && e.message === 'CAMERA_TIMEOUT') {
          cameraHint.value = '摄像头响应超时，已切换为模拟画面，可继续文字面试'
        } else if (e && e.name === 'NotAllowedError') {
          cameraHint.value = '未获得摄像头权限，可继续文字面试'
        } else {
          cameraHint.value = '摄像头不可用，已切换为模拟画面'
        }
        ElMessage.warning(cameraHint.value)
      }
    }

    const stopCamera = () => {
      if (cameraStream) {
        cameraStream.getTracks().forEach(track => track.stop())
        cameraStream = null
      }
      if (videoRef.value) videoRef.value.srcObject = null
      cameraOn.value = false
      cameraHint.value = '开启视频，模拟真实面试场景'
    }

    const toggleCamera = () => {
      if (cameraOn.value) stopCamera()
      else startCamera()
    }

    const toggleMic = () => {
      micMuted.value = !micMuted.value
      ElMessage.info(micMuted.value ? '麦克风已静音（模拟）' : '麦克风已开启（模拟）')
    }

    const loadCredits = async () => {
      const userId = userStore.userInfo?.userId
      if (!userStore.token || !userId) return
      try {
        const res = await getActiveCredit(userId)
        credits.value = res.data?.activeCredits || 0
      } catch (e) {
        credits.value = 0
      }
    }
    onMounted(() => {
      loadCredits()
      window.addEventListener('online', handleOnline)
      window.addEventListener('offline', handleOffline)
    })

    const config = reactive({
      direction: 'java',
      difficulty: 'medium',
      maxRounds: 6,
      personalized: true
    })
    const rules = {
      direction: [{ required: true, message: '请选择技术方向', trigger: 'change' }]
    }

    let interviewId = null
    const messages = ref([])
    const round = ref(0)
    const maxRounds = ref(6)
    const answerText = ref('')
    const result = ref({})
    const reportFailed = ref(false)
    let pollTimer = null

    // ---- 通话模式：默认纯视频通话，开启后显示文字回答区 ----
    const textMode = ref(false)
    const answerInputRef = ref()
    const lastAiMessage = computed(() => {
      for (let i = messages.value.length - 1; i >= 0; i--) {
        if (messages.value[i].role === 'ai') return messages.value[i].content
      }
      return ''
    })
    const awaitingAnswer = computed(() => {
      const last = messages.value[messages.value.length - 1]
      return !!last && last.role === 'ai' && !aiLoading.value
    })
    const openTextMode = () => { textMode.value = true }
    // 展开后自动聚焦输入框并滚到最新消息，切换即时生效
    watch(textMode, (on) => {
      if (!on) return
      nextTick(() => {
        scrollBottom()
        if (answerInputRef.value) answerInputRef.value.focus()
      })
    })

    // ---- 桌宠动画状态：running 思考 / jumping 用户打字 / waiting 等待输入 / failed 回答较差 ----
    const petState = ref('idle')
    let petTimer = null
    let typingTimer = null
    const playPet = (state, holdMs = 0) => {
      if (petTimer) { clearTimeout(petTimer); petTimer = null }
      petState.value = state
      if (holdMs > 0) {
        petTimer = setTimeout(() => {
          petTimer = null
          petState.value = awaitingAnswer.value ? 'waiting' : 'idle'
        }, holdMs)
      }
    }
    // 用户打字时播放捧腹大笑，停止输入 1.2 秒后回到等待输入
    const handleTyping = (eventOrValue) => {
      if (aiLoading.value) return
      // el-input 的 @input 可能收到字符串（自定义事件）或原生 InputEvent（透传监听）
      let text = answerText.value
      if (typeof eventOrValue === 'string') {
        text = eventOrValue
      } else if (eventOrValue && eventOrValue.target && typeof eventOrValue.target.value === 'string') {
        text = eventOrValue.target.value
      }
      if (typingTimer) { clearTimeout(typingTimer); typingTimer = null }
      if (!text.trim()) {
        if (petState.value === 'jumping') {
          petState.value = awaitingAnswer.value ? 'waiting' : 'idle'
        }
        return
      }
      playPet('jumping')
      typingTimer = setTimeout(() => {
        typingTimer = null
        if (!aiLoading.value) {
          petState.value = awaitingAnswer.value ? 'waiting' : 'idle'
        }
      }, 1200)
    }
    // 输入事件通过 .text-panel 的原生 input 冒泡处理，兼容中文输入法组合输入
    const POOR_ANSWER_HINTS = ['不够', '不足', '欠缺', '不准确', '有误', '偏题', '遗漏', '没有提到', '答不上', '不完整', '比较基础', '不够深入', '片面', '需要加强', '有待提高', '答得比较空泛']
    const isPoorAnswer = (answerScore, comment) => {
      const hasScore = answerScore !== null && answerScore !== undefined && answerScore !== ''
      const score = hasScore ? Number(answerScore) : NaN
      if (Number.isFinite(score)) return score < 40
      const text = String(comment || '')
      return POOR_ANSWER_HINTS.some(k => text.includes(k))
    }

    const directionLabel = computed(() =>
      ({ java: 'Java', frontend: '前端', bigdata: '大数据', algorithm: '算法', devops: '运维', testing: '测试' }[config.direction] || config.direction)
    )
    const difficultyLabel = computed(() =>
      ({ easy: '简单', medium: '中等', hard: '困难' }[config.difficulty] || config.difficulty)
    )

    const scoreValue = computed(() => {
      const value = Number(result.value && result.value.score)
      return Number.isFinite(value) ? Math.max(0, Math.min(100, value)) : 0
    })
    const scoreOffset = computed(() => 326.73 * (1 - scoreValue.value / 100))
    const scoreTone = computed(() => (scoreValue.value >= 80 ? 'high' : scoreValue.value >= 60 ? 'mid' : 'low'))
    const qaScoreTone = (score) => {
      const value = Number(score) || 0
      return value >= 80 ? 'high' : value >= 60 ? 'mid' : 'low'
    }

    const scrollBottom = () => {
      nextTick(() => {
        if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
      })
    }

    // 积分不足：统一弹窗引导去充值，不再抛红色错误提示
    const promptRecharge = async (message) => {
      try {
        await ElMessageBox.confirm(
          message || `积分不足，开始一场 AI 面试需 ${AI_INTERVIEW_COST} 积分。前往充值页面获取积分后即可继续。`,
          '积分不足',
          { confirmButtonText: '去充值', cancelButtonText: '暂不', type: 'warning', autofocus: false }
        )
        router.push('/pay')
      } catch (e) { /* 用户取消 */ }
    }

    const handleStart = async () => {
      try {
        await configRef.value.validate()
      } catch (e) { return }

      // 扣费确认：每场面试消耗 10 积分
      try {
        await ElMessageBox.confirm(
          `开始一场 AI 面试将消耗 ${AI_INTERVIEW_COST} 积分（当前余额 ${credits.value}），确认开始？`,
          '面试计费确认',
          { confirmButtonText: '确认开始', cancelButtonText: '取消', type: 'warning' }
        )
      } catch (e) { return }

      starting.value = true
      try {
        const res = await request.post('/api/ai-interview/start', config, { timeout: 120000 })
        if (res.code === 200) {
          interviewId = res.data.interviewId
          messages.value = res.data.messages || []
          round.value = res.data.round
          maxRounds.value = res.data.maxRounds
          stage.value = 'chatting'
          credits.value = Math.max(0, credits.value - AI_INTERVIEW_COST)
          degradedHint.value = ''
          showDegraded(res.data.degraded, 'start')
          startTimer()
          scrollBottom()
          playPet('waiting')
        } else if (res.code === 4002) {
          promptRecharge(res.message)
        } else {
          ElMessage.error(res.message || '开始失败')
        }
      } catch (e) {
        if (e && e.businessCode === 4002) {
          promptRecharge(e.message)
        } else if (e && (e.code === 'ECONNABORTED' || String(e.message || '').includes('timeout'))) {
          ElMessage.warning('AI 出题超时，请检查网络后重试（若已扣积分，重新进入面试即可）')
        } else {
          ElMessage.error('网络异常，开始面试失败，请检查网络后重试')
        }
      } finally {
        starting.value = false
      }
    }

    const handleSend = async () => {
      const text = answerText.value.trim()
      if (!text || aiLoading.value) return
      messages.value.push({ role: 'user', content: text })
      answerText.value = ''
      scrollBottom()
      if (typingTimer) { clearTimeout(typingTimer); typingTimer = null }
      aiLoading.value = true
      playPet('running')
      try {
        const res = await request.post('/api/ai-interview/answer', {
          interviewId, userAnswer: text
        }, { timeout: 120000 })
        if (res.code === 200) {
          // 更新对话消息
          if (res.data.messages) messages.value = res.data.messages
          // 报告生成中：切到等待页，轮询
          if (res.data.reportPending || res.data.reportStatus === 'GENERATING') {
            stage.value = 'waiting'
            stopTimer()
            stopCamera()
            startPolling()
          } else if (res.data.ended && res.data.reportStatus === 'DONE') {
            result.value = res.data
            stage.value = 'finished'
            stopTimer()
            stopCamera()
          } else if (!res.data.ended) {
            round.value = res.data.round
            maxRounds.value = res.data.maxRounds
            showDegraded(res.data.degraded, 'turn')
            // 回答较差 → failed 3 秒；正常回复后直接进入等待输入（大笑只在用户打字时播放）
            if (isPoorAnswer(res.data.answerScore, res.data.comment)) {
              playPet('failed', 3000)
            } else {
              playPet('waiting')
            }
          }
          scrollBottom()
        } else {
          ElMessage.error(res.message || '提交失败')
          messages.value.pop()
          answerText.value = text
          playPet('waiting')
        }
      } catch (e) {
        // 网络异常/超时：回滚本轮回答，保留输入框内容，避免用户重复输入
        messages.value.pop()
        answerText.value = text
        playPet('waiting')
        if (e && (e.code === 'ECONNABORTED' || String(e.message || '').includes('timeout'))) {
          ElMessage.warning('AI 响应超时，回答已保留，请点击发送重试')
        } else {
          ElMessage.error('网络异常，回答已保留，请检查网络后重试')
        }
      } finally {
        aiLoading.value = false
      }
    }

    const startPolling = () => {
      stopPolling()
      pollFailures = 0
      pollHint.value = ''
      pollTimer = setInterval(async () => {
        try {
          const res = await request.get(`/api/ai-interview/report/${interviewId}`, { timeout: 15000 })
          if (res.code === 200) {
            pollFailures = 0
            pollHint.value = ''
            const d = res.data
            if (d.reportStatus === 'DONE') {
              stopPolling()
              result.value = d
              stage.value = 'finished'
              stopTimer()
              stopCamera()
              scrollBottom()
            } else if (d.reportStatus === 'FAILED') {
              stopPolling()
              reportFailed.value = true
            }
          }
        } catch (e) {
          // 轮询失败：保持重试，连续失败时给出友好提示，不打断页面
          pollFailures++
          if (pollFailures >= 5) {
            pollHint.value = '网络不稳定，报告获取较慢，正在自动重试…'
          }
        }
      }, 5000)
    }

    const stopPolling = () => {
      if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
    }

    const handleFinish = async () => {
      finishing.value = true
      try {
        const res = await request.post('/api/ai-interview/finish', { interviewId }, { timeout: 120000 })
        if (res.code === 200) {
          if (res.data.reportPending || res.data.reportStatus === 'GENERATING') {
            stage.value = 'waiting'
            stopTimer()
            stopCamera()
            startPolling()
          } else if (res.data.reportStatus === 'DONE') {
            result.value = res.data
            stage.value = 'finished'
            stopTimer()
            stopCamera()
          }
        } else {
          ElMessage.error(res.message || '结束失败')
        }
      } catch (e) {
        if (e && (e.code === 'ECONNABORTED' || String(e.message || '').includes('timeout'))) {
          ElMessage.warning('结束请求超时，请稍后重试；当前面试进度已保留')
        } else {
          ElMessage.error('网络异常，结束失败，请检查网络后重试')
        }
      } finally {
        finishing.value = false
      }
    }

    const handleRetryReport = () => {
      reportFailed.value = false
      // 重新触发 finish 接口（后端幂等/重入）
      handleFinish()
    }

    const handleHangup = async () => {
      try {
        await ElMessageBox.confirm(
          '确定结束本次面试吗？结束后将生成面试报告。',
          '结束面试',
          { confirmButtonText: '结束面试', cancelButtonText: '继续面试', type: 'warning', autofocus: false }
        )
      } catch (e) {
        return // 用户取消
      }
      handleFinish()
    }

    const resetAll = () => {
      stopPolling()
      stopTimer()
      stopCamera()
      if (degradedTimer) { clearTimeout(degradedTimer); degradedTimer = null }
      reportFailed.value = false
      pollHint.value = ''
      pollFailures = 0
      degradedHint.value = ''
      micMuted.value = false
      textMode.value = false
      if (petTimer) { clearTimeout(petTimer); petTimer = null }
      if (typingTimer) { clearTimeout(typingTimer); typingTimer = null }
      petState.value = 'idle'
      stage.value = 'config'
      messages.value = []
      result.value = {}
      interviewId = null
      answerText.value = ''
      elapsedSeconds.value = 0
    }

    const goLearningPath = () => router.push('/learning-path')

    onBeforeUnmount(() => {
      stopPolling()
      stopTimer()
      stopCamera()
      if (degradedTimer) { clearTimeout(degradedTimer); degradedTimer = null }
      if (petTimer) { clearTimeout(petTimer); petTimer = null }
      if (typingTimer) { clearTimeout(typingTimer); typingTimer = null }
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    })

    return {
      stage, config, rules, configRef, starting, aiLoading, finishing,
      messages, round, maxRounds, answerText, result, chatBodyRef, credits,
      reportFailed, pollHint,
      directionLabel, difficultyLabel,
      scoreValue, scoreOffset, scoreTone, qaScoreTone, fmtAi,
      isOnline, degradedHint, elapsedText,
      videoRef, cameraOn, cameraHint, micMuted, toggleCamera, toggleMic,
      textMode, answerInputRef, lastAiMessage, awaitingAnswer, openTextMode,
      petState, handleTyping,
      handleStart, handleSend, handleFinish, handleHangup, handleRetryReport, resetAll, goLearningPath
    }
  }
}
</script>

<style scoped>
.ai-interview {
  --ai-primary: #6c5ce7;
  --ai-primary-2: #8a7cf5;
  --ai-accent: #6ee7d8;
  --ai-warm: #ffb36b;
  --ai-ink: #2f2b45;
  --ai-muted: #7d7896;
  --ai-line: rgba(108, 92, 231, 0.12);
  --ai-card: rgba(255, 255, 255, 0.94);
  max-width: 1080px;
  margin: 0 auto;
  padding: 28px 20px 44px;
  min-height: 100vh;
  color: var(--ai-ink);
  background:
    radial-gradient(900px 520px at 8% -10%, rgba(138, 124, 245, 0.16), transparent 62%),
    radial-gradient(760px 480px at 100% 2%, rgba(110, 231, 216, 0.14), transparent 58%),
    #f7f6fc;
}
/* ---------- 开始页 ---------- */
.interview-config {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(0, 0.92fr);
  gap: 24px;
  align-items: stretch;
}
.config-hero {
  position: relative;
  overflow: hidden;
  min-height: 486px;
  padding: 34px;
  border-radius: 26px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  background:
    radial-gradient(420px 300px at 88% 18%, rgba(110, 231, 216, 0.22), transparent 70%),
    radial-gradient(380px 300px at 12% 92%, rgba(255, 159, 154, 0.16), transparent 70%),
    linear-gradient(158deg, #ffffff 0%, #f4f1ff 52%, #ecfbf8 100%);
  box-shadow: 0 26px 60px rgba(87, 70, 160, 0.13);
}
.config-hero::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: 25px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  pointer-events: none;
}
.config-hero-copy { position: relative; z-index: 2; max-width: 440px; }
.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.9);
  font-size: 12px;
  color: #6b6688;
  box-shadow: 0 4px 12px rgba(87, 70, 160, 0.06);
}
.hero-kicker .ai-badge { width: 26px; height: 26px; font-size: 10px; }
.config-hero h2 {
  margin: 20px 0 14px;
  font-size: clamp(28px, 3.4vw, 40px);
  line-height: 1.2;
  letter-spacing: -0.6px;
  color: var(--ai-ink);
}
.config-hero h2 em {
  font-style: normal;
  background: linear-gradient(100deg, #6c5ce7 0%, #18b7a0 70%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.config-hero-copy > p {
  margin: 0;
  max-width: 430px;
  font-size: 14px;
  line-height: 1.9;
  color: var(--ai-muted);
}
.hero-chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 20px; }
.hero-chip {
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: #5b5675;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--ai-line);
}
.hero-meta { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 24px; }
.credit-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #5b4dbb;
  background: rgba(138, 124, 245, 0.12);
}
.credit-pill .el-icon { font-size: 14px; }
.credit-pill.is-soft { color: #3f8f7f; background: rgba(110, 231, 216, 0.16); }
.hero-flow {
  position: absolute;
  left: 34px;
  bottom: 30px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 18px;
  font-size: 11px;
  color: #8b86a3;
}
.hero-flow span { display: inline-flex; align-items: center; gap: 6px; }
.hero-flow i {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-style: normal;
  font-size: 10px;
  color: #6c5ce7;
  background: rgba(138, 124, 245, 0.12);
}
.config-hero-pet {
  position: absolute;
  right: 28px;
  bottom: 24px;
  width: 150px;
  height: 162px;
  z-index: 1;
  opacity: 0.98;
}

.config-panel {
  display: flex;
  flex-direction: column;
  padding: 30px;
  border-radius: 26px;
  background: var(--ai-card);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 26px 60px rgba(87, 70, 160, 0.11);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}
.panel-kicker {
  display: block;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #8a7cf5;
}
.panel-head h3 { margin: 5px 0 0; font-size: 22px; letter-spacing: -0.3px; }
.panel-note {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 9px;
  border-radius: 999px;
  white-space: nowrap;
  font-size: 11px;
  color: var(--ai-muted);
  background: #f4f2fb;
}
.config-form { flex: 1; }
.config-form :deep(.el-form-item) { margin-bottom: 18px; }
.config-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #4b4663;
}
.config-form :deep(.el-input__wrapper),
.config-form :deep(.el-select__wrapper) {
  min-height: 42px;
  border-radius: 12px;
  box-shadow: 0 0 0 1px var(--ai-line) inset;
  background: #fbfaff;
}
.config-form :deep(.el-input__wrapper.is-focus),
.config-form :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #8a7cf5 inset, 0 6px 18px rgba(108, 92, 231, 0.12);
}
.segmented { display: flex; flex-wrap: wrap; gap: 8px; }
.segmented :deep(.el-radio-button__inner) {
  min-width: 58px;
  padding: 8px 12px;
  border: 1px solid var(--ai-line) !important;
  border-radius: 10px !important;
  box-shadow: none !important;
  color: #5f5a78;
  font-size: 13px;
  background: #fbfaff;
}
.segmented :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-left: 1px solid var(--ai-line) !important;
}
.segmented :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  color: #fff;
  border-color: transparent !important;
  background: linear-gradient(135deg, #8a7cf5, #6c5ce7);
  box-shadow: 0 8px 18px rgba(108, 92, 231, 0.24) !important;
}
.switch-row { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.form-tip { color: var(--ai-muted); font-size: 12px; line-height: 1.6; }
.video-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 2px 0 18px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f7f6fd;
  color: #85809d;
  font-size: 12px;
  line-height: 1.7;
}
.video-tip .el-icon { margin-top: 2px; color: #8a7cf5; }
.config-summary {
  margin-top: auto;
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f7f4ff, #f0fcf9);
  border: 1px solid rgba(108, 92, 231, 0.08);
}
.summary-label { display: block; font-size: 11px; color: #8b86a3; margin-bottom: 8px; }
.summary-tags { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.summary-tags span { font-size: 13px; font-weight: 600; color: #4f4a6b; }
.summary-tags i { width: 3px; height: 3px; border-radius: 50%; background: #c9c3e3; }
.config-actions { display: flex; align-items: center; gap: 10px; margin-top: 18px; }
.start-btn {
  height: 46px;
  padding: 0 22px;
  border: none;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #8a7cf5 0%, #6c5ce7 100%);
  box-shadow: 0 14px 28px rgba(108, 92, 231, 0.28);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.start-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 18px 34px rgba(108, 92, 231, 0.34);
}
.start-btn .start-arrow { margin-left: 6px; transition: transform 0.18s ease; }
.start-btn:hover .start-arrow { transform: translateX(3px); }
.config-actions .start-btn { flex: 1; }
.ghost-btn {
  height: 46px;
  padding: 0 16px;
  border-radius: 14px;
  color: #6b6684;
  background: transparent;
}
.ghost-btn:hover { color: #4b4568; background: #f4f2fb; }

/* 视频通话台：奶蛙桌宠作为面试官形象，纯通话时占满文字区收起后的空间 */
.call-stage {
  position: relative;
  flex: 1 1 auto;
  min-height: 200px;
  overflow: hidden;
  background: linear-gradient(180deg, #f6f4ff 0%, #e9e6fb 100%);
}
.ai-orb-caption {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  color: #8d8aa8;
  letter-spacing: 0.3px;
}
/* 桌宠定位；动画与物理交互见 components/NaiwaPet.vue
   素材：https://github.com/timerring/codex-pet-naiwa（MIT）；Credits: LynnShaw/naiwa-pet */
.ai-pet-wrap {
  position: absolute;
  left: 50%;
  top: 46%;
  width: 176px;
  height: 190px;
  transform: translate(-50%, -50%);
  pointer-events: auto;
}
/* 纯视频模式空间更大，桌宠略上移，为底部问题字幕让位 */
.call-stage:not(.text-open) .ai-pet-wrap { top: 38%; }
.user-cam {
  position: absolute;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  width: 104px;
  height: 140px;
  border-radius: 10px;
  overflow: hidden;
  background: #2a2a33;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 24px rgba(70, 60, 120, 0.18);
  z-index: 2;
}
.cam-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
}
.cam-off {
  height: 100%;
  padding: 0 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #9aa0b4;
  text-align: center;
  font-size: 10px;
}
.cam-off .el-icon { font-size: 22px; }
.cam-label {
  position: absolute;
  left: 6px;
  bottom: 6px;
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 10px;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
}
.call-timer {
  position: absolute;
  left: 50%;
  bottom: 76px;
  transform: translateX(-50%);
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #8a8fa3;
  letter-spacing: 1px;
  background: rgba(246, 244, 255, 0.72);
}
.call-subtitle {
  position: absolute;
  left: 50%;
  bottom: 112px;
  transform: translateX(-50%);
  z-index: 3;
  width: min(520px, calc(100% - 180px));
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 10px 26px rgba(70, 60, 120, 0.12);
  backdrop-filter: blur(6px);
}
.subtitle-head { display: flex; align-items: center; gap: 8px; }
.subtitle-tag {
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 10px;
  color: #7c6cf0;
  background: rgba(138, 124, 245, 0.12);
}
.subtitle-status { font-size: 10px; color: #9aa0b4; }
.subtitle-text {
  margin: 6px 0 0;
  max-height: 96px;
  overflow-y: auto;
  font-size: 13px;
  line-height: 1.7;
  color: #4a4f63;
  white-space: pre-wrap;
  word-break: break-word;
}
.subtitle-action {
  margin-top: 8px;
  border: 0;
  padding: 5px 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #fff;
  background: linear-gradient(135deg, #8a7cf5, #6ee7d8);
  cursor: pointer;
}
.subtitle-action:hover { filter: brightness(1.05); }
.subtitle-fade-enter-active, .subtitle-fade-leave-active { transition: opacity 0.18s ease; }
.subtitle-fade-enter-from, .subtitle-fade-leave-to { opacity: 0; }
.call-controls {
  position: absolute;
  left: 50%;
  bottom: 16px;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 20px;
}
.ctrl-btn {
  width: 46px;
  height: 46px;
  border: 0;
  padding: 0;
  border-radius: 50%;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: #4a4f63;
  font-size: 20px;
  box-shadow: 0 6px 16px rgba(70, 60, 120, 0.14);
  transition: transform 0.16s ease, background 0.16s ease, color 0.16s ease;
}
.ctrl-btn:hover { transform: translateY(-2px); }
.ctrl-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.ctrl-btn.ctrl-muted { background: #f0f0f5; color: #909399; }
.ctrl-btn.ctrl-active { background: #e8f7ee; color: #2f9e5f; }
.ctrl-btn.ctrl-hangup {
  background: #ff4d4f;
  color: #fff;
  box-shadow: 0 8px 20px rgba(255, 77, 79, 0.38);
}
.ctrl-btn.ctrl-hangup .el-icon { transform: rotate(135deg); }
.degraded-alert { margin: 8px 16px 0; border-radius: 8px; }

@media (max-width: 640px) {
  .ai-pet-wrap { width: 138px; height: 150px; }
  .user-cam { width: 86px; height: 116px; right: 14px; }
  .call-timer { bottom: 68px; }
  .call-controls { bottom: 12px; gap: 16px; }
  .ctrl-btn { width: 42px; height: 42px; font-size: 18px; }
  .call-subtitle { left: 16px; width: calc(100% - 150px); bottom: 104px; transform: none; }
  .subtitle-text { max-height: 66px; }
  .call-live { display: none; }
  .chat-header { padding: 12px 14px; }
}
/* 矮屏时为底部问题字幕留出空间 */
@media (max-height: 720px) {
  .call-stage:not(.text-open) .ai-pet-wrap { top: 30%; }
}

/* 对话区 */
.interview-chat {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  overflow: hidden;
  height: calc(100vh - 120px);
  min-height: 540px;
  display: flex;
  flex-direction: column;
}
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: #f6f4ff;
  border-bottom: 0;
  flex-shrink: 0;
}
.chat-title { display: flex; align-items: center; gap: 12px; }
.chat-title h3 { margin: 0; font-size: 16px; }
.chat-sub { color: #909399; font-size: 12px; }
.ai-badge {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #8a7cf5, #6ee7d8);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}
.header-actions { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.mode-switch {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  border: 1px solid #ece9fb;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
}
.mode-icon { font-size: 14px; color: #8a7cf5; }
.mode-label { font-size: 12px; color: #6b6880; white-space: nowrap; }
.mode-switch .el-switch { --el-switch-on-color: #8a7cf5; }
.call-live {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px;
  border-radius: 999px;
  color: #52a35f;
  background: rgba(103, 194, 58, 0.12);
  font-size: 11px;
}
.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #67c23a;
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.7);
}
/* 文字回答面板：高度过渡展开，通话台通过 flex 平滑让位 */
.text-panel {
  flex: 0 0 auto;
  height: 0;
  opacity: 0;
  overflow: hidden;
  background: #fff;
  border-top: 1px solid transparent;
  box-sizing: border-box;
  pointer-events: none;
  transition: height 0.3s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.18s ease;
}
.text-panel.open {
  height: min(300px, 42vh);
  opacity: 1;
  border-top-color: #f0f0f0;
  pointer-events: auto;
}
.text-panel-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.chat-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
  background: #f8f9fb;
}
.msg-row {
  display: flex;
  margin-bottom: 16px;
  gap: 10px;
}
.msg-row.user { flex-direction: row-reverse; }
.msg-avatar {
  width: 28px; height: 28px;
  border-radius: 50%;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}
.msg-avatar.ai { background: #409eff; color: #fff; }
.msg-avatar.user { background: #67c23a; color: #fff; }
.msg-bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-bubble.ai { background: #fff; border: 1px solid #ebeef5; }
.msg-bubble.user { background: #409eff; color: #fff; }
.msg-bubble.typing { color: #909399; }
.chat-input {
  border-top: 1px solid #f0f0f0;
  padding: 12px 16px;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.chat-input .el-textarea { flex: 1; }
.input-actions { flex-shrink: 0; }

/* 报告生成等待页 */
.interview-waiting { padding: 48px 20px; }
.waiting-card {
  max-width: 520px;
  margin: 0 auto;
  padding: 36px 30px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  text-align: center;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 26px 60px rgba(87, 70, 160, 0.12);
}
.waiting-icon { font-size: 42px; color: #8a7cf5; animation: rotating 1.5s linear infinite; }
@keyframes rotating { from { transform: rotate(0); } to { transform: rotate(360deg); } }
.waiting-card h3 { margin: 16px 0 8px; font-size: 20px; }
.waiting-card p { color: #6f6a86; font-size: 13px; margin: 0 0 6px; }
.waiting-tip { color: #9b96ae !important; font-size: 12px !important; }
.waiting-card .poll-alert { margin: 16px 0 0; }

/* 结果页 */
.interview-result { padding: 4px 0 0; }
.result-hero {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr) 160px;
  align-items: center;
  gap: 24px;
  padding: 28px 30px;
  border-radius: 26px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  background:
    radial-gradient(420px 260px at 92% 20%, rgba(110, 231, 216, 0.22), transparent 70%),
    radial-gradient(360px 260px at 8% 100%, rgba(255, 159, 154, 0.16), transparent 70%),
    linear-gradient(150deg, #ffffff 0%, #f4f1ff 55%, #eefcf9 100%);
  box-shadow: 0 26px 60px rgba(87, 70, 160, 0.12);
}
.result-score {
  position: relative;
  width: 132px;
  height: 132px;
  display: grid;
  place-items: center;
}
.score-ring { width: 132px; height: 132px; transform: rotate(-90deg); }
.score-ring-bg { fill: none; stroke: rgba(108, 92, 231, 0.12); stroke-width: 10; }
.score-ring-value {
  fill: none;
  stroke-width: 10;
  stroke-linecap: round;
  stroke-dasharray: 326.73;
  animation: score-draw 1.1s cubic-bezier(0.22, 0.61, 0.36, 1) forwards;
  filter: drop-shadow(0 4px 8px rgba(108, 92, 231, 0.18));
}
@keyframes score-draw {
  from { stroke-dashoffset: 326.73px; }
  to { stroke-dashoffset: var(--score-offset); }
}
.score-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
}
.score-center strong { font-size: 42px; line-height: 1; letter-spacing: -2px; color: #2f2b45; }
.score-center span { font-size: 11px; color: #8b86a3; }
.result-hero.tone-high .score-center strong { color: #1f9d74; }
.result-hero.tone-mid .score-center strong { color: #6c5ce7; }
.result-hero.tone-low .score-center strong { color: #d9695d; }
.result-hero-copy { min-width: 0; }
.result-kicker {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.6px;
  color: #8a7cf5;
}
.result-kicker .el-icon { font-size: 14px; }
.result-hero h2 { margin: 8px 0 10px; font-size: clamp(24px, 2.6vw, 32px); letter-spacing: -0.5px; }
.result-summary-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: #6f6a86;
  white-space: pre-wrap;
  word-break: break-word;
}
.result-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 16px; font-size: 11px; color: #6f6a86; }
.result-meta span { padding: 4px 9px; border-radius: 999px; background: rgba(255, 255, 255, 0.7); border: 1px solid var(--ai-line); }
.result-meta i { width: 3px; height: 3px; border-radius: 50%; background: #c9c3e3; }
.result-hero-pet {
  width: 160px;
  height: 160px;
  justify-self: end;
  display: flex;
  align-items: center;
  justify-content: center;
}
.result-laugh-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  pointer-events: none;
  border-radius: 18px;
  mix-blend-mode: multiply;
}

.result-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; margin-top: 20px; }
.result-grid.is-single { grid-template-columns: minmax(0, 1fr); }
.insight-card {
  padding: 22px 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(108, 92, 231, 0.08);
  box-shadow: 0 16px 36px rgba(87, 70, 160, 0.07);
}
.insight-head { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.insight-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(108, 92, 231, 0.1);
  color: #6c5ce7;
  font-size: 18px;
}
.strengths .insight-icon { background: rgba(63, 191, 127, 0.12); color: #2f9e68; }
.improvements .insight-icon { background: rgba(255, 179, 107, 0.16); color: #d18a3a; }
.insight-head h3 { margin: 0; font-size: 15px; }
.insight-head p { margin: 3px 0 0; font-size: 11px; color: var(--ai-muted); }
.insight-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.insight-list li {
  display: flex;
  gap: 9px;
  align-items: flex-start;
  font-size: 13px;
  line-height: 1.75;
  color: #5f5a78;
  white-space: pre-wrap;
  word-break: break-word;
}
.insight-list .dot { width: 5px; height: 5px; margin-top: 9px; border-radius: 50%; flex-shrink: 0; background: #8a7cf5; }
.strengths .dot { background: #3fbf7f; }
.improvements .dot { background: #f0a23b; }

.diagnosis-card { margin-top: 18px; }
.diag-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 14px 18px; }
.diag-top { display: flex; align-items: center; justify-content: space-between; gap: 10px; font-size: 12px; color: #5f5a78; }
.diag-top strong { font-size: 12px; }
.diag-top strong.high { color: #2f9e68; }
.diag-top strong.mid { color: #d18a3a; }
.diag-top strong.low { color: #d9695d; }
.diag-track { height: 6px; margin-top: 7px; border-radius: 999px; background: #f0eef8; overflow: hidden; }
.diag-track i { display: block; height: 100%; border-radius: 999px; background: linear-gradient(90deg, #8a7cf5, #6ee7d8); transition: width 0.4s ease; }
.diag-track i.high { background: linear-gradient(90deg, #3fbf7f, #6ee7d8); }
.diag-track i.mid { background: linear-gradient(90deg, #f0a23b, #8a7cf5); }
.diag-track i.low { background: linear-gradient(90deg, #ff8a80, #ffb36b); }

.qa-section { margin-top: 18px; }
.qa-head { position: relative; }
.qa-count { margin-left: auto; padding: 4px 10px; border-radius: 999px; font-size: 11px; color: #6c5ce7; background: rgba(138, 124, 245, 0.1); }
.qa-list { display: flex; flex-direction: column; gap: 14px; }
.qa-card { padding: 18px; border-radius: 16px; background: #fbfaff; border: 1px solid rgba(108, 92, 231, 0.1); }
.qa-top { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 12px; }
.qa-index { font-size: 12px; font-weight: 700; letter-spacing: 0.6px; color: #6c5ce7; }
.qa-score { padding: 3px 9px; border-radius: 999px; font-size: 11px; font-weight: 600; }
.qa-score.high { color: #2f9e68; background: rgba(63, 191, 127, 0.12); }
.qa-score.mid { color: #c47f2c; background: rgba(240, 162, 59, 0.14); }
.qa-score.low { color: #c65b50; background: rgba(255, 138, 128, 0.16); }
.qa-line { display: grid; grid-template-columns: 64px minmax(0, 1fr); gap: 10px; padding: 9px 0; border-top: 1px dashed rgba(108, 92, 231, 0.12); }
.qa-line:first-of-type { border-top: 0; }
.qa-label { font-size: 11px; font-weight: 600; color: #8b86a3; padding-top: 2px; }
.qa-line p { margin: 0; font-size: 13px; line-height: 1.8; color: #5f5a78; white-space: pre-wrap; word-break: break-word; }
.qa-line.answer .qa-label { color: #d18a3a; }
.qa-line.answer p { color: #6b5a4a; }
.qa-line.ideal { background: linear-gradient(90deg, rgba(63, 191, 127, 0.08), rgba(110, 231, 216, 0.06)); border-radius: 10px; padding: 10px 12px; margin-top: 4px; }
.qa-line.ideal .qa-label { color: #2f9e68; }
.qa-line.ideal p { color: #3f5f52; }
.result-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 22px; }
.result-actions .el-button + .el-button { margin-left: 0; }
.result-actions .start-btn { flex: 0 0 auto; }

/* 响应式 */
@media (max-width: 980px) {
  .interview-config { grid-template-columns: 1fr; }
  .config-hero { min-height: 360px; }
}
@media (max-width: 760px) {
  .ai-interview { padding: 18px 14px 32px; }
  .config-hero { padding: 24px; border-radius: 22px; min-height: 0; }
  .config-hero-pet { display: none; }
  .hero-flow { position: static; margin-top: 18px; }
  .config-panel { padding: 22px; border-radius: 22px; }
  .config-hero h2 { font-size: 28px; }
  .config-actions { flex-wrap: wrap; }
  .config-actions .start-btn { width: 100%; flex: 0 0 100%; }
  .result-hero { grid-template-columns: 1fr; gap: 18px; padding: 24px; text-align: center; }
  .result-score { margin: 0 auto; order: 1; }
  .result-hero-pet { order: 2; width: 150px; height: 150px; margin: 0 auto; justify-self: center; }
  .result-hero-copy { order: 3; text-align: center; }
  .result-meta { justify-content: center; }
  .result-grid { grid-template-columns: 1fr; }
  .insight-card { padding: 18px; }
  .qa-line { grid-template-columns: 1fr; gap: 4px; }
  .qa-line .qa-label { padding-top: 0; }
  .result-actions { flex-direction: column; }
  .result-actions .el-button { width: 100%; margin-left: 0; }
}
</style>
