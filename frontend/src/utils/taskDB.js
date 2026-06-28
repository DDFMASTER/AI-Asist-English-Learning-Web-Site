/**
 * IndexedDB 任务缓存工具
 *
 * 数据库: AAEL_TaskDB
 * ┌─ dailyTasks (key: date YYYY-MM-DD)
 * │    { date, tasks: [...], xpEarned, generatedAt }
 * └─ xpHistory (key: date YYYY-MM-DD)
 *      { date, xpEarned, taskCount }
 */

const DB_NAME = 'AAEL_TaskDB'
const DB_VERSION = 1

/**
 * 打开/初始化数据库
 */
function openDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onupgradeneeded = (event) => {
      const db = event.target.result

      // 每日任务存储
      if (!db.objectStoreNames.contains('dailyTasks')) {
        db.createObjectStore('dailyTasks', { keyPath: 'date' })
      }

      // 经验历史存储（用于趋势图）
      if (!db.objectStoreNames.contains('xpHistory')) {
        db.createObjectStore('xpHistory', { keyPath: 'date' })
      }
    }

    request.onsuccess = (event) => {
      resolve(event.target.result)
    }

    request.onerror = (event) => {
      console.error('IndexedDB 打开失败:', event.target.error)
      reject(event.target.error)
    }
  })
}

/**
 * 获取今天的日期字符串 YYYY-MM-DD
 */
export function todayDate() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/**
 * 获取最近 N 天的日期数组
 */
export function recentDates(days = 7) {
  const dates = []
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    dates.push(`${y}-${m}-${day}`)
  }
  return dates
}

/**
 * 获取某天的任务数据
 * @param {string} date - YYYY-MM-DD
 * @returns {Promise<{date: string, tasks: Array, xpEarned: number, generatedAt: number}|null>}
 */
export async function getDailyTasks(date) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction('dailyTasks', 'readonly')
    const store = tx.objectStore('dailyTasks')
    const request = store.get(date)

    request.onsuccess = () => resolve(request.result || null)
    request.onerror = () => reject(request.error)

    tx.oncomplete = () => db.close()
  })
}

/**
 * 保存某天的任务数据
 * @param {string} date
 * @param {{tasks: Array, xpEarned: number}} data
 */
export async function saveDailyTasks(date, data) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction('dailyTasks', 'readwrite')
    const store = tx.objectStore('dailyTasks')
    const record = {
      date,
      tasks: data.tasks,
      xpEarned: data.xpEarned,
      generatedAt: Date.now(),
    }
    store.put(record)

    tx.oncomplete = () => {
      db.close()
      resolve()
    }
    tx.onerror = () => reject(tx.error)
  })
}

/**
 * 获取最近 N 天的经验历史
 * @param {number} days - 天数
 * @returns {Promise<Array<{date: string, xpEarned: number, taskCount: number}>>}
 */
export async function getXpHistory(days = 7) {
  const db = await openDB()
  const dateList = recentDates(days)

  return new Promise((resolve, reject) => {
    const tx = db.transaction('xpHistory', 'readonly')
    const store = tx.objectStore('xpHistory')
    const results = []
    let count = 0

    dateList.forEach((date) => {
      const req = store.get(date)
      req.onsuccess = () => {
        if (req.result) {
          results.push(req.result)
        } else {
          // 没有记录的日子返回零值
          results.push({ date, xpEarned: 0, taskCount: 0 })
        }
        count++
        if (count === dateList.length) {
          // 按日期排序
          results.sort((a, b) => a.date.localeCompare(b.date))
          resolve(results)
        }
      }
      req.onerror = () => {
        count++
        results.push({ date, xpEarned: 0, taskCount: 0 })
        if (count === dateList.length) resolve(results)
      }
    })

    tx.oncomplete = () => db.close()
  })
}

/**
 * 保存某天的经验统计
 * @param {string} date
 * @param {{xpEarned: number, taskCount: number}} data
 */
export async function saveXpHistory(date, data) {
  const db = await openDB()
  return new Promise((resolve, reject) => {
    const tx = db.transaction('xpHistory', 'readwrite')
    const store = tx.objectStore('xpHistory')
    store.put({ date, xpEarned: data.xpEarned, taskCount: data.taskCount })

    tx.oncomplete = () => {
      db.close()
      resolve()
    }
    tx.onerror = () => reject(tx.error)
  })
}

/**
 * 清除某天之前的所有数据（可选，用于清理旧数据）
 * @param {string} beforeDate
 */
export async function clearOldData(beforeDate) {
  const db = await openDB()

  const clearStore = (storeName) => {
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, 'readwrite')
      const store = tx.objectStore(storeName)
      const range = IDBKeyRange.upperBound(beforeDate, true)
      store.delete(range)
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
    })
  }

  await clearStore('dailyTasks')
  await clearStore('xpHistory')
  db.close()
}
