<template>
  <div class="login-page">
    <main class="auth-col">
      <header class="brand">
        <img src="@/assets/logo.svg" alt="" class="brand-mark" />
        <h1 class="brand-name">智能学习平台</h1>
      </header>

      <form class="auth-form" @submit.prevent="handleLogin">
        <label class="field">
          <span class="field-label">用户名</span>
          <el-input
            v-model="loginForm.username"
            size="large"
            placeholder="请输入用户名"
            autocomplete="username"
            :disabled="loading"
          />
        </label>

        <label class="field">
          <span class="field-label">密码</span>
          <el-input
            v-model="loginForm.password"
            type="password"
            size="large"
            placeholder="请输入密码"
            autocomplete="current-password"
            show-password
            :disabled="loading"
          />
        </label>

        <p v-if="errorMsg" class="form-error" role="alert" aria-live="polite">{{ errorMsg }}</p>

        <button class="submit" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登 录' }}
        </button>

        <p class="hint">按 <kbd>Enter</kbd> 快速提交</p>

        <nav class="links">
          <router-link to="/register">没有账号？立即注册</router-link>
          <router-link to="/home">返回首页</router-link>
        </nav>
      </form>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const errorMsg = ref('')

const loginForm = reactive({
  username: '',
  password: ''
})

const validate = () => {
  if (!loginForm.username.trim()) return '请输入用户名'
  if (!loginForm.password) return '请输入密码'
  if (loginForm.password.length < 6) return '密码长度不能小于 6 位'
  return ''
}

const handleLogin = async () => {
  errorMsg.value = validate()
  if (errorMsg.value) return

  loading.value = true
  try {
    const res = await login(loginForm)
    userStore.setToken(res.data.token)
    userStore.setUserInfo({
      userId: res.data.userId,
      username: res.data.username,
      realName: res.data.realName,
      role: res.data.role
    })
    errorMsg.value = ''
    ElMessage.success('登录成功')
    const redirect = router.currentRoute.value.query.redirect
    router.push(redirect || '/home')
  } catch (error) {
    // 行内错误（role=alert 可被读屏播报）；弹窗提示由 axios 拦截器统一负责
    errorMsg.value = (error && error.message) ? error.message : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
/* 极简单列：零装饰、白底、可见边框、单一强调色（Element 蓝），聚焦效率与可访问性 */
.login-page {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  background: #fff;
  padding: 24px;
}

.auth-col {
  width: min(360px, 100%);
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 28px;
}

.brand-mark {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.brand-name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  letter-spacing: .01em;
}

.field {
  display: block;
  margin-bottom: 14px;
}

.field-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #606266;
}

.form-error {
  margin: 0 0 12px;
  font-size: 13px;
  color: #f56c6c;
}

.submit {
  width: 100%;
  height: 44px;
  border: 0;
  border-radius: 8px;
  background: #409eff;
  color: #fff;
  font-size: 15px;
  letter-spacing: .08em;
  cursor: pointer;
  transition: background .2s ease;
}

.submit:hover:not(:disabled) {
  background: #66b1ff;
}

.submit:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.submit:focus-visible {
  outline: 2px solid #409eff;
  outline-offset: 2px;
}

.hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #909399;
}

.hint kbd {
  font-family: ui-monospace, Consolas, monospace;
  font-size: 11px;
  color: #606266;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 1px 5px;
}

.links {
  display: flex;
  justify-content: space-between;
  margin-top: 22px;
  font-size: 13px;
}

.links a {
  color: #409eff;
  text-decoration: none;
}

.links a:hover {
  text-decoration: underline;
}

/* Element Plus 输入框：可见边框优先（覆盖默认的浅色阴影边框） */
:deep(.el-input__wrapper) {
  height: 44px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #d1d5db inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset, 0 0 0 3px rgba(64, 158, 255, .18);
}

:deep(.el-input__inner) {
  font-size: 14px;
}
</style>