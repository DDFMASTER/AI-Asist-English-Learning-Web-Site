<template>
  <main class="max-w-[1200px] mx-auto px-6 mt-10">
    <!-- 用户信息卡片 -->
    <div class="card flex items-center gap-8 mb-8">
      <div
        class="w-20 h-20 rounded-xl flex items-center justify-center flex-none cursor-pointer hover:ring-4 hover:ring-blue-200 transition-all overflow-hidden relative group"
        @click="showAvatarPicker = true"
        title="点击更换头像"
      >
        <img
          v-if="currentAvatarSrc"
          :src="currentAvatarSrc"
          :alt="'头像 ' + currentAvatarId"
          class="w-full h-full object-cover"
        />
        <span v-else class="text-white text-2xl font-bold bg-gradient-to-br from-blue-400 to-purple-500 w-full h-full flex items-center justify-center">
          {{ avatarLetter }}
        </span>
        <!-- 悬停提示 -->
        <div class="absolute inset-0 bg-black/30 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
          <Icon icon="ph:camera-bold" class="text-white text-xl" />
        </div>
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

    <!-- ========== 头像选择弹窗 ========== -->
    <Teleport to="body">
      <div
        v-if="showAvatarPicker"
        class="fixed inset-0 z-[200] flex items-center justify-center bg-black/40 backdrop-blur-sm"
        @click.self="showAvatarPicker = false"
      >
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[80vh] overflow-y-auto p-6">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-bold">选择头像</h3>
            <button
              class="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center hover:bg-gray-200 transition-colors"
              @click="showAvatarPicker = false"
            >
              <Icon icon="ph:x-bold" class="text-gray-500" />
            </button>
          </div>
          <div class="grid grid-cols-5 gap-3">
            <div
              v-for="avatar in avatarList"
              :key="avatar.id"
              class="aspect-square rounded-xl overflow-hidden cursor-pointer border-2 transition-all hover:scale-105"
              :class="currentAvatarId === avatar.id ? 'border-[#2563EB] ring-2 ring-blue-200' : 'border-gray-100 hover:border-gray-300'"
              @click="selectAvatar(avatar)"
            >
              <img
                :src="photoBase + avatar.file"
                :alt="avatar.name"
                class="w-full h-full object-cover"
              />
            </div>
          </div>
        </div>
      </div>
    </Teleport>

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

        <!-- 生词本 -->
        <div v-else-if="activeDrawer === 'vocab'" key="vocab" class="card drawer-card">
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-bold">📝 生词本</h3>
            <span class="text-xs text-gray-400">{{ vocabWords.length }} 个单词</span>
          </div>
          <div v-if="vocabWords.length === 0" class="text-center py-12 text-gray-400">
            <Icon icon="ph:bookmark-simple-bold" class="text-3xl mx-auto mb-3 opacity-30" />
            <p class="text-sm">暂无生词</p>
            <p class="text-xs mt-1">在阅读时点击单词卡片右上角的 <span class="text-[#2563EB] font-bold">+</span> 即可收藏</p>
          </div>
          <div v-else class="max-h-96 overflow-y-auto">
            <div class="grid grid-cols-4 gap-3">
              <div
                v-for="item in vocabWords"
                :key="item.wordLower"
                class="p-3 bg-gray-50 rounded-xl group hover:bg-blue-50 transition-colors relative"
              >
                <button
                  class="absolute top-1.5 right-1.5 text-gray-300 hover:text-red-400 transition-colors opacity-0 group-hover:opacity-100"
                  @click="removeVocabWord(item.wordLower)"
                  title="移出生词本"
                >
                  <Icon icon="ph:x-circle-fill" class="text-sm" />
                </button>
                <p class="text-sm font-bold text-gray-800 truncate pr-4">{{ item.word }}</p>
                <p v-if="item.phonetic" class="text-[10px] text-gray-400 font-mono truncate mt-0.5">{{ item.phonetic }}</p>
                <p class="text-xs text-gray-500 mt-1 line-clamp-2 leading-relaxed">{{ item.translation }}</p>
                <div class="flex items-center gap-1.5 mt-2">
                  <span v-if="item.source" class="text-[10px] text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded">{{ item.source }}</span>
                  <span class="text-[10px] text-gray-300">{{ formatAddedTime(item.addedAt) }}</span>
                </div>
              </div>
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
import { useTaskStore } from '@/stores/task'
import { useRequireAuth } from '@/composables/useAuth'
import { getRecentOnlineTime, getTodayMinutes, getDailyTarget, setDailyTarget, getStreak } from '@/utils/onlineTimeDB'
import { getRecentHistory, countHistory } from '@/utils/historyDB'
const gggAudioModules = import.meta.glob('../ggg/*.mp3', { eager: true })
const gggAudioFiles = Object.values(gggAudioModules).map(m => m.default)

const router = useRouter()
const userStore = useUserStore()
const taskStore = useTaskStore()
const { guard } = useRequireAuth()

// ========== 头像 ==========
const AVATAR_STORAGE_KEY = 'aael_selected_avatar'
const defaultAvatarId = 1

// 头像资源基础路径（dev → /photo/, prod → ./photo/）
const photoBase = import.meta.env.BASE_URL + 'photo/'

