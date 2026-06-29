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
   * 保护一个函数：已登录则直接执行，未登录则弹出登录窗口并保存待执行。
   *
   * 重要：此函数设计为在 Vue @click 内联表达式中使用：
   *   @click="guard(someHandler)"
   * Vue 模板编译器会将内联表达式包装为 $event => guard(someHandler)，
   * 因此 guard 必须直接执行 fn，而不是返回一个包装函数（返回值会被丢弃）。
   *
   * @param {Function} fn - 要保护的回调（零参数或已通过闭包捕获参数）
   */
  function guard(fn) {
    console.log('[guard] isLoggedIn:', userStore.isLoggedIn, 'token:', !!userStore.token, 'user:', !!userStore.user)
    if (!userStore.isLoggedIn) {
      console.log('[guard] 未登录，弹出登录弹窗，已保存待执行函数')
      // 保存待执行函数，登录成功后自动重试
      userStore.setPendingAction(fn)
      userStore.openLoginModal()
      return
    }
    console.log('[guard] 已登录，执行原函数')
    return fn()
  }

  return { guard, isLoggedIn: userStore.isLoggedIn }
}
