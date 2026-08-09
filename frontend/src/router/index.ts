import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'

const SYSTEM = '京东方投资测算系统'

/**
 * R-03 前端路由拆分：业务页按 upgrade_plan §9 拆为独立 view，
 * 尚未落地的页（/dashboard 等）由 R-07/R-15 等后续改造项补齐。
 */
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true, title: '登录' } },
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { requiresAuth: true, title: '工作台看板' } },
    { path: '/projects', name: 'projects', component: () => import('@/views/ProjectsView.vue'), meta: { requiresAuth: true, title: '项目管理' } },
    { path: '/scenarios', name: 'scenarios', component: () => import('@/views/ScenariosView.vue'), meta: { requiresAuth: true, title: '测算方案' } },
    { path: '/collab', name: 'collab', component: () => import('@/views/CollabView.vue'), meta: { requiresAuth: true, title: '协同编辑' } },
    { path: '/inputs', name: 'inputs', component: () => import('@/views/InputsView.vue'), meta: { requiresAuth: true, title: '测算输入' } },
    { path: '/calculation', name: 'calculation', component: () => import('@/views/CalculationView.vue'), meta: { requiresAuth: true, title: '测算执行' } },
    { path: '/risk', name: 'risk', component: () => import('@/views/RiskAnalysisView.vue'), meta: { requiresAuth: true, title: '风险分析' } },
    { path: '/compare', name: 'compare', component: () => import('@/views/ComparisonView.vue'), meta: { requiresAuth: true, title: '方案比选' } },
    { path: '/library', name: 'library', component: () => import('@/views/LibraryView.vue'), meta: { requiresAuth: true, title: '项目库' } },
    { path: '/reports', name: 'reports', component: () => import('@/views/ReportsView.vue'), meta: { requiresAuth: true, title: '报告中心' } },
    { path: '/governance', name: 'governance', component: () => import('@/views/GovernanceView.vue'), meta: { requiresAuth: true, title: '流程治理' } },
    { path: '/audit', name: 'audit', component: () => import('@/views/AuditView.vue'), meta: { requiresAuth: true, title: '审计日志' } }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { path: '/' }
  }
  return true
})

// DESIGN.md 全局规则：页签标题 = "京东方{系统名称} - {页面名称}"
router.afterEach((to) => {
  const page = (to.meta.title as string) ?? ''
  document.title = page ? `${SYSTEM} - ${page}` : SYSTEM
})
