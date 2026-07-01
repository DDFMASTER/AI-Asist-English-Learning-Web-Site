/**
 * IndexedDB 每日在线时长存储工具
 *
 * 数据库: AAEL_OnlineTimeDB
 * └─ dailyOnlineTime (key: date, 格式 "YYYY-MM-DD")
 *      { date: string, minutes: number }
 */

import { userDBName, userKey } from '@/utils/storage'

const DB_VERSION = 1

function getTodayKey() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function daysAgoKey(n) {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function openDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(userDBName('AAEL_OnlineTimeDB'), DB_VERSION)

    request.onupgradeneeded = (event) => {
      const db = event.target.result
      if (!db.objectStoreNames.contains('dailyOnlineTime')) {
        db.createObjectStore('dailyOnlineTime', { keyPath: 'date' })
      }
    }

    request.onsuccess = (event) => resolve(event.target.result)
    request.onerror = (event) => {
      console.error('[onlineTimeDB] 打开失败:', event.target.error)
      reject(event.target.error)
    }
  })
}

/**
 * 累加今日在线时长
 * @param {number} minutes - 要累加的分钟数（支持小数）
 */
export async function addTodayMinutes(minutes) {
  if (minutes <= 0) return
  const date = getTodayKey()
  const db = await openDB()

  return new Promise((resolve, reject) => {
    const tx = db.transaction('dailyOnlineTime', 'readwrite')
    const store = tx.objectStore('dailyOnlineTime')

    const getReq = store.get(date)
    getReq.onsuccess = () => {
      const existing = getReq.result
      const newMinutes = existing ? existing.minutes + minutes : minutes
      store.put({ date, minutes: Math.round(newMinutes * 10) / 10 })
    }
    getReq.onerror = () => reject(getReq.error)

    tx.oncomplete = () => {
      db.close()
      resolve()
    }
    tx.onerror = () => reject(tx.error)
  })
}

/**
 * 获取最近 N 天的在线时长数据
 * @param {number} days - 天数（含今天）
 * @returns {Promise<Array<{date: string, minutes: number}>>}
 */
export async function getRecentOnlineTime(days = 14) {
  const db = await openDB()

  return new Promise((resolve, reject) => {
    const tx = db.transaction('dailyOnlineTime', 'readonly')
    const store = tx.objectStore('dailyOnlineTime')
    const getAll = store.getAll()

    getAll.onsuccess = () => {
      const records = getAll.result || []
      // 构建最近 N 天的完整列表，缺失的填 0
      const result = []
      for (let i = days - 1; i >= 0; i--) {
        const dateKey = daysAgoKey(i)
        const found = records.find((r) => r.date === dateKey)
        result.push({
          date: dateKey,
          minutes: found ? found.minutes : 0,
        })
      }
      resolve(result)
    }
    getAll.onerror = () => reject(getAll.error)

    tx.oncomplete = () => db.close()
  })
}

/**
 * 获取今日在线时长（分钟）
 */
export async function getTodayMinutes() {
  const date = getTodayKey()
  const db = await openDB()

  return new Promise((resolve, reject) => {
    const tx = db.transaction('dailyOnlineTime', 'readonly')
    const store = tx.objectStore('dailyOnlineTime')
    const getReq = store.get(date)

    getReq.onsuccess = () => {
      const record = getReq.result
      resolve(record ? record.minutes : 0)
    }
    getReq.onerror = () => reject(getReq.error)

    tx.oncomplete = () => db.close()
  })
}

/**
 * 计算连续学习天数（从今天往回数，分钟数 > 0 的天数）
 */
export async function getStreak() {
  const records = await getRecentOnlineTime(365)
  let streak = 0
  // 从今天开始往回数
  for (let i = records.length - 1; i >= 0; i--) {
    if (records[i].minutes > 0) {
      streak++
    } else {
      break
    }
  }
  return streak
}

// ========== 每日学习目标（localStorage）==========

const getTargetKey = () => userKey('aael_daily_target_minutes')
const DEFAULT_TARGET = 30

/**
 * 获取每日学习目标时长（分钟）
 */
export function getDailyTarget() {
  try {
    const val = localStorage.getItem(getTargetKey())
    if (val !== null) {
      const num = parseInt(val, 10)
      if (!isNaN(num) && num > 0) return num
    }
  } catch (_) { /* ignore */ }
  return DEFAULT_TARGET
}

/**
 * 设置每日学习目标时长（分钟）
 */
export function setDailyTarget(minutes) {
  try {
    localStorage.setItem(getTargetKey(), String(minutes))
  } catch (_) { /* ignore */ }
}
