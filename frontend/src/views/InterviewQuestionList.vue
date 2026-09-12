<template>
  <div class="interview-questions">
    <!-- 顶部导航 -->
    <div class="top-nav">
      <div class="nav-tabs">
        <div class="tab active">热门企业</div>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <el-select v-model="filters.direction" placeholder="技术方向" clearable style="width: 180px" @change="handleFilterChange">
        <el-option label="全部" value=""></el-option>
              <el-option label="Java" value="java"></el-option>
              <el-option label="前端" value="frontend"></el-option>
              <el-option label="大数据" value="bigdata"></el-option>
              <el-option label="算法" value="algorithm"></el-option>
              <el-option label="运维" value="devops"></el-option>
              <el-option label="测试" value="testing"></el-option>
            </el-select>
      
      <el-select v-model="filters.year" placeholder="面试年份" clearable style="width: 120px" @change="handleFilterChange">
        <el-option label="全部" value=""></el-option>
              <el-option
                v-for="year in yearOptions"
                :key="year"
                :label="year"
          :value="year">
        </el-option>
            </el-select>
          
      <el-select v-model="filters.difficulty" placeholder="难度级别" clearable style="width: 120px" @change="handleFilterChange">
        <el-option label="全部" value=""></el-option>
              <el-option label="简单" value="easy"></el-option>
              <el-option label="中等" value="medium"></el-option>
              <el-option label="困难" value="hard"></el-option>
            </el-select>
      
      <el-input v-model="filters.keyword" placeholder="搜索企业或题目" clearable style="width: 200px" @input="handleFilterChange">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <!-- 删除上传我的真题按钮，原本在筛选区 -->
      <!-- <el-button type="primary" style="margin-left: auto;" @click="showUploadDialog = true">上传我的真题</el-button> -->
    </div>

    <!-- 企业列表 -->
    <div v-if="!questionsMode" class="company-list" v-loading="loading">
      <div 
        v-for="company in filteredCompanies" 
        :key="company.id"
        class="company-item"
      >
        <!-- 企业头部 -->
        <div class="company-header" @click="toggleCompany(company)">
          <div class="company-info">
            <div v-if="company.logo" class="company-logo-wrap">
              <img :src="company.logo" :alt="company.name" class="company-logo" @error="company.logo = ''" />
            </div>
            <div v-else class="company-logo company-logo-fallback">{{ company.name.charAt(0) }}</div>
            <div class="company-details">
              <h3 class="company-name">{{ company.name }}</h3>
              <p class="company-desc">{{ company.description }}</p>
            </div>
          </div>
          <div class="company-stats">
            <span class="stat-item">{{ company.totalQuestions }}道题目</span>
            <span class="stat-item">{{ company.questionSets?.length || 0 }}个分类</span>
          </div>
          <div class="expand-icon">
            <el-icon>
              <component :is="company.expanded ? 'ArrowUp' : 'ArrowDown'" />
            </el-icon>
          </div>
        </div>

        <!-- 题目分类列表（展开时显示） -->
        <div v-if="company.expanded" class="question-categories">
          <div 
            v-for="category in company.questionSets" 
            :key="category.id"
            class="category-item"
            @click="viewCategoryQuestions(company, category)"
          >
            <div class="category-info">
              <h4 class="category-name">{{ category.name }}</h4>
              <div class="category-meta">
                <el-tag size="small" :type="getDifficultyType(category.difficulty)">
                  {{ getDifficultyLabel(category.difficulty) }}
                </el-tag>
                <el-tag size="small" type="info">{{ getTechLabel(category.direction) }}</el-tag>
                <span class="category-year">{{ category.year }}年</span>
            </div>
            </div>
            <div class="category-stats">
              <span class="question-count">{{ category.questionCount }}题</span>
              <el-icon class="view-icon"><ArrowRight /></el-icon>
            </div>
    </div>

          <!-- 空状态 -->
          <div v-if="!company.questionSets || company.questionSets.length === 0" class="empty-categories">
            <p>暂无题目分类</p>
          </div>
                </div>
              </div>
      
      <!-- 空状态 -->
      <div v-if="filteredCompanies.length === 0" class="empty-state">
        <el-empty description="暂无企业数据" :image-size="120">
        </el-empty>
        </div>
    </div>

    <!-- 真题列表（从企业分类进入） -->
    <div v-else class="question-list" v-loading="loading">
      <div class="question-list-header">
        <el-button size="small" @click="backToCompanies" icon="Back">返回企业列表</el-button>
        <span v-if="currentCompanyName" class="question-list-title">{{ currentCompanyName }}</span>
      </div>
      <div v-if="questionList.length === 0" class="empty-state">
        <el-empty description="该分类下暂无真题" :image-size="120"></el-empty>
      </div>
      <div
        v-for="q in questionList"
        :key="q.id"
        class="question-card"
        @click="viewQuestionDetail(q.id)"
      >
        <div class="question-card-main">
          <h4 class="question-card-title">{{ q.questionContent }}</h4>
          <div class="question-card-meta">
            <el-tag v-if="q.direction" size="small" type="info">{{ getTechLabel(q.direction) }}</el-tag>
            <el-tag v-if="q.difficultyLevel" size="small" :type="getDifficultyType(q.difficultyLevel)">
              {{ getDifficultyLabel(q.difficultyLevel) }}
            </el-tag>
            <span v-if="q.viewCount" class="question-card-views">{{ q.viewCount }}次浏览</span>
          </div>
        </div>
        <el-icon class="question-card-arrow"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 删除上传我的真题弹窗及表单 -->
    <!--
    <el-dialog v-model="showUploadDialog" title="上传我的真题" width="500px">
      <el-form :model="uploadForm" ref="uploadFormRef" label-width="100px">
        <el-form-item label="题目描述">
          <el-input v-model="uploadForm.content" type="textarea" :rows="4" placeholder="请输入题目内容或描述" />
        </el-form-item>
        <el-form-item label="图片上传">
          <el-upload
            class="upload-demo"
            drag
            action="#"
            :auto-upload="false"
            :file-list="uploadForm.images"
            :on-change="handleImageChange"
            :limit="3"
            accept="image/*"
          >
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">将图片拖到此处，或<em>点击上传</em></div>
            <div class="el-upload__tip" slot="tip">支持jpg/png格式，最多3张</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpload">提交</el-button>
      </template>
    </el-dialog>
    -->
  </div>
  <BackHome />
  </template>

