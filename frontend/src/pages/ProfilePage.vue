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
    </div>

    <!-- 抽屉式便签栏 -->
    <div class="drawer-tabs flex items-center gap-2 mb-0 flex-wrap">
      <button
        v-for="tab in drawerTabs"
        :key="tab.key"
        class="drawer-tab-label"
        :class="{ active: activeDrawer === tab.key }"
        @click="toggleDrawer(tab.key)"
      >
        <span class="text-lg">{{ tab.emoji }}</span>
        <span class="text-sm font-bold">{{ tab.label }}</span>
        <Icon
          :icon="activeDrawer === tab.key ? 'ph:caret-up-bold' : 'ph:caret-down-bold'"
          class="text-xs transition-transform duration-300"
        />
      </button>
    </div>

    <!-- 抽屉内容区域 -->
    <div class="drawer-content-wrapper">
      <Transition name="drawer">
        <!-- 学习趋势 -->
        <div v-if="activeDrawer === 'trend'" key="trend" class="card drawer-card">
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-bold">📊 每日在线时长</h3>
            <div class="flex items-center gap-2 px-4 py-2 bg-indigo-50 rounded-xl">
              <span class="text-xs text-indigo-400">今日在线</span>
              <span class="text-lg font-bold text-indigo-600">{{ todayOnlineMinutes }}<span class="text-xs font-normal text-indigo-400"> 分钟</span></span>
            </div>
          </div>
          <div ref="trendChartRef" class="w-full h-72"></div>
        </div>

        <!-- 学习记录 -->
        <div v-else-if="activeDrawer === 'records'" key="records" class="card drawer-card">
          <h3 class="text-lg font-bold mb-6">📖 学习记录</h3>
          <div v-if="historyRecords.length === 0" class="text-center py-12 text-gray-400">
            <Icon icon="ph:book-open-bold" class="text-3xl mx-auto mb-3 opacity-30" />
            <p class="text-sm">暂无阅读记录</p>
            <router-link to="/materials" class="text-xs text-[#2563EB] hover:underline mt-1 inline-block">
              去发现一些文章吧 →
            </router-link>
          </div>
          <div v-else class="space-y-4 max-h-96 overflow-y-auto">
            <div
              v-for="record in historyRecords"
              :key="record.articleId"
              class="flex items-center justify-between p-4 bg-gray-50 rounded-xl cursor-pointer hover:bg-blue-50 hover:shadow-sm transition-all group"
              @click="goToReader(record.articleId)"
            >
              <div class="flex items-center gap-4">
                <div class="w-10 h-10 rounded-xl bg-blue-50 flex items-center justify-center flex-none group-hover:bg-blue-100 transition-colors">
                  <Icon icon="ph:book-open-bold" class="text-xl text-[#2563EB]" />
                </div>
                <div>
                  <div class="text-sm font-bold group-hover:text-[#2563EB] transition-colors">{{ record.title }}</div>
                  <div class="text-[10px] text-gray-400 mt-0.5">{{ record.timeAgo }}</div>
                </div>
              </div>
              <div class="text-right flex items-center gap-2">
                <span class="text-[10px] text-gray-300">阅读</span>
                <Icon icon="ph:arrow-right-bold" class="text-gray-300 group-hover:text-[#2563EB] group-hover:translate-x-0.5 transition-all text-sm" />
              </div>
            </div>
          </div>
        </div>

        <!-- 勋章墙 -->
        <div v-else-if="activeDrawer === 'badges'" key="badges" class="card drawer-card">
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
        <div v-else-if="activeDrawer === 'settings'" key="settings" class="card drawer-card">
          <h3 class="text-lg font-bold mb-6">⚙️ 设置</h3>
          <div class="space-y-1">
            <!-- 学习目标时长 -->
            <div
              class="flex items-center justify-between p-4 hover:bg-gray-50 rounded-xl transition-colors"
              :class="{ 'cursor-pointer': !editingTarget }"
              @click="editingTarget || startEditTarget()"
            >
              <span class="text-sm font-medium text-gray-700">学习目标时长</span>
              <span v-if="!editingTarget" class="text-sm text-gray-400">{{ studyTarget }} 分钟/天</span>
              <div v-else class="flex items-center gap-2" @click.stop>
                <input
                  ref="targetInputRef"
                  v-model.number="targetDraft"
                  type="number"
                  min="1"
                  max="480"
                  class="w-16 h-8 text-sm text-center border border-[#2563EB] rounded-lg focus:outline-none"
                  @keydown.enter="saveTarget"
                  @keydown.escape="editingTarget = false"
                  @blur="saveTarget"
                />
                <span class="text-xs text-gray-400">分钟/天</span>
              </div>
            </div>
            <!-- 难度偏好 -->
            <div
              class="flex items-center justify-between p-4 hover:bg-gray-50 rounded-xl transition-colors cursor-pointer"
              @click="guard(editDifficulty)"
            >
              <span class="text-sm font-medium text-gray-700">难度偏好</span>
              <span class="text-sm text-gray-400">{{ difficultyPreference }}</span>
            </div>
            <!-- 关于我们 -->
            <div
              class="flex items-center justify-between p-4 hover:bg-gray-50 rounded-xl transition-colors cursor-pointer"
              @click="openAbout"
            >
              <span class="text-sm font-medium text-gray-700">关于我们</span>
              <Icon icon="ph:caret-right-bold" class="text-gray-300 text-lg" />
            </div>
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
      </Transition>
    </div>

    <!-- 关于我们弹窗 -->
    <Teleport to="body">
      <div
        v-if="showAboutModal"
        class="fixed inset-0 z-[200] flex items-center justify-center bg-black/40 backdrop-blur-sm"
        @click.self="showAboutModal = false"
      >
        <div class="relative bg-white rounded-2xl shadow-2xl overflow-hidden" style="width:192px;height:108px">
          <img src="/gggg.png" alt="关于我们" class="w-full h-full object-contain" />
        </div>
      </div>
    </Teleport>

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
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import * as echarts from 'echarts'
import { useUserStore } from '@/stores/user'
import { useRequireAuth } from '@/composables/useAuth'
import { getRecentOnlineTime, getTodayMinutes, getDailyTarget, setDailyTarget, getStreak } from '@/utils/onlineTimeDB'
import { getRecentHistory, countHistory } from '@/utils/historyDB'
import aboutAudioSrc from '@/ggg/gggg.mp3'

