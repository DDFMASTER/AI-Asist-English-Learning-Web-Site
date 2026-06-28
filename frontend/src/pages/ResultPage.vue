<template>
  <main class="max-w-[1000px] mx-auto px-6 mt-12">
    <!-- 顶部结果卡片 -->
    <div class="card mb-8 overflow-hidden relative">
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
                :stroke-dashoffset="144"
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
          <h1 class="text-3xl font-bold mb-4">恭喜！你已成功晋级 {{ result.nextLevel }} 阶段</h1>
          <p class="text-gray-500 leading-relaxed mb-8">
            AI 分析显示，你在阅读理解和逻辑分析方面表现优异，但在复杂长难句的语法拆解及文化背景知识上仍有提升空间。我们已为你定制了下一阶段的学习计划。
          </p>
          <div class="flex items-center gap-4">
            <router-link
              :to="`/materials?level=${result.nextLevel}`"
              class="px-8 py-3 bg-[#2563EB] text-white rounded-xl font-bold shadow-lg shadow-blue-200 hover:scale-105 transition-transform inline-block"
            >
              开启 {{ result.nextLevel }} 学习之旅
            </router-link>
            <button
              class="px-8 py-3 border border-gray-200 text-gray-500 rounded-xl font-bold hover:bg-gray-50 transition-all"
              @click="guard(restartAssessment)"
            >
              重新测评
            </button>
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

    <!-- 底部推荐 -->
    <div class="grid grid-cols-2 gap-8 mt-8">
      <!-- AI 薄弱词汇推送 -->
      <div class="card">
        <h3 class="text-lg font-bold mb-6 flex items-center gap-2">
          <Icon icon="ph:lightning-bold" class="text-[#F59E0B]" />
          AI 薄弱词汇推送
        </h3>
        <div class="grid grid-cols-1 gap-4">
          <div
            v-for="word in weakWords"
            :key="word.word"
            class="flex items-center justify-between p-4 bg-gray-50 rounded-xl"
          >
            <div>
              <div class="font-bold text-[#2563EB]">{{ word.word }}</div>
              <div class="text-xs text-gray-400">{{ word.definition }}</div>
            </div>
            <button
              class="text-xl text-gray-300 hover:text-[#2563EB] cursor-pointer transition-colors"
              @click="guard(() => addToVocabulary(word))"
            >
              <Icon icon="ph:plus-circle-bold" />
            </button>
          </div>
        </div>
      </div>

      <!-- 针对性强化阅读 -->
      <div class="card">
        <h3 class="text-lg font-bold mb-6 flex items-center gap-2">
          <Icon icon="ph:book-bookmark-bold" class="text-[#2563EB]" />
          针对性强化阅读
        </h3>
        <div class="space-y-4">
          <div
            v-for="article in recommendedArticles"
            :key="article.id"
            class="flex gap-4 group cursor-pointer"
            @click="guard(() => goToReader(article.id))"
          >
            <div class="w-16 h-16 rounded-lg bg-gradient-to-br from-blue-100 to-purple-100 flex items-center justify-center flex-none">
              <Icon :icon="article.icon" class="text-2xl opacity-40" :class="article.iconColor" />
            </div>
            <div>
              <h4 class="text-sm font-bold group-hover:text-[#2563EB] transition-colors">
                {{ article.title }}
              </h4>
              <p class="text-[10px] text-gray-400 mt-1">适合提升你的"{{ article.targetDimension }}"维度</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import * as echarts from 'echarts'
import { useRequireAuth } from '@/composables/useAuth'

const router = useRouter()
const { guard } = useRequireAuth()

// ========== 结果数据 ==========
const result = reactive({
  level: 'B1',
  nextLevel: 'B2',
  score: 68,
})

// 分项能力详情
const abilityDetails = ref([
  { label: '词汇量 (Vocabulary)', percent: 68, colorClass: 'text-[#2563EB]', barClass: 'bg-[#2563EB]' },
  { label: '语法掌握度 (Grammar)', percent: 72, colorClass: 'text-[#2563EB]', barClass: 'bg-[#2563EB]' },
  { label: '阅读理解 (Reading)', percent: 65, colorClass: 'text-[#2563EB]', barClass: 'bg-[#2563EB]' },
  { label: '文化背景 (Culture)', percent: 55, colorClass: 'text-[#F59E0B]', barClass: 'bg-[#F59E0B]' },
  { label: '逻辑分析 (Logic)', percent: 70, colorClass: 'text-[#10B981]', barClass: 'bg-[#10B981]' },
])

// 薄弱词汇
const weakWords = ref([
  { word: 'Algorithm', definition: 'n. 算法，演算法' },
  { word: 'Paradigm', definition: 'n. 范式，典范' },
  { word: 'Cognitive', definition: 'adj. 认知的，感知的' },
])

// 推荐文章
const recommendedArticles = ref([
  {
    id: 1,
    title: 'The Philosophy of Education',
    targetDimension: '文化背景',
    icon: 'ph:books-bold',
    iconColor: 'text-[#2563EB]',
  },
  {
    id: 2,
    title: 'Advanced Grammar in Use',
    targetDimension: '语法掌握度',
    icon: 'ph:pen-bold',
    iconColor: 'text-yellow-500',
  },
])

// ========== ECharts 雷达图 ==========
const radarChartRef = ref(null)
let radarChart = null

function initRadarChart() {
  if (!radarChartRef.value) return
  radarChart = echarts.init(radarChartRef.value)
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
        value: [68, 72, 65, 55, 70],
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

// ========== 操作方法 ==========
function restartAssessment() {
  router.push('/assessment')
}

function goToReader(articleId) {
  router.push(`/reader?id=${articleId}`)
}

function addToVocabulary(word) {
  // TODO: 调用 API 加入生词本
  alert(`已添加「${word.word}」到生词本`)
}

onMounted(async () => {
  await nextTick()
  initRadarChart()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (radarChart) {
    radarChart.dispose()
    radarChart = null
  }
})
</script>
