<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="fixed inset-0 z-[200] flex items-center justify-center"
      @click.self="handleClose"
    >
      <!-- 背景遮罩 -->
      <div class="absolute inset-0 bg-black/40 backdrop-blur-sm"></div>

      <!-- 弹窗 -->
      <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-md p-8 z-10">
        <!-- 关闭按钮 -->
        <button
          class="absolute top-4 right-4 text-gray-400 hover:text-gray-600"
          @click="handleClose"
        >
          <Icon icon="ph:x-bold" class="text-xl" />
        </button>

        <!-- 标题 -->
        <div class="text-center mb-8">
          <div class="w-12 h-12 bg-blue-50 text-[#2563EB] rounded-xl flex items-center justify-center mx-auto mb-4">
            <Icon icon="ph:book-open-text-bold" class="text-2xl" />
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
            class="w-full h-11 bg-[#2563EB] text-white rounded-xl font-bold text-sm hover:bg-blue-600 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
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
            class="text-sm font-bold text-[#2563EB] hover:underline ml-1"
            @click="toggleMode"
          >
            {{ isRegisterMode ? '去登录' : '注册' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Icon } from '@iconify/vue'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  visible: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'login-success'])

const userStore = useUserStore()
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

function handleClose() {
  emit('close')
  userStore.closeLoginModal()
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
      console.log('[LoginModal] 登录提交中...')
      await userStore.login(form.username, form.password)
      console.log('[LoginModal] 登录成功, pendingAction:', !!userStore.pendingAction)
      emit('login-success')
      userStore.runPendingAction() // 登录成功后执行被 guard 拦截的原操作
      console.log('[LoginModal] pendingAction 已执行')
      handleClose()
    }
  } catch (error) {
    errorMsg.value = error.message || '操作失败，请重试'
  } finally {
    submitting.value = false
  }
}
</script>
