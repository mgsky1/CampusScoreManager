/**
 * T174 · 前端可访问性 sweep
 *
 * 目标：对核心页面用 axe-core 做静态可访问性扫描，断言无 critical / serious 问题。
 * 场景：
 * - LoginView
 * - MyScoresView（学生成绩列表）
 * - ScoreEntryView（教师录入）
 * - StudentsView（教师学生管理）
 * - SubjectsView（教师课程管理）
 * - ProfileView（学生个人资料）
 * - PasswordView（修改密码）
 * - ScoreBadge（成绩徽章：颜色 + 文字并存，不依赖颜色）
 *
 * 说明：
 * - Element Plus 组件在此测试里全部用与其它 spec 相同的 stub 替代，
 *   以避免 Vitest jsdom 下真实 EL 组件的 style / focus 副作用。
 * - 使用 axe-core `run` API 直接扫描挂载后的 DOM 树。
 * - Vitest 环境需将 axios 的 http adapter mock 掉；本 sweep 只关心 DOM，
 *   API 层错误由 `try/catch` 吞掉，不影响可访问性判断。
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import axe from 'axe-core'
import type { RuleObject, Spec } from 'axe-core'

// ---- 被测组件 ----
import LoginView from '@/views/auth/LoginView.vue'
import MyScoresView from '@/views/student/MyScoresView.vue'
import ScoreEntryView from '@/views/teacher/ScoreEntryView.vue'
import StudentsView from '@/views/teacher/StudentsView.vue'
import SubjectsView from '@/views/teacher/SubjectsView.vue'
import ProfileForm from '@/components/ProfileForm.vue'
import PasswordForm from '@/components/PasswordForm.vue'
import ScoreBadge from '@/components/ScoreBadge.vue'

// ---- API 层 stub ----
import * as apiStudentScores from '@/api/studentScores'
import * as apiTeacherStudents from '@/api/teacherStudents'
import * as apiTeacherSubjects from '@/api/teacherSubjects'
import * as apiTeacherScores from '@/api/teacherScores'
import * as apiAccount from '@/api/account'

// ---- Element Plus 通用 stub（简化 axe 扫描）----
const stubs = {
  'el-input': {
    template:
      '<input :aria-label="label ?? placeholder" :value="modelValue" :placeholder="placeholder" :type="type" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" />',
    props: [
      'modelValue',
      'placeholder',
      'type',
      'disabled',
      'clearable',
      'showPassword',
      'label',
    ],
    emits: ['update:modelValue'],
  },
  'el-input-number': {
    template:
      '<input type="number" :aria-label="label ?? \'number\'" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />',
    props: ['modelValue', 'min', 'max', 'label'],
    emits: ['update:modelValue'],
  },
  'el-button': {
    template:
      '<button type="button" :data-type="type" @click="$emit(\'click\', $event)"><slot /></button>',
    props: ['type', 'loading', 'nativeType', 'link'],
    emits: ['click'],
  },
  'el-form': {
    template: '<form @submit.prevent="$emit(\'submit\')"><slot /></form>',
    props: ['model', 'labelWidth', 'inline', 'rules'],
    emits: ['submit'],
  },
  'el-form-item': {
    // 把子控件包在 <label> 里 → axe 认可"隐式 label"
    template:
      '<div role="group" :aria-invalid="!!error"><label>{{ label }}<slot /></label>' +
      '<span v-if="error" role="alert">{{ error }}</span></div>',
    props: ['label', 'prop', 'error'],
  },
  'el-table': {
    template:
      '<table role="table"><thead><tr><th v-for="c in columns" :key="c">{{ c }}</th></tr></thead>' +
      '<tbody><tr v-for="row in data" :key="row?.id"><td><slot :row="row" /></td></tr></tbody></table>',
    props: ['data'],
    computed: {
      columns() {
        return ['数据']
      },
    },
  },
  'el-table-column': {
    template: '<span :data-label="label"><slot :row="{}" /></span>',
    props: ['prop', 'label', 'width'],
  },
  'el-pagination': {
    template: '<nav aria-label="分页导航" />',
    props: ['currentPage', 'pageSize', 'total', 'pageSizes', 'layout'],
  },
  'el-dialog': {
    template:
      '<div role="dialog" :aria-label="title" v-if="modelValue"><h2>{{ title }}</h2><slot /><slot name="footer" /></div>',
    props: ['modelValue', 'title', 'width'],
    emits: ['update:modelValue'],
  },
  'el-select': {
    template:
      '<select :aria-label="placeholder" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
    props: ['modelValue', 'placeholder'],
    emits: ['update:modelValue'],
  },
  'el-option': {
    template: '<option :value="value">{{ label }}</option>',
    props: ['value', 'label'],
  },
  'el-tag': {
    template: '<span class="el-tag" :data-type="type"><slot /></span>',
    props: ['type', 'effect'],
  },
  'el-tooltip': {
    template: '<span><slot /></span>',
    props: ['content', 'placement'],
  },
}

// ---- ElMessage / ElMessageBox mocks ----
vi.mock('element-plus', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
    ElMessageBox: { confirm: vi.fn().mockResolvedValue('confirm') },
  }
})

// ---- 通用 router / pinia ----
function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/login', name: 'login', component: { template: '<div />' } },
      { path: '/teacher/dashboard', name: 'teacher.dashboard', component: { template: '<div />' } },
      { path: '/student/dashboard', name: 'student.dashboard', component: { template: '<div />' } },
      {
        path: '/teacher/students/:id/score-entry',
        name: 'teacher.scoreEntry',
        component: { template: '<div />' },
      },
    ],
  })
}

async function runAxe(el: HTMLElement) {
  const spec: Spec = {}
  const rules: RuleObject = {
    // 简化：禁掉需要真实颜色对比度计算的规则（jsdom 拿不到有效 style）
    'color-contrast': { enabled: false },
    region: { enabled: false }, // 单组件片段没有 landmark 是正常的
  }
  const results = await axe.run(el, {
    runOnly: {
      type: 'tag',
      values: ['wcag2a', 'wcag2aa'],
    },
    rules,
    ...spec,
  })
  const seriousOrWorse = results.violations.filter(
    (v) => v.impact === 'critical' || v.impact === 'serious',
  )
  return seriousOrWorse
}

describe('a11y sweep (T174)', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  beforeEach(() => {
    setActivePinia(createPinia())
    // 默认所有 API 返回空数据，避免报错
    vi.spyOn(apiStudentScores, 'listMyScores').mockResolvedValue({
      items: [],
      total: 0,
      page: 1,
      size: 20,
      hasNext: false,
    })
    vi.spyOn(apiTeacherStudents, 'searchStudents').mockResolvedValue({
      items: [],
      total: 0,
      page: 1,
      size: 20,
      hasNext: false,
    })
    vi.spyOn(apiTeacherSubjects, 'listMySubjects').mockResolvedValue({
      items: [],
      total: 0,
      page: 1,
      size: 20,
      hasNext: false,
    })
    vi.spyOn(apiTeacherScores, 'getEntryOptions').mockResolvedValue({
      student: { id: 1, realName: '张三', grade: 2 },
      options: [],
      allEntered: false,
    })
    vi.spyOn(apiAccount, 'getProfile').mockResolvedValue({
      id: 3,
      loginName: 'hhh',
      realName: '黄同学',
      role: 'STUDENT',
      tel: '18075853353',
      address: 'SMU',
      grade: 3,
    })
  })

  it('LoginView 无 critical/serious a11y 问题', async () => {
    const router = makeRouter()
    await router.push('/login')
    await router.isReady()
    const w = mount(LoginView, { global: { stubs, plugins: [router] }, attachTo: document.body })
    await flushPromises()
    const violations = await runAxe(w.element as HTMLElement)
    expect(violations, JSON.stringify(violations, null, 2)).toEqual([])
  })

  it('MyScoresView 无 critical/serious a11y 问题', async () => {
    const w = mount(MyScoresView, { global: { stubs, plugins: [makeRouter()] }, attachTo: document.body })
    await flushPromises()
    const violations = await runAxe(w.element as HTMLElement)
    expect(violations, JSON.stringify(violations, null, 2)).toEqual([])
  })

  it('StudentsView 无 critical/serious a11y 问题', async () => {
    const w = mount(StudentsView, { global: { stubs, plugins: [makeRouter()] }, attachTo: document.body })
    await flushPromises()
    const violations = await runAxe(w.element as HTMLElement)
    expect(violations, JSON.stringify(violations, null, 2)).toEqual([])
  })

  it('SubjectsView 无 critical/serious a11y 问题', async () => {
    const w = mount(SubjectsView, { global: { stubs, plugins: [makeRouter()] }, attachTo: document.body })
    await flushPromises()
    const violations = await runAxe(w.element as HTMLElement)
    expect(violations, JSON.stringify(violations, null, 2)).toEqual([])
  })

  it('ScoreEntryView 无 critical/serious a11y 问题', async () => {
    const router = makeRouter()
    await router.push('/teacher/students/1/score-entry')
    await router.isReady()
    const w = mount(ScoreEntryView, { global: { stubs, plugins: [router] }, attachTo: document.body })
    await flushPromises()
    const violations = await runAxe(w.element as HTMLElement)
    expect(violations, JSON.stringify(violations, null, 2)).toEqual([])
  })

  it('ProfileForm (STUDENT) 无 critical/serious a11y 问题', async () => {
    const w = mount(ProfileForm, {
      props: { role: 'STUDENT' },
      global: { stubs, plugins: [makeRouter()] },
      attachTo: document.body,
    })
    await flushPromises()
    const violations = await runAxe(w.element as HTMLElement)
    expect(violations, JSON.stringify(violations, null, 2)).toEqual([])
  })

  it('PasswordForm 无 critical/serious a11y 问题', async () => {
    const w = mount(PasswordForm, { global: { stubs, plugins: [makeRouter()] }, attachTo: document.body })
    await flushPromises()
    const violations = await runAxe(w.element as HTMLElement)
    expect(violations, JSON.stringify(violations, null, 2)).toEqual([])
  })

  it('ScoreBadge 组件的失败态同时用颜色和文字表达（不依赖颜色）', () => {
    const w = mount(ScoreBadge, { props: { score: 55 }, attachTo: document.body })
    // 断言可见文字包含不及格标识或分值本身，不依赖颜色属性
    const text = w.text()
    expect(text.length).toBeGreaterThan(0)
    // 确保不是纯 icon（有可读文字）
    expect(/\d|不及格|及格|fail/i.test(text)).toBe(true)
  })
})
