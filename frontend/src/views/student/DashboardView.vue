<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const router = useRouter()
const cards: Array<{ title: string; desc: string; to: string; icon: string }> = [
  { title: '我的成绩', desc: '查看所有课程分数与不及格提醒', to: '/student/scores', icon: '📚' },
  { title: '个人资料', desc: '维护联系方式与地址', to: '/student/profile', icon: '👤' },
  { title: '修改密码', desc: '定期更换密码更安全', to: '/student/password', icon: '🔒' },
]
</script>

<template>
  <div class="csm-dash">
    <h2>欢迎，{{ auth.user?.realName }} 同学</h2>
    <el-row :gutter="16">
      <el-col v-for="c in cards" :key="c.to" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="csm-dash__card" @click="router.push(c.to)">
          <div class="csm-dash__icon">{{ c.icon }}</div>
          <div class="csm-dash__title">{{ c.title }}</div>
          <div class="csm-dash__desc">{{ c.desc }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.csm-dash__card {
  cursor: pointer;
  margin-bottom: 16px;
}
.csm-dash__icon {
  font-size: 32px;
  margin-bottom: 8px;
}
.csm-dash__title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}
.csm-dash__desc {
  font-size: 13px;
  color: #909399;
}
</style>
