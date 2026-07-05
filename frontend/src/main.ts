import { createApp } from 'vue'
import { createPinia } from 'pinia'

// Element Plus 走按需引入：
// - 组件由 `unplugin-vue-components` + `ElementPlusResolver` 自动注册；
// - `ElMessage` / `ElMessageBox` 等 API-only 组件由 `unplugin-auto-import` 自动引入；
// - 相应样式由 resolver 自动带入。
// 深色主题、通用样式仍需要一次性引入：
import 'element-plus/theme-chalk/base.css'
import 'element-plus/theme-chalk/el-loading.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import 'element-plus/theme-chalk/el-notification.css'
import 'element-plus/theme-chalk/el-overlay.css'

import App from './App.vue'
import router from './router'
import { registerRouterGuards } from './router/guards'

const app = createApp(App)

app.use(createPinia())
app.use(router)

registerRouterGuards(router)

app.mount('#app')
