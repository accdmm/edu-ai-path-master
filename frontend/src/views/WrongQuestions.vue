<template>
  <div class="wrong-questions-page">
    <div class="page-container">
      <!-- 顶部统计概览 -->
      <el-card class="overview-card" shadow="never">
        <div class="overview-header">
          <div class="overview-title">
            <el-icon class="title-icon"><Collection /></el-icon>
            <span>我的错题本</span>
          </div>
          <div class="overview-desc">错题在每次考试判卷后自动收录，按知识点归类，掌握后自动归档</div>
        </div>
        <div class="stat-grid">
          <div class="stat-block">
            <div class="stat-number">{{ stats.total || 0 }}</div>
            <div class="stat-label">错题总数</div>
          </div>
          <div class="stat-block unmastered">
            <div class="stat-number">{{ stats.unmastered || 0 }}</div>
            <div class="stat-label">未掌握</div>
          </div>
          <div class="stat-block learning">
            <div class="stat-number">{{ stats.learning || 0 }}</div>
            <div class="stat-label">半掌握（部分正确）</div>
          </div>
          <div class="stat-block mastered">
            <div class="stat-number">{{ stats.mastered || 0 }}</div>
            <div class="stat-label">已掌握</div>
          </div>
        </div>
      </el-card>

      <!-- 筛选区 -->
      <el-card class="filter-card" shadow="never">
        <div class="filter-row">
          <span class="filter-label">知识点：</span>
          <el-tag
            :effect="activeCategoryId === null ? 'dark' : 'plain'"
            class="filter-tag"
            @click="handleCategoryChange(null)"
          >全部（{{ stats.total || 0 }}）</el-tag>
          <el-tag
            v-for="cat in stats.categories"
            :key="cat.categoryId"
            :effect="activeCategoryId === cat.categoryId ? 'dark' : 'plain'"
            class="filter-tag"
            @click="handleCategoryChange(cat.categoryId)"
          >{{ cat.categoryName }}（{{ cat.total }}）</el-tag>
        </div>
        <div class="filter-row">
          <span class="filter-label">掌握状态：</span>
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
          shadow="never"
        >
          <div class="item-head" @click="toggleExpand(item.id)">
            <div class="item-tags">
              <el-tag :type="typeTag(item.type)" size="small">{{ typeText(item.type) }}</el-tag>
              <el-tag :type="difficultyTag(item.difficulty)" size="small">{{ difficultyText(item.difficulty) }}</el-tag>
              <el-tag v-if="item.categoryName" type="info" size="small" effect="plain">{{ item.categoryName }}</el-tag>
            </div>
            <div class="item-title">{{ item.title }}</div>
            <div class="item-meta">
              <el-tag :type="statusTag(item.status)" size="small">{{ statusText(item.status) }}</el-tag>
              <span class="wrong-count">错 {{ item.wrongCount }} 次</span>
              <el-icon class="expand-icon" :class="{ expanded: expandedIds.has(item.id) }"><ArrowDown /></el-icon>
            </div>
          </div>

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
                <el-tag v-if="isChoiceCorrect(item, idx)" type="success" size="small" effect="plain">正确答案</el-tag>
                <el-tag v-if="isUserChoice(item, idx)" type="danger" size="small" effect="plain">你的选择</el-tag>
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

            <!-- AI 评语（简答题） -->
            <div v-if="item.aiCorrection" class="ai-correction">
              <span class="answer-label">AI 批改</span>
              <span>{{ item.aiCorrection }}</span>
            </div>

            <!-- 解析 -->
            <div v-if="item.analysis" class="analysis">
              <span class="answer-label">解析</span>
              <span>{{ item.analysis }}</span>
            </div>

            <div class="detail-footer">
              <span class="last-wrong-time">最近答错：{{ item.lastWrongTime || '-' }}</span>
            </div>
          </div>
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
import { Collection, ArrowDown } from '@element-plus/icons-vue'
import { getWrongQuestions, getWrongQuestionStats } from '@/api/wrongQuestion'

const router = useRouter()

const stats = ref({ total: 0, unmastered: 0, learning: 0, mastered: 0, categories: [] })
const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const activeCategoryId = ref(null)
const statusFilter = ref('')
const expandedIds = ref(new Set())

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

.overview-card,
.filter-card {
  margin-bottom: 16px;
  border-radius: 10px;
}

.overview-header {
  margin-bottom: 18px;
}

.overview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.title-icon {
  color: #e6a23c;
  font-size: 22px;
}

.overview-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-block {
  text-align: center;
  padding: 14px 8px;
  background: #f7f8fa;
  border-radius: 8px;
}

.stat-block.unmastered .stat-number { color: #f56c6c; }
.stat-block.learning .stat-number { color: #e6a23c; }
.stat-block.mastered .stat-number { color: #67c23a; }

.stat-number {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-row + .filter-row {
  margin-top: 12px;
}

.filter-label {
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}

.filter-tag {
  cursor: pointer;
}

.wrong-list {
  min-height: 200px;
}

.wrong-item {
  margin-bottom: 12px;
  border-radius: 10px;
}

.item-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
}

.item-tags {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  padding-top: 2px;
}

.item-title {
  flex: 1;
  font-size: 15px;
  color: #303133;
  line-height: 1.5;
  word-break: break-all;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.wrong-count {
  font-size: 13px;
  color: #f56c6c;
}

.expand-icon {
  color: #909399;
  transition: transform 0.2s;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.item-detail {
  margin-top: 14px;
  padding-top: 14px;
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
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  font-size: 14px;
  color: #606266;
}

.choice-item.choice-correct,
.choice-item.choice-correct-user {
  border-color: #67c23a;
  background: #f0f9eb;
}

.choice-item.choice-wrong {
  border-color: #f56c6c;
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
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
}

.answer-block {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.answer-label {
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

.answer-value {
  font-size: 14px;
  color: #303133;
  word-break: break-all;
}

.user-answer .answer-value {
  color: #f56c6c;
}

.correct-answer .answer-value {
  color: #67c23a;
  font-weight: 500;
}

.ai-correction,
.analysis {
  display: flex;
  gap: 8px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
}

.detail-footer {
  font-size: 12px;
  color: #c0c4cc;
}

.empty-card {
  border-radius: 10px;
}

.pagination-row {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .item-head {
    flex-direction: column;
  }

  .answer-compare {
    flex-direction: column;
    gap: 8px;
  }
}
</style>