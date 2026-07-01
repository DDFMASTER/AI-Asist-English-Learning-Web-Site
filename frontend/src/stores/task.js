import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

// ========== 固定每日任务 ==========
const FIXED_DAILY_TASKS = [
  { name: '完成一篇基础类阅读', difficulty: 'easy', xp: 20, requiredCategory: 'basic', requiredCount: 1 },
  { name: '完成两篇应试类阅读', difficulty: 'medium', xp: 35, requiredCategory: 'exam', requiredCount: 2 },
  { name: '完成一篇进阶类阅读', difficulty: 'medium', xp: 35, requiredCategory: 'advanced', requiredCount: 1 },
  { name: '完成一次测试', difficulty: 'hard', xp: 50 },
  { name: '复习生词本中的单词', difficulty: 'easy', xp: 20 },
]

// ========== 难度 → 分类映射 ==========
function difficultyToCategory(difficulty) {
  const d = (difficulty || '').trim()
  if (['TOEFL', '托福', '期刊', '原著', '网络新闻'].includes(d)) return 'advanced'
  if (['CET-4', '四级', 'CET-6', '六级', '考研'].includes(d)) return 'exam'
  if (['初中', '高中', '故事', '日常'].includes(d)) return 'basic'
  return null
}

// ========== localStorage 读写（当日已读文章追踪，按用户隔离） ==========
import { userKey } from '@/utils/storage'

function getStorageKey() { return userKey('engliai_read_articles_today') }
function getXpSyncedKey() { return userKey('engliai_xp_synced_today') }
function getPendingXpKey() { return userKey('engliai_pending_xp') }  // 待同步经验值队列（登录后自动补发）

function todayKey() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

function loadReadArticles() {
  try {
    const raw = localStorage.getItem(getStorageKey())
    const data = raw ? JSON.parse(raw) : {}
    if (data.date !== todayKey()) {
      return { date: todayKey(), articles: {} }
    }
    return data
  } catch {
    return { date: todayKey(), articles: {} }
  }
}

function saveReadArticles(data) {
  localStorage.setItem(getStorageKey(), JSON.stringify(data))
}

/** 待同步经验值队列：保存到 localStorage，登录后自动补发 */
function savePendingXp(deltaXp) {
  try {
    const raw = localStorage.getItem(getPendingXpKey())
    const queue = raw ? JSON.parse(raw) : []
    queue.push({ deltaXp, time: Date.now() })
    localStorage.setItem(getPendingXpKey(), JSON.stringify(queue))
  } catch (_) { /* ignore */ }
}