const router = useRouter()
const userStore = useUserStore()
const { guard } = useRequireAuth()

// ========== 用户信息 ==========
const showEditProfile = ref(false)
const username = computed(() => userStore.user?.username || 'Alex')
const userLevel = computed(() => userStore.user?.study_purpose || 'B1 · 中级')
const avatarLetter = computed(() => username.value.charAt(0).toUpperCase())
const streak = ref(0)
const totalRead = ref(0)
const totalXp = computed(() => userStore.user?.experience || 0)

// ========== 学习记录（浏览历史）==========
const historyRecords = ref([])

async function loadHistoryRecords() {
  try {
    const { getRecentHistory, relativeTime } = await import('@/utils/historyDB')
    const records = await getRecentHistory(50)
    historyRecords.value = records.map((r) => ({
      articleId: r.articleId,
      title: r.title || 'Untitled',
      timeAgo: relativeTime(r.visitedAt),
    }))
  } catch (e) {
    console.error('加载浏览历史失败:', e)
    historyRecords.value = []
  }
}

function goToReader(articleId) {
  if (!articleId) return
  router.push(`/reader?id=${articleId}`)
}

// ========== 勋章 ==========
const badges = ref([
  { name: '初出茅庐', emoji: '🥇', unlocked: true, bgClass: 'bg-blue-50', circleBg: 'bg-blue-100', labelColor: 'text-blue-500' },
  { name: '小有成就', emoji: '🥈', unlocked: true, bgClass: 'bg-yellow-50', circleBg: 'bg-yellow-100', labelColor: 'text-yellow-500' },
  { name: '词汇达人', emoji: '📚', unlocked: false },
  { name: '阅读之星', emoji: '⭐', unlocked: false },
  { name: '打卡达人', emoji: '📅', unlocked: false },
  { name: '全能学者', emoji: '🎓', unlocked: false },
])

// ========== 抽屉便签 ==========
const drawerTabs = [
  { key: 'trend', label: '学习趋势', emoji: '📊' },
  { key: 'records', label: '学习记录', emoji: '📖' },
  { key: 'badges', label: '勋章墙', emoji: '🏆' },
  { key: 'settings', label: '设置', emoji: '⚙️' },
]
const activeDrawer = ref('trend')

function toggleDrawer(key) {
  // 点击已打开的便签 → 关闭；点击其他便签 → 切换
  activeDrawer.value = activeDrawer.value === key ? null : key
}

const studyTarget = ref(getDailyTarget())
const difficultyPreference = ref('自适应')

// 学习目标行内编辑
const editingTarget = ref(false)
const targetDraft = ref(studyTarget.value)
const targetInputRef = ref(null)

function startEditTarget() {
  targetDraft.value = studyTarget.value
  editingTarget.value = true
  nextTick(() => {
    if (targetInputRef.value) {
      targetInputRef.value.focus()
      targetInputRef.value.select()
    }
  })
}

function saveTarget() {
  editingTarget.value = false
  const val = targetDraft.value
  if (!isNaN(val) && val > 0) {
    studyTarget.value = val
    setDailyTarget(val)
    updateChartTargetLine()
  }
}

function editDifficulty() {
  const options = ['简单', '自适应', '挑战']
  const current = options.indexOf(difficultyPreference.value)
  const next = (current + 1) % options.length
  difficultyPreference.value = options[next]
  // TODO: 调用后端 API 保存
}

const showAboutModal = ref(false)

