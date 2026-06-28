<template>
  <div id="lingua-app">
    <AppNav />
    <router-view />
    <LoginModal
      :visible="userStore.showLoginModal"
      @close="userStore.closeLoginModal()"
      @login-success="onLoginSuccess"
    />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import AppNav from '@/components/AppNav.vue'
import LoginModal from '@/components/LoginModal.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

function onLoginSuccess() {
  // 登录成功后的回调
  userStore.fetchProfile()
}

onMounted(() => {
  // 尝试从 localStorage 恢复用户状态
  if (userStore.token) {
    userStore.fetchProfile()
  }
})
</script>

<style>
#lingua-app {
  min-height: 100vh;
}
</style>
