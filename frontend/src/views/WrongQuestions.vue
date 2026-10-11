<template>
  <div class="wrong-questions-page">
    <div class="page-container">
      <!-- 顶部渐变头图 + 统计概览 -->
      <div class="hero-banner">
        <div class="hero-left">
          <div class="hero-title">
            <el-icon class="hero-icon"><Collection /></el-icon>
            <span>我的错题本</span>
          </div>
          <div class="hero-sub">考试判卷后自动收录，按知识点归类；连续答对 2 次或手动标记即掌握</div>
          <div class="hero-actions">
            <el-button
              class="consolidate-btn"
              :loading="consolidateLoading"
              :disabled="consolidateDisabled"
              @click="handleConsolidate"
            >
              <el-icon><DocumentAdd /></el-icon>
              <span class="btn-text">生成巩固练习卷</span>
            </el-button>
            <span class="hero-hint">基于未掌握/半掌握错题自动组卷，考完自动回写掌握度</span>
          </div>
        </div>
        <div class="hero-stats">
          <div class="hero-stat">
            <div class="hero-stat-number">{{ stats.total || 0 }}</div>
            <div class="hero-stat-label">错题总数</div>
          </div>
          <div class="hero-stat">
            <div class="hero-stat-number">{{ stats.unmastered || 0 }}</div>
            <div class="hero-stat-label">未掌握</div>
          </div>
          <div class="hero-stat">
            <div class="hero-stat-number">{{ stats.learning || 0 }}</div>
            <div class="hero-stat-label">半掌握</div>
          </div>
          <div class="hero-stat">
            <div class="hero-stat-number">{{ stats.mastered || 0 }}</div>
            <div class="hero-stat-label">已掌握</div>
          </div>
        </div>
      </div>

      <!-- 筛选区 -->
      <el-card class="filter-card" shadow="never">
        <div class="filter-row">
          <span class="filter-label">知识点</span>
          <span
            class="cat-chip"
            :class="{ active: activeCategoryId === null }"
            @click="handleCategoryChange(null)"
          >全部（{{ stats.total || 0 }}）</span>
          <span
            v-for="cat in stats.categories"
            :key="cat.categoryId"
            class="cat-chip"
            :class="{ active: activeCategoryId === cat.categoryId }"
            @click="handleCategoryChange(cat.categoryId)"
          >{{ cat.categoryName }}（{{ cat.total }}）</span>
        </div>
        <div class="filter-row">
          <span class="filter-label">掌握状态</span>
          <el-radio-group v-model="statusFilter" size="small" @change="handleStatusChange">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="UNMASTERED">未掌握</el-radio-button>
            <el-radio-button value="LEARNING">半掌握</el-radio-button>
            <el-radio-button value="MASTERED">已掌握</el-radio-button>
          </el-radio-group>
        </div>
      </el-card>

      <!-- 错题列表 -->
      <div v-loading="loading" class="wrong-list">
        <el-card
          v-for="item in list"
          :key="item.id"
          class="wrong-item"
          :class="'status-' + (item.status || 'unmastered').toLowerCase()"
          shadow="never"
        >
          <div class="item-head" @click="toggleExpand(item.id)">
            <div class="item-main">
              <div class="item-tags">
                <el-tag :type="typeTag(item.type)" size="small" effect="light">{{ typeText(item.type) }}</el-tag>
                <el-tag :type="difficultyTag(item.difficulty)" size="small" effect="plain">{{ difficultyText(item.difficulty) }}</el-tag>
                <el-tag v-if="item.multi" type="warning" size="small" effect="plain">多选</el-tag>
                <el-tag v-if="item.categoryName" size="small" effect="plain" class="category-tag">{{ item.categoryName }}</el-tag>
              </div>
              <div class="item-title">{{ item.title }}</div>
            </div>
            <div class="item-meta">
              <el-tag :type="statusTag(item.status)" size="small" effect="light" round>{{ statusText(item.status) }}</el-tag>
              <span class="wrong-count-pill">错 {{ item.wrongCount }} 次</span>
              <el-icon class="expand-icon" :class="{ expanded: expandedIds.has(item.id) }"><ArrowDown /></el-icon>
            </div>
          </div>

          <el-collapse-transition>
            <div v-show="expandedIds.has(item.id)" class="item-detail">
              <!-- 选择题：点击选项重新作答（与刷题交互一致） -->
              <div v-if="item.type === 'CHOICE'" class="question-choices">
                <div
                  v-for="(choice, idx) in item.choices"
                  :key="choice.id"
                  class="choice-item"
                  :class="choiceClass(item, idx)"
                  @click.stop="selectChoice(item, idx)"
                >
                  <span class="choice-label">{{ letterOf(idx) }}</span>
                  <span class="choice-content">{{ choice.content }}</span>
                  <el-icon v-if="showCorrectMark(item, idx)" class="correct-icon"><Check /></el-icon>
                </div>
              </div>

              <!-- 判断题：单选框作答 -->
              <div v-if="item.type === 'JUDGE'" class="judge-options">
                <el-radio-group
                  v-model="retryOf(item).answer"
                  @change="handleJudgeAnswer(item)"
                >
                  <el-radio value="TRUE" size="large">正确</el-radio>
                  <el-radio value="FALSE" size="large">错误</el-radio>
                </el-radio-group>
              </div>

              <!-- 简答题：文本作答（不自动判对错） -->
              <div v-if="item.type === 'TEXT'" class="text-answer">
                <el-input
                  v-model="retryOf(item).answer"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入您的答案..."
                />
              </div>

              <!-- 实时校验反馈（与刷题反馈格式一致） -->
              <div v-if="feedbackVisible(item)" class="feedback-section">
                <el-alert
                  :title="retryOf(item).correct ? '回答正确！' : '回答错误！'"
                  :type="retryOf(item).correct ? 'success' : 'error'"
                  show-icon
                  :closable="false"
                >
                  <div v-if="!retryOf(item).correct">
                    <div>你的答案是：<strong>{{ formatRetryAnswer(item) }}</strong></div>
                  </div>
                </el-alert>
              </div>

              <!-- 操作行：查看答案 / AI 解析 -->
              <div class="detail-toolbar">
                <el-button
                  size="small"
                  :type="revealedIds.has(item.id) ? 'warning' : 'primary'"
                  @click.stop="toggleAnswer(item)"
                >
                  {{ revealedIds.has(item.id) ? '隐藏答案' : '查看答案' }}
                </el-button>
                <el-button
                  type="warning"
                  plain
                  size="small"
                  :loading="aiState(item.id).loading"
                  @click.stop="handleAiAnalysis(item)"
                >
                  <el-icon><MagicStick /></el-icon>
                  <span class="btn-text">AI 解析本题</span>
                </el-button>
                <span v-if="aiState(item.id).result" class="ai-tip" :class="{ 'is-free': aiState(item.id).free }">
                  {{ aiTip(aiState(item.id)) }}
                </span>
              </div>

              <!-- 答案区（与正式刷题场景格式完全一致） -->
              <div v-if="revealedIds.has(item.id)" class="answer-section">
                <div class="answer-header">
                  <el-icon><InfoFilled /></el-icon>
                  <span>正确答案</span>
                </div>
                <div class="answer-content">
                  <div v-if="item.type === 'CHOICE'">
                    <div v-for="(choice, idx) in item.choices" :key="choice.id">
                      <span v-if="isChoiceCorrect(item, idx)" class="correct-answer">
                        {{ letterOf(idx) }}. {{ choice.content }}
                      </span>
                    </div>
                  </div>
                  <div v-else-if="item.type === 'JUDGE'">
                    <span class="correct-answer">{{ isTrueAnswer(item) ? '正确' : '错误' }}</span>
                  </div>
                  <div v-else>
                    <div class="text-answer-content">{{ item.correctAnswer }}</div>
                  </div>
                </div>

                <div v-if="item.analysis" class="analysis-section">
                  <div class="analysis-header">
                    <el-icon><Notebook /></el-icon>
                    <span>题目解析</span>
                  </div>
                  <div class="analysis-content">{{ item.analysis }}</div>
                </div>
              </div>

              <!-- AI 解析结果 -->
              <div v-if="aiState(item.id).loading" class="ai-analysis-row">
                <span class="ai-analysis-label">AI 解析：</span>
                <span class="ai-analysis-text">AI 正在讲解，约需 10~30 秒...</span>
              </div>
              <div v-else-if="aiState(item.id).result" class="ai-analysis-row">
                <span class="ai-analysis-label">AI 解析：</span>
                <span class="ai-analysis-text">{{ aiState(item.id).result }}</span>
              </div>

              <!-- 掌握进度 -->
              <div class="mastery-row" :class="{ mastered: item.status === 'MASTERED' }">
                <el-icon class="mastery-icon"><Aim /></el-icon>
                <span class="mastery-text">{{ masteryHint(item) }}</span>
                <el-progress
                  v-if="item.status !== 'MASTERED'"
                  class="mastery-bar"
                  :percentage="masteryPercent(item)"
                  :stroke-width="6"
                  :show-text="false"
                  :color="masteryColor"
                />
              </div>

              <div class="detail-footer">
                <span class="last-wrong-time">最近答错：{{ item.lastWrongTime || '-' }}</span>
                <div class="detail-actions">
                  <el-button
                    size="small"
                    :type="item.status === 'MASTERED' ? 'info' : 'success'"
                    plain
                    round
                    :loading="actionLoadingId === item.id"
                    @click.stop="handleToggleMaster(item)"
                  >{{ item.status === 'MASTERED' ? '取消掌握' : '标记已掌握' }}</el-button>
                  <el-button
                    size="small"
                    type="danger"
                    text
                    :disabled="actionLoadingId === item.id"
                    @click.stop="handleRemove(item)"
                  >移出错题本</el-button>
                </div>
              </div>
            </div>
          </el-collapse-transition>
        </el-card>

        <!-- 空态 -->
        <el-card v-if="!loading && !list.length" class="empty-card" shadow="never">
          <el-empty :description="emptyText">
            <el-button v-if="hasFilter" @click="resetFilter">清除筛选</el-button>
            <el-button v-else type="primary" @click="router.push('/exam/list')">去做题</el-button>
          </el-empty>
        </el-card>
      </div>

      <!-- 分页 -->
      <div v-if="total > size" class="pagination-row">
        <el-pagination
          layout="prev, pager, next, total"
          :total="total"
          :page-size="size"
          :current-page="page"
          background
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Collection, ArrowDown, Aim, Check, InfoFilled, Notebook, MagicStick, DocumentAdd
} from '@element-plus/icons-vue'
import {
  getWrongQuestions,
  getWrongQuestionStats,
  toggleWrongQuestionMaster,
  removeWrongQuestion,
  aiAnalyzeWrongQuestion,
  generateConsolidationPaper
} from '@/api/wrongQuestion'

