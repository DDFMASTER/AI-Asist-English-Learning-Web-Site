/**
 * IndexedDB 生词本存储工具
 *
 * 数据库: AAEL_VocabDB
 * └─ vocabWords (key: wordLower — 小写单词)
 *      { word, phonetic, translation, source, addedAt }
 *
 * 与 onlineTimeDB / historyDB 保持一致的 API 风格。
 */

import { userDBName } from '@/utils/storage'

const DB_VERSION = 1
const MAX_WORDS = 200

function openDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(userDBName('AAEL_VocabDB'), DB_VERSION)

    request.onupgradeneeded = (event) => {
      const db = event.target.result
      if (!db.objectStoreNames.contains('vocabWords')) {
        db.createObjectStore('vocabWords', { keyPath: 'wordLower' })
      }
    }

    request.onsuccess = (event) => resolve(event.target.result)
    request.onerror = (event) => {
      console.error('[vocabDB] 打开失败:', event.target.error)
      reject(event.target.error)
    }
  })
}

/**
 * 添加单词到生词本
 * @param {{ word: string, phonetic?: string, translation?: string, source?: string }} wordData
 * @returns {Promise<boolean>} true=新增, false=已存在
 */
export async function addToVocab(wordData) {
  if (!wordData || !wordData.word) return false

  const wordLower = wordData.word.toLowerCase().trim()
  const db = await openDB()

  // 检查是否已存在
  const exists = await new Promise((resolve, reject) => {
    const tx = db.transaction('vocabWords', 'readonly')
    const store = tx.objectStore('vocabWords')
    const getReq = store.get(wordLower)
    getReq.onsuccess = () => resolve(!!getReq.result)
    getReq.onerror = () => reject(getReq.error)
    tx.oncomplete = () => {}
  })

  if (exists) {
    db.close()
    return false
  }

  // 写入
  await new Promise((resolve, reject) => {
    const tx = db.transaction('vocabWords', 'readwrite')
    const store = tx.objectStore('vocabWords')
    store.put({
      wordLower,
      word: wordData.word,
      phonetic: wordData.phonetic || '',
      translation: wordData.translation || '',
      source: wordData.source || '',
      addedAt: Date.now(),
    })
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })

  // 超过上限时删除最旧的
  await trimVocab(db)

  db.close()
  return true
}

/**
 * 从生词本移除单词
 * @param {string} word
 */
export async function removeFromVocab(word) {
  const wordLower = word.toLowerCase().trim()
  const db = await openDB()

  await new Promise((resolve, reject) => {
    const tx = db.transaction('vocabWords', 'readwrite')
    const store = tx.objectStore('vocabWords')
    store.delete(wordLower)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })

  db.close()
}

/**
 * 判断单词是否已在生词本中
 * @param {string} word
 * @returns {Promise<boolean>}
 */
export async function isInVocab(word) {
  const wordLower = word.toLowerCase().trim()
  const db = await openDB()

  return new Promise((resolve, reject) => {
    const tx = db.transaction('vocabWords', 'readonly')
    const store = tx.objectStore('vocabWords')
    const getReq = store.get(wordLower)
    getReq.onsuccess = () => resolve(!!getReq.result)
    getReq.onerror = () => reject(getReq.error)
    tx.oncomplete = () => db.close()
  })
}

/**
 * 获取生词本全部单词（按添加时间降序）
 * @returns {Promise<Array<{wordLower, word, phonetic, translation, source, addedAt}>>}
 */
export async function getAllVocab() {
  const db = await openDB()

  return new Promise((resolve, reject) => {
    const tx = db.transaction('vocabWords', 'readonly')
    const store = tx.objectStore('vocabWords')
    const getAll = store.getAll()

    getAll.onsuccess = () => {
      const records = getAll.result || []
      records.sort((a, b) => b.addedAt - a.addedAt)
      resolve(records)
    }
    getAll.onerror = () => reject(getAll.error)
    tx.oncomplete = () => db.close()
  })
}

/**
 * 获取生词本单词数量
 * @returns {Promise<number>}
 */
export async function countVocab() {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction('vocabWords', 'readonly')
    const store = tx.objectStore('vocabWords')
    const req = store.count()
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
    tx.oncomplete = () => db.close()
  })
}

/**
 * 删除多余的旧记录，仅保留最近 MAX_WORDS 条
 */
function trimVocab(db) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction('vocabWords', 'readwrite')
    const store = tx.objectStore('vocabWords')

    const getAll = store.getAll()
    getAll.onsuccess = () => {
      const records = getAll.result || []
      if (records.length > MAX_WORDS) {
        records.sort((a, b) => b.addedAt - a.addedAt)
        const toDelete = records.slice(MAX_WORDS)
        toDelete.forEach((r) => store.delete(r.wordLower))
      }
    }
    getAll.onerror = () => reject(getAll.error)

    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}
