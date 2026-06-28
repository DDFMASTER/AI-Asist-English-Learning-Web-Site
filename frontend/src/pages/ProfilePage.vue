<template>
  <main class="max-w-[1200px] mx-auto px-6 mt-10">
    <!-- 用户信息卡片 -->
    <div class="card flex items-center gap-8 mb-8">
      <div class="w-20 h-20 rounded-full bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white text-2xl font-bold flex-none">
        {{ avatarLetter }}
      </div>
      <div class="flex-1">
        <div class="flex items-center gap-3 mb-1">
          <h1 class="text-2xl font-bold">{{ username }}</h1>
          <span class="px-3 py-1 bg-blue-50 text-[#2563EB] rounded-full text-xs font-bold">
            {{ userLevel }}
          </span>
        </div>
        <div class="flex items-center gap-8 mt-4">
          <div class="text-center">
            <div class="text-xl font-bold">{{ streak }}</div>
            <div class="text-[10px] text-gray-400">连续学习 天</div>
          </div>
          <div class="w-px h-8 bg-gray-100"></div>
          <div class="text-center">
            <div class="text-xl font-bold">{{ totalRead }}</div>
            <div class="text-[10px] text-gray-400">累计阅读 篇</div>
          </div>
          <div class="w-px h-8 bg-gray-100"></div>
          <div class="text-center">
            <div class="text-xl font-bold text-[#F59E0B]">{{ totalXp }}</div>
            <div class="text-[10px] text-gray-400">总经验 XP</div>
          </div>
        </div>
      </div>
      <button
        class="px-6 py-2.5 border border-gray-200 rounded-xl text-sm font-bold text-gray-500 hover:bg-gray-50 transition-all flex-none"
        @click="guard(() => { showEditProfile = true })"
      >
        编辑资料
      </button>
    </div>

    <!-- 主体：两栏布局 -->
    <div class="grid grid-cols-12 gap-8">
      <!-- 左侧 55% -->
      <div class="col-span-7 flex flex-col gap-8">
        <!-- 学习趋势 -->
        <div class="card">
          <h3 class="text-lg font-bold mb-6">📊 学习趋势</h3>
          <div ref="trendChartRef" class="w-full h-72"></div>
        </div>

        <!-- 学习记录 -->
        <div class="card">
          <h3 class="text-lg font-bold mb-6">📖 学习记录</h3>
          <div class="space-y-4 max-h-96 overflow-y-auto">
            <div
              v-for="record in learningRecords"
              :key="record.id"
              class="flex items-center justify-between p-4 bg-gray-50 rounded-xl"
            >
              <div class="flex items-center gap-4">
                <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-none" :class="record.bgClass">
                  <Icon :icon="record.icon" class="text-xl" :class="record.iconColor" />
                </div>
                <div>
                  <div class="text-sm font-bold">{{ record.title }}</div>
                  <div class="text-[10px] text-gray-400 mt-0.5">{{ record.time }}</div>
                </div>
              </div>
              <div class="text-right">
                <div class="text-xs font-bold text-[#10B981]">+{{ record.xp }} XP</div>
                <div class="text-[10px] text-gray-400">{{ record.duration }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧 45% -->
      <div class="col-span-5 flex flex-col gap-8">
        <!-- 勋章墙 -->
        <div class="card">
          <h3 class="text-lg font-bold mb-6">🏆 勋章墙</h3>
          <div class="grid grid-cols-3 gap-4">
            <div
              v-for="badge in badges"
              :key="badge.name"
              class="flex flex-col items-center gap-2 p-4 rounded-xl"
              :class="badge.unlocked ? badge.bgClass : 'bg-gray-50 opacity-50'"
            >
              <div class="w-12 h-12 rounded-full flex items-center justify-center" :class="badge.unlocked ? badge.circleBg : 'bg-gray-100'">
                <span class="text-2xl">{{ badge.unlocked ? badge.emoji : '🔒' }}</span>
              </div>
              <span class="text-[11px] font-bold text-center" :class="badge.unlocked ? 'text-gray-700' : 'text-gray-400'">
                {{ badge.name }}
              </span>
              <span class="text-[9px] font-bold" :class="badge.unlocked ? badge.labelColor : 'text-gray-400'">
                {{ badge.unlocked ? '已解锁' : '未解锁' }}
              </span>
            </div>
          </div>
        </div>

        <!-- 设置 -->
        <div class="card">
          <h3 class="text-lg font-bold mb-6">⚙️ 设置</h3>
          <div class="space-y-1">
            <!-- 每日学习提醒 -->
            <div class="flex items-center justify-between p-4 hover:bg-gray-50 rounded-xl transition-colors">
              <span class="text-sm font-medium text-gray-700">每日学习提醒</span>
              <ToggleSwitch v-model="reminderEnabled" @change="onReminderChange" />
            </div>
            <!-- 学习目标时长 -->
            <div
              class="flex items-center justify-between p-4 hover:bg-gray-50 rounded-xl transition-colors cursor-pointer"
              @click="guard(editStudyTarget)"
            >
              <span class="text-sm font-medium text-gray-700">学习目标时长</span>
              <span class="text-sm text-gray-400">{{ studyTarget }} 分钟/天</span>
            </div>
            <!-- 难度偏好 -->
            <div
              class="flex items-center justify-between p-4 hover:bg-gray-50 rounded-xl transition-colors cursor-pointer"
              @click="guard(editDifficulty)"
            >
              <span class="text-sm font-medium text-gray-700">难度偏好</span>
              <span class="text-sm text-gray-400">{{ difficultyPreference }}</span>
            </div>
            <!-- 通知设置 -->
            <div class="flex items-center justify-between p-4 hover:bg-gray-50 rounded-xl transition-colors">
              <span class="text-sm font-medium text-gray-700">通知设置</span>
              <ToggleSwitch v-model="notificationEnabled" @change="onNotificationChange" />
            </div>
            <!-- 关于我们 -->
            <router-link
              to="/about"
              class="flex items-center justify-between p-4 hover:bg-gray-50 rounded-xl transition-colors no-underline"
            >
              <span class="text-sm font-medium text-gray-700">关于我们</span>
              <Icon icon="ph:caret-right-bold" class="text-gray-300 text-lg" />
            </router-link>
            <!-- 退出登录 -->
            <div
              class="flex items-center justify-between p-4 hover:bg-red-50 rounded-xl transition-colors cursor-pointer"
              @click="handleLogout"
            >
              <span class="text-sm font-medium text-red-500">退出登录</span>
              <Icon icon="ph:sign-out-bold" class="text-red-300 text-lg" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 退出登录确认浮窗 -->
    <Teleport to="body">
      <div
        v-if="showLogoutConfirm"
        class="fixed z-[200] glass-popover p-5 w-72"
        :style="{ left: logoutPopoverPos.x + 'px', top: logoutPopoverPos.y + 'px' }"
        @click.stop
      >
        <p class="text-sm font-medium text-gray-700 mb-4">确定要退出当前账号吗？</p>
        <div class="flex gap-2">
          <button
            class="flex-1 h-9 border border-gray-200 rounded-lg text-xs font-medium text-gray-500 hover:bg-gray-50 transition-all"
            @click="cancelLogout"
          >
            取消
          </button>
          <button
            class="flex-1 h-9 bg-red-500 text-white rounded-lg text-xs font-bold hover:bg-red-600 transition-all"
            @click="confirmLogout"
          >
            退出
          </button>
        </div>
      </div>
    </Teleport>
  </main>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import * as echarts from 'echarts'
import { useUserStore } from '@/stores/user'
import ToggleSwitch from '@/components/ToggleSwitch.vue'
import { useRequireAuth } from '@/composables/useAuth'

const router = useRouter()
const userStore = useUserStore()
const { guard } = useRequireAuth()

// ========== 用户信息 ==========
const showEditProfile = ref(false)
const username = computed(() => userStore.user?.username || 'Alex')
const userLevel = computed(() => userStore.user?.study_purpose || 'B1 · 中级')
const avatarLetter = computed(() => username.value.charAt(0).toUpperCase())
const streak = ref(12)
const totalRead = ref(23)
const totalXp = ref(1580)

// ========== 学习记录 ==========
const learningRecords = ref([
  {
    id: 1, title: '阅读：The Future of AI', time: '今天 09:30', xp: 50, duration: '15 min',
    icon: 'ph:book-open-bold', iconColor: 'text-[#2563EB]', bgClass: 'bg-blue-50',
  },
  {
    id: 2, title: '学习 5 个新词', time: '今天 09:15', xp: 30, duration: '10 min',
    icon: 'ph:translate-bold', iconColor: 'text-[#F59E0B]', bgClass: 'bg-yellow-50',
  },
  {
    id: 3, title: '复习 10 个旧词', time: '今天 08:50', xp: 30, duration: '10 min',
    icon: 'ph:repeat-bold', iconColor: 'text-[#10B981]', bgClass: 'bg-green-50',
  },
  {
    id: 4, title: '完成 B1 能力测评', time: '昨天 14:20', xp: 100, duration: '25 min',
    icon: 'ph:clipboard-text-bold', iconColor: 'text-purple-500', bgClass: 'bg-purple-50',
  },
  {
    id: 5, title: '口语练习 5 分钟', time: '昨天 13:40', xp: 20, duration: '5 min',
    icon: 'ph:headset-bold', iconColor: 'text-orange-500', bgClass: 'bg-orange-50',
  },
])

// ========== 勋章 ==========
const badges = ref([
  { name: '初出茅庐', emoji: '🥇', unlocked: true, bgClass: 'bg-blue-50', circleBg: 'bg-blue-100', labelColor: 'text-blue-500' },
  { name: '小有成就', emoji: '🥈', unlocked: true, bgClass: 'bg-yellow-50', circleBg: 'bg-yellow-100', labelColor: 'text-yellow-500' },
  { name: '词汇达人', emoji: '📚', unlocked: false },
  { name: '阅读之星', emoji: '⭐', unlocked: false },
  { name: '打卡达人', emoji: '📅', unlocked: false },
  { name: '全能学者', emoji: '🎓', unlocked: false },
])

// ========== 设置 ==========
const reminderEnabled = ref(true)
const notificationEnabled = ref(false)
const studyTarget = ref(30)
const difficultyPreference = ref('自适应')

function onReminderChange(val) {
  // TODO: 调用后端 API 保存设置
  console.log('每日学习提醒:', val)
}

function onNotificationChange(val) {
  // TODO: 调用后端 API 保存设置
  console.log('通知设置:', val)
}

function editStudyTarget() {
  const val = prompt('设置每日学习时长（分钟）：', studyTarget.value)
  if (val && !isNaN(val) && Number(val) > 0) {
    studyTarget.value = Number(val)
    // TODO: 调用后端 API 保存
  }
}

function editDifficulty() {
  const options = ['简单', '自适应', '挑战']
  const current = options.indexOf(difficultyPreference.value)
  const next = (current + 1) % options.length
  difficultyPreference.value = options[next]
  // TODO: 调用后端 API 保存
}

const showLogoutConfirm = ref(false)
const logoutPopoverPos = reactive({ x: 0, y: 0 })

function handleLogout(e) {
  e.stopPropagation()
  // 计算弹窗位置：水平居中，垂直在点击位置附近
  const popoverWidth = 288 // w-72 = 288px
  const x = Math.max(10, (window.innerWidth - popoverWidth) / 2)
  const y = Math.max(10, e.clientY - 80)
  logoutPopoverPos.x = x
  logoutPopoverPos.y = y
  showLogoutConfirm.value = true
}

function confirmLogout() {
  userStore.logout()
  router.push('/login')
}

function cancelLogout() {
  showLogoutConfirm.value = false
}

// ========== ECharts 趋势图 ==========
const trendChartRef = ref(null)
let trendChart = null

function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  const option = {
    grid: { top: 10, bottom: 30, left: 40, right: 10 },
    xAxis: {
      type: 'category',
      data: ['6/1', '6/3', '6/5', '6/7', '6/9', '6/11', '6/13', '6/15', '6/17', '6/19', '6/21', '6/23', '6/25'],
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#9CA3AF', fontSize: 10 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { type: 'dashed', color: '#F3F4F6' } },
      axisLabel: { color: '#9CA3AF', fontSize: 10 },
    },
    series: [{
      data: [20, 35, 0, 45, 50, 30, 60, 40, 55, 25, 70, 45, 35],
      type: 'bar',
      barWidth: '10px',
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: function (params) {
          return params.dataIndex === 12 ? '#2563EB' : '#DBEAFE'
        },
      },
    }],
  }
  trendChart.setOption(option)
  window.addEventListener('resize', handleResize)
}

function handleResize() {
  if (trendChart) trendChart.resize()
}

function handleGlobalClick() {
  showLogoutConfirm.value = false
}

onMounted(async () => {
  await nextTick()
  initTrendChart()
  document.addEventListener('click', handleGlobalClick)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('click', handleGlobalClick)
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>
