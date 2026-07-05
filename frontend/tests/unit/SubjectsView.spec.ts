import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { nextTick } from 'vue'
import SubjectsView from '@/views/teacher/SubjectsView.vue'
import * as api from '@/api/teacherSubjects'
import { ApiError } from '@/utils/http'
import { ApiErrorCode } from '@/types/api'

// ---------- Element Plus stubs ----------
const stubs = {
  'el-input': {
    template:
      '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" class="el-input" :placeholder="placeholder" />',
    props: ['modelValue', 'placeholder', 'clearable'],
    emits: ['update:modelValue'],
  },
  'el-input-number': {
    template:
      '<input type="number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" class="el-input-number" />',
    props: ['modelValue', 'min', 'max'],
    emits: ['update:modelValue'],
  },
  'el-button': {
    template:
      '<button class="el-button" :data-type="type" @click="$emit(\'click\', $event)"><slot /></button>',
    props: ['type', 'link', 'nativeType'],
    emits: ['click'],
  },
  'el-form': {
    template: '<form class="el-form" @submit.prevent="$emit(\'submit\')"><slot /></form>',
    props: ['model', 'inline', 'labelWidth'],
    emits: ['submit'],
  },
  'el-form-item': {
    template:
      '<div class="el-form-item" :data-label="label" :data-error="error"><slot />' +
      '<span v-if="error" class="el-form-item__error">{{ error }}</span></div>',
    props: ['label', 'prop', 'error'],
  },
  'el-table': {
    template: '<div class="el-table"><slot :row="firstRow" /></div>',
    props: ['data'],
    computed: {
      firstRow(this: { data?: unknown[] }): unknown {
        return this.data && this.data[0] ? this.data[0] : {}
      },
    },
  },
  'el-table-column': {
    template: '<div class="el-table-column" :data-label="label"><slot :row="$parent.firstRow" /></div>',
    props: ['prop', 'label', 'width'],
  },
  'el-pagination': {
    template: '<div class="el-pagination" />',
    props: ['currentPage', 'pageSize', 'total', 'pageSizes', 'layout'],
  },
  'el-dialog': {
    template:
      '<div class="el-dialog" v-if="modelValue"><div class="el-dialog__title">{{ title }}</div><slot /><slot name="footer" /></div>',
    props: ['modelValue', 'title', 'width'],
    emits: ['update:modelValue'],
  },
}

// ---------- ElMessage / ElMessageBox mocks (hoisted) ----------
const { messageMocks, messageBoxConfirm } = vi.hoisted(() => ({
  messageMocks: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
  messageBoxConfirm: vi.fn(),
}))
vi.mock('element-plus', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('element-plus')
  return {
    ...actual,
    ElMessage: messageMocks,
    ElMessageBox: { confirm: messageBoxConfirm },
  }
})

// ---------- helpers ----------
function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/teacher/subjects', component: SubjectsView, name: 'teacher.subjects' }],
  })
}

function makeSubject(overrides: Partial<Record<string, unknown>> = {}) {
  return { id: 5, name: 'Java EE', grade: 3, ...overrides }
}

function mockList(items: Array<Record<string, unknown>> = []) {
  return vi.spyOn(api, 'listMySubjects').mockResolvedValue({
    items: items as never,
    total: items.length,
    page: 1,
    size: 20,
    hasNext: false,
  })
}

async function mountView() {
  const router = makeRouter()
  router.push('/teacher/subjects')
  await router.isReady()
  const wrapper = mount(SubjectsView, { global: { plugins: [router], stubs } })
  await flushPromises()
  await nextTick()
  return { wrapper, router }
}

