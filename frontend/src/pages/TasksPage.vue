<template>
  <main class="max-w-[1200px] mx-auto px-6 mt-10">
    <!-- 顶部标题区 -->
    <div class="flex items-center justify-between mb-10">
      <div>
        <h1 class="text-3xl font-bold mb-2">任务中心</h1>
        <p class="text-gray-500">管理你的学习计划，追踪每日完成情况</p>
      </div>
      <!-- 本周完成率 -->
      <div class="card !p-6 flex items-center gap-4">
        <div class="relative w-20 h-20 flex items-center justify-center">
          <svg class="w-full h-full transform -rotate-90">
            <circle cx="40" cy="40" fill="transparent" r="34" stroke="#F3F4F6" stroke-width="6" />
            <circle
              cx="40" cy="40" fill="transparent" r="34"
              stroke="#10B981" stroke-width="6"
              :stroke-dasharray="circleDasharray"
              :stroke-dashoffset="circleDashoffset"
              stroke-linecap="round"
            />
          </svg>
          <span class="absolute text-lg font-bold text-[#10B981]">{{ taskStore.weekProgress }}%</span>
        </div>
        <div>
          <div class="text-xs text-gray-400 font-medium">本周活跃率</div>
          <div class="text-lg font-bold">继续加油！</div>
        </div>
      </div>
    </div>

    <!-- 主体：两栏布局 -->
    <div class="grid grid-cols-12 gap-8">
      <!-- 左侧 60% -->
      <div class="col-span-7 flex flex-col gap-8">
        <!-- 每日经验进度条 -->
        <div class="card !p-6">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-sm font-bold text-gray-500">🔥 今日经验</h3>
            <span class="text-sm font-bold" :class="taskStore.isXpCapped ? 'text-[#EF4444]' : 'text-[#2563EB]'">
              {{ taskStore.todayXp }} / {{ taskStore.xpCap }} XP
            </span>
          </div>
          <div class="h-3 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full rounded-full transition-all duration-500"
              :class="taskStore.isXpCapped ? 'bg-[#EF4444]' : 'bg-[#2563EB]'"
              :style="{ width: taskStore.xpProgressPercent + '%' }"
            ></div>
          </div>
          <p class="text-[11px] text-gray-400 mt-2">
            剩余可获经验：
            <span class="font-bold" :class="taskStore.remainingXp > 0 ? 'text-[#10B981]' : 'text-[#EF4444]'">
              {{ taskStore.remainingXp }} XP
            </span>
            <span v-if="taskStore.isXpCapped" class="text-[#EF4444] font-bold"> — 今日已达上限！</span>
          </p>
        </div>

        <!-- 今日任务 -->
        <div class="card">
          <div class="flex items-center justify-between mb-6">
            <div>
              <h3 class="text-lg font-bold">🎯 今日任务</h3>
              <p class="text-[11px] text-gray-400 mt-0.5">每日 5 个随机任务，完成后获得经验</p>
            </div>
            <button
              class="px-4 py-2 bg-white border border-[#2563EB] text-[#2563EB] rounded-lg text-xs font-bold hover:bg-blue-50 transition-all"
              @click="guard(addTodayTask)"
            >
              + 自定义任务 (10 XP)
            </button>
          </div>

          <div v-if="taskStore.loading" class="text-center text-gray-400 py-8">
            <Icon icon="ph:spinner-bold" class="text-2xl animate-spin mx-auto mb-2" />
            <p class="text-xs">加载任务中...</p>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="task in taskStore.todayTasks"
              :key="task.id"
              class="flex items-center justify-between p-4 border border-gray-100 rounded-xl hover:border-[#2563EB] transition-all group"
              :class="{ 'bg-gray-50/50': task.done, 'opacity-60': taskStore.isXpCapped && !task.done }"
            >
              <div class="flex items-center gap-3">
                <input
                  type="checkbox"
                  :checked="task.done"
                  :disabled="taskStore.isXpCapped && !task.done"
                  class="w-5 h-5 rounded accent-[#10B981] cursor-pointer disabled:cursor-not-allowed disabled:opacity-40"
                  @change="guard(() => taskStore.toggleTask(task.id))"
                />
                <div>
                  <span
                    class="font-medium"
                    :class="{
                      'line-through text-gray-400': task.done,
                      'cursor-pointer': !task.done,
                    }"
                    @click="guard(() => !task.done && task.isManual && editTodayTaskName(task))"
                  >
                    {{ task.name }}
                  </span>
                  <div class="flex items-center gap-2 mt-0.5">
                    <span :class="difficultyBadgeClass(task)">{{ difficultyLabel(task) }}</span>
                    <span v-if="task.isManual" class="text-[10px] text-gray-300">自定义</span>
                  </div>
                </div>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-xs font-bold text-[#2563EB]">+{{ task.xp }} XP</span>
                <button
                  v-if="task.isManual"
                  class="p-1 text-gray-200 hover:text-red-500 transition-colors"
                  @click="guard(() => taskStore.deleteTodayTask(task.id))"
                  title="删除自定义任务"
                >
                  <Icon icon="ph:x-bold" class="text-lg" />
                </button>
                <span v-else class="w-7"></span>
              </div>
            </div>

            <div v-if="taskStore.todayTasks.length === 0 && !taskStore.loading" class="text-center text-gray-400 text-sm py-8">
              暂无任务，明天将自动刷新
            </div>
          </div>
        </div>

        <!-- 周期计划 -->
        <div class="card">
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-bold">📅 周期计划</h3>
            <button
              class="px-4 py-2 border border-gray-200 text-gray-500 rounded-lg text-xs font-bold hover:bg-gray-50 transition-all"
              @click="guard(editCyclePlan)"
            >
              修改周期计划
            </button>
          </div>
          <div class="flex items-center gap-8">
            <div class="relative w-24 h-24 flex items-center justify-center flex-none">
              <svg class="w-full h-full transform -rotate-90">
                <circle cx="48" cy="48" fill="transparent" r="40" stroke="#F3F4F6" stroke-width="8" />
                <circle
                  cx="48" cy="48" fill="transparent" r="40"
                  stroke="#2563EB" stroke-width="8"
                  :stroke-dasharray="251"
                  :stroke-dashoffset="138"
                  stroke-linecap="round"
                />
              </svg>
              <span class="absolute text-lg font-bold text-[#2563EB]">45%</span>
            </div>
            <div class="flex-1">
              <div class="text-sm font-bold text-gray-700 mb-1">{{ cycleDateRange }}</div>
              <div class="text-sm text-gray-500">本月目标：词汇量提升至 B2 水平</div>
              <div class="mt-3 flex items-center gap-2">
                <div class="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden">
                  <div class="h-full bg-[#2563EB] rounded-full" style="width: 45%"></div>
                </div>
                <span class="text-xs font-bold text-[#2563EB]">45%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧 40% -->
      <div class="col-span-5 flex flex-col gap-8">
        <!-- 今日概况 -->
        <div class="card">
          <h3 class="text-lg font-bold mb-6">今日概况</h3>
          <div class="flex items-center justify-center gap-10">
            <div class="text-center">
              <div class="text-4xl font-bold text-[#2563EB]">{{ taskStore.todayDoneCount }}</div>
              <div class="text-xs text-gray-400 mt-1">已完成</div>
            </div>
            <div class="w-px h-16 bg-gray-100"></div>
            <div class="text-center">
              <div class="text-4xl font-bold text-gray-300">{{ taskStore.todayTotalCount }}</div>
              <div class="text-xs text-gray-400 mt-1">总任务</div>
            </div>
            <div class="w-px h-16 bg-gray-100"></div>
            <div class="text-center">
              <div class="text-4xl font-bold" :class="taskStore.isXpCapped ? 'text-[#EF4444]' : 'text-[#F59E0B]'">
                {{ taskStore.todayXp }}
              </div>
              <div class="text-xs text-gray-400 mt-1">
                今日 XP
                <span class="block text-[#10B981]">剩余 {{ taskStore.remainingXp }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 本周完成趋势（来自 IndexedDB 真实数据） -->
        <div class="card">
          <h3 class="text-lg font-bold mb-6">本周 XP 趋势</h3>
          <div class="flex items-end justify-between h-40 px-2">
            <div
              v-for="(day, idx) in weekChartData"
              :key="day.label"
              class="flex flex-col items-center gap-2 flex-1"
            >
              <div
                class="w-8 bar"
                :class="idx === weekChartData.length - 1 ? 'bg-[#2563EB]' : 'bg-gray-200'"
                :style="{ height: Math.max(4, day.height) + 'px' }"
                :title="`${day.label}: ${day.xp} XP / ${day.count} 任务`"
              ></div>
              <span class="text-[10px] text-gray-400">{{ day.label }}</span>
            </div>
          </div>
          <div class="mt-4 flex items-center justify-center gap-4 text-[10px] text-gray-400">
            <span class="flex items-center gap-1">
              <span class="w-3 h-3 rounded bg-[#2563EB]"></span> 今天
            </span>
            <span class="flex items-center gap-1">
              <span class="w-3 h-3 rounded bg-gray-200"></span> 历史
            </span>
          </div>
        </div>

        <!-- 已完成任务 -->
        <div class="card">
          <h3 class="text-lg font-bold mb-6">✅ 已完成</h3>
          <div class="space-y-4 max-h-80 overflow-y-auto">
            <div
              v-for="task in taskStore.completedTasks"
              :key="task.id"
              class="flex items-center justify-between p-3 bg-gray-50 rounded-xl"
            >
              <div>
                <span class="text-sm font-medium text-gray-500 line-through">{{ task.name }}</span>
                <div class="flex items-center gap-2 mt-1">
                  <span class="text-[10px] text-gray-400">已完成 · 今天</span>
                  <span v-if="task.isManual" class="text-[10px] text-gray-300">自定义</span>
                </div>
              </div>
              <span class="text-xs font-bold text-[#10B981]">+{{ task.xp }} XP</span>
            </div>
            <div
              v-if="taskStore.completedTasks.length === 0"
              class="text-center text-gray-400 text-sm py-4"
            >
              今天还没有完成任务
            </div>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useTaskStore } from '@/stores/task'
import { useRequireAuth } from '@/composables/useAuth'

const taskStore = useTaskStore()
const { guard } = useRequireAuth()

// 当前周数
const currentWeek = computed(() => {
  const now = new Date()
  const start = new Date(now.getFullYear(), 0, 1)
  return Math.ceil(((now - start) / 86400000 + start.getDay() + 1) / 7)
})

// 周期计划日期范围
const cycleDateRange = computed(() => {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), 16)
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 15)
  return `${start.getMonth() + 1}月${start.getDate()}日 - ${end.getMonth() + 1}月${end.getDate()}日`
})

