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
      <div class="config-header">
        <h2>AI 面试官</h2>
        <p>像真实面试一样：AI 实时提问，根据你的回答逐轮追问</p>
        <div class="free-badge">
          <el-tag type="warning" effect="dark" size="small">每场消耗 10 积分</el-tag>
          <el-tag type="info" size="small">当前积分：{{ credits }}</el-tag>
        </div>
        <p class="video-tip">面试页支持开启摄像头做视频模拟预览（仅本地展示，不会上传）</p>
      </div>
      <el-card class="config-card">
        <el-form :model="config" :rules="rules" ref="configRef" label-width="110px">
          <el-form-item label="技术方向" prop="direction">
            <el-select v-model="config.direction" placeholder="选择技术方向">
              <el-option label="Java" value="java" />
              <el-option label="前端" value="frontend" />
              <el-option label="大数据" value="bigdata" />
              <el-option label="算法" value="algorithm" />
              <el-option label="运维" value="devops" />
              <el-option label="测试" value="testing" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度" prop="difficulty">
            <el-radio-group v-model="config.difficulty">
              <el-radio value="easy">简单</el-radio>
              <el-radio value="medium">中等</el-radio>
              <el-radio value="hard">困难</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="面试轮数" prop="maxRounds">
            <el-radio-group v-model="config.maxRounds">
              <el-radio-button :value="3">3 轮</el-radio-button>
              <el-radio-button :value="4">4 轮</el-radio-button>
              <el-radio-button :value="6">6 轮</el-radio-button>
              <el-radio-button :value="8">8 轮</el-radio-button>
            </el-radio-group>
            <span class="form-tip">AI 可自主结束</span>
          </el-form-item>
          <el-form-item label="个性化">
            <el-switch v-model="config.personalized" active-text="开" inactive-text="关" />
            <span class="form-tip">结合你的答题诊断，优先考察薄弱知识点</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="starting" @click="handleStart">开始面试</el-button>
            <el-button @click="$router.go(-1)">返回</el-button>
          </el-form-item>
        </el-form>
      </el-card>
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
        <span class="call-live"><i class="live-dot"></i>面试中</span>
      </div>

      <!-- BOSS 直聘式语音面试间：面试官不使用视频，以动态光晕模拟；用户摄像头为右侧竖屏小窗 -->
      <div class="call-stage">
        <span class="ai-orb-caption">{{ aiLoading ? 'AI 面试官正在提问…' : 'AI 面试官在线' }}</span>
        <div class="ai-orb-wrap" :class="{ speaking: aiLoading }" aria-label="AI 面试官（模拟形象）">
          <svg class="ai-orb" viewBox="0 0 200 200" aria-hidden="true">
            <defs>
              <linearGradient id="aiOrbGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#6ee7d8" />
                <stop offset="45%" stop-color="#8a7cf5" />
                <stop offset="100%" stop-color="#ff9f9a" />
              </linearGradient>
              <filter id="aiOrbBlur" x="-50%" y="-50%" width="200%" height="200%">
                <feGaussianBlur stdDeviation="9" />
              </filter>
            </defs>
            <path
              d="M100 8 C112 66 134 88 192 100 C134 112 112 134 100 192 C88 134 66 112 8 100 C66 88 88 66 100 8Z"
              fill="url(#aiOrbGradient)"
              filter="url(#aiOrbBlur)"
            />
          </svg>
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

    <!-- 报告生成中 -->
    <div v-else-if="stage === 'waiting'" class="interview-waiting">
      <el-card class="waiting-card">
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
      </el-card>
    </div>

    <!-- 面试结果页 -->
    <div v-else-if="stage === 'finished'" class="interview-result">
      <el-card class="result-card">
        <template #header><span>面试结果</span></template>
        <el-alert
          v-if="result.degraded"
          title="本次报告使用了降级评分"
          description="部分 AI 请求超时或不可用，分数与建议仅供参考，建议重新面试以获取完整评价"
          type="warning"
          :closable="false"
          show-icon
          class="degraded-report"
        />
        <div class="score-big">
          <span class="score-num">{{ result.score }}</span>
          <span class="score-unit">/ 100</span>
        </div>
        <div class="result-summary">{{ result.summary }}</div>
        <div class="result-section" v-if="result.strengths && result.strengths.length">
          <h4>优势表现</h4>
          <ul><li v-for="(s, i) in result.strengths" :key="i">{{ s }}</li></ul>
        </div>
        <div class="result-section" v-if="result.improvements && result.improvements.length">
          <h4>改进建议</h4>
          <ul><li v-for="(s, i) in result.improvements" :key="i">{{ s }}</li></ul>
        </div>
        <div class="result-section" v-if="result.diagnosis && result.diagnosis.length">
          <h4>薄弱知识点（联动答题诊断）</h4>
          <el-tag
            v-for="d in result.diagnosis"
            :key="d.categoryName"
            size="small"
            :type="d.correctRate < 50 ? 'danger' : (d.correctRate < 80 ? 'warning' : 'success')"
            class="diag-tag"
          >{{ d.categoryName }} {{ d.correctRate }}%</el-tag>
        </div>

        <!-- 题目回顾：每题你的回答 + 参考答案 -->
        <div class="result-section" v-if="result.qaReview && result.qaReview.length">
          <h4>题目回顾与参考答案</h4>
          <div class="qa-review" v-for="(qa, i) in result.qaReview" :key="i">
            <div class="qa-header">
              <span class="qa-index">第 {{ i + 1 }} 题</span>
              <el-tag size="small" :type="qa.score >= 80 ? 'success' : (qa.score >= 60 ? 'warning' : 'danger')">
                单题 {{ qa.score }} 分
              </el-tag>
            </div>
            <div class="qa-block"><span class="qa-label">题目：</span>{{ qa.question }}</div>
            <div class="qa-block"><span class="qa-label">你的回答：</span>{{ qa.userAnswer || '（未作答）' }}</div>
            <div class="qa-block ideal"><span class="qa-label">参考答案：</span>{{ qa.idealAnswer || '（暂无）' }}</div>
          </div>
        </div>

        <div class="result-actions">
          <el-button type="primary" @click="resetAll">再面一次</el-button>
          <el-button @click="goLearningPath">生成学习路径</el-button>
          <el-button @click="$router.push('/interview-questions')">查看真题</el-button>
        </div>
      </el-card>
    </div>
  </div>
  <BackHome />
  </template>

