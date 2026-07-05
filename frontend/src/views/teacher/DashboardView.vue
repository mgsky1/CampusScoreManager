<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

interface QuickLink {
  title: string
  desc: string
  icon: string
  to: { name: string }
}

const quickLinks: QuickLink[] = [
  { title: '学生管理', desc: '管理学生档案', icon: '👨‍🎓', to: { name: 'teacher.students' } },
  { title: '我的课程', desc: '维护授课表', icon: '📚', to: { name: 'teacher.subjects' } },
  { title: '成绩管理', desc: '查询 / 录入 / 修改学生成绩', icon: '📝', to: { name: 'teacher.scoreManage' } },
  { title: '个人信息', desc: '查看 / 修改个人资料', icon: '👤', to: { name: 'teacher.profile' } },
  { title: '修改密码', desc: '更新登录密码', icon: '🔒', to: { name: 'teacher.password' } },
]

function go(to: { name: string }) {
  void router.push(to)
}
</script>

<template>
  <div class="csm-teacher-dashboard">
    <div class="csm-teacher-dashboard__welcome">
      <h2>欢迎回来，{{ auth.user?.realName ?? '老师' }}</h2>
      <p class="csm-teacher-dashboard__subtitle">
        通过下方入口快速开始工作
      </p>
    </div>

    <el-row :gutter="16" class="csm-teacher-dashboard__grid">
      <el-col
        v-for="link in quickLinks"
        :key="link.title"
        :span="8"
        :xs="24"
        :sm="12"
        :md="8"
      >
        <el-card class="csm-teacher-dashboard__card" shadow="hover" @click="go(link.to)">
          <div class="csm-teacher-dashboard__icon">{{ link.icon }}</div>
          <div class="csm-teacher-dashboard__title">{{ link.title }}</div>
          <div class="csm-teacher-dashboard__desc">{{ link.desc }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.csm-teacher-dashboard {
  &__welcome {
    margin-bottom: 24px;
  }
  &__subtitle {
    color: var(--el-text-color-secondary);
    margin-top: 4px;
  }
  &__grid {
    margin-top: 8px;
  }
  &__card {
    cursor: pointer;
    margin-bottom: 16px;
    transition: transform 0.15s ease-in-out;
  }
  &__card:hover {
    transform: translateY(-2px);
  }
  &__icon {
    font-size: 32px;
    margin-bottom: 8px;
  }
  &__title {
    font-weight: 600;
    font-size: 16px;
  }
  &__desc {
    color: var(--el-text-color-secondary);
    font-size: 14px;
    margin-top: 4px;
  }
}
</style>
