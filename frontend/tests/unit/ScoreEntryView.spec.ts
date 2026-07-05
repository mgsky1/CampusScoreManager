import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { defineComponent, h, nextTick } from 'vue'
import ScoreEntryView from '@/views/teacher/ScoreEntryView.vue'
import * as api from '@/api/teacherScores'
import { ApiError } from '@/utils/http'
import { ApiErrorCode } from '@/types/api'

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
  'el-button': ELButton,
  'el-select': {
    template:
      '<select class="el-select" :value="modelValue" @change="$emit(\'update:modelValue\', Number($event.target.value))"><slot /></select>',
    props: ['modelValue', 'placeholder'],
    emits: ['update:modelValue'],
  },
  'el-option': {
    template: '<option class="el-option" :value="value">{{ label }}</option>',
    props: ['value', 'label'],
  },
  'el-input-number': {
    template:
      '<input type="number" class="el-input-number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />',
    props: ['modelValue', 'min', 'max', 'step', 'precision', 'controlsPosition'],
    emits: ['update:modelValue'],
  },
  'el-form': { template: '<form class="el-form"><slot /></form>', props: ['model', 'rules'] },
  'el-form-item': {
    template:
      '<div class="el-form-item"><slot /><span v-if="error" class="csm-field-error">{{ error }}</span></div>',
    props: ['label', 'prop', 'error'],
  },
  'el-alert': {
    template: '<div class="el-alert" :data-type="type">{{ title }} <slot /></div>',
    props: ['title', 'type', 'closable', 'showIcon'],
  },
  'el-descriptions': {
    template: '<div class="el-descriptions"><slot /></div>',
    props: ['column', 'border'],
  },
  'el-descriptions-item': {
    template: '<div class="el-descriptions-item"><span class="label">{{ label }}: </span><slot /></div>',
    props: ['label'],
  },
}

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/teacher/students/:id/score-entry', name: 'teacher.scoreEntry', component: ScoreEntryView },
      { path: '/teacher/students/:id/score-detail', name: 'teacher.scoreDetail', component: { template: '<div />' } },
      { path: '/teacher/score-manage', name: 'teacher.scoreManage', component: { template: '<div />' } },
    ],
  })
}

async function mountView(entryResp: {
  student: { id: number; realName: string; grade: number }
  options: Array<{ subjectId: number; subjectName: string; grade: number }>
  allEntered: boolean
}) {
  vi.spyOn(api, 'getEntryOptions').mockResolvedValue(entryResp)
  const router = makeRouter()
  router.push(`/teacher/students/${entryResp.student.id}/score-entry`)
  await router.isReady()
  const wrapper = mount(ScoreEntryView, { global: { plugins: [router], stubs } })
  await flushPromises()
  await nextTick()
  return { wrapper, router }
}

describe('ScoreEntryView.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.restoreAllMocks()
  })

  it('shows form when options are available', async () => {
    const { wrapper } = await mountView({
      student: { id: 3, realName: '黄同学', grade: 3 },
      options: [
        { subjectId: 5, subjectName: 'Java EE', grade: 3 },
        { subjectId: 6, subjectName: '云计算', grade: 3 },
      ],
      allEntered: false,
    })
    expect(wrapper.find('select.el-select').exists()).toBe(true)
    expect(wrapper.findAll('option.el-option').length).toBe(2)
    expect(wrapper.find('input.el-input-number').exists()).toBe(true)
  })

  it('shows "全部录入完毕" alert when allEntered=true', async () => {
    const { wrapper } = await mountView({
      student: { id: 3, realName: '黄同学', grade: 3 },
      options: [],
      allEntered: true,
    })
    expect(wrapper.text()).toContain('全部录入完毕')
    expect(wrapper.find('select.el-select').exists()).toBe(false)
  })

  it('shows EmptyState when options=[] && allEntered=false', async () => {
    const { wrapper } = await mountView({
      student: { id: 3, realName: '黄同学', grade: 3 },
      options: [],
      allEntered: false,
    })
    expect(wrapper.text()).toContain('暂无可录入的课程')
  })

  it('submits valid score and navigates to detail', async () => {
    const createSpy = vi.spyOn(api, 'createScore').mockResolvedValue({
      id: 101, subjectId: 5, subjectName: 'Java EE', grade: 3, score: 88,
      isFailing: false, editable: true,
    })
    const { wrapper, router } = await mountView({
      student: { id: 3, realName: '黄同学', grade: 3 },
      options: [{ subjectId: 5, subjectName: 'Java EE', grade: 3 }],
      allEntered: false,
    })
    const pushSpy = vi.spyOn(router, 'push')

    // 选择课程（stub 已经自动 emit）
    const select = wrapper.find('select.el-select')
    await select.setValue('5')
    const input = wrapper.find('input.el-input-number')
    await input.setValue(88)
    const submit = wrapper.findAll('button.el-button').find((b) => b.text().includes('提交'))
    expect(submit).toBeTruthy()
    await submit!.trigger('click')
    await flushPromises()

    expect(createSpy).toHaveBeenCalledWith(3, { subjectId: 5, score: 88 })
    expect(pushSpy).toHaveBeenCalledWith(expect.objectContaining({
      name: 'teacher.scoreDetail',
      params: { id: 3 },
    }))
  })

  it('field validation blocks submit on out-of-range', async () => {
    const createSpy = vi.spyOn(api, 'createScore')
    const { wrapper } = await mountView({
      student: { id: 3, realName: '黄同学', grade: 3 },
      options: [{ subjectId: 5, subjectName: 'Java EE', grade: 3 }],
      allEntered: false,
    })
    const select = wrapper.find('select.el-select')
    await select.setValue('5')
    const input = wrapper.find('input.el-input-number')
    await input.setValue(101)
    const submit = wrapper.findAll('button.el-button').find((b) => b.text().includes('提交'))
    await submit!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toMatch(/成绩必须在 0[-–]100/)
    expect(createSpy).not.toHaveBeenCalled()
  })

  it('server 2302 (duplicate) keeps on page with error surfaced', async () => {
    vi.spyOn(api, 'createScore').mockRejectedValue(
      new ApiError(ApiErrorCode.SCORE_ALREADY_EXISTS, 'dup', 409),
    )
    const { wrapper, router } = await mountView({
      student: { id: 3, realName: '黄同学', grade: 3 },
      options: [{ subjectId: 5, subjectName: 'Java EE', grade: 3 }],
      allEntered: false,
    })
    const pushSpy = vi.spyOn(router, 'push')

    const select = wrapper.find('select.el-select')
    await select.setValue('5')
    const input = wrapper.find('input.el-input-number')
    await input.setValue(88)
    const submit = wrapper.findAll('button.el-button').find((b) => b.text().includes('提交'))
    await submit!.trigger('click')
    await flushPromises()

    expect(pushSpy).not.toHaveBeenCalled()
  })
})
