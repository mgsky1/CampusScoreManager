import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import type { UserRole } from '@/types/api'

/** 路由 meta 类型扩展；`role` 存在时，guards 会强制校验。 */
declare module 'vue-router' {
  interface RouteMeta {
    role?: UserRole
    public?: boolean
    title?: string
  }
}

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/forbidden',
    name: 'forbidden',
    component: () => import('@/views/common/ForbiddenView.vue'),
    meta: { public: true },
  },
  // ---------- Student ----------
  {
    path: '/student',
    component: () => import('@/layouts/StudentLayout.vue'),
    meta: { role: 'STUDENT' },
    redirect: '/student/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'student.dashboard',
        component: () => import('@/views/student/DashboardView.vue'),
        meta: { role: 'STUDENT', title: '首页' },
      },
      {
        path: 'scores',
        name: 'student.scores',
        component: () => import('@/views/student/MyScoresView.vue'),
        meta: { role: 'STUDENT', title: '我的成绩' },
      },
      {
        path: 'profile',
        name: 'student.profile',
        component: () => import('@/views/student/ProfileView.vue'),
        meta: { role: 'STUDENT', title: '个人资料' },
      },
      {
        path: 'password',
        name: 'student.password',
        component: () => import('@/views/student/PasswordView.vue'),
        meta: { role: 'STUDENT', title: '修改密码' },
      },
    ],
  },
  // ---------- Teacher ----------
  {
    path: '/teacher',
    component: () => import('@/layouts/TeacherLayout.vue'),
    meta: { role: 'TEACHER' },
    redirect: '/teacher/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'teacher.dashboard',
        component: () => import('@/views/teacher/DashboardView.vue'),
        meta: { role: 'TEACHER', title: '首页' },
      },
      {
        path: 'students',
        name: 'teacher.students',
        component: () => import('@/views/teacher/StudentsView.vue'),
        meta: { role: 'TEACHER', title: '学生管理' },
      },
      {
        path: 'subjects',
        name: 'teacher.subjects',
        component: () => import('@/views/teacher/SubjectsView.vue'),
        meta: { role: 'TEACHER', title: '我的课程' },
      },
      {
        path: 'score-manage',
        name: 'teacher.scoreManage',
        component: () => import('@/views/teacher/ScoreManageView.vue'),
        meta: { role: 'TEACHER', title: '成绩管理' },
      },
      {
        path: 'students/:id/score-detail',
        name: 'teacher.scoreDetail',
        component: () => import('@/views/teacher/ScoreDetailView.vue'),
        meta: { role: 'TEACHER', title: '查看学生成绩' },
      },
      {
        path: 'students/:id/score-entry',
        name: 'teacher.scoreEntry',
        component: () => import('@/views/teacher/ScoreEntryView.vue'),
        meta: { role: 'TEACHER', title: '录入学生成绩' },
      },
      {
        path: 'profile',
        name: 'teacher.profile',
        component: () => import('@/views/teacher/ProfileView.vue'),
        meta: { role: 'TEACHER', title: '个人信息' },
      },
      {
        path: 'password',
        name: 'teacher.password',
        component: () => import('@/views/teacher/PasswordView.vue'),
        meta: { role: 'TEACHER', title: '修改密码' },
      },
    ],
  },
  // ---------- 404 ----------
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/common/NotFoundView.vue'),
    meta: { public: true },
  },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
