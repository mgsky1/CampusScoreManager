import { ElMessageBox } from 'element-plus'

export interface ConfirmOptions {
  title?: string
  confirmText?: string
  cancelText?: string
  type?: 'warning' | 'info' | 'error' | 'success'
}

/**
 * 全局二次确认。Promise 版：resolve(true) = 确认，resolve(false) = 取消。
 * 组件里用法：
 *   const { confirm } = useConfirm()
 *   const ok = await confirm('删除该学生？', '此操作无法撤销')
 *   if (!ok) return
 */
export function useConfirm() {
  async function confirm(
    message: string,
    detail: string | undefined = undefined,
    opts: ConfirmOptions = {},
  ): Promise<boolean> {
    try {
      await ElMessageBox.confirm(detail ?? message, opts.title ?? '请确认', {
        confirmButtonText: opts.confirmText ?? '确认',
        cancelButtonText: opts.cancelText ?? '取消',
        type: opts.type ?? 'warning',
      })
      return true
    } catch {
      return false
    }
  }
  return { confirm }
}