<script>
import { ref, reactive, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Microphone, Mute, User, Loading, VideoCameraFilled, VideoCamera, PhoneFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getActiveCredit } from '@/api/user'
import { useUserStore } from '@/stores/user'

export default {
  name: 'AiInterview',
  components: { Microphone, Mute, User, Loading, VideoCameraFilled, VideoCamera, PhoneFilled },
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

    const directionLabel = computed(() =>
      ({ java: 'Java', frontend: '前端', bigdata: '大数据', algorithm: '算法', devops: '运维', testing: '测试' }[config.direction] || config.direction)
    )
    const difficultyLabel = computed(() =>
      ({ easy: '简单', medium: '中等', hard: '困难' }[config.difficulty] || config.difficulty)
    )

    const scrollBottom = () => {
      nextTick(() => {
        if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
      })
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
        } else if (res.code === 4002) {
          // 积分不足：引导购买邀请码获取积分
          try {
            await ElMessageBox.confirm(
              res.message || `积分不足，开始一场 AI 面试需 ${AI_INTERVIEW_COST} 积分`,
              '积分不足',
              { confirmButtonText: '去获取积分', cancelButtonText: '取消', type: 'warning' }
            )
            router.push('/pay')
          } catch (e) { /* 用户取消 */ }
        } else {
          ElMessage.error(res.message || '开始失败')
        }
      } catch (e) {
        if (e && (e.code === 'ECONNABORTED' || String(e.message || '').includes('timeout'))) {
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
      aiLoading.value = true
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
          }
          scrollBottom()
        } else {
          ElMessage.error(res.message || '提交失败')
          messages.value.pop()
          answerText.value = text
        }
      } catch (e) {
        // 网络异常/超时：回滚本轮回答，保留输入框内容，避免用户重复输入
        messages.value.pop()
        answerText.value = text
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
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    })

    return {
      stage, config, rules, configRef, starting, aiLoading, finishing,
      messages, round, maxRounds, answerText, result, chatBodyRef, credits,
      reportFailed, pollHint,
      directionLabel, difficultyLabel,
      isOnline, degradedHint, elapsedText,
      videoRef, cameraOn, cameraHint, micMuted, toggleCamera, toggleMic,
      handleStart, handleSend, handleFinish, handleHangup, handleRetryReport, resetAll, goLearningPath
    }
  }
}
</script>

