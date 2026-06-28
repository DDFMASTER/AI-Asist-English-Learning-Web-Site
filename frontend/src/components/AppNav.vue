<template>
  <nav class="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-100">
    <div class="max-w-[1200px] mx-auto px-6 h-16 flex items-center justify-between">
      <!-- Logo -->
      <router-link to="/materials" class="flex items-center gap-2">
        <div class="w-8 h-8 bg-[#2563EB] rounded-lg flex items-center justify-center">
          <Icon icon="ph:book-open-text-bold" class="text-white text-xl" />
        </div>
        <span class="text-xl font-bold tracking-tight text-[#1F2937]">LinguaAI</span>
      </router-link>

      <!-- 导航链接 -->
      <div class="flex items-center gap-8">
        <router-link to="/materials" class="nav-link text-gray-500" active-class="nav-active">
          读物匹配
        </router-link>
        <router-link to="/tasks" class="nav-link text-gray-500" active-class="nav-active">
          任务管理
        </router-link>
        <router-link to="/assessment" class="nav-link text-gray-500" active-class="nav-active">
          测评中心
        </router-link>
      </div>

      <!-- 右侧操作 -->
      <div class="flex items-center gap-4">
        <!-- 铃铛通知 -->
        <button
          class="p-2 text-gray-400 hover:text-gray-600 relative"
          @click="guard(() => { showNotifications = !showNotifications })"
        >
          <Icon icon="ph:bell-bold" class="text-2xl" />
          <span
            v-if="unreadCount > 0"
            class="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[10px] rounded-full flex items-center justify-center"
          >
            {{ unreadCount }}
          </span>
        </button>

        <!-- 登录按钮（未登录时显示） -->
        <button
          v-if="!userStore.isLoggedIn"
          class="px-4 py-2 bg-[#2563EB] text-white rounded-lg text-sm font-bold hover:bg-blue-600 transition-all"
          @click="userStore.openLoginModal()"
        >
          登录
        </button>

        <!-- 头像（已登录时显示） -->
        <router-link
          v-else
          to="/profile"
          class="w-10 h-10 rounded-full bg-blue-100 border-2 border-white overflow-hidden cursor-pointer hover:ring-2 hover:ring-[#2563EB] transition-all"
        >
          <div class="w-full h-full rounded-full bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white text-sm font-bold">
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
import { ref, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { useUserStore } from '@/stores/user'
import { useRequireAuth } from '@/composables/useAuth'

const userStore = useUserStore()
const { guard } = useRequireAuth()
const showNotifications = ref(false)
const unreadCount = ref(0)

const avatarLetter = computed(() => {
  const username = userStore.user?.username || 'A'
  return username.charAt(0).toUpperCase()
})
</script>
