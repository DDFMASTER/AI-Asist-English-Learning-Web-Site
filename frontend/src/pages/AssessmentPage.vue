<template>
  <main class="max-w-[1200px] mx-auto px-6 mt-10">
    <!-- ========== 状态 1: 开始屏幕 ========== -->
    <div v-if="store.currentScreen === 'start'" class="max-w-2xl mx-auto text-center py-20">
      <div class="w-20 h-20 bg-blue-50 text-[#2563EB] rounded-3xl flex items-center justify-center mx-auto mb-8">
        <Icon icon="ph:clipboard-text-bold" class="text-4xl" />
      </div>
      <h1 class="text-4xl font-bold mb-6">英语能力自适应测评</h1>
      <p class="text-gray-500 text-lg mb-10 leading-relaxed">
        本测评将通过 15 道精选题目，实时评估你的词汇量、语法掌握度及阅读理解能力。AI 会根据你的答题表现动态调整后续题目难度。
      </p>
      <div class="grid grid-cols-3 gap-6 mb-12 text-left">
        <div class="card !p-6">
          <div class="text-[#2563EB] mb-2 font-bold">10-15 min</div>
          <div class="text-xs text-gray-400">预计时长</div>
        </div>
        <div class="card !p-6">
          <div class="text-[#2563EB] mb-2 font-bold">15 题</div>
          <div class="text-xs text-gray-400">题目数量</div>
        </div>
        <div class="card !p-6">
          <div class="text-[#2563EB] mb-2 font-bold">动态难度</div>
          <div class="text-xs text-gray-400">AI 实时调整</div>
        </div>
      </div>
      <button
        class="px-12 py-4 bg-[#2563EB] text-white rounded-2xl text-lg font-bold shadow-xl shadow-blue-200 hover:scale-105 transition-transform"
        @click="guard(handleStart)"
      >
        开始测评
      </button>
    </div>

    <!-- ========== 状态 2: 答题屏幕 ========== -->
    <div v-else-if="store.currentQuestion" class="max-w-4xl mx-auto">
      <!-- 顶部进度 -->
      <div class="flex items-center justify-between mb-8">
        <div class="flex items-center gap-3">
          <span class="text-2xl font-bold text-[#2563EB]">
            {{ String(store.currentIndex + 1).padStart(2, '0') }}
          </span>
          <span class="text-gray-300 font-bold">/ {{ store.totalQuestions }}</span>
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

          <!-- 理解程度标记 -->
          <div class="card !bg-[#F8F7F4]">
            <h4 class="text-sm font-bold mb-4 text-center">你对这段内容的理解程度？</h4>
            <div class="flex gap-3">
              <button
                class="mark-btn flex-1 py-3 rounded-xl flex flex-col items-center gap-1 border transition-all"
                :class="store.currentComprehension === 'understood'
                  ? 'mark-active-green border-transparent'
                  : 'bg-white border-gray-200'"
                @click="store.markComprehension('understood')"
              >
                <Icon icon="ph:check-circle-bold" class="text-xl" :class="store.currentComprehension === 'understood' ? 'text-white' : 'text-green-500'" />
                <span class="text-[10px] font-bold" :class="store.currentComprehension === 'understood' ? 'text-white' : 'text-gray-500'">看得懂</span>
              </button>
              <button
                class="mark-btn flex-1 py-3 rounded-xl flex flex-col items-center gap-1 border transition-all"
                :class="store.currentComprehension === 'unclear'
                  ? 'mark-active-yellow border-transparent'
                  : 'bg-white border-gray-200'"
                @click="store.markComprehension('unclear')"
              >
                <Icon icon="ph:question-bold" class="text-xl" :class="store.currentComprehension === 'unclear' ? 'text-white' : 'text-yellow-500'" />
                <span class="text-[10px] font-bold" :class="store.currentComprehension === 'unclear' ? 'text-white' : 'text-gray-500'">模糊</span>
              </button>
              <button
                class="mark-btn flex-1 py-3 rounded-xl flex flex-col items-center gap-1 border transition-all"
                :class="store.currentComprehension === 'not-understood'
                  ? 'mark-active-red border-transparent'
                  : 'bg-white border-gray-200'"
                @click="store.markComprehension('not-understood')"
              >
                <Icon icon="ph:x-circle-bold" class="text-xl" :class="store.currentComprehension === 'not-understood' ? 'text-white' : 'text-red-500'" />
                <span class="text-[10px] font-bold" :class="store.currentComprehension === 'not-understood' ? 'text-white' : 'text-gray-500'">看不懂</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部导航 -->
      <div class="mt-12 flex justify-between items-center">
        <button
          class="px-8 py-3 font-bold transition-colors"
          :class="store.isFirstQuestion ? 'text-gray-300 cursor-not-allowed' : 'text-gray-400 hover:text-gray-600'"
          :disabled="store.isFirstQuestion"
          @click="store.prevQuestion()"
        >
          ← 上一题
        </button>
        <div class="flex items-center gap-4">
          <span class="text-sm text-gray-400">已自动保存进度</span>
          <button
            class="px-10 py-3 bg-[#2563EB] text-white rounded-xl font-bold shadow-lg shadow-blue-100 hover:scale-105 transition-transform"
            @click="handleNext"
          >
            {{ store.isLastQuestion ? '查看结果' : '下一题' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-else class="text-center py-20">
      <Icon icon="ph:spinner-bold" class="text-4xl text-[#2563EB] animate-spin mx-auto mb-4" />
      <p class="text-gray-400">加载测评题目中...</p>
    </div>
  </main>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useAssessmentStore } from '@/stores/assessment'
import { useRequireAuth } from '@/composables/useAuth'

const store = useAssessmentStore()
const router = useRouter()
const { guard } = useRequireAuth()

async function handleStart() {
  await store.startAssessment()
}

async function handleNext() {
  if (store.isLastQuestion) {
    const result = await store.submitAssessment()
    if (result && result.success) {
      router.push('/result')
    }
  } else {
    store.nextQuestion()
  }
}

function handleExit() {
  if (confirm('确定退出测评吗？当前进度已自动保存。')) {
    store.resetAssessment()
    router.push('/materials')
  }
}

onMounted(() => {
  // 每次进入页面重置状态
  store.resetAssessment()
})
</script>