<script>
import { ref, reactive, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ArrowDown, ArrowUp, ArrowRight, Back } from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'InterviewQuestionList',
  components: {
    Search,
    ArrowDown,
    ArrowUp,
    ArrowRight,
    Back
  },
  setup() {
    // 响应式数据
    const loading = ref(false)
    const companyList = ref([])
    const route = useRoute()
    const router = useRouter()
    // 真题列表模式（从企业分类进入）
    const questionsMode = ref(false)
    const questionList = ref([])
    const currentCompanyName = ref('')
    
    // 动态生成今年及过去5年的年份选项
    const currentYear = new Date().getFullYear()
    const yearOptions = Array.from({ length: 6 }, (_, i) => String(currentYear - i))
    
    // 筛选条件
    const filters = reactive({
      direction: '',
      year: '',
      difficulty: '',
      keyword: ''
    })
    
    // 添加企业相关
    const addCompanyDialogVisible = ref(false)
    const addingCompany = ref(false)
    const companyFormRef = ref()
    const companyForm = reactive({
      name: '',
      description: '',
      logo: ''
    })
    
    const companyRules = {
      name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
      description: [{ required: true, message: '请输入企业描述', trigger: 'blur' }]
    }
    
    // 批量录入相关
    const batchUploadDialogVisible = ref(false)
    const batchUploading = ref(false)
    const batchFormRef = ref()
    const currentCompany = ref(null)
    const batchForm = reactive({
      categoryName: '',
      direction: '',
      difficulty: '',
      year: '',
      round: '',
      questions: [
        { content: '' }
      ]
    })
    
    const batchRules = {
      categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
      direction: [{ required: true, message: '请选择技术方向', trigger: 'change' }],
      difficulty: [{ required: true, message: '请选择难度级别', trigger: 'change' }],
      year: [{ required: true, message: '请选择面试年份', trigger: 'change' }]
    }
    
    // 批量输入相关
    const batchInputDialogVisible = ref(false)
    const batchInputText = ref('')
    
    // 加载企业及题集分类（真实数据）
    const fetchCompanies = async () => {
      loading.value = true
      try {
        const res = await axios.get('/api/companies/list', { params: { page: 1, size: 100 } })
        const companies = res.data.code === 200 ? (res.data.data.records || []) : []
        const list = []
        for (const c of companies) {
          const catRes = await axios.get('/api/company-question-categories/enabled', {
            params: { companyId: c.id }
          })
          const sets = (catRes.data.code === 200 ? catRes.data.data : []) || []
          list.push({
            id: c.id,
            name: c.name,
            logo: c.logo || '',
            description: c.description || '',
            totalQuestions: c.totalQuestions || 0,
            expanded: false,
            questionSets: sets.map(cat => ({
              id: cat.id,
              name: cat.name,
              direction: cat.direction || '',
              difficulty: cat.difficulty || '',
              year: cat.year || '',
              questionCount: cat.questionCount || 0,
              companyId: c.id
            }))
          })
        }
        companyList.value = list
      } catch (e) {
        console.error('加载企业数据失败:', e)
        ElMessage.error('加载企业数据失败，请检查网络连接')
      } finally {
        loading.value = false
      }
    }

    // 加载真题列表（按企业/方向/关键词筛选）
    const fetchQuestions = async () => {
      loading.value = true
      try {
        const params = { page: 1, size: 50, companyId: route.query.companyId || undefined }
        if (route.query.direction) params.direction = route.query.direction
        if (filters.keyword) params.keyword = filters.keyword
        const res = await axios.get('/api/interview-questions/list', { params })
        if (res.data.code === 200) {
          questionList.value = res.data.data.records || []
        } else {
          ElMessage.error('加载真题失败：' + res.data.message)
        }
      } catch (e) {
        console.error('加载真题失败:', e)
        ElMessage.error('加载真题失败，请检查网络连接')
      } finally {
        loading.value = false
      }
    }

    // 进入真题列表模式
    const enterQuestionsMode = async () => {
      questionsMode.value = true
      await fetchQuestions()
    }

    // 返回企业列表模式
    const backToCompanies = () => {
      questionsMode.value = false
      questionList.value = []
      filters.keyword = ''
      router.replace('/interview-questions')
    }

    // 查看某企业分类下的真题
    const viewCategoryQuestions = (company, category) => {
      currentCompanyName.value = `${company.name} · ${category.name}`
      router.push({
        path: '/interview-questions',
        query: {
          companyId: company.id,
          direction: category.direction || ''
        }
      })
    }

    // 查看真题详情
    const viewQuestionDetail = (id) => {
      router.push(`/interview-questions/${id}`)
    }

    // 计算过滤后的企业列表
    const filteredCompanies = computed(() => {
      return companyList.value.filter(company => {
        // 关键词筛选
        if (filters.keyword && !company.name.toLowerCase().includes(filters.keyword.toLowerCase())) {
          return false
        }
        
        // 技术方向筛选
        if (filters.direction) {
          const hasMatchingDirection = company.questionSets?.some(set => set.direction === filters.direction)
          if (!hasMatchingDirection) return false
    }
    
        // 难度筛选
        if (filters.difficulty) {
          const hasMatchingDifficulty = company.questionSets?.some(set => set.difficulty === filters.difficulty)
          if (!hasMatchingDifficulty) return false
    }
    
        // 年份筛选
        if (filters.year) {
          const hasMatchingYear = company.questionSets?.some(set => set.year === parseInt(filters.year))
          if (!hasMatchingYear) return false
        }
        
        return true
      })
    })
    
    // 工具函数
    const getTechLabel = (tech) => {
      const labels = {
        java: 'Java',
        frontend: '前端',
        bigdata: '大数据',
        algorithm: '算法',
        devops: '运维',
        testing: '测试'
      }
      return labels[tech] || tech
    }
    
    const getDifficultyLabel = (difficulty) => {
      const labels = {
        easy: '简单',
        medium: '中等',
        hard: '困难'
      }
      return labels[difficulty] || difficulty
    }
    
    const getDifficultyType = (difficulty) => {
      const types = {
        easy: 'success',
        medium: 'warning',
        hard: 'danger'
      }
      return types[difficulty] || ''
    }
    
    // 事件处理函数
    const handleFilterChange = () => {
      // 真题列表模式：关键词筛选重新请求
      if (questionsMode.value) {
        fetchQuestions()
      }
    }
    
    const toggleCompany = (company) => {
      company.expanded = !company.expanded
    }
    
    const showAddCompanyDialog = () => {
      addCompanyDialogVisible.value = true
    }
    
    const handleAddCompany = async () => {
      try {
        await companyFormRef.value.validate()
        addingCompany.value = true
        
        // 模拟添加企业API调用
        const newCompany = {
          id: Date.now(),
          name: companyForm.name,
          description: companyForm.description,
          logo: companyForm.logo || '/default-logo.png',
          totalQuestions: 0,
          expanded: false,
          questionSets: []
        }
        
        companyList.value.push(newCompany)
        
        ElMessage.success('企业添加成功')
        addCompanyDialogVisible.value = false
        
        // 重置表单
        Object.keys(companyForm).forEach(key => {
          companyForm[key] = ''
        })
        
      } catch (error) {
        ElMessage.error('添加失败：' + error.message)
      } finally {
        addingCompany.value = false
      }
    }
    
    const showBatchUploadDialog = (company) => {
      currentCompany.value = company
      batchUploadDialogVisible.value = true
    }
    
    // 批量录入相关方法
    const addQuestion = () => {
      batchForm.questions.push({ content: '' })
    }
    
    const removeQuestion = (index) => {
      if (batchForm.questions.length > 1) {
        batchForm.questions.splice(index, 1)
      }
    }
    
    const showBatchInputDialog = () => {
      batchInputDialogVisible.value = true
    }
    
    const handleBatchInput = () => {
      if (!batchInputText.value.trim()) {
        ElMessage.warning('请输入题目内容')
        return
      }
      
      // 解析批量输入的文本
      const lines = batchInputText.value.split('\n').filter(line => line.trim())
      const questions = lines.map(line => {
        // 移除序号（如 "1. "）
        const content = line.replace(/^\d+\.\s*/, '').trim()
        return { content }
      }).filter(q => q.content)
      
      if (questions.length === 0) {
        ElMessage.warning('未解析到有效题目')
        return
      }
      
      batchForm.questions = questions
      batchInputDialogVisible.value = false
      batchInputText.value = ''
      
      ElMessage.success(`成功导入 ${questions.length} 道题目`)
    }
    
    const handleBatchSubmit = async () => {
      try {
        await batchFormRef.value.validate()
        
        // 验证题目内容
        const validQuestions = batchForm.questions.filter(q => q.content.trim())
        if (validQuestions.length === 0) {
          ElMessage.warning('请至少添加一道题目')
          return
        }
        
        batchUploading.value = true
        
        // 模拟API调用
        const newCategory = {
          id: Date.now(),
          name: batchForm.categoryName,
          direction: batchForm.direction,
          difficulty: batchForm.difficulty,
          year: parseInt(batchForm.year),
          round: batchForm.round,
          questionCount: validQuestions.length
        }
        
        // 更新企业数据
        if (currentCompany.value) {
          if (!currentCompany.value.questionSets) {
            currentCompany.value.questionSets = []
          }
          currentCompany.value.questionSets.push(newCategory)
          currentCompany.value.totalQuestions += validQuestions.length
        }
        
        ElMessage.success('题目保存成功')
        batchUploadDialogVisible.value = false
        
        // 重置表单
        Object.keys(batchForm).forEach(key => {
          if (key === 'questions') {
            batchForm[key] = [{ content: '' }]
          } else {
            batchForm[key] = ''
          }
        })
        
      } catch (error) {
        ElMessage.error('保存失败：' + error.message)
      } finally {
        batchUploading.value = false
      }
    }
    
    // 图片选择事件
    const handleImageChange = (file, fileList) => {
      // 删除与上传相关的响应式数据、方法、import等
    }
    
    // 提交上传
    const handleUpload = () => {
      // 删除与上传相关的响应式数据、方法、import等
    }
    
    // 生命周期：监听路由 query，企业列表模式 <-> 真题列表模式
    // （点分类是同一路由 query 变化，组件复用，onMounted 不会重跑，必须用 watch）
    watch(() => route.query.companyId, async (companyId) => {
      if (companyId) {
        questionsMode.value = true
        await fetchQuestions()
      } else {
        questionsMode.value = false
        questionList.value = []
        filters.keyword = ''
        await fetchCompanies()
      }
    }, { immediate: true })
    
    return {
      loading,
      companyList,
      yearOptions,
      filters,
      filteredCompanies,
      questionsMode,
      questionList,
      currentCompanyName,
      addCompanyDialogVisible,
      addingCompany,
      companyFormRef,
      companyForm,
      companyRules,
      batchUploadDialogVisible,
      batchUploading,
      batchFormRef,
      batchForm,
      batchRules,
      batchInputDialogVisible,
      batchInputText,
      handleFilterChange,
      toggleCompany,
      showAddCompanyDialog,
      handleAddCompany,
      showBatchUploadDialog,
      viewCategoryQuestions,
      viewQuestionDetail,
      backToCompanies,
      addQuestion,
      removeQuestion,
      showBatchInputDialog,
      handleBatchInput,
      handleBatchSubmit,
      getTechLabel,
      getDifficultyLabel,
      getDifficultyType,
      // 删除与上传相关的响应式数据、方法、import等
    }
  }
}
</script>

