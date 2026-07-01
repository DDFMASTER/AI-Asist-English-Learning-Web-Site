/**
 * 获取当前登录用户的 ID（从 localStorage 读取）
 * 用于隔离不同用户的本地数据。
 */
export function getCurrentUserId() {
  try {
    const raw = localStorage.getItem('user')
    if (!raw) return null
    const user = JSON.parse(raw)
    return user.userId || null
  } catch {
    return null
  }
}

/** 生成用户隔离的 localStorage key */
export function userKey(baseKey) {
  const uid = getCurrentUserId()
  return uid ? `${baseKey}_${uid}` : baseKey
}

/** 生成用户隔离的 IndexedDB 数据库名 */
export function userDBName(baseName) {
  const uid = getCurrentUserId()
  return uid ? `${baseName}_${uid}` : baseName
}
