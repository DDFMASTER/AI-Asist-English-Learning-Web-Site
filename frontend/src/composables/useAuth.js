import { useUserStore } from '@/stores/user'

/**
 * 登录守卫 composable
 *
 * 用法：
 *   const { guard } = useRequireAuth()
 *   @click="guard(someHandler)"
 *   或内联：@click="guard(() => doSomething())"
 */
export function useRequireAuth() {
  const userStore = useUserStore()

  /**
   * 包装一个函数：已登录则正常执行，未登录则弹出登录窗口
   * @param {Function} fn - 要保护的回调
   * @returns {Function} 包装后的函数
   */
  function guard(fn) {
    return (...args) => {
      console.log('[guard] isLoggedIn:', userStore.isLoggedIn, 'token:', !!userStore.token, 'user:', !!userStore.user)
      if (!userStore.isLoggedIn) {
        console.log('[guard] 未登录，弹出登录弹窗，已保存待执行函数')
        // 保存待执行函数，登录成功后自动重试
        userStore.setPendingAction(() => fn(...args))
        userStore.openLoginModal()
        return
      }
      console.log('[guard] 已登录，执行原函数')
      return fn(...args)
    }
  }

  return { guard, isLoggedIn: userStore.isLoggedIn }
}