// 头像列表（与 photo/ 目录下文件名对应）
const avatarList = [
  { id: 1,  file: '1.白猫.svg',   name: '白猫' },
  { id: 2,  file: '2.边牧.svg',   name: '边牧' },
  { id: 3,  file: '3.布偶猫.svg', name: '布偶猫' },
  { id: 4,  file: '4.仓鼠.svg',   name: '仓鼠' },
  { id: 5,  file: '5.藏獒.svg',   name: '藏獒' },
  { id: 6,  file: '6.柴犬.svg',   name: '柴犬' },
  { id: 7,  file: '7.哈士奇.svg', name: '哈士奇' },
  { id: 8,  file: '8.荷兰猪.svg', name: '荷兰猪' },
  { id: 9,  file: '9.黑猫.svg',   name: '黑猫' },
  { id: 10, file: '10.金毛.svg',  name: '金毛' },
  { id: 11, file: '11.橘猫.svg',  name: '橘猫' },
  { id: 12, file: '12.柯基.svg',  name: '柯基' },
  { id: 13, file: '13.可达鸭.svg', name: '可达鸭' },
  { id: 14, file: '14.蓝猫.svg',  name: '蓝猫' },
  { id: 15, file: '15.奶牛猫.svg', name: '奶牛猫' },
  { id: 16, file: '16.三花猫.svg', name: '三花猫' },
  { id: 17, file: '17.田园犬.svg', name: '田园犬' },
  { id: 18, file: '18.暹罗猫.svg', name: '暹罗猫' },
  { id: 19, file: '19.羊.svg',    name: '羊' },
  { id: 20, file: '20.bvvd.png',  name: 'bvvd' },
]

function loadSelectedAvatarId() {
  try {
    const val = localStorage.getItem(AVATAR_STORAGE_KEY)
    if (val !== null) {
      const num = parseInt(val, 10)
      if (!isNaN(num) && num >= 1 && num <= avatarList.length) return num
    }
  } catch (_) { /* ignore */ }
  return defaultAvatarId
}

function saveSelectedAvatarId(id) {
  localStorage.setItem(AVATAR_STORAGE_KEY, String(id))
}

const currentAvatarId = ref(loadSelectedAvatarId())
const showAvatarPicker = ref(false)

const currentAvatarSrc = computed(() => {
  const avatar = avatarList.find(a => a.id === currentAvatarId.value)
  return avatar ? photoBase + avatar.file : ''
})

function selectAvatar(avatar) {
  currentAvatarId.value = avatar.id
  saveSelectedAvatarId(avatar.id)
  showAvatarPicker.value = false
  // 通知导航栏同步更新头像
  window.dispatchEvent(new CustomEvent('avatar-changed'))
}

// ========== 用户信息 ==========
const showEditProfile = ref(false)
const username = computed(() => userStore.user?.username || 'Alex')
function literacyToLevel(literacy) {
  if (!literacy || literacy === 0) return null
  if (literacy < 1500) return 'A1 · 初级'
  if (literacy < 3000) return 'A2 · 初级上'
  if (literacy < 5000) return 'B1 · 中级'
  if (literacy < 8000) return 'B2 · 中高级'
  if (literacy < 12000) return 'C1 · 高级'
  return 'C2 · 精通'
}
function getLocalVocabResult() {
  try {
    const uid = userStore.user?.userId
    if (!uid) return null
    const raw = localStorage.getItem(`aael_vocab_result_${uid}`)
    return raw ? JSON.parse(raw) : null
  } catch { return null }
}
const userLevel = computed(() => {
  const local = getLocalVocabResult()
  if (local) return `${local.cefrLevel} · ${local.cefrLabel}`
  return literacyToLevel(userStore.user?.literacy) || userStore.user?.study_purpose || 'B1 · 中级'
})
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
  { key: 'vocab', label: '生词本', emoji: '📝' },
  { key: 'badges', label: '勋章墙', emoji: '🏆' },
  { key: 'settings', label: '设置', emoji: '⚙️' },
]
const activeDrawer = ref('trend')

function toggleDrawer(key) {
  // 点击已打开的便签 → 关闭；点击其他便签 → 切换
  activeDrawer.value = activeDrawer.value === key ? null : key
}

// ========== 生词本 ==========
const vocabWords = ref([])
let vocabTimer = null
const VOCAB_REVIEW_SECONDS = 30  // 停留 30 秒视为完成复习任务

function startVocabTimer() {
  clearVocabTimer()
  vocabTimer = setTimeout(() => {
    taskStore.completeVocabReviewTask()
  }, VOCAB_REVIEW_SECONDS * 1000)
}

function clearVocabTimer() {
  if (vocabTimer) {
    clearTimeout(vocabTimer)
    vocabTimer = null
  }
}

async function loadVocabWords() {
  try {
    const { getAllVocab } = await import('@/utils/vocabDB')
    vocabWords.value = await getAllVocab()
  } catch (e) {
    console.error('加载生词本失败:', e)
  }
}

async function removeVocabWord(wordLower) {
  try {
    const { removeFromVocab } = await import('@/utils/vocabDB')
    await removeFromVocab(wordLower)
    vocabWords.value = vocabWords.value.filter(v => v.wordLower !== wordLower)
  } catch (e) {
    console.error('移出生词失败:', e)
  }
}

function formatAddedTime(timestamp) {
  const now = Date.now()
  const diff = now - timestamp
  const days = Math.floor(diff / 86400000)
  if (days < 1) return '今天'
  if (days < 2) return '昨天'
  if (days < 7) return `${days}天前`
  const d = new Date(timestamp)
  return `${d.getMonth() + 1}/${d.getDate()}`
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
  const randomSrc = gggAudioFiles[Math.floor(Math.random() * gggAudioFiles.length)]
  const audio = new Audio(randomSrc)
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
  // 进入生词本抽屉 → 刷新生词列表 + 启动复习计时
  if (val === 'vocab') {
    loadVocabWords()
    startVocabTimer()
  }
  // 离开生词本 → 停止计时
  if (oldVal === 'vocab') {
    clearVocabTimer()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('click', handleGlobalClick)
  clearVocabTimer()
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
