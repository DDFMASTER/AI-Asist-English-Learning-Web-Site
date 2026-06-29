<template>
  <main class="max-w-[1200px] mx-auto px-6 mt-10">
    <!-- 顶部标题区 -->
    <div class="mb-10">
      <h1 class="text-3xl font-bold mb-2">智能读物匹配</h1>
      <p class="text-gray-500">
        基于你的当前水平
        <span class="font-bold text-[#2563EB]">{{ userLevel }}</span>
        ，AI 为你精准推荐
      </p>
    </div>

    <div class="grid grid-cols-12 gap-8">
      <!-- 左栏：文章列表 -->
      <div class="col-span-8">
        <!-- Tab 切换 -->
        <div class="flex p-1 bg-white rounded-xl mb-8 w-fit shadow-sm">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="px-6 py-2 rounded-lg text-sm font-medium transition-all"
            :class="activeCategory === tab.key ? 'tab-active' : 'text-gray-500 hover:bg-gray-50'"
            @click="activeCategory = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- 加载骨架屏 -->
        <div v-if="loadingArticles" class="space-y-6">
          <div v-for="n in 3" :key="'skel-'+n" class="card flex gap-6 animate-pulse">
            <div class="w-48 h-32 rounded-xl bg-gray-200 flex-none" />
            <div class="flex-1 space-y-3 py-2">
              <div class="h-4 w-20 bg-gray-200 rounded" />
              <div class="h-6 w-3/4 bg-gray-200 rounded" />
              <div class="h-4 w-full bg-gray-200 rounded" />
              <div class="h-4 w-1/3 bg-gray-200 rounded" />
            </div>
          </div>
        </div>

        <!-- 文章卡片列表 -->
        <div v-else class="space-y-6">
          <ArticleCard
            v-for="article in filteredArticles"
            :key="article.id"
            :article="article"
          />
          <div
            v-if="filteredArticles.length === 0"
            class="card text-center text-gray-400 py-12"
          >
            <Icon icon="ph:book-open-bold" class="text-4xl mb-3 opacity-30" />
            <p>暂无此类文章，敬请期待</p>
          </div>
        </div>

        <!-- 底部提示条 -->
        <div class="mt-8 p-6 bg-blue-50 rounded-2xl border border-blue-100 flex items-center gap-4">
          <div class="w-10 h-10 rounded-full bg-white flex items-center justify-center text-[#2563EB] shadow-sm">
            <Icon icon="ph:sparkle-bold" class="text-xl animate-pulse" />
          </div>
          <div class="flex-1">
            <h4 class="font-bold text-[#2563EB] text-sm">难度动态调整中</h4>
            <p class="text-[11px] text-blue-600/70">
              AI 正在根据你最近 3 天的阅读速度和查词频率调整推荐权重，当前的素材更贴合你的"舒适区+1"学习曲线。
            </p>
          </div>
        </div>
      </div>

      <!-- 右栏：边栏 -->
      <div class="col-span-4 space-y-8">
        <!-- 今日任务 -->
        <div class="card">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-bold">🎯 今日任务</h3>
            <div class="flex items-center gap-2">
              <div class="w-20 h-2 bg-gray-100 rounded-full overflow-hidden">
                <div
                  class="h-full bg-[#10B981] transition-all"
                  :style="{ width: taskProgressPercent + '%' }"
                ></div>
              </div>
              <span class="text-sm font-medium text-[#10B981]">
                {{ taskStore.todayDoneCount }}/{{ taskStore.todayTotalCount }}
              </span>
            </div>
          </div>
          <div class="space-y-3">
            <div
              v-for="task in taskStore.todayTasks"
              :key="task.id"
              class="flex items-center gap-3 text-sm"
            >
              <span
                v-if="task.done"
                class="w-5 h-5 rounded bg-[#10B981] flex items-center justify-center flex-none"
              >
                <Icon icon="ph:check-bold" class="text-white text-[10px]" />
              </span>
              <span v-else class="w-5 h-5 rounded border-2 border-gray-200 flex-none"></span>
              <span :class="{ 'text-gray-500 line-through': task.done }">{{ task.name }}</span>
            </div>
          </div>
        </div>

        <!-- 浏览历史 -->
        <div class="card">
          <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
            <Icon icon="ph:history-bold" class="text-[#2563EB]" />
            浏览历史
          </h3>
          <div class="space-y-5">
            <div
              v-for="item in historyItems"
              :key="item.id"
              class="flex gap-3 cursor-pointer group"
              @click="goToReader(item.articleId)"
            >
              <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-blue-100 to-purple-100 flex items-center justify-center flex-none">
                <Icon :icon="item.icon" class="text-lg opacity-50" :class="item.iconColor" />
              </div>
              <div class="flex-1 min-w-0">
                <h4 class="text-sm font-bold truncate group-hover:text-[#2563EB] transition-colors">
                  {{ item.title }}
                </h4>
                <div class="flex items-center gap-2 mt-1">
                  <span :class="item.statusClass">{{ item.status }}</span>
                  <span class="text-[10px] text-gray-400">{{ item.time }}</span>
                </div>
              </div>
            </div>
            <div v-if="historyItems.length === 0" class="text-center text-gray-400 text-xs py-4">
              暂无浏览记录
            </div>
          </div>
          <button
            class="w-full py-3 mt-5 border border-gray-100 rounded-xl text-xs font-bold text-gray-500 hover:bg-gray-50 transition-all"
            @click="guard(loadMoreHistory)"
          >
            查看全部历史
          </button>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useTaskStore } from '@/stores/task'
