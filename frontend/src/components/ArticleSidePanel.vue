<template>
  <div
    v-if="visible"
    class="fixed z-[55] w-[340px] max-h-[calc(100vh-160px)] overflow-y-auto"
    :style="{ right: position.x + 'px', top: position.y + 'px' }"
  >
    <!-- ========== 文化背景 ========== -->
    <div class="glass-popover p-4 mb-4">
      <div class="flex items-center gap-2 mb-3">
        <Icon icon="ph:globe-hemisphere-east-bold" class="text-amber-500 text-lg" />
        <h3 class="text-sm font-bold text-gray-700">文化背景讲解</h3>
      </div>

      <!-- 加载中 -->
      <div v-if="culturalNotes?.loading" class="flex items-center justify-center py-6">
        <Icon icon="ph:spinner-bold" class="text-base text-amber-500 animate-spin" />
        <span class="text-xs text-gray-400 ml-2">AI 正在分析全文...</span>
      </div>

      <!-- 错误 -->
      <div v-else-if="culturalNotes?.error" class="text-center py-4 px-2">
        <Icon icon="ph:warning-circle-bold" class="text-lg text-red-400 mx-auto mb-1" />
        <p class="text-[11px] text-red-500">{{ culturalNotes.error }}</p>
        <p class="text-[10px] text-gray-400 mt-1">请检查 Tomcat 是否已重启</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!culturalNotes?.notes || culturalNotes.notes.length === 0" class="text-center py-4">
        <Icon icon="ph:check-circle-bold" class="text-base text-green-400 mx-auto mb-1" />
        <p class="text-xs text-gray-400">本文暂无明显文化背景知识需要讲解</p>
      </div>

      <!-- 文化点列表 -->
      <div v-else class="space-y-2">
        <div
          v-for="(note, idx) in culturalNotes.notes"
          :key="idx"
          class="border border-amber-100 rounded-lg overflow-hidden"
        >
          <button
            class="w-full flex items-center justify-between px-3 py-2 bg-amber-50/50 hover:bg-amber-50 text-left transition-colors"
            @click="toggleNote(idx)"
          >
            <span class="text-xs font-bold text-amber-700">{{ note.title }}</span>
            <Icon
              :icon="expandedNotes.has(idx) ? 'ph:caret-up-bold' : 'ph:caret-down-bold'"
              class="text-xs text-amber-400 flex-none"
            />
          </button>
          <div v-if="expandedNotes.has(idx)" class="px-3 py-2 bg-white">
            <p class="text-xs text-gray-700 leading-relaxed">
              <template v-for="(seg, sIdx) in tokenizeText(note.content)" :key="sIdx">
                <span v-if="seg.type === 'text'">{{ seg.text }}</span>
                <span
                  v-else-if="seg.type === 'word'"
                  class="interactive-word text-gray-900"
                  @click.stop="(e) => handleCultureWordClick(seg.data, e)"
                >{{ seg.data.word }}</span>
              </template>
            </p>
            <!-- 点击查看中文翻译 -->
            <button
              v-if="note.zh"
              class="mt-1.5 text-[11px] text-blue-500 hover:text-blue-600 transition-colors"
              @click.stop="toggleZh(idx)"
            >
              <Icon icon="ph:translate-bold" class="inline text-[10px] mr-0.5" />
              {{ showZh.has(idx) ? '收起翻译' : '查看中文翻译' }}
            </button>
            <p v-if="showZh.has(idx) && note.zh" class="mt-1 text-xs text-gray-500 leading-relaxed pl-2 border-l-2 border-blue-200">
              {{ note.zh }}
            </p>
          </div>
        </div>
        <p class="text-[9px] text-gray-300 text-center pt-1">
          内容由 AI 生成，仅供参考
        </p>
      </div>
    </div>

    <!-- ========== 阅读选择题 ========== -->
    <div class="glass-popover p-4">
      <div class="flex items-center gap-2 mb-3">
        <Icon icon="ph:question-bold" class="text-[#2563EB] text-lg" />
        <h3 class="text-sm font-bold text-gray-700">阅读选择题</h3>
      </div>

      <!-- 加载中 -->
      <div v-if="quizData?.loading" class="flex items-center justify-center py-6">
        <Icon icon="ph:spinner-bold" class="text-base text-[#2563EB] animate-spin" />
        <span class="text-xs text-gray-400 ml-2">AI 正在阅读全文并出题...</span>
      </div>

      <!-- 错误 -->
      <div v-else-if="quizData?.error" class="text-center py-4 px-2">
        <Icon icon="ph:warning-circle-bold" class="text-lg text-red-400 mx-auto mb-1" />
        <p class="text-[11px] text-red-500">{{ quizData.error }}</p>
        <p class="text-[10px] text-gray-400 mt-1">请检查 Tomcat 是否已重启</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!quizData?.questions || quizData.questions.length === 0" class="text-center py-4">
        <p class="text-xs text-gray-400">暂无题目</p>
      </div>

      <!-- 题目列表 -->
      <div v-else class="space-y-4">
        <div
          v-for="(q, qIdx) in quizData.questions"
          :key="qIdx"
          class="border border-gray-100 rounded-lg p-3"
        >
          <p class="text-xs font-bold text-gray-700 mb-2">
            {{ qIdx + 1 }}. {{ q.question }}
          </p>
          <div class="space-y-1.5">
            <button
              v-for="(opt, oIdx) in q.options"
              :key="oIdx"
              class="w-full text-left px-3 py-1.5 rounded-lg text-xs transition-all"
              :class="getOptionClass(qIdx, oIdx)"
              :disabled="hasAnswered(qIdx)"
              @click="selectAnswer(qIdx, oIdx)"
            >
              <span class="font-bold mr-2">{{ 'ABCD'[oIdx] }}.</span>
              {{ opt }}
              <Icon
                v-if="hasAnswered(qIdx) && oIdx === q.answer"
                icon="ph:check-circle-bold"
                class="text-green-500 text-sm ml-1 inline"
              />
              <Icon
                v-if="hasAnswered(qIdx) && userAnswers[qIdx] === oIdx && oIdx !== q.answer"
                icon="ph:x-circle-bold"
                class="text-red-500 text-sm ml-1 inline"
              />
            </button>
          </div>
          <!-- 答案解析 -->
          <div
            v-if="hasAnswered(qIdx)"
            class="mt-2 px-3 py-2 bg-blue-50 rounded-lg"
          >
            <p class="text-[11px] text-blue-700">
              <span class="font-bold">解析：</span>{{ q.explanation }}
            </p>
          </div>
        </div>
        <p class="text-[9px] text-gray-300 text-center pt-1">
          内容由 AI 生成，仅供参考
        </p>
      </div>
    </div>

    <!-- 单词查词浮窗 -->
    <WordPopover
      :visible="wordPopover.visible"
      :word="wordPopover.word"
      :loading="wordPopover.loading"
      :position="wordPopover.position"
      @close="wordPopover.visible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Icon } from '@iconify/vue'
