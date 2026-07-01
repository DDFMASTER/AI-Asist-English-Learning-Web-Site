<template>
  <main class="max-w-[1200px] mx-auto px-6 mt-10">
    <!-- ========== 状态 1: 开始屏幕 ========== -->
    <div v-if="store.currentScreen === 'start'" class="max-w-2xl mx-auto text-center py-20">
      <div class="w-20 h-20 bg-blue-50 text-[#2563EB] rounded-3xl flex items-center justify-center mx-auto mb-8">
        <Icon icon="ph:clipboard-text-bold" class="text-4xl" />
      </div>
      <h1 class="text-4xl font-bold mb-6">英语能力自适应测评</h1>
      <p class="text-gray-500 text-lg mb-10 leading-relaxed">
        本测评将通过 10 道精选题目，实时评估你的词汇量、语法掌握度及阅读理解能力。AI 会根据你的答题表现动态调整后续题目难度。
      </p>
      <div class="grid grid-cols-3 gap-6 mb-12 text-left">
        <div class="card !p-6">
          <div class="text-[#2563EB] mb-2 font-bold">10-15 min</div>
          <div class="text-xs text-gray-400">预计时长</div>
        </div>
        <div class="card !p-6">
          <div class="text-[#2563EB] mb-2 font-bold">10 题</div>
          <div class="text-xs text-gray-400">题目数量</div>
        </div>
        <div class="card !p-6">
          <div class="text-[#2563EB] mb-2 font-bold">动态难度</div>
          <div class="text-xs text-gray-400">AI 实时调整</div>
        </div>
      </div>

      <!-- 继续上次测评 -->
      <div v-if="savedProgress" class="mb-6">
        <div class="inline-flex items-center gap-2 px-5 py-3 bg-amber-50 border border-amber-200 rounded-xl text-sm text-amber-700 mb-4">
          <Icon icon="ph:clock-counter-clockwise-bold" class="text-lg" />
          <span>上次答到第 <strong>{{ savedProgress.currentIndex + 1 }}</strong> 题（共 {{ savedProgress.questions.length }} 题）</span>
        </div>
        <br />
        <button
          class="px-10 py-3 bg-white border-2 border-[#2563EB] text-[#2563EB] rounded-2xl font-bold hover:bg-blue-50 transition-colors mr-4"
          @click="handleResume"
        >
          继续上次测评
        </button>
        <button
          class="px-10 py-3 bg-[#2563EB] text-white rounded-2xl font-bold shadow-xl shadow-blue-200 hover:scale-105 transition-transform"
          @click="handleStart"
        >
          重新开始
        </button>
      </div>

      <!-- 无进度时的开始按钮 -->
      <button
        v-else
        class="px-12 py-4 bg-[#2563EB] text-white rounded-2xl text-lg font-bold shadow-xl shadow-blue-200 hover:scale-105 transition-transform disabled:opacity-50 disabled:cursor-wait"
        :disabled="store.loading"
        @click="handleStart"
      >
        <Icon v-if="store.loading" icon="ph:spinner-bold" class="inline animate-spin mr-2" />
        {{ store.loading ? '正在生成题目...' : '开始测评' }}
      </button>

      <!-- 错误提示 -->
      <div v-if="error" class="mt-6">
        <div class="inline-flex items-center gap-2 px-5 py-3 bg-red-50 border border-red-200 rounded-xl text-sm text-red-600">
          <Icon icon="ph:warning-circle-bold" class="text-lg" />
          <span>{{ error }}</span>
        </div>
        <button
          class="ml-4 px-6 py-2 bg-red-500 text-white rounded-xl text-sm font-bold hover:bg-red-600 transition-colors"
          @click="handleStart"
        >
          重试
        </button>
      </div>
    </div>

    <!-- ========== 状态 2: 答题屏幕 ========== -->
    <div v-else-if="store.currentQuestion" class="max-w-4xl mx-auto">
      <!-- 顶部进度 -->
      <div class="flex items-center justify-between mb-8">
        <div class="flex items-center gap-3">
          <span class="text-2xl font-bold text-[#2563EB]">
            {{ String(store.currentIndex + 1).padStart(2, '0') }}
          </span>
          <span class="text-gray-300 font-bold">/ {{ store.totalTarget }}</span>
        </div>
        <div v-if="!store.allQuestionsReady" class="flex items-center gap-1.5 ml-3">
          <Icon icon="ph:spinner-bold" class="text-[11px] text-amber-500 animate-spin" />
          <span class="text-[11px] text-amber-500">正在生成剩余题目...</span>
        </div>
        <div class="flex-1 max-w-md h-2 bg-gray-100 rounded-full mx-8 overflow-hidden">
          <div
            class="h-full bg-[#2563EB] transition-all duration-500"
            :style="{ width: store.progressPercent + '%' }"
          ></div>
        </div>
        <button
          class="text-sm font-bold text-gray-400 hover:text-red-500 transition-colors"
          @click="handleExit"
        >
          退出测评
        </button>
      </div>

      <!-- 题目内容 -->
      <div class="grid grid-cols-12 gap-8">
        <!-- 左侧：阅读文本 -->
        <div class="col-span-7">
          <div class="card h-full">
            <h3 class="text-xs font-bold text-gray-400 uppercase tracking-widest mb-4">
              Reading Passage
            </h3>
            <div class="text-lg leading-relaxed text-gray-700">
              <p>{{ store.currentQuestion.passage }}</p>
            </div>
          </div>
        </div>

        <!-- 右侧：问题与选项 -->
        <div class="col-span-5 flex flex-col gap-6">
          <!-- 问题和选项 -->
          <div class="card flex-1">
            <h3 class="font-bold text-xl mb-6">{{ store.currentQuestion.question }}</h3>
            <div class="space-y-4">
              <div
                v-for="option in store.currentQuestion.options"
                :key="option.id"
                class="option-card p-4 rounded-xl flex items-center gap-4"
                :class="{ 'option-selected': store.selectedOption === option.id }"
                @click="store.selectOption(option.id)"
              >
                <div
                  class="w-6 h-6 rounded-full border-2 flex items-center justify-center flex-none"
                  :class="store.selectedOption === option.id
                    ? 'border-[#2563EB] bg-[#2563EB]'
                    : 'border-gray-200'"
                >
                  <span
                    class="text-xs font-bold"
                    :class="store.selectedOption === option.id ? 'text-white' : ''"
                  >
                    {{ option.id }}
                  </span>
                </div>
                <span class="text-sm font-medium">{{ option.text }}</span>
              </div>
            </div>
          </div>

        </div>
      </div>

      <!-- 底部导航 -->
      <div class="mt-12 flex justify-between items-center">
        <button
          class="px-6 py-3 rounded-xl font-bold transition-all flex items-center gap-1.5"
          :class="!store.isFirstQuestion
            ? 'bg-white border border-gray-200 text-gray-600 hover:border-[#2563EB] hover:text-[#2563EB]'
            : 'bg-gray-50 text-gray-300 cursor-not-allowed'"
          :disabled="store.isFirstQuestion"
          @click="store.prevQuestion()"
        >
          <Icon icon="ph:arrow-left-bold" class="text-sm" />
          上一题
        </button>
        <div class="flex items-center gap-4">
          <span class="text-sm text-gray-400">已自动保存进度</span>
          <button
            class="px-10 py-3 rounded-xl font-bold shadow-lg transition-all"
            :class="store.selectedOption && !submitting
              ? 'bg-[#2563EB] text-white shadow-blue-100 hover:scale-105'
              : 'bg-gray-200 text-gray-400 cursor-not-allowed'"
            :disabled="!store.selectedOption || submitting"
            @click="handleNext"
          >
            <Icon v-if="submitting" icon="ph:spinner-bold" class="inline animate-spin mr-1.5" />
            {{ submitting ? 'AI 正在分析评估...' : (store.allQuestionsReady && store.isLastQuestion ? '查看结果' : '下一题') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-else class="text-center py-20">
      <Icon icon="ph:spinner-bold" class="text-4xl text-[#2563EB] animate-spin mx-auto mb-4" />
      <p class="text-gray-400">加载测评题目中...</p>
    </div>

    <!-- ========== 退出确认弹窗 ========== -->
    <Teleport to="body">
      <div
        v-if="showExitConfirm"
        class="fixed inset-0 z-[200] flex items-center justify-center"
        @click.self="showExitConfirm = false"
      >
        <!-- 遮罩 -->
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm"></div>
        <!-- 卡片 -->
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-sm p-8 z-10 text-center">
          <div class="w-14 h-14 bg-red-50 text-red-400 rounded-2xl flex items-center justify-center mx-auto mb-5">
            <Icon icon="ph:warning-bold" class="text-3xl" />
          </div>
          <h3 class="text-lg font-bold text-[#1F2937] mb-2">确定退出测评？</h3>
          <p class="text-sm text-gray-400 mb-6">
            当前进度已自动保存，下次进入时可以从第 <strong class="text-[#2563EB]">{{ store.currentIndex + 1 }}</strong> 题继续。
          </p>
          <div class="flex gap-3">
            <button
              class="flex-1 h-11 border border-gray-200 rounded-xl text-sm font-medium text-gray-500 hover:bg-gray-50 transition-all"
              @click="showExitConfirm = false"
            >
              继续答题
            </button>
            <button
              class="flex-1 h-11 bg-red-500 text-white rounded-xl text-sm font-bold hover:bg-red-600 transition-all"
              @click="handleConfirmExit"
            >
              退出测评
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useAssessmentStore } from '@/stores/assessment'
import { useUserStore } from '@/stores/user'
import { useTaskStore } from '@/stores/task'

const store = useAssessmentStore()
const userStore = useUserStore()
const taskStore = useTaskStore()
const router = useRouter()

const showExitConfirm = ref(false)
const savedProgress = ref(null)
const error = ref('')
const submitting = ref(false)

async function handleStart() {
  error.value = ''
  store.clearProgress()
  savedProgress.value = null
  try {
    await store.startAssessment()
  } catch (err) {
    error.value = err.message || '生成测评题目失败，请稍后重试'
  }
}

function handleResume() {
  const data = savedProgress.value
  if (data) {
    store.restoreProgress(data)
    savedProgress.value = null
  }
}

async function handleNext() {
  if (store.allQuestionsReady && store.isLastQuestion) {
    if (submitting.value) return
    submitting.value = true
    const result = await store.submitAssessment()
    if (result && result.success) {
      // 测评完成后同步最新用户数据（经验值等）
      userStore.fetchProfile()
      // 标记今日"完成一次测试"任务并发放经验
      await taskStore.completeAssessmentTask()
      router.push('/result')
    } else if (result && !result.success) {
      error.value = result.error || '评估失败，请稍后重试'
    }
    submitting.value = false
  } else {
    store.nextQuestion()
  }
}

function handleExit() {
  showExitConfirm.value = true
}

function handleConfirmExit() {
  showExitConfirm.value = false
  // 不清除进度——退出时保留，下次可继续
  store.resetAssessment()
  router.push('/materials')
}

onMounted(() => {
  // 检测是否有上次保存的进度
  const data = store.loadProgress()
  if (data) {
    // 保存完整数据用于恢复
    savedProgress.value = data
  }
  store.resetAssessment()
})
</script>
