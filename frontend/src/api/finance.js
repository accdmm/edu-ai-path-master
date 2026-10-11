import request from '../utils/request'

/**
 * 后台管理 - 企业财务报表相关 API
 */

// 年度概览（总收入/总支出/净利润/利润率）
export function getFinanceOverview(year) {
  return request({
    url: '/api/admin/finance/overview',
    method: 'get',
    params: { year }
  })
}

// 月度报表（指定年度 12 个月）
export function getFinanceMonthly(year) {
  return request({
    url: '/api/admin/finance/monthly',
    method: 'get',
    params: { year }
  })
}

// 年度报表（全部年份聚合）
export function getFinanceYearly() {
  return request({
    url: '/api/admin/finance/yearly',
    method: 'get'
  })
}

// 分类构成（饼图）：recordType=INCOME/EXPENSE，year 可为空取全部
export function getFinanceCategoryStats(recordType, year) {
  return request({
    url: '/api/admin/finance/category-stats',
    method: 'get',
    params: { recordType, year }
  })
}

// 分类列表（筛选下拉）
export function getFinanceCategories(recordType) {
  return request({
    url: '/api/admin/finance/categories',
    method: 'get',
    params: { recordType }
  })
}

// 收支明细分页
export function getFinanceRecords(params) {
  return request({
    url: '/api/admin/finance/records',
    method: 'get',
    params
  })
}