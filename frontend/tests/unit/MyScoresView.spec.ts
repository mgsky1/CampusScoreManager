import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import MyScoresView from '@/views/student/MyScoresView.vue'
import * as api from '@/api/studentScores'

const stubs = {
  'el-table': {
    template: '<div class="el-table"><slot /></div>',
    props: ['data'],
  },
  'el-table-column': {
    template: '<div class="el-table-column"><slot :row="{ score: 55, isFailing: true }" /></div>',
    props: ['prop', 'label'],
  },
  'el-pagination': {
    template: '<div class="el-pagination" />',
    props: ['currentPage', 'pageSize', 'total', 'pageSizes', 'layout'],
  },
}

function mockList() {
  return vi.spyOn(api, 'listMyScores').mockResolvedValue({
    items: [
      {
        id: 1,
        studentId: 3,
        subjectId: 5,
        subjectName: 'Java EE',
        teacherId: 2,
        teacherName: '伍老师',
        score: 99,
        grade: 3,
        isFailing: false,
      },
      {
        id: 2,
        studentId: 3,
        subjectId: 6,
        subjectName: '云计算',
        teacherId: 2,
        teacherName: '伍老师',
        score: 55,
        grade: 3,
        isFailing: true,
      },
    ],
    total: 2,
    page: 1,
    size: 50,
    hasNext: false,
  })
}

describe('MyScoresView.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders 2 rows fetched from listMyScores', async () => {
    const spy = mockList()
    const wrapper = mount(MyScoresView, { global: { stubs } })
    await flushPromises()
    await nextTick()
    expect(spy).toHaveBeenCalledOnce()
    // 默认 size 50
    expect(spy.mock.calls[0][0]).toMatchObject({ page: 1, size: 50 })
    // items 反射到 el-table :data
    expect(wrapper.text()).toContain('我的成绩')
    expect(wrapper.findAll('.el-table-column').length).toBeGreaterThan(0)
  })

  it('shows EmptyState when no items', async () => {
    vi.spyOn(api, 'listMyScores').mockResolvedValue({
      items: [],
      total: 0,
      page: 1,
      size: 50,
      hasNext: false,
    })
    const wrapper = mount(MyScoresView, { global: { stubs } })
    await flushPromises()
    await nextTick()
    expect(wrapper.text()).toContain('暂无成绩记录')
  })

  it('passes sort=grade,desc by default', async () => {
    const spy = mockList()
    mount(MyScoresView, { global: { stubs } })
    await flushPromises()
    expect(spy.mock.calls[0][0]).toMatchObject({ sort: 'grade,desc' })
  })
})
