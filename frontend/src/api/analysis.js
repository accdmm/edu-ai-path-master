import request from '../utils/request'

/**
 * AI 学习分析相关 API
 */

// 获取学习分析报告（概览/成绩趋势/知识点掌握/雷达图）
export function getLearningReport() {
  return request({
    url: '/api/analysis/learning-report',
    method: 'get'
  })
}

// 生成 AI 个性化学习建议
export function getAiSuggest() {
  return request({
    url: '/api/analysis/ai-suggest',
    method: 'get',
    timeout: 120000
  })
}