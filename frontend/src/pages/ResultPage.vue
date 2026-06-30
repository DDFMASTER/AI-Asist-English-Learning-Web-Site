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
          <h1 class="text-3xl font-bold mb-4">恭喜！你已成功晋级 {{ result.nextLevel }} 阶段</h1>
          <p class="text-gray-500 leading-relaxed mb-8">
            根据你的答题表现，AI 系统从词汇、语法、阅读理解、文化背景和逻辑分析五个维度评估了你的英语能力。继续加油，下一阶段目标是 {{ result.nextLevel }}！
          </p>
          <div class="flex items-center gap-4">
            <router-link
              :to="`/materials?level=${result.nextLevel}`"
              class="px-8 py-3 bg-[#2563EB] text-white rounded-xl font-bold shadow-lg shadow-blue-200 hover:scale-105 transition-transform inline-block"
            >
              开启 {{ result.nextLevel }} 学习之旅
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
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (radarChart) {
    radarChart.dispose()
    radarChart = null
  }
})
</script>
