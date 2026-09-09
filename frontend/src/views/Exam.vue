<template>
  <div class="exam-container">
    <!-- 考试头部区域 -->
    <div class="exam-header">
      <div class="header-left">
        <h2 class="paper-title">{{ examRecord.paper?.name || '在线考试' }}</h2>
        <div class="student-info" v-if="examRecord.studentName">
          <el-icon><User /></el-icon>
          <span>考生：{{ examRecord.studentName }}</span>
        </div>
      </div>
      <div class="header-right">
        <div class="timer-display">
          <el-icon><Timer /></el-icon>
          <span>剩余时间: {{ formattedTime }}</span>
        </div>
        <el-progress 
          :percentage="answerProgress" 
          :stroke-width="8" 
          class="timer-progress"
        />
        <span class="answer-count-tip" v-if="totalQuestionCount > 0">已作答 {{ answeredCount }}/{{ totalQuestionCount }} 题</span>
      </div>
    </div>

    <!-- 试题区域 -->
    <div class="question-area" v-if="examRecord.paper">
      <div v-for="(group, type) in groupedQuestions" :key="type" class="question-group">
        <h3 class="group-title">{{ getQuestionTypeName(type) }}</h3>
        <div v-for="(question, index) in group" :key="question.id" class="question-card">
          <div class="question-title">
            <span class="question-number">第 {{ question.globalIndex }} 题 ({{ question.paperScore }}分)</span>
            <p class="question-content">{{ question.title }}</p>
          </div>
          <div class="question-options">
            <!-- 单选题 -->
            <el-radio-group v-if="question.type === 'CHOICE' && !question.multi" v-model="answers[question.id]" class="choice-options">
              <el-radio 
                v-for="(choice, optIndex) in question.choices" 
                :key="choice.id" 
                :label="getOptionLabel(optIndex)" 
                class="option-item"
              >
                <span class="option-label">{{ getOptionLabel(optIndex) }}.</span>
                <span class="option-content">{{ choice.content }}</span>
              </el-radio>
            </el-radio-group>
            <!-- 多选题 -->
            <el-checkbox-group v-if="question.type === 'CHOICE' && question.multi" v-model="answers[question.id]" class="choice-options">
              <el-checkbox
                v-for="(choice, optIndex) in question.choices"
                :key="choice.id"
                :label="getOptionLabel(optIndex)"
                class="option-item"
              >
                <span class="option-label">{{ getOptionLabel(optIndex) }}.</span>
                <span class="option-content">{{ choice.content }}</span>
              </el-checkbox>
            </el-checkbox-group>
            <!-- 判断题 -->
            <el-radio-group v-else-if="question.type === 'JUDGE'" v-model="answers[question.id]" class="judge-options">
              <el-radio label="T" class="judge-item">正确</el-radio>
              <el-radio label="F" class="judge-item">错误</el-radio>
            </el-radio-group>
            <!-- 简答题 -->
            <el-input 
              v-else-if="question.type === 'TEXT'" 
              type="textarea" 
              :rows="4" 
              placeholder="请输入你的答案（禁止粘贴，请手动输入）"
              v-model="answers[question.id]"
              class="text-input"
              @paste.prevent="handlePasteAttempt"
              @contextmenu.prevent="handleRightClick"
              @keydown="handleKeyDown"
              autocomplete="off"
              spellcheck="false"
            />
          </div>
        </div>
      </div>
    </div>
    
    <!-- 提交按钮区域 -->
    <div class="submission-footer">
       <el-button type="primary" size="large" @click="submit" :loading="isSubmitting">交卷</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getExamRecordById, submitAnswers } from '../api/exam.js';
import { Timer, User, Loading } from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();

const examRecord = ref({});
const answers = ref({});
const timer = ref(null);
const remainingTime = ref(0);
const totalTime = ref(0);
const isSubmitting = ref(false);

