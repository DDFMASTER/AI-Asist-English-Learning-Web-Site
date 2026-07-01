<template>
  <nav class="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-100">
    <div class="max-w-[1200px] mx-auto px-6 h-16 flex items-center justify-between">
      <!-- 左侧：Logo + 导航链接 -->
      <div class="flex items-center gap-6">
        <router-link to="/materials" class="flex items-center gap-2 shrink-0">
          <div class="w-8 h-8 bg-[#2563EB] rounded-lg flex items-center justify-center">
            <Icon icon="ph:book-open-text-bold" class="text-white text-xl" />
          </div>
          <span class="text-xl font-bold tracking-tight text-[#1F2937]">EngliAI</span>
        </router-link>

        <!-- 导航链接 -->
        <router-link to="/materials" class="nav-link text-gray-500" active-class="nav-active">
          读物匹配
        </router-link>
        <router-link to="/assessment" class="nav-link text-gray-500" active-class="nav-active">
          测评中心
        </router-link>
        <router-link
          v-if="userStore.user?.role === 'admin'"
          to="/admin"
          class="nav-link text-gray-500"
          active-class="nav-active"
        >
          管理
        </router-link>
      </div>

      <!-- 中间：搜索框 -->
      <div class="relative flex-1 max-w-md mx-8" ref="searchContainer">
        <div class="relative">
          <Icon
            icon="ph:magnifying-glass-bold"
            class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-lg pointer-events-none"
          />
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索单词..."
            class="w-full h-10 pl-10 pr-4 bg-gray-100 rounded-xl text-sm text-gray-700 placeholder-gray-400 focus:outline-none focus:bg-white focus:ring-2 focus:ring-[#2563EB] focus:border-transparent transition-all"
            @keydown.enter="handleSearch"
            @focus="onSearchFocus"
          />
          <!-- 搜索中 loading -->
          <div v-if="searching" class="absolute right-3 top-1/2 -translate-y-1/2">
            <Icon icon="ph:spinner-bold" class="text-gray-400 text-lg animate-spin" />
          </div>
          <!-- 清除按钮 -->
          <button
            v-if="searchQuery && !searching"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-300 hover:text-gray-500"
            @click="clearSearch"
          >
            <Icon icon="ph:x-circle-fill" class="text-lg" />
          </button>
        </div>

        <!-- 搜索结果卡片 -->
        <div
          v-if="showResult && searchResult"
          class="absolute top-full mt-2 left-0 right-0 bg-white rounded-xl shadow-xl border border-gray-100 p-5 z-50"
        >
          <!-- 未找到 -->
          <div v-if="!searchResult.found" class="text-center py-2">
            <Icon icon="ph:smiley-sad-bold" class="text-3xl text-gray-300 mb-2" />
            <p class="text-sm text-gray-400">未找到该单词的释义</p>
          </div>

          <!-- 找到结果 -->
          <template v-else>
            <!-- 单词与音标 -->
            <div class="flex items-center justify-between mb-3">
              <div>
                <h3 class="text-xl font-bold text-[#2563EB]">{{ searchResult.word }}</h3>
                <p v-if="searchResult.phonetic" class="text-xs text-gray-400 mt-0.5">{{ searchResult.phonetic }}</p>
                <p v-if="searchResult.lemmaFrom" class="text-xs text-gray-400 mt-0.5">
                  原形: {{ searchResult.lemmaTo }}
                </p>
              </div>
              <button
                class="text-gray-400 hover:text-red-500 transition-colors"
                @click="clearSearch"
              >
                <Icon icon="ph:x-bold" class="text-lg" />
              </button>
            </div>

            <!-- 释义列表 -->
            <div class="space-y-2 max-h-64 overflow-y-auto">
              <div
                v-for="(group, idx) in searchResult.results"
                :key="idx"
                class="p-3 bg-gray-50 rounded-lg"
              >
                <span class="text-xs font-bold text-gray-500 mb-1.5 block">{{ group.source }}</span>
                <div class="space-y-1">
                  <div
                    v-for="entry in group.entries"
                    :key="entry.id"
                    class="flex items-start gap-2"
                  >
                    <span class="text-xs text-gray-400 mt-0.5 shrink-0">{{ entry.id }}.</span>
                    <span class="text-sm text-gray-700">{{ entry.translation }}</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- 右侧操作 -->
      <div class="flex items-center gap-4 shrink-0">
        <!-- 铃铛通知 -->
        <button
          class="p-2 text-gray-400 hover:text-gray-600 relative"
          @click="showNotifications = !showNotifications; showResult = false"
        >
          <Icon icon="ph:bell-bold" class="text-2xl" />
          <span
            v-if="unreadCount > 0"
            class="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[10px] rounded-full flex items-center justify-center"
          >
            {{ unreadCount }}
          </span>
        </button>

        <!-- 头像 -->
        <router-link
          to="/profile"
          class="w-10 h-10 rounded-xl border-2 border-white overflow-hidden cursor-pointer hover:ring-2 hover:ring-[#2563EB] transition-all"
        >
          <img
            v-if="navAvatarSrc"
            :src="navAvatarSrc"
            alt="头像"
            class="w-full h-full object-cover rounded-xl"
          />
          <div v-else class="w-full h-full rounded-xl bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white text-sm font-bold">
            {{ avatarLetter }}
          </div>
        </router-link>
      </div>
    </div>

    <!-- 通知浮窗 -->
    <div
      v-if="showNotifications"
      class="absolute right-0 top-16 w-80 bg-white rounded-xl shadow-xl border border-gray-100 p-4 z-50 mr-6"
    >
      <div class="text-sm font-bold mb-3">通知</div>
      <div class="text-xs text-gray-400 text-center py-4">
        暂无新通知
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useUserStore } from '@/stores/user'
import { useReaderStore } from '@/stores/reader'

