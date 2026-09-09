<template>
  <div class="pay-result">
    <el-result v-if="paid" icon="success" title="支付成功，权益已到账！"
      :sub-title="`积分 +${bonus}，已自动生成并激活邀请码`">
      <template #extra>
        <div class="result-extra">
          <div class="code-box">
            邀请码：<el-tag type="success" size="large">{{ inviteCode || '—' }}</el-tag>
          </div>
          <div class="credits">当前可用积分：<b>{{ activeCredits }}</b></div>
          <div class="actions">
            <el-button type="primary" @click="$router.push('/interview-questions')">去企业真题</el-button>
            <el-button @click="$router.push('/')">返回首页</el-button>
          </div>
        </div>
      </template>
    </el-result>

    <el-result v-else-if="failed" icon="error" title="未检测到支付完成"
      sub-title="如果已完成付款，请稍后到「我的订单」确认到账（积分会自动发放）">
      <template #extra>
        <div class="actions">
          <el-button type="primary" @click="$router.push('/pay')">返回购买页</el-button>
          <el-button @click="$router.push('/')">返回首页</el-button>
        </div>
      </template>
    </el-result>

    <el-result v-else icon="info" title="正在确认支付结果...">
      <template #extra>
        <div class="actions">
          <span class="waiting">已等待 {{ waited }}s{{ pollCount >= MAX_POLL ? '，尚未收到支付状态' : '' }}</span>
        </div>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPayStatus } from '@/api/pay'

const route = useRoute()
const orderNo = route.query.out_trade_no || route.query.orderNo || ''

const MAX_POLL = 15
const paid = ref(false)
const failed = ref(false)
const bonus = ref(0)
const inviteCode = ref('')
const activeCredits = ref(0)
const pollCount = ref(0)
const waited = ref(0)

let timer = null

async function check() {
  if (!orderNo) {
    failed.value = true
    return
  }
  pollCount.value++
  try {
    const res = await getPayStatus(orderNo)
    const data = res.data || {}
    if (data.status === 'PAID') {
      clearInterval(timer)
      paid.value = true
      bonus.value = data.bonus || 0
      inviteCode.value = data.code || ''
      activeCredits.value = data.activeCredits || 0
      return
    }
  } catch (e) {
  }
  if (pollCount.value >= MAX_POLL) {
    clearInterval(timer)
    failed.value = true
  }
}

onMounted(() => {
  check()
  timer = setInterval(() => {
    waited.value += 2
    check()
  }, 2000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.pay-result {
  padding: 40px 20px;
  max-width: 640px;
  margin: 0 auto;
}

.result-extra {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.code-box {
  font-size: 15px;
  color: #606266;
}

.credits {
  font-size: 15px;
  color: #303133;
}

.credits b {
  color: #f56c6c;
  font-size: 18px;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.waiting {
  color: #909399;
}
</style>