<style scoped>
.interview-questions {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: #f8f9fa;
  min-height: 100vh;
}

/* 顶部导航样式 */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: #fff;
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.nav-tabs {
  display: flex;
  gap: 32px;
}

.tab {
  padding: 8px 0;
  font-size: 16px;
  font-weight: 500;
  color: #8c8c8c;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.3s ease;
}

.tab.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.tab:hover {
  color: #1890ff;
}

/* 筛选区域样式 */
.filter-section {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  align-items: center;
}

/* 企业列表样式 */
.company-list {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.company-item {
  border-bottom: 1px solid #f0f0f0;
}

.company-item:last-child {
  border-bottom: none;
}

.company-header {
  display: flex;
  align-items: center;
  padding: 20px 24px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.company-header:hover {
  background-color: #f8f9fa;
}

.company-info {
  display: flex;
  align-items: center;
  flex: 1;
}

.company-logo {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  margin-right: 16px;
  object-fit: cover;
}

.company-logo-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #1890ff;
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  flex-shrink: 0;
}

/* 真题列表样式 */
.question-list {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 16px 24px;
}

.question-list-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.question-list-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.question-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.question-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border-color: #1890ff;
}

.question-card-main {
  flex: 1;
  min-width: 0;
  margin-right: 12px;
}

