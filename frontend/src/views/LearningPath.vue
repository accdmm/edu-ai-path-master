<template>
  <div class="learning-path-page">
    <div class="page-container">
      <!-- 生成中（后台任务，轮询等待） -->
      <div v-if="isGenerating" class="generate-loading">
        <el-icon class="loading-icon is-loading"><Loading /></el-icon>
        <div class="loading-title">AI 正在后台规划你的学习路径…</div>
        <el-progress
          :percentage="genProgress"
          :stroke-width="12"
          :color="progressColor"
          class="gen-progress"
        />
        <div class="loading-stage">{{ genStageText }}</div>
        <div class="loading-desc">已提交后台任务，你可以离开本页面去做其他事，回来后自动显示结果</div>
        <div class="loading-elapsed">已等待 {{ elapsed }} 秒（约需 1-3 分钟）</div>
      </div>

      <!-- 生成失败 -->
      <el-card v-else-if="isFailed" class="empty-card" shadow="never">
        <el-alert type="error" :closable="false" show-icon class="failed-alert">
          <template #title>{{ path.summary || '生成失败' }}</template>
          <div>本次生成未成功，旧路径数据仍保留在系统中。请点击下方按钮重新生成。</div>
        </el-alert>
        <div class="failed-actions">
          <el-button type="primary" @click="handleGenerate">重新生成</el-button>
        </div>
      </el-card>

      <template v-else>
        <!-- 路径头部 -->
        <el-card v-if="path" class="path-header" shadow="never">
          <div class="header-top">
            <div class="header-title">
              <el-icon class="title-icon"><Guide /></el-icon>
              <span>我的 AI 学习路径</span>
              <el-tag v-if="path.status === 'COMPLETED'" type="success" size="small">已完成</el-tag>
              <el-tag v-else type="primary" size="small">进行中</el-tag>
            </div>
            <div class="header-actions">
              <el-button :icon="Refresh" @click="handleRegenerate">重新生成</el-button>
            </div>
          </div>
          <div class="progress-row">
            <el-progress
              :percentage="progressPercent"
              :stroke-width="14"
              :color="progressColor"
              class="progress-bar"
            />
            <span class="progress-text">{{ path.completedCount || 0 }} / {{ path.nodeCount }} 节点已完成</span>
          </div>
          <div v-if="path.summary" class="summary-text">规划思路：{{ path.summary }}</div>
          <!-- 诊断快照 -->
          <div v-if="path.diagnosis && path.diagnosis.length" class="diagnosis-row">
            <span class="diagnosis-label">掌握度诊断（薄弱优先）：</span>
            <el-tag
              v-for="item in path.diagnosis"
              :key="item.categoryName"
              :color="rateColor(item.correctRate)"
              size="small"
              class="diagnosis-tag"
            >
              {{ item.categoryName }} {{ item.correctRate }}%
            </el-tag>
          </div>
        </el-card>

        <!-- 阶段与节点 -->
        <div v-if="path && path.phases && path.phases.length" class="phases">
          <el-card
            v-for="phase in path.phases"
            :key="phase.phase"
            class="phase-card"
            shadow="never"
          >
            <template #header>
              <div class="phase-header">
                <span class="phase-badge">{{ phase.phase }}</span>
                <span class="phase-title">{{ phase.phaseTitle }}</span>
              </div>
            </template>
            <div
              v-for="node in phase.nodes"
              :key="node.id"
              class="node-item"
              :class="{ 'node-completed': node.status === 'COMPLETED' }"
            >
              <div class="node-main">
                <div class="node-title-row">
                  <el-tag :type="typeMeta(node.nodeType).tagType" size="small" class="node-type-tag">
                    {{ typeMeta(node.nodeType).label }}
                  </el-tag>
                  <span class="node-title">{{ node.title }}</span>
                  <el-tag v-if="node.status === 'COMPLETED'" type="success" size="small" effect="light">
                    已完成
                  </el-tag>
                </div>
                <div v-if="node.description" class="node-desc">{{ node.description }}</div>
              </div>
              <div class="node-actions">
                <template v-if="node.nodeType === 'PAPER'">
                  <el-button type="primary" size="small" @click="goExam(node.refId)">去考试</el-button>
                </template>
                <template v-else>
                  <el-button
                    v-if="node.nodeType === 'QUESTION'"
                    type="success"
                    size="small"
                    @click="goQuestion(node.refId)"
                  >查看真题</el-button>
                  <el-tag v-if="node.nodeType === 'KNOWLEDGE'" type="info" size="small" effect="plain">知识点学习</el-tag>
                  <el-button
                    size="small"
                    :type="node.status === 'COMPLETED' ? '' : 'success'"
                    :plain="node.status === 'COMPLETED'"
                    @click="handleToggle(node)"
                  >{{ node.status === 'COMPLETED' ? '取消完成' : '标记完成' }}</el-button>
                </template>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 空态 -->
        <el-card v-if="!path" class="empty-card" shadow="never">
          <el-empty :description="emptyText">
            <div class="empty-actions">
              <el-button type="primary" size="large" :loading="diagLoading" @click="handleDiagnostic">
                一键生成诊断卷
              </el-button>
              <el-button size="large" @click="handleGenerate">直接生成学习路径</el-button>
            </div>
            <div class="empty-hint">没有答题数据？先做一套混合诊断卷（10 题，约 10 分钟），考完 AI 即可诊断你的薄弱知识点</div>
          </el-empty>
        </el-card>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Guide, Refresh } from '@element-plus/icons-vue'
