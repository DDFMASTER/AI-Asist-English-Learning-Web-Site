import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { lemmatize } from '@/utils/lemmatize'

export const useReaderStore = defineStore('reader', () => {
  // ========== 状态 ==========
  const article = ref(null)
  const readingProgress = ref(0)    // 0-100
  const fontSize = ref(18)
  const showTranslation = ref(false)
  const bookmarks = ref([])
  const loading = ref(false)

  // ========== AI 例句缓存 ==========
  /** @type {import('vue').Ref<Record<string, {examples: Array<{en:string,zh:string}>, loading: boolean}>>} */
  const aiExampleCache = ref({})
  const aiExampleLoading = ref(false)

  // ========== AI 文化背景 & 选择题缓存 ==========
  const culturalNotesCache = ref(null)  // { notes: [], loading: bool, error: str }
  const quizCache = ref(null)           // { questions: [], loading: bool, error: str }

  // ========== 段落翻译缓存 ==========
  /** @type {import('vue').Ref<Record<string, {zh: string, loading: boolean, error?: string}>>} */
  const paragraphTranslations = ref({})
  const translatingIndex = ref(-1)

  // ========== 计算属性 ==========
  const articleTitle = computed(() => article.value?.title || '加载中...')
  const articleContent = computed(() => article.value?.content || '')

  // ========== 动作 ==========

  /**
   * 获取文章详情（优先走后端 API，失败时回退 mock）
   */
  async function fetchArticle(articleId) {
    if (!articleId) return
    loading.value = true
    try {
      const data = await request.get('/article/detail', {
        params: { id: articleId }
      })
      if (data.success && data.article) {
        article.value = mapArticle(data.article)
        return
      }
    } catch (err) {
      console.warn('从后端获取文章详情失败:', err.message)
    } finally {
      loading.value = false
    }

    // 未拿到数据时，从列表页 mock 中取（MaterialsPage 也用的 mock）
    article.value = getMockArticle(articleId)
  }

  /**
   * 将 API 返回的文章字段映射为 reader 所需格式
   */
  function mapArticle(raw) {
    return {
      id: raw.articleId,
      title: raw.title || '',
      content: raw.content || '',
      source: raw.source || '',
      difficulty: raw.difficulty || '',
      wordCount: raw.wordCount || 0,
      readTime: (raw.readTime || 5) + ' min read',
      category: raw.source || '',
    }
  }

  /**
   * 后备 mock 数据（后端不可用时使用）
   */
  function getMockArticle(id) {
    const mockArticles = {
      1: {
        id: 1,
        title: 'The Green Hydrogen Revolution: Hope or Hype?',
        content: 'The concept of green hydrogen has captured the imagination of policymakers and investors alike. Produced by splitting water molecules using renewable electricity, hydrogen offers a carbon-free fuel for industries that are notoriously difficult to decarbonize — from steelmaking to long-haul aviation.\n\nHowever, the economics remain challenging. Electrolyzers are expensive, and the round-trip efficiency of converting electricity to hydrogen and back is only about 30%. Critics argue that in many applications, direct electrification is far more efficient.\n\nDespite these hurdles, governments around the world are betting big. The European Union has pledged billions in subsidies, and China is rapidly scaling up its electrolyzer manufacturing capacity.',
        source: 'The Economist',
        difficulty: 'TOEFL',
        readTime: '8 min read',
      },
      2: {
        id: 2,
        title: 'The Architecture of Silence: Modern Minimalism',
        content: 'Minimalism in architecture is more than an aesthetic choice — it represents a philosophical stance against the noise and clutter of contemporary life. By stripping away the non-essential, minimalist spaces invite contemplation and offer a rare commodity in the modern world: silence.\n\nArchitects like Tadao Ando and John Pawson have demonstrated that empty space can be profoundly expressive. Ando\'s Church of the Light uses a simple concrete box with a cruciform cutout to create one of the most spiritually powerful spaces of the twentieth century.',
        source: 'The New Yorker',
        difficulty: '考研',
        readTime: '10 min read',
      },
      3: {
        id: 3,
        title: '考研英语阅读精选: Education Reform',
        content: 'Education reform has become a central topic in policy debates across the globe. As economies shift toward knowledge-based models, the traditional classroom — with its rows of desks and standardized curricula — appears increasingly outdated.\n\nFinland\'s approach offers a compelling alternative. Rather than emphasizing standardized testing, Finnish schools prioritize student well-being, teacher autonomy, and interdisciplinary learning. The results speak for themselves: Finnish students consistently rank among the world\'s top performers.',
        source: 'China Daily',
        difficulty: '考研',
        readTime: '7 min read',
      },
      4: {
        id: 4,
        title: 'CET-4 Reading: Campus Life',
        content: 'College life is a time of transition and growth. For many students, it represents their first experience living away from home, managing their own schedules, and taking full responsibility for their learning.\n\nTime management is often cited as the most critical skill for academic success. Studies show that students who maintain a consistent study schedule perform significantly better than those who cram before exams.',
        source: '英语周报',
        difficulty: 'CET-4',
        readTime: '5 min read',
      },
    }

    return mockArticles[id] || {
      id,
      title: '',
      content: '文章内容加载失败，请返回重试。',
      source: '',
      difficulty: '',
      readTime: '',
    }
  }

  /**
   * 更新阅读进度
   */
  function updateProgress(progress) {
    readingProgress.value = Math.min(100, Math.max(0, progress))
  }

  /**
   * 切换翻译显示（段落翻译模式）
   */
  function toggleTranslation() {
    showTranslation.value = !showTranslation.value
    // 切换模式时清除当前翻译面板
    if (!showTranslation.value) {
      translatingIndex.value = -1
    }
  }

  /**
   * 获取段落翻译（优先从缓存取，否则调用 AI）
   * @param {string|number} articleId
   * @param {number} pIdx 段落索引
   * @param {string} paragraphText 段落原文
   */
  async function fetchParagraphTranslation(articleId, pIdx, paragraphText) {
    const key = `${articleId}_${pIdx}`
    // 已缓存则直接返回
    if (paragraphTranslations.value[key]) return

    // 标记为加载中
    paragraphTranslations.value[key] = { zh: '', loading: true }
    translatingIndex.value = pIdx

    try {
      const data = await request.post('/article/translate-paragraph', {
        paragraph: paragraphText,
      }, {
        timeout: 25000, // AI 翻译较慢，给 25 秒
      })
      if (data.success && data.translation) {
        paragraphTranslations.value[key] = {
          zh: data.translation,
          loading: false,
        }
      } else {
        paragraphTranslations.value[key] = {
          zh: '',
          loading: false,
          error: data.message || '翻译失败',
        }
      }
    } catch (err) {
      console.error('段落翻译失败:', err)
      paragraphTranslations.value[key] = {
        zh: '',
        loading: false,
        error: '翻译失败，请稍后重试',
      }
    } finally {
      if (translatingIndex.value === pIdx) {
        translatingIndex.value = -1
      }
    }
  }

  /**
   * 清除当前段落翻译显示
   */
  function clearParagraphTranslation() {
    translatingIndex.value = -1
  }

  /**
   * 调整字体大小
   */
  function adjustFontSize(delta) {
    fontSize.value = Math.min(24, Math.max(14, fontSize.value + delta))
  }

  /**
   * 添加书签
   */
  function addBookmark(position) {
    if (!bookmarks.value.includes(position)) {
      bookmarks.value.push(position)
    }
    // TODO: 调用后端 API 保存
  }

  /**
   * 移除书签
   */
  function removeBookmark(position) {
    bookmarks.value = bookmarks.value.filter(b => b !== position)
  }

  /**
   * 跨全部词库查词，支持词形还原。
   * 先精确匹配，未命中则对单词做 lemmatize 后用候选原形再查。
   * @param {string} word 要查询的单词
   * @param {string} [studyPurpose] 用户学习阶段，用于过滤词书结果
   */
  async function lookupWord(word, studyPurpose) {
    const w = word.toLowerCase().trim()

    // 1. 精确匹配：先查后端，再查本地
    const exact = await tryLookup(w, studyPurpose)
    if (exact && exact.found) return exact

    // 2. 词形还原：生成候选原形列表，逐个尝试
    const candidates = lemmatize(w)
    // 跳过第一个（就是原词本身，已经精确匹配过了）
    for (let i = 1; i < candidates.length; i++) {
      const candidate = candidates[i]
      if (candidate === w) continue
      const result = await tryLookup(candidate, studyPurpose)
      if (result && result.found) {
        // 标注是通过词形还原找到的
        result.lemmaFrom = w
        result.lemmaTo = candidate
        return result
      }
    }

    // 3. 全部未命中
    return { success: true, word: w, found: false, results: [] }
  }

  /** 尝试查词：先后端 API，再本地 mock */
  async function tryLookup(w, studyPurpose) {
    try {
      const params = { word: w }
      if (studyPurpose) {
        params.studyPurpose = studyPurpose
      }
      const data = await request.get('/word/lookup', { params })
      if (data.success) return data
    } catch (error) {
      // 后端不可用，继续尝试本地
    }
    return getMockWordResult(w, studyPurpose)
  }

  /** 本地 mock 词库，后端不可用时的后备 */
  function getMockWordResult(word, studyPurpose) {
    const MOCK_DICT = {
      'hydrogen': { phonetic: '/ˈhaɪdrədʒən/', translations: { '四级': ['氢'], '六级': ['氢'], '考研': ['氢'], '托福': ['氢'] } },
      'revolution': { phonetic: '/ˌrevəˈluːʃn/', translations: { '初中': ['革命'], '高中': ['革命'], '四级': ['革命；重大变革'], '六级': ['革命；旋转'], '考研': ['革命；变革'], '托福': ['revolution'] } },
      'imagination': { phonetic: '/ɪˌmædʒɪˈneɪʃn/', translations: { '四级': ['想象力；想象'], '六级': ['想象力；创造力'], '考研': ['想象；想象力'] } },
      'policymakers': { phonetic: '', translations: { '托福': ['政策制定者'] } },
      'investors': { phonetic: '', translations: { '四级': ['投资者'], '六级': ['投资者'], '考研': ['投资者'], '托福': ['投资者'] } },
      'renewable': { phonetic: '/rɪˈnjuːəbl/', translations: { '四级': ['可再生的'], '六级': ['可再生的；可更新的'], '考研': ['可再生的'], '托福': ['可再生的；可延期的'] } },
      'electricity': { phonetic: '/ɪˌlekˈtrɪsəti/', translations: { '初中': ['电；电力'], '高中': ['电；电力'], '四级': ['电；电力；电流'], '六级': ['电力；电学'] } },
      'carbon': { phonetic: '/ˈkɑːrbən/', translations: { '高中': ['碳'], '四级': ['碳'], '六级': ['碳'], '考研': ['碳'], '托福': ['碳'] } },
      'industries': { phonetic: '', translations: { '初中': ['工业；产业'], '高中': ['工业；产业'], '四级': ['工业；行业'], '六级': ['产业；工业'] } },
      'decarbonize': { phonetic: '', translations: { '托福': ['脱碳；去碳化'] } },
      'steelmaking': { phonetic: '', translations: { '考研': ['炼钢'], '托福': ['炼钢'] } },
      'aviation': { phonetic: '/ˌeɪviˈeɪʃn/', translations: { '六级': ['航空；飞行'], '考研': ['航空；飞机制造业'], '托福': ['航空；飞行'] } },
      'economics': { phonetic: '/ˌiːkəˈnɒmɪks/', translations: { '高中': ['经济学'], '四级': ['经济学'], '六级': ['经济学'], '考研': ['经济学'], '托福': ['经济学'] } },
      'challenging': { phonetic: '/ˈtʃælɪndʒɪŋ/', translations: { '初中': ['有挑战性的'], '高中': ['有挑战性的'], '四级': ['有挑战性的；困难的'], '六级': ['挑战性的'] } },
      'efficiency': { phonetic: '/ɪˈfɪʃnsi/', translations: { '高中': ['效率'], '四级': ['效率；效能'], '六级': ['效率；功效'], '考研': ['效率；效能'] } },
      'converting': { phonetic: '', translations: { '四级': ['转换；转变'], '六级': ['转换；变换'], '考研': ['转变；转换'] } },
      'applications': { phonetic: '', translations: { '四级': ['应用；申请'], '六级': ['应用；应用程序'], '考研': ['应用；运用'], '托福': ['应用；申请'] } },
      'critics': { phonetic: '', translations: { '四级': ['批评者；评论家'], '六级': ['批评家；评论员'], '考研': ['批评者'], '托福': ['批评者；评论家'] } },
      'efficient': { phonetic: '/ɪˈfɪʃnt/', translations: { '初中': ['高效的'], '高中': ['高效的'], '四级': ['高效的；有效率的'], '六级': ['有效率的'] } },
      'hurdles': { phonetic: '', translations: { '六级': ['障碍；跨栏'], '考研': ['障碍；困难'], '托福': ['障碍；跨栏'] } },
      'subsidies': { phonetic: '', translations: { '六级': ['补贴；补助金'], '考研': ['补贴；津贴'], '托福': ['补贴；补助金'] } },
      'manufacturing': { phonetic: '/ˌmænjuˈfæktʃərɪŋ/', translations: { '四级': ['制造业；制造'], '六级': ['制造业；生产'], '考研': ['制造；制造业'], '托福': ['制造；生产'] } },
      'capacity': { phonetic: '/kəˈpæsəti/', translations: { '高中': ['能力；容量'], '四级': ['能力；容量；生产力'], '六级': ['能力；容量'], '考研': ['能力；容量'], '托福': ['能力；产能'] } },
      'architecture': { phonetic: '/ˈɑːrkɪtektʃər/', translations: { '四级': ['建筑；建筑学'], '六级': ['建筑；架构'], '考研': ['建筑学；架构'], '托福': ['建筑；建筑学'] } },
      'silence': { phonetic: '/ˈsaɪləns/', translations: { '初中': ['沉默；寂静'], '高中': ['沉默；寂静'], '四级': ['沉默；寂静'], '六级': ['寂静；沉默'] } },
      'minimalism': { phonetic: '', translations: { '托福': ['极简主义；简约主义'] } },
      'aesthetic': { phonetic: '/iːsˈθetɪk/', translations: { '六级': ['美学的；审美的'], '考研': ['美学的；审美的'], '托福': ['美学的；审美的'] } },
      'philosophical': { phonetic: '/ˌfɪləˈsɒfɪkl/', translations: { '六级': ['哲学的'], '考研': ['哲学的；达观的'], '托福': ['哲学的'] } },
      'contemporary': { phonetic: '/kənˈtempəreri/', translations: { '四级': ['当代的；同时代的'], '六级': ['当代的'], '考研': ['当代的；同时代的人'], '托福': ['当代的'] } },
      'essential': { phonetic: '/ɪˈsenʃl/', translations: { '高中': ['必要的；本质的'], '四级': ['必要的；必不可少的'], '六级': ['本质的；必要的'], '考研': ['必要的；本质的'] } },
      'contemplation': { phonetic: '', translations: { '考研': ['沉思；冥想'], '托福': ['沉思；凝视'] } },
      'commodity': { phonetic: '/kəˈmɒdəti/', translations: { '六级': ['商品；日用品'], '考研': ['商品；货物'], '托福': ['商品；日用品'] } },
      'profoundly': { phonetic: '', translations: { '六级': ['深刻地；深深地'], '考研': ['深刻地；极度地'], '托福': ['深刻地'] } },
      'expressive': { phonetic: '/ɪkˈspresɪv/', translations: { '六级': ['有表现力的'], '考研': ['富于表现力的'], '托福': ['有表现力的；表达的'] } },
      'spiritually': { phonetic: '', translations: { '考研': ['精神上地'], '托福': ['精神上地'] } },
      'century': { phonetic: '/ˈsentʃəri/', translations: { '初中': ['世纪；百年'], '高中': ['世纪'], '四级': ['世纪；百年'], '六级': ['世纪'] } },
      'education': { phonetic: '/ˌedʒuˈkeɪʃn/', translations: { '初中': ['教育'], '高中': ['教育'], '四级': ['教育；教育学'], '六级': ['教育'] } },
      'reform': { phonetic: '/rɪˈfɔːrm/', translations: { '高中': ['改革；革新'], '四级': ['改革；改良'], '六级': ['改革；革新'], '考研': ['改革；改良'], '托福': ['改革'] } },
      'policy': { phonetic: '/ˈpɒləsi/', translations: { '高中': ['政策'], '四级': ['政策；方针'], '六级': ['政策；保险单'], '考研': ['政策'], '托福': ['政策'] } },
      'debates': { phonetic: '', translations: { '四级': ['辩论；争论'], '六级': ['辩论；讨论'], '考研': ['辩论'], '托福': ['辩论'] } },
      'economies': { phonetic: '', translations: { '四级': ['经济体'], '六级': ['经济体；经济'], '考研': ['经济；经济体'] } },
      'traditional': { phonetic: '/trəˈdɪʃənl/', translations: { '初中': ['传统的'], '高中': ['传统的'], '四级': ['传统的；惯例的'], '六级': ['传统的'] } },
      'curricula': { phonetic: '', translations: { '考研': ['课程'], '托福': ['课程（curriculum的复数）'] } },
      'increasingly': { phonetic: '/ɪnˈkriːsɪŋli/', translations: { '高中': ['越来越；日益'], '四级': ['越来越多地；日益'], '六级': ['日益；愈加'], '考研': ['日益'] } },
      'outdated': { phonetic: '', translations: { '六级': ['过时的'], '考研': ['过时的；陈旧的'], '托福': ['过时的'] } },
      'compelling': { phonetic: '/kəmˈpelɪŋ/', translations: { '六级': ['引人入胜的；有说服力的'], '考研': ['令人信服的；引人入胜的'], '托福': ['有说服力的'] } },
      'alternative': { phonetic: '/ɔːlˈtɜːrnətɪv/', translations: { '高中': ['替代方案；可替代的'], '四级': ['替代的；可供选择的'], '六级': ['替代的；另类的'], '考研': ['替代的'], '托福': ['替代方案'] } },
      'emphasizing': { phonetic: '', translations: { '四级': ['强调；着重'], '六级': ['强调'], '考研': ['强调'] } },
      'standardized': { phonetic: '', translations: { '六级': ['标准化的'], '考研': ['标准化的'], '托福': ['标准化的'] } },
      'prioritize': { phonetic: '/praɪˈɒrətaɪz/', translations: { '六级': ['优先处理；优先考虑'], '考研': ['优先考虑'], '托福': ['优先处理'] } },
      'autonomy': { phonetic: '/ɔːˈtɒnəmi/', translations: { '考研': ['自治；自主权'], '托福': ['自治；自主'] } },
      'interdisciplinary': { phonetic: '', translations: { '考研': ['跨学科的'], '托福': ['跨学科的'] } },
      'consistently': { phonetic: '', translations: { '四级': ['一贯地；始终如一地'], '六级': ['一贯地；一致地'], '考研': ['始终；一贯地'] } },
      'performers': { phonetic: '', translations: { '四级': ['表现者；表演者'], '六级': ['表演者；执行者'] } },
      'college': { phonetic: '/ˈkɒlɪdʒ/', translations: { '初中': ['大学；学院'], '高中': ['大学；学院'], '四级': ['大学；学院'] } },
      'transition': { phonetic: '/trænˈzɪʃn/', translations: { '四级': ['过渡；转变'], '六级': ['过渡；转型'], '考研': ['过渡；转变'], '托福': ['过渡；转变'] } },
      'growth': { phonetic: '/ɡrəʊθ/', translations: { '初中': ['成长；增长'], '高中': ['成长；增长'], '四级': ['增长；发展'], '六级': ['成长；增长'] } },
      'experience': { phonetic: '/ɪkˈspɪriəns/', translations: { '初中': ['经验；体验'], '高中': ['经验；经历'], '四级': ['经验；经历；体验'], '六级': ['经验；经历'] } },
      'responsibility': { phonetic: '/rɪˌspɒnsəˈbɪləti/', translations: { '高中': ['责任；职责'], '四级': ['责任；职责'], '六级': ['责任；义务'], '考研': ['责任'] } },
      'management': { phonetic: '/ˈmænɪdʒmənt/', translations: { '高中': ['管理'], '四级': ['管理；经营'], '六级': ['管理；管理层'], '考研': ['管理'], '托福': ['管理'] } },
      'critical': { phonetic: '/ˈkrɪtɪkl/', translations: { '高中': ['关键的；批评的'], '四级': ['关键的；危急的'], '六级': ['批评的；关键的'], '考研': ['关键的；批评的'] } },
      'academic': { phonetic: '/ˌækəˈdemɪk/', translations: { '四级': ['学术的；学业的'], '六级': ['学术的；学院的'], '考研': ['学术的'], '托福': ['学术的'] } },
      'significantly': { phonetic: '/sɪɡˈnɪfɪkəntli/', translations: { '四级': ['显著地；重大地'], '六级': ['显著地；重要地'], '考研': ['显著地'] } },
      'schedule': { phonetic: '/ˈʃedjuːl/', translations: { '初中': ['日程；时间表'], '高中': ['时间表；日程'], '四级': ['时间表；计划'], '六级': ['日程安排'], '托福': ['时间表'] } },
      'concept': { phonetic: '/ˈkɒnsept/', translations: { '高中': ['概念；观念'], '四级': ['概念；观念'], '六级': ['概念'], '考研': ['概念'], '托福': ['概念'] } },
      'global': { phonetic: '/ˈɡləʊbl/', translations: { '初中': ['全球的'], '高中': ['全球的'], '四级': ['全球的；全世界的'], '六级': ['全球的'] } },
      'green': { phonetic: '/ɡriːn/', translations: { '初中': ['绿色的；环保的'], '高中': ['绿色的'], '四级': ['绿色的；环保的'], '六级': ['绿色的'] } },
      'produced': { phonetic: '', translations: { '初中': ['生产；制造'], '高中': ['生产'], '四级': ['生产；产生'], '六级': ['生产；制作'] } },
      'splitting': { phonetic: '', translations: { '四级': ['分裂；分割'], '六级': ['分裂；分离'], '考研': ['分裂'] } },
      'molecules': { phonetic: '', translations: { '高中': ['分子'], '四级': ['分子'], '六级': ['分子'], '考研': ['分子'], '托福': ['分子'] } },
      'offers': { phonetic: '', translations: { '初中': ['提供；给予'], '高中': ['提供'], '四级': ['提供；给予'], '六级': ['提供'] } },
      'fuel': { phonetic: '/ˈfjuːəl/', translations: { '初中': ['燃料'], '高中': ['燃料'], '四级': ['燃料；刺激'], '六级': ['燃料'] } },
      'nations': { phonetic: '', translations: { '高中': ['国家；民族'], '四级': ['国家'], '六级': ['国家'] } },
      'pledged': { phonetic: '', translations: { '六级': ['承诺；保证'], '考研': ['承诺；发誓'], '托福': ['保证'] } },
      'billions': { phonetic: '', translations: { '初中': ['数十亿'], '高中': ['数十亿'], '四级': ['数十亿'], '六级': ['数十亿'] } },
      'scaling': { phonetic: '', translations: { '六级': ['扩展；规模化'], '考研': ['扩展'], '托福': ['扩展；按比例增加'] } },
      'clutter': { phonetic: '', translations: { '考研': ['杂乱；混乱'], '托福': ['杂乱；混乱'] } },
      'invite': { phonetic: '/ɪnˈvaɪt/', translations: { '初中': ['邀请'], '高中': ['邀请'], '四级': ['邀请；引起'], '六级': ['邀请'] } },
      'rare': { phonetic: '/reər/', translations: { '初中': ['稀有的；罕见的'], '高中': ['罕见的'], '四级': ['稀有的；珍贵的'], '六级': ['罕见的；稀有的'] } },
    }

    const entry = MOCK_DICT[word]
    if (!entry) {
      return { success: true, word, found: false, results: [] }
    }

    // 如果指定了学习阶段，只保留匹配阶段的释义
    let translations = entry.translations
    if (studyPurpose) {
      const filtered = {}
      if (translations[studyPurpose]) {
        filtered[studyPurpose] = translations[studyPurpose]
      }
      translations = filtered
    }

    const results = Object.entries(translations).map(([source, trans]) => ({
      source,
      entries: trans.map((t, i) => ({
        id: i + 1,
        word,
        phonetic: entry.phonetic || '',
        translation: t,
      })),
    }))

    return {
      success: true,
      word,
      phonetic: entry.phonetic || '',
      found: Object.keys(translations).length > 0,
      results,
    }
  }

  /**
   * 添加单词到生词本
   */
  async function addToVocabulary(word) {
    try {
      await request.post('/word/collect', { word })
      return true
    } catch (error) {
      console.error('添加生词失败:', error)
      return false
    }
  }

  // ========== AI 例句 ==========

  /**
   * 预取单词的 AI 例句并缓存。
   * 在用户点击单词后即可调用，后台静默加载。
   * @param {string} word
   */
  async function prefetchAIExamples(word) {
    const key = word.toLowerCase().trim()
    // 已缓存则跳过
    if (aiExampleCache.value[key]) return

    // 标记为加载中
    aiExampleCache.value[key] = { examples: [], loading: true }

    try {
      const data = await request.get('/word/ai-examples', {
        params: { word: key },
        timeout: 20000, // AI 调用较慢，给 20 秒
      })
      if (data.success && data.examples) {
        aiExampleCache.value[key] = {
          examples: data.examples,
          loading: false,
        }
      } else {
        aiExampleCache.value[key] = {
          examples: [],
          loading: false,
          error: data.message || '例句生成失败',
        }
      }
    } catch (err) {
      console.error('预取 AI 例句失败:', err)
      aiExampleCache.value[key] = {
        examples: [],
        loading: false,
        error: '例句生成失败，请稍后重试',
      }
    }
  }

  /**
   * 获取单词的 AI 例句缓存。
   * @param {string} word
   * @returns {{ examples: Array<{en:string,zh:string}>, loading: boolean, error?: string } | null}
   */
  function getAIExamples(word) {
    const key = word.toLowerCase().trim()
    return aiExampleCache.value[key] || null
  }

  // ========== 文化背景 ==========

  /**
   * 预取文章的文化背景分析（以 articleId 为 key 缓存）
   */
  async function prefetchCulturalNotes(articleId, content) {
    if (!content || culturalNotesCache.value?.articleId === articleId) return

    culturalNotesCache.value = { articleId, notes: [], loading: true, error: null }

    try {
      const data = await request.post('/article/cultural-notes',
        { content },
        { timeout: 30000 }
      )
      if (data.success) {
        culturalNotesCache.value = {
          articleId,
          notes: data.notes || [],
          loading: false,
          error: null,
        }
      } else {
        culturalNotesCache.value = {
          articleId,
          notes: [],
          loading: false,
          error: data.message || '文化背景分析失败',
        }
      }
    } catch (err) {
      culturalNotesCache.value = {
        articleId,
        notes: [],
        loading: false,
        error: '文化背景分析失败，请稍后重试',
      }
    }
  }

  // ========== 阅读理解出题 ==========

  /**
   * 预取文章的阅读理解选择题（以 articleId 为 key 缓存）
   */
  async function prefetchQuiz(articleId, content) {
    if (!content || quizCache.value?.articleId === articleId) return

    quizCache.value = { articleId, questions: [], loading: true, error: null }

    try {
      const data = await request.post('/article/quiz',
        { content },
        { timeout: 30000 }
      )
      if (data.success) {
        quizCache.value = {
          articleId,
          mainIdea: data.mainIdea || '',
          questions: data.questions || [],
          loading: false,
          error: null,
        }
      } else {
        quizCache.value = {
          articleId,
          mainIdea: '',
          questions: [],
          loading: false,
          error: data.message || '题目生成失败',
        }
      }
    } catch (err) {
      quizCache.value = {
        articleId,
        questions: [],
        loading: false,
        error: '题目生成失败，请稍后重试',
      }
    }
  }

  return {
    article,
    readingProgress,
    fontSize,
    showTranslation,
    bookmarks,
    loading,
    articleTitle,
    articleContent,
    fetchArticle,
    updateProgress,
    toggleTranslation,
    adjustFontSize,
    addBookmark,
    removeBookmark,
    lookupWord,
    addToVocabulary,
    aiExampleCache,
    aiExampleLoading,
    prefetchAIExamples,
    getAIExamples,
    culturalNotesCache,
    quizCache,
    prefetchCulturalNotes,
    prefetchQuiz,
    paragraphTranslations,
    translatingIndex,
    fetchParagraphTranslation,
    clearParagraphTranslation,
  }
})