const getExamData = async () => {
  try {
    const res = await getExamRecordById(route.params.id);
    examRecord.value = res.data;
    console.log(examRecord.value);
    // 检查考试状态，如果已完成则自动跳转到结果页面
    if (examRecord.value.status === 'COMPLETED' || examRecord.value.status === '已批阅') {
      ElMessage.warning({
        message: '该考试已完成，正在跳转到结果页面...',
        duration: 2000,
        showClose: false
      });
      
      // 延迟跳转，让用户看到提示信息
      setTimeout(() => {
        router.replace(`/exam-result/${route.params.id}`);
      }, 2000);
      return;
    }
    
    // 如果考试尚未开始或状态异常，也进行相应处理
    if (examRecord.value.status !== '进行中' && examRecord.value.status !== 'IN_PROGRESS') {
      ElMessage.error({
        message: '考试状态异常，正在跳转到考试列表...',
        duration: 2000,
        showClose: false
      });
      
      setTimeout(() => {
        router.replace('/exam/list');
      }, 2000);
      return;
    }
    
    // 正常的考试逻辑
    totalTime.value = (examRecord.value.paper?.duration || 0) * 60;
    remainingTime.value = totalTime.value;
    startTimer();
  } catch (error) {
    console.error('加载考试信息失败:', error);
    console.error('加载考试信息失败:', error);
    console.error('加载考试信息失败:', error);
    console.error('加载考试信息失败:', error);
    ElMessage.error('加载考试信息失败，正在跳转到考试列表...');
    setTimeout(() => {
      router.replace('/exam/list');
    }, 2000);
  }
};

const startTimer = () => {
  timer.value = setInterval(() => {
    if (remainingTime.value > 0) {
      remainingTime.value--;
    } else {
      clearInterval(timer.value);
      // 时间到时强制交卷，不给选择机会
      ElMessage.error({
        message: '⏰ 考试时间已到！系统将在3秒后自动交卷...',
        duration: 3000,
        showClose: false
      });
      
      // 禁用所有输入控件，防止继续答题
      disableAllInputs();
      
      // 3秒后强制提交
      setTimeout(() => {
        forceSubmit();
      }, 3000);
    }
  }, 1000);
};

// 禁用所有输入控件的函数
const disableAllInputs = () => {
  // 禁用所有单选框
  const radioInputs = document.querySelectorAll('.el-radio__input input');
  radioInputs.forEach(input => {
    input.disabled = true;
  });
  
  // 禁用所有多选框
  const checkboxInputs = document.querySelectorAll('.el-checkbox__input input');
  checkboxInputs.forEach(input => {
    input.disabled = true;
  });
  
  // 禁用所有文本框
  const textareas = document.querySelectorAll('.el-textarea__inner');
  textareas.forEach(textarea => {
    textarea.disabled = true;
    textarea.style.backgroundColor = '#f5f5f5';
    textarea.style.cursor = 'not-allowed';
  });
  
  // 在页面顶部显示时间到期提示
  showTimeUpOverlay();
};

// 显示时间到期遮罩
const showTimeUpOverlay = () => {
  const overlay = document.createElement('div');
  overlay.className = 'time-up-overlay';
  overlay.innerHTML = `
    <div class="time-up-content">
      <div class="time-up-icon">⏰</div>
      <h3>考试时间已到</h3>
      <p>系统正在自动交卷，请稍候...</p>
      <div class="countdown-progress"></div>
    </div>
  `;
  document.body.appendChild(overlay);
};

// 强制交卷函数（时间到期时调用）
const forceSubmit = async () => {
  // 防止重复提交
  if (isSubmitting.value) {
    console.log('正在提交中，跳过重复提交');
    return;
  }
  
  isSubmitting.value = true;
  
  try {
    const examRecordId = route.params.id;
    
    if (!examRecordId || examRecordId === 'undefined') {
      throw new Error('考试记录ID无效，请重新开始考试');
    }
    
    // 检查是否已经提交过
    if (examRecord.value.status === 'COMPLETED') {
      console.log('考试已完成，直接跳转结果页面');
      ElMessage.success('考试已完成，正在跳转到结果页面...');
      setTimeout(() => {
        router.push(`/exam-result/${examRecordId}`);
      }, 1500);
      return;
    }
    
    const formattedAnswers = Object.entries(answers.value).map(([questionId, answer]) => ({
      questionId: Number(questionId),
      userAnswer: Array.isArray(answer) ? answer.sort().join(',') : answer
    }));
    
    await submitAnswers(examRecordId, formattedAnswers);
    
    // 移除时间到期遮罩
    const overlay = document.querySelector('.time-up-overlay');
    if (overlay) {
      overlay.remove();
    }
    
    ElMessage.success('时间到期，系统已自动交卷！AI 判卷进行中，请稍候');
    
    // 提交成功后直接进入结果页（AI 判卷在后台异步进行）
    router.push(`/exam-result/${examRecordId}`);
    
  } catch (error) {
    console.error('自动交卷失败:', error);
    
    // 移除时间到期遮罩
    const overlay = document.querySelector('.time-up-overlay');
    if (overlay) {
      overlay.remove();
    }
    
    // 如果是重复提交错误，直接跳转
    if (error.message && error.message.includes('已完成')) {
      ElMessage.success('考试已完成，正在跳转到结果页面...');
      setTimeout(() => {
        router.push(`/exam-result/${route.params.id}`);
      }, 1500);
    } else {
      ElMessage.error('自动交卷失败，正在跳转到结果页面...');
      setTimeout(() => {
        router.push(`/exam-result/${route.params.id}`);
      }, 2000);
    }
  } finally {
    isSubmitting.value = false;
  }
};

