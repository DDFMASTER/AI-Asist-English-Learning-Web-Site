<template>
  <main class="page">
    <section class="panel">
      <h1>AI Assisted English Learning</h1>
      <p>{{ message }}</p>

      <button @click="testBackend">
        测试后端连接
      </button>
    </section>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'

const message = ref('Vue 前端已启动')

async function testBackend() {
  try {
    const data = await request.get('/connect-test')
    message.value = typeof data === 'string' ? data : JSON.stringify(data)
  } catch (error) {
    message.value = '后端请求失败，请检查 Tomcat 接口和 Vite 代理配置'
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: #f5f7fb;
  font-family: Arial, sans-serif;
}

.panel {
  width: min(480px, 92vw);
  padding: 32px;
  border: 1px solid #d8dee9;
  border-radius: 8px;
  background: #ffffff;
}

h1 {
  margin: 0 0 12px;
  font-size: 26px;
}

p {
  min-height: 24px;
  margin: 0 0 24px;
  color: #4b5563;
  word-break: break-word;
}

button {
  height: 40px;
  padding: 0 16px;
  border: 0;
  border-radius: 6px;
  background: #2563eb;
  color: white;
  cursor: pointer;
}
</style>
