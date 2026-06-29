<template>
  <div
    class="min-h-screen flex flex-col items-center justify-center relative overflow-hidden"
    :style="{ background: `linear-gradient(135deg, ${gradient.from}, ${gradient.to})` }"
  >
    <!-- 背景虚化装饰 -->
    <div class="absolute inset-0 backdrop-blur-[60px] bg-white/10"></div>
    <div class="absolute top-1/4 -left-20 w-72 h-72 rounded-full opacity-20" :style="{ background: gradient.from }"></div>
    <div class="absolute bottom-1/3 -right-20 w-96 h-96 rounded-full opacity-20" :style="{ background: gradient.to }"></div>
    <div class="absolute top-10 right-1/4 w-48 h-48 rounded-full opacity-10 bg-white"></div>

    <!-- 左上 Logo -->
    <div class="absolute top-8 left-8 z-20 flex items-center gap-2">
      <div class="w-8 h-8 bg-white/20 backdrop-blur rounded-lg flex items-center justify-center">
        <Icon icon="ph:book-open-text-bold" class="text-white text-xl" />
      </div>
      <span class="text-xl font-bold text-white tracking-tight">EngliAI</span>
    </div>

    <!-- 中央卡片 -->
    <div class="relative z-10 w-full max-w-md mx-4">
      <div class="bg-white/90 backdrop-blur-xl rounded-2xl shadow-2xl p-8">
        <!-- 标题 -->
        <div class="text-center mb-8">
          <div class="w-14 h-14 rounded-2xl flex items-center justify-center mx-auto mb-4" :style="{ background: `linear-gradient(135deg, ${gradient.from}, ${gradient.to})` }">
            <Icon icon="ph:book-open-text-bold" class="text-white text-3xl" />
          </div>
          <h2 class="text-2xl font-bold text-[#1F2937]">
            {{ isRegisterMode ? '创建账号' : '欢迎回来' }}
          </h2>
          <p class="text-sm text-gray-400 mt-1">
            {{ isRegisterMode ? '开始你的英语学习之旅' : '登录以继续学习' }}
          </p>
        </div>

        <!-- 表单 -->
        <form @submit.prevent="handleSubmit" class="space-y-4">
          <!-- 用户名 -->
          <div>
            <label class="block text-sm font-medium text-gray-600 mb-1">用户名</label>
            <input
              v-model="form.username"
              type="text"
              maxlength="16"
              required
              class="w-full h-11 px-4 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-[#2563EB] focus:ring-1 focus:ring-[#2563EB] transition-all"
              :placeholder="isRegisterMode ? '请输入用户名（≤16字符）' : '请输入用户名'"
            />
          </div>

          <!-- 密码 -->
          <div>
            <label class="block text-sm font-medium text-gray-600 mb-1">密码</label>
            <input
              v-model="form.password"
              type="password"
              required
              class="w-full h-11 px-4 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-[#2563EB] focus:ring-1 focus:ring-[#2563EB] transition-all"
              placeholder="请输入密码"
            />
          </div>

          <!-- 学习阶段（仅注册） -->
          <div v-if="isRegisterMode">
            <label class="block text-sm font-medium text-gray-600 mb-1">学习阶段</label>
            <select
              v-model="form.studyPurpose"
              required
              class="w-full h-11 px-4 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-[#2563EB] focus:ring-1 focus:ring-[#2563EB] transition-all bg-white"
            >
              <option value="">-- 请选择 --</option>
              <option v-for="s in stages" :key="s" :value="s">{{ s }}</option>
            </select>
          </div>

          <!-- 错误提示 -->
          <div v-if="errorMsg" class="text-sm text-red-500 bg-red-50 rounded-lg px-4 py-2">
            {{ errorMsg }}
          </div>

          <!-- 提交按钮 -->
          <button
            type="submit"
            :disabled="submitting"
            class="w-full h-11 text-white rounded-xl font-bold text-sm transition-all disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-lg"
            :style="{ background: `linear-gradient(135deg, ${gradient.from}, ${gradient.to})` }"
          >
            {{ submitting ? '处理中...' : (isRegisterMode ? '注册' : '登录') }}
          </button>
        </form>

        <!-- 切换登录/注册 -->
        <div class="text-center mt-6">
          <span class="text-sm text-gray-400">
            {{ isRegisterMode ? '已有账号？' : '还没有账号？' }}
          </span>
          <button
            class="text-sm font-bold hover:underline ml-1"
            :style="{ color: gradient.from }"
            @click="toggleMode"
          >
            {{ isRegisterMode ? '去登录' : '注册' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 底部介绍文字 -->
    <div class="relative z-10 mt-10 mb-8 px-4 max-w-2xl text-center">
      <p class="text-white/80 text-sm leading-relaxed">
        EngliAI 是一款基于人工智能的英语学习平台，通过智能分析你的词汇量、阅读偏好和学习目标，
        为你精准匹配适合当前水平的英文读物。结合自适应测评系统与游戏化学习体验，
        让每一次阅读都成为高效的能力进阶之旅。
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 随机双色渐变
const colorPairs = [
  { from: '#2563EB', to: '#7C3AED' },   // 蓝 → 紫
  { from: '#0891B2', to: '#2563EB' },   // 青 → 蓝
  { from: '#4F46E5', to: '#DB2777' },   // 靛 → 粉
  { from: '#059669', to: '#0891B2' },   // 绿 → 青
  { from: '#EA580C', to: '#DC2626' },   // 橙 → 红
  { from: '#7C3AED', to: '#2563EB' },   // 紫 → 蓝
  { from: '#0284C7', to: '#4F46E5' },   // 天蓝 → 靛
  { from: '#9333EA', to: '#DB2777' },   // 紫 → 玫红
]
const gradient = colorPairs[Math.floor(Math.random() * colorPairs.length)]

const isRegisterMode = ref(false)
const submitting = ref(false)
const errorMsg = ref('')

const stages = ['初中', '高中', '四级', '六级', '考研', '托福']

const form = reactive({
  username: '',
  password: '',
  studyPurpose: '四级',
})

function toggleMode() {
  isRegisterMode.value = !isRegisterMode.value
  errorMsg.value = ''
  form.username = ''
  form.password = ''
}

async function handleSubmit() {
  errorMsg.value = ''
  submitting.value = true

  try {
    if (isRegisterMode.value) {
      await userStore.register(form.username, form.password, form.studyPurpose)
      // 注册成功后切换到登录
      isRegisterMode.value = false
      errorMsg.value = ''
    } else {
      await userStore.login(form.username, form.password)
      // 清除可能存在的过期 pendingAction（全页面登录后会跳转，不需要重试原操作）
      userStore.runPendingAction()
      // 登录成功后跳转
      const redirect = route.query.redirect || '/materials'
      router.push(redirect)
    }
  } catch (error) {
    errorMsg.value = error.message || '操作失败，请重试'
  } finally {
    submitting.value = false
  }
}
</script>