const router = useRouter()

/** 连续答对次数阈值（与后端 WrongQuestionService.MASTER_STREAK 保持一致） */
const MASTER_STREAK = 2

const stats = ref({ total: 0, unmastered: 0, learning: 0, mastered: 0, categories: [] })
const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const activeCategoryId = ref(null)
const statusFilter = ref('')
const expandedIds = ref(new Set())
/** 已展开"查看答案"的错题ID */
const revealedIds = ref(new Set())
/** 重新作答状态：错题ID -> { answer, correct, answered } */
const retryStates = reactive({})
/** AI 解析状态：错题ID -> { loading, result, free, remainingFree, credits } */
const aiStates = reactive({})
/** 操作进行中的错题ID：防止连续点击导致掌握状态被反复切换 */
const actionLoadingId = ref(null)
/** 巩固练习卷生成中 */
const consolidateLoading = ref(false)

const hasFilter = computed(() => activeCategoryId.value !== null || statusFilter.value !== '')

/** 无未掌握/半掌握错题时禁用巩固卷生成（已掌握题目不再出题） */
const consolidateDisabled = computed(() =>
  ((stats.value.unmastered || 0) + (stats.value.learning || 0)) === 0
)

const emptyText = computed(() =>
  hasFilter.value
    ? '当前筛选条件下没有错题'
    : '还没有错题记录。考试交卷判卷后，答错和部分正确的题目会自动收录到这里'
)

