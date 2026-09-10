import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 100000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
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
        router.push('/login')
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
      router.push('/login')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request