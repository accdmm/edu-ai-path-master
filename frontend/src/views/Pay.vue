<template>
  <div class="pay-page">
    <div class="pay-header">
      <el-button @click="$router.go(-1)">返回</el-button>
      <h2>购买邀请码</h2>
      <el-button type="primary" plain @click="loadOrders">我的订单</el-button>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom: 20px"
      title="购买即激活：完成支付后自动生成邀请码并发放对应积分，直接解锁企业真题与模拟面试" />

    <div class="product-grid">
      <div v-for="p in products" :key="p.type" class="product-card" :class="{ active: selected === p.type }"
        @click="selected = p.type">
        <el-tag :type="p.tag">{{ p.label }}邀请码</el-tag>
        <div class="price">
          <span class="yuan">¥</span>{{ p.price }}
        </div>
        <ul class="features">
          <li v-for="f in p.features" :key="f">{{ f }}</li>
        </ul>
        <el-button type="primary" size="large" style="width: 100%"
          :loading="buying && selected === p.type" @click.stop="handleBuy(p.type)">
          立即购买
        </el-button>
      </div>
    </div>

    <!-- 我的订单 -->
    <el-dialog v-model="ordersVisible" title="我的支付记录" width="720px">
      <el-table :data="orders" v-loading="ordersLoading" empty-text="暂无支付记录">
        <el-table-column prop="createTime" label="下单时间" width="170" />
        <el-table-column prop="productLabel" label="商品" width="110" />
        <el-table-column prop="amount" label="金额（元）" width="100">
          <template #default="scope">¥{{ scope.row.amount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'PAID' ? 'success' : 'info'">
              {{ scope.row.status === 'PAID' ? '已支付' : '未支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tradeNo" label="支付宝交易号" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPayOrder, getMyPayOrders } from '@/api/pay'

const router = useRouter()
const selected = ref('vip')
const buying = ref(false)

const products = reactive([
  {
    type: 'normal',
    label: '普通',
    price: '9.90',
    tag: 'info',
    features: ['浏览企业真题', '参与模拟面试', '查看面试结果', '奖励 100 积分']
  },
  {
    type: 'vip',
    label: 'VIP',
    price: '29.90',
    tag: 'warning',
    features: ['所有普通功能', '优先 AI 评分', '详细能力分析', '专属学习建议', '奖励 100 积分']
  },
  {
    type: 'enterprise',
    label: '企业',
    price: '99.90',
    tag: 'success',
    features: ['所有 VIP 功能', '企业真题优先', '面试官点评', '团队协作功能', '奖励 300 积分']
  }
])

async function handleBuy(type) {
  buying.value = true
  const win = window.open('', '_blank')
  try {
    const res = await createPayOrder(type)
    if (!win) {
      ElMessage.error('请允许浏览器弹出支付窗口后重试')
      return
    }
    win.document.write(res.data.form)
    win.document.close()
    win.focus()
    ElMessage.success('已打开支付宝沙箱支付窗口，请完成付款')
    setTimeout(() => {
      router.push(`/pay/result?out_trade_no=${res.data.orderNo}`)
    }, 3000)
  } catch (e) {
    if (win) win.close()
    ElMessage.error('创建订单失败')
  } finally {
    buying.value = false
  }
}

const ordersVisible = ref(false)
const ordersLoading = ref(false)
const orders = ref([])

async function loadOrders() {
  ordersVisible.value = true
  ordersLoading.value = true
  try {
    const res = await getMyPayOrders()
    orders.value = res.data || []
  } catch (e) {
  } finally {
    ordersLoading.value = false
  }
}
</script>

<style scoped>
.pay-page {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.pay-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.pay-header h2 {
  margin: 0;
  color: #303133;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.product-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 24px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-card.active {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.15);
}

.price {
  font-size: 34px;
  font-weight: 700;
  color: #f56c6c;
}

.price .yuan {
  font-size: 18px;
  margin-right: 4px;
}

.features {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.9;
  flex: 1;
}

@media (max-width: 720px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>