<template>
  <div class="min-h-screen">
    <!-- 沉浸式导航栏 -->
    <nav class="sticky top-0 z-50 bg-white/60 backdrop-blur-md border-b border-gray-100">
      <div class="max-w-[1200px] mx-auto px-6 h-16 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <router-link to="/materials" class="p-2 hover:bg-gray-100 rounded-full transition-colors">
            <Icon icon="ph:arrow-left-bold" class="text-xl" />
          </router-link>
          <div class="h-6 w-px bg-gray-200"></div>
          <h2 class="font-bold truncate max-w-[400px]">{{ readerStore.articleTitle }}</h2>
        </div>
        <div class="flex items-center gap-3">
          <!-- 字体大小 -->
          <button class="p-2 text-gray-400 hover:text-gray-600" @click="guard(() => readerStore.adjustFontSize(2))">
            <Icon icon="ph:text-t-bold" class="text-2xl" />
          </button>
          <!-- 书签 -->
          <button class="p-2 text-gray-400 hover:text-gray-600" @click="guard(handleBookmark)">
            <Icon icon="ph:bookmark-simple-bold" class="text-2xl" />
          </button>
        </div>
      </div>
    </nav>

    <!-- 阅读区域 -->
    <main class="reader-container" style="max-width:800px;margin:0 auto" v-if="readerStore.article">
      <header class="mb-12">
        <!-- 分类/阅读时间/难度（无数据时不展示） -->
        <div
          v-if="readerStore.article.difficulty || readerStore.article.readTime"
          class="flex items-center gap-2 text-sm text-gray-400 font-medium mb-4"
        >
          <template v-if="readerStore.article.difficulty">
            <span class="text-[#2563EB]">{{ readerStore.article.difficulty }}</span>
            <span v-if="readerStore.article.readTime">•</span>
          </template>
          <span v-if="readerStore.article.readTime">{{ readerStore.article.readTime }}</span>
        </div>

        <!-- 标题（无数据时不展示） -->
        <h1
          v-if="readerStore.article.title"
          class="text-4xl font-bold leading-tight mb-8"
        >{{ readerStore.article.title }}</h1>

        <!-- 作者信息（无数据时不展示） -->
        <div v-if="readerStore.article.author" class="flex items-center gap-3 mb-12">
          <div class="w-10 h-10 rounded-full bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white text-xs font-bold flex-none">
            {{ (readerStore.article.authorAvatar || readerStore.article.author).charAt(0).toUpperCase() }}
          </div>
          <div>
            <div class="text-sm font-bold">{{ readerStore.article.author }}</div>
            <div v-if="readerStore.article.publishDate" class="text-xs text-gray-400">
              Published on {{ readerStore.article.publishDate }}
            </div>
          </div>
        </div>
      </header>

      <article id="articleContent">
        <!-- 文章段落（按 \\n\\n 分割） -->
        <template v-for="(paragraph, pIdx) in paragraphs" :key="pIdx">
          <p class="article-p" :style="{ fontSize: readerStore.fontSize + 'px' }">
            <!-- 段落内混合文本、交互单词、文化注解 -->
            <template v-for="(segment, sIdx) in paragraph.segments" :key="sIdx">
              <span v-if="segment.type === 'text'">{{ segment.text }}</span>
              <span
                v-else-if="segment.type === 'word'"
                class="interactive-word"
                @click="(e) => handleWordClick(segment.data, e)"
              >
                {{ segment.data.word }}
              </span>
              <span
                v-else-if="segment.type === 'culture'"
                class="culture-mark"
                @click="(e) => handleCultureClick(segment.data, e)"
              >
                ★
              </span>
            </template>
          </p>

          <!-- 苏格拉底式提问卡片（在第二个段落后） -->
          <div
            v-if="pIdx === 1 && readerStore.article.socraticQuestion"
            class="my-12 socratic-card border-l-4 border-l-[#2563EB]"
          >
            <div class="flex items-start gap-4">
              <div class="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center text-[#2563EB] flex-none">
                <Icon icon="ph:chats-circle-bold" class="text-2xl" />
              </div>
              <div>
                <h4 class="text-lg font-bold mb-3">🤔 AI 深度思考</h4>
                <p class="text-gray-600 leading-relaxed mb-6">
                  {{ readerStore.article.socraticQuestion }}
                </p>
                <div class="flex items-center gap-3">
                  <button
                    class="px-6 py-2.5 bg-[#2563EB] text-white rounded-xl text-sm font-bold shadow-lg shadow-blue-200 hover:bg-blue-600 transition-all"
                    @click="guard(() => { showAnswerInput = true })"
                  >
                    写下你的想法
                  </button>
                  <button
                    class="px-6 py-2.5 bg-gray-50 text-gray-500 rounded-xl text-sm font-bold hover:bg-gray-100 transition-all"
                    @click="guard(skipQuestion)"
                  >
                    稍后回答
                  </button>
                </div>

                <!-- 回答输入框 -->
                <div v-if="showAnswerInput" class="mt-4">
                  <textarea
                    v-model="answerText"
                    class="w-full h-24 p-3 border border-gray-200 rounded-xl text-sm resize-none focus:outline-none focus:border-[#2563EB]"
                    placeholder="写下你的想法..."
                  ></textarea>
                  <button
                    class="mt-2 px-4 py-2 bg-[#2563EB] text-white rounded-lg text-xs font-bold"
                    @click="guard(submitAnswer)"
                  >
                    提交
                  </button>
                </div>
              </div>
            </div>
          </div>
        </template>
      </article>

      <!-- 底部留白，防止被操作栏遮挡 -->
      <div class="h-28"></div>
    </main>

    <!-- 加载中 -->
    <div v-else class="text-center py-40">
      <Icon icon="ph:spinner-bold" class="text-4xl text-[#2563EB] animate-spin mx-auto mb-4" />
      <p class="text-gray-400">加载文章中...</p>
    </div>

    <!-- 底部固定操作栏 -->
    <div class="fixed bottom-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-md border-t border-gray-100">
      <div class="max-w-[800px] mx-auto px-6 h-16 flex items-center justify-between">
        <!-- 进度条 -->
        <div class="flex items-center gap-3 flex-1 mr-8">
          <span class="text-xs font-bold text-gray-400 flex-none">{{ readerStore.readingProgress }}%</span>
          <div class="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
            <div
              class="h-full bg-[#2563EB] transition-all"
              :style="{ width: readerStore.readingProgress + '%' }"
            ></div>
          </div>
        </div>

        <!-- 对照翻译按钮 -->
        <button
          class="flex items-center gap-2 px-5 py-2 rounded-full text-sm font-bold transition-all flex-none"
          :class="readerStore.showTranslation
            ? 'bg-[#2563EB] text-white shadow-lg shadow-blue-200'
            : 'bg-blue-50 text-[#2563EB] hover:bg-blue-100'"
          @click="guard(readerStore.toggleTranslation)"
        >
          <Icon icon="ph:translate-bold" />
          对照翻译
        </button>
      </div>
    </div>

    <!-- 单词查词浮窗 -->
    <WordPopover
      :visible="wordPopover.visible"
      :word="wordPopover.word"
      :loading="wordPopover.loading"
      :position="wordPopover.position"
      @close="wordPopover.visible = false"
      @add-vocab="onAddVocab"
    />

    <!-- 文化注解浮窗 -->
    <CulturePopover
      :visible="culturePopover.visible"
      :title="culturePopover.title"
      :content="culturePopover.content"
      :position="culturePopover.position"
    />
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useReaderStore } from '@/stores/reader'
import WordPopover from '@/components/WordPopover.vue'
import CulturePopover from '@/components/CulturePopover.vue'
import { useRequireAuth } from '@/composables/useAuth'

