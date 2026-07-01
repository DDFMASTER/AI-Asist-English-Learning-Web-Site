/**
 * IndexedDB 错题本存储工具
 *
 * 数据库: AAEL_WrongBookDB
 * └─ wrongQuestions (key: id — 自增主键)
 *      { id, questionId, passage, question, options, userAnswer, correctAnswer, explanation, addedAt }
 *
 * 限制最多 200 道错题。
 */

import { userDBName } from '@/utils/storage'

const DB_VERSION = 1
const MAX_QUESTIONS = 200

function openDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(userDBName('AAEL_WrongBookDB'), DB_VERSION)

    request.onupgradeneeded = (event) => {
      const db = event.target.result
      if (!db.objectStoreNames.contains('wrongQuestions')) {
        const store = db.createObjectStore('wrongQuestions', {
          keyPath: 'id',
          autoIncrement: true,
        })
        // 按 questionId 建立索引，用于快速判重
        store.createIndex('questionIdIdx', 'questionId', { unique: true })
      }
    }

    request.onsuccess = (event) => resolve(event.target.result)
    request.onerror = (event) => {
      console.error('[wrongBookDB] 打开失败:', event.target.error)
      reject(event.target.error)
    }
  })
}

/**
 * 添加题目到错题本
 * @param {{ questionId: number, passage?: string, question: string, options: Array<{id: string, text: string}>, userAnswer?: string, correctAnswer: string, explanation?: string }} questionData
 * @returns {Promise<{success: boolean, exists?: boolean}>}
 */
export async function addToWrongBook(questionData) {
  if (!questionData || questionData.questionId == null) return { success: false }

  const db = await openDB()

  // 检查是否已存在（通过 questionId 索引）
  const exists = await new Promise((resolve, reject) => {
    const tx = db.transaction('wrongQuestions', 'readonly')
    const store = tx.objectStore('wrongQuestions')
    const idx = store.index('questionIdIdx')
    const getReq = idx.get(questionData.questionId)
    getReq.onsuccess = () => resolve(!!getReq.result)
    getReq.onerror = () => reject(getReq.error)
  })

  if (exists) {
    db.close()
    return { success: false, exists: true }
  }

  // 写入（先序列化消除 Vue 响应式 Proxy，否则 IndexedDB 无法克隆）
  await new Promise((resolve, reject) => {
    const tx = db.transaction('wrongQuestions', 'readwrite')
    const store = tx.objectStore('wrongQuestions')
    const plainData = JSON.parse(JSON.stringify({
      questionId: questionData.questionId,
      passage: questionData.passage || '',
      question: questionData.question || '',
      options: questionData.options || [],
      userAnswer: questionData.userAnswer || '',
      correctAnswer: questionData.correctAnswer || '',
      explanation: questionData.explanation || '',
      addedAt: Date.now(),
    }))
    store.add(plainData)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })

  // 超过上限时删除最旧的
  await trimWrongBook(db)

  db.close()
  return { success: true }
}

/**
 * 从错题本移除题目
 * @param {number} id — 错题本内部 id
 */
export async function removeFromWrongBook(id) {
  if (id == null) return
  const db = await openDB()

  await new Promise((resolve, reject) => {
    const tx = db.transaction('wrongQuestions', 'readwrite')
    const store = tx.objectStore('wrongQuestions')
    store.delete(id)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })

  db.close()
}

/**
 * 判断题目是否已在错题本中
 * @param {number} questionId — 原始题号
 * @returns {Promise<boolean>}
 */
export async function isInWrongBook(questionId) {
  if (questionId == null) return false
  const db = await openDB()

  return new Promise((resolve, reject) => {
    const tx = db.transaction('wrongQuestions', 'readonly')
    const store = tx.objectStore('wrongQuestions')
    const idx = store.index('questionIdIdx')
    const getReq = idx.get(questionId)
    getReq.onsuccess = () => resolve(!!getReq.result)
    getReq.onerror = () => reject(getReq.error)
    tx.oncomplete = () => db.close()
  })
}

/**
 * 获取错题本全部题目（按添加时间降序）
 * @returns {Promise<Array>}
 */
export async function getAllWrongQuestions() {
  const db = await openDB()

  return new Promise((resolve, reject) => {
    const tx = db.transaction('wrongQuestions', 'readonly')
    const store = tx.objectStore('wrongQuestions')
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
 * 获取错题本题目数量
 * @returns {Promise<number>}
 */
export async function countWrongQuestions() {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction('wrongQuestions', 'readonly')
    const store = tx.objectStore('wrongQuestions')
    const req = store.count()
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
    tx.oncomplete = () => db.close()
  })
}

/**
 * 删除多余的旧记录，仅保留最近 MAX_QUESTIONS 条
 */
function trimWrongBook(db) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction('wrongQuestions', 'readwrite')
    const store = tx.objectStore('wrongQuestions')

    const getAll = store.getAll()
    getAll.onsuccess = () => {
      const records = getAll.result || []
      if (records.length > MAX_QUESTIONS) {
        records.sort((a, b) => b.addedAt - a.addedAt)
        const toDelete = records.slice(MAX_QUESTIONS)
        toDelete.forEach((r) => store.delete(r.id))
      }
    }
    getAll.onerror = () => reject(getAll.error)

    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}
