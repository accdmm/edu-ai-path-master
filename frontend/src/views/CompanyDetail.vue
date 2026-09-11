<template>
  <div class="company-detail">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-nav">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item @click="$router.push('/interview-questions')">
          <span class="breadcrumb-link">企业题库</span>
        </el-breadcrumb-item>
        <el-breadcrumb-item>{{ company.name || '企业详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 公司信息头部 -->
    <div class="company-header" v-loading="loading">
      <div class="company-logo">
        <img :src="company.logo || '/default-company-logo.png'" :alt="company.name">
      </div>
      <div class="company-info">
        <h1 class="company-title">
          {{ company.name }} 面试真题
          <span class="plus-badge" v-if="company.isPremium">Plus</span>
        </h1>
        <p class="company-desc">{{ company.description || '暂无企业简介' }}</p>
        <div class="company-stats">
          <div class="stat-item">
            <span class="stat-number">{{ questions.length }}</span>
            <span class="stat-label">道真题</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">{{ totalViews }}</span>
            <span class="stat-label">总浏览</span>
          </div>
        </div>
      </div>
      <div class="company-actions">
        <el-button type="primary" size="large" @click="startAiInterview">
          开始 AI 面试
        </el-button>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <div class="filter-left">
        <el-select v-model="filters.direction" placeholder="技术方向" clearable style="width: 150px">
          <el-option label="全部" value=""></el-option>
          <el-option label="Java" value="java"></el-option>
          <el-option label="前端" value="frontend"></el-option>
          <el-option label="大数据" value="bigdata"></el-option>
          <el-option label="算法" value="algorithm"></el-option>
          <el-option label="运维" value="devops"></el-option>
          <el-option label="测试" value="testing"></el-option>
        </el-select>

        <el-select v-model="filters.difficulty" placeholder="难度" clearable style="width: 120px">
          <el-option label="全部" value=""></el-option>
          <el-option label="简单" value="easy"></el-option>
          <el-option label="中等" value="medium"></el-option>
          <el-option label="困难" value="hard"></el-option>
        </el-select>

        <el-select v-model="filters.year" placeholder="年份" clearable style="width: 120px">
          <el-option label="全部" value=""></el-option>
          <el-option
            v-for="year in yearOptions"
            :key="year"
            :label="year"
            :value="year">
          </el-option>
        </el-select>
      </div>

      <div class="filter-right">
        <el-input v-model="filters.keyword" placeholder="搜索题目内容" clearable style="width: 250px">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 真题列表 -->
    <div class="question-list" v-loading="loading">
      <div v-for="q in filteredQuestions" :key="q.id" class="question-card" @click="viewQuestionDetail(q)">
        <div class="card-tags">
          <el-tag size="small" type="info">{{ getTechLabel(q.direction) }}</el-tag>
          <el-tag size="small" :type="getDifficultyType(q.difficultyLevel)">
            {{ getDifficultyLabel(q.difficultyLevel) }}
          </el-tag>
          <el-tag v-if="q.interviewYear" size="small">{{ q.interviewYear }}年</el-tag>
        </div>
        <p class="question-text">{{ q.questionContent }}</p>
        <div class="card-footer">
          <span class="meta"><el-icon><View /></el-icon> {{ q.viewCount || 0 }} 浏览</span>
          <span class="meta"><el-icon><Star /></el-icon> {{ q.favoriteCount || 0 }} 收藏</span>
          <div class="card-actions">
            <el-button size="small" @click.stop="viewQuestionDetail(q)">查看详情</el-button>
            <el-button size="small" type="primary" @click.stop="practiceQuestion(q)">开始练习</el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && filteredQuestions.length === 0" class="empty-state">
        <el-empty description="该企业暂无真题" :image-size="120">
          <el-button type="primary" @click="$router.push('/interview-questions')">看看其他企业</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Star, View } from '@element-plus/icons-vue'
import request from '@/utils/request'

export default {
  name: 'CompanyDetail',
  components: { Search, Star, View },
  setup() {
    const route = useRoute()
    const router = useRouter()

    const loading = ref(false)
    const company = ref({})
    const questions = ref([])

    const currentYear = new Date().getFullYear()
    const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))

    const filters = reactive({
      direction: '',
      difficulty: '',
      year: '',
      keyword: ''
    })

    const totalViews = computed(() =>
      questions.value.reduce((sum, q) => sum + (q.viewCount || 0), 0))

    const filteredQuestions = computed(() => {
      return questions.value.filter(q => {
        if (filters.direction && q.direction !== filters.direction) return false
        if (filters.difficulty && q.difficultyLevel !== filters.difficulty) return false
        if (filters.year && String(q.interviewYear) !== filters.year) return false
        if (filters.keyword) {
          const kw = filters.keyword.toLowerCase()
          if (!String(q.questionContent || '').toLowerCase().includes(kw)) return false
        }
        return true
      })
    })

    const getTechLabel = (tech) => ({
      java: 'Java', frontend: '前端', bigdata: '大数据',
      algorithm: '算法', devops: '运维', testing: '测试'
    }[tech] || tech)

    const getDifficultyLabel = (difficulty) => ({
      easy: '简单', medium: '中等', hard: '困难'
    }[difficulty] || difficulty)

    const getDifficultyType = (difficulty) => ({
      easy: 'success', medium: 'warning', hard: 'danger'
    }[difficulty] || '')

    /** 加载企业信息 + 该企业真题（均走真实接口） */
    const loadData = async () => {
      const companyId = route.params.id
      loading.value = true
      try {
        const [companyRes, questionRes] = await Promise.all([
          request.get(`/api/companies/${companyId}`),
          request.get('/api/interview-questions/list', { params: { companyId, page: 1, size: 100 } })
        ])

        if (companyRes.code === 200) {
          company.value = companyRes.data || {}
        }

        const page = questionRes.data
        questions.value = (page && (page.records || page.list)) || []
      } catch (e) {
        company.value = {}
        questions.value = []
      } finally {
        loading.value = false
      }
    }

    const startAiInterview = () => router.push('/ai-interview')
    const viewQuestionDetail = (q) => router.push(`/interview-questions/${q.id}`)
    const practiceQuestion = (q) => router.push(`/interview-practice/${q.id}`)

    onMounted(loadData)

    return {
      loading, company, questions, yearOptions, filters,
      filteredQuestions, totalViews,
      startAiInterview, viewQuestionDetail, practiceQuestion,
      getTechLabel, getDifficultyLabel, getDifficultyType
    }
  }
}
</script>