function openAbout() {
  showAboutModal.value = true
  const audio = new Audio(aboutAudioSrc)
  audio.play().catch((e) => console.error('播放失败:', e.message))
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

// ========== ECharts 趋势图（每日在线时长）==========
const trendChartRef = ref(null)
let trendChart = null
const todayOnlineMinutes = ref(0)

async function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)

  // 从 IndexedDB 读取最近 14 天的在线时长
  const records = await getRecentOnlineTime(14)

  // 格式化日期为短格式 (MM/DD)
  const dates = records.map((r) => {
    const parts = r.date.split('-')
    return `${parseInt(parts[1])}/${parseInt(parts[2])}`
  })
  const data = records.map((r) => r.minutes)
  const todayIdx = data.length - 1

  // 更新今日在线时长显示
  todayOnlineMinutes.value = data[todayIdx]

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#E5E7EB',
      borderWidth: 1,
      textStyle: { color: '#1F2937', fontSize: 12 },
      formatter: function (params) {
        const val = params[0].value
        const h = Math.floor(val / 60)
        const m = Math.round(val % 60)
        if (h > 0) {
          return `${params[0].axisValue}<br/><b>${h} 小时 ${m} 分钟</b>`
        }
        return `${params[0].axisValue}<br/><b>${m} 分钟</b>`
      },
    },
    grid: { top: 20, bottom: 30, left: 45, right: 15 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#9CA3AF', fontSize: 10 },
    },
    yAxis: {
      type: 'value',
      name: '分钟',
      nameTextStyle: { color: '#9CA3AF', fontSize: 10 },
      splitLine: { lineStyle: { type: 'dashed', color: '#F3F4F6' } },
      axisLabel: { color: '#9CA3AF', fontSize: 10 },
    },
    series: [
      {
        name: '在线时长',
        data: data,
        type: 'bar',
        barWidth: '12px',
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: function (params) {
            if (params.dataIndex === todayIdx) {
              return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#6366F1' },
                { offset: 1, color: '#2563EB' },
              ])
            }
            return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#A5B4FC' },
              { offset: 1, color: '#93C5FD' },
            ])
          },
        },
      },
      {
        name: '每日目标',
        type: 'line',
        data: Array(dates.length).fill(studyTarget.value),
        symbol: 'none',
        lineStyle: { color: '#F59E0B', type: 'dashed', width: 1.5 },
        itemStyle: { color: '#F59E0B' },
        silent: true,
      },
    ],
  }
  trendChart.setOption(option)
  window.addEventListener('resize', handleResize)
}

function updateChartTargetLine() {
  if (!trendChart) return
  const target = studyTarget.value
  trendChart.setOption({
    series: [
      {},
      { data: Array(14).fill(target) },
    ],
  })
}

function handleResize() {
  if (trendChart) trendChart.resize()
}

function handleGlobalClick() {
  showLogoutConfirm.value = false
}

async function loadUserStats() {
  try {
    const [s, count] = await Promise.all([
      getStreak(),
      countHistory(),
    ])
    streak.value = s
    totalRead.value = count
  } catch (e) {
    console.error('加载用户统计失败:', e)
  }
}

onMounted(async () => {
  await nextTick()
  userStore.fetchProfile()        // 刷新后端 XP
  loadUserStats()                 // 刷新本地统计
  if (activeDrawer.value === 'trend') {
    initTrendChart()
  }
  document.addEventListener('click', handleGlobalClick)
})

// 离开趋势抽屉时销毁图表实例，保证下次进入时重新绑定新 DOM
function disposeChart() {
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
}

// 监听抽屉切换
watch(activeDrawer, async (val, oldVal) => {
  // 离开趋势抽屉 → 销毁图表
  if (oldVal === 'trend') {
    disposeChart()
  }
  // 进入趋势抽屉 → 重新初始化图表
  if (val === 'trend') {
    await nextTick()
    try {
      const mins = await getTodayMinutes()
      todayOnlineMinutes.value = mins
    } catch (_) { /* ignore */ }
    initTrendChart()
  }
  // 进入学习记录抽屉 → 刷新浏览历史
  if (val === 'records') {
    loadHistoryRecords()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('click', handleGlobalClick)
  disposeChart()
})
</script>

<style scoped>
/* ========== 抽屉便签样式 ========== */
.drawer-tabs {
  position: relative;
  z-index: 10;
}

.drawer-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #F3F4F6;
  color: #6B7280;
  border: none;
  border-radius: 12px 12px 0 0;
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;
  position: relative;
}

.drawer-tab-label:hover {
  background: #E5E7EB;
  color: #374151;
}

.drawer-tab-label.active {
  background: white;
  color: #2563EB;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
}

.drawer-tab-label.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #2563EB;
  border-radius: 3px 3px 0 0;
}

.drawer-content-wrapper {
  position: relative;
  min-height: 100px;
}

.drawer-card {
  border-radius: 0 16px 16px 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

/* ========== 抽屉切换动画 ========== */
.drawer-enter-active {
  animation: drawer-slide-in 0.5s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.drawer-leave-active {
  animation: drawer-slide-out 0.35s cubic-bezier(0.55, 0.06, 0.68, 0.19);
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
}

@keyframes drawer-slide-in {
  0% {
    opacity: 0;
    transform: translateY(-20px);
  }
  60% {
    opacity: 0.85;
    transform: translateY(-4px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes drawer-slide-out {
  0% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  100% {
    opacity: 0;
    transform: translateY(-12px) scale(0.98);
  }
}
</style>