function loadPendingXp() {
  try {
    const raw = localStorage.getItem(getPendingXpKey())
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function clearPendingXp() {
  localStorage.removeItem(getPendingXpKey())
}

/** 获取今日已同步过经验的每日任务ID列表 */
function loadXpSyncedTasks() {
  try {
    const raw = localStorage.getItem(getXpSyncedKey())
    const data = raw ? JSON.parse(raw) : {}
    if (data.date !== todayKey()) {
      return { date: todayKey(), synced: [] }
    }
    return data
  } catch {
    return { date: todayKey(), synced: [] }
  }
}

/** 记录某个每日任务的经验已同步，防止页面刷新后重复发放 */
function saveXpSyncedTask(taskId) {
  const data = loadXpSyncedTasks()
  if (!data.synced.includes(taskId)) {
    data.synced.push(taskId)
    localStorage.setItem(getXpSyncedKey(), JSON.stringify(data))
  }
}

/** 移除某个每日任务的经验同步记录（API 调用失败时回滚用） */
function removeXpSyncedTask(taskId) {
  const data = loadXpSyncedTasks()
  const idx = data.synced.indexOf(taskId)
  if (idx !== -1) {
    data.synced.splice(idx, 1)
    localStorage.setItem(getXpSyncedKey(), JSON.stringify(data))
  }
}

export const useTaskStore = defineStore('task', () => {
  // ========== 状态 ==========
  const todayTasks = ref([])
  const xpEarned = ref(0)
  const readArticlesData = ref(loadReadArticles())

  // ========== 计算属性 ==========
  const todayDoneCount = computed(() =>
    todayTasks.value.filter(t => t.done).length
  )

  const todayTotalCount = computed(() => todayTasks.value.length)

  // ========== 动作 ==========

  /** 初始化任务 */
  function initDailyTasks() {
    if (todayTasks.value.length > 0) return

    // 读取今日已同步过经验的每日任务ID
    const xpSyncedData = loadXpSyncedTasks()
    const syncedIds = xpSyncedData.synced || []

    todayTasks.value = FIXED_DAILY_TASKS.map((t, idx) => ({
      id: `daily-${idx}`,
      name: t.name,
      difficulty: t.difficulty,
      xp: t.xp,
      requiredCategory: t.requiredCategory || null,
      requiredCount: t.requiredCount || 0,
      done: false,
    }))

    // 恢复今日已有的完成状态（只恢复 done 状态，不重复发放经验）
    restoreTaskDoneState(syncedIds)

    // 自动补发队列中待同步的经验值（登录/session 恢复后）
    const userStore = useUserStore()
    if (userStore.isLoggedIn && userStore.user?.userId) {
      flushPendingXpInternal(userStore)
    }
  }

  /** 根据已读文章和已同步经验记录恢复任务打勾状态，但不重复发放经验 */
  function restoreTaskDoneState(syncedIds) {
    for (const task of todayTasks.value) {
      if (!task.requiredCategory || task.requiredCount <= 0) continue

      // 只有经验已成功同步到后端的任务才恢复为完成状态
      if (!syncedIds.includes(task.id)) continue

      const articles = readArticlesData.value.articles || {}
      const readCount = (articles[task.requiredCategory] || []).length

      if (readCount >= task.requiredCount) {
        task.done = true
      }
    }

    // 手动任务（测试、复习单词）：根据已同步记录恢复
    for (const syncedId of syncedIds) {
      const task = todayTasks.value.find(t => t.id === syncedId)
      if (task) {
        task.done = true
      }
    }
  }

  /** 切换任务完成状态（手动勾选/取消） */
  async function toggleTask(taskId) {
    const task = todayTasks.value.find(t => t.id === taskId)
    if (!task) return

    task.done = !task.done
    if (task.done) {
      // 乐观更新：先标记完成，再同步后端
      xpEarned.value += task.xp
      saveXpSyncedTask(taskId)
      const ok = await syncXp(task.xp)
      if (!ok) {
        // 同步失败，回滚
        xpEarned.value = Math.max(0, xpEarned.value - task.xp)
        task.done = false
        removeXpSyncedTask(taskId)
      }
    } else {
      xpEarned.value = Math.max(0, xpEarned.value - task.xp)
      removeXpSyncedTask(taskId)
      await syncXp(-task.xp)
    }
  }

  /**
   * 记录一次文章阅读，自动检测是否满足任务条件
   * @param {number|string} articleId
   * @param {string} difficulty — 文章难度标签，如 "TOEFL"、"CET-4"、"考研"
   */
  function recordArticleRead(articleId, difficulty) {
    const category = difficultyToCategory(difficulty)
    if (!category) return

    // 确保数据是今天的
    if (readArticlesData.value.date !== todayKey()) {
      readArticlesData.value = { date: todayKey(), articles: {} }
    }

    // 确保每类已读文章集合存在
    if (!readArticlesData.value.articles[category]) {
      readArticlesData.value.articles[category] = []
    }

    const readSet = readArticlesData.value.articles[category]

    // 已读过的不重复计数
    const id = String(articleId)
    if (readSet.includes(id)) return

    readSet.push(id)
    saveReadArticles(readArticlesData.value)

    // 重新检查所有自动任务
    recheckAllTasks()
  }

  /** 根据已读文章数量重新检查所有自动任务 */
  async function recheckAllTasks() {
    const xpSyncedData = loadXpSyncedTasks()
    const syncedIds = xpSyncedData.synced || []
    let xpChanged = 0
    const newlyCompletedIds = []

    for (const task of todayTasks.value) {
      if (!task.requiredCategory || task.requiredCount <= 0) continue
      if (task.done) continue // 已完成的跳过

      const articles = readArticlesData.value.articles || {}
      const readCount = (articles[task.requiredCategory] || []).length

      if (readCount >= task.requiredCount) {
        task.done = true

        if (!syncedIds.includes(task.id)) {
          xpEarned.value += task.xp
          xpChanged += task.xp
          newlyCompletedIds.push(task.id)
          // 乐观更新：先记录已同步，再调 API
          saveXpSyncedTask(task.id)
        }
      }
    }

    if (xpChanged > 0) {
      const ok = await syncXp(xpChanged)
      if (ok) {
        // 同步成功，刷新用户信息以更新显示的 XP
        try {
          const userStore = useUserStore()
          await userStore.fetchProfile()
        } catch (_) { /* 非关键，静默失败 */ }
      } else {
        // 同步失败，回滚所有本次完成的任务
        for (const task of todayTasks.value) {
          if (newlyCompletedIds.includes(task.id)) {
            task.done = false
            xpEarned.value = Math.max(0, xpEarned.value - task.xp)
            removeXpSyncedTask(task.id)
          }
        }
      }
    }
  }

  /**
   * 同步经验值到后端（含待同步队列自动补发）
   * @param {number} deltaXp — 经验值变动量（正数增加，负数扣减）
   * @returns {Promise<boolean|string>}
   *   - true: 同步成功
   *   - false: 同步失败（网络/服务器错误），应回滚
   *   - 'unauth': 未登录/session 过期，保留本地状态，自动入队等待补发
   */
  async function syncXp(deltaXp) {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn || !userStore.user?.userId) {
      console.warn('[Task] 无法同步经验值：用户未登录或无 userId')
      savePendingXp(deltaXp)
      return 'unauth'
    }

    try {
      // 先尝试补发队列中待同步的经验值
      await flushPendingXpInternal(userStore)

      const params = new URLSearchParams()
      params.append('userId', String(userStore.user.userId))
      params.append('xp', String(deltaXp))
      await request.post('/user/experience', params)
      return true
    } catch (error) {
      if (error.response?.status === 401) {
        console.warn('[Task] 同步经验值需要登录，已暂存待补发')
        savePendingXp(deltaXp)
        return 'unauth'
      }
      console.error('[Task] 同步经验值失败:', error)
      return false
    }
  }

  /** 补发队列中所有待同步的经验值（内部方法，不暴露） */
  async function flushPendingXpInternal(userStore) {
    const queue = loadPendingXp()
    if (queue.length === 0) return

    let totalXp = 0
    for (const item of queue) {
      totalXp += item.deltaXp
    }
    if (totalXp === 0) {
      clearPendingXp()
      return
    }

    try {
      const params = new URLSearchParams()
      params.append('userId', String(userStore.user.userId))
      params.append('xp', String(totalXp))
      await request.post('/user/experience', params)
      clearPendingXp()
      console.log(`[Task] 已补发待同步经验值: +${totalXp}（${queue.length} 笔）`)
      // 刷新用户信息
      try { await userStore.fetchProfile() } catch (_) { /* ignore */ }
    } catch (error) {
      if (error.response?.status === 401) {
        // 仍未登录，保留队列
        return
      }
      // 其他错误也保留队列，下次重试
      console.warn('[Task] 补发经验值失败，将重试:', error)
    }
  }

  /**
   * 测评完成后调用：标记"完成一次测试"任务为已完成并发放经验。
   * 每日首次完成有效，重复调用不会重复发放。
   */
  async function completeAssessmentTask() {
    const ASSESSMENT_TASK_ID = 'daily-3'

    // 确保任务列表已初始化
    if (todayTasks.value.length === 0) {
      initDailyTasks()
    }

    const task = todayTasks.value.find(t => t.id === ASSESSMENT_TASK_ID)
    if (!task) return

    // 已完成的跳过
    if (task.done) return

    // 检查今日是否已同步过经验（防止页面刷新后重复）
    const xpSyncedData = loadXpSyncedTasks()
    if ((xpSyncedData.synced || []).includes(ASSESSMENT_TASK_ID)) {
      task.done = true
      return
    }

    // 乐观更新：先标记完成并记录已同步，再调 API
    task.done = true
    xpEarned.value += task.xp
    saveXpSyncedTask(ASSESSMENT_TASK_ID)
    const ok = await syncXp(task.xp)
    if (!ok) {
      // 同步失败，回滚本地状态以便重试
      task.done = false
      xpEarned.value = Math.max(0, xpEarned.value - task.xp)
      removeXpSyncedTask(ASSESSMENT_TASK_ID)
      return
    }

    // 同步后刷新用户信息
    const { useUserStore } = await import('@/stores/user')
    useUserStore().fetchProfile()
  }

  /**
   * 复习生词本任务完成（停留生词本一定时间后调用）。
   * 每日首次完成有效，重复调用不会重复发放。
   */
  async function completeVocabReviewTask() {
    const VOCAB_TASK_ID = 'daily-4'

    if (todayTasks.value.length === 0) {
      initDailyTasks()
    }

    const task = todayTasks.value.find(t => t.id === VOCAB_TASK_ID)
    if (!task || task.done) return

    const xpSyncedData = loadXpSyncedTasks()
    if ((xpSyncedData.synced || []).includes(VOCAB_TASK_ID)) {
      task.done = true
      return
    }

    // 乐观更新：先标记完成并记录已同步，再调 API
    task.done = true
    xpEarned.value += task.xp
    saveXpSyncedTask(VOCAB_TASK_ID)
    const ok = await syncXp(task.xp)
    if (!ok) {
      // 同步失败，回滚本地状态以便重试
      task.done = false
      xpEarned.value = Math.max(0, xpEarned.value - task.xp)
      removeXpSyncedTask(VOCAB_TASK_ID)
      return
    }

    const { useUserStore } = await import('@/stores/user')
    useUserStore().fetchProfile()
  }

  return {
    todayTasks,
    xpEarned,
    todayDoneCount,
    todayTotalCount,
    initDailyTasks,
    toggleTask,
    recordArticleRead,
    completeAssessmentTask,
    completeVocabReviewTask,
  }
})