const route = useRoute()
const router = useRouter()
const readerStore = useReaderStore()
const { guard } = useRequireAuth()

// ========== 单词分词 ==========
function tokenize(text) {
  const segments = []
  // 匹配英文单词（支持缩写如 don't, it's 和复合词如 well-known）
  const wordRe = /([a-zA-Z]+(?:['-][a-zA-Z]+)*)/g
  let lastIdx = 0
  let match

  while ((match = wordRe.exec(text)) !== null) {
    // 单词之前的纯文本（标点、空格等）
    if (match.index > lastIdx) {
      segments.push({ type: 'text', text: text.slice(lastIdx, match.index) })
    }
    // 单词本身
    segments.push({
      type: 'word',
      text: match[0],
      data: { word: match[0] },
    })
    lastIdx = match.index + match[0].length
  }

  // 末尾剩余文本
  if (lastIdx < text.length) {
    segments.push({ type: 'text', text: text.slice(lastIdx) })
  }

  return segments
}

// ========== 段落解析 ==========
const paragraphs = computed(() => {
  if (!readerStore.article?.content) return []
  let text = readerStore.article.content

  // 1. 统一换行符
  text = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n')

  // 2. 策略A：按空行（连续换行）分段
  let parts = text.split(/\n\s*\n/)

  // 3. 策略B：如果策略A只产出1段，尝试按单换行分段
  if (parts.length <= 1) {
    parts = text.split(/\n+/)
  }

  // 4. 策略C：如果还是只有1段且内容很长，按句号+空格强制断句
  parts = parts.flatMap(p => {
    const trimmed = p.trim()
    if (!trimmed) return []
    // 超过500字符的段落，在句号处断句
    if (trimmed.length > 500) {
      const sentences = trimmed.split(/(?<=[.!?])\s+(?=[A-Z])/)
      if (sentences.length > 1) {
        // 每2-3句合并为一段
        const groups = []
        for (let i = 0; i < sentences.length; i += 3) {
          groups.push(sentences.slice(i, i + 3).join(' '))
        }
        return groups
      }
    }
    return [trimmed]
  })

  return parts
    .map(p => p.trim())
    .filter(p => p.length > 0)
    .map(paraText => ({
      segments: tokenize(paraText)
    }))
})

// ========== 浮窗状态 ==========
const wordPopover = reactive({
  visible: false,
  loading: false,
  word: { word: '', results: [] },
  position: { x: 0, y: 0 },
})

const culturePopover = reactive({
  visible: false,
  title: '',
  content: '',
  position: { x: 0, y: 0 },
})

// ========== 苏格拉底提问 ==========
const showAnswerInput = ref(false)
const answerText = ref('')

// ========== 目录和设置面板 ==========
const showToc = ref(false)
const showSettings = ref(false)

// ========== 单词点击 ==========
function handleWordClick(wordData, event) {
  event.stopPropagation()
  const rect = event.target.getBoundingClientRect()

  // 立即显示弹窗（加载状态）
  wordPopover.word = { word: wordData.word, results: [] }
  wordPopover.loading = true
  wordPopover.position = {
    x: Math.min(rect.left + rect.width / 2 - 160, window.innerWidth - 340),
    y: Math.max(rect.top - 200, 80),
  }
  wordPopover.visible = true
  culturePopover.visible = false

  // 异步查询
  lookupWordForPopover(wordData.word)
}

async function lookupWordForPopover(word) {
  const result = await readerStore.lookupWord(word)

  // 竞态保护：弹窗已被其他单词覆盖则忽略
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
  } else {
    wordPopover.word = {
      word: word,
      results: [],
      found: false,
      error: result.message || '查询失败',
    }
  }
  wordPopover.loading = false
}

// ========== 文化注解点击 ==========
function handleCultureClick(data, event) {
  event.stopPropagation()
  const rect = event.target.getBoundingClientRect()
  culturePopover.title = data.title
  culturePopover.content = data.content
  culturePopover.position = {
    x: Math.max(rect.left - 150, 10),
    y: Math.min(rect.top + 30, window.innerHeight - 200),
  }
  culturePopover.visible = true
  wordPopover.visible = false
}

// ========== 其他操作 ==========
function handleBookmark() {
  readerStore.addBookmark(readerStore.readingProgress)
  alert('已添加书签')
}

function handleTTS() {
  if ('speechSynthesis' in window) {
    const utterance = new SpeechSynthesisUtterance(readerStore.article?.content?.slice(0, 500))
    utterance.lang = 'en-US'
    speechSynthesis.speak(utterance)
  } else {
    alert('您的浏览器不支持语音朗读')
  }
}

function scrollToNextParagraph() {
  // 更新进度
  readerStore.updateProgress(readerStore.readingProgress + 15)
  window.scrollBy({ top: 400, behavior: 'smooth' })
}

function skipQuestion() {
  showAnswerInput.value = false
}

function submitAnswer() {
  // TODO: 提交回答到后端
  alert('你的想法已记录！')
  answerText.value = ''
  showAnswerInput.value = false
}

function onAddVocab(word) {
  // 已在 store 中处理
}

// ========== 全局点击关闭浮窗 ==========
function handleGlobalClick() {
  wordPopover.visible = false
  culturePopover.visible = false
  showToc.value = false
  showSettings.value = false
}

// ========== 滚动监听更新进度 ==========
function handleScroll() {
  const scrollTop = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  if (docHeight > 0) {
    readerStore.updateProgress(Math.round((scrollTop / docHeight) * 100))
  }
}

onMounted(() => {
  const articleId = route.query.id
  readerStore.fetchArticle(articleId)
  document.addEventListener('click', handleGlobalClick)
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClick)
  window.removeEventListener('scroll', handleScroll)
})
</script>
