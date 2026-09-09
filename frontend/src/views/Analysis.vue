<template>
  <div class="analysis-page">
    <div class="page-header">
      <h1>📊 AI 学习分析</h1>
      <p>智能学习报告 · 能力雷达图 · 个性化学习建议</p>
    </div>

    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>正在生成学习报告，请稍候...</p>
    </div>

    <template v-else>
      <!-- 学习概览 -->
      <div class="overview-grid">
        <div class="stat-card">
          <div class="stat-icon exam"><Trophy /></div>
          <div class="stat-info">
            <div class="stat-value">{{ report.overview.examCount }}</div>
            <div class="stat-label">考试次数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon rate"><TrendCharts /></div>
          <div class="stat-info">
            <div class="stat-value">{{ report.overview.avgScoreRate }}%</div>
            <div class="stat-label">平均得分率</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon answer"><Edit /></div>
          <div class="stat-info">
            <div class="stat-value">{{ report.overview.answerCount }}</div>
            <div class="stat-label">累计答题</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon correct"><CircleCheck /></div>
          <div class="stat-info">
            <div class="stat-value">{{ report.overview.correctRate }}%</div>
            <div class="stat-label">整体正确率</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon ai"><Cpu /></div>
          <div class="stat-info">
            <div class="stat-value">{{ report.overview.aiPaperCount }}</div>
            <div class="stat-label">AI 试卷</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon interview"><Microphone /></div>
          <div class="stat-info">
            <div class="stat-value">{{ report.overview.interviewCount }}</div>
            <div class="stat-label">模拟面试</div>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="charts-grid">
        <!-- 成绩趋势 -->
        <div class="chart-card trend">
          <div class="chart-title">📈 成绩趋势</div>
          <div v-if="report.scoreTrend.length" ref="trendChartRef" class="chart-body"></div>
          <el-empty v-else description="暂无考试记录，去「智能考试」参加一场考试吧" :image-size="80" />
        </div>

        <!-- 知识点掌握度 -->
        <div class="chart-card mastery">
          <div class="chart-title">📚 知识点掌握度</div>
          <div v-if="report.categoryMastery.length" ref="masteryChartRef" class="chart-body"></div>
          <el-empty v-else description="暂无答题数据" :image-size="80" />
        </div>

        <!-- 能力雷达图 -->
        <div class="chart-card radar">
          <div class="chart-title">🎯 能力雷达图</div>
          <div v-if="report.radar.length" ref="radarChartRef" class="chart-body"></div>
          <el-empty v-else description="答题后即可生成能力雷达图" :image-size="80" />
        </div>
      </div>

      <!-- AI 学习建议 -->
      <div class="suggest-card">
        <div class="suggest-header">
          <div class="chart-title">🤖 AI 个性化学习建议</div>
          <el-button type="primary" :loading="suggestLoading" @click="loadSuggest" :disabled="!hasData">
            {{ suggestions.length ? '重新生成' : '生成学习建议' }}
          </el-button>
        </div>
        <div v-if="suggestLoading" class="suggest-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>AI 正在分析你的学习数据，请稍候...</span>
        </div>
        <div v-else-if="suggestions.length" class="suggest-list">
          <div v-for="(item, index) in suggestions" :key="index" class="suggest-item">
            <span class="suggest-index">{{ index + 1 }}</span>
            <span class="suggest-text">{{ item }}</span>
          </div>
        </div>
        <el-empty v-else-if="!hasData" description="积累一些答题数据后，AI 就能给出更精准的学习建议" :image-size="80" />
        <el-empty v-else description="点击上方按钮生成专属学习建议" :image-size="80" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, Trophy, TrendCharts, Edit, CircleCheck, Cpu, Microphone } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getLearningReport, getAiSuggest } from '@/api/analysis'

const loading = ref(true)
const suggestLoading = ref(false)
const report = ref({
  overview: {},
  scoreTrend: [],
  categoryMastery: [],
  radar: []
})
const suggestions = ref([])

const trendChartRef = ref(null)
const masteryChartRef = ref(null)
const radarChartRef = ref(null)
let trendChart = null
let masteryChart = null
let radarChart = null

const hasData = computed(() => {
  const o = report.value.overview || {}
  return (report.value.scoreTrend.length || report.value.categoryMastery.length || (o.answerCount || 0) > 0)
})

onMounted(async () => {
  await loadReport()
})

onBeforeUnmount(() => {
  if (trendChart) trendChart.dispose()
  if (masteryChart) masteryChart.dispose()
  if (radarChart) radarChart.dispose()
})

async function loadReport() {
  try {
    const res = await getLearningReport()
    report.value = res.data || report.value
    await nextTick()
    renderCharts()
  } catch (e) {
    console.error('加载学习报告失败', e)
  } finally {
    loading.value = false
  }
}

async function loadSuggest() {
  suggestLoading.value = true
  suggestions.value = []
  try {
    const res = await getAiSuggest()
    suggestions.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    ElMessage.error('生成学习建议失败，请稍后重试')
  } finally {
    suggestLoading.value = false
  }
}

