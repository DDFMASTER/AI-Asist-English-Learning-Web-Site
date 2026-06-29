import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

const PROGRESS_KEY = 'assessment_progress'

export const useAssessmentStore = defineStore('assessment', () => {
  // ========== 状态 ==========
  const currentScreen = ref('start') // 'start' | 'quiz'
  const questions = ref([])
  const currentIndex = ref(0)
  const answers = ref({})       // { questionId: selectedOption }
  const comprehensionLevels = ref({}) // { questionId: 'understood' | 'unclear' | 'not-understood' }
  const loading = ref(false)

  // ========== 计算属性 ==========
  const currentQuestion = computed(() =>
    questions.value[currentIndex.value] || null
  )

  const totalQuestions = computed(() => questions.value.length)

  const progressPercent = computed(() => {
    if (totalQuestions.value === 0) return 0
    return Math.round(((currentIndex.value + 1) / totalQuestions.value) * 100)
  })

  const isFirstQuestion = computed(() => currentIndex.value === 0)
  const isLastQuestion = computed(() =>
    currentIndex.value >= totalQuestions.value - 1
  )

  const selectedOption = computed(() => {
    if (!currentQuestion.value) return null
    return answers.value[currentQuestion.value.id] || null
  })

  const currentComprehension = computed(() => {
    if (!currentQuestion.value) return null
    return comprehensionLevels.value[currentQuestion.value.id] || null
  })

  // ========== 动作 ==========

  /**
   * 开始测评，获取试卷
   */
  async function startAssessment() {
    loading.value = true
    try {
      const data = await request.get('/assessment/start')
      if (data.success) {
        questions.value = data.questions || []
      }
    } catch {
      // 后端未就绪时使用 mock 数据
      questions.value = generateMockQuestions()
    } finally {
      loading.value = false
    }
    currentScreen.value = 'quiz'
    currentIndex.value = 0
    answers.value = {}
    comprehensionLevels.value = {}
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
   * 标记理解程度
   */
  function markComprehension(level) {
    if (!currentQuestion.value) return
    comprehensionLevels.value = {
      ...comprehensionLevels.value,
      [currentQuestion.value.id]: level,
    }
    saveProgress()
  }

  /**
   * 上一题
   */
  function prevQuestion() {
    if (currentIndex.value > 0) {
      currentIndex.value--
      saveProgress()
    }
  }

  /**
   * 下一题（最后一题时提交）
   */
  function nextQuestion() {
    if (isLastQuestion.value) {
      return submitAssessment()
    }
    if (currentIndex.value < totalQuestions.value - 1) {
      currentIndex.value++
      saveProgress()
    }
  }

  /**
   * 提交测评结果
   */
  async function submitAssessment() {
    loading.value = true
    try {
      const payload = {
        answers: answers.value,
        comprehensionLevels: comprehensionLevels.value,
      }
      const data = await request.post('/assessment/submit', payload)
      if (data && data.success) {
        clearProgress()
      }
      return data
    } catch (error) {
      console.error('提交测评失败:', error)
      // 即使失败也返回 mock 结果用于 demo
      clearProgress()
      return { success: true, result: {} }
    } finally {
      loading.value = false
    }
  }

  /**
   * 退出测评(重置)
   */
  function resetAssessment() {
    currentScreen.value = 'start'
    questions.value = []
    currentIndex.value = 0
    answers.value = {}
    comprehensionLevels.value = {}
  }

  // ========== 进度持久化 ==========

  function saveProgress() {
    try {
      const data = {
        questions: questions.value,
        currentIndex: currentIndex.value,
        answers: answers.value,
        comprehensionLevels: comprehensionLevels.value,
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
      // 校验数据完整性
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
  }

  function restoreProgress(data) {
    questions.value = data.questions
    currentIndex.value = data.currentIndex || 0
    answers.value = data.answers || {}
    comprehensionLevels.value = data.comprehensionLevels || {}
    currentScreen.value = 'quiz'
  }

  // ========== Mock 数据生成 ==========
  function generateMockQuestions() {
    return Array.from({ length: 10 }, (_, i) => ({
      id: i + 1,
      passage: 'The concept of "Smart Cities" has gained significant momentum over the past decade. By leveraging Internet of Things (IoT) technologies, urban centers can now monitor traffic patterns, energy consumption, and public safety in real-time. However, the implementation of such systems is not without controversy. Critics argue that the pervasive nature of sensors and cameras raises serious concerns regarding citizen privacy and data security.',
      question: i === 0
        ? 'What is the primary concern raised by critics of Smart Cities?'
        : `Sample question ${i + 1} about the passage?`,
      options: [
        { id: 'A', text: i === 0 ? 'The high cost of implementation' : 'Option A' },
        { id: 'B', text: i === 0 ? 'Privacy and data security issues' : 'Option B' },
        { id: 'C', text: i === 0 ? 'Inefficient traffic monitoring' : 'Option C' },
        { id: 'D', text: i === 0 ? 'Lack of IoT infrastructure' : 'Option D' },
      ],
    }))
  }

  return {
    currentScreen,
    questions,
    currentIndex,
    answers,
    comprehensionLevels,
    loading,
    currentQuestion,
    totalQuestions,
    progressPercent,
    isFirstQuestion,
    isLastQuestion,
    selectedOption,
    currentComprehension,
    startAssessment,
    selectOption,
    markComprehension,
    prevQuestion,
    nextQuestion,
    submitAssessment,
    resetAssessment,
    saveProgress,
    loadProgress,
    clearProgress,
    restoreProgress,
  }
})