// 格式化时间显示
const formattedTime = computed(() => {
  const minutes = Math.floor(remainingTime.value / 60);
  const seconds = remainingTime.value % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
});

// 尝试将进度条与答题进度绑定（原为基于时间）
const totalQuestionCount = computed(() => {
  return examRecord.value.paper?.questions?.length || 0;
});

const answeredCount = computed(() => {
  const qs = examRecord.value.paper?.questions || [];
  if (qs.length === 0) return 0;
  return qs.filter(q => {
    const v = answers.value[q.id];
    if (Array.isArray(v)) return v.length > 0;
    return v != null && String(v).trim() !== '';
  }).length;
});

// 答题进度条百分比
const answerProgress = computed(() => {
  const total = totalQuestionCount.value;
  if (total === 0) return 0;
  return Math.floor((answeredCount.value / total) * 100);
});

const getOptionLabel = (index) => {
  return String.fromCharCode(65 + index); // A, B, C, D...
};

const groupedQuestions = computed(() => {
  if (!examRecord.value.paper?.questions) {
    return {};
  }
  let globalIndex = 0;
  const groups = examRecord.value.paper.questions.reduce((acc, q) => {
    q.globalIndex = ++globalIndex; // 添加全局索引

    // 初始化答案容器
    if (q.type === 'CHOICE' && q.multi) {
      answers.value[q.id] = []; // 多选题初始化为空数组
    } else {
      answers.value[q.id] = ''; // 其他题型初始化为空字符串
    }

    if (!acc[q.type]) {
      acc[q.type] = [];
    }
    acc[q.type].push(q);
    return acc;
  }, {});
  return groups;
});

const getQuestionTypeName = (type) => {
  const map = {
    'CHOICE': '一、选择题',
    'JUDGE': '二、判断题',
    'TEXT': '三、简答题'
  };
  return map[type] || '其他题目';
};

const submit = async () => {
  // 防止重复提交
  if (isSubmitting.value) {
    ElMessage.warning('正在交卷中，请稍候...');
    return;
  }
  
  // 检查是否已经提交过
  if (examRecord.value.status === 'COMPLETED') {
    ElMessage.success('考试已完成，正在跳转到结果页面...');
    setTimeout(() => {
      router.push(`/exam-result/${route.params.id}`);
    }, 1500);
    return;
  }
  
  try {
    await ElMessageBox.confirm(
      '确定要交卷吗？交卷后将无法修改答案。',
      '确认交卷',
      {
        confirmButtonText: '确定交卷',
        cancelButtonText: '继续答题',
        type: 'warning',
      }
    );
  } catch (error) {
    return;
  }

  isSubmitting.value = true;
  const formattedAnswers = Object.entries(answers.value).map(([questionId, answer]) => ({
    questionId: Number(questionId),
    // 对多选题的答案(数组)进行处理
    userAnswer: Array.isArray(answer) ? answer.sort().join(',') : answer
  }));
  
  try {
    // 获取考试记录ID，添加调试信息
    const examRecordId = route.params.id;
    console.log('当前考试记录ID:', examRecordId);
    console.log('提交的答案:', formattedAnswers);
    
    if (!examRecordId || examRecordId === 'undefined') {
      throw new Error('考试记录ID无效，请重新开始考试');
    }
    
    // 提交答案
    await submitAnswers(examRecordId, formattedAnswers);
    ElMessage.closeAll();
    ElMessage.success('交卷成功！AI 判卷进行中，请稍候进入结果页');
    
    // 提交成功后直接进入结果页（AI 判卷在后台异步执行，结果页会自动轮询）
    router.push(`/exam-result/${examRecordId}`);
    
  } catch (error) {
    console.error('提交试卷失败:', error);
    
    // 如果是重复提交错误，直接跳转
    if (error.message && error.message.includes('已完成')) {
      ElMessage.success('考试已完成，正在跳转到结果页面...');
      setTimeout(() => {
        router.push(`/exam-result/${route.params.id}`);
      }, 1500);
    } else {
      ElMessage.error(error.message || '交卷失败，请稍后重试');
    }
  } finally {
    isSubmitting.value = false;
  }
};

