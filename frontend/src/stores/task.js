import { defineStore } from 'pinia'
import { ref, computed, toRaw } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import {
  todayDate,
  getDailyTasks,
  saveDailyTasks,
  getXpHistory,
  saveXpHistory,
} from '@/utils/taskDB'

// ========== 常量 ==========
const DAILY_XP_CAP = 200
const MANUAL_TASK_XP = 10
const DAILY_TASK_COUNT = 5

// ========== 预定义任务池 ==========
const TASK_POOL = [
  // 简单 (easy, +20 XP)
  { name: '阅读一篇英语短文并标记生词', difficulty: 'easy', xp: 20 },
  { name: '跟读英语句子 10 遍', difficulty: 'easy', xp: 20 },
  { name: '默写 10 个基础英语单词', difficulty: 'easy', xp: 20 },
  { name: '背诵一段 3 句英文对话', difficulty: 'easy', xp: 20 },
  { name: '用英语写下今天的心情日记', difficulty: 'easy', xp: 20 },
  { name: '听一首英文歌并学歌词', difficulty: 'easy', xp: 20 },
  { name: '用英语描述 5 个身边物品', difficulty: 'easy', xp: 20 },

  // 中等 (medium, +35 XP)
  { name: '完成一篇精读练习并总结大意', difficulty: 'medium', xp: 35 },
  { name: '学习 5 个新单词并各造一个句子', difficulty: 'medium', xp: 35 },
  { name: '阅读一篇 TED 演讲摘要并复述', difficulty: 'medium', xp: 35 },
  { name: '翻译一段 200 词英文段落', difficulty: 'medium', xp: 35 },
  { name: '完成一套英语语法练习题(10 题)', difficulty: 'medium', xp: 35 },
  { name: '观看 5 分钟英语视频并做笔记', difficulty: 'medium', xp: 35 },
  { name: '用英语写一封简短邮件', difficulty: 'medium', xp: 35 },

  // 困难 (hard, +50 XP)
  { name: '完成一次阅读理解测验(全对)', difficulty: 'hard', xp: 50 },
  { name: '撰写一篇 300 词英语短文', difficulty: 'hard', xp: 50 },
  { name: '完成一套四六级听力真题', difficulty: 'hard', xp: 50 },
  { name: '背诵 10 个高频词汇并掌握用法', difficulty: 'hard', xp: 50 },
  { name: '进行 15 分钟英语口语自我对话', difficulty: 'hard', xp: 50 },
  { name: '阅读并分析一篇学术论文摘要', difficulty: 'hard', xp: 50 },
]