describe('SubjectsView.vue (T138)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.restoreAllMocks()
    messageMocks.success.mockReset()
    messageMocks.error.mockReset()
    messageMocks.warning.mockReset()
    messageBoxConfirm.mockReset()
  })

  it('renders subject list', async () => {
    const spy = mockList([makeSubject()])
    const { wrapper } = await mountView()
    expect(spy).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('我的课程')
  })

  it('shows empty state when list empty', async () => {
    mockList([])
    const { wrapper } = await mountView()
    expect(wrapper.text()).toContain('暂无课程')
  })

  it('clicking "新增" opens create dialog', async () => {
    mockList([])
    const { wrapper } = await mountView()
    const addBtn = wrapper
      .findAll('button.el-button')
      .find((b) => b.text().includes('新增'))
    await addBtn!.trigger('click')
    await nextTick()
    expect(wrapper.find('.el-dialog').exists()).toBe(true)
    expect(wrapper.find('.el-dialog').text()).toContain('新增课程')
  })

  it('create validates name required', async () => {
    mockList([])
    const createSpy = vi.spyOn(api, 'createSubject').mockResolvedValue(makeSubject() as never)
    const { wrapper } = await mountView()
    await wrapper.findAll('button.el-button').find((b) => b.text().includes('新增'))!.trigger('click')
    await nextTick()

    // 直接提交（未填 name）
    const dlg = wrapper.find('.el-dialog')
    await dlg.findAll('button.el-button').find((b) => b.text().includes('提交'))!.trigger('click')
    await flushPromises()

    expect(createSpy).not.toHaveBeenCalled()
    expect(wrapper.text()).toMatch(/课程名不能为空|不能为空/)
  })

  it('create submits payload and reloads', async () => {
    const listSpy = mockList([])
    const createSpy = vi.spyOn(api, 'createSubject').mockResolvedValue(makeSubject() as never)
    const { wrapper } = await mountView()
    listSpy.mockClear()

    await wrapper.findAll('button.el-button').find((b) => b.text().includes('新增'))!.trigger('click')
    await nextTick()

    const dlg = wrapper.find('.el-dialog')
    await dlg.find('input.el-input').setValue('云计算')
    await dlg.find('input.el-input-number').setValue('3')
    await dlg.findAll('button.el-button').find((b) => b.text().includes('提交'))!.trigger('click')
    await flushPromises()

    expect(createSpy).toHaveBeenCalledWith({ name: '云计算', grade: 3 })
    expect(listSpy).toHaveBeenCalled()
  })

  it('maps 2201 SUBJECT_DUPLICATE_FOR_TEACHER on create', async () => {
    mockList([])
    vi.spyOn(api, 'createSubject').mockRejectedValue(
      new ApiError(
        ApiErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER,
        '该教师已存在同名课程',
        409,
      ),
    )
    const { wrapper } = await mountView()
    await wrapper.findAll('button.el-button').find((b) => b.text().includes('新增'))!.trigger('click')
    await nextTick()

    const dlg = wrapper.find('.el-dialog')
    await dlg.find('input.el-input').setValue('Java EE')
    await dlg.find('input.el-input-number').setValue('3')
    await dlg.findAll('button.el-button').find((b) => b.text().includes('提交'))!.trigger('click')
    await flushPromises()

    const fieldShown = wrapper.text().match(/该教师已存在同名课程|已存在同名/)
    const globalShown = messageMocks.error.mock.calls.flat().some(
      (m) => typeof m === 'string' && /该教师已存在同名课程|已存在同名/.test(m),
    )
    expect(fieldShown || globalShown).toBeTruthy()
  })

  it('maps 2201 SUBJECT_DUPLICATE_FOR_TEACHER on update', async () => {
    mockList([makeSubject()])
    vi.spyOn(api, 'updateSubject').mockRejectedValue(
      new ApiError(
        ApiErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER,
        '该教师已存在同名课程',
        409,
      ),
    )
    const { wrapper } = await mountView()

    const editBtn = wrapper.findAll('button.el-button').find((b) => b.text().includes('编辑'))
    await editBtn!.trigger('click')
    await nextTick()
    const dlg = wrapper.find('.el-dialog')
    expect(dlg.text()).toContain('编辑课程')

    await dlg.find('input.el-input').setValue('计算机导论')
    await dlg.findAll('button.el-button').find((b) => b.text().includes('提交'))!.trigger('click')
    await flushPromises()

    const shown = wrapper.text().match(/已存在同名|该教师已存在同名/) ||
      messageMocks.error.mock.calls.flat().some((m) => typeof m === 'string' && /已存在同名/.test(m))
    expect(shown).toBeTruthy()
  })

  it('delete: cancel in ConfirmDialog does NOT call API', async () => {
    mockList([makeSubject()])
    const delSpy = vi.spyOn(api, 'deleteSubject').mockResolvedValue(undefined as never)
    messageBoxConfirm.mockRejectedValue(new Error('cancel'))
    const { wrapper } = await mountView()

    const delBtn = wrapper.findAll('button.el-button').find((b) => b.text().includes('删除'))
    await delBtn!.trigger('click')
    await flushPromises()

    expect(messageBoxConfirm).toHaveBeenCalled()
    expect(delSpy).not.toHaveBeenCalled()
  })

  it('delete: confirm calls API and reloads', async () => {
    const listSpy = mockList([makeSubject()])
    const delSpy = vi.spyOn(api, 'deleteSubject').mockResolvedValue(undefined as never)
    messageBoxConfirm.mockResolvedValue('confirm')
    const { wrapper } = await mountView()
    listSpy.mockClear()

    const delBtn = wrapper.findAll('button.el-button').find((b) => b.text().includes('删除'))
    await delBtn!.trigger('click')
    await flushPromises()

    expect(delSpy).toHaveBeenCalledWith(5)
    expect(listSpy).toHaveBeenCalled()
  })

  it('delete: 2202 SUBJECT_HAS_SCORES shows friendly message', async () => {
    mockList([makeSubject()])
    vi.spyOn(api, 'deleteSubject').mockRejectedValue(
      new ApiError(ApiErrorCode.SUBJECT_HAS_SCORES, '该课程存在成绩记录', 400),
    )
    messageBoxConfirm.mockResolvedValue('confirm')
    const { wrapper } = await mountView()

    const delBtn = wrapper.findAll('button.el-button').find((b) => b.text().includes('删除'))
    await delBtn!.trigger('click')
    await flushPromises()

    const shown = messageMocks.error.mock.calls.flat().some(
      (m) => typeof m === 'string' && /该课程存在成绩记录|请先删除相关成绩/.test(m),
    )
    expect(shown).toBeTruthy()
  })
})
