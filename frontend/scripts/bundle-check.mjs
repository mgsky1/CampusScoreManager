#!/usr/bin/env node
// T175 · 打包体积门禁
// 断言 dist/assets/index-*.js 的 gzip 尺寸 ≤ 300 KB。
// 未跑构建时先自动 `vite build`（可通过 --skip-build 跳过）。

import { execSync } from 'node:child_process'
import { readdirSync, readFileSync, existsSync } from 'node:fs'
import { gzipSync } from 'node:zlib'
import { join, dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = resolve(__dirname, '..')
const distDir = join(root, 'dist', 'assets')
const LIMIT_KB = Number(process.env.BUNDLE_GZIP_LIMIT_KB || 300)

if (!process.argv.includes('--skip-build')) {
  console.log('==> 运行 vite build...')
  execSync('npx vite build', { cwd: root, stdio: 'inherit' })
}

if (!existsSync(distDir)) {
  console.error(`[bundle-check] ${distDir} 不存在；请先 npm run build`)
  process.exit(2)
}

const entries = readdirSync(distDir)
  .filter((f) => /^index-[A-Za-z0-9_-]+\.js$/.test(f))

if (entries.length === 0) {
  console.error('[bundle-check] 未找到 index-*.js 主 chunk')
  process.exit(2)
}

let hasFailure = false
console.log(`==> 门禁：主 chunk gzip ≤ ${LIMIT_KB} KB`)
for (const f of entries) {
  const buf = readFileSync(join(distDir, f))
  const gz = gzipSync(buf).length
  const kb = gz / 1024
  const ok = kb <= LIMIT_KB
  const marker = ok ? '✓' : '✗'
  console.log(`  ${marker} ${f}  raw=${(buf.length / 1024).toFixed(1)}KB  gzip=${kb.toFixed(2)}KB`)
  if (!ok) hasFailure = true
}

if (hasFailure) {
  console.error(`[bundle-check] FAIL：主 chunk gzip 超过 ${LIMIT_KB} KB`)
  process.exit(1)
}
console.log('[bundle-check] OK')
