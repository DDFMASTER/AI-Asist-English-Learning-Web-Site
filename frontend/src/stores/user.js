import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  // ========== 状态 ==========
  const user = ref(loadUserFromStorage())
  const token = ref(localStorage.getItem('token') || '')
  const showLoginModal = ref(false)
  const pendingAction = ref(null) // 登录成功后待执行的回调

  // ========== 计算属性 ==========
  const isLoggedIn = computed(() => !!token.value && !!user.value)

  // ========== 辅助方法 ==========
  function loadUserFromStorage() {
    try {
      const raw = localStorage.getItem('user')
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  function saveUserToStorage() {
    if (user.value) {
      localStorage.setItem('user', JSON.stringify(user.value))
    } else {
      localStorage.removeItem('user')
    }
  }

  // ========== 动作 ==========

  /**
   * 用户登录
   * @param {string} username
   * @param {string} password
   */
  async function login(username, password) {
    const params = new URLSearchParams()
    params.append('username', username)
    params.append('password', password)

    const data = await request.post('/user/login', params)
    if (data.success) {
      token.value = data.token || 'session'
      // 后端返回扁平字段（userId/username/role/...），构造完整 user 对象
      user.value = data.user || {
        userId: data.userId,
        username: data.username || username,
        role: data.role || 'normal',
        studyPurpose: data.studyPurpose || '四级',
        experience: data.experience || 0,
        literacy: data.literacy || 0,
      }
      localStorage.setItem('token', token.value)
      saveUserToStorage()
      showLoginModal.value = false
      return data
    }
    throw new Error(data.message || '登录失败')
  }

  /**
   * 用户注册
   * @param {string} username
   * @param {string} password
   * @param {string} studyPurpose
   */
  async function register(username, password, studyPurpose) {
    const params = new URLSearchParams()
    params.append('username', username)
    params.append('password', password)
    params.append('studyPurpose', studyPurpose)

    const data = await request.post('/user/register', params)
    if (data.success) {
      return data
    }
    throw new Error(data.message || '注册失败')
  }

  /**
   * 退出登录
   */
  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  /**
   * 获取用户信息
   */
  async function fetchProfile() {
    if (!token.value) return
    try {
      const data = await request.get('/user/profile', {
        params: { token: token.value }
      })
      if (data.success) {
        user.value = data.user
        saveUserToStorage()
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }

  /**
   * 打开登录弹窗
   */
  function openLoginModal() {
    showLoginModal.value = true
  }

  /**
   * 关闭登录弹窗
   */
  function closeLoginModal() {
    showLoginModal.value = false
    pendingAction.value = null // 关闭弹窗时清除待执行动作
  }

  /**
   * 设置登录成功后待执行的回调
   */
  function setPendingAction(fn) {
    pendingAction.value = fn
  }

  /**
   * 执行并清除待执行回调（登录成功后调用）
   */
  function runPendingAction() {
    const fn = pendingAction.value
    pendingAction.value = null
    if (typeof fn === 'function') {
      fn()
    }
  }

  /**
   * 检查登录状态，未登录则弹窗
   */
  function requireLogin() {
    if (!isLoggedIn.value) {
      showLoginModal.value = true
      return false
    }
    return true
  }

  return {
    user,
    token,
    showLoginModal,
    isLoggedIn,
    pendingAction,
    login,
    register,
    logout,
    fetchProfile,
    openLoginModal,
    closeLoginModal,
    setPendingAction,
    runPendingAction,
    requireLogin,
  }
})