<style scoped>
.company-detail {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: #f8f9fa;
  min-height: 100vh;
}

.breadcrumb-nav { margin-bottom: 24px; }
.breadcrumb-link { color: #1890ff; cursor: pointer; }
.breadcrumb-link:hover { text-decoration: underline; }

.company-header {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  margin-bottom: 24px;
  padding: 32px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.company-logo {
  width: 80px;
  height: 80px;
  border-radius: 16px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.company-logo img { width: 100%; height: 100%; object-fit: cover; }

.company-info { flex: 1; min-width: 0; }

.company-title {
  margin: 0 0 12px 0;
  font-size: 28px;
  font-weight: 700;
  color: #262626;
  display: flex;
  align-items: center;
  gap: 12px;
}

.plus-badge {
  background: #ff9800;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 8px;
  line-height: 1;
}

.company-desc {
  margin: 0 0 20px 0;
  font-size: 15px;
  color: #666;
  line-height: 1.6;
}

.company-stats { display: flex; gap: 32px; }
.stat-item { text-align: center; }
.stat-number {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #1890ff;
  margin-bottom: 4px;
}
.stat-label { font-size: 14px; color: #666; }

.company-actions { display: flex; flex-direction: column; gap: 12px; flex-shrink: 0; }

.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.filter-left { display: flex; gap: 16px; }
.filter-right { flex-shrink: 0; }

.question-list { display: flex; flex-direction: column; gap: 14px; }

.question-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 22px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}
.question-card:hover {
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.12);
  transform: translateY(-2px);
}

.card-tags { display: flex; gap: 8px; margin-bottom: 10px; }

.question-text {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #262626;
  line-height: 1.6;
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 18px;
}
.card-footer .meta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #999;
}
.card-actions { margin-left: auto; }

.empty-state {
  text-align: center;
  padding: 60px;
  background: #fff;
  border-radius: 16px;
}

@media (max-width: 768px) {
  .company-detail { padding: 16px; }
  .company-header { flex-direction: column; text-align: center; }
  .company-stats { justify-content: center; }
  .filter-section { flex-direction: column; align-items: stretch; }
  .filter-left { flex-direction: column; gap: 12px; }
  .card-actions { margin-left: 0; }
}
</style>
