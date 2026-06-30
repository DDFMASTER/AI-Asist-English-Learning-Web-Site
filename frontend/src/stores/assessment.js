import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

const PROGRESS_KEY = 'assessment_progress'

export const useAssessmentStore = defineStore('assessment', () => {
  // ========== 状态 ==========
  const currentScreen = ref('start') // 'start' | 'quiz'
  const questions = ref([])
  const currentIndex = ref(0)
  const answers = ref({})       // { questionId: selectedOptionId }
  const loading = ref(false)
  const assessmentResult = ref(null) // 评估结果，供 ResultPage 使用
  const sessionId = ref('')          // 后台生成的会话 ID，用于轮询
  const totalTarget = ref(10)        // 目标总题数
  let pollTimer = null               // 轮询定时器

  // ========== 计算属性 ==========
  const currentQuestion = computed(() =>
    questions.value[currentIndex.value] || null
  )

  // 是否所有题目都已生成完毕
  const allQuestionsReady = computed(() =>
    questions.value.length >= totalTarget.value
  )

  const totalQuestions = computed(() => questions.value.length)

  const progressPercent = computed(() => {
    if (totalTarget.value === 0) return 0
    return Math.round(((currentIndex.value + 1) / totalTarget.value) * 100)
  })

  const isFirstQuestion = computed(() => currentIndex.value === 0)
  const isLastQuestion = computed(() =>
    currentIndex.value >= totalQuestions.value - 1
  )

  const selectedOption = computed(() => {
    if (!currentQuestion.value) return null
    return answers.value[currentQuestion.value.id] || null
  })

  // ========== 动作 ==========

  /**
   * 开始测评：获取首题（快速），剩余题目后台生成并轮询拉取。
   */
  async function startAssessment() {
    loading.value = true
    assessmentResult.value = null
    stopPolling()
    try {
      const { useUserStore } = await import('@/stores/user')
      const userStore = useUserStore()
      const studyPurpose = userStore.user?.studyPurpose || '四级'

      const data = await request.post('/assessment/generate',
        { studyPurpose },
        { timeout: 30000 }  // 首题生成约 15-20s
      )

      if (!data.success || !data.questions || data.questions.length === 0) {
        throw new Error(data.message || '生成测评题目失败')
      }

      sessionId.value = data.sessionId || ''
      totalTarget.value = data.totalTarget || 10
      questions.value = transformQuestions(data.questions)

      // 如果首题拿到但总数不足，启动轮询
      if (sessionId.value && questions.value.length < totalTarget.value) {
        startPolling()
      }
    } catch (err) {
      console.error('生成测评失败:', err)
      throw err
    } finally {
      loading.value = false
    }
    currentScreen.value = 'quiz'
    currentIndex.value = 0
    answers.value = {}
    saveProgress()
  }

  /** 将后端题目格式转为前端格式 */
  function transformQuestions(rawQuestions) {
    return rawQuestions.map((q, i) => ({
      id: i + 1,
      passage: q.passage || '',
      question: q.question || '',
      options: [
        { id: 'A', text: q.optionA || '' },
        { id: 'B', text: q.optionB || '' },
        { id: 'C', text: q.optionC || '' },
        { id: 'D', text: q.optionD || '' },
      ],
      correctAnswer: q.answer,
      explanation: q.explanation || '',
    }))
  }

  /** 轮询获取后台生成的剩余题目 */
  function startPolling() {
    stopPolling()
    pollTimer = setInterval(async () => {
      try {
        const data = await request.get('/assessment/questions', {
          params: { sessionId: sessionId.value },
        })
        if (!data.success) return

        const newQuestions = data.questions || []
        if (newQuestions.length > questions.value.length) {
          // 重新编号（保持连续性）
          questions.value = transformQuestions(newQuestions)
          saveProgress()
        }

        if (data.complete || questions.value.length >= totalTarget.value) {
          stopPolling()
        }
      } catch {
        // 轮询失败静默重试
      }
    }, 3000)  // 每 3 秒轮询一次
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  /**
   * 选择答案
   */
  function selectOption(optionId) {
    if (!currentQuestion.value) return
    answers.value = {
      ...answers.value,
      [currentQuestion.value.id]: optionId,
    }
    saveProgress()
  }

  /**
   * 下一题
   */
  function nextQuestion() {
    if (currentIndex.value < totalQuestions.value - 1) {
      currentIndex.value++
      saveProgress()
    }
  }

  /**
   * 提交测评结果（AI 评估）
   */
  async function submitAssessment() {
    loading.value = true
    try {
      // 构造提交载荷：题目 + 用户答案
      const payload = {
        questions: questions.value.map(q => ({
          passage: q.passage,
          question: q.question,
          optionA: q.options[0]?.text || '',
          optionB: q.options[1]?.text || '',
          optionC: q.options[2]?.text || '',
          optionD: q.options[3]?.text || '',
          correctAnswer: q.correctAnswer,
          explanation: q.explanation || '',
        })),
        answers: questions.value.reduce((acc, q) => {
          const selected = answers.value[q.id]
          if (selected) acc[q.id] = selected
          return acc
        }, {}),
      }

      const data = await request.post('/assessment/evaluate',
        payload,
        { timeout: 45000 }
      )

      if (!data.success) {
        throw new Error(data.message || '评估失败')
      }

      // 存储评估结果供 ResultPage 使用
      assessmentResult.value = {
        level: data.cefrLevel || 'A1',
        nextLevel: getNextLevel(data.cefrLevel),
        score: data.overallScore || 0,
        dimensions: {
          vocabulary: data.vocabulary || 0,
          grammar: data.grammar || 0,
          reading: data.reading || 0,
          culture: data.culture || 0,
          logic: data.logic || 0,
        },
      }

      clearProgress()
      return { success: true, result: assessmentResult.value }
    } catch (error) {
      console.error('提交测评失败:', error)
      return { success: false, error: error.message }
    } finally {
      loading.value = false
    }
  }

  /**
   * 退出测评（重置）
   */
  function resetAssessment() {
    currentScreen.value = 'start'
    questions.value = []
    currentIndex.value = 0
    answers.value = {}
    sessionId.value = ''
    stopPolling()
  }

  /**
   * 获取评估结果
   */
  function getResult() {
    return assessmentResult.value
  }

  // ========== 辅助函数 ==========

  /** 根据当前 CEFR 等级推算下一阶段 */
  function getNextLevel(level) {
    const levels = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']
    const idx = levels.indexOf(level)
    if (idx >= 0 && idx < levels.length - 1) return levels[idx + 1]
    return level
  }

  // ========== 进度持久化 ==========

  function saveProgress() {
    try {
      const data = {
        questions: questions.value,
        currentIndex: currentIndex.value,
        answers: answers.value,
        sessionId: sessionId.value,
        totalTarget: totalTarget.value,
      }
      localStorage.setItem(PROGRESS_KEY, JSON.stringify(data))
    } catch {
      // localStorage 不可用时静默失败
    }
  }

  function loadProgress() {
    try {
      const raw = localStorage.getItem(PROGRESS_KEY)
      if (!raw) return null
      const data = JSON.parse(raw)
      if (!data.questions || !Array.isArray(data.questions) || data.questions.length === 0) {
        return null
      }
      return data
    } catch {
      return null
    }
  }

  function clearProgress() {
    try {
      localStorage.removeItem(PROGRESS_KEY)
    } catch {
      // 静默失败
    }
    // 注意：不在此处清除 assessmentResult，ResultPage 需要在导航后读取
  }

  function restoreProgress(data) {
    questions.value = data.questions || []
    currentIndex.value = data.currentIndex || 0
    answers.value = data.answers || {}
    sessionId.value = data.sessionId || ''
    totalTarget.value = data.totalTarget || 10
    currentScreen.value = 'quiz'
    // 恢复后如果题目数不足且 sessionId 有效，继续轮询
    if (sessionId.value && questions.value.length < totalTarget.value) {
      startPolling()
    }
  }

  return {
    currentScreen,
    questions,
    currentIndex,
    answers,
    loading,
    assessmentResult,
    sessionId,
    totalTarget,
    allQuestionsReady,
    currentQuestion,
    totalQuestions,
    progressPercent,
    isFirstQuestion,
    isLastQuestion,
    selectedOption,
    startAssessment,
    selectOption,
    nextQuestion,
    submitAssessment,
    resetAssessment,
    getResult,
    saveProgress,
    loadProgress,
    clearProgress,
    restoreProgress,
  }
})
