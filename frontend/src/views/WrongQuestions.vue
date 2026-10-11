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
              <!-- 选择题选项 -->
              <div v-if="item.type === 'CHOICE'" class="choices">
                <div
                  v-for="(choice, idx) in item.choices"
                  :key="choice.id"
                  class="choice-item"
                  :class="choiceClass(item, idx)"
                >
                  <span class="choice-letter">{{ letterOf(idx) }}</span>
                  <span class="choice-content">{{ choice.content }}</span>
                  <el-tag v-if="isChoiceCorrect(item, idx)" type="success" size="small" effect="light" round>正确答案</el-tag>
                  <el-tag v-if="isUserChoice(item, idx)" type="danger" size="small" effect="light" round>你的选择</el-tag>
                </div>
              </div>

              <!-- 作答对比 -->
              <div class="answer-compare">
                <div class="answer-block user-answer">
                  <span class="answer-label">你的答案</span>
                  <span class="answer-value">{{ displayAnswer(item.userAnswer, item.type) || '未作答' }}</span>
                </div>
                <div class="answer-block correct-answer">
                  <span class="answer-label">正确答案</span>
                  <span class="answer-value">{{ displayAnswer(item.correctAnswer, item.type) }}</span>
                </div>
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

              <!-- AI 评语（简答题） -->
              <div v-if="item.aiCorrection" class="note-block ai-correction">
                <span class="note-label ai-label">AI 批改</span>
                <span class="note-content">{{ item.aiCorrection }}</span>
              </div>

              <!-- 解析 -->
              <div v-if="item.analysis" class="note-block analysis">
                <span class="note-label">解析</span>
                <span class="note-content">{{ item.analysis }}</span>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Collection, ArrowDown, Aim } from '@element-plus/icons-vue'
import {
  getWrongQuestions,
  getWrongQuestionStats,
  toggleWrongQuestionMaster,
  removeWrongQuestion
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
/** 操作进行中的错题ID：防止连续点击导致掌握状态被反复切换 */
const actionLoadingId = ref(null)

const hasFilter = computed(() => activeCategoryId.value !== null || statusFilter.value !== '')

const emptyText = computed(() =>
  hasFilter.value
    ? '当前筛选条件下没有错题'
    : '还没有错题记录。考试交卷判卷后，答错和部分正确的题目会自动收录到这里'
)

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

// ------- 展示辅助 -------

const letterOf = (idx) => String.fromCharCode(65 + idx)

function correctLetters(item) {
  if (!item.correctAnswer) return []
  return String(item.correctAnswer).split(',').map(s => s.trim().toUpperCase()).filter(Boolean)
}

function userLetters(item) {
  if (!item.userAnswer) return []
  return String(item.userAnswer).split(',').map(s => s.trim().toUpperCase()).filter(Boolean)
}

const isChoiceCorrect = (item, idx) => correctLetters(item).includes(letterOf(idx))
const isUserChoice = (item, idx) => userLetters(item).includes(letterOf(idx))

function choiceClass(item, idx) {
  const correct = isChoiceCorrect(item, idx)
  const user = isUserChoice(item, idx)
  if (correct && user) return 'choice-correct-user'
  if (correct) return 'choice-correct'
  if (user) return 'choice-wrong'
  return ''
}

function displayAnswer(answer, type) {
  if (!answer) return ''
  if (type === 'JUDGE') {
    const v = String(answer).toUpperCase()
    if (v === 'TRUE' || v === 'T') return '正确'
    if (v === 'FALSE' || v === 'F') return '错误'
  }
  return answer
}

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

.choices {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.choice-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
  transition: border-color 0.2s ease;
}

.choice-item.choice-correct,
.choice-item.choice-correct-user {
  border-color: #b3e19d;
  background: #f0f9eb;
}

.choice-item.choice-wrong {
  border-color: #fab6b6;
  background: #fef0f0;
}

.choice-letter {
  font-weight: 600;
  color: #303133;
}

.choice-content {
  flex: 1;
  word-break: break-all;
}

.answer-compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}

.answer-block {
  padding: 10px 16px;
  border-radius: 10px;
}

.answer-block.user-answer {
  background: #fef0f0;
}

.answer-block.correct-answer {
  background: #f0f9eb;
}

.answer-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.answer-value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  word-break: break-all;
}

.user-answer .answer-value {
  color: #f56c6c;
}

.correct-answer .answer-value {
  color: #67c23a;
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

/* 解析 / AI 批改 */
.note-block {
  display: flex;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #f7f8fa;
  margin-bottom: 10px;
  line-height: 1.6;
}

.note-label {
  flex-shrink: 0;
  align-self: flex-start;
  padding: 1px 8px;
  border-radius: 6px;
  font-size: 12px;
  color: #7952b3;
  background: rgba(121, 82, 179, 0.1);
}

.ai-label {
  color: #409eff;
  background: rgba(64, 158, 255, 0.12);
}

.ai-correction {
  background: #f0f7ff;
}

.note-content {
  font-size: 14px;
  color: #606266;
  word-break: break-all;
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

  .answer-compare {
    grid-template-columns: 1fr;
  }

  .detail-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>