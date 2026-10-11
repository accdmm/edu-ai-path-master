<template>
  <div class="login-page">
    <AuthBackground variant="aurora" />

    <main class="auth-col">
      <header class="brand">
        <img src="@/assets/logo.svg" alt="" class="brand-mark" />
        <h1 class="brand-name">智能学习平台</h1>
      </header>

      <form class="auth-form" @submit.prevent="handleRegister">
        <label class="field">
          <span class="field-label">用户名</span>
          <el-input
            v-model="registerForm.username"
            size="large"
            placeholder="3-20 个字符"
            autocomplete="username"
            :disabled="loading"
          />
        </label>

        <label class="field">
          <span class="field-label">密码</span>
          <el-input
            v-model="registerForm.password"
            type="password"
            size="large"
            placeholder="至少 6 位"
            autocomplete="new-password"
            show-password
            :disabled="loading"
          />
        </label>

        <label class="field">
          <span class="field-label">确认密码</span>
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            size="large"
            placeholder="请再次输入密码"
            autocomplete="new-password"
            show-password
            :disabled="loading"
          />
        </label>

        <p v-if="errorMsg" class="form-error" role="alert" aria-live="polite">{{ errorMsg }}</p>

        <button class="submit" type="submit" :disabled="loading">
          {{ loading ? '注册中…' : '注 册' }}
        </button>

        <p class="hint">按 <kbd>Enter</kbd> 快速提交</p>

        <nav class="links">
          <router-link to="/login">已有账号？立即登录</router-link>
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
import { register } from '@/api/user'
import AuthBackground from '@/components/AuthBackground.vue'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const validate = () => {
  const username = registerForm.username.trim()
  if (!username) return '请输入用户名'
  if (username.length < 3 || username.length > 20) return '用户名长度在 3-20 个字符'
  if (!registerForm.password) return '请输入密码'
  if (registerForm.password.length < 6) return '密码长度不能小于 6 位'
  if (registerForm.confirmPassword !== registerForm.password) return '两次输入的密码不一致'
  return ''
}

const handleRegister = async () => {
  errorMsg.value = validate()
  if (errorMsg.value) return

  loading.value = true
  try {
    await register(registerForm)
    errorMsg.value = ''
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    // 行内错误（role=alert 可被读屏播报）；弹窗提示由 axios 拦截器统一负责
    errorMsg.value = (error && error.message) ? error.message : '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
/* 极简单列：与登录页同一套视觉语言（零装饰、白底、可见边框、单一强调色） */
.login-page {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  background: #fff;
  padding: 24px;
}

.auth-col {
  position: relative;
  z-index: 1; /* 内容层压在背景动画之上 */
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