// 环形进度条参数
const circleDasharray = 2 * Math.PI * 34
const circleDashoffset = computed(() => {
  const progress = taskStore.weekProgress / 100
  return circleDasharray * (1 - progress)
})

// ========== 难度徽章 ==========
const difficultyLabelMap = {
  easy: '简单',
  medium: '中等',
  hard: '困难',
  manual: '自定义',
}

const difficultyBadgeMap = {
  easy: 'text-[10px] font-bold px-1.5 py-0.5 rounded bg-green-50 text-green-600',
  medium: 'text-[10px] font-bold px-1.5 py-0.5 rounded bg-yellow-50 text-yellow-600',
  hard: 'text-[10px] font-bold px-1.5 py-0.5 rounded bg-red-50 text-red-600',
  manual: 'text-[10px] font-bold px-1.5 py-0.5 rounded bg-blue-50 text-blue-500',
}

function difficultyLabel(task) {
  return difficultyLabelMap[task.difficulty] || '未知'
}

function difficultyBadgeClass(task) {
  return difficultyBadgeMap[task.difficulty] || ''
}

// ========== 趋势图（来自 IndexedDB 真实数据） ==========
const weekDayLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const weekChartData = computed(() => {
  // xpHistory 是最近 7 天的 [{date, xpEarned, taskCount}, ...]
  const history = taskStore.xpHistory
  const maxXp = Math.max(taskStore.xpCap, ...history.map(h => h.xpEarned || 0), 1)

  return history.map((day, idx) => {
    const xp = day.xpEarned || 0
    // 最高柱子 120px
    const height = Math.round((xp / maxXp) * 120)
    return {
      label: weekDayLabels[idx] || '?',
      height,
      xp,
      count: day.taskCount || 0,
      isToday: idx === history.length - 1,
    }
  })
})

// ========== 操作方法 ==========

function addTodayTask() {
  const name = prompt('请输入自定义任务名称（完成可得 10 XP）：')
  if (name && name.trim()) {
    taskStore.addTodayTask(name.trim())
  }
}

function editTodayTaskName(task) {
  const newName = prompt('编辑任务名称：', task.name)
  if (newName && newName.trim()) {
    taskStore.editTodayTaskName(task.id, newName.trim())
  }
}

function editCyclePlan() {
  alert('周期计划编辑功能即将上线')
}

onMounted(async () => {
  await taskStore.initDailyTasks()
  await taskStore.fetchWeekTasks()
})
</script>