// 禁止粘贴相关函数
const handlePasteAttempt = () => {
  ElMessage.warning('为保证考试公平性，简答题禁止粘贴内容，请手动输入答案！');
};

const handleRightClick = () => {
  ElMessage.warning('考试期间禁止右键操作！');
};

const handleKeyDown = (event) => {
  // 阻止Ctrl+V粘贴
  if ((event.ctrlKey || event.metaKey) && event.key === 'v') {
    event.preventDefault();
    ElMessage.warning('为保证考试公平性，简答题禁止粘贴内容，请手动输入答案！');
    return;
  }
  
  // 阻止Ctrl+A全选（可选，根据需要启用）
  // if ((event.ctrlKey || event.metaKey) && event.key === 'a') {
  //   event.preventDefault();
  //   ElMessage.warning('考试期间禁止全选操作！');
  //   return;
  // }
  
  // 阻止F12开发者工具（可选）
  if (event.key === 'F12') {
    event.preventDefault();
    ElMessage.warning('考试期间禁止打开开发者工具！');
    return;
  }
};

onMounted(() => {
  getExamData();
});

onUnmounted(() => {
  clearInterval(timer.value);
});
</script>

<style scoped>
/* 考试容器主体 */
.exam-container {
  max-width: 1000px; /* 设置最大宽度 */
  margin: 20px auto; /* 居中显示 */
  background-color: #ffffff; /* 纯白背景 */
  border: 1px solid #ddd; /* 简单灰色边框 */
  overflow: hidden; /* 隐藏溢出内容 */
}

/* 考试头部样式 */
.exam-header {
  background-color: #f5f5f5; /* 浅灰背景 */
  padding: 20px; /* 内边距 */
  border-bottom: 1px solid #ddd; /* 底部边框 */
  display: flex; /* 弹性布局 */
  justify-content: space-between; /* 两端对齐 */
  align-items: center; /* 垂直居中 */
  flex-wrap: wrap; /* 允许换行 */
  gap: 16px; /* 项目间距 */
}

.header-left .paper-title {
  font-size: 20px; /* 标题字体大小 */
  font-weight: normal; /* 正常字体粗细 */
  color: #333; /* 深灰色文字 */
  margin: 0 0 8px 0; /* 外边距 */
}

.student-info {
  display: flex; /* 弹性布局 */
  align-items: center; /* 垂直居中 */
  gap: 8px; /* 图标与文字间距 */
  color: #666; /* 灰色文字 */
  font-size: 14px; /* 字体大小 */
}

.header-right {
  display: flex; /* 弹性布局 */
  flex-direction: column; /* 垂直排列 */
  align-items: flex-end; /* 右对齐 */
  gap: 8px; /* 项目间距 */
  min-width: 200px; /* 最小宽度 */
}

.timer-display {
  display: flex; /* 弹性布局 */
  align-items: center; /* 垂直居中 */
  gap: 8px; /* 图标与文字间距 */
  color: #d32f2f; /* 红色提醒 */
  font-weight: normal; /* 正常字体粗细 */
  font-size: 16px; /* 字体大小 */
}

.timer-progress {
  width: 100%; /* 占满宽度 */
}

.answer-count-tip {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  text-align: right;
}

/* 题目区域样式 */
.question-area {
  padding: 20px; /* 内边距 */
}

.question-group {
  margin-bottom: 30px; /* 组间距 */
  border: 1px solid #ddd; /* 简单边框 */
}

.group-title {
  font-size: 16px; /* 标题字体大小 */
  font-weight: normal; /* 正常字体粗细 */
  margin: 0; /* 清除外边距 */
  padding: 12px 16px; /* 内边距 */
  background-color: #f0f0f0; /* 浅灰背景 */
  color: #333; /* 深灰文字 */
  border-bottom: 1px solid #ddd; /* 底部边框 */
}

