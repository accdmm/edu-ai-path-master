<template>
  <div class="paper-detail-container">
    <div class="page-header">
      <el-button type="primary" plain @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <el-button plain @click="$router.push('/')">
        <el-icon><HomeFilled /></el-icon>
        首页
      </el-button>
      <div class="header-info">
        <h2>试卷详情</h2>
        <p>查看试卷题目与参考答案</p>
      </div>
    </div>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <div class="paper-card">
        <div class="card-top">
          <div class="paper-name">
            <el-icon color="#e6a23c"><Document /></el-icon>
            <span class="name-text">{{ paper.name }}</span>
          </div>
          <el-tag size="small" type="warning">AI 生成</el-tag>
        </div>
        <div class="paper-desc">{{ paper.description || '暂无描述' }}</div>
        <div class="paper-meta">
          <span><el-icon><CollectionTag /></el-icon> {{ paper.questionCount }} 道题</span>
          <span><el-icon><TrophyBase /></el-icon> {{ paper.totalScore }} 分</span>
          <span><el-icon><Timer /></el-icon> {{ paper.duration }} 分钟</span>
        </div>
      </div>

      <div class="questions-section">
        <h3 class="section-title">题目列表（含参考答案）</h3>
        <el-empty v-if="!questions.length" description="该试卷暂未设置题目" :image-size="80" />
        <div v-for="(q, index) in questions" :key="q.id" class="question-card">
          <div class="question-head">
            <span class="q-no">{{ index + 1 }}</span>
            <el-tag size="small" :type="typeTagType(q.type)">{{ typeText(q.type) }}</el-tag>
            <span class="q-score">{{ q.paperScore || q.score }} 分</span>
          </div>
          <div class="q-title">{{ q.title }}</div>

          <div v-if="q.type === 'CHOICE'" class="q-choices">
            <div
              v-for="(option, i) in sortedChoices(q)"
              :key="option.id"
              class="choice-item"
              :class="{ correct: option.isCorrect }"
            >
              <span class="choice-letter">{{ choiceChar(i) }}</span>
              <span class="choice-content">{{ option.content }}</span>
              <el-icon v-if="option.isCorrect" class="check-icon"><CircleCheckFilled /></el-icon>
            </div>
          </div>

          <div class="q-answer">
            <span class="answer-label">参考答案：</span>
            <span class="answer-text">{{ answerText(q) }}</span>
          </div>
          <div v-if="q.analysis" class="q-analysis">
            <span class="answer-label">题目解析：</span>
            <span class="analysis-text">{{ q.analysis }}</span>
          </div>
          <div class="q-ai">
            <el-button
              type="warning"
              plain
              size="small"
              :loading="aiState(q.id).loading"
              @click="handleAiAnalysis(q)"
            >
              <el-icon><MagicStick /></el-icon>
              AI 解析本题
            </el-button>
            <span v-if="aiState(q.id).result" class="ai-tip" :class="{ 'is-free': aiState(q.id).free }">
              {{ aiTip(aiState(q.id)) }}
            </span>
          </div>
          <div v-if="aiState(q.id).loading" class="q-analysis">
            <span class="answer-label">AI 解析：</span>
            <span class="analysis-text">AI 正在讲解，约需 10~30 秒...</span>
          </div>
          <div v-else-if="aiState(q.id).result" class="q-analysis">
            <span class="answer-label">AI 解析：</span>
            <span class="analysis-text analysis-ai">{{ aiState(q.id).result }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPaperById, aiAnalyzePaperQuestion } from '../api/paper.js'
import { ArrowLeft, Document, CollectionTag, TrophyBase, Timer, CircleCheckFilled, MagicStick } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const paper = ref({})
const questions = ref([])
const loading = ref(true)

const aiStates = reactive({})
const aiState = (qid) => {
  if (!aiStates[qid]) {
    aiStates[qid] = { loading: false, result: '', free: false, remainingFree: 3, credits: 0 }
  }
  return aiStates[qid]
}

const aiTip = (s) =>
  s.free
    ? `已用免费额度 · 今日剩余 ${s.remainingFree} 次`
    : `本次消耗 5 积分 · 当前 ${s.credits} 积分`

