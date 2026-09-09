import request from '@/utils/request'

/**
 * 发送消息给智能客服
 * @param {Object} data - 聊天数据，例如 { content: '问题内容' }
 * @returns {Promise}
 */
export function sendChatMessage(data) {
    return request({
        url: '/api/chat',
        method: 'post',
        data
    })
}

/**
 * 获取用户的聊天记录列表
 * @returns {Promise}
 */
export function getChatHistory() {
    return request({
        url: '/api/chat/history',
        method: 'get'
    })
}

/**
 * 删除指定的聊天记录
 * @param {string} id - 聊天记录 ID
 * @returns {Promise}
 */
export function deleteChatRecord(id) {
    return request({
        url: `/api/chat/history/${id}`,
        method: 'delete'
    })
}

/**
 * 清空所有聊天记录
 * @returns {Promise}
 */
export function clearAllChatRecords() {
    return request({
        url: '/api/chat/history/clear',
        method: 'delete'
    })
}