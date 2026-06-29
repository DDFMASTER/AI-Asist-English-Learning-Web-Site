/**
 * 在线时长追踪器
 *
 * 使用场景：在 App.vue 中调用 startOnlineTimer()，全局追踪用户每日在线时长。
 *
 * 机制：
 * - 每 60 秒将累加的时间写入 IndexedDB
 * - 页面隐藏（切走）时暂停计时，恢复时继续
 * - 页面关闭前立即保存
 * - 跨天自动重置计数器
 */

import { addTodayMinutes, getTodayMinutes } from '@/utils/onlineTimeDB'

const SAVE_INTERVAL_MS = 60_000 // 每 60 秒存一次

let timerHandle = null
let startTimestamp = null
let todayKey = null
let running = false

function currentDateKey() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function flush() {
  if (!startTimestamp) return
  const elapsed = (Date.now() - startTimestamp) / 60_000 // 转为分钟
  if (elapsed <= 0) return
  startTimestamp = Date.now()
  try {
    await addTodayMinutes(elapsed)
    console.log(`[onlineTimer] 已保存 +${elapsed.toFixed(1)} 分钟`)
  } catch (e) {
    console.error('[onlineTimer] 保存失败:', e)
  }
}

function scheduleNext() {
  if (!running) return
  timerHandle = setTimeout(async () => {
    await flush()
    // 检查是否跨天
    const newKey = currentDateKey()
    if (newKey !== todayKey) {
      todayKey = newKey
      startTimestamp = Date.now()
    }
    scheduleNext()
  }, SAVE_INTERVAL_MS)
}

/**
 * 启动在线计时器（幂等）
 */
export async function startOnlineTimer() {
  if (running) return
  running = true
  todayKey = currentDateKey()
  startTimestamp = Date.now()

  // 恢复今日已有分钟数（仅用于日志）
  try {
    const existing = await getTodayMinutes()
    console.log(`[onlineTimer] 今日已在线 ${existing} 分钟，继续计时`)
  } catch (_) { /* ignore */ }

  scheduleNext()
}

/**
 * 暂停计时（页面隐藏时调用）
 */
export function pauseTimer() {
  running = false
  if (timerHandle) {
    clearTimeout(timerHandle)
    timerHandle = null
  }
  // 不等待，fire-and-forget
  flush().catch(() => {})
}

/**
 * 恢复计时（页面可见时调用）
 */
export function resumeTimer() {
  if (running) return
  running = true
  const newKey = currentDateKey()
  if (newKey !== todayKey) {
    todayKey = newKey
  }
  startTimestamp = Date.now()
  scheduleNext()
}
