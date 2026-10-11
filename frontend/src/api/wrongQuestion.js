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