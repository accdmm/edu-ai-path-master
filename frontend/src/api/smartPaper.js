import request from '../utils/request'

/**
 * AI 智能组卷相关 API
 */

// 查询薄弱知识点（错题按知识点聚合，AI 组卷将重点考察）
export function getWeakPoints() {
  return request({
    url: '/api/smart-paper/weak-points',
    method: 'get'
  })
}

// AI 根据薄弱点智能组卷（每次消耗积分，生成失败不扣费），返回试卷组成与扣费结果
export function generateSmartPaper(count) {
  return request({
    url: '/api/smart-paper/generate',
    method: 'post',
    params: count ? { count } : {},
    timeout: 180000
  })
}