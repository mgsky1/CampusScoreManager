<script setup lang="ts">
import { ElMessageBox } from 'element-plus'

/**
 * 全局二次确认。Promise 版：resolve(true) = 确认，resolve(false) = 取消。
 * 用法：
 *   const ok = await confirm('删除该学生？', '此操作无法撤销')
 *   if (!ok) return
 *
 * 组件本身仅导出 confirm 函数，无模板；这样调用方无需挂载。
 */
export interface ConfirmOptions {
  title?: string
  confirmText?: string
  cancelText?: string
  type?: 'warning' | 'info' | 'error' | 'success'
}

export async function confirm(
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

defineExpose({ confirm })
</script>

<template>
  <!-- 逻辑组件，无 UI -->
</template>
