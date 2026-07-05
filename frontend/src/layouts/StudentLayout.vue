<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { logout as apiLogout } from '@/api/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const activeIndex = computed<string>(() => route.path)

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
  <el-container class="csm-student-layout">
    <el-header class="csm-student-layout__header">
      <div class="csm-brand">学生成绩管理系统</div>
      <el-menu
        mode="horizontal"
        :default-active="activeIndex"
        router
        class="csm-nav"
        background-color="transparent"
      >
        <el-menu-item index="/student/dashboard">首页</el-menu-item>
        <el-menu-item index="/student/scores">我的成绩</el-menu-item>
        <el-menu-item index="/student/profile">个人资料</el-menu-item>
        <el-menu-item index="/student/password">修改密码</el-menu-item>
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
.csm-student-layout {
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