function renderCharts() {
  if (report.value.scoreTrend.length) {
    renderTrendChart()
  }
  if (report.value.categoryMastery.length) {
    renderMasteryChart()
  }
  if (report.value.radar.length) {
    renderRadarChart()
  }
}

function renderTrendChart() {
  trendChart = initChart(trendChart.value, trendChartRef.value)
  const data = report.value.scoreTrend
  const xLabels = data.map(item => formatDate(item.examTime))
  const rates = data.map(item => item.scoreRate)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 40 },
    xAxis: {
      type: 'category',
      data: xLabels,
      axisLabel: { color: '#606266' }
    },
    yAxis: {
      type: 'value',
      name: '得分率(%)',
      min: 0,
      max: 100,
      axisLabel: { color: '#606266' }
    },
    series: [{
      name: '得分率',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.2 },
      lineStyle: { color: '#409EFF', width: 3 },
      itemStyle: { color: '#409EFF' },
      data: rates
    }]
  })
}

function renderMasteryChart() {
  masteryChart = initChart(masteryChart.value, masteryChartRef.value)
  const data = report.value.categoryMastery
  const names = data.map(item => item.categoryName)
  const rates = data.map(item => item.correctRate)
  masteryChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 60 },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { color: '#606266', interval: 0, rotate: 30 }
    },
    yAxis: {
      type: 'value',
      name: '得分率(%)',
      min: 0,
      max: 100,
      axisLabel: { color: '#606266' }
    },
    series: [{
      name: '得分率',
      type: 'bar',
      barWidth: '45%',
      itemStyle: {
        color: params => {
          const value = params.value
          if (value >= 80) return '#67C23A'
          if (value >= 60) return '#409EFF'
          if (value >= 40) return '#E6A23C'
          return '#F56C6C'
        },
        borderRadius: [4, 4, 0, 0]
      },
      data: rates
    }]
  })
}

function renderRadarChart() {
  radarChart = initChart(radarChart.value, radarChartRef.value)
  const data = report.value.radar
  const indicators = data.map(item => ({ name: item.name, max: 100 }))
  const values = data.map(item => item.value)
  radarChart.setOption({
    tooltip: {},
    radar: {
      indicator: indicators,
      radius: '65%',
      splitArea: { areaStyle: { color: ['rgba(64,158,255,0.05)', 'rgba(64,158,255,0.1)'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '能力得分',
        areaStyle: { opacity: 0.25, color: '#409EFF' },
        lineStyle: { color: '#409EFF', width: 2 },
        itemStyle: { color: '#409EFF' }
      }]
    }]
  })
}

function initChart(chart, ref) {
  if (chart) chart.dispose()
  return echarts.init(ref)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr.replace(/-/g, '/'))
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${m}/${day}`
}
</script>

<style lang="scss" scoped>
.analysis-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

.page-header {
  margin-bottom: 24px;
  h1 {
    margin: 0 0 8px;
    font-size: 26px;
  }
  p {
    margin: 0;
    color: #909399;
    font-size: 14px;
  }
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #909399;
  font-size: 14px;
  .el-icon {
    font-size: 36px;
    margin-bottom: 12px;
  }
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 10px;
  padding: 18px 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s;
  &:hover { transform: translateY(-2px); }

  .stat-icon {
    width: 42px;
    height: 42px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #fff;
    margin-right: 12px;
    flex-shrink: 0;
    &.exam { background: #409EFF; }
    &.rate { background: #67C23A; }
    &.answer { background: #E6A23C; }
    &.correct { background: #F56C6C; }
    &.ai { background: #909399; }
    &.interview { background: #7c4dff; }
  }

  .stat-value {
    font-size: 22px;
    font-weight: 700;
    color: #303133;
    line-height: 1.2;
  }
  .stat-label {
    font-size: 12px;
    color: #909399;
    margin-top: 2px;
  }
}

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.chart-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  &.radar { grid-column: span 2; }

  .chart-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 12px;
  }
  .chart-body {
    height: 300px;
  }
}

.suggest-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  .suggest-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  .suggest-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 0;
    color: #909399;
    font-size: 14px;
    .el-icon { margin-right: 8px; }
  }

  .suggest-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .suggest-item {
    display: flex;
    align-items: flex-start;
    background: #f5f7fa;
    border-radius: 8px;
    padding: 12px 16px;

    .suggest-index {
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
      margin-right: 12px;
      flex-shrink: 0;
      margin-top: 1px;
    }
    .suggest-text {
      font-size: 14px;
      color: #303133;
      line-height: 1.7;
    }
  }
}

@media (max-width: 900px) {
  .overview-grid { grid-template-columns: repeat(3, 1fr); }
  .charts-grid { grid-template-columns: 1fr; }
  .chart-card.radar { grid-column: span 1; }
}
</style>