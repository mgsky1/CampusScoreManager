<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '@/api/account'
import type { ProfileResponse } from '@/api/account'
import type { UserRole } from '@/types/api'
import { ApiErrorCode } from '@/types/api'
import { ApiError } from '@/utils/http'
import { translateErrorCode } from '@/utils/errorMap'

/**
 * 账号资料编辑表单。按 `role` 决定字段：
 * - STUDENT: tel + address（后端必填）
 * - TEACHER: 仅 tel
 *
 * 前端阻断：空 tel、tel 非 8-11 位数字、学生空 address。
 * 服务端 1000 字段错误映射到对应表单项 `:error`。
 */
const props = defineProps<{ role: UserRole }>()

const loading = ref(false)
const submitting = ref(false)
const profile = ref<ProfileResponse | null>(null)

const form = reactive({
  tel: '',
  address: '',
})

const fieldErrors = reactive({
  tel: '',
  address: '',
})

const isStudent = computed(() => props.role === 'STUDENT')

async function reload() {
  loading.value = true
  try {
    const r = await getProfile()
    profile.value = r
    form.tel = r.tel ?? ''
    form.address = r.address ?? ''
  } catch (e) {
    if (e instanceof ApiError) {
      ElMessage.error(translateErrorCode(e.code, e.message))
    } else {
      ElMessage.error('加载失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

function validate(): boolean {
  fieldErrors.tel = ''
  fieldErrors.address = ''
  let ok = true
  const tel = form.tel.trim()
  if (!tel) {
    fieldErrors.tel = 'tel 不能为空'
    ok = false
  } else if (!/^\d{8,11}$/.test(tel)) {
    fieldErrors.tel = 'tel 必须为 8-11 位数字'
    ok = false
  }
  if (isStudent.value) {
    const addr = form.address.trim()
    if (!addr) {
      fieldErrors.address = 'address 不能为空'
      ok = false
    } else if (addr.length > 50) {
      fieldErrors.address = 'address 长度不能超过 50'
      ok = false
    }
  }
  return ok
}

async function onSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    const payload: { tel: string; address?: string } = { tel: form.tel.trim() }
    if (isStudent.value) {
      payload.address = form.address.trim()
    }
    const r = await updateProfile(payload)
    profile.value = r
    form.tel = r.tel ?? ''
    form.address = r.address ?? ''
    ElMessage.success('保存成功')
  } catch (e) {
    if (e instanceof ApiError) {
      if (e.code === ApiErrorCode.VALIDATION_FAILED && e.errors && e.errors.length > 0) {
        for (const fe of e.errors) {
          if (fe.field === 'tel') fieldErrors.tel = fe.message ?? 'tel 不合法'
          if (fe.field === 'address') fieldErrors.address = fe.message ?? 'address 不合法'
        }
        if (!fieldErrors.tel && !fieldErrors.address) {
          ElMessage.error(translateErrorCode(e.code, e.message))
        }
      } else {
        ElMessage.error(translateErrorCode(e.code, e.message))
      }
    } else {
      ElMessage.error('保存失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => void reload())
</script>

<template>
  <div class="csm-profile-form" v-loading="loading">
    <h2>个人资料</h2>
    <el-form :model="form" label-width="90px" @submit.prevent="onSubmit">
      <el-form-item label="登录名">
        <el-input :model-value="profile?.loginName ?? ''" disabled />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input :model-value="profile?.realName ?? ''" disabled />
      </el-form-item>
      <el-form-item label="角色">
        <el-input :model-value="profile?.role ?? ''" disabled />
      </el-form-item>
      <el-form-item v-if="isStudent" label="年级">
        <el-input :model-value="String(profile?.grade ?? '')" disabled />
      </el-form-item>
      <el-form-item label="电话" prop="tel" :error="fieldErrors.tel">
        <el-input
          v-model="form.tel"
          placeholder="8-11 位数字"
          class="csm-profile-form__tel"
        />
      </el-form-item>
      <el-form-item
        v-if="isStudent"
        label="地址"
        prop="address"
        :error="fieldErrors.address"
      >
        <el-input
          v-model="form.address"
          placeholder="最长 50 字"
          class="csm-profile-form__address"
        />
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          :loading="submitting"
          native-type="submit"
          class="csm-profile-form__submit"
          @click="onSubmit"
        >保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped lang="scss">
.csm-profile-form {
  max-width: 480px;
}
</style>
