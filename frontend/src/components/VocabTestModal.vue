<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="fixed inset-0 z-[150] flex items-center justify-center bg-black/40"
      @click.self="() => {}"
    >
      <div class="bg-white rounded-2xl shadow-2xl w-[720px] max-h-[90vh] flex flex-col">
        <!-- 头部 -->
        <div class="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
          <div>
            <h2 class="text-lg font-bold text-gray-800">词汇量测试</h2>
            <p class="text-xs text-gray-400 mt-0.5">
              勾选你认识的单词，诚实作答以获得准确结果
            </p>
          </div>
          <span class="text-sm text-gray-400">
            {{ checkedCount }} / {{ words.length }}
          </span>
        </div>

        <!-- 单词网格 -->
        <div class="flex-1 overflow-y-auto px-6 py-4">
          <div v-if="loading" class="text-center py-12 text-gray-400">加载测试词中...</div>

          <div v-else-if="result" class="text-center py-8">
            <div class="text-5xl mb-3">{{ cefrEmoji }}</div>
            <div class="text-2xl font-bold text-[#2563EB] mb-1">{{ result.cefrLevel }} · {{ result.cefrLabel }}</div>
            <div class="text-sm text-gray-500 mb-4">
              估算词汇量：<span class="font-bold text-gray-700">{{ result.estimatedVocab.toLocaleString() }}</span> 词
              <span class="text-gray-400 text-xs ml-1">
                ({{ result.lowerCI.toLocaleString() }} – {{ result.upperCI.toLocaleString() }})
              </span>
            </div>
            <div class="text-xs text-gray-400 mb-6">
              伪词命中率 {{ result.fakeHitRate }}%（越低越可信）
            </div>
            <button
              class="px-6 py-2 bg-[#2563EB] text-white rounded-lg text-sm font-medium hover:bg-blue-600 transition-colors"
              @click="$emit('done', result)"
            >开始学习</button>
          </div>

          <div v-else class="grid grid-cols-5 gap-2">
            <div
              v-for="(w, idx) in words"
              :key="idx"
              class="border rounded-lg p-2 text-center cursor-pointer transition-colors select-none"
              :class="w.checked
                ? 'border-[#2563EB] bg-blue-50 text-[#2563EB]'
                : 'border-gray-200 hover:border-gray-300 text-gray-700'"
              @click="toggleWord(idx)"
            >
              <span class="text-sm font-medium">{{ w.word }}</span>
              <div class="text-[10px] mt-0.5" :class="w.checked ? 'text-[#2563EB]' : 'text-gray-400'">
                {{ w.checked ? '✓ 认识' : '不认识' }}
              </div>
            </div>
          </div>
        </div>

        <!-- 底部按钮 -->
        <div v-if="!result" class="px-6 py-4 border-t border-gray-100 flex justify-end gap-2">
          <button
            class="px-4 py-2 text-sm rounded-lg bg-gray-100 text-gray-500 hover:bg-gray-200 transition-colors"
            @click="resetAll"
          >全部清除</button>
          <button
            class="px-6 py-2 text-sm rounded-lg bg-[#2563EB] text-white hover:bg-blue-600 transition-colors disabled:opacity-50"
            :disabled="submitting || loading"
            @click="submitTest"
          >
            {{ submitting ? 'AI 分析中...' : '提交测试' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  visible: Boolean,
})
const emit = defineEmits(['done', 'close'])

const userStore = useUserStore()
const words = ref([])
const loading = ref(false)
const submitting = ref(false)
const result = ref(null)

const checkedCount = computed(() => words.value.filter(w => w.checked).length)

const cefrEmoji = computed(() => {
  const map = { A1: '🌱', A2: '🌿', B1: '📗', B2: '📘', C1: '📚', C2: '🎓' }
  return map[result.value?.cefrLevel] || '📗'
})

function toggleWord(idx) {
  words.value[idx].checked = !words.value[idx].checked
}

function resetAll() {
  words.value.forEach(w => w.checked = false)
}

async function loadWords() {
  loading.value = true
  result.value = null
  try {
    const data = await request.get('/vocabtest/words')
    if (data.success) {
      words.value = (data.words || []).map(w => ({ ...w, checked: false }))
    }
  } catch (e) {
    console.error('加载测试词失败:', e)
  } finally {
    loading.value = false
  }
}

async function submitTest() {
  submitting.value = true
  try {
    const answers = words.value.map(w => w.checked ? '1' : '0').join(',')
    const params = new URLSearchParams()
    params.append('userId', String(userStore.user?.userId))
    params.append('answers', answers)
    const data = await request.post('/vocabtest/submit', params)
    if (data.success) {
      result.value = data
      // 存入本地（按用户 ID 隔离，切换账号不会串）
      const uid = userStore.user?.userId
      if (uid) {
        localStorage.setItem(`aael_vocab_result_${uid}`, JSON.stringify({
          literacy: data.estimatedVocab,
          cefrLevel: data.cefrLevel,
          cefrLabel: data.cefrLabel,
        }))
        userStore.user.literacy = data.estimatedVocab
        localStorage.setItem('user', JSON.stringify(userStore.user))
      }
    } else {
      alert(data.message || '提交失败')
    }
  } catch (e) {
    console.error('提交测试失败:', e)
    alert('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 监听 visible 变化，自动加载词表
import { watch } from 'vue'
watch(() => props.visible, (val) => {
  if (val) loadWords()
})
</script>  ← wrong closing tag
