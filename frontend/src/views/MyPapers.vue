<template>
  <div class="my-papers-container">
    <div class="page-header">
      <div>
        <h2>我的 AI 试卷</h2>
        <p>由 AI 客服小胡为你智能生成的私有试卷，仅你本人可见</p>
      </div>
      <el-button type="primary" @click="$router.push('/chat')">
        <el-icon><ChatDotRound /></el-icon>
        去问小胡生成试卷
      </el-button>
    </div>

    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="4" animated />
    </div>

    <el-empty v-else-if="papers.length === 0" description="还没有 AI 生成试卷，去聊天里让小胡帮你出一套吧">
      <el-button type="primary" @click="$router.push('/chat')">立即去生成</el-button>
    </el-empty>

    <div v-else class="paper-grid">
      <div v-for="paper in papers" :key="paper.id" class="paper-card">
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
        <div class="card-actions">
          <el-button type="primary" size="small" @click="startExam(paper.id)">开始考试</el-button>
          <el-button size="small" @click="viewDetail(paper.id)">查看详情</el-button>
        </div>
      </div>
    </div>
  </div>
  <BackHome />
  </template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyAiPapers } from '@/api/paper'
import { ChatDotRound, Document, CollectionTag, TrophyBase, Timer } from '@element-plus/icons-vue'

const router = useRouter()
const papers = ref([])
const loading = ref(true)

const loadPapers = async () => {
  try {
    const res = await getMyAiPapers()
    papers.value = res.data || []
  } catch (error) {
    console.error('加载AI试卷失败:', error)
    ElMessage.error('加载试卷失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const startExam = (paperId) => {
  router.push(`/exam/start/${paperId}`)
}

const viewDetail = (paperId) => {
  router.push(`/paper/detail/${paperId}`)
}

onMounted(() => {
  loadPapers()
})
</script>

<style scoped>
.my-papers-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 30px 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.page-header h2 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.loading-wrap {
  padding: 20px 0;
}

.paper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.paper-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid #ebeef5;
  transition: all 0.3s;
}

.paper-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
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
}

.paper-desc {
  margin: 12px 0;
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
  min-height: 40px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.paper-meta {
  display: flex;
  gap: 16px;
  color: #606266;
  font-size: 13px;
  padding: 10px 0;
  border-top: 1px solid #f0f2f5;
}

.paper-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-actions {
  display: flex;
  gap: 10px;
  margin-top: 6px;
}
</style>