const handleAiAnalysis = async (q) => {
  const s = aiState(q.id)
  if (s.loading) return
  s.loading = true
  try {
    const res = await aiAnalyzePaperQuestion(route.params.id, q.id)
    s.result = res.data.analysis
    s.free = res.data.free
    s.remainingFree = res.data.remainingFree
    s.credits = res.data.activeCredits
  } catch (error) {
    if (error.message && error.message.includes('积分不足')) {
      ElMessageBox.confirm(
        '积分不足，每次 AI 解析需 5 积分。购买邀请码即可获得积分，是否现在去购买？',
        '积分不足',
        { confirmButtonText: '去购买', cancelButtonText: '取消', type: 'warning' }
      )
        .then(() => router.push('/pay'))
        .catch(() => {})
    }
  } finally {
    s.loading = false
  }
}

const typeText = (type) => {
  if (type === 'CHOICE') return '选择题'
  if (type === 'JUDGE') return '判断题'
  if (type === 'TEXT') return '简答题'
  return type || '未知题型'
}

const typeTagType = (type) => {
  if (type === 'CHOICE') return ''
  if (type === 'JUDGE') return 'warning'
  if (type === 'TEXT') return 'success'
  return 'info'
}

const sortedChoices = (q) => {
  if (!q.choices || !q.choices.length) return []
  return [...q.choices].sort((a, b) => a.sort - b.sort)
}

const choiceChar = (i) => String.fromCharCode(65 + i)

const answerText = (q) => {
  if (!q.answer) return '无'
  const ans = q.answer.answer
  if (!ans) return '无'
  if (q.type === 'JUDGE') {
    return ans === 'TRUE' ? '正确' : ans === 'FALSE' ? '错误' : ans
  }
  return ans
}

const loadPaper = async () => {
  try {
    const res = await getPaperById(route.params.id)
    paper.value = res.data || {}
    questions.value = res.data?.questions || []
  } catch (error) {
    console.error('加载试卷详情失败:', error)
    ElMessage.error('试卷不存在或无权访问')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPaper()
})
</script>

<style scoped>
.paper-detail-container {
  max-width: 980px;
  margin: 0 auto;
  padding: 24px 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.header-info {
  margin-left: 6px;
}

.header-info h2 {
  margin: 0 0 4px;
  font-size: 22px;
  color: #303133;
}

.header-info p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.loading-wrap {
  padding: 20px 0;
}

.paper-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid #ebeef5;
  margin-bottom: 20px;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.paper-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #303133;
  font-size: 17px;
}

.paper-desc {
  margin: 10px 0;
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
}

.paper-meta {
  display: flex;
  gap: 16px;
  color: #606266;
  font-size: 13px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

.paper-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.section-title {
  margin: 0 0 14px;
  font-size: 17px;
  color: #303133;
}

.question-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid #ebeef5;
}

.question-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.q-no {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #409EFF;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.q-score {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

.q-title {
  font-size: 15px;
  color: #303133;
  line-height: 1.7;
  margin-bottom: 12px;
}

.q-choices {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.choice-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
  background: #fafafa;
}

.choice-item.correct {
  border-color: #67C23A;
  background: #f0f9eb;
  color: #303133;
}

.choice-letter {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #dcdfe6;
  color: #606266;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.choice-item.correct .choice-letter {
  background: #67C23A;
  border-color: #67C23A;
  color: #fff;
}

.check-icon {
  margin-left: auto;
  color: #67C23A;
  font-size: 16px;
}

.q-answer,
.q-analysis {
  font-size: 14px;
  line-height: 1.7;
}

.q-ai {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}

.ai-tip {
  font-size: 12px;
  color: #e6a23c;
}

.ai-tip.is-free {
  color: #67C23A;
}

.analysis-ai {
  white-space: pre-wrap;
  color: #606266;
}

.q-answer {
  margin-top: 4px;
}

.answer-label {
  color: #909399;
  font-weight: 600;
}

.answer-text {
  color: #67C23A;
  font-weight: 600;
}

.q-analysis .analysis-text {
  color: #606266;
}
</style>