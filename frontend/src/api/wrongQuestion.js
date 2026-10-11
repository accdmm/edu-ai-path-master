import request from '../utils/request'

/**
 * 错题本相关 API
 */

// 分页查询错题本（支持知识点/掌握状态筛选）
export function getWrongQuestions(params) {
  return request({
    url: '/api/wrong-questions/list',
    method: 'get',
    params
  })
}

// 错题统计（总览 + 按知识点聚合）
export function getWrongQuestionStats() {
  return request({
    url: '/api/wrong-questions/stats',
    method: 'get'
  })
}

// 切换掌握状态（未掌握/半掌握 ↔ 已掌握，可撤销）
export function toggleWrongQuestionMaster(id) {
  return request({
    url: `/api/wrong-questions/${id}/toggle-master`,
    method: 'post'
  })
}

// 移出错题本（之后再次答错会被重新收录）
export function removeWrongQuestion(id) {
  return request({
    url: `/api/wrong-questions/${id}`,
    method: 'delete'
  })
}

// AI 解析错题（统一 AI 解析计费：每日 3 次免费，超出每次扣 5 积分）
export function aiAnalyzeWrongQuestion(id) {
  return request({
    url: `/api/wrong-questions/${id}/ai-analysis`,
    method: 'post'
  })
}