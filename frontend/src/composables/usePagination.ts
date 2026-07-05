import { reactive, computed, toRefs } from 'vue'

export interface PaginationState {
  page: number
  size: number
  keyword: string
  sort: string
}

export interface UsePaginationOptions {
  defaultSize?: number
  defaultSort?: string
}

/**
 * 分页 / 排序 / 搜索的 UI 状态封装。绑定到 <el-pagination> 与检索表单。
 * 只管本地状态；数据加载由调用方通过 `watch` state 触发。
 */
export function usePagination(opts: UsePaginationOptions = {}) {
  const state = reactive<PaginationState>({
    page: 1,
    size: opts.defaultSize ?? 20,
    keyword: '',
    sort: opts.defaultSort ?? '',
  })

  function reset(): void {
    state.page = 1
    state.size = opts.defaultSize ?? 20
    state.keyword = ''
    state.sort = opts.defaultSort ?? ''
  }

  function toPage(n: number): void {
    state.page = Math.max(1, Math.floor(n))
  }

  function toSize(n: number): void {
    state.size = Math.min(100, Math.max(1, Math.floor(n)))
    state.page = 1
  }

  function search(kw: string): void {
    state.keyword = kw
    state.page = 1
  }

  function setSort(field: string, dir: 'asc' | 'desc'): void {
    state.sort = `${field}:${dir}`
    state.page = 1
  }

  /** 绑定 el-pagination 常用属性 */
  const elPaginationBind = computed(() => ({
    currentPage: state.page,
    pageSize: state.size,
    pageSizes: [10, 20, 50, 100],
    layout: 'total, sizes, prev, pager, next, jumper',
    background: true,
  }))

  return {
    ...toRefs(state),
    state,
    reset,
    toPage,
    toSize,
    search,
    setSort,
    elPaginationBind,
  }
}
