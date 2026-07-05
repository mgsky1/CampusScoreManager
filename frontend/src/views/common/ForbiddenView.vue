<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
function backToLogin() {
  router.replace('/login')
}
</script>

<template>
  <div class="csm-403">
    <el-result icon="warning" title="403 无权限访问" sub-title="您没有查看此页面的权限。请切换账号后重试。">
      <template #extra>
        <el-button type="primary" @click="backToLogin">返回登录</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped lang="scss">
.csm-403 {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
