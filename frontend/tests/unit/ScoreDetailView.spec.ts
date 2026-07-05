import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { defineComponent, h, inject, nextTick, provide } from 'vue'
import ScoreDetailView from '@/views/teacher/ScoreDetailView.vue'
import * as api from '@/api/teacherScores'
import * as tsApi from '@/api/teacherStudents'
import { ApiError } from '@/utils/http'
import { ApiErrorCode, type TeacherScoreItem } from '@/types/api'

const ELTable = defineComponent({
  props: { data: { type: Array, default: () => [] } },
  setup(props, { slots }) {
    provide('__rows', props.data)
    return () => h('div', { class: 'el-table' }, slots.default ? slots.default() : [])
  },
})

const ELTableColumn = defineComponent({
  props: { prop: String, label: String },
  setup(_, { slots }) {
    const rows = inject<unknown[]>('__rows', [])
    return () =>
      h(
        'div',
        { class: 'el-table-column' },
        rows.map((row, i) =>
          h(
            'div',
            { class: 'el-table-cell', key: i },
            slots.default ? slots.default({ row }) : [],
          ),
        ),
      )
  },
})

const ELButton = defineComponent({
  props: { type: String, link: Boolean, disabled: Boolean },
  emits: ['click'],
  setup(props, { slots, emit }) {
    return () =>
      h(
        'button',
        {
          class: 'el-button' + (props.disabled ? ' is-disabled' : ''),
          disabled: props.disabled || undefined,
          onClick: () => !props.disabled && emit('click'),
        },
        slots.default ? slots.default() : [],
      )
  },
})

const stubs = {
  'el-input-number': {
    template:
      '<input type="number" class="el-input-number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />',
    props: ['modelValue', 'min', 'max', 'step', 'precision', 'controlsPosition'],
    emits: ['update:modelValue'],
  },
  'el-button': ELButton,
  'el-table': ELTable,
  'el-table-column': ELTableColumn,
  'el-tooltip': {
    template: '<span class="el-tooltip"><slot /></span>',
    props: ['content', 'disabled'],
  },
  'el-dialog': {
    template: '<div class="el-dialog" v-if="modelValue"><slot /><slot name="footer" /></div>',
    props: ['modelValue', 'title'],
    emits: ['update:modelValue'],
  },
  'el-form': {
    template: '<form class="el-form"><slot /></form>',
    props: ['model', 'rules'],
  },
  'el-form-item': {
    template:
      '<div class="el-form-item"><slot /><span v-if="error" class="csm-field-error">{{ error }}</span></div>',
    props: ['label', 'prop', 'error'],
  },
  'el-pagination': {
    template: '<div class="el-pagination" />',
    props: ['currentPage', 'pageSize', 'total', 'pageSizes', 'layout'],
  },
}

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/teacher/students/:id/score-detail', name: 'teacher.scoreDetail', component: ScoreDetailView },
    ],
  })
}

async function mountView(routeId = 3, items: TeacherScoreItem[] = sample) {
  vi.spyOn(tsApi, 'getStudent').mockResolvedValue({
    id: 3, loginName: 'hhh', realName: '黄同学', tel: '138', address: 'SMU', grade: 3,
  })
  vi.spyOn(api, 'listStudentScores').mockResolvedValue({
    items, total: items.length, page: 1, size: 20, hasNext: false,
  })
  const router = makeRouter()
  router.push(`/teacher/students/${routeId}/score-detail`)
  await router.isReady()
  const wrapper = mount(ScoreDetailView, { global: { plugins: [router], stubs } })
  await flushPromises()
  await nextTick()
  return wrapper
}

const sample: TeacherScoreItem[] = [
  { id: 11, subjectId: 5, subjectName: 'Java EE', grade: 3, score: 99, isFailing: false, editable: true, updatedAt: '2026-07-04T12:00:00' },
  { id: 12, subjectId: 9, subjectName: '旧课程', grade: 1, score: 55, isFailing: true, editable: false, updatedAt: '2024-06-01T12:00:00' },
]

describe('ScoreDetailView.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.restoreAllMocks()
  })

  it('editable=false row has disabled 修改 button', async () => {
    const wrapper = await mountView()
    const modifyBtns = wrapper.findAll('button.el-button').filter((b) => b.text().includes('修改'))
    expect(modifyBtns.length).toBe(2)
    expect(modifyBtns[0].attributes('disabled')).toBeUndefined() // editable true
    expect(modifyBtns[1].attributes('disabled')).toBe('') // editable false
  })

  it('click 修改 opens dialog', async () => {
    const wrapper = await mountView()
    const enabled = wrapper.findAll('button.el-button').filter((b) => b.text().includes('修改'))[0]
    await enabled.trigger('click')
    await nextTick()
    expect(wrapper.find('.el-dialog').exists()).toBe(true)
  })

  it('submitting invalid score (-1) blocks API call and shows field error', async () => {
    const spy = vi.spyOn(api, 'updateScore')
    const wrapper = await mountView()
    const enabled = wrapper.findAll('button.el-button').filter((b) => b.text().includes('修改'))[0]
    await enabled.trigger('click')
    await nextTick()

    const input = wrapper.find('input.el-input-number')
    await input.setValue(-1)
    const submit = wrapper.findAll('button.el-button').find((b) => b.text().includes('确定'))
    await submit!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toMatch(/成绩必须在 0[-–]100/)
    expect(spy).not.toHaveBeenCalled()
  })

  it('submitting invalid score (101) blocks API call and shows field error', async () => {
    const spy = vi.spyOn(api, 'updateScore')
    const wrapper = await mountView()
    const enabled = wrapper.findAll('button.el-button').filter((b) => b.text().includes('修改'))[0]
    await enabled.trigger('click')
    await nextTick()

    const input = wrapper.find('input.el-input-number')
    await input.setValue(101)
    const submit = wrapper.findAll('button.el-button').find((b) => b.text().includes('确定'))
    await submit!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toMatch(/成绩必须在 0[-–]100/)
    expect(spy).not.toHaveBeenCalled()
  })

  it('submitting valid score (90) calls updateScore and reloads', async () => {
    const wrapper = await mountView()
    const updateSpy = vi.spyOn(api, 'updateScore').mockResolvedValue({
      id: 11, subjectId: 5, subjectName: 'Java EE', grade: 3, score: 90,
      isFailing: false, editable: true,
    })
    const listSpy = vi.spyOn(api, 'listStudentScores')
    listSpy.mockClear()

    const enabled = wrapper.findAll('button.el-button').filter((b) => b.text().includes('修改'))[0]
    await enabled.trigger('click')
    await nextTick()

    const input = wrapper.find('input.el-input-number')
    await input.setValue(90)
    const submit = wrapper.findAll('button.el-button').find((b) => b.text().includes('确定'))
    await submit!.trigger('click')
    await flushPromises()

    expect(updateSpy).toHaveBeenCalledWith(11, { score: 90 })
    expect(listSpy).toHaveBeenCalled()
  })

  it('server 2301 error keeps dialog open', async () => {
    const wrapper = await mountView()
    vi.spyOn(api, 'updateScore').mockRejectedValue(
      new ApiError(ApiErrorCode.SCORE_OUT_OF_RANGE, 'oor', 400),
    )
    const enabled = wrapper.findAll('button.el-button').filter((b) => b.text().includes('修改'))[0]
    await enabled.trigger('click')
    await nextTick()

    const input = wrapper.find('input.el-input-number')
    await input.setValue(50)
    const submit = wrapper.findAll('button.el-button').find((b) => b.text().includes('确定'))
    await submit!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.el-dialog').exists()).toBe(true)
  })
})
