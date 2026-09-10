import request from '@/utils/request'

export function login(data) {
    return request({
        url: '/api/user/login',
        method: 'post',
        data
    })
}

export function register(data) {
    return request({
        url: '/api/user/register',
        method: 'post',
        data
    })
}

export function getUserInfo() {
    return request({
        url: '/api/user/info',
        method: 'get'
    })
}

export function updatePwd(data) {
    return request({
        url: '/api/user/updatePwd',
        method: 'put',
        data
    })
}

export function checkAdmin(userId) {
    return request({
        url: `/api/user/check-admin/${userId}`,
        method: 'get'
    })
}

export function getActiveCredit(userId) {
    return request({
        url: `/api/user-interview-credits/active/${userId}`,
        method: 'get'
    })
}