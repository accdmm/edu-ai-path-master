import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')

    const readUserInfo = () => {
        try {
            const v = localStorage.getItem('userInfo')
            const parsed = v ? JSON.parse(v) : {}
            return (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) ? parsed : {}
        } catch {
            return {}
        }
    }
    const userInfo = ref(readUserInfo())

    const setToken = (newToken) => {
        token.value = newToken
        localStorage.setItem('token', newToken)
    }

    const setUserInfo = (info) => {
        userInfo.value = info
        localStorage.setItem('userInfo', JSON.stringify(info))
    }

    const logout = () => {
        token.value = ''
        userInfo.value = {}
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    return {
        token,
        userInfo,
        setToken,
        setUserInfo,
        logout
    }
})