<style scoped>
.ai-interview {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  background: #f5f7fa;
}
.config-header {
  text-align: center;
  margin-bottom: 20px;
}
.config-header h2 { margin: 0 0 8px; }
.config-header p { color: #909399; font-size: 14px; margin: 0; }
.free-badge { margin-top: 10px; display: flex; gap: 8px; justify-content: center; }
.config-card { border-radius: 10px; }
.form-tip { margin-left: 10px; color: #909399; font-size: 12px; }
.video-tip { margin: 8px 0 0; color: #909399; font-size: 12px; }
.offline-banner { margin-bottom: 12px; border-radius: 8px; }
.poll-alert { margin: 12px 40px 0; border-radius: 8px; }
.degraded-report { margin-bottom: 12px; border-radius: 8px; }

/* BOSS 直聘式语音面试间：面试官无视频，用动态光晕模拟 */
.call-stage {
  position: relative;
  height: 280px;
  flex-shrink: 0;
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
.ai-orb-wrap {
  position: absolute;
  left: 50%;
  top: 46%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.ai-orb {
  width: 190px;
  height: 190px;
  animation: orb-breathe 4.5s ease-in-out infinite;
  will-change: transform;
}
.ai-orb-wrap.speaking .ai-orb { animation: orb-speak 1.4s ease-in-out infinite; }
@keyframes orb-breathe {
  0%, 100% { transform: scale(1) rotate(0deg); }
  50% { transform: scale(1.05) rotate(5deg); }
}
@keyframes orb-speak {
  0%, 100% { transform: scale(0.98) rotate(0deg); }
  50% { transform: scale(1.1) rotate(8deg); }
}
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
  .call-stage { height: 240px; }
  .ai-orb { width: 150px; height: 150px; }
  .user-cam { width: 86px; height: 116px; right: 14px; }
  .call-timer { bottom: 68px; }
  .call-controls { bottom: 12px; gap: 16px; }
  .ctrl-btn { width: 42px; height: 42px; font-size: 18px; }
}

/* 对话区 */
.interview-chat {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  overflow: hidden;
  height: calc(100vh - 120px);
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
.chat-body {
  flex: 1;
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
.interview-waiting { padding: 60px 20px; }
.waiting-card { border-radius: 12px; text-align: center; padding: 20px 0; }
.waiting-icon { font-size: 48px; color: #409eff; animation: rotating 1.5s linear infinite; }
@keyframes rotating { from { transform: rotate(0); } to { transform: rotate(360deg); } }
.waiting-card h3 { margin: 16px 0 8px; }
.waiting-card p { color: #606266; font-size: 14px; margin: 0 0 6px; }
.waiting-tip { color: #909399 !important; font-size: 12px !important; }

/* 结果页 */
.interview-result { padding: 20px 0; }
.result-card { border-radius: 12px; }
.score-big { text-align: center; padding: 20px 0 10px; }
.score-num { font-size: 56px; font-weight: 700; color: #409eff; }
.score-unit { color: #909399; font-size: 16px; }
.result-summary {
  color: #303133; font-size: 14px; line-height: 1.8;
  background: #f5f7fa; border-radius: 8px; padding: 12px 16px;
  margin: 12px 0 20px;
}
.result-section { margin-bottom: 16px; }
.result-section h4 { margin: 0 0 8px; font-size: 14px; color: #606266; }
.result-section ul { margin: 0; padding-left: 20px; color: #606266; font-size: 14px; line-height: 2; }
.diag-tag { margin: 0 8px 8px 0; }
.result-actions { display: flex; gap: 12px; margin-top: 20px; }

/* 题目回顾 */
.qa-review {
  border: 1px solid #ebeef5; border-radius: 8px;
  padding: 12px 14px; margin-bottom: 12px; background: #fafbfc;
}
.qa-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.qa-index { font-weight: 600; font-size: 14px; color: #303133; }
.qa-block { font-size: 13px; line-height: 1.8; color: #606266; margin-bottom: 4px; }
.qa-block .qa-label { font-weight: 600; color: #303133; }
.qa-block.ideal { background: #f0f9eb; border-radius: 6px; padding: 8px 10px; margin-top: 6px; }
</style>
