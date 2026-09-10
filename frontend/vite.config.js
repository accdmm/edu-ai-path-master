import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    host: true, // 绑定 0.0.0.0，IPv4/IPv6 均可访问（避免 localhost 解析差异导致连不上）
    port: 3001, // 前端服务端口
    //现在配置的绝对地址，不走代理！后台处理跨域
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 后端服务地址
        changeOrigin: true
      }
    }
  }
}) 