import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: 'http://localhost:8081', // 后端端口（与 backend/application.yml 的 server.port 对应；2026-10-11 起为 8081）
  timeout: 100000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 需登录的业务接口前缀（边界与后端 SecurityConfig 保持一致）
// 无 token 时直接拦截、不再发送请求，避免无意义的 401 往返
const AUTH_REQUIRED_PREFIXES = [
  '/api/exams',
  '/api/exam-records',
  '/api/learning-path',
  '/api/wrong-questions',
  '/api/smart-paper',
  '/api/analysis',
  '/api/chat',
  '/api/pay',
  '/api/mock-interview',
  '/api/ai-interview'
]

const needsAuth = (url = '') => AUTH_REQUIRED_PREFIXES.some(prefix => url.startsWith(prefix))

// 统一跳登录页并带上回跳地址
const redirectToLogin = () => {
  const current = router.currentRoute.value
  if (current.path === '/login') return
  router.push({ path: '/login', query: { redirect: current.fullPath } })
}

request.interceptors.request.use(
  config => {
    const userStore = useUserStore()

    // API 层前置校验：未登录访问需鉴权接口 → 直接拦截并跳转登录页
    if (!userStore.token && needsAuth(config.url)) {
      ElMessage.warning('请先登录后再继续操作')
      redirectToLogin()
      return Promise.reject(new Error('未登录，已跳转到登录页'))
    }

    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    // 为聊天接口设置更长的超时时间（后端 DashScope 模型 timeout=8min，AI 生成试卷链路可能超 60s）
    if (config.url && config.url.includes('/chat')) {
      config.timeout = 480000
    }
    // 交卷接口包含同步 AI 判卷，放宽超时防止前端先于后端返回而误报
    if (config.url && config.url.includes('/submit')) {
      config.timeout = 200000
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    const res = response.data

    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        redirectToLogin()
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },
  error => {
    console.error('响应错误:', error)
    if (error.response && error.response.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      redirectToLogin()
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request