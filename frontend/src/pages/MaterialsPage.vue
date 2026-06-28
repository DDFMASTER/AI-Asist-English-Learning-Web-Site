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

        <!-- 文章卡片列表 -->
        <div class="space-y-6">
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
          <router-link
            to="/tasks"
            class="block w-full text-center mt-4 py-2 border border-gray-100 rounded-xl text-xs font-bold text-gray-500 hover:bg-gray-50 transition-all"
          >
            查看全部 →
          </router-link>
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
              @click="guard(() => goToReader(item.articleId))"
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

// 获取文章列表
async function fetchArticles() {
  try {
    const data = await request.get('/article/list')
    if (data.success && Array.isArray(data.articles)) {
      const mapped = data.articles.map(mapArticle)
      articles.value = pickPerCategory(mapped, 3)
      return
    }
  } catch (err) {
    console.warn('从后端获取文章失败，使用 mock 数据:', err.message)
  }

  // 后端未就绪时使用 mock 数据（每类 ≥3 篇）
  articles.value = [
    // ===== 进阶类 (advanced) =====
    {
      id: 1, article_id: 1,
      title: 'The Green Hydrogen Revolution: Hope or Hype?',
      source: 'The Economist',
      difficulty: 'TOEFL',
      category: 'advanced',
      readTime: '12 min read', wordCount: '850 words', newWords: '12 道词汇题',
      abstract: '氢能被视为实现零排放的关键，但高昂的成本与技术难题依然是其商业化道路上的巨大障碍...',
    },
    {
      id: 10, article_id: 10,
      title: 'The Ethics of Artificial Intelligence in Healthcare',
      source: 'Scientific American',
      difficulty: 'TOEFL',
      category: 'advanced',
      readTime: '14 min read', wordCount: '1100 words', newWords: '18 道词汇题',
      abstract: 'AI 在医疗领域的应用引发了关于隐私、偏见和责任的深刻伦理讨论...',
    },
    {
      id: 11, article_id: 11,
      title: 'Global Trade in the Post-Pandemic Era',
      source: 'Financial Times',
      difficulty: 'TOEFL',
      category: 'advanced',
      readTime: '10 min read', wordCount: '780 words', newWords: '9 道词汇题',
      abstract: '疫情后的全球贸易格局变化，供应链重组与区域化趋势分析...',
    },
    // ===== 应试类 (exam) =====
    {
      id: 2, article_id: 2,
      title: 'The Architecture of Silence: Modern Minimalism',
      source: 'The New Yorker',
      difficulty: '考研',
      category: 'exam',
      readTime: '15 min read', wordCount: '1200 words', newWords: '24 道词汇题',
      abstract: '探讨现代主义建筑中"留白"的哲学意义...',
    },
    {
      id: 3, article_id: 3,
      title: '考研英语阅读精选: Education Reform',
      source: 'China Daily',
      difficulty: '考研',
      category: 'exam',
      readTime: '10 min read', wordCount: '720 words', newWords: '8 道词汇题',
      abstract: '教育改革的深入探讨，涵盖政策变化与教学方法创新的最新趋势...',
    },
    {
      id: 7, article_id: 7,
      title: '考研英语: Climate Policy and Economics',
      source: '经济学人',
      difficulty: '考研',
      category: 'exam',
      readTime: '13 min read', wordCount: '950 words', newWords: '20 道词汇题',
      abstract: '气候政策的经济影响分析，考研阅读常见话题...',
    },
    {
      id: 4, article_id: 4,
      title: 'CET-4 Reading: Campus Life',
      source: '英语周报',
      difficulty: 'CET-4',
      category: 'exam',
      readTime: '8 min read', wordCount: '540 words', newWords: '5 道词汇题',
      abstract: '大学校园生活的方方面面，帮助学生熟悉四级阅读常见话题...',
    },
    {
      id: 5, article_id: 5,
      title: 'CET-6: Technology and Society',
      source: 'The Guardian',
      difficulty: 'CET-6',
      category: 'exam',
      readTime: '11 min read', wordCount: '900 words', newWords: '15 道词汇题',
      abstract: '科技对社会的影响，包含六级常考的逻辑推理和态度判断题型...',
    },
    {
      id: 8, article_id: 8,
      title: 'CET-4: Environmental Protection',
      source: 'BBC Learning',
      difficulty: 'CET-4',
      category: 'exam',
      readTime: '7 min read', wordCount: '480 words', newWords: '6 道词汇题',
      abstract: '环境保护与可持续发展的英语短文，四级难度...',
    },
    // ===== 基础类 (basic) =====
    {
      id: 6, article_id: 6,
      title: 'Why Deep Sleep is Critical for Memory',
      source: 'Science Daily',
      difficulty: '高中',
      category: 'basic',
      readTime: '8 min read', wordCount: '540 words', newWords: '暂无题目',
      abstract: '最新的神经科学研究表明，深度睡眠阶段是大脑进行记忆巩固的关键时期...',
    },
    {
      id: 7, article_id: 7,
      title: 'My Daily Routine — A Junior High Reading',
      source: 'English Club',
      difficulty: '初中',
      category: 'basic',
      readTime: '5 min read', wordCount: '320 words', newWords: '暂无题目',
      abstract: '以日常作息为主题的英语短文，适合初中阶段学习者阅读练习...',
    },
    {
      id: 9, article_id: 9,
      title: '高中英语阅读: Cultural Differences',
      source: 'English Weekly',
      difficulty: '高中',
      category: 'basic',
      readTime: '9 min read', wordCount: '600 words', newWords: '暂无题目',
      abstract: '中西方文化差异的英语文章，适合高中阶段学习者提升阅读理解能力...',
    },
  ]
}

// 获取浏览历史
function fetchHistory() {
  // Mock 数据
  historyItems.value = [
    {
      id: 1,
      articleId: 1,
      title: 'The Rise of Remote Work',
      icon: 'ph:briefcase-bold',
      iconColor: 'text-[#2563EB]',
      status: '100% 已读',
      statusClass: 'text-[10px] text-green-600 font-bold bg-green-50 px-1 rounded',
      time: '2天前',
    },
    {
      id: 2,
      articleId: 2,
      title: 'Urban Farming in Singapore',
      icon: 'ph:tree-bold',
      iconColor: 'text-green-500',
      status: '45% 在读',
      statusClass: 'text-[10px] text-blue-600 font-bold bg-blue-50 px-1 rounded',
      time: '昨天',
    },
    {
      id: 3,
      articleId: 3,
      title: 'History of Jazz Music',
      icon: 'ph:music-notes-bold',
      iconColor: 'text-yellow-500',
      status: '100% 已读',
      statusClass: 'text-[10px] text-green-600 font-bold bg-green-50 px-1 rounded',
      time: '上周',
    },
  ]
}

onMounted(async () => {
  fetchArticles()
  await taskStore.initDailyTasks()
  fetchHistory()
})
</script>
