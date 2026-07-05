import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { nextTick } from 'vue'
import StudentsView from '@/views/teacher/StudentsView.vue'
import * as api from '@/api/teacherStudents'
import { ApiError } from '@/utils/http'
import { ApiErrorCode } from '@/types/api'

// ---------- Element Plus stubs ----------
const stubs = {
  'el-input': {
    template:
      '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" class="el-input" :data-name="name || placeholder" />',
    props: ['modelValue', 'placeholder', 'clearable', 'type', 'showPassword', 'name'],
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
    template: '<div class="el-dialog" v-if="modelValue"><div class="el-dialog__title">{{ title }}</div><slot /><slot name="footer" /></div>',
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
    routes: [{ path: '/teacher/students', component: StudentsView, name: 'teacher.students' }],
  })
}

function makeStudent(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 3,
    loginName: 'hhh',
    realName: '黄同学',
    tel: '13800138000',
    address: 'SMU',
    grade: 3,
    ...overrides,
  }
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

async function mountView() {
  const router = makeRouter()
  router.push('/teacher/students')
  await router.isReady()
  const wrapper = mount(StudentsView, { global: { plugins: [router], stubs } })
  await flushPromises()
  await nextTick()
  return { wrapper, router }
}

describe('StudentsView.vue (T118)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.restoreAllMocks()
    messageMocks.success.mockReset()
    messageMocks.error.mockReset()
    messageMocks.warning.mockReset()
    messageBoxConfirm.mockReset()
  })

  it('renders paged students', async () => {
    const spy = mockSearch([makeStudent()])
    const { wrapper } = await mountView()
    expect(spy).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('学生管理')
    expect(wrapper.findAll('.el-table-column').length).toBeGreaterThan(0)
  })

  it('shows empty state when list is empty', async () => {
    mockSearch([])
    const { wrapper } = await mountView()
    expect(wrapper.text()).toContain('暂无学生')
  })

  it('search passes keyword to API', async () => {
    const spy = mockSearch([])
    const { wrapper } = await mountView()
    spy.mockClear()

    const input = wrapper.find('input.el-input')
    await input.setValue('张')
    const searchBtn = wrapper
      .findAll('button.el-button')
      .find((b) => b.text().includes('搜索'))
    await searchBtn!.trigger('click')
    await flushPromises()

    expect(spy).toHaveBeenCalled()
    const lastCall = spy.mock.calls.at(-1)![0]
    expect(lastCall).toMatchObject({ keyword: '张', page: 1 })
  })

  it('clicking "新增" opens create dialog', async () => {
    mockSearch([])
    const { wrapper } = await mountView()
    const addBtn = wrapper
      .findAll('button.el-button')
      .find((b) => b.text().includes('新增'))
    expect(addBtn).toBeTruthy()
    await addBtn!.trigger('click')
    await nextTick()
    expect(wrapper.find('.el-dialog').exists()).toBe(true)
    expect(wrapper.find('.el-dialog').text()).toContain('新增学生')
  })

  it('create form rejects 7-digit tel (client-side validation)', async () => {
    mockSearch([])
    const createSpy = vi.spyOn(api, 'createStudent').mockResolvedValue(makeStudent() as never)
    const { wrapper } = await mountView()

    await wrapper.findAll('button.el-button').find((b) => b.text().includes('新增'))!.trigger('click')
    await nextTick()

    // 填合法字段 + 非法 tel
    const dlg = wrapper.find('.el-dialog')
    const inputs = dlg.findAll('input.el-input')
    // 按渲染顺序：loginName / realName / password / tel / address (grade 用 input-number)
    await inputs[0].setValue('new_stu')
    await inputs[1].setValue('新同学')
    await inputs[2].setValue('123456')
    await inputs[3].setValue('1234567') // 7 位 tel
    await inputs[4].setValue('SMU')
    await dlg.find('input.el-input-number').setValue('3')

    const submit = dlg.findAll('button.el-button').find((b) => b.text().includes('提交'))
    await submit!.trigger('click')
    await flushPromises()

    expect(createSpy).not.toHaveBeenCalled()
    expect(wrapper.text()).toMatch(/8-11 位|电话/)
  })

  it('create form rejects 12-digit tel', async () => {
    mockSearch([])
    const createSpy = vi.spyOn(api, 'createStudent').mockResolvedValue(makeStudent() as never)
    const { wrapper } = await mountView()
    await wrapper.findAll('button.el-button').find((b) => b.text().includes('新增'))!.trigger('click')
    await nextTick()

    const dlg = wrapper.find('.el-dialog')
    const inputs = dlg.findAll('input.el-input')
    await inputs[0].setValue('new_stu')
    await inputs[1].setValue('新同学')
    await inputs[2].setValue('123456')
    await inputs[3].setValue('123456789012') // 12 位
    await inputs[4].setValue('SMU')
    await dlg.find('input.el-input-number').setValue('3')

    await dlg.findAll('button.el-button').find((b) => b.text().includes('提交'))!.trigger('click')
    await flushPromises()

    expect(createSpy).not.toHaveBeenCalled()
  })

  it('create form accepts 8-11 digit tel and calls API', async () => {
    mockSearch([])
    const createSpy = vi.spyOn(api, 'createStudent').mockResolvedValue(makeStudent() as never)
    const { wrapper } = await mountView()
    await wrapper.findAll('button.el-button').find((b) => b.text().includes('新增'))!.trigger('click')
    await nextTick()

    const dlg = wrapper.find('.el-dialog')
    const inputs = dlg.findAll('input.el-input')
    await inputs[0].setValue('new_stu')
    await inputs[1].setValue('新同学')
    await inputs[2].setValue('123456')
    await inputs[3].setValue('13800138000') // 11 位合法
    await inputs[4].setValue('SMU')
    await dlg.find('input.el-input-number').setValue('3')

    await dlg.findAll('button.el-button').find((b) => b.text().includes('提交'))!.trigger('click')
    await flushPromises()

    expect(createSpy).toHaveBeenCalledOnce()
    expect(createSpy.mock.calls[0][0]).toMatchObject({
      loginName: 'new_stu',
      realName: '新同学',
      password: '123456',
      tel: '13800138000',
      address: 'SMU',
      grade: 3,
    })
  })

  it('maps 2101 LOGIN_NAME_TAKEN to friendly message on create', async () => {
    mockSearch([])
    vi.spyOn(api, 'createStudent').mockRejectedValue(
      new ApiError(ApiErrorCode.LOGIN_NAME_TAKEN, '登录名已被使用', 409),
    )
    const { wrapper } = await mountView()

    await wrapper.findAll('button.el-button').find((b) => b.text().includes('新增'))!.trigger('click')
    await nextTick()

    const dlg = wrapper.find('.el-dialog')
    const inputs = dlg.findAll('input.el-input')
    await inputs[0].setValue('dup_stu')
    await inputs[1].setValue('重名同学')
    await inputs[2].setValue('123456')
    await inputs[3].setValue('13800138000')
    await inputs[4].setValue('SMU')
    await dlg.find('input.el-input-number').setValue('3')
    await dlg.findAll('button.el-button').find((b) => b.text().includes('提交'))!.trigger('click')
    await flushPromises()

    // 期望某处出现"登录名已存在/被使用"提示（表单字段错误 或 全局 message）
    const fieldErrorShown = wrapper.text().match(/登录名已(存在|被使用)/)
    const messageErrorShown = messageMocks.error.mock.calls
      .flat()
      .some((m) => typeof m === 'string' && /登录名已(存在|被使用)/.test(m))
    expect(fieldErrorShown || messageErrorShown).toBeTruthy()
  })

  it('delete flow: cancel in ConfirmDialog does NOT call API', async () => {
    mockSearch([makeStudent()])
    const deleteSpy = vi.spyOn(api, 'deleteStudent').mockResolvedValue(undefined as never)
    messageBoxConfirm.mockRejectedValue(new Error('cancel'))
    const { wrapper } = await mountView()

    const delBtn = wrapper
      .findAll('button.el-button')
      .find((b) => b.text().includes('删除'))
    expect(delBtn).toBeTruthy()
    await delBtn!.trigger('click')
    await flushPromises()

    expect(messageBoxConfirm).toHaveBeenCalled()
    expect(deleteSpy).not.toHaveBeenCalled()
  })

  it('delete flow: confirm calls API and reloads list', async () => {
    const searchSpy = mockSearch([makeStudent()])
    const deleteSpy = vi.spyOn(api, 'deleteStudent').mockResolvedValue(undefined as never)
    messageBoxConfirm.mockResolvedValue('confirm')
    const { wrapper } = await mountView()

    searchSpy.mockClear()
    const delBtn = wrapper
      .findAll('button.el-button')
      .find((b) => b.text().includes('删除'))
    await delBtn!.trigger('click')
    await flushPromises()

    expect(deleteSpy).toHaveBeenCalledWith(3)
    // reload
    expect(searchSpy).toHaveBeenCalled()
  })
})
