<template>
  <div class="exam-ranking-page">
    <!-- 页面标题 - 重新设计 -->
    <div class="page-header">
      <div class="header-eyebrow">HALL OF FAME · 荣誉榜</div>
      <h2 class="main-title">考试排行榜</h2>
      <p class="subtitle">每一分，都是实力的见证</p>
      <div class="title-underline"></div>
    </div>

    <!-- 筛选条件 - 美化 -->
    <div class="filter-bar">
      <div class="filter-label">筛选</div>
      <el-select 
        v-model="selectedPaperId" 
        placeholder="选择试卷" 
        clearable 
        style="width: 300px"
        @change="loadRanking"
        class="custom-select"
      >
        <el-option 
          v-for="paper in paperList" 
          :key="paper.id" 
          :label="paper.name" 
          :value="paper.id" 
        />
      </el-select>
      <el-select 
        v-model="rankingLimit" 
        placeholder="显示数量" 
        style="width: 150px"
        @change="loadRanking"
        class="custom-select"
      >
        <el-option label="前10名" :value="10" />
        <el-option label="前20名" :value="20" />
        <el-option label="前50名" :value="50" />
        <el-option label="前100名" :value="100" />
      </el-select>
      <el-button 
        type="primary" 
        @click="loadRanking" 
        :loading="loading" 
        icon="Refresh"
        class="refresh-btn"
      >
        刷新榜单
      </el-button>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧排行榜列表 -->
      <div class="ranking-container">
        <!-- 冠军展示区 -->
        <div v-if="rankingList.length > 0" class="champion-showcase">
          <div class="champion-badge">NO.1</div>
          <div class="champion-info">
            <div class="champion-label">当前榜首</div>
            <div class="champion-name">{{ rankingList[0].studentName }}</div>
          </div>
          <div class="champion-score">{{ rankingList[0].score }}<span class="champion-unit">分</span></div>
        </div>

        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="10" animated />
        </div>
        
        <div v-else-if="rankingList.length > 0" class="ranking-list">
          <div 
            v-for="(record, index) in rankingList" 
            :key="record.id" 
            class="ranking-item"
            :class="{ 'top-three': index < 3 }"
          >
            <div class="rank-number" :class="getRankClass(index + 1)">
              <span>{{ index + 1 }}</span>
            </div>
            <div class="student-info">
              <div class="student-name">{{ record.studentName }}</div>
              <div class="paper-name">{{ record.paper?.name }}</div>
              <div class="exam-time">{{ formatDateTime(record.endTime) }}</div>
            </div>
            <div class="score-info">
              <div class="score-line">
                <span class="score">{{ record.score }}</span>
                <span class="total-score">/ {{ record.paper?.totalScore }}</span>
                <span class="percentage">{{ calculatePercentage(record.score, record.paper?.totalScore) }}%</span>
              </div>
              <div class="score-track">
                <i class="score-fill" :style="{ width: calculatePercentage(record.score, record.paper?.totalScore) + '%' }"></i>
              </div>
            </div>
          </div>
        </div>
        
        <div v-else class="empty-state">
          <div class="empty-icon">—</div>
          <div class="empty-text">暂无排行榜数据</div>
          <div class="empty-hint">快去参加考试，成为第一个上榜的人吧！</div>
        </div>
      </div>

      <!-- 右侧统计信息 -->
      <div v-if="allRecords.length > 0" class="statistics-sidebar">
        <div class="stats-title">{{ statsTitle }}</div>
        <div class="stats-vertical">
          <div class="stat-card-vertical">
            <div class="stat-info">
              <div class="stat-value">{{ totalParticipants }}</div>
              <div class="stat-label">参与人数</div>
            </div>
          </div>
          <div class="stat-card-vertical">
            <div class="stat-info">
              <div class="stat-value">{{ averageScore }}</div>
              <div class="stat-label">平均分</div>
            </div>
          </div>
          <div class="stat-card-vertical">
            <div class="stat-info">
              <div class="stat-value">{{ maxScore }}</div>
              <div class="stat-label">最高分</div>
            </div>
          </div>
          <div class="stat-card-vertical">
            <div class="stat-info">
              <div class="stat-value">{{ minScore }}</div>
              <div class="stat-label">最低分</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部激励区域 -->
    <div class="motivation-section">
      <div class="motivation-text">每一次挑战，都是向更高名次的一次靠近</div>
    </div>
  </div>
  <BackHome />
  </template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '../utils/request'