.question-card {
  padding: 20px; /* 内边距 */
  border-bottom: 1px solid #eee; /* 底部边框 */
  background-color: #ffffff; /* 白色背景 */
}

.question-card:last-child {
  border-bottom: none; /* 最后一个不显示底部边框 */
}

.question-title {
  margin-bottom: 16px; /* 底部间距 */
}

.question-number {
  display: block; /* 块级显示 */
  font-weight: normal; /* 正常字体粗细 */
  color: #666; /* 灰色题号 */
  margin-bottom: 8px; /* 底部间距 */
  font-size: 14px; /* 字体大小 */
}

.question-content {
  margin: 0; /* 清除外边距 */
  color: #333; /* 深灰文字 */
  line-height: 1.5; /* 行高 */
  font-size: 16px; /* 字体大小 */
}

/* 选择题选项样式 - 左对齐 */
.choice-options {
  display: flex; /* 弹性布局 */
  flex-direction: column; /* 垂直排列 */
  gap: 8px; /* 选项间距 */
  align-items: flex-start; /* 左对齐 */
}

.choice-options .option-item {
  display: flex; /* 弹性布局 */
  align-items: flex-start; /* 顶部对齐 */
  padding: 8px 12px; /* 内边距 */
  border: 1px solid #ddd; /* 简单边框 */
  background-color: #ffffff; /* 白色背景 */
  cursor: pointer; /* 鼠标指针 */
  margin: 0; /* 清除外边距 */
  min-width: 300px; /* 最小宽度确保对齐效果 */
  max-width: 500px; /* 最大宽度 */
}

.choice-options .option-item:hover {
  background-color: #f9f9f9; /* 悬停时背景变浅灰 */
}

.choice-options .el-radio__input.is-checked + .el-radio__label,
.choice-options .el-checkbox__input.is-checked + .el-checkbox__label {
  background-color: #e8f4f8 !important; /* 选中时背景变浅蓝 */
}

.option-label {
  font-weight: normal; /* 正常字体粗细 */
  color: #666; /* 灰色标签 */
  margin-right: 8px; /* 右侧间距 */
  min-width: 20px; /* 最小宽度 */
}

.option-content {
  flex: 1; /* 占满剩余空间 */
  color: #333; /* 深灰文字 */
  line-height: 1.4; /* 行高 */
}

/* 判断题样式 - 左对齐 */
.judge-options {
  display: flex; /* 弹性布局 */
  gap: 12px; /* 选项间距 */
  justify-content: flex-start; /* 左对齐 */
}

.judge-options .judge-item {
  padding: 8px 16px; /* 内边距 */
  border: 1px solid #ddd; /* 简单边框 */
  background-color: #ffffff; /* 白色背景 */
  color: #333; /* 深灰文字 */
  font-weight: normal; /* 正常字体粗细 */
  cursor: pointer; /* 鼠标指针 */
  margin: 0; /* 清除外边距 */
  min-width: 60px; /* 最小宽度 */
  text-align: center; /* 文字居中 */
}

.judge-options .judge-item:hover {
  background-color: #f9f9f9; /* 悬停时背景变浅灰 */
}

.judge-options .el-radio__input.is-checked + .el-radio__label {
  background-color: #e8f4f8 !important; /* 选中时背景变浅蓝 */
}

/* 简答题输入框样式 - 左对齐 */
.text-input {
  margin-top: 8px; /* 顶部间距 */
  display: flex; /* 弹性布局 */
  justify-content: flex-start; /* 左对齐 */
}

.text-input .el-textarea {
  max-width: 500px; /* 最大宽度 */
  width: 100%; /* 占满容器宽度 */
}

.text-input .el-textarea__inner {
  border: 1px solid #ddd; /* 简单边框 */
  padding: 12px; /* 内边距 */
  font-size: 14px; /* 字体大小 */
  line-height: 1.4; /* 行高 */
  resize: vertical; /* 只允许垂直调整大小 */
  background-color: #ffffff; /* 白色背景 */
  position: relative; /* 相对定位 */
  /* 防止复制粘贴的CSS样式 */
  -webkit-user-select: text; /* 允许选择文本但限制操作 */
  -moz-user-select: text;
  -ms-user-select: text;
  user-select: text;
}