.question-card-title {
  margin: 0 0 8px 0;
  font-size: 15px;
  font-weight: 500;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.question-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-card-views {
  font-size: 12px;
  color: #999;
}

.question-card-arrow {
  color: #999;
  flex-shrink: 0;
}

.company-details {
  flex: 1;
}

.company-name {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.company-desc {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.company-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-right: 16px;
}

.stat-item {
  font-size: 14px;
  color: #666;
}

.expand-icon {
  color: #999;
  transition: transform 0.3s ease;
}

/* 题目分类列表样式 */
.question-categories {
  padding: 0 24px 20px 24px;
  background: #fafafa;
}

.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.category-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.category-item:last-child {
  margin-bottom: 0;
}

.category-info {
  flex: 1;
}

.category-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
  color: #262626;
}

.category-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-year {
  font-size: 12px;
  color: #999;
}

.category-stats {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-count {
  font-size: 14px;
  color: #666;
}

.view-icon {
  color: #999;
}

/* 空状态样式 */
.empty-categories {
  text-align: center;
  padding: 40px;
  color: #999;
}

.empty-state {
  text-align: center;
  padding: 60px;
}

/* 表单样式 */
.form-section {
  margin-bottom: 24px;
}

.form-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.questions-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.question-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: flex-start;
}

.question-item:last-child {
  margin-bottom: 0;
}

.question-number {
  font-size: 14px;
  font-weight: 500;
  color: #666;
  margin-top: 8px;
  flex-shrink: 0;
  width: 24px;
}

.question-input {
  flex: 1;
}

.question-actions {
  flex-shrink: 0;
  margin-top: 4px;
}

/* 批量输入提示 */
.batch-input-tip {
  margin-bottom: 12px;
  padding: 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
}

.batch-input-tip p {
  margin: 0;
  font-size: 14px;
  color: #52c41a;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .interview-questions {
    padding: 16px;
  }
  
  .filter-section {
    flex-direction: column;
    gap: 12px;
  }
  
  .top-nav {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .nav-tabs {
    justify-content: center;
  }
  
  .company-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .company-stats {
    justify-content: space-between;
    margin-right: 0;
  }
}

/* 滚动条样式 */
.questions-list::-webkit-scrollbar {
  width: 6px;
}

.questions-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.questions-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.questions-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style> 