const userStore = useUserStore()
const readerStore = useReaderStore()

const showNotifications = ref(false)
const unreadCount = ref(0)
const searchContainer = ref(null)

// 搜索相关
const searchQuery = ref('')
const searching = ref(false)
const searchResult = ref(null)
const showResult = ref(false)

const avatarLetter = computed(() => {
  const username = userStore.user?.username || 'A'
  return username.charAt(0).toUpperCase()
})

// 导航栏头像（与个人中心同步，读取 localStorage）
const photoBase = import.meta.env.BASE_URL + 'photo/'
const avatarFiles = ['1.白猫.svg','2.边牧.svg','3.布偶猫.svg','4.仓鼠.svg','5.藏獒.svg','6.柴犬.svg','7.哈士奇.svg','8.荷兰猪.svg','9.黑猫.svg','10.金毛.svg','11.橘猫.svg','12.柯基.svg','13.可达鸭.svg','14.蓝猫.svg','15.奶牛猫.svg','16.三花猫.svg','17.田园犬.svg','18.暹罗猫.svg','19.羊.svg','20.bvvd.png']

function getAvatarSrc() {
  try {
    const id = parseInt(localStorage.getItem('aael_selected_avatar'), 10)
    if (!isNaN(id) && id >= 1 && id <= avatarFiles.length) {
      return photoBase + avatarFiles[id - 1]
    }
  } catch (_) { /* ignore */ }
  return photoBase + avatarFiles[0]
}

const navAvatarSrc = ref(getAvatarSrc())

// 监听头像变更事件（由 ProfilePage 触发）
function onAvatarChanged() {
  navAvatarSrc.value = getAvatarSrc()
}

onMounted(() => {
  document.addEventListener('click', onClickOutside)
  window.addEventListener('avatar-changed', onAvatarChanged)
})

onUnmounted(() => {
  document.removeEventListener('click', onClickOutside)
  window.removeEventListener('avatar-changed', onAvatarChanged)
})

// 点击外部关闭搜索卡片和通知面板
function onClickOutside(e) {
  if (searchContainer.value && !searchContainer.value.contains(e.target)) {
    showResult.value = false
  }
  // 通知面板的关闭逻辑：点击非铃铛区域时关闭
  const bellBtn = e.target.closest('button[class*="p-2 text-gray-400"]')
  if (!bellBtn && showNotifications.value) {
    showNotifications.value = false
  }
}

async function handleSearch() {
  const query = searchQuery.value.trim()
  if (!query) return

  searching.value = true
  showResult.value = false
  searchResult.value = null

  try {
    const result = await readerStore.lookupWord(query)
    searchResult.value = result
    showResult.value = true
  } catch (err) {
    searchResult.value = { success: true, word: query, found: false, results: [] }
    showResult.value = true
  } finally {
    searching.value = false
  }
}

function clearSearch() {
  searchQuery.value = ''
  searchResult.value = null
  showResult.value = false
}

function onSearchFocus() {
  if (searchResult.value) showResult.value = true
}
</script>

<style scoped>
/* 搜索动画 */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.animate-spin {
  animation: spin 0.8s linear infinite;
}
</style>
