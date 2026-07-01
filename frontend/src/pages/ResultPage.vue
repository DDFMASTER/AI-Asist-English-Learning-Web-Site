<template>
  <main class="max-w-[1000px] mx-auto px-6 mt-12">
    <!-- 顶部结果卡片 -->
    <div ref="resultTopRef" class="card mb-8 overflow-hidden relative">
      <div class="absolute top-0 right-0 w-64 h-64 bg-blue-50/50 rounded-full -mr-20 -mt-20 z-0"></div>
      <div class="relative z-10 flex items-center gap-12">
        <!-- 等级环形图 -->
        <div class="flex-none text-center">
          <div class="relative w-40 h-40 flex items-center justify-center mx-auto mb-4">
            <svg class="w-full h-full transform -rotate-90">
              <circle cx="80" cy="80" fill="transparent" r="72" stroke="#F3F4F6" stroke-width="12" />
              <circle
                cx="80" cy="80" fill="transparent" r="72"
                stroke="#2563EB" stroke-width="12"
                :stroke-dasharray="452"
                :stroke-dashoffset="ringOffset"
                stroke-linecap="round"
              />
            </svg>
            <div class="absolute flex flex-col items-center">
              <span class="text-4xl font-black text-[#2563EB]">{{ result.level }}</span>
              <span class="text-xs font-bold text-gray-400">CEFR 等级</span>
            </div>
          </div>
          <div class="text-sm font-bold text-gray-500">综合得分: {{ result.score }}/100</div>
        </div>

        <!-- 结果描述 -->
        <div class="flex-1">
          <div class="inline-flex items-center gap-2 px-3 py-1 bg-green-50 text-green-600 rounded-full text-xs font-bold mb-4">
            <Icon icon="ph:party-bold" />
            测评已通过
          </div>
          <h1 v-if="result.leveledUp" class="text-3xl font-bold mb-4">🎉 恭喜！你已成功晋级 {{ result.level }} 阶段</h1>
          <h1 v-else class="text-3xl font-bold mb-4">测评完成！当前等级 {{ result.level }}</h1>
          <p class="text-gray-500 leading-relaxed mb-8">
            根据你的答题表现，AI 系统从词汇、语法、阅读理解、文化背景和逻辑分析五个维度评估了你的英语能力。{{ result.leveledUp ? '你已升级到新阶段！' : '继续加油，下一阶段目标是 ' + result.nextLevel + '！' }}
          </p>
          <div class="flex items-center gap-4">
            <router-link
              :to="`/materials`"
              class="px-8 py-3 bg-[#2563EB] text-white rounded-xl font-bold shadow-lg shadow-blue-200 hover:scale-105 transition-transform inline-block"
            >
              {{ result.leveledUp ? '开启 ' + result.level + ' 学习之旅' : '返回读物匹配' }}
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-12 gap-8">
      <!-- 左侧：雷达图 -->
      <div class="col-span-5 flex flex-col gap-8">
        <div class="card h-full">
          <h3 class="text-lg font-bold mb-6">能力雷达图</h3>
          <div ref="radarChartRef" class="w-full h-80"></div>
        </div>
      </div>

      <!-- 右侧：分项能力 -->
      <div class="col-span-7 flex flex-col gap-8">
        <div class="card">
          <h3 class="text-lg font-bold mb-8">分项能力详情</h3>
          <div class="space-y-8">
            <div v-for="item in abilityDetails" :key="item.label">
              <div class="flex justify-between items-center mb-2">
                <span class="text-sm font-bold">{{ item.label }}</span>
                <span class="text-sm font-bold" :class="item.colorClass">{{ item.percent }}%</span>
              </div>
              <div class="progress-bar">
                <div
                  class="progress-inner"
                  :class="item.barClass"
                  :style="{ width: item.percent + '%' }"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 答题回顾（分页） ========== -->
    <div ref="reviewSectionRef" class="mt-12">
      <div class="flex items-center gap-3 mb-8">
        <Icon icon="ph:check-square-offset-bold" class="text-2xl text-[#2563EB]" />
        <h2 class="text-2xl font-bold">答题回顾</h2>
        <span class="text-sm text-gray-400 ml-2">
          共 {{ questionReview.length }} 题，
          答对 <strong class="text-green-500">{{ correctCount }}</strong> 题，
          答错 <strong class="text-red-500">{{ wrongCount }}</strong> 题
        </span>
      </div>

      <!-- 进度条：可点击题号 -->
      <div class="flex items-center justify-center gap-2 mb-8">
        <button
          class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-all hover:scale-110 flex-none"
          :class="currentReviewIndex > 0
            ? 'bg-white border border-gray-200 text-gray-500 hover:border-[#2563EB] hover:text-[#2563EB] cursor-pointer'
            : 'bg-gray-100 text-gray-300 cursor-not-allowed'"
          :disabled="currentReviewIndex === 0"
          @click="goToQuestion(currentReviewIndex - 1)"
        >
          <Icon icon="ph:caret-left-bold" class="text-sm" />
        </button>

        <div class="flex items-center gap-1.5 overflow-x-auto py-1 px-2">
          <button
            v-for="(item, idx) in questionReview"
            :key="item.id"
            class="w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold transition-all flex-none"
            :class="getProgressDotClass(idx, item)"
            @click="goToQuestion(idx)"
          >
            {{ item.id }}
          </button>
        </div>

        <button
          class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-all hover:scale-110 flex-none"
          :class="currentReviewIndex < questionReview.length - 1
            ? 'bg-white border border-gray-200 text-gray-500 hover:border-[#2563EB] hover:text-[#2563EB] cursor-pointer'
            : 'bg-gray-100 text-gray-300 cursor-not-allowed'"
          :disabled="currentReviewIndex === questionReview.length - 1"
          @click="goToQuestion(currentReviewIndex + 1)"
        >
          <Icon icon="ph:caret-right-bold" class="text-sm" />
        </button>
      </div>

      <!-- 当前题目卡片（带动画过渡） -->
      <div class="card border-l-4 transition-all duration-300"
        :class="currentItem.isCorrect ? 'border-l-green-400' : 'border-l-red-400'"
        :key="currentReviewIndex"
      >
        <!-- 题号与结果 -->
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-3">
            <span class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold"
              :class="currentItem.isCorrect
                ? 'bg-green-50 text-green-600'
                : 'bg-red-50 text-red-600'"
            >
              {{ currentItem.id }}
            </span>
            <span class="text-xs font-bold text-gray-400 uppercase tracking-widest">
              Question {{ currentItem.id }}
            </span>
          </div>
          <div class="flex items-center gap-3">
            <!-- 加入错题本按钮 -->
            <button
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all duration-200"
              :class="isInWrongBookSet(currentItem.id)
                ? 'bg-amber-50 text-amber-500 border border-amber-200 cursor-default'
                : 'bg-white border border-gray-200 text-gray-400 hover:text-[#2563EB] hover:border-[#2563EB] hover:shadow-sm cursor-pointer'"
              :disabled="isInWrongBookSet(currentItem.id)"
              @click="addCurrentToWrongBook"
              :title="isInWrongBookSet(currentItem.id) ? '已在错题本中' : '加入错题本'"
            >
              <Icon
                :icon="isInWrongBookSet(currentItem.id) ? 'ph:check-bold' : 'ph:plus-bold'"
                class="text-sm"
              />
              {{ isInWrongBookSet(currentItem.id) ? '已加入' : '错题本' }}
            </button>
            <div class="flex items-center gap-2">
              <Icon
                :icon="currentItem.isCorrect ? 'ph:check-circle-fill' : 'ph:x-circle-fill'"
                class="text-xl"
                :class="currentItem.isCorrect ? 'text-green-500' : 'text-red-500'"
              />
              <span
                class="text-sm font-bold"
                :class="currentItem.isCorrect ? 'text-green-600' : 'text-red-600'"
              >
                {{ currentItem.isCorrect ? '回答正确' : '回答错误' }}
              </span>
            </div>
          </div>
        </div>

        <!-- 阅读文本 -->
        <div v-if="currentItem.passage" class="mb-4 p-4 bg-gray-50 rounded-xl">
          <p class="text-sm text-gray-500 leading-relaxed">{{ currentItem.passage }}</p>
        </div>

        <!-- 问题 -->
        <p class="font-bold text-lg mb-4">{{ currentItem.question }}</p>

        <!-- 选项 -->
        <div class="space-y-3 mb-4">
          <div
            v-for="opt in currentItem.options"
            :key="opt.id"
            class="flex items-center gap-3 p-3 rounded-xl text-sm"
            :class="getOptionClass(opt.id, currentItem)"
          >
            <div
              class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold flex-none"
              :class="getOptionDotClass(opt.id, currentItem)"
            >
              {{ opt.id }}
            </div>
            <span class="font-medium">{{ opt.text }}</span>
            <span v-if="opt.id === currentItem.correctAnswer" class="ml-auto text-xs font-bold text-green-600">
              ✓ 正确答案
            </span>
            <span v-else-if="opt.id === currentItem.userAnswer && !currentItem.isCorrect" class="ml-auto text-xs font-bold text-red-500">
              ✗ 你的答案
            </span>
            <span v-else-if="opt.id === currentItem.userAnswer && currentItem.isCorrect" class="ml-auto text-xs font-bold text-green-600">
              ✓ 你的答案
            </span>
          </div>
        </div>

        <!-- 解析 -->
        <div class="p-4 rounded-xl"
          :class="currentItem.isCorrect ? 'bg-green-50 border border-green-100' : 'bg-amber-50 border border-amber-100'"
        >
          <div class="flex items-start gap-2">
            <Icon
              :icon="currentItem.isCorrect ? 'ph:lightbulb-fill' : 'ph:info-fill'"
              class="text-lg flex-none mt-0.5"
              :class="currentItem.isCorrect ? 'text-green-500' : 'text-amber-500'"
            />
            <div>
              <p class="text-xs font-bold mb-1"
                :class="currentItem.isCorrect ? 'text-green-700' : 'text-amber-700'"
              >
                {{ currentItem.isCorrect ? '题目解析' : '错题解析' }}
              </p>
              <p class="text-sm leading-relaxed"
                :class="currentItem.isCorrect ? 'text-green-800' : 'text-amber-800'"
              >
                {{ currentItem.explanation }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部翻页按钮 -->
      <div class="flex items-center justify-between mt-6">
        <button
          class="flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold transition-all"
          :class="currentReviewIndex > 0
            ? 'bg-white border border-gray-200 text-gray-600 hover:border-[#2563EB] hover:text-[#2563EB]'
            : 'bg-gray-50 text-gray-300 cursor-not-allowed'"
          :disabled="currentReviewIndex === 0"
          @click="goToQuestion(currentReviewIndex - 1)"
        >
          <Icon icon="ph:arrow-left-bold" class="text-base" />
          上一题
        </button>

        <span class="text-sm text-gray-400 font-medium">
          {{ currentReviewIndex + 1 }} / {{ questionReview.length }}
        </span>

        <button
          class="flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold transition-all"
          :class="currentReviewIndex < questionReview.length - 1
            ? 'bg-[#2563EB] text-white hover:bg-blue-600'
            : 'bg-gray-50 text-gray-300 cursor-not-allowed'"
          :disabled="currentReviewIndex === questionReview.length - 1"
          @click="goToQuestion(currentReviewIndex + 1)"
        >
          下一题
          <Icon icon="ph:arrow-right-bold" class="text-base" />
        </button>
      </div>

      <!-- 键盘提示 -->
      <p class="text-center text-xs text-gray-300 mt-4">
        使用 ← → 方向键或点击题号快速切换题目
      </p>
    </div>

  </main>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import * as echarts from 'echarts'
import { useAssessmentStore } from '@/stores/assessment'

const router = useRouter()
const assessmentStore = useAssessmentStore()
const storeResult = assessmentStore.assessmentResult

// 无结果时重定向
if (!storeResult) {
  router.replace('/assessment')
}

// ========== 答题回顾数据 ==========
const resultTopRef = ref(null)
const reviewSectionRef = ref(null)
const currentReviewIndex = ref(0)

const questionReview = computed(() => assessmentStore.questionReview)

const currentItem = computed(() =>
  questionReview.value[currentReviewIndex.value] || questionReview.value[0] || {}
)

const correctCount = computed(() =>
  questionReview.value.filter(q => q.isCorrect).length
)

const wrongCount = computed(() =>
  questionReview.value.filter(q => !q.isCorrect).length
)

// ========== 错题本 ==========
const wrongBookIds = ref(new Set())

/** 加载已在错题本中的题号 */
async function loadWrongBookIds() {
  try {
    const { getAllWrongQuestions } = await import('@/utils/wrongBookDB')
    const all = await getAllWrongQuestions()
    wrongBookIds.value = new Set(all.map(q => q.questionId))
  } catch (e) {
    console.error('加载错题本失败:', e)
  }
}

/** 判断某题是否已在错题本中 */
function isInWrongBookSet(questionId) {
  return wrongBookIds.value.has(questionId)
}

/** 将当前题目加入错题本 */
async function addCurrentToWrongBook() {
  const item = currentItem.value
  if (!item || !item.id) return
  try {
    const { addToWrongBook } = await import('@/utils/wrongBookDB')
    const result = await addToWrongBook({
      questionId: item.id,
      passage: item.passage || '',
      question: item.question || '',
      options: item.options || [],
      userAnswer: item.userAnswer || '',
      correctAnswer: item.correctAnswer || '',
      explanation: item.explanation || '',
    })
    if (result.success) {
      wrongBookIds.value = new Set([...wrongBookIds.value, item.id])
    }
  } catch (e) {
    console.error('加入错题本失败:', e)
  }
}

/** 跳转到指定题号的题目 */
function goToQuestion(index) {
  if (index >= 0 && index < questionReview.value.length) {
    currentReviewIndex.value = index
  }
}

/** 进度条圆点样式 */
function getProgressDotClass(idx, item) {
  const isCurrent = idx === currentReviewIndex.value
  const base = 'transition-all hover:scale-110'

  if (isCurrent) {
    return `${base} bg-[#2563EB] text-white shadow-md shadow-blue-200 scale-110`
  }
  if (item.isCorrect) {
    return `${base} bg-green-50 text-green-600 border border-green-200 cursor-pointer`
  }
  return `${base} bg-red-50 text-red-600 border border-red-200 cursor-pointer`
}

/** 滚动到解析区域 */
function scrollToReview() {
  if (reviewSectionRef.value) {
    reviewSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

/** 键盘左右方向键切换题目 */
function handleReviewKeydown(e) {
  if (e.key === 'ArrowLeft') {
    e.preventDefault()
    goToQuestion(currentReviewIndex.value - 1)
  } else if (e.key === 'ArrowRight') {
    e.preventDefault()
    goToQuestion(currentReviewIndex.value + 1)
  }
}

/** 监听滚轮：上滚回到结果页顶部，下滚定位到解析 */
let wheelFired = false
function handleReviewWheel(e) {
  if (wheelFired) return
  const reviewRect = reviewSectionRef.value?.getBoundingClientRect()
  const topRect = resultTopRef.value?.getBoundingClientRect()
  if (!reviewRect) return

  if (e.deltaY > 0) {
    // 向下滚动 → 定位到解析区域
    if (reviewRect.top > window.innerHeight * 0.3) {
      wheelFired = true
      scrollToReview()
      setTimeout(() => { wheelFired = false }, 800)
    }
  } else if (e.deltaY < 0 && topRect) {
    // 向上滚动 → 回到结果页顶部
    if (topRect.top < -50) {
      wheelFired = true
      resultTopRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
      setTimeout(() => { wheelFired = false }, 800)
    }
  }
}

/**
 * 选项卡片样式
 */
function getOptionClass(optionId, item) {
  const isUserAnswer = optionId === item.userAnswer
  const isCorrectAnswer = optionId === item.correctAnswer

  if (isCorrectAnswer && isUserAnswer) {
    // 答对了 — 绿色高亮
    return 'bg-green-50 border border-green-300'
  }
  if (isCorrectAnswer && !isUserAnswer) {
    // 正确答案（用户选错了）— 绿色边框提示
    return 'bg-green-50 border border-green-300'
  }
  if (isUserAnswer && !isCorrectAnswer) {
    // 用户选错了 — 红色高亮
    return 'bg-red-50 border border-red-300'
  }
  // 普通选项
  return 'bg-gray-50 border border-transparent'
}

/**
 * 选项圆点样式
 */
function getOptionDotClass(optionId, item) {
  const isUserAnswer = optionId === item.userAnswer
  const isCorrectAnswer = optionId === item.correctAnswer

  if (isCorrectAnswer) {
    return 'bg-green-500 text-white'
  }
  if (isUserAnswer && !isCorrectAnswer) {
    return 'bg-red-500 text-white'
  }
  return 'bg-gray-200 text-gray-500'
}

// ========== 结果数据 ==========
const result = reactive({
  level: storeResult?.level || 'A1',
  nextLevel: storeResult?.nextLevel || 'A2',
  score: storeResult?.score || 0,
})

// 分项能力详情（从 store 动态读取）
const abilityDetails = computed(() => {
  const dims = storeResult?.dimensions || {}
  return [
    { label: '词汇量 (Vocabulary)', percent: dims.vocabulary || 0, colorClass: 'text-[#2563EB]', barClass: 'bg-[#2563EB]' },
    { label: '语法掌握度 (Grammar)', percent: dims.grammar || 0, colorClass: 'text-[#2563EB]', barClass: 'bg-[#2563EB]' },
    { label: '阅读理解 (Reading)', percent: dims.reading || 0, colorClass: 'text-[#2563EB]', barClass: 'bg-[#2563EB]' },
    { label: '文化背景 (Culture)', percent: dims.culture || 0, colorClass: 'text-[#F59E0B]', barClass: 'bg-[#F59E0B]' },
    { label: '逻辑分析 (Logic)', percent: dims.logic || 0, colorClass: 'text-[#10B981]', barClass: 'bg-[#10B981]' },
  ]
})

// 环形进度条的 stroke-dashoffset 动态计算
const ringOffset = computed(() => {
  const circumference = 452 // 2 * PI * 72 ≈ 452
  return circumference - (circumference * result.score / 100)
})

// ========== ECharts 雷达图 ==========
const radarChartRef = ref(null)
let radarChart = null

function initRadarChart() {
  if (!radarChartRef.value) return
  radarChart = echarts.init(radarChartRef.value)
  const dims = storeResult?.dimensions || {}
  const option = {
    radar: {
      indicator: [
        { name: '词汇', max: 100 },
        { name: '语法', max: 100 },
        { name: '阅读理解', max: 100 },
        { name: '文化背景', max: 100 },
        { name: '逻辑分析', max: 100 },
      ],
      splitArea: { show: false },
      axisLine: { lineStyle: { color: '#E5E7EB' } },
      splitLine: { lineStyle: { color: '#E5E7EB' } },
    },
    series: [{
      type: 'radar',
      data: [{
        value: [
          dims.vocabulary || 0,
          dims.grammar || 0,
          dims.reading || 0,
          dims.culture || 0,
          dims.logic || 0,
        ],
        name: '能力评估',
        itemStyle: { color: '#2563EB' },
        areaStyle: { color: 'rgba(37, 99, 235, 0.2)' },
        lineStyle: { width: 3 },
      }],
    }],
  }
  radarChart.setOption(option)
  window.addEventListener('resize', handleResize)
}

function handleResize() {
  if (radarChart) radarChart.resize()
}

onMounted(async () => {
  await nextTick()
  initRadarChart()
  loadWrongBookIds()
  window.addEventListener('keydown', handleReviewKeydown)
  window.addEventListener('wheel', handleReviewWheel, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('keydown', handleReviewKeydown)
  window.removeEventListener('wheel', handleReviewWheel)
  if (radarChart) {
    radarChart.dispose()
    radarChart = null
  }
})
</script>