// ========== 工具函数 ==========
function shuffleArray(arr) {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

function generateDailyTasks() {
  // 每个难度至少 1 题，其余从整体池随机
  const easy = TASK_POOL.filter(t => t.difficulty === 'easy')
  const medium = TASK_POOL.filter(t => t.difficulty === 'medium')
  const hard = TASK_POOL.filter(t => t.difficulty === 'hard')

  const picked = []
  // 保证每个难度至少 1 题
  picked.push(shuffleArray(easy)[0])
  picked.push(shuffleArray(medium)[0])
  picked.push(shuffleArray(hard)[0])

  // 剩余 2 题从全池随机（去重）
  const remaining = TASK_POOL.filter(t => !picked.find(p => p.name === t.name))
  const extras = shuffleArray(remaining).slice(0, DAILY_TASK_COUNT - picked.length)
  picked.push(...extras)

  // 打乱顺序
  return shuffleArray(picked).map((t, idx) => ({
    id: `daily-${Date.now()}-${idx}`,
    name: t.name,
    difficulty: t.difficulty,
    xp: t.xp,
    done: false,
    isManual: false,
    date: todayDate(),
  }))
}

export const useTaskStore = defineStore('task', () => {
  // ========== 状态 ==========
  const todayTasks = ref([])
  const xpEarned = ref(0)
  const xpHistory = ref([])    // 最近 7 天的 {{date, xpEarned, taskCount}}
  const loading = ref(false)
  const initialized = ref(false)

  // ========== 计算属性 ==========

  /** 已完成的任务列表 */
  const completedTasks = computed(() =>
    todayTasks.value.filter(t => t.done)
  )

  /** 今日已完成数量 */
  const todayDoneCount = computed(() => completedTasks.value.length)

  /** 今日任务总数 */
  const todayTotalCount = computed(() => todayTasks.value.length)

  /** 今日已获得经验 */
  const todayXp = computed(() => xpEarned.value)

  /** 每日经验上限 */
  const xpCap = computed(() => DAILY_XP_CAP)

  /** 还剩多少 XP 可获取 */
  const remainingXp = computed(() =>
    Math.max(0, DAILY_XP_CAP - xpEarned.value)
  )

  /** XP 进度百分比(相对上限) */
  const xpProgressPercent = computed(() =>
    Math.min(100, Math.round((xpEarned.value / DAILY_XP_CAP) * 100))
  )

  /** XP 是否已达上限 */
  const isXpCapped = computed(() => xpEarned.value >= DAILY_XP_CAP)

  /** 本周完成率（以每日 XP 达标为基础） */
  const weekProgress = computed(() => {
    if (xpHistory.value.length === 0) return 0
    const daysWithXp = xpHistory.value.filter(d => d.xpEarned > 0).length
    return Math.round((daysWithXp / xpHistory.value.length) * 100)
  })

  // ========== 动作 ==========

  /**
   * 初始化每日任务
   * 今天首次调用 → 生成随机任务
   * 同日再次调用 → 从 IndexedDB 加载
   */
  async function initDailyTasks() {
    if (initialized.value) return
    loading.value = true

    try {
      const today = todayDate()

      // 1. 尝试从 IndexedDB 读取今日任务
      const cached = await getDailyTasks(today)

      if (cached && cached.tasks && cached.tasks.length > 0) {
        // 同日已生成过 → 直接加载
        todayTasks.value = cached.tasks
        xpEarned.value = cached.xpEarned || 0
      } else {
        // 新的一天 → 随机生成任务
        const newTasks = generateDailyTasks()
        todayTasks.value = newTasks
        xpEarned.value = 0

        // 存入 IndexedDB
        await saveDailyTasks(today, {
          tasks: newTasks,
          xpEarned: 0,
        })
      }

      // 2. 加载 XP 历史(最近 7 天)
      const history = await getXpHistory(7)
      xpHistory.value = history

      initialized.value = true
    } catch (error) {
      console.error('初始化每日任务失败:', error)
      // 降级：直接在内存生成
      if (todayTasks.value.length === 0) {
        todayTasks.value = generateDailyTasks()
        xpEarned.value = 0
      }
    } finally {
      loading.value = false
    }
  }

  /**
   * 同步经验值到后端数据库
   * @param {number} deltaXp — 本次变动的 XP（正数为增加，负数为减少）
   */
  async function syncXpToBackend(deltaXp) {
    const userStore = useUserStore()

    if (!userStore.isLoggedIn || !userStore.user?.userId) {
      // 未登录时仅使用本地 IndexedDB 缓存，不写入后端
      console.log('[Task] 未登录，跳过后端经验同步')
      return
    }

    try {
      const params = new URLSearchParams()
      params.append('userId', String(userStore.user.userId))
      params.append('xp', String(deltaXp))

      const result = await request.post('/user/experience', params)
      console.log('[Task] 经验同步成功:', result)
    } catch (error) {
      console.error('[Task] 同步经验值到后端失败:', error)
    }
  }

  /**
   * 切换任务完成状态
   */
  async function toggleTask(taskId) {
    const task = todayTasks.value.find(t => t.id === taskId)
    if (!task) return

    let deltaXp = 0

    if (task.done) {
      // 取消完成 → 扣除 XP
      task.done = false
      deltaXp = -task.xp
      xpEarned.value = Math.max(0, xpEarned.value + deltaXp)
    } else {
      // 标记完成 → 检查 XP 上限
      const potentialXp = xpEarned.value + task.xp
      if (potentialXp > DAILY_XP_CAP) {
        // 已达/将达上限 — 只加到上限
        const addable = DAILY_XP_CAP - xpEarned.value
        if (addable <= 0) {
          alert(`今日经验已达上限 ${DAILY_XP_CAP} XP，无法再获取更多经验！`)
          return
        }
        // 部分增加
        task.done = true
        deltaXp = addable
        xpEarned.value = DAILY_XP_CAP
        alert(`注意：该任务原为 +${task.xp} XP，但今日上限剩余仅 ${addable} XP，经验已截断至上限。`)
      } else {
        task.done = true
        deltaXp = task.xp
        xpEarned.value = potentialXp
      }
    }

    // 持久化到 IndexedDB
    await persistTasks()
    await updateXpHistory()

    // 同步到后端数据库
    if (deltaXp !== 0) {
      await syncXpToBackend(deltaXp)
    }
  }

  /**
   * 添加自定义任务（手动添加，固定 10 XP）
   */
  async function addTodayTask(name) {
    const task = {
      id: `manual-${Date.now()}`,
      name,
      difficulty: 'manual',
      xp: MANUAL_TASK_XP,
      done: false,
      isManual: true,
      date: todayDate(),
    }
    todayTasks.value.push(task)
    await persistTasks()
  }

  /**
   * 删除今日任务（仅允许手动任务）
   */
  async function deleteTodayTask(taskId) {
    const task = todayTasks.value.find(t => t.id === taskId)
    if (!task) return
    if (!task.isManual) {
      alert('系统任务不可删除')
      return
    }
    // 如果已完成 → 先扣除 XP 并同步到后端
    if (task.done) {
      xpEarned.value = Math.max(0, xpEarned.value - task.xp)
      await syncXpToBackend(-task.xp)
    }
    todayTasks.value = todayTasks.value.filter(t => t.id !== taskId)
    await persistTasks()
    if (task.done) await updateXpHistory()
  }

  /**
   * 编辑今日任务名称（主要针对手动任务）
   */
  async function editTodayTaskName(taskId, newName) {
    const task = todayTasks.value.find(t => t.id === taskId)
    if (task && newName.trim()) {
      task.name = newName.trim()
      await persistTasks()
    }
  }

  /**
   * 将当前任务列表持久化到 IndexedDB
   * 注意：必须用 toRaw + JSON 转换剥离 Vue 响应式代理，
   *       否则 IndexedDB 的 structured clone 会丢失数据。
   */
  async function persistTasks() {
    try {
      // 用 toRaw 剥离 Pinia 响应式代理，再 JSON 序列化/反序列化得到纯对象
      const rawTasks = toRaw(todayTasks.value)
      const cleanTasks = JSON.parse(JSON.stringify(rawTasks))
      const cleanXp = JSON.parse(JSON.stringify(toRaw(xpEarned.value)))

      await saveDailyTasks(todayDate(), {
        tasks: cleanTasks,
        xpEarned: cleanXp,
      })
    } catch (error) {
      console.error('持久化任务失败:', error)
    }
  }

  /**
   * 更新 XP 历史记录
   */
  async function updateXpHistory() {
    try {
      const today = todayDate()
      const count = completedTasks.value.length
      await saveXpHistory(today, {
        xpEarned: xpEarned.value,
        taskCount: count,
      })
      // 刷新本地 xpHistory
      xpHistory.value = await getXpHistory(7)
    } catch (error) {
      console.error('更新 XP 历史失败:', error)
    }
  }

  // ========== 向后兼容的 fetch 函数 ==========

  /**
   * 初始化今日任务（MaterialPage 等侧边栏调用）
   */
  async function fetchTodayTasks() {
    await initDailyTasks()
  }

  /** @deprecated 本周任务现改为从每日统计派生 */
  async function fetchWeekTasks() {
    // 从 XP 历史计算本周汇总
    if (xpHistory.value.length === 0) {
      await getXpHistory(7).then(h => { xpHistory.value = h })
    }
  }

  /** @deprecated 不在此页面使用的占位 */
  function addWeekTask() {}
  function deleteWeekTask() {}
  function editWeekTask() {}

  return {
    // 状态
    todayTasks,
    xpEarned,
    xpHistory,
    loading,
    initialized,

    // 计算属性
    completedTasks,
    todayDoneCount,
    todayTotalCount,
    todayXp,
    xpCap,
    remainingXp,
    xpProgressPercent,
    isXpCapped,
    weekProgress,

    // 动作
    initDailyTasks,
    toggleTask,
    addTodayTask,
    deleteTodayTask,
    editTodayTaskName,
    fetchTodayTasks,
    fetchWeekTasks,
    addWeekTask,
    deleteWeekTask,
    editWeekTask,
  }
})
