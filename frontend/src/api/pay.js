import request from '../utils/request'

/**
 * 支付宝沙箱支付相关 API（购买邀请码）
 */

// 创建支付订单（返回支付宝电脑网站支付 form 页面）
export function createPayOrder(type) {
  return request({
    url: '/api/pay/create',
    method: 'post',
    data: { type }
  })
}

// 查询订单支付状态（后端会向支付宝确认并自动结算）
export function getPayStatus(orderNo) {
  return request({
    url: '/api/pay/order/status',
    method: 'get',
    params: { orderNo }
  })
}

// 当前用户的支付记录
export function getMyPayOrders() {
  return request({
    url: '/api/pay/orders',
    method: 'get'
  })
}