.text-input .el-textarea__inner:focus {
  border-color: #999; /* 聚焦时边框变灰 */
  outline: none; /* 去除默认聚焦轮廓 */
  box-shadow: 0 0 0 2px rgba(153, 153, 153, 0.2); /* 聚焦时添加淡阴影 */
}

/* 为简答题添加防作弊提示 */
.text-input::before {
  content: "⚠️ 此区域禁止粘贴"; /* 提示内容 */
  position: absolute; /* 绝对定位 */
  top: -20px; /* 顶部位置 */
  right: 0; /* 右侧对齐 */
  font-size: 12px; /* 字体大小 */
  color: #ff6b6b; /* 红色警告文字 */
  background-color: #ffe8e8; /* 浅红色背景 */
  padding: 2px 8px; /* 内边距 */
  border-radius: 4px; /* 圆角 */
  border: 1px solid #ffcdd2; /* 浅红色边框 */
  z-index: 10; /* 层级 */
  font-weight: 500; /* 字体加粗 */
}

/* 提交按钮区域 */
.submission-footer {
  text-align: center; /* 文字居中 */
  padding: 20px; /* 内边距 */
  background-color: #f5f5f5; /* 浅灰背景 */
  border-top: 1px solid #ddd; /* 顶部边框 */
}

.submission-footer .el-button {
  padding: 10px 30px; /* 内边距 */
  font-size: 14px; /* 字体大小 */
  font-weight: normal; /* 正常字体粗细 */
  background-color: #666; /* 灰色背景 */
  border-color: #666; /* 灰色边框 */
  color: #ffffff; /* 白色文字 */
}

.submission-footer .el-button:hover {
  background-color: #555; /* 悬停时背景变深灰 */
  border-color: #555; /* 悬停时边框变深灰 */
}

/* AI判卷遮罩样式 */
.grading-overlay {
  position: fixed; /* 固定定位 */
  top: 0; /* 顶部对齐 */
  left: 0; /* 左侧对齐 */
  width: 100%; /* 占满宽度 */
  height: 100%; /* 占满高度 */
  background: rgba(0, 0, 0, 0.5); /* 半透明黑色背景 */
  display: flex; /* 弹性布局 */
  justify-content: center; /* 水平居中 */
  align-items: center; /* 垂直居中 */
  z-index: 1000; /* 层级 */
}

.grading-content {
  background: #ffffff; /* 白色背景 */
  padding: 30px; /* 内边距 */
  border: 1px solid #ddd; /* 简单边框 */
  text-align: center; /* 文字居中 */
  max-width: 350px; /* 最大宽度 */
  width: 80%; /* 宽度占比 */
}

.grading-content h3 {
  margin: 0 0 12px 0; /* 外边距 */
  font-size: 18px; /* 字体大小 */
  font-weight: normal; /* 正常字体粗细 */
  color: #333; /* 深灰文字 */
}

.grading-content p {
  margin: 0 0 20px 0; /* 外边距 */
  font-size: 14px; /* 字体大小 */
  color: #666; /* 灰色文字 */
  line-height: 1.4; /* 行高 */
}

.grading-icon {
  margin-bottom: 16px; /* 底部间距 */
}

.grading-icon .el-icon {
  font-size: 36px; /* 图标大小 */
  color: #666; /* 灰色图标 */
}

.grading-progress {
  margin-bottom: 16px; /* 底部间距 */
}

.progress-text {
  margin-top: 8px; /* 顶部间距 */
  font-size: 14px; /* 字体大小 */
  font-weight: normal; /* 正常字体粗细 */
  color: #333; /* 深灰文字 */
}

/* 响应式设计 */
@media (max-width: 768px) {
  .exam-container {
    margin: 10px; /* 移动端边距 */
  }
  
  .exam-header {
    padding: 16px; /* 移动端内边距 */
    flex-direction: column; /* 垂直排列 */
    text-align: center; /* 文字居中 */
    gap: 12px; /* 项目间距 */
  }
  
  .header-right {
    width: 100%; /* 占满宽度 */
    align-items: center; /* 居中对齐 */
  }
  
  .question-area {
    padding: 16px; /* 移动端内边距 */
  }
  
  .question-card {
    padding: 16px; /* 移动端内边距 */
  }
  
  .choice-options {
    align-items: stretch; /* 拉伸对齐，移动端占满宽度 */
  }
  
  .choice-options .option-item {
    min-width: auto; /* 移动端取消最小宽度限制 */
    max-width: none; /* 移动端取消最大宽度限制 */
  }
  
  .judge-options {
    justify-content: flex-start; /* 移动端也保持左对齐 */
    flex-wrap: wrap; /* 允许换行 */
  }
  
  .text-input {
    justify-content: stretch; /* 移动端拉伸对齐 */
  }
  
  .text-input .el-textarea {
    max-width: none; /* 移动端取消最大宽度限制 */
  }
}