function retryOf(item) {
  if (!retryStates[item.id]) {
    retryStates[item.id] = { answer: '', correct: false, answered: false }
  }
  return retryStates[item.id]
}

function aiState(id) {
  if (!aiStates[id]) {
    aiStates[id] = { loading: false, result: '', free: false, remainingFree: 3, credits: 0 }
  }
  return aiStates[id]
}

const aiTip = (s) =>
  s.free
    ? `已用免费额度 · 今日剩余 ${s.remainingFree} 次`
    : `本次消耗 5 积分 · 当前 ${s.credits} 积分`

async function loadStats() {
  try {
    const res = await getWrongQuestionStats()
    if (res.code === 200 && res.data) {
      stats.value = res.data
    }
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  }
}

async function loadList() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (activeCategoryId.value !== null) params.categoryId = activeCategoryId.value
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getWrongQuestions(params)
    if (res.code === 200 && res.data) {
      list.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function reload() {
  loadStats()
  loadList()
}

function handleCategoryChange(categoryId) {
  activeCategoryId.value = categoryId
  page.value = 1
  loadList()
}

function handleStatusChange() {
  page.value = 1
  loadList()
}

function handlePageChange(newPage) {
  page.value = newPage
  loadList()
}

function resetFilter() {
  activeCategoryId.value = null
  statusFilter.value = ''
  page.value = 1
  loadList()
}

function toggleExpand(id) {
  const next = new Set(expandedIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedIds.value = next
}

// ------- 作答交互（对齐刷题模块） -------

const letterOf = (idx) => String.fromCharCode(65 + idx)

function correctLetters(item) {
  if (!item.correctAnswer) return []
  return String(item.correctAnswer).split(',').map(s => s.trim().toUpperCase()).filter(Boolean)
}

const isChoiceCorrect = (item, idx) => correctLetters(item).includes(letterOf(idx))

function normalizeJudgeAnswer(value) {
  const v = String(value || '').trim().toUpperCase()
  if (v === 'TRUE' || v === 'T' || v === '正确') return 'TRUE'
  if (v === 'FALSE' || v === 'F' || v === '错误') return 'FALSE'
  return v
}

const isTrueAnswer = (item) => normalizeJudgeAnswer(item.correctAnswer) === 'TRUE'

/** 单选/多选点击选项：记录本次作答并实时校验 */
function selectChoice(item, idx) {
  const st = retryOf(item)
  const letter = letterOf(idx)
  if (item.multi) {
    const answers = st.answer ? st.answer.split(',') : []
    const pos = answers.indexOf(letter)
    if (pos > -1) {
      answers.splice(pos, 1)
    } else {
      answers.push(letter)
    }
    answers.sort()
    st.answer = answers.join(',')
  } else {
    st.answer = letter
  }
  st.answered = true
  st.correct = isRetryCorrect(item)
}

function handleJudgeAnswer(item) {
  const st = retryOf(item)
  st.answered = true
  st.correct = isRetryCorrect(item)
}

function isRetryCorrect(item) {
  const st = retryOf(item)
  if (item.type === 'JUDGE') {
    return normalizeJudgeAnswer(st.answer) === normalizeJudgeAnswer(item.correctAnswer)
  }
  const correct = correctLetters(item)
  const user = st.answer ? st.answer.split(',') : []
  if (item.multi) {
    return correct.length === user.length && correct.every(letter => user.includes(letter))
  }
  return user.length === 1 && correct.includes(user[0])
}

/** 判断题/选择题作答后实时展示对错反馈；简答题不自动判分 */
const feedbackVisible = (item) =>
  item.type !== 'TEXT' && retryOf(item).answered

/** 用户本次作答的展示（与刷题一致：选择题显示选项标号） */
function formatRetryAnswer(item) {
  const st = retryOf(item)
  if (!st.answer) return ''
  if (item.type === 'JUDGE') {
    return normalizeJudgeAnswer(st.answer) === 'TRUE' ? '正确' : '错误'
  }
  return item.multi ? st.answer.split(',').join('、') : st.answer
}

/** 选项样式：本次选错标红、正确项标绿对比；未作答且未查看答案时保持默认样式 */
function choiceClass(item, idx) {
  const letter = letterOf(idx)
  const st = retryOf(item)
  const correct = isChoiceCorrect(item, idx)
  if (st.answered) {
    const picked = st.answer ? st.answer.split(',').includes(letter) : false
    if (picked && correct) return 'choice-correct-user'
    if (picked && !correct) return 'choice-wrong'
    if (correct) return 'choice-correct'
    return ''
  }
  if (revealedIds.value.has(item.id) && correct) return 'choice-correct'
  return ''
}

const showCorrectMark = (item, idx) =>
  isChoiceCorrect(item, idx) && (retryOf(item).answered || revealedIds.value.has(item.id))

function toggleAnswer(item) {
  const next = new Set(revealedIds.value)
  if (next.has(item.id)) {
    next.delete(item.id)
  } else {
    next.add(item.id)
  }
  revealedIds.value = next
}

// ------- AI 解析（统一计费） -------

async function handleAiAnalysis(item) {
  const s = aiState(item.id)
  if (s.loading) return
  s.loading = true
  try {
    const res = await aiAnalyzeWrongQuestion(item.id)
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

// ------- 掌握状态操作 -------

async function handleToggleMaster(item) {
  if (actionLoadingId.value) return
  const toMastered = item.status !== 'MASTERED'
  actionLoadingId.value = item.id
  try {
    const res = await toggleWrongQuestionMaster(item.id)
    if (res.code === 200) {
      ElMessage.success(toMastered ? '已标记为掌握，继续保持！' : '已取消掌握标记')
      reload()
    }
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    actionLoadingId.value = null
  }
}

async function handleRemove(item) {
  try {
    await ElMessageBox.confirm(
      '移出后该题若再次答错会被重新收录，确定移出错题本吗？',
      '移出错题本',
      { type: 'warning', confirmButtonText: '移出', cancelButtonText: '取消', autofocus: false }
    )
  } catch (e) {
    return // 用户取消
  }
  if (actionLoadingId.value) return
  actionLoadingId.value = item.id
  try {
    const res = await removeWrongQuestion(item.id)
    if (res.code === 200) {
      ElMessage.success('已移出错题本')
      reload()
    }
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    actionLoadingId.value = null
  }
}

// ------- 巩固练习卷 -------

async function handleConsolidate() {
  if (consolidateLoading.value) return
  consolidateLoading.value = true
  try {
    const res = await generateConsolidationPaper()
    if (res.code === 200 && res.data) {
      ElMessage.success('巩固练习卷已生成，即将开始作答')
      router.push(`/exam/start/${res.data}`)
    }
  } catch (e) {
    // 错误提示由 request 拦截器统一处理（无可用错题时返回业务提示）
  } finally {
    consolidateLoading.value = false
  }
}

// ------- 掌握度展示 -------

const masteryPercent = (item) =>
  Math.min(100, Math.round(((item.correctStreak || 0) / MASTER_STREAK) * 100))

const masteryColor = [
  { color: '#e6a23c', percentage: 50 },
  { color: '#67c23a', percentage: 100 }
]

function masteryHint(item) {
  if (item.status === 'MASTERED') return '已掌握：答错会自动回到未掌握，继续保持！'
  const streak = item.correctStreak || 0
  if (streak > 0) return `再答对 ${MASTER_STREAK - streak} 次即可自动掌握`
  return `连续答对 ${MASTER_STREAK} 次可自动掌握，也可直接手动标记`
}

function typeText(type) {
  switch (type) {
    case 'CHOICE': return '选择题'
    case 'JUDGE': return '判断题'
    case 'TEXT': return '简答题'
    default: return type || '-'
  }
}

function typeTag(type) {
  switch (type) {
    case 'CHOICE': return 'primary'
    case 'JUDGE': return 'warning'
    case 'TEXT': return 'success'
    default: return 'info'
  }
}

function difficultyText(difficulty) {
  switch (difficulty) {
    case 'EASY': return '简单'
    case 'MEDIUM': return '中等'
    case 'HARD': return '困难'
    default: return difficulty || '-'
  }
}

function difficultyTag(difficulty) {
  switch (difficulty) {
    case 'EASY': return 'success'
    case 'MEDIUM': return 'warning'
    case 'HARD': return 'danger'
    default: return 'info'
  }
}

function statusText(status) {
  switch (status) {
    case 'UNMASTERED': return '未掌握'
    case 'LEARNING': return '半掌握'
    case 'MASTERED': return '已掌握'
    default: return status || '-'
  }
}

function statusTag(status) {
  switch (status) {
    case 'UNMASTERED': return 'danger'
    case 'LEARNING': return 'warning'
    case 'MASTERED': return 'success'
    default: return 'info'
  }
}

onMounted(() => {
  loadStats()
  loadList()
})
</script>

<style scoped>
.wrong-questions-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px 0 48px;
}

.page-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 16px;
}

/* ------- 渐变头图 ------- */
.hero-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  padding: 28px 32px;
  border-radius: 16px;
  background: linear-gradient(135deg, #7952b3 0%, #9d7fd6 60%, #b79ce8 100%);
  box-shadow: 0 12px 32px rgba(121, 82, 179, 0.25);
  margin-bottom: 16px;
}

.hero-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.hero-icon {
  font-size: 26px;
}

.hero-sub {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
}

/* 头图上的白色主按钮（生成巩固练习卷） */
.consolidate-btn {
  background: #fff;
  border: none;
  color: #7952b3;
  font-weight: 600;
}

.consolidate-btn:hover,
.consolidate-btn:focus {
  background: #f3ecff;
  color: #7952b3;
}

.consolidate-btn.is-disabled {
  background: rgba(255, 255, 255, 0.45);
  color: rgba(121, 82, 179, 0.55);
}

.hero-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.hero-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-stat {
  min-width: 86px;
  text-align: center;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.hero-stat-number {
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
}

.hero-stat-label {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}

/* ------- 筛选区 ------- */
.filter-card {
  margin-bottom: 16px;
  border-radius: 12px;
  border: none;
}

.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-row + .filter-row {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #ebeef5;
}

.filter-label {
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

.cat-chip {
  padding: 5px 14px;
  border-radius: 999px;
  font-size: 13px;
  color: #606266;
  background: #f2f3f5;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cat-chip:hover {
  background: #e9eaf0;
}

.cat-chip.active {
  color: #7952b3;
  background: rgba(121, 82, 179, 0.1);
  border-color: rgba(121, 82, 179, 0.35);
  font-weight: 600;
}

/* ------- 错题卡片 ------- */
.wrong-list {
  min-height: 200px;
}

.wrong-item {
  position: relative;
  margin-bottom: 12px;
  border-radius: 12px;
  border: none;
  overflow: hidden;
  transition: box-shadow 0.25s ease, transform 0.25s ease;
}

.wrong-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

/* 左侧状态色条 */
.wrong-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #f56c6c;
}

.wrong-item.status-learning::before {
  background: #e6a23c;
}

.wrong-item.status-mastered::before {
  background: #67c23a;
}

.item-head {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  cursor: pointer;
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.category-tag {
  border-color: #e4e7ed;
  color: #606266;
}

.item-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  line-height: 1.6;
  word-break: break-all;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  padding-top: 2px;
}

.wrong-count-pill {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #f56c6c;
  background: #fef0f0;
}

.expand-icon {
  color: #909399;
  transition: transform 0.25s ease;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

/* ------- 展开详情 ------- */
.item-detail {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #ebeef5;
}

/* 选择题选项（对齐刷题模块交互样式） */
.question-choices {
  margin-bottom: 16px;
}

.choice-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: #f8f9fa;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.choice-item:hover {
  background: #e6f7ff;
  border-color: #409eff;
}

.choice-label {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #409eff;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: bold;
  margin-right: 12px;
  flex-shrink: 0;
}

.choice-content {
  flex: 1;
  word-break: break-all;
}

.choice-item.choice-correct,
.choice-item.choice-correct-user {
  background: #f6ffed;
  border-color: #52c41a;
}

.choice-item.choice-correct .choice-label,
.choice-item.choice-correct-user .choice-label {
  background: #52c41a;
}

.choice-item.choice-wrong {
  background: #fff1f0;
  border-color: #ff4d4f;
}

.choice-item.choice-wrong .choice-label {
  background: #ff4d4f;
}

.correct-icon {
  color: #52c41a;
  font-size: 18px;
  margin-left: 8px;
}

/* 判断题 / 简答题 */
.judge-options {
  margin-bottom: 16px;
}

.text-answer {
  margin-bottom: 16px;
}

/* 实时校验反馈 */
.feedback-section {
  margin-bottom: 16px;
}

/* 操作行 */
.detail-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.btn-text {
  margin-left: 4px;
}

.ai-tip {
  font-size: 12px;
  color: #e6a23c;
}

.ai-tip.is-free {
  color: #67c23a;
}

/* 答案区（与刷题模块格式一致） */
.answer-section {
  background: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.answer-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #1890ff;
}

.answer-content {
  margin-bottom: 16px;
}

.correct-answer {
  color: #52c41a;
  font-weight: 600;
}

.text-answer-content {
  background: #fff;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #d9d9d9;
  white-space: pre-wrap;
}

.analysis-section {
  border-top: 1px solid #d9d9d9;
  padding-top: 16px;
}

.analysis-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: 600;
  color: #722ed1;
}

.analysis-content {
  color: #666;
  line-height: 1.6;
}

/* AI 解析结果 */
.ai-analysis-row {
  padding: 12px 16px;
  border-radius: 8px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  margin-bottom: 16px;
  line-height: 1.7;
}

.ai-analysis-label {
  font-weight: 600;
  color: #e6a23c;
  flex-shrink: 0;
}

.ai-analysis-text {
  font-size: 14px;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 掌握进度 */
.mastery-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 14px;
  border-radius: 10px;
  background: #f7f8fa;
  font-size: 13px;
  color: #909399;
}

.mastery-row.mastered {
  background: #f0f9eb;
  color: #67c23a;
}

.mastery-icon {
  flex-shrink: 0;
}

.mastery-text {
  flex-shrink: 0;
}

.mastery-bar {
  flex: 1;
  min-width: 80px;
}

/* 底部操作行 */
.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.last-wrong-time {
  font-size: 12px;
  color: #c0c4cc;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-card {
  border-radius: 12px;
}

.pagination-row {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .hero-banner {
    padding: 22px 20px;
  }

  .hero-stat {
    min-width: 70px;
    padding: 10px 12px;
  }

  .hero-stat-number {
    font-size: 22px;
  }

  .item-head {
    flex-direction: column;
    gap: 10px;
  }

  .detail-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>