import { generateLearningPath, getActiveLearningPath, togglePathNode, generateDiagnosticPaper } from '@/api/learningPath'

const router = useRouter()
const path = ref(null)
const loadError = ref(false)
const diagLoading = ref(false)
const elapsed = ref(0)
let timer = null
let pollTimer = null
let pollCount = 0
const POLL_INTERVAL = 3000
const MAX_POLLS = 100 // 3s * 100 = 5 分钟上限

const isGenerating = computed(() => path.value && path.value.status === 'GENERATING')
const isFailed = computed(() => path.value && path.value.status === 'FAILED')

// 生成进度（后端随阶段推进返回 10→30→70→90→结束）
const genProgress = computed(() => {
  if (!isGenerating.value) return 0
  return path.value?.progress ?? 10
})

const genStageText = computed(() => {
  const p = genProgress.value
  if (p < 30) return '正在检索题库候选资源…'
  if (p < 70) return 'AI 正在编排学习路径（耗时最长的环节）…'
  if (p < 90) return 'AI 编排完成，正在校验节点…'
  if (p < 100) return '正在保存路径…'
  return '即将完成…'
})

const emptyText = computed(() =>
  loadError.value
    ? '加载失败，点击重试'
    : '还没有学习路径。AI 将根据你的答题数据诊断薄弱知识点，规划一份分阶段、可执行的学习路径'
)

async function handleDiagnostic() {
  diagLoading.value = true
  try {
    const res = await generateDiagnosticPaper()
    if (res.code === 200 && res.data) {
      ElMessage.success('诊断卷已生成，即将开始考试')
      router.push(`/exam/start/${res.data}`)
    } else {
      ElMessage.error(res.message || '生成诊断卷失败')
    }
  } catch (e) {
    ElMessage.error('生成诊断卷失败，请重试')
  } finally {
    diagLoading.value = false
  }
}

const progressPercent = computed(() => {
  if (!path.value || !path.value.nodeCount) return 0
  return Math.round((path.value.completedCount || 0) * 100 / path.value.nodeCount)
})

const progressColor = [
  { color: '#f56c6c', percentage: 30 },
  { color: '#e6a23c', percentage: 70 },
  { color: '#67c23a', percentage: 100 }
]

function typeMeta(type) {
  switch (type) {
    case 'PAPER': return { label: '试卷实战', tagType: 'primary' }
    case 'QUESTION': return { label: '企业真题', tagType: 'success' }
    default: return { label: '知识点讲解', tagType: 'warning' }
  }
}

function rateColor(rate) {
  if (rate < 50) return '#fef0f0'
  if (rate < 80) return '#fdf6ec'
  return '#f0f9eb'
}

function startElapsed() {
  elapsed.value = 0
  stopElapsed()
  timer = setInterval(() => { elapsed.value++ }, 1000)
}

function stopElapsed() {
  if (timer) { clearInterval(timer); timer = null }
}

