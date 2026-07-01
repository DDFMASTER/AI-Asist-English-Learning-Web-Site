import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/materials',
    name: 'Materials',
    component: () => import('@/pages/MaterialsPage.vue'),
    meta: { title: '智能读物匹配', requiresAuth: true }
  },
  {
    path: '/assessment',
    name: 'Assessment',
    component: () => import('@/pages/AssessmentPage.vue'),
    meta: { title: '自适应测评', requiresAuth: true }
  },
  {
    path: '/result',
    name: 'Result',
    component: () => import('@/pages/ResultPage.vue'),
    meta: { title: '测评结果报告', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/pages/ProfilePage.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/reader',
    name: 'Reader',
    component: () => import('@/pages/ReaderPage.vue'),
    meta: { title: 'AI 沉浸式阅读器', requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/pages/AdminPage.vue'),
    meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true }
  },
  // 兜底：未匹配路由重定向到登录页
  { path: '/:pathMatch(.*)*', redirect: '/login' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

// 导航守卫：未登录用户只能访问 guest 页面
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const requiresAuth = to.meta.requiresAuth
  const isGuest = to.meta.guest
  const requiresAdmin = to.meta.requiresAdmin

  if (requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (requiresAdmin) {
    // 检查用户角色是否为 admin
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      if (user.role !== 'admin') {
        next('/materials')
        return
      }
    } catch (_) {
      next('/materials')
      return
    }
    next()
  } else if (isGuest && token) {
    // 已登录用户访问登录页，重定向到读物匹配
    next('/materials')
  } else if (to.path === '/' || to.path === '') {
    // 根路径：已登录 → 读物匹配，未登录 → 登录页
    next(token ? '/materials' : '/login')
  } else {
    next()
  }
})

// 设置页面标题
router.afterEach((to) => {
  const title = to.meta.title || 'EngliAI'
  document.title = `${title} - EngliAI`
})

export default router