import { useUserStore } from '@/stores/user'
import ArticleCard from '@/components/ArticleCard.vue'
import request from '@/utils/request'
import { getRecentHistory, relativeTime } from '@/utils/historyDB'
import { useRequireAuth } from '@/composables/useAuth'

const router = useRouter()
const taskStore = useTaskStore()
const userStore = useUserStore()
const { guard } = useRequireAuth()

// Tab 状态
const tabs = [
  { key: 'advanced', label: '进阶类 (期刊/原著)' },
  { key: 'exam', label: '应试类 (考研/四六级)' },
  { key: 'basic', label: '基础类 (日常/故事)' },
]
const activeCategory = ref('advanced')

// 文章数据
const articles = ref([])
const loadingArticles = ref(true)
const historyItems = ref([])

// 用户水平
const userLevel = computed(() => {
  return userStore.user?.study_purpose || 'B1 · 中级'
})

// Task 进度
const taskProgressPercent = computed(() => {
  if (taskStore.todayTotalCount === 0) return 0
  return Math.round((taskStore.todayDoneCount / taskStore.todayTotalCount) * 100)
})

/**
 * 难度 → 分类映射
 *   进阶类: 托福
 *   应试类: 考研, 四级, 六级
 *   基础类: 初中, 高中
 */
const DIFFICULTY_CATEGORY = {
  '托福': 'advanced',
  '考研': 'exam',
  '四级': 'exam',
  '六级': 'exam',
  '初中': 'basic',
  '高中': 'basic',
}

/**
 * 难度 → 显示简称（用于卡片徽章）
 */
const DIFFICULTY_LABEL = {
  '初中': '初中',
  '高中': '高中',
  '四级': 'CET-4',
  '六级': 'CET-6',
  '考研': '考研',
  '托福': 'TOEFL',
}

// 按分类过滤文章
const filteredArticles = computed(() => {
  if (articles.value.length === 0) return articles.value
  return articles.value.filter(a => a.category === activeCategory.value)
})

// 跳转到阅读器
function goToReader(articleId) {
  router.push(`/reader?id=${articleId}`)
}

// 加载更多历史
function loadMoreHistory() {
  // TODO: 加载更多或跳转历史页
}

/**
 * 将 API 返回的文章数据映射为 ArticleCard 所需格式
 */
function mapArticle(raw) {
  const difficulty = raw.difficulty || '四级'
  const category = DIFFICULTY_CATEGORY[difficulty] || 'basic'
  const vocquizNum = raw.vocquizNum || 0

  return {
    id: raw.articleId,
    article_id: raw.articleId,
    title: raw.title || 'Untitled',
    source: raw.source || '未知来源',
    difficulty: DIFFICULTY_LABEL[difficulty] || difficulty,
    category,
    readTime: (raw.readTime || 5) + ' min read',
    wordCount: (raw.wordCount || 500) + ' words',
    newWords: vocquizNum > 0 ? vocquizNum + ' 道词汇题' : '暂无题目',
    abstract: '难度: ' + (DIFFICULTY_LABEL[difficulty] || difficulty)
            + ' · 来源: ' + (raw.source || '未知')
            + (vocquizNum > 0 ? ' · 含 ' + vocquizNum + ' 道词汇题' : ''),
    articleLikeCount: raw.articleLikeCount || 0,
  }
}

/**
 * 数组洗牌
 */
function shuffle(arr) {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

/**
 * 每类随机抽取最多 N 篇
 */
function pickPerCategory(all, perCategory = 3) {
  const groups = { advanced: [], exam: [], basic: [] }
  all.forEach(a => {
    const cat = a.category || 'basic'
    if (groups[cat]) groups[cat].push(a)
  })
  const picked = []
  for (const cat of ['advanced', 'exam', 'basic']) {
    picked.push(...shuffle(groups[cat]).slice(0, perCategory))
  }
  return picked
}

// 获取文章列表（骨架屏占位 → API 响应 → 展示真实数据）
async function fetchArticles() {
  loadingArticles.value = true
  try {
    const data = await request.get('/article/list')
    if (data.success && Array.isArray(data.articles)) {
      const mapped = data.articles.map(mapArticle)
      articles.value = pickPerCategory(mapped, 3)
    }
  } catch (err) {
    console.warn('从后端获取文章失败:', err.message)
  } finally {
    loadingArticles.value = false
  }
}

/** 历史记录图标列表（用于前 3 条做视觉区分） */
const HISTORY_ICONS = [
  { icon: 'ph:book-open-bold', color: 'text-[#2563EB]' },
  { icon: 'ph:bookmark-simple-bold', color: 'text-green-500' },
  { icon: 'ph:books-bold', color: 'text-yellow-500' },
]

// 获取浏览历史（从 IndexedDB 读取最近 3 条）
async function fetchHistory() {
  try {
    const records = await getRecentHistory(3)
    console.log('[MaterialsPage] 读取到浏览历史:', records.length, '条', records)
    historyItems.value = records.map((r, i) => {
      const iconCfg = HISTORY_ICONS[i] || HISTORY_ICONS[HISTORY_ICONS.length - 1]
      return {
        id: r.articleId,
        articleId: r.articleId,
        title: r.title,
        icon: iconCfg.icon,
        iconColor: iconCfg.color,
        status: '已读',
        statusClass: 'text-[10px] text-green-600 font-bold bg-green-50 px-1 rounded',
        time: relativeTime(r.visitedAt),
      }
    })
  } catch (err) {
    console.error('读取浏览历史失败:', err)
    historyItems.value = []
  }
}

onMounted(() => {
  // 所有数据并行加载，不阻塞页面渲染
  fetchArticles()
  taskStore.initDailyTasks()
  fetchHistory()
})
</script>