function startPolling() {
  stopPolling()
  pollCount = 0
  pollTimer = setInterval(async () => {
    pollCount++
    if (pollCount > MAX_POLLS) {
      stopPolling()
      ElMessage.error('等待超时，请稍后刷新页面查看结果')
      return
    }
    try {
      const res = await getActiveLearningPath()
      if (res.code === 200 && res.data) {
        // 每次轮询都刷新，驱动进度条随阶段推进（10→30→70→90）
        path.value = res.data
        if (res.data.status === 'ACTIVE' || res.data.status === 'COMPLETED') {
          stopPolling()
          stopElapsed()
          ElMessage.success('学习路径已生成')
        } else if (res.data.status === 'FAILED') {
          stopPolling()
          stopElapsed()
        }
      }
    } catch (e) { /* 轮询失败静默重试 */ }
  }, POLL_INTERVAL)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

async function loadPath() {
  try {
    const res = await getActiveLearningPath()
    if (res.code === 200) {
      path.value = res.data
      loadError.value = false
      // 进入页面时发现后台任务仍在生成（用户曾离开），继续轮询
      if (res.data && res.data.status === 'GENERATING') {
        startElapsed()
        startPolling()
      }
    } else {
      ElMessage.error(res.message || '加载学习路径失败')
      loadError.value = true
    }
  } catch (e) {
    loadError.value = true
  }
}

async function handleGenerate() {
  try {
    const res = await generateLearningPath()
    if (res.code === 200 && res.data) {
      path.value = res.data
      if (res.data.status === 'GENERATING') {
        startElapsed()
        startPolling()
      }
    } else if (res.code === 500 && res.message && res.message.includes('暂无答题数据')) {
      ElMessageBox.confirm(
        'AI 需要你的答题数据才能诊断薄弱点。可以立即做一套混合诊断卷（10 道题），考完后回来生成学习路径。',
        '暂无答题数据',
        { confirmButtonText: '生成诊断卷并开考', cancelButtonText: '知道了', type: 'warning' }
      ).then(handleDiagnostic).catch(() => {})
    } else {
      ElMessage.error(res.message || '生成失败，请重试')
    }
  } catch (e) {
    ElMessage.error('提交生成任务失败（网络异常），请重试')
  }
}

function handleRegenerate() {
  ElMessageBox.confirm(
    '将根据你最新的答题数据重新规划路径，当前路径会在新路径生成成功后自动归档（生成失败则保留旧路径）。',
    '重新生成学习路径',
    { confirmButtonText: '开始生成', cancelButtonText: '取消', type: 'info' }
  ).then(handleGenerate).catch(() => {})
}

function goExam(paperId) {
  if (paperId) router.push(`/exam/start/${paperId}`)
}

async function handleToggle(node) {
  try {
    const res = await togglePathNode(node.id)
    if (res.code === 200) {
      // 重新拉取：服务端已重算节点状态/进度/路径完成态（含 PAPER 自动同步）
      await loadPath()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  }
}

function goQuestion(questionId) {
  if (questionId) router.push(`/interview-questions/${questionId}`)
}

onMounted(loadPath)
onBeforeUnmount(() => { stopElapsed(); stopPolling() })
</script>

<style scoped>
.learning-path-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px 0;
}
.page-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 16px;
}
.generate-loading {
  background: #fff;
  border-radius: 8px;
  padding: 80px 24px;
  text-align: center;
}
.loading-icon {
  font-size: 42px;
  color: #409eff;
}
.loading-title {
  font-size: 18px;
  font-weight: 600;
  margin-top: 16px;
}
.gen-progress {
  width: 420px;
  max-width: 80%;
  margin: 20px auto 0;
}
.loading-stage {
  margin-top: 12px;
  color: #409eff;
  font-size: 13px;
}
.loading-desc {
  color: #909399;
  margin-top: 8px;
  font-size: 13px;
}
.loading-elapsed {
  color: #c0c4cc;
  margin-top: 12px;
  font-size: 12px;
}
.path-header {
  margin-bottom: 16px;
  border-radius: 8px;
}
.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}
.title-icon {
  color: #409eff;
  font-size: 20px;
}
.progress-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
}
.progress-bar {
  flex: 1;
}
.progress-text {
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
}
.summary-text {
  margin-top: 14px;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px 12px;
}
.diagnosis-row {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.diagnosis-label {
  font-size: 13px;
  color: #909399;
}
.diagnosis-tag {
  border: none;
  color: #303133;
}
.phases {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.phase-card {
  border-radius: 8px;
}
.phase-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.phase-badge {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.phase-title {
  font-weight: 600;
  font-size: 15px;
}
.node-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 10px;
  transition: background 0.2s;
}
.node-item:last-child {
  margin-bottom: 0;
}
.node-item:hover {
  background: #f5f7fa;
}
.node-completed {
  opacity: 0.65;
  background: #f0f9eb;
}
.node-main {
  flex: 1;
  min-width: 0;
}
.node-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.node-title {
  font-weight: 600;
  font-size: 14px;
}
.node-desc {
  margin-top: 8px;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}
.node-actions {
  flex-shrink: 0;
}
.empty-card {
  border-radius: 8px;
}
.empty-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
.empty-hint {
  margin-top: 12px;
  color: #909399;
  font-size: 12px;
}
.failed-alert {
  margin-bottom: 16px;
}
.failed-actions {
  text-align: center;
}
</style>
