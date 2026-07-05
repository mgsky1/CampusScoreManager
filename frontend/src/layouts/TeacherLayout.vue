<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { logout as apiLogout } from '@/api/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

// 顶部菜单按前缀高亮：例如 `/teacher/students/3/score-detail` 依然让"成绩管理"或"学生管理"高亮
const activeIndex = computed<string>(() => {
  const p = route.path
  if (p.startsWith('/teacher/dashboard')) return '/teacher/dashboard'
  if (p.startsWith('/teacher/students') && !p.startsWith('/teacher/students/')) return '/teacher/students'
  if (p.startsWith('/teacher/score-manage') || p.startsWith('/teacher/students/')) return '/teacher/score-manage'
  if (p.startsWith('/teacher/subjects')) return '/teacher/subjects'
  if (p.startsWith('/teacher/profile')) return '/teacher/profile'
  if (p.startsWith('/teacher/password')) return '/teacher/password'
  return p
})

async function onLogout() {
  try {
    await ElMessageBox.confirm('确定要登出吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await apiLogout()
  } catch {
    /* 忽略错误，前端仍然清理 */
  }
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <el-container class="csm-teacher-layout">
    <el-header class="csm-teacher-layout__header">
      <div class="csm-brand">学生成绩管理系统 · 教师端</div>
      <el-menu
        mode="horizontal"
        :default-active="activeIndex"
        router
        class="csm-nav"
        background-color="transparent"
      >
        <el-menu-item index="/teacher/dashboard">首页</el-menu-item>
        <el-menu-item index="/teacher/students">学生管理</el-menu-item>
        <el-menu-item index="/teacher/subjects">我的课程</el-menu-item>
        <el-menu-item index="/teacher/score-manage">成绩管理</el-menu-item>
        <el-menu-item index="/teacher/profile">个人信息</el-menu-item>
        <el-menu-item index="/teacher/password">修改密码</el-menu-item>
      </el-menu>
      <div class="csm-user">
        <span>{{ auth.user?.realName }}</span>
        <el-button link type="primary" @click="onLogout">登出</el-button>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped lang="scss">
.csm-teacher-layout {
  min-height: 100vh;
  &__header {
    display: flex;
    align-items: center;
    background: #fff;
    border-bottom: 1px solid var(--el-border-color-light);
    padding: 0 24px;
    gap: 24px;
  }
}
.csm-brand {
  font-weight: 600;
  font-size: 18px;
}
.csm-nav {
  flex: 1;
  border-bottom: none !important;
}
.csm-user {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