/* 时间到期遮罩样式 */
.time-up-overlay {
  position: fixed; /* 固定定位 */
  top: 0; /* 顶部对齐 */
  left: 0; /* 左侧对齐 */
  width: 100%; /* 占满宽度 */
  height: 100%; /* 占满高度 */
  background: rgba(220, 53, 69, 0.9); /* 红色半透明背景 */
  backdrop-filter: blur(8px); /* 背景模糊效果 */
  display: flex; /* 弹性布局 */
  justify-content: center; /* 水平居中 */
  align-items: center; /* 垂直居中 */
  z-index: 9999; /* 最高层级 */
  animation: slideDown 0.5s ease-out; /* 滑入动画 */
}

.time-up-content {
  background: #ffffff; /* 白色背景 */
  padding: 40px; /* 内边距 */
  border-radius: 12px; /* 圆角 */
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3); /* 阴影 */
  text-align: center; /* 文字居中 */
  max-width: 400px; /* 最大宽度 */
  width: 90%; /* 宽度占比 */
  border: 3px solid #dc3545; /* 红色边框 */
}

.time-up-icon {
  font-size: 60px; /* 图标大小 */
  margin-bottom: 20px; /* 底部间距 */
  animation: pulse 1s infinite; /* 脉冲动画 */
}

.time-up-content h3 {
  margin: 0 0 16px 0; /* 外边距 */
  font-size: 24px; /* 字体大小 */
  font-weight: 600; /* 字体加粗 */
  color: #dc3545; /* 红色文字 */
}

.time-up-content p {
  margin: 0 0 24px 0; /* 外边距 */
  font-size: 16px; /* 字体大小 */
  color: #6c757d; /* 灰色文字 */
  line-height: 1.5; /* 行高 */
}

.countdown-progress {
  width: 100%; /* 占满宽度 */
  height: 4px; /* 进度条高度 */
  background-color: #f8f9fa; /* 背景色 */
  border-radius: 2px; /* 圆角 */
  position: relative; /* 相对定位 */
  overflow: hidden; /* 隐藏溢出 */
}

.countdown-progress::before {
  content: ''; /* 空内容 */
  position: absolute; /* 绝对定位 */
  top: 0; /* 顶部对齐 */
  left: 0; /* 左侧对齐 */
  height: 100%; /* 占满高度 */
  background: linear-gradient(90deg, #dc3545, #ff6b6b); /* 红色渐变 */
  border-radius: 2px; /* 圆角 */
  animation: countdown 3s linear; /* 倒计时动画 */
}

/* 滑入动画 */
@keyframes slideDown {
  from {
    opacity: 0; /* 开始透明 */
    transform: translateY(-50px); /* 开始向上偏移 */
  }
  to {
    opacity: 1; /* 结束不透明 */
    transform: translateY(0); /* 结束无偏移 */
  }
}

/* 脉冲动画 */
@keyframes pulse {
  0%, 100% { 
    transform: scale(1); /* 正常大小 */
    opacity: 1; /* 不透明 */
  }
  50% { 
    transform: scale(1.1); /* 放大 */
    opacity: 0.8; /* 半透明 */
  }
}

/* 倒计时进度条动画 */
@keyframes countdown {
  from { width: 0%; } /* 开始空 */
  to { width: 100%; } /* 结束满 */
}

/* 禁用状态的输入控件样式 */
:deep(.el-radio.is-disabled .el-radio__input) {
  cursor: not-allowed !important; /* 禁用光标 */
}

:deep(.el-checkbox.is-disabled .el-checkbox__input) {
  cursor: not-allowed !important; /* 禁用光标 */
}

:deep(.el-textarea.is-disabled .el-textarea__inner) {
  background-color: #f5f5f5 !important; /* 禁用背景色 */
  cursor: not-allowed !important; /* 禁用光标 */
  color: #999 !important; /* 禁用文字颜色 */
}
</style> 