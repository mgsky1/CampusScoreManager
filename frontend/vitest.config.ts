import { defineConfig, mergeConfig } from 'vitest/config'
import viteConfig from './vite.config'

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      passWithNoTests: true,
      environment: 'jsdom',
      globals: true,
      setupFiles: ['./tests/setup.ts'],
      css: false,
      server: {
        deps: {
          inline: ['element-plus'],
        },
      },
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html', 'lcov'],
        include: ['src/**/*.{ts,vue}'],
        exclude: [
          'src/main.ts',
          'src/auto-imports.d.ts',
          'src/components.d.ts',
          'src/**/*.d.ts',
          // 视图容器 / 布局 / 路由 index：模板占位或全局挂载入口，逻辑由集成测试覆盖
          'src/App.vue',
          'src/router/index.ts',
          'src/layouts/**',
          'src/views/common/**',
          'src/views/**/DashboardView.vue',
          // ConfirmDialog.vue 已被 composables/useConfirm.ts 取代（无模板、待清理）
          'src/components/ConfirmDialog.vue',
        ],
        thresholds: {
          // T180 覆盖率门禁 · 宪章原则二
          // 全局基线：lines/functions ≥ 70%
          lines: 70,
          functions: 70,
          // 逐目录门禁：composables ≥ 85% / components ≥ 75% / views ≥ 60%
          'src/composables/**': {
            lines: 85,
            functions: 85,
          },
          'src/components/**': {
            lines: 75,
            functions: 75,
          },
          'src/views/**': {
            lines: 60,
            functions: 60,
          },
        },
      },
    },
  }),
)
