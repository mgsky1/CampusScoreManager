import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useConfirm } from '@/composables/useConfirm'
import { usePagination } from '@/composables/usePagination'

const { messageBoxConfirm } = vi.hoisted(() => ({
  messageBoxConfirm: vi.fn(),
}))
vi.mock('element-plus', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('element-plus')
  return { ...actual, ElMessageBox: { confirm: messageBoxConfirm } }
})

describe('composables/useConfirm', () => {
  beforeEach(() => {
    messageBoxConfirm.mockReset()
  })

  it('resolves true when user confirms', async () => {
    messageBoxConfirm.mockResolvedValueOnce('confirm')
    const { confirm } = useConfirm()
    await expect(confirm('删除该项？')).resolves.toBe(true)
    expect(messageBoxConfirm).toHaveBeenCalledTimes(1)
  })

  it('resolves false when user cancels (ElMessageBox rejects)', async () => {
    messageBoxConfirm.mockRejectedValueOnce('cancel')
    const { confirm } = useConfirm()
    await expect(confirm('删除该项？')).resolves.toBe(false)
  })

  it('passes custom title / buttons / type through', async () => {
    messageBoxConfirm.mockResolvedValueOnce('confirm')
    const { confirm } = useConfirm()
    await confirm('清空成绩', '此操作不可撤销', {
      title: '危险操作',
      confirmText: '我知道',
      cancelText: '算了',
      type: 'error',
    })
    expect(messageBoxConfirm).toHaveBeenCalledWith('此操作不可撤销', '危险操作', {
      confirmButtonText: '我知道',
      cancelButtonText: '算了',
      type: 'error',
    })
  })

  it('uses defaults when opts omitted', async () => {
    messageBoxConfirm.mockResolvedValueOnce('confirm')
    const { confirm } = useConfirm()
    await confirm('确认？')
    expect(messageBoxConfirm).toHaveBeenCalledWith('确认？', '请确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
  })
})

describe('composables/usePagination', () => {
  it('has defaults page=1 size=20 keyword=""', () => {
    const p = usePagination()
    expect(p.state.page).toBe(1)
    expect(p.state.size).toBe(20)
    expect(p.state.keyword).toBe('')
    expect(p.state.sort).toBe('')
  })

  it('respects custom defaultSize and defaultSort', () => {
    const p = usePagination({ defaultSize: 50, defaultSort: 'created_at:desc' })
    expect(p.state.size).toBe(50)
    expect(p.state.sort).toBe('created_at:desc')
  })

  it('toPage clamps to >= 1 and floors', () => {
    const p = usePagination()
    p.toPage(5.9)
    expect(p.state.page).toBe(5)
    p.toPage(0)
    expect(p.state.page).toBe(1)
    p.toPage(-3)
    expect(p.state.page).toBe(1)
  })

  it('toSize clamps to [1, 100] and resets page', () => {
    const p = usePagination()
    p.state.page = 5
    p.toSize(200)
    expect(p.state.size).toBe(100)
    expect(p.state.page).toBe(1)
    p.toSize(0)
    expect(p.state.size).toBe(1)
  })

  it('search sets keyword and resets page', () => {
    const p = usePagination()
    p.state.page = 4
    p.search('  hello')
    expect(p.state.keyword).toBe('  hello')
    expect(p.state.page).toBe(1)
  })

  it('setSort composes "field:dir" and resets page', () => {
    const p = usePagination()
    p.state.page = 3
    p.setSort('real_name', 'asc')
    expect(p.state.sort).toBe('real_name:asc')
    expect(p.state.page).toBe(1)
    p.setSort('grade', 'desc')
    expect(p.state.sort).toBe('grade:desc')
  })

  it('reset restores defaults', () => {
    const p = usePagination({ defaultSize: 50, defaultSort: 'x:asc' })
    p.state.page = 7
    p.state.size = 10
    p.state.keyword = 'k'
    p.state.sort = 'y:desc'
    p.reset()
    expect(p.state.page).toBe(1)
    expect(p.state.size).toBe(50)
    expect(p.state.keyword).toBe('')
    expect(p.state.sort).toBe('x:asc')
  })

  it('elPaginationBind reflects current state', () => {
    const p = usePagination({ defaultSize: 30 })
    expect(p.elPaginationBind.value).toMatchObject({
      currentPage: 1,
      pageSize: 30,
      pageSizes: [10, 20, 50, 100],
      background: true,
    })
    p.toPage(4)
    expect(p.elPaginationBind.value.currentPage).toBe(4)
  })
})
