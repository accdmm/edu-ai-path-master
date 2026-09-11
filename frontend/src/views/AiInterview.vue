<template>
  <div class="ai-interview">
    <!-- 配置页 -->
    <div v-if="stage === 'config'" class="interview-config">
      <div class="config-header">
        <h2>AI 面试官</h2>
        <p>像真实面试一样：AI 实时提问，根据你的回答逐轮追问</p>
        <div class="free-badge">
          <el-tag type="warning" effect="dark" size="small">每场消耗 10 积分</el-tag>
          <el-tag type="info" size="small">当前积分：{{ credits }}</el-tag>
        </div>
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
            <el-input-number v-model="config.maxRounds" :min="3" :max="12" />
            <span class="form-tip">轮（AI 可自主结束）</span>
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
          <el-icon class="chat-avatar"><Microphone /></el-icon>
          <div>
            <h3>AI 面试官</h3>
            <span class="chat-sub">第 {{ round }} / {{ maxRounds }} 轮 · {{ directionLabel }} · {{ difficultyLabel }}</span>
          </div>
        </div>
        <el-button size="small" type="danger" plain :loading="finishing" @click="handleFinish">结束面试</el-button>
      </div>

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
        <el-button v-if="reportFailed" type="warning" @click="handleRetryReport">重新生成报告</el-button>
      </el-card>
    </div>

    <!-- 面试结果页 -->
    <div v-else-if="stage === 'finished'" class="interview-result">
      <el-card class="result-card">
        <template #header><span>面试结果</span></template>
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
</template>

<script>
import { ref, reactive, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Microphone, User, Loading } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getActiveCredit } from '@/api/user'
import { useUserStore } from '@/stores/user'

export default {
  name: 'AiInterview',
  components: { Microphone, User, Loading },
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

    const AI_INTERVIEW_COST = 10

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
    onMounted(loadCredits)

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

    const directionLabel = { java: 'Java', frontend: '前端', bigdata: '大数据', algorithm: '算法', devops: '运维', testing: '测试' }[config.direction] || config.direction
    const difficultyLabel = { easy: '简单', medium: '中等', hard: '困难' }[config.difficulty] || config.difficulty

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
        ElMessage.error('开始失败，请重试')
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
            startPolling()
          } else if (res.data.ended && res.data.reportStatus === 'DONE') {
            result.value = res.data
            stage.value = 'finished'
          } else if (!res.data.ended) {
            round.value = res.data.round
            maxRounds.value = res.data.maxRounds
          }
          scrollBottom()
        } else {
          ElMessage.error(res.message || '提交失败')
          messages.value.pop()
        }
      } catch (e) {
        ElMessage.error('网络异常，请重试')
        messages.value.pop()
      } finally {
        aiLoading.value = false
      }
    }

    const startPolling = () => {
      stopPolling()
      pollTimer = setInterval(async () => {
        try {
          const res = await request.get(`/api/ai-interview/report/${interviewId}`, { timeout: 15000 })
          if (res.code === 200) {
            const d = res.data
            if (d.reportStatus === 'DONE') {
              stopPolling()
              result.value = d
              stage.value = 'finished'
              scrollBottom()
            } else if (d.reportStatus === 'FAILED') {
              stopPolling()
              reportFailed.value = true
            }
          }
        } catch (e) {
          // 轮询失败忽略，继续
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
            startPolling()
          } else if (res.data.reportStatus === 'DONE') {
            result.value = res.data
            stage.value = 'finished'
          }
        } else {
          ElMessage.error(res.message || '结束失败')
        }
      } catch (e) {
        ElMessage.error('网络异常，请重试')
      } finally {
        finishing.value = false
      }
    }

    const handleRetryReport = () => {
      reportFailed.value = false
      // 重新触发 finish 接口（后端幂等/重入）
      handleFinish()
    }

    const resetAll = () => {
      stopPolling()
      reportFailed.value = false
      stage.value = 'config'
      messages.value = []
      result.value = {}
      interviewId = null
      answerText.value = ''
    }

    const goLearningPath = () => router.push('/learning-path')

    onBeforeUnmount(() => {
      stopPolling()
    })

    return {
      stage, config, rules, configRef, starting, aiLoading, finishing,
      messages, round, maxRounds, answerText, result, chatBodyRef, credits,
      reportFailed,
      directionLabel, difficultyLabel,
      handleStart, handleSend, handleFinish, handleRetryReport, resetAll, goLearningPath
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
  border-bottom: 1px solid #f0f0f0;
}
.chat-title { display: flex; align-items: center; gap: 12px; }
.chat-title h3 { margin: 0; font-size: 16px; }
.chat-sub { color: #909399; font-size: 12px; }
.chat-avatar {
  width: 40px; height: 40px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
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
  width: 32px; height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
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
