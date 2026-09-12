<template>
  <!-- 考场桌面背景（浅蓝） -->
  <div class="exam-page-bg" aria-hidden="true"></div>
  <!-- 考场密封线（装饰） -->
  <div class="seal-line" aria-hidden="true"><span>- - - - 密&nbsp;封&nbsp;线&nbsp;内&nbsp;不&nbsp;得&nbsp;答&nbsp;题 - - - -</span></div>
  <div class="exam-start-container">
    <div class="exam-start-card">
      <div class="card-header">
        <h2 class="title">开始考试</h2>
        <p class="subtitle">请填写您的信息开始考试</p>
      </div>
      
      <div class="paper-info" v-if="paperInfo">
        <h3 class="paper-name">{{ paperInfo.name }}</h3>
        <p class="paper-description">{{ paperInfo.description || '暂无描述' }}</p>
        <div class="paper-meta">
          <span><el-icon><CollectionTag /></el-icon> 题目数量: {{ paperInfo.questionCount }} 道</span>
          <span><el-icon><TrophyBase /></el-icon> 总分: {{ paperInfo.totalScore }} 分</span>
          <span><el-icon><Timer /></el-icon> 考试时长: {{ paperInfo.duration }} 分钟</span>
        </div>
      </div>

      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        class="exam-form"
        @submit.prevent="handleStartExam"
      >
        <el-form-item label="考生姓名" prop="studentName">
          <el-input 
            v-model="form.studentName" 
            placeholder="请输入您的姓名"
            size="large"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            size="large" 
            @click="handleStartExam" 
            :loading="loading"
            style="width: 100%"
          >
            开始考试
          </el-button>
        </el-form-item>
      </el-form>

      <div class="exam-rules">
        <h4>考试规则</h4>
        <ul>
          <li>请确保网络连接稳定</li>
          <li>考试过程中请勿切换窗口或刷新页面</li>
          <li>考试时间到后将自动交卷</li>
          <li>提交后将无法修改答案</li>
        </ul>
      </div>
    </div>
  </div>
  <BackHome />
  </template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPaperById } from '../api/paper.js'
import { startExam } from '../api/exam.js'
import { CollectionTag, TrophyBase, Timer } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const loading = ref(false)
const paperInfo = ref(null)

// 表单数据
const form = ref({
  studentName: '' // 考生姓名
})

// 表单验证规则
const rules = {
  studentName: [
    { required: true, message: '请输入考生姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度在 2 到 20 个字符', trigger: 'blur' }
  ]
}

// 获取试卷信息
const getPaperInfo = async () => {
  try {
    const paperId = route.params.paperId
    const res = await getPaperById(paperId)
    paperInfo.value = res.data
  } catch (error) {
    ElMessage.error('获取试卷信息失败')
    router.push('/exam/list')
  }
}

// 开始考试
const handleStartExam = async () => {
  try {
    // 验证表单
    await formRef.value.validate()
    
    loading.value = true
    const paperId = route.params.paperId
    
    // 调用开始考试API
    const res = await startExam(paperId, form.value.studentName)
    
    ElMessage.success('考试创建成功，正在跳转...')
    
    // 跳转到考试页面
    router.push(`/exam/${res.data.id}`)
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    } else {
      ElMessage.error('开始考试失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getPaperInfo()
})
</script>

<style scoped>
/* ============================================================
   开始考试页 · 「蓝白考卷」设计（与答题页同一套语言）
   浅蓝桌面 + 白色考卷卡 + 衬线标题 + 密封线
   ============================================================ */

/* 浅蓝桌面 */
.exam-page-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  background:
    radial-gradient(1100px 520px at 50% -8%, rgba(64, 158, 255, 0.10), transparent 62%),
    repeating-linear-gradient(0deg, transparent 0 34px, rgba(47, 107, 223, 0.035) 34px 35px),
    linear-gradient(175deg, #eaf1f9 0%, #e2ebf5 60%, #d8e4f0 100%);
}

/* 密封线 */
.seal-line {
  position: fixed;
  left: 18px;
  top: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  z-index: 1;
  border-left: 2px dashed rgba(47, 107, 223, 0.35);
  padding-left: 10px;
  user-select: none;
}
.seal-line span {
  writing-mode: vertical-rl;
  letter-spacing: 6px;
  font-size: 12px;
  color: rgba(47, 86, 143, 0.55);
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
}

.exam-start-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
}

/* 考卷卡 */
.exam-start-card {
  width: 100%;
  max-width: 560px;
  background:
    linear-gradient(0deg, rgba(47, 107, 223, 0.02) 0 1px, transparent 1px 26px),
    #ffffff;
  border-radius: 4px;
  padding: 34px 40px 30px;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.8) inset,
    0 18px 40px -18px rgba(31, 59, 115, 0.28),
    0 4px 12px rgba(31, 59, 115, 0.14);
}

/* 卷头：衬线标题 + 蓝色双线 */
.card-header {
  text-align: center;
  padding-bottom: 16px;
  margin-bottom: 22px;
  border-bottom: 3px solid #2f6bdf;
  box-shadow: 0 5px 0 -3px #2f6bdf, 0 9px 0 -6px rgba(47, 107, 223, 0.3);
}
.title {
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 8px;
  text-indent: 8px;
  color: #1f2d3d;
  margin: 0 0 6px;
}
.subtitle {
  color: #8492a6;
  font-size: 13px;
  letter-spacing: 2px;
  margin: 0;
}

/* 试卷信息：浅蓝信息卡 + 蓝左线 */
.paper-info {
  background: #f4f8fd;
  border: 1px solid rgba(47, 107, 223, 0.18);
  border-left: 4px solid #2f6bdf;
  border-radius: 4px;
  padding: 16px 20px;
  margin-bottom: 24px;
}
.paper-name {
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
  margin: 0 0 6px;
}
.paper-description {
  color: #5a6b85;
  font-size: 13.5px;
  margin: 0 0 12px;
  line-height: 1.6;
}
.paper-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  color: #2f6bdf;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}
.paper-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

/* 表单 */
.exam-form :deep(.el-form-item__label) {
  color: #3d4c63;
  font-weight: 600;
}
.exam-form :deep(.el-input__wrapper) {
  border-radius: 4px;
}

/* 开始按钮：蓝色衬线 */
.exam-form :deep(.el-button--primary) {
  background: #2f6bdf;
  border-color: #2f6bdf;
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 8px;
  text-indent: 8px;
  border-radius: 4px;
  box-shadow: 0 6px 14px -4px rgba(47, 107, 223, 0.5);
}
.exam-form :deep(.el-button--primary:hover) {
  background: #4a80e8;
  border-color: #4a80e8;
}

/* 考试规则：浅黄提示卡 */
.exam-rules {
  background: #fffaf0;
  border: 1px solid #f0e2b8;
  border-radius: 4px;
  padding: 16px 20px;
  margin-top: 24px;
}
.exam-rules h4 {
  font-size: 14px;
  letter-spacing: 2px;
  color: #a0701c;
  margin: 0 0 10px;
}
.exam-rules ul {
  margin: 0;
  padding-left: 18px;
}
.exam-rules li {
  color: #6b6250;
  font-size: 13.5px;
  line-height: 1.9;
}

/* 窄屏 */
@media (max-width: 640px) {
  .exam-start-card { padding: 26px 20px 24px; }
  .seal-line { display: none; }
}
</style>
