<template>
  <div class="smart-paper-page">
    <div class="page-container">
      <!-- 头部 -->
      <div class="hero-banner">
        <div class="hero-title">
          <el-icon class="hero-icon"><MagicStick /></el-icon>
          <span>AI 智能组卷</span>
        </div>
        <div class="hero-sub">AI 自动诊断你的薄弱知识点，从题库智能选题组卷（薄弱点加权 + 题型覆盖 + 难度梯度），考完自动回写错题掌握度</div>
      </div>

      <!-- 薄弱知识点 -->
      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">AI 将重点考察的薄弱知识点</span>
            <el-button size="small" text type="primary" :loading="weakLoading" @click="loadWeakPoints">刷新</el-button>
          </div>
        </template>
        <div v-loading="weakLoading" class="weak-box">
          <template v-if="weakPoints.length">
            <el-tag
              v-for="point in weakPoints"
              :key="point.categoryId"
              type="danger"
              effect="light"
              round
              class="weak-tag"
            >{{ point.categoryName }}（{{ point.wrongCount }} 道错题）</el-tag>
          </template>
          <div v-else-if="!weakLoading" class="weak-empty">
            暂未检测到薄弱点，将按题库做综合组卷；想更精准？先去
            <el-link type="primary" :underline="false" @click="router.push('/wrong-questions')">错题本</el-link>
            看看，或完成一次考试积累诊断数据
          </div>
        </div>
      </el-card>

      <!-- 组卷参数与生成 -->
      <el-card class="panel-card" shadow="never">
        <div class="gen-row">
          <span class="gen-label">题量</span>
          <el-radio-group v-model="questionCount" size="large">
            <el-radio-button :value="10">10 题</el-radio-button>
            <el-radio-button :value="15">15 题</el-radio-button>
            <el-radio-button :value="20">20 题</el-radio-button>
          </el-radio-group>
        </div>
        <div class="gen-row credit-row">
          <el-icon class="credit-icon"><GoldMedal /></el-icon>
          <span>每次消耗 <strong class="cost-text">{{ cost }}</strong> 积分 · 当前可用 <strong class="cost-text">{{ activeCredits }}</strong> 积分</span>
          <el-link v-if="activeCredits < cost" type="warning" :underline="false" @click="router.push('/pay')">积分不足，去购买 →</el-link>
        </div>
        <div class="gen-actions">
          <el-button
            type="primary"
            size="large"
            :loading="generating"
            @click="handleGenerate"
          >
            <el-icon v-if="!generating"><MagicStick /></el-icon>
            <span class="btn-text">{{ generating ? 'AI 正在选题组卷…（约 10~30 秒）' : 'AI 智能组卷并开始练习' }}</span>
          </el-button>
          <el-button size="large" @click="router.push('/wrong-questions')">去错题本</el-button>
        </div>
        <div class="gen-hint">AI 从题库选题（不会直接从错题本复制题目），若某知识点题量不足会自动以其他题目补足，保证题量充足</div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, GoldMedal } from '@element-plus/icons-vue'
import { getWeakPoints, generateSmartPaper } from '@/api/smartPaper'
import { getActiveCredit } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const weakPoints = ref([])
const weakLoading = ref(false)
const generating = ref(false)
const questionCount = ref(10)
const activeCredits = ref(0)

/** 与后端 SmartPaperServiceImpl.SMART_PAPER_COST 保持一致 */
const cost = 10

const TYPE_TEXT = { CHOICE: '选择题', JUDGE: '判断题', TEXT: '简答题' }
const DIFF_TEXT = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }

async function loadWeakPoints() {
  weakLoading.value = true
  try {
    const res = await getWeakPoints()
    if (res.code === 200) {
      weakPoints.value = res.data || []
    }
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    weakLoading.value = false
  }
}

async function loadCredits() {
  const userId = userStore.userInfo?.userId
  if (!userId) {
    activeCredits.value = 0
    return
  }
  try {
    const res = await getActiveCredit(userId)
    activeCredits.value = res.data?.activeCredits || 0
  } catch (e) {
    activeCredits.value = 0
  }
}

async function handleGenerate() {
  if (generating.value) return
  generating.value = true
  try {
    const res = await generateSmartPaper(questionCount.value)
    if (res.code === 200 && res.data) {
      const d = res.data
      const types = Object.entries(d.typeBreakdown || {})
        .map(([k, v]) => `${TYPE_TEXT[k] || k}${v}`).join('/')
      const diffs = Object.entries(d.difficultyBreakdown || {})
        .map(([k, v]) => `${DIFF_TEXT[k] || k}${v}`).join('/')
      ElMessage.success(
        `AI 已生成 ${d.questionCount} 题（错题 ${d.wrongQuestionCount} + 题库 ${d.bankQuestionCount}）· 消耗 ${d.cost} 积分${types ? '｜' + types : ''}${diffs ? '｜' + diffs : ''}`
      )
      router.push(`/exam/start/${d.paperId}`)
    }
  } catch (error) {
    if (error.message && error.message.includes('积分不足')) {
      ElMessageBox.confirm(
        `积分不足，AI 智能组卷每次需 ${cost} 积分。购买邀请码即可获得积分，是否现在去购买？`,
        '积分不足',
        { confirmButtonText: '去购买', cancelButtonText: '取消', type: 'warning' }
      )
        .then(() => router.push('/pay'))
        .catch(() => {})
    }
  } finally {
    generating.value = false
  }
}

onMounted(() => {
  loadWeakPoints()
  loadCredits()
})
</script>

<style scoped>
.smart-paper-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px 0 48px;
}

.page-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 0 16px;
}

.hero-banner {
  padding: 26px 30px;
  border-radius: 16px;
  background: linear-gradient(135deg, #7952b3 0%, #9d7fd6 60%, #b79ce8 100%);
  box-shadow: 0 12px 32px rgba(121, 82, 179, 0.25);
  margin-bottom: 16px;
  color: #fff;
}

.hero-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
}

.hero-icon {
  font-size: 24px;
}

.hero-sub {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.88);
}

.panel-card {
  margin-bottom: 16px;
  border-radius: 12px;
  border: none;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.weak-box {
  min-height: 56px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.weak-tag {
  font-size: 13px;
}

.weak-empty {
  font-size: 13px;
  color: #909399;
  line-height: 1.8;
}

.gen-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.gen-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.credit-row {
  margin-top: 18px;
  font-size: 13px;
  color: #606266;
}

.credit-icon {
  color: #e6a23c;
  font-size: 18px;
}

.cost-text {
  color: #e6a23c;
  font-size: 15px;
}

.gen-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}

.btn-text {
  margin-left: 4px;
}

.gen-hint {
  margin-top: 14px;
  font-size: 12px;
  color: #909399;
  text-align: center;
  line-height: 1.6;
}
</style>