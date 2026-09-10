import request from '../utils/request'

/**
 * AI 学习路径相关 API
 */

// 生成学习路径（LLM 编排，约 30-60 秒；旧 ACTIVE 路径自动归档）
export function generateLearningPath() {
  return request({
    url: '/api/learning-path/generate',
    method: 'post',
    timeout: 180000
  })
}

// 查询当前 ACTIVE 学习路径
export function getActiveLearningPath() {
  return request({
    url: '/api/learning-path/active',
    method: 'get'
  })
}

// 切换节点完成状态（KNOWLEDGE/QUESTION 手动打勾；PAPER 由系统自动判定）
export function togglePathNode(nodeId) {
  return request({
    url: `/api/learning-path/node/${nodeId}/toggle`,
    method: 'post'
  })
}

// 一键生成综合诊断卷（冷启动：题库混合抽题，返回试卷ID后直接开考）
export function generateDiagnosticPaper() {
  return request({
    url: '/api/learning-path/diagnostic-paper',
    method: 'post'
  })
}
