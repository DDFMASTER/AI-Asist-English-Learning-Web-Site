/**
 * IndexedDB 浏览历史缓存工具
 *
 * 数据库: AAEL_HistoryDB
 * └─ browseHistory (key: articleId)
 *      { articleId, title, visitedAt }
 *
 * 保留最近访问的 50 篇文章。
 */

const DB_NAME = 'AAEL_HistoryDB'
const DB_VERSION = 1
const MAX_HISTORY = 50

/**
 * 打开/初始化数据库
 */
function openDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onupgradeneeded = (event) => {
      const db = event.target.result
      if (!db.objectStoreNames.contains('browseHistory')) {
        db.createObjectStore('browseHistory', { keyPath: 'articleId' })
      }
    }

    request.onsuccess = (event) => {
      resolve(event.target.result)
    }

    request.onerror = (event) => {
      console.error('IndexedDB (History) 打开失败:', event.target.error)
      reject(event.target.error)
    }
  })
}

/**
 * 记录一次文章浏览
 * @param {number|string} articleId - 文章 ID
 * @param {string} title - 文章标题
 */
export async function addToHistory(articleId, title) {
  if (!articleId) return

  console.log('[historyDB] 记录浏览:', articleId, title)
  const db = await openDB()

  // 1. 写入/更新该文章的访问记录
  await new Promise((resolve, reject) => {
    const tx = db.transaction('browseHistory', 'readwrite')
    const store = tx.objectStore('browseHistory')
    store.put({
      articleId: Number(articleId),
      title: title || 'Untitled',
      visitedAt: Date.now(),
    })
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })

  // 2. 仅保留最近 MAX_HISTORY 条（按 visitedAt 降序）
  await trimHistory(db)

  db.close()
  console.log('[historyDB] 记录完成:', articleId, title)
}

/**
 * 删除多余的旧记录，仅保留最近 MAX_HISTORY 条
 */
function trimHistory(db) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction('browseHistory', 'readwrite')
    const store = tx.objectStore('browseHistory')

    // 获取全部记录并按访问时间排序
    const getAll = store.getAll()
    getAll.onsuccess = () => {
      const records = getAll.result || []
      if (records.length > MAX_HISTORY) {
        // 按 visitedAt 降序排序
        records.sort((a, b) => b.visitedAt - a.visitedAt)
        // 删除多余的旧记录
        const toDelete = records.slice(MAX_HISTORY)
        toDelete.forEach((r) => store.delete(r.articleId))
      }
      // 不在此 resolve，等 tx.oncomplete 确保删除操作已提交
    }
    getAll.onerror = () => reject(getAll.error)

    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

/**
 * 获取浏览历史总数
 * @returns {Promise<number>}
 */
export async function countHistory() {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction('browseHistory', 'readonly')
    const store = tx.objectStore('browseHistory')
    const req = store.count()
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
    tx.oncomplete = () => db.close()
  })
}

/**
 * 获取最近 N 条浏览历史
 * @param {number} limit - 返回条数，默认 3
 * @returns {Promise<Array<{articleId: number, title: string, visitedAt: number}>>}
 */
export async function getRecentHistory(limit = MAX_HISTORY) {
  const db = await openDB()

  return new Promise((resolve, reject) => {
    const tx = db.transaction('browseHistory', 'readonly')
    const store = tx.objectStore('browseHistory')
    const getAll = store.getAll()

    getAll.onsuccess = () => {
      const records = getAll.result || []
      // 按 visitedAt 降序排序，取前 N 条
      records.sort((a, b) => b.visitedAt - a.visitedAt)
      const result = records.slice(0, limit)
      console.log('[historyDB] getRecentHistory 返回:', result.length, '条', result)
      resolve(result)
    }
    getAll.onerror = () => reject(getAll.error)

    tx.oncomplete = () => db.close()
  })
}

/**
 * 获取所有已读文章的 ID 集合
 * @returns {Promise<Set<number>>}
 */
export async function getReadArticleIds() {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction('browseHistory', 'readonly')
    const store = tx.objectStore('browseHistory')
    const getAll = store.getAll()
    getAll.onsuccess = () => {
      const records = getAll.result || []
      const ids = new Set(records.map(r => Number(r.articleId)))
      resolve(ids)
    }
    getAll.onerror = () => reject(getAll.error)
    tx.oncomplete = () => db.close()
  })
}

/**
 * 将时间戳转为相对时间描述
 * @param {number} timestamp
 * @returns {string} 如 "刚刚"、"5分钟前"、"2小时前"、"3天前"
 */
export function relativeTime(timestamp) {
  const now = Date.now()
  const diff = now - timestamp
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  // 超过 30 天显示日期
  const d = new Date(timestamp)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}