<template>
  <!-- 考场桌面背景（浅蓝） -->
  <div class="exam-page-bg" aria-hidden="true"></div>
  <!-- 考场密封线（装饰） -->
  <div class="seal-line" aria-hidden="true"><span>- - - - 密&nbsp;封&nbsp;线&nbsp;内&nbsp;不&nbsp;得&nbsp;答&nbsp;题 - - - -</span></div>
  <div class="exam-container">
    <!-- 考试头部区域 -->
    <div class="exam-header">
      <div class="header-left">
        <h2 class="paper-title">{{ examRecord.paper?.name || '在线考试' }}</h2>
        <div class="student-info" v-if="examRecord.studentName">
          <el-icon><User /></el-icon>
          <span>考生：{{ examRecord.studentName }}</span>
          <el-divider direction="vertical" />
          <el-button link type="primary" class="home-link" @click="$router.push('/')">
            <el-icon><HomeFilled /></el-icon>
            首页
          </el-button>
        </div>
      </div>
      <div class="header-right">
        <div class="timer-display" :class="{ 'time-danger': remainingTime <= 300 }">
          <el-icon><Timer /></el-icon>
          <span>{{ formattedTime }}</span>
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
/* ============================================================
   考试页 · 「蓝白考卷」设计（对标全局浅色 + Element 蓝）
   浅蓝桌面 + 纯白试卷纸 + 衬线标题 + 密封线
   ============================================================ */

/* 浅蓝桌面 */
.exam-page-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  background:
    radial-gradient(1100px 520px at 50% -8%, rgba(64, 158, 255, 0.10), transparent 62%),
    repeating-linear-gradient(0deg, transparent 0 34px, rgba(47, 107, 223, 0.035) 34px 35px),
    linear-gradient(175deg, #eaf1f9 0%, #e2ebf5 60%, #d8e4f0 100%);
}

/* 密封线：竖排虚线 */
.seal-line {
  position: fixed;
  left: 18px;
  top: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  z-index: 1;
  border-left: 2px dashed rgba(47, 107, 223, 0.35);
  padding-left: 10px;
  user-select: none;
}
.seal-line span {
  writing-mode: vertical-rl;
  letter-spacing: 6px;
  font-size: 12px;
  color: rgba(47, 86, 143, 0.55);
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
}

/* 试卷纸 */
.exam-container {
  position: relative;
  max-width: 960px;
  margin: 28px auto 48px;
  background:
    linear-gradient(0deg, rgba(47, 107, 223, 0.025) 0 1px, transparent 1px 26px),
    #ffffff;
  border-radius: 4px;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.8) inset,
    0 18px 40px -18px rgba(31, 59, 115, 0.28),
    0 4px 12px rgba(31, 59, 115, 0.14);
  overflow: hidden;
}

/* 考卷头：白底 + 蓝色双线 */
.exam-header {
  background: #f4f8fd;
  padding: 20px 28px 16px;
  border-bottom: 3px solid #2f6bdf;
  box-shadow: 0 5px 0 -3px #2f6bdf, 0 9px 0 -6px rgba(47, 107, 223, 0.3);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left .paper-title {
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #1f2d3d;
  margin: 0 0 8px 0;
}

.student-info {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #5a6b85;
  font-size: 13px;
  border: 1px solid rgba(47, 107, 223, 0.3);
  border-radius: 3px;
  padding: 3px 10px;
  background: rgba(47, 107, 223, 0.05);
}
.home-link {
  font-size: 13px;
  padding: 0;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  min-width: 220px;
}

/* 考钟胶囊 */
.timer-display {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #2f6bdf;
  color: #f2f7ff;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 1px;
  padding: 8px 18px;
  border-radius: 999px;
  box-shadow: 0 3px 8px rgba(47, 107, 223, 0.35);
}
/* 临期 5 分钟：红底脉动 */
.timer-display.time-danger {
  background: #d9534f;
  animation: clock-pulse 1s ease-in-out infinite;
}
@keyframes clock-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(217, 83, 79, 0.45); }
  50% { box-shadow: 0 0 0 8px rgba(217, 83, 79, 0); }
}

.timer-progress {
  width: 100%;
}

.answer-count-tip {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: #7a8699;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

/* 题目区域 */
.question-area {
  padding: 26px 28px 10px;
}

/* 大题组：衬线题号 + 蓝色双线 */
.question-group {
  margin-bottom: 26px;
}
.group-title {
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #1f2d3d;
  margin: 0 0 14px;
  padding: 0 2px 8px;
  border-bottom: 3px double rgba(47, 107, 223, 0.65);
}

/* 题目卡：题号方块章 */
.question-card {
  background: #fdfeff;
  border: 1px solid rgba(47, 107, 223, 0.16);
  border-radius: 4px;
  padding: 18px 20px;
  margin-bottom: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.question-card:hover {
  border-color: rgba(47, 107, 223, 0.45);
  box-shadow: 0 2px 10px rgba(47, 107, 223, 0.08);
}

.question-title {
  margin-bottom: 12px;
}
.question-number {
  display: inline-block;
  background: #2f6bdf;
  color: #f2f7ff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
  padding: 3px 10px;
  border-radius: 2px;
  margin-bottom: 8px;
  font-variant-numeric: tabular-nums;
}
.question-content {
  font-size: 15.5px;
  line-height: 1.75;
  color: #253244;
  margin: 0;
}

/* 选项：加大 + 左对齐 */
.choice-options,
.judge-options {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  width: 100%;
  gap: 6px;
}
.choice-options .option-item,
.judge-options .judge-item {
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  width: 100%;
  margin-right: 0;
  text-align: left;
  font-size: 15px;
  padding: 11px 14px;
  border-radius: 4px;
  transition: background-color 0.15s ease;
}
.choice-options .option-item:hover,
.judge-options .judge-item:hover {
  background: rgba(47, 107, 223, 0.06);
}
.choice-options :deep(.el-radio__label),
.judge-options :deep(.el-radio__label) {
  font-size: 15px;
  color: #2b3a4f;
  line-height: 1.6;
}
.option-label {
  font-weight: 700;
  color: #2f6bdf;
  margin-right: 6px;
}
.option-content {
  color: #2b3a4f;
  line-height: 1.65;
}

.text-input :deep(.el-textarea__inner) {
  background: #fdfeff;
  border: 1px solid rgba(47, 107, 223, 0.25);
  border-radius: 4px;
  font-size: 14.5px;
  line-height: 1.7;
  color: #253244;
}
.text-input :deep(.el-textarea__inner:focus) {
  border-color: #2f6bdf;
  box-shadow: 0 0 0 2px rgba(47, 107, 223, 0.15);
}

/* 交卷区 */
.submission-footer {
  padding: 18px 28px 30px;
  display: flex;
  justify-content: center;
  border-top: 1px dashed rgba(47, 107, 223, 0.3);
  margin: 8px 28px 0;
}
.submission-footer :deep(.el-button--primary) {
  background: #2f6bdf;
  border-color: #2f6bdf;
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 8px;
  text-indent: 8px;
  padding: 22px 56px;
  border-radius: 4px;
  box-shadow: 0 6px 14px -4px rgba(47, 107, 223, 0.5);
}
.submission-footer :deep(.el-button--primary:hover) {
  background: #4a80e8;
  border-color: #4a80e8;
}

/* 判卷中 / 时间到 覆盖层（保持功能，配色随主题） */
.grading-overlay,
.time-up-overlay {
  position: fixed;
  inset: 0;
  background: rgba(23, 42, 74, 0.88);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}
.grading-content,
.time-up-content {
  text-align: center;
  color: #eaf2ff;
  font-family: "Noto Serif SC", "Source Han Serif SC", "Songti SC", STSong, SimSun, serif;
}
.grading-icon,
.time-up-icon {
  font-size: 44px;
  margin-bottom: 14px;
}
.grading-progress,
.grading-content p,
.time-up-content p {
  color: rgba(234, 242, 255, 0.85);
}
.progress-text,
.countdown-progress {
  font-variant-numeric: tabular-nums;
}

/* 窄屏适配 */
@media (max-width: 768px) {
  .exam-container { margin: 12px 10px 32px; }
  .question-area { padding: 18px 16px 6px; }
  .exam-header { padding: 16px 16px 12px; }
  .header-right { align-items: flex-start; min-width: 0; }
  .seal-line { display: none; }
}
</style>
