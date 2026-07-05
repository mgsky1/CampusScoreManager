<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/account'
import { useAuthStore } from '@/stores/auth'
import { ApiErrorCode } from '@/types/api'
import { ApiError } from '@/utils/http'
import { translateErrorCode } from '@/utils/errorMap'

/**
 * 修改密码表单。
 * - 三字段必填；new/confirm 不一致在前端阻断；
 * - 服务端 2002/2003 映射到对应字段；
 * - 成功后 `authStore.logout()` + 跳 `/login?msg=password-changed`。
 */
const router = useRouter()
const authStore = useAuthStore()
const submitting = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const fieldErrors = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

function resetErrors() {
  fieldErrors.oldPassword = ''
  fieldErrors.newPassword = ''
  fieldErrors.confirmPassword = ''
}

function validate(): boolean {
  resetErrors()
  let ok = true
  if (!form.oldPassword) {
    fieldErrors.oldPassword = '原密码不能为空'
    ok = false
  }
  if (!form.newPassword) {
    fieldErrors.newPassword = '新密码不能为空'
    ok = false
  } else if (form.newPassword.length < 6 || form.newPassword.length > 32) {
    fieldErrors.newPassword = '新密码长度需在 6-32 位'
    ok = false
  }
  if (!form.confirmPassword) {
    fieldErrors.confirmPassword = '确认密码不能为空'
    ok = false
  } else if (form.newPassword && form.confirmPassword !== form.newPassword) {
    fieldErrors.confirmPassword = '确认密码与新密码不一致'
    ok = false
  }
  return ok
}

async function onSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    authStore.logout()
    void router.push({ path: '/login', query: { msg: 'password-changed' } })
  } catch (e) {
    if (e instanceof ApiError) {
      if (e.code === ApiErrorCode.OLD_PASSWORD_MISMATCH) {
        fieldErrors.oldPassword = '原密码错误'
      } else if (e.code === ApiErrorCode.CONFIRM_PASSWORD_MISMATCH) {
        fieldErrors.confirmPassword = '确认密码与新密码不一致'
      } else if (e.code === ApiErrorCode.VALIDATION_FAILED && e.errors && e.errors.length > 0) {
        for (const fe of e.errors) {
          const msg = fe.message ?? '字段不合法'
          if (fe.field === 'oldPassword') fieldErrors.oldPassword = msg
          else if (fe.field === 'newPassword') fieldErrors.newPassword = msg
          else if (fe.field === 'confirmPassword' || fe.field === 'confirmMatchesNew') {
            fieldErrors.confirmPassword = fe.field === 'confirmMatchesNew'
              ? '确认密码与新密码不一致'
              : msg
          }
        }
        const anySet =
          fieldErrors.oldPassword || fieldErrors.newPassword || fieldErrors.confirmPassword
        if (!anySet) ElMessage.error(translateErrorCode(e.code, e.message))
      } else {
        ElMessage.error(translateErrorCode(e.code, e.message))
      }
    } else {
      ElMessage.error('修改失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="csm-password-form">
    <h2>修改密码</h2>
    <el-form :model="form" label-width="90px" @submit.prevent="onSubmit">
      <el-form-item label="原密码" prop="oldPassword" :error="fieldErrors.oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          show-password
          placeholder="请输入原密码"
          class="csm-password-form__old"
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword" :error="fieldErrors.newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          placeholder="6-32 位"
          class="csm-password-form__new"
        />
      </el-form-item>
      <el-form-item
        label="确认密码"
        prop="confirmPassword"
        :error="fieldErrors.confirmPassword"
      >
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="再次输入新密码"
          class="csm-password-form__confirm"
        />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          :loading="submitting"
          native-type="submit"
          @click="onSubmit"
        >提交</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped lang="scss">
.csm-password-form {
  max-width: 480px;
}
</style>