import { useUserStore } from '@/stores/user'
import { useReaderStore } from '@/stores/reader'
import WordPopover from '@/components/WordPopover.vue'

const props = defineProps({
  visible: { type: Boolean, default: true },
  culturalNotes: { type: Object, default: () => null },
  quizData: { type: Object, default: () => null },
  position: {
    type: Object,
    default: () => ({ x: 20, y: 140 }),
  },
})

const userStore = useUserStore()
const readerStore = useReaderStore()

// ========== 单词分词（用于文化背景内容中的单词点击查词） ==========
function tokenizeText(text) {
  const segments = []
  const wordRe = /([a-zA-Z]+(?:['-][a-zA-Z]+)*)/g
  let lastIdx = 0
  let match

  while ((match = wordRe.exec(text)) !== null) {
    if (match.index > lastIdx) {
      segments.push({ type: 'text', text: text.slice(lastIdx, match.index) })
    }
    segments.push({
      type: 'word',
      text: match[0],
      data: { word: match[0] },
    })
    lastIdx = match.index + match[0].length
  }
  if (lastIdx < text.length) {
    segments.push({ type: 'text', text: text.slice(lastIdx) })
  }
  return segments
}

// ========== 单词查词浮窗 ==========
const wordPopover = reactive({
  visible: false,
  loading: false,
  word: { word: '', results: [] },
  position: { x: 20, y: 140 },
})

function handleCultureWordClick(wordData, event) {
  event.stopPropagation()
  wordPopover.word = { word: wordData.word, results: [] }
  wordPopover.loading = true
  wordPopover.visible = true
  lookupCultureWord(wordData.word)
}

async function lookupCultureWord(word) {
  const studyPurpose = userStore.user?.studyPurpose || ''
  const result = await readerStore.lookupWord(word, studyPurpose)

  // 竞态保护
  if (wordPopover.word.word.toLowerCase() !== word.toLowerCase()) {
    return
  }

  if (result.success) {
    wordPopover.word = {
      word: result.word,
      phonetic: result.phonetic || '',
      results: result.results || [],
      found: result.found,
      lemmaFrom: result.lemmaFrom || '',
      lemmaTo: result.lemmaTo || '',
    }
    readerStore.prefetchAIExamples(result.word || word)
  } else {
    wordPopover.word = {
      word,
      results: [],
      found: false,
      error: result.message || '查询失败',
    }
  }
  wordPopover.loading = false
}

const expandedNotes = ref(new Set())
const showZh = ref(new Set())
const userAnswers = ref({}) // { questionIndex: selectedOptionIndex }

function toggleNote(idx) {
  if (expandedNotes.value.has(idx)) {
    expandedNotes.value.delete(idx)
    showZh.value.delete(idx) // 折叠时也收起翻译
  } else {
    expandedNotes.value.add(idx)
  }
}

function toggleZh(idx) {
  if (showZh.value.has(idx)) {
    showZh.value.delete(idx)
  } else {
    showZh.value.add(idx)
  }
}

function selectAnswer(qIdx, oIdx) {
  if (userAnswers.value[qIdx] != null) return // 已作答
  userAnswers.value = { ...userAnswers.value, [qIdx]: oIdx }
}

function hasAnswered(qIdx) {
  return userAnswers.value[qIdx] != null
}

function getOptionClass(qIdx, oIdx) {
  const answered = hasAnswered(qIdx)
  if (!answered) {
    return 'text-gray-700 bg-white border border-gray-200 hover:bg-blue-50 hover:border-blue-300 cursor-pointer'
  }
  if (oIdx === props.quizData.questions[qIdx].answer) {
    return 'bg-green-50 text-green-700 font-bold'
  }
  if (userAnswers.value[qIdx] === oIdx) {
    return 'bg-red-50 text-red-500'
  }
  return 'text-gray-400'
}
</script>
