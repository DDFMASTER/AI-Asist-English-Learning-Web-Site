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
  if (['TOEFL', '托福', '期刊', '原著'].includes(d)) return 'advanced'
  if (['CET-4', '四级', 'CET-6', '六级', '考研'].includes(d)) return 'exam'
  if (['初中', '高中', '故事', '日常'].includes(d)) return 'basic'
  return null
}

// ========== localStorage 读写（当日已读文章追踪） ==========
const STORAGE_KEY = 'engliai_read_articles_today'

function todayKey() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

function loadReadArticles() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const data = raw ? JSON.parse(raw) : {}
    // 如果不是今天的记录，重置
    if (data.date !== todayKey()) {
      return { date: todayKey(), articles: {} }
    }
    return data
  } catch {
    return { date: todayKey(), articles: {} }
  }
}

function saveReadArticles(data) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
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
    todayTasks.value = FIXED_DAILY_TASKS.map((t, idx) => ({
      id: `daily-${idx}`,
      name: t.name,
      difficulty: t.difficulty,
      xp: t.xp,
      requiredCategory: t.requiredCategory || null,
      requiredCount: t.requiredCount || 0,
      done: false,
    }))

    // 恢复今日已有的完成状态
    recheckAllTasks()
  }

  /** 切换任务完成状态（手动勾选/取消） */
  function toggleTask(taskId) {
    const task = todayTasks.value.find(t => t.id === taskId)
    if (!task) return

    task.done = !task.done
    if (task.done) {
      xpEarned.value += task.xp
      syncXp(task.xp)
    } else {
      xpEarned.value = Math.max(0, xpEarned.value - task.xp)
      syncXp(-task.xp)
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
  function recheckAllTasks() {
    let xpChanged = 0

    for (const task of todayTasks.value) {
      if (!task.requiredCategory || task.requiredCount <= 0) continue
      if (task.done) continue // 已完成的跳过

      const articles = readArticlesData.value.articles || {}
      const readCount = (articles[task.requiredCategory] || []).length

      if (readCount >= task.requiredCount) {
        task.done = true
        xpEarned.value += task.xp
        xpChanged += task.xp
      }
    }

    if (xpChanged > 0) {
      syncXp(xpChanged)
    }
  }

  /** 同步经验值到后端 */
  async function syncXp(deltaXp) {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn || !userStore.user?.userId) return

    try {
      const params = new URLSearchParams()
      params.append('userId', String(userStore.user.userId))
      params.append('xp', String(deltaXp))
      await request.post('/user/experience', params)
    } catch (error) {
      console.error('[Task] 同步经验值失败:', error)
    }
  }

  return {
    todayTasks,
    xpEarned,
    todayDoneCount,
    todayTotalCount,
    initDailyTasks,
    toggleTask,
    recordArticleRead,
  }
})
