<template>
  <div class="finance-page">
    <!-- 页头 -->
    <div class="page-head">
      <div class="head-title">
        <h2>企业财务报表</h2>
        <p>收支明细 · 利润统计 · 月度 / 年度报表</p>
      </div>
      <div class="head-actions">
        <el-select v-model="year" style="width: 120px" @change="handleYearChange">
          <el-option v-for="y in yearOptions" :key="y" :label="y + ' 年'" :value="y" />
        </el-select>
        <el-button type="primary" :icon="Download" @click="handleExport">导出 Excel</el-button>
      </div>
    </div>

    <!-- 统计卡 -->
    <div class="stat-grid" v-loading="overviewLoading">
      <div class="stat-card">
        <div class="stat-label">年度总收入</div>
        <div class="stat-value income">{{ fmtMoney(overview.totalIncome) }}</div>
        <div class="stat-sub">{{ overview.recordCount || 0 }} 笔流水</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">年度总支出</div>
        <div class="stat-value expense">{{ fmtMoney(overview.totalExpense) }}</div>
        <div class="stat-sub">共 {{ year }} 年度</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">年度净利润</div>
        <div class="stat-value" :class="overview.profit >= 0 ? 'income' : 'expense'">
          {{ fmtMoney(overview.profit) }}
        </div>
        <div class="stat-sub">收入 - 支出</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">利润率</div>
        <div class="stat-value rate">{{ overview.profitRate || 0 }}%</div>
        <div class="stat-sub">净利润 / 收入</div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-row">
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="chart-header">
            <span>月度收支趋势</span>
            <span class="chart-tip">柱：收入 / 支出，线：净利润</span>
          </div>
        </template>
        <div ref="trendRef" class="chart-box"></div>
      </el-card>
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="chart-header">
            <span>分类构成</span>
            <el-radio-group v-model="pieType" size="small" @change="handlePieTypeChange">
              <el-radio-button value="EXPENSE">支出</el-radio-button>
              <el-radio-button value="INCOME">收入</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="pieRef" class="chart-box"></div>
      </el-card>
    </div>

    <!-- 明细 / 报表 -->
    <el-card class="report-card" shadow="never">
      <el-tabs v-model="activeTab">
        <!-- 收支明细 -->
        <el-tab-pane label="收支明细" name="detail">
          <div class="filter-bar">
            <el-select v-model="filters.recordType" placeholder="类型" clearable style="width: 120px" @change="handleTypeChange">
              <el-option label="收入" value="INCOME" />
              <el-option label="支出" value="EXPENSE" />
            </el-select>
            <el-select v-model="filters.category" placeholder="分类" clearable style="width: 160px" @change="handleSearch">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 280px"
              @change="handleSearch"
            />
            <el-input
              v-model="filters.keyword"
              placeholder="流水号 / 分类 / 备注"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            <el-button type="success" plain :icon="Download" @click="handleExport">导出当前筛选</el-button>
          </div>

          <el-table :data="records" v-loading="loading" stripe style="width: 100%">
            <el-table-column prop="recordNo" label="流水号" width="150" />
            <el-table-column prop="recordDate" label="日期" width="110" />
            <el-table-column label="类型" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.recordType === 'INCOME' ? 'success' : 'danger'" effect="light" size="small">
                  {{ scope.row.recordType === 'INCOME' ? '收入' : '支出' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="category" label="分类" width="140" />
            <el-table-column label="金额" width="140" align="right">
              <template #default="scope">
                <span :class="scope.row.recordType === 'INCOME' ? 'amt-income' : 'amt-expense'">
                  {{ scope.row.recordType === 'INCOME' ? '+' : '-' }}{{ fmtMoney(scope.row.amount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="结算方式" width="100">
              <template #default="scope">{{ methodText(scope.row.paymentMethod) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="pagination.current"
              v-model:page-size="pagination.size"
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="loadRecords"
            />
          </div>
        </el-tab-pane>

        <!-- 月度报表 -->
        <el-tab-pane label="月度报表" name="monthly">
          <el-table :data="monthlyWithTotal" stripe :row-class-name="rowClassName" style="width: 100%">
            <el-table-column prop="month" label="月份" width="120" />
            <el-table-column label="收入" align="right" min-width="150">
              <template #default="scope">
                <span class="amt-income">{{ fmtMoney(scope.row.income) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="支出" align="right" min-width="150">
              <template #default="scope">
                <span class="amt-expense">{{ fmtMoney(scope.row.expense) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="净利润" align="right" min-width="150">
              <template #default="scope">
                <span :class="scope.row.profit >= 0 ? 'amt-income' : 'amt-expense'">{{ fmtMoney(scope.row.profit) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="利润率" width="100" align="right">
              <template #default="scope">{{ rateOf(scope.row) }}%</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 年度报表 -->
        <el-tab-pane label="年度报表" name="yearly">
          <el-table :data="yearlyWithTotal" stripe :row-class-name="rowClassName" style="width: 100%">
            <el-table-column prop="year" label="年份" width="120" />
            <el-table-column label="收入" align="right" min-width="150">
              <template #default="scope">
                <span class="amt-income">{{ fmtMoney(scope.row.income) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="支出" align="right" min-width="150">
              <template #default="scope">
                <span class="amt-expense">{{ fmtMoney(scope.row.expense) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="净利润" align="right" min-width="150">
              <template #default="scope">
                <span :class="scope.row.profit >= 0 ? 'amt-income' : 'amt-expense'">{{ fmtMoney(scope.row.profit) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="利润率" width="100" align="right">
              <template #default="scope">{{ rateOf(scope.row) }}%</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search, Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'
import request from '../utils/request'
import { useUserStore } from '@/stores/user'
import {
  getFinanceOverview,
  getFinanceMonthly,
  getFinanceYearly,
  getFinanceCategoryStats,
  getFinanceCategories,
  getFinanceRecords
} from '@/api/finance'

const userStore = useUserStore()
const currentYear = new Date().getFullYear()

const year = ref(currentYear)
const yearOptions = ref([currentYear])
const overview = ref({ totalIncome: 0, totalExpense: 0, profit: 0, profitRate: 0, recordCount: 0 })
const monthly = ref([])
const yearly = ref([])
const categories = ref([])
const pieType = ref('EXPENSE')
const pieStats = ref([])

const activeTab = ref('detail')
const records = ref([])
const loading = ref(false)
const overviewLoading = ref(false)
const pagination = reactive({ current: 1, size: 10, total: 0 })
const filters = reactive({ recordType: '', category: '', dateRange: [], keyword: '' })

const trendRef = ref(null)
const pieRef = ref(null)
let trendChart = null
let pieChart = null

/* ---------------- 计算 ---------------- */
const monthlyWithTotal = computed(() => {
  const sumIncome = monthly.value.reduce((s, i) => s + Number(i.income || 0), 0)
  const sumExpense = monthly.value.reduce((s, i) => s + Number(i.expense || 0), 0)
  return [
    ...monthly.value,
    { month: '全年合计', income: sumIncome, expense: sumExpense, profit: sumIncome - sumExpense, isTotal: true }
  ]
})

const yearlyWithTotal = computed(() => {
  const sumIncome = yearly.value.reduce((s, i) => s + Number(i.income || 0), 0)
  const sumExpense = yearly.value.reduce((s, i) => s + Number(i.expense || 0), 0)
  return [
    ...yearly.value,
    { year: '合计', income: sumIncome, expense: sumExpense, profit: sumIncome - sumExpense, isTotal: true }
  ]
})

/* ---------------- 工具 ---------------- */
const fmtMoney = (v) => '¥' + Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const methodText = (m) => ({ ALIPAY: '支付宝', BANK: '银行转账', CASH: '现金' }[m] || m || '-')
const rateOf = (row) => {
  const income = Number(row.income || 0)
  if (income <= 0) return 0
  return Math.round((Number(row.profit || 0) * 100) / income)
}
const rowClassName = ({ row }) => (row.isTotal ? 'total-row' : '')

/* ---------------- 数据加载 ---------------- */
const buildQueryParams = () => ({
  recordType: filters.recordType || undefined,
  category: filters.category || undefined,
  startDate: filters.dateRange && filters.dateRange[0] ? filters.dateRange[0] : undefined,
  endDate: filters.dateRange && filters.dateRange[1] ? filters.dateRange[1] : undefined,
  keyword: filters.keyword || undefined,
  page: pagination.current,
  size: pagination.size
})

async function loadOverview() {
  overviewLoading.value = true
  try {
    const res = await getFinanceOverview(year.value)
    if (res.code === 200) overview.value = res.data
  } catch (e) { /* 拦截器已提示 */ } finally {
    overviewLoading.value = false
  }
}

async function loadMonthly() {
  const res = await getFinanceMonthly(year.value)
  if (res.code === 200) monthly.value = res.data || []
}

async function loadYearly() {
  const res = await getFinanceYearly()
  if (res.code === 200) {
    yearly.value = res.data || []
    const years = new Set(yearly.value.map((i) => i.year))
    years.add(currentYear)
    yearOptions.value = Array.from(years).sort((a, b) => b - a)
  }
}

async function loadPie() {
  const res = await getFinanceCategoryStats(pieType.value, year.value)
  if (res.code === 200) {
    pieStats.value = res.data || []
    await nextTick()
    renderPieChart()
  }
}

async function loadCategories() {
  const res = await getFinanceCategories(filters.recordType || undefined)
  if (res.code === 200) categories.value = res.data || []
}

async function loadRecords() {
  loading.value = true
  try {
    const res = await getFinanceRecords(buildQueryParams())
    if (res.code === 200) {
      records.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

/* ---------------- 图表 ---------------- */
function initChart(chart, el) {
  if (chart) chart.dispose()
  return echarts.init(el)
}

function renderTrendChart() {
  if (!trendRef.value) return
  trendChart = initChart(trendChart, trendRef.value)
  const months = monthly.value.map((i) => Number(i.month.slice(5)) + '月')
  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v) => '¥' + Number(v || 0).toLocaleString('zh-CN')
    },
    legend: { data: ['收入', '支出', '净利润'], top: 0 },
    grid: { left: 60, right: 24, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: months, axisLabel: { color: '#606266' } },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#909399',
        formatter: (v) => (Math.abs(v) >= 10000 ? v / 10000 + '万' : v)
      }
    },
    series: [
      { name: '收入', type: 'bar', barMaxWidth: 16, itemStyle: { color: '#67c23a' }, data: monthly.value.map((i) => i.income) },
      { name: '支出', type: 'bar', barMaxWidth: 16, itemStyle: { color: '#f56c6c' }, data: monthly.value.map((i) => i.expense) },
      {
        name: '净利润',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        lineStyle: { width: 3, color: '#e6a23c' },
        itemStyle: { color: '#e6a23c' },
        data: monthly.value.map((i) => i.profit)
      }
    ]
  })
}

function renderPieChart() {
  if (!pieRef.value) return
  pieChart = initChart(pieChart, pieRef.value)
  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      valueFormatter: (v) => '¥' + Number(v || 0).toLocaleString('zh-CN')
    },
    legend: { orient: 'vertical', right: 8, top: 'center', type: 'scroll' },
    series: [
      {
        name: pieType.value === 'EXPENSE' ? '支出构成' : '收入构成',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['38%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{d}%', color: '#606266' },
        data: pieStats.value.map((i) => ({ name: i.category, value: Number(i.amount || 0) }))
      }
    ]
  })
}

const handleResize = () => {
  if (trendChart) trendChart.resize()
  if (pieChart) pieChart.resize()
}

/* ---------------- 交互 ---------------- */
async function handleYearChange() {
  await Promise.all([loadOverview(), loadMonthly(), loadPie()])
  await nextTick()
  renderTrendChart()
  handleSearch()
}

function handleTypeChange() {
  filters.category = ''
  loadCategories()
  handleSearch()
}

function handleSearch() {
  pagination.current = 1
  loadRecords()
}

function resetSearch() {
  filters.recordType = ''
  filters.category = ''
  filters.dateRange = []
  filters.keyword = ''
  loadCategories()
  handleSearch()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.current = 1
  loadRecords()
}

function handlePieTypeChange() {
  loadPie()
}

const handleExport = async () => {
  try {
    const params = buildQueryParams()
    delete params.page
    delete params.size
    const resp = await axios.get(request.defaults.baseURL + '/api/admin/finance/export', {
      params,
      responseType: 'blob',
      headers: userStore.token ? { Authorization: `Bearer ${userStore.token}` } : {}
    })
    const blob = new Blob([resp.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `财务报表_${year.value}_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败，请稍后重试')
  }
}

/* ---------------- 生命周期 ---------------- */
onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await Promise.all([loadOverview(), loadMonthly(), loadYearly(), loadCategories()])
  await nextTick()
  renderTrendChart()
  loadPie()
  loadRecords()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (trendChart) trendChart.dispose()
  if (pieChart) pieChart.dispose()
})
</script>

<style scoped>
.finance-page {
  padding-bottom: 24px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.head-title h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #303133;
}

.head-title p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 14px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 18px;
  border: 1px solid #ebeef5;
}

.stat-label {
  color: #909399;
  font-size: 13px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  margin: 8px 0 4px;
  color: #303133;
}

.stat-value.income,
.amt-income {
  color: #67c23a;
}

.stat-value.expense,
.amt-expense {
  color: #f56c6c;
}

.stat-value.rate {
  color: #409eff;
}

.stat-sub {
  color: #c0c4cc;
  font-size: 12px;
}

.chart-row {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 14px;
  margin-bottom: 14px;
}

.chart-card {
  border-radius: 8px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #303133;
}

.chart-tip {
  color: #c0c4cc;
  font-size: 12px;
  font-weight: 400;
}

.chart-box {
  height: 300px;
  width: 100%;
}

.report-card {
  border-radius: 8px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

:deep(.total-row) {
  font-weight: 700;
  background: #f5f7fa;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-row {
    grid-template-columns: 1fr;
  }
}
</style>