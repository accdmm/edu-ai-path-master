import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import BackHome from './components/BackHome.vue'

// 创建Vue应用实例
const app = createApp(App)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局注册：悬浮“返回首页”按钮（各业务页面模板直接 <BackHome /> 使用）
app.component('BackHome', BackHome)

// 使用插件
app.use(createPinia()) // 状态管理
app.use(router) // 路由
app.use(ElementPlus) // UI组件库

// 挂载应用
app.mount('#app') 