<template>
  <div id="engli-app">
    <AppNav v-if="$route.name !== 'Login'" />
    <router-view />
    <LoginModal
      :visible="userStore.showLoginModal"
      @close="userStore.closeLoginModal()"
      @login-success="onLoginSuccess"
    />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import AppNav from '@/components/AppNav.vue'
import LoginModal from '@/components/LoginModal.vue'
import { useUserStore } from '@/stores/user'
import { startOnlineTimer, pauseTimer, resumeTimer } from '@/composables/useOnlineTimer'

const userStore = useUserStore()

function onLoginSuccess() {
  userStore.fetchProfile()
}

function onVisibilityChange() {
  if (document.hidden) {
    pauseTimer()
  } else {
    resumeTimer()
  }
}

function onBeforeUnload() {
  pauseTimer()
}

onMounted(() => {
  if (userStore.token) {
    userStore.fetchProfile()
  }
  // 启动在线时长追踪（无论是否登录都追踪）
  startOnlineTimer()
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('beforeunload', onBeforeUnload)
})

onUnmounted(() => {
  pauseTimer()
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('beforeunload', onBeforeUnload)
})
</script>

<style>
#engli-app {
  min-height: 100vh;
}
</style>