// 响应式数据
const loading = ref(false)
const rankingList = ref([])
const paperList = ref([])
const selectedPaperId = ref(null)
const rankingLimit = ref(10)
const allRecords = ref([]) // 用于统计的所有考试记录

// 计算属性 - 基于所有记录进行统计
const averageScore = computed(() => {
  if (allRecords.value.length === 0) return 0
  const total = allRecords.value.reduce((sum, record) => sum + Number(record.score), 0)
  return (total / allRecords.value.length).toFixed(1)
})

const maxScore = computed(() => {
  if (allRecords.value.length === 0) return 0
  return Math.max(...allRecords.value.map(record => Number(record.score)))
})

const minScore = computed(() => {
  if (allRecords.value.length === 0) return 0
  return Math.min(...allRecords.value.map(record => Number(record.score)))
})

const totalParticipants = computed(() => {
  return allRecords.value.length
})

// 动态统计标题
const statsTitle = computed(() => {
  if (selectedPaperId.value) {
    const selectedPaper = paperList.value.find(p => p.id === selectedPaperId.value)
    return `${selectedPaper?.name || '选中试卷'} 统计`
  }
  return '全部试卷统计'
})

// 获取试卷列表
const getPaperList = async () => {
  try {
    // 调用后端试卷列表API，只传递状态筛选参数
    const res = await request.get('/api/papers/list', {
      params: {
        status: 'PUBLISHED'  // 只获取已发布的试卷
      }
    })
    // 修正数据解析：后端返回的数据直接在res.data中，不是res.data.records
    paperList.value = res.data || []
    console.log('试卷列表加载成功，共', paperList.value.length, '个试卷')
  } catch (error) {
    console.error('获取试卷列表失败：', error)
    ElMessage.error('获取试卷列表失败')
  }
}

// 加载排行榜数据
const loadRanking = async () => {
  loading.value = true
  try {
    // 修正API调用参数，使用后端支持的paperId和limit参数
    const displayParams = {
      paperId: selectedPaperId.value,   // 试卷ID筛选参数
      limit: rankingLimit.value        // 显示数量限制参数
    }
    
    const statsParams = {
      paperId: selectedPaperId.value,   // 试卷ID筛选参数  
      limit: 1000                      // 统计时获取所有记录
    }
    
    // 并行调用两个API：一个用于显示，一个用于统计
    const [rankingRes, statsRes] = await Promise.all([
      request.get('/api/exam-records/ranking', { params: displayParams }),
      request.get('/api/exam-records/ranking', { params: statsParams })
    ])
    
    // 设置排行榜数据和统计数据
    rankingList.value = rankingRes.data || []
    allRecords.value = statsRes.data || []
  } catch (error) {
    console.error('获取排行榜数据失败：', error)
    ElMessage.error('获取排行榜数据失败')
  } finally {
    loading.value = false
  }
}

// 获取排名样式类
const getRankClass = (rank) => {
  if (rank === 1) return 'rank-gold'
  if (rank === 2) return 'rank-silver'
  if (rank === 3) return 'rank-bronze'
  return 'rank-normal'
}

