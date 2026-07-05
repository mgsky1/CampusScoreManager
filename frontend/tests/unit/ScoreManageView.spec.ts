import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { nextTick } from 'vue'
import ScoreManageView from '@/views/teacher/ScoreManageView.vue'
import * as api from '@/api/teacherStudents'

// Element Plus 组件 stubs（保持测试轻量）
const stubs = {
  'el-input': {
    template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" class="el-input" />',
    props: ['modelValue', 'placeholder', 'clearable'],
    emits: ['update:modelValue'],
  },
  'el-button': {
    template: '<button class="el-button" @click="$emit(\'click\')" ><slot /></button>',
    props: ['type', 'link'],
    emits: ['click'],
  },
  'el-table': {
    template: '<div class="el-table"><slot :row="{ id: 3, realName: \'黄同学\', grade: 3 }" /></div>',
    props: ['data'],
  },
  'el-table-column': {
    template: '<div class="el-table-column"><slot :row="{ id: 3, realName: \'黄同学\', grade: 3, loginName: \'hhh\', tel: \'138\', address: \'SMU\' }" /></div>',
    props: ['prop', 'label'],
  },
  'el-pagination': {
    template: '<div class="el-pagination" />',
    props: ['currentPage', 'pageSize', 'total', 'pageSizes', 'layout'],
  },
}

function mockSearch(items: Array<Record<string, unknown>> = []) {
  return vi.spyOn(api, 'searchStudents').mockResolvedValue({
    items: items as never,
    total: items.length,
    page: 1,
    size: 20,
    hasNext: false,
  })
}

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/teacher/score-manage', component: ScoreManageView, name: 'sm' },
      { path: '/teacher/students/:id/score-detail', name: 'teacher.scoreDetail', component: { template: '<div />' } },
      { path: '/teacher/students/:id/score-entry', name: 'teacher.scoreEntry', component: { template: '<div />' } },
    ],
  })
}

describe('ScoreManageView.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders student table when data returned', async () => {
    const spy = mockSearch([
      { id: 3, loginName: 'hhh', realName: '黄同学', tel: '138', address: 'SMU', grade: 3 },
    ])
    const router = makeRouter()
    router.push('/teacher/score-manage')
    await router.isReady()
    const wrapper = mount(ScoreManageView, { global: { plugins: [router], stubs } })
    await flushPromises()
    await nextTick()
    expect(spy).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('成绩管理')
    expect(wrapper.findAll('.el-table-column').length).toBeGreaterThan(0)
  })

  it('shows empty state when no students', async () => {
    mockSearch([])
    const router = makeRouter()
    router.push('/teacher/score-manage')
    await router.isReady()
    const wrapper = mount(ScoreManageView, { global: { plugins: [router], stubs } })
    await flushPromises()
    await nextTick()
    expect(wrapper.text()).toContain('暂无学生')
  })

  it('search triggers API call with keyword', async () => {
    const spy = mockSearch([])
    const router = makeRouter()
    router.push('/teacher/score-manage')
    await router.isReady()
    const wrapper = mount(ScoreManageView, { global: { plugins: [router], stubs } })
    await flushPromises()
    spy.mockClear()

    const input = wrapper.find('input.el-input')
    await input.setValue('张')
    // 点击搜索按钮触发
    const searchBtn = wrapper.findAll('button.el-button').find((b) => b.text().includes('搜索'))
    if (searchBtn) {
      await searchBtn.trigger('click')
    }
    await flushPromises()

    expect(spy).toHaveBeenCalled()
    const lastCall = spy.mock.calls.at(-1)![0]
    expect(lastCall).toMatchObject({ keyword: '张', page: 1 })
  })

  it('click "查看成绩" navigates to score-detail', async () => {
    mockSearch([
      { id: 3, loginName: 'hhh', realName: '黄同学', tel: '138', address: 'SMU', grade: 3 },
    ])
    const router = makeRouter()
    const pushSpy = vi.spyOn(router, 'push')
    router.push('/teacher/score-manage')
    await router.isReady()
    const wrapper = mount(ScoreManageView, { global: { plugins: [router], stubs } })
    await flushPromises()

    const btns = wrapper.findAll('button.el-button')
    const detailBtn = btns.find((b) => b.text().includes('查看成绩'))
    expect(detailBtn).toBeTruthy()
    await detailBtn!.trigger('click')
    expect(pushSpy).toHaveBeenCalledWith(expect.objectContaining({
      name: 'teacher.scoreDetail',
      params: { id: 3 },
    }))
  })

  it('click "录入成绩" navigates to score-entry', async () => {
    mockSearch([
      { id: 3, loginName: 'hhh', realName: '黄同学', tel: '138', address: 'SMU', grade: 3 },
    ])
    const router = makeRouter()
    const pushSpy = vi.spyOn(router, 'push')
    router.push('/teacher/score-manage')
    await router.isReady()
    const wrapper = mount(ScoreManageView, { global: { plugins: [router], stubs } })
    await flushPromises()

    const btns = wrapper.findAll('button.el-button')
    const entryBtn = btns.find((b) => b.text().includes('录入成绩'))
    expect(entryBtn).toBeTruthy()
    await entryBtn!.trigger('click')
    expect(pushSpy).toHaveBeenCalledWith(expect.objectContaining({
      name: 'teacher.scoreEntry',
      params: { id: 3 },
    }))
  })
})
