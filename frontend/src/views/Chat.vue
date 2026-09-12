<template>
  <div class="chat-full-container">
    <!-- 顶部导航栏 -->
    <div class="chat-header">
      <div class="header-left">
        <el-button type="text" class="back-btn" @click="goBack">
          <el-icon><arrow-left /></el-icon> 返回
        </el-button>
        <div class="header-title">
          <el-icon size="24" class="header-icon"><cpu /></el-icon>
          <div class="title-text">
            <h2>AI 智能客服</h2>
            <p>有任何问题都可以问我哦</p>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <el-button
          size="small"
          type="danger"
          plain
          @click="handleClearAll"
          :disabled="chatRecords.length === 0"
        >
          <el-icon><delete /></el-icon> 清空记录
        </el-button>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="chat-body">
      <!-- 左侧边栏 - 聊天记录列表 -->
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <div class="header-title">
            <el-icon size="20"><chat-line-round /></el-icon>
            <span>最近对话</span>
          </div>
          <el-tag size="small" type="info">{{ chatRecords.length }} 条</el-tag>
        </div>

        <div class="sidebar-actions">
          <el-button
            type="primary"
            size="small"
            @click="handleNewChat"
            class="new-chat-btn"
          >
            <el-icon><plus /></el-icon> 新建对话
          </el-button>
        </div>

        <div class="record-list">
          <div
            v-for="record in chatRecords"
            :key="record.id"
            class="record-item"
            :class="{ active: currentRecordId === record.id }"
            @click="loadChatRecord(record.id)"
          >
            <div class="record-content">
              <div class="record-icon">
                <el-icon size="18"><chat-dot-square /></el-icon>
              </div>
              <div class="record-info">
                <div class="record-title">{{ record.title || '新对话' }}</div>
                <div class="record-time">{{ formatTime(record.createTime) }}</div>
              </div>
            </div>
            <el-button
              class="delete-btn"
              type="text"
              size="small"
              @click.stop="handleDeleteRecord(record.id)"
            >
              <el-icon><close /></el-icon>
            </el-button>
          </div>
          <div v-if="chatRecords.length === 0" class="empty-tip">
            <el-empty description="暂无聊天记录" :image-size="60" />
          </div>
        </div>
      </div>

      <!-- 主聊天区域 -->
      <div class="chat-main">
        <!-- 消息列表 -->
        <div class="message-list" ref="messageListRef">
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="message-item"
            :class="message.role"
          >
            <div class="message-avatar">
              <el-icon v-if="message.role === 'user'" size="24"><user /></el-icon>
              <el-icon v-else size="24"><cpu /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-bubble" v-html="renderContent(message.content)"></div>
              <div class="message-time">{{ message.time }}</div>
            </div>
          </div>
          <div v-if="isLoading" class="message-item ai">
            <div class="message-avatar">
              <el-icon size="24"><cpu /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-bubble loading">
                <span class="dot">正在思考</span>
                <span class="dots">...</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 - 居中设计 -->
        <div class="input-area">
          <div class="input-wrapper">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="请输入您的问题，例如：什么是缓存击穿？"
              resize="none"
              @keydown.ctrl.enter="handleSend"
            />
            <div class="input-actions">
              <div class="tips">
                <el-text size="small" type="info">按 Ctrl+Enter 发送消息</el-text>
              </div>
              <el-button
                type="primary"
                size="large"
                :loading="isLoading"
                @click="handleSend"
                class="send-btn"
              >
                <el-icon><chat-dot-round /></el-icon> 发送
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <BackHome />
  </template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User,
  Cpu,
  Delete,
  Close,
  ChatDotRound,
  ChatLineRound,
  ChatDotSquare,
  Plus,
  ArrowLeft
} from '@element-plus/icons-vue'
import { sendChatMessage, getChatHistory, deleteChatRecord, clearAllChatRecords } from '@/api/chat'

const router = useRouter()
const messageListRef = ref(null)
const isLoading = ref(false)
const inputMessage = ref('')
const messages = ref([])
const chatRecords = ref([])
const currentRecordId = ref(null)

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`

  return date.toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 将聊天内容渲染为 HTML：支持换行与 /exam/start/{id} 试卷入口链接；其余文本 HTML 转义防 XSS
const escapeHtml = (text) => {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const renderContent = (content) => {
  if (!content) return ''
  const escaped = escapeHtml(content)
  return escaped
    .replace(/\/exam\/start\/(\d+)/g, '<a href="/exam/start/$1" style="color:#4fc3f7;text-decoration:underline;">点击开始考试</a>')
    .replace(/\n/g, '<br/>')
}

// 获取当前时间字符串
const getCurrentTime = () => {
  const now = new Date()
  return now.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 新建对话
const handleNewChat = () => {
  currentRecordId.value = null
  messages.value = []
  inputMessage.value = ''
  ElMessage.success('已新建对话')
}

// 加载聊天记录列表
const loadChatRecords = async () => {
  try {
    const res = await getChatHistory()
    chatRecords.value = res.data || []
    if (chatRecords.value.length > 0 && !currentRecordId.value) {
      loadChatRecord(chatRecords.value[0].id)
    }
  } catch (error) {
    console.error('加载聊天记录失败:', error)
    ElMessage.warning('加载聊天记录失败，请稍后重试')
  }
}

// 从 LangChain4j 序列化格式中提取消息文本
// LangChain4j: USER {type:"USER", contents:[{type:"TEXT",text:"..."}]}  AI {type:"AI",text:"..."}
const extractMessageText = (msg) => {
  const node = msg.contents || msg.content
  if (node) {
    if (Array.isArray(node)) {
      return node
        .filter(c => c && (c.type === 'TEXT' || c.text))
        .map(c => c.text || '')
        .join('')
    }
    return node
  }
  if (msg.text) return msg.text
  return ''
}

// 加载指定聊天记录
const loadChatRecord = (id) => {
  currentRecordId.value = id
  const record = chatRecords.value.find(r => r.id === id)
  if (record && record.content) {
    try {
      const parsedMessages = JSON.parse(record.content)
      messages.value = parsedMessages
        .filter(msg => msg.type !== 'SYSTEM')
        .map(msg => ({
        role: (msg.type === 'USER' || msg.role === 'USER' || msg.role === 'user') ? 'user' : 'ai',
        content: extractMessageText(msg),
        time: getCurrentTime()
      }))
      scrollToBottom()
    } catch (e) {
      console.error('解析聊天记录失败:', e)
    }
  }
}

// 删除聊天记录
const handleDeleteRecord = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条聊天记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteChatRecord(id)
    ElMessage.success('删除成功')

    await loadChatRecords()

    if (currentRecordId.value === id) {
      messages.value = []
      currentRecordId.value = null
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 清空所有记录
const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有聊天记录吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await clearAllChatRecords()
    ElMessage.success('清空成功')

    chatRecords.value = []
    messages.value = []
    currentRecordId.value = null
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空失败:', error)
    }
  }
}

// 发送消息
const handleSend = async () => {
  const content = inputMessage.value.trim()
  if (!content || isLoading.value) return

  messages.value.push({
    role: 'user',
    content: content,
    time: getCurrentTime()
  })

  inputMessage.value = ''
  isLoading.value = true
  scrollToBottom()

  try {
    const res = await sendChatMessage({
      content: content
    })

    messages.value.push({
      role: 'ai',
      content: res.data,
      time: getCurrentTime()
    })

    isLoading.value = false
    scrollToBottom()

    await loadChatRecords()
  } catch (error) {
    console.error('发送消息失败:', error)
    messages.value.push({
      role: 'ai',
      content: '抱歉，服务器开小差了，请稍后再试~',
      time: getCurrentTime()
    })
    isLoading.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  loadChatRecords()
})
</script>

<style scoped lang="scss">
.chat-full-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0f0f1a;
  overflow: hidden;
}

// 顶部导航栏
.chat-header {
  height: 70px;
  padding: 0 30px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16162a 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 20px;

    .back-btn {
      color: rgba(255, 255, 255, 0.8);
      font-size: 14px;
      padding: 8px 12px;
      border-radius: 8px;
      transition: all 0.3s;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
        color: #fff;
      }
    }

    .header-title {
      display: flex;
      align-items: center;
      gap: 15px;

      .header-icon {
        color: #4fc3f7;
        background: rgba(79, 195, 247, 0.1);
        padding: 10px;
        border-radius: 12px;
      }

      .title-text {
        h2 {
          margin: 0;
          font-size: 20px;
          color: #fff;
          font-weight: 600;
        }

        p {
          margin: 3px 0 0 0;
          font-size: 13px;
          color: rgba(255, 255, 255, 0.5);
        }
      }
    }
  }
}

// 主体内容
.chat-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

// 左侧边栏
.chat-sidebar {
  width: 300px;
  min-width: 300px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16162a 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .sidebar-header {
    padding: 20px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #fff;
      font-size: 15px;
      font-weight: 600;
    }
  }

  .sidebar-actions {
    padding: 15px 20px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);

    .new-chat-btn {
      width: 100%;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
    }
  }

  .record-list {
    flex: 1;
    overflow-y: auto;
    padding: 10px;

    .record-item {
      padding: 12px 15px;
      margin-bottom: 8px;
      background: rgba(255, 255, 255, 0.03);
      border-radius: 12px;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      align-items: center;
      transition: all 0.3s;
      border: 1px solid transparent;

      &:hover {
        background: rgba(255, 255, 255, 0.06);
        border-color: rgba(255, 255, 255, 0.1);
      }

      &.active {
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
        border-color: rgba(102, 126, 234, 0.5);
      }

      .record-content {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 0;

        .record-icon {
          color: rgba(255, 255, 255, 0.6);
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .record-info {
          flex: 1;
          min-width: 0;

          .record-title {
            color: #fff;
            font-size: 14px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            margin-bottom: 4px;
            font-weight: 500;
          }

          .record-time {
            color: rgba(255, 255, 255, 0.4);
            font-size: 12px;
          }
        }
      }

      .delete-btn {
        opacity: 0;
        color: rgba(255, 255, 255, 0.4);
        padding: 4px;
        transition: all 0.3s;

        &:hover {
          color: #ff6b6b;
          transform: scale(1.1);
        }
      }

      &:hover .delete-btn {
        opacity: 1;
      }
    }

    .empty-tip {
      text-align: center;
      padding: 60px 20px;
      color: rgba(255, 255, 255, 0.3);
    }
  }
}

// 主聊天区域
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #0f0f1a;
  overflow: hidden;

  .message-list {
    flex: 1;
    overflow-y: auto;
    padding: 30px;
    scroll-behavior: smooth;

    .message-item {
      display: flex;
      margin-bottom: 25px;
      animation: fadeIn 0.3s ease;

      &.user {
        flex-direction: row-reverse;

        .message-bubble {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: #fff;
          border-bottom-right-radius: 4px;
        }

        .message-time {
          text-align: right;
          color: rgba(255, 255, 255, 0.5);
        }
      }

      &.ai {
        .message-bubble {
          background: rgba(255, 255, 255, 0.08);
          color: #e0e0e0;
          border-bottom-left-radius: 4px;

          &.loading {
            .dots {
              animation: blink 1.4s infinite both;
            }
          }
        }

        .message-time {
          color: rgba(255, 255, 255, 0.5);
        }
      }

      .message-avatar {
        width: 42px;
        height: 42px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.05);
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 12px;
        flex-shrink: 0;
        color: #4fc3f7;

        &.user {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: #fff;
        }
      }

      .message-content {
        max-width: 70%;
        display: flex;
        flex-direction: column;

        .message-bubble {
          padding: 14px 18px;
          border-radius: 16px;
          line-height: 1.6;
          word-wrap: break-word;
          white-space: pre-wrap;
          font-size: 14px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .message-time {
          font-size: 12px;
          margin-top: 6px;
          padding: 0 4px;
        }
      }
    }
  }

  .input-area {
    padding: 25px 40px;
    background: rgba(255, 255, 255, 0.03);
    border-top: 1px solid rgba(255, 255, 255, 0.08);

    .input-wrapper {
      max-width: 900px;
      margin: 0 auto;
      width: 100%;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 16px;
      padding: 20px;
      border: 1px solid rgba(255, 255, 255, 0.1);

      .el-textarea {
        :deep(.el-textarea__inner) {
          background: rgba(255, 255, 255, 0.08);
          border: 1px solid rgba(255, 255, 255, 0.15);
          color: #fff;
          border-radius: 12px;
          resize: none;
          padding: 15px;
          font-size: 14px;
          min-height: 80px !important;

          &:focus {
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
            background: rgba(255, 255, 255, 0.1);
          }

          &::placeholder {
            color: rgba(255, 255, 255, 0.4);
          }
        }
      }

      .input-actions {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 15px;
        padding-top: 15px;
        border-top: 1px solid rgba(255, 255, 255, 0.08);

        .tips {
          color: rgba(255, 255, 255, 0.4);
          font-size: 13px;
        }

        .send-btn {
          min-width: 130px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border: none;
          border-radius: 12px;
          padding: 14px 35px;
          font-size: 15px;
          font-weight: 500;
          transition: all 0.3s;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
          }

          &:active {
            transform: translateY(0);
          }
        }
      }
    }
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes blink {
  0% { opacity: 0.2; }
  20% { opacity: 1; }
  100% { opacity: 0.2; }
}

// 滚动条样式
.chat-sidebar {
  .record-list {
    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-track {
      background: rgba(255, 255, 255, 0.02);
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.1);
      border-radius: 3px;

      &:hover {
        background: rgba(255, 255, 255, 0.2);
      }
    }
  }
}

.message-list {
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.02);
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 3px;

    &:hover {
      background: rgba(255, 255, 255, 0.2);
    }
  }
}

:deep(.el-empty__description) {
  color: rgba(255, 255, 255, 0.4);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

:deep(.el-button--danger.is-plain) {
  background: rgba(244, 67, 54, 0.1);
  border-color: rgba(244, 67, 54, 0.3);
  color: #f44336;

  &:hover {
    background: rgba(244, 67, 54, 0.2);
    border-color: rgba(244, 67, 54, 0.5);
  }
}
</style>