// 计算百分比
const calculatePercentage = (score, totalScore) => {
  if (!score || !totalScore) return 0
  return ((score / totalScore) * 100).toFixed(1)
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

onMounted(() => {
  getPaperList()
  loadRanking()
})
</script>

<style scoped>
/* ============================================================
   排行榜 · 「荣誉榜 · 浅色」设计（对标全局 #f5f7fa + Element 蓝）
   白卡片 + 金银铜奖牌徽章 + 衬线大数字
   ============================================================ */

.exam-ranking-page {
  position: relative;
  min-height: 100vh;
  padding: 44px 24px 40px;
  max-width: 1200px;
  margin: 0 auto;
  font-variant-numeric: tabular-nums;
}
/* 浅色背景：淡蓝渐出 + 细点纹理 */
.exam-ranking-page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: -1;
  background:
    radial-gradient(900px 400px at 50% -100px, rgba(64, 158, 255, 0.10), transparent 62%),
    radial-gradient(rgba(47, 86, 143, 0.05) 1px, transparent 1.5px),
    linear-gradient(180deg, #f6f9fd 0%, #eef3f9 60%, #eaf0f7 100%);
  background-size: auto, 26px 26px, auto;
}

/* ---------- 页头 ---------- */
.page-header {
  text-align: center;
  margin-bottom: 34px;
}
.header-eyebrow {
  display: inline-block;
  font-size: 12px;
  letter-spacing: 6px;
  color: #2f6bdf;
  border: 1px solid rgba(47, 107, 223, 0.4);
  background: rgba(47, 107, 223, 0.04);
  padding: 5px 16px 5px 22px;
  border-radius: 999px;
  margin-bottom: 16px;
}
.main-title {
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 10px;
  text-indent: 10px;
  color: #1f2d3d;
  margin: 0 0 10px;
}
.subtitle {
  color: #8492a6;
  margin: 0 0 16px;
  font-size: 14px;
  letter-spacing: 3px;
}
.title-underline {
  width: 72px;
  height: 3px;
  margin: 0 auto;
  border-radius: 2px;
  background: linear-gradient(90deg, transparent, #2f6bdf, transparent);
}

/* ---------- 筛选条：白卡片 ---------- */
.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 26px;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  background: #ffffff;
  border: 1px solid #e4eaf2;
  padding: 14px 20px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(31, 59, 115, 0.05);
}
.filter-label {
  color: #5a6b85;
  font-size: 14px;
  letter-spacing: 4px;
  padding-right: 4px;
  border-right: 1px solid #e4eaf2;
}
.custom-select {
  width: 280px;
}
.refresh-btn {
  border-radius: 8px !important;
  font-weight: 600 !important;
  padding: 11px 22px !important;
}

/* ---------- 主区 ---------- */
.main-content {
  display: flex;
  gap: 22px;
  align-items: flex-start;
}
.ranking-container {
  flex: 1;
  min-width: 0;
}

/* ---------- 榜首横幅：浅金 ---------- */
.champion-showcase {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 26px;
  margin-bottom: 18px;
  border-radius: 12px;
  background:
    linear-gradient(120deg, rgba(212, 175, 55, 0.12), rgba(212, 175, 55, 0.02) 55%),
    #fffdf6;
  border: 1px solid #ecd9a0;
  box-shadow: 0 4px 16px -6px rgba(196, 154, 42, 0.35);
  overflow: hidden;
}
.champion-showcase::after {
  content: '';
  position: absolute;
  top: 0;
  left: -60%;
  width: 40%;
  height: 100%;
  background: linear-gradient(100deg, transparent, rgba(255, 236, 170, 0.35), transparent);
  animation: sheen 4.5s ease-in-out infinite;
}
@keyframes sheen {
  0%, 55% { left: -60%; }
  85%, 100% { left: 130%; }
}
.champion-badge {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #5c4308;
  background: linear-gradient(160deg, #f3d97e, #d4af37);
  padding: 8px 12px;
  border-radius: 8px;
  box-shadow: 0 3px 10px -2px rgba(196, 154, 42, 0.55);
}
.champion-info {
  flex: 1;
  min-width: 0;
}
.champion-label {
  font-size: 12px;
  letter-spacing: 5px;
  color: #a08838;
  margin-bottom: 4px;
}
.champion-name {
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
  font-size: 26px;
  font-weight: 700;
  color: #1f2d3d;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.champion-score {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 42px;
  font-weight: 700;
  color: #b8860b;
  line-height: 1;
}
.champion-unit {
  font-size: 15px;
  color: #a08838;
  margin-left: 4px;
}

/* ---------- 榜单行：白卡片 ---------- */
.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ranking-item {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 16px 20px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #e6ecf4;
  box-shadow: 0 1px 4px rgba(31, 59, 115, 0.04);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  animation: rise 0.5s ease backwards;
}
.ranking-item:nth-child(1) { animation-delay: 0.05s; }
.ranking-item:nth-child(2) { animation-delay: 0.1s; }
.ranking-item:nth-child(3) { animation-delay: 0.15s; }
.ranking-item:nth-child(4) { animation-delay: 0.2s; }
.ranking-item:nth-child(5) { animation-delay: 0.25s; }
.ranking-item:nth-child(6) { animation-delay: 0.3s; }
.ranking-item:hover {
  transform: translateX(4px);
  border-color: rgba(47, 107, 223, 0.4);
  box-shadow: 0 4px 14px rgba(31, 59, 115, 0.1);
}
@keyframes rise {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 金银铜奖牌徽章 */
.rank-number {
  flex: none;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 17px;
  font-weight: 700;
  color: #7a8699;
  border: 1px solid #dfe6ef;
  background: #f4f7fb;
}
.rank-gold {
  color: #5c4308;
  background: linear-gradient(150deg, #f2d67e 0%, #d4af37 45%, #c09a2e 100%);
  border-color: #e8c96a;
  box-shadow: 0 0 12px -2px rgba(212, 175, 55, 0.6);
}
.rank-silver {
  color: #3d4652;
  background: linear-gradient(150deg, #eef1f5 0%, #c3cad4 55%, #9aa5b3 100%);
  border-color: #d5dbe3;
}
.rank-bronze {
  color: #4a2f18;
  background: linear-gradient(150deg, #e0aa74 0%, #c08348 55%, #9a6431 100%);
  border-color: #d9a06b;
}
.ranking-item.top-three {
  background: #fffdf6;
  border-color: #f0e2b8;
}

/* 行内信息 */
.student-info {
  flex: 1;
  min-width: 0;
}
.student-name {
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
  font-size: 17px;
  font-weight: 700;
  color: #253244;
  margin-bottom: 3px;
}
.paper-name {
  font-size: 12.5px;
  color: #8492a6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.exam-time {
  font-size: 12px;
  color: #a5b0c0;
  margin-top: 2px;
}

/* 分数：衬线大数字 + 得分率细条 */
.score-info {
  flex: none;
  width: 168px;
  text-align: right;
}
.score-line {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 5px;
}
.score {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 28px;
  font-weight: 700;
  color: #2f6bdf;
  line-height: 1;
}
.top-three .score { color: #b8860b; }
.total-score {
  font-size: 13px;
  color: #a5b0c0;
}
.percentage {
  font-size: 12.5px;
  color: #7a8699;
  margin-left: 6px;
}
.score-track {
  margin-top: 8px;
  height: 4px;
  border-radius: 2px;
  background: #e8eef5;
  overflow: hidden;
}
.score-fill {
  display: block;
  height: 100%;
  border-radius: 2px;
  background: linear-gradient(90deg, #6aa7ff, #2f6bdf);
  transition: width 0.6s ease;
}

/* ---------- 右侧统计：白卡片 ---------- */
.statistics-sidebar {
  flex: none;
  width: 220px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: sticky;
  top: 20px;
}
.stats-title {
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
  font-size: 14px;
  letter-spacing: 3px;
  color: #5a6b85;
  padding-bottom: 8px;
  border-bottom: 2px solid rgba(47, 107, 223, 0.35);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stats-vertical {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.stat-card-vertical {
  padding: 16px 18px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #e6ecf4;
  box-shadow: 0 1px 4px rgba(31, 59, 115, 0.04);
}
.stat-value {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 30px;
  font-weight: 700;
  color: #2f6bdf;
  line-height: 1.1;
}
.stat-label {
  margin-top: 4px;
  font-size: 12.5px;
  letter-spacing: 2px;
  color: #8492a6;
}

/* ---------- 空状态 / 加载 ---------- */
.loading-container {
  padding: 20px;
}
.empty-state {
  text-align: center;
  padding: 70px 0 60px;
}
.empty-icon {
  font-family: Georgia, serif;
  font-size: 40px;
  color: #c9d4e2;
  margin-bottom: 14px;
}
.empty-text {
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
  font-size: 18px;
  color: #4a5a70;
}
.empty-hint {
  margin-top: 8px;
  font-size: 13px;
  color: #a5b0c0;
}

/* ---------- 底部 ---------- */
.motivation-section {
  margin-top: 34px;
  text-align: center;
}
.motivation-text {
  display: inline-block;
  font-size: 13px;
  letter-spacing: 3px;
  color: #a5b0c0;
  padding: 0 24px 14px;
  border-bottom: 1px solid #dfe6ef;
}

/* ---------- 窄屏 ---------- */
@media (max-width: 900px) {
  .main-content { flex-direction: column; }
  .statistics-sidebar { width: 100%; position: static; }
  .stats-vertical { flex-direction: row; }
  .stat-card-vertical { flex: 1; }
}
@media (max-width: 640px) {
  .champion-showcase { flex-wrap: wrap; }
  .score-info { width: 120px; }
  .score { font-size: 22px; }
}
</style>
