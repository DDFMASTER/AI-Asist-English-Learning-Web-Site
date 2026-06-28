import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/materials' },
  {
    path: '/materials',
    name: 'Materials',
    component: () => import('@/pages/MaterialsPage.vue'),
    meta: { title: '智能读物匹配' }
  },
  {
    path: '/tasks',
    name: 'Tasks',
    component: () => import('@/pages/TasksPage.vue'),
    meta: { title: '任务管理中心' }
  },
  {
    path: '/assessment',
    name: 'Assessment',
    component: () => import('@/pages/AssessmentPage.vue'),
    meta: { title: '自适应测评' }
  },
  {
    path: '/result',
    name: 'Result',
    component: () => import('@/pages/ResultPage.vue'),
    meta: { title: '测评结果报告' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/pages/ProfilePage.vue'),
    meta: { title: '个人中心' }
  },
  {
    path: '/reader',
    name: 'Reader',
    component: () => import('@/pages/ReaderPage.vue'),
    meta: { title: 'AI 沉浸式阅读器' }
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

// 设置页面标题
router.afterEach((to) => {
  const title = to.meta.title || 'LinguaAI'
  document.title = `${title} - LinguaAI`
})

export default router
