<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { login as apiLogin } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/utils/http'
import { translateErrorCode } from '@/utils/errorMap'

interface Form {
  loginName: string
  password: string
  remember: boolean
}

const form = reactive<Form>({ loginName: '', password: '', remember: false })
const rules = reactive<FormRules<Form>>({
  loginName: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
})

const formRef = ref<FormInstance>()
const submitting = ref(false)
const errorMsg = ref('')

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function onSubmit(): Promise<void> {
  errorMsg.value = ''
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const r = await apiLogin({ loginName: form.loginName.trim(), password: form.password })
    auth.login(
      { accessToken: r.accessToken, refreshToken: r.refreshToken, user: r.user },
      form.remember,
    )
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string | undefined) ?? defaultHomeByRole(r.user.role)
    router.replace(redirect)
  } catch (e) {
    if (e instanceof ApiError) {
      errorMsg.value = translateErrorCode(e.code, e.message)
    } else {
      errorMsg.value = '登录失败，请稍后重试'
    }
  } finally {
    submitting.value = false
  }
}

function defaultHomeByRole(role: 'STUDENT' | 'TEACHER'): string {
  return role === 'TEACHER' ? '/teacher/dashboard' : '/student/dashboard'
}
</script>

<template>
  <div class="csm-login">
    <el-card class="csm-login__card" shadow="always">
      <template #header>
        <div class="csm-login__title">学生成绩管理系统</div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px" @keyup.enter="onSubmit">
        <el-form-item label="登录名" prop="loginName">
          <el-input v-model="form.loginName" placeholder="请输入登录名" autocomplete="username" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.remember">记住我</el-checkbox>
        </el-form-item>
        <el-alert
          v-if="errorMsg"
          :title="errorMsg"
          type="error"
          show-icon
          :closable="false"
          class="csm-login__error"
        />
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.csm-login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--csm-color-bg, #f5f7fa);
  &__card {
    width: 400px;
  }
  &__title {
    text-align: center;
    font-size: var(--csm-font-size-lg, 20px);
    font-weight: 600;
  }
  &__error {
    margin-bottom: var(--csm-spacing-md, 12px);
  }
}
</style>
