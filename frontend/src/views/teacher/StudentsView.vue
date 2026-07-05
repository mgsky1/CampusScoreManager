<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createStudent,
  deleteStudent,
  searchStudents,
  updateStudent,
} from '@/api/teacherStudents'
import type {
  CreateStudentPayload,
  StudentDetail,
  UpdateStudentPayload,
} from '@/types/api'
import { ApiErrorCode } from '@/types/api'
import { ApiError } from '@/utils/http'
import { translateErrorCode } from '@/utils/errorMap'
import { usePagination } from '@/composables/usePagination'
import EmptyState from '@/components/EmptyState.vue'

// ---------- 列表状态 ----------
const items = ref<StudentDetail[]>([])
const total = ref(0)
const loading = ref(false)
const keywordInput = ref('')

const { state, toPage, toSize } = usePagination({ defaultSize: 20 })

async function reload() {
  loading.value = true
  try {
    const r = await searchStudents({
      page: state.page,
      size: state.size,
      keyword: state.keyword || undefined,
    })
    items.value = r.items
    total.value = r.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  state.keyword = keywordInput.value.trim()
  state.page = 1
  void reload()
}

function onReset() {
  keywordInput.value = ''
  state.keyword = ''
  state.page = 1
  void reload()
}

// ---------- 新增 / 编辑 对话框 ----------
type DialogMode = 'create' | 'edit'
const dialogVisible = ref(false)
const dialogMode = ref<DialogMode>('create')
const editingId = ref<number | null>(null)
const submitting = ref(false)

const form = reactive({
  loginName: '',
  realName: '',
  password: '',
  tel: '',
  address: '',
  grade: 1 as number,
})

const fieldErrors = reactive({
  loginName: '',
  realName: '',
  password: '',
  tel: '',
  address: '',
  grade: '',
})

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增学生' : '编辑学生'))

function resetForm() {
  form.loginName = ''
  form.realName = ''
  form.password = ''
  form.tel = ''
  form.address = ''
  form.grade = 1
  for (const k of Object.keys(fieldErrors) as Array<keyof typeof fieldErrors>) {
    fieldErrors[k] = ''
  }
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: StudentDetail) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  resetForm()
  form.loginName = row.loginName
  form.realName = row.realName
  form.tel = row.tel
  form.address = row.address
  form.grade = row.grade
  dialogVisible.value = true
}

function validate(): boolean {
  for (const k of Object.keys(fieldErrors) as Array<keyof typeof fieldErrors>) {
    fieldErrors[k] = ''
  }
  let ok = true
  if (!form.loginName.trim()) {
    fieldErrors.loginName = '登录名不能为空'
    ok = false
  } else if (!/^[A-Za-z0-9_]+$/.test(form.loginName)) {
    fieldErrors.loginName = '登录名只能包含字母、数字与下划线'
    ok = false
  } else if (form.loginName.length > 50) {
    fieldErrors.loginName = '登录名长度不能超过 50'
    ok = false
  }
  if (!form.realName.trim()) {
    fieldErrors.realName = '姓名不能为空'
    ok = false
  } else if (form.realName.length > 20) {
    fieldErrors.realName = '姓名长度不能超过 20'
    ok = false
  }
  // password：create 必填；edit 选填但如填了要 6-32
  const pwd = form.password
  if (dialogMode.value === 'create') {
    if (!pwd) {
      fieldErrors.password = '密码不能为空'
      ok = false
    } else if (pwd.length < 6 || pwd.length > 32) {
      fieldErrors.password = '密码长度需在 6-32 之间'
      ok = false
    }
  } else if (pwd && (pwd.length < 6 || pwd.length > 32)) {
    fieldErrors.password = '密码长度需在 6-32 之间'
    ok = false
  }
  if (!form.tel.trim()) {
    fieldErrors.tel = '电话不能为空'
    ok = false
  } else if (!/^\d{8,11}$/.test(form.tel)) {
    fieldErrors.tel = '电话必须为 8-11 位数字'
    ok = false
  }
  if (!form.address.trim()) {
    fieldErrors.address = '地址不能为空'
    ok = false
  } else if (form.address.length > 50) {
    fieldErrors.address = '地址长度不能超过 50'
    ok = false
  }
  const g = Number(form.grade)
  if (!Number.isInteger(g) || g < 1 || g > 6) {
    fieldErrors.grade = '年级需在 1-6 之间'
    ok = false
  }
  return ok
}

async function onSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      const payload: CreateStudentPayload = {
        loginName: form.loginName.trim(),
        realName: form.realName.trim(),
        password: form.password,
        tel: form.tel.trim(),
        address: form.address.trim(),
        grade: Number(form.grade),
      }
      await createStudent(payload)
      ElMessage.success('新增成功')
    } else if (editingId.value != null) {
      const payload: UpdateStudentPayload = {
        loginName: form.loginName.trim(),
        realName: form.realName.trim(),
        tel: form.tel.trim(),
        address: form.address.trim(),
        grade: Number(form.grade),
      }
      if (form.password) payload.password = form.password
      await updateStudent(editingId.value, payload)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    await reload()
  } catch (e) {
    if (e instanceof ApiError) {
      if (e.code === ApiErrorCode.LOGIN_NAME_TAKEN) {
        fieldErrors.loginName = '登录名已存在'
      } else if (e.code === ApiErrorCode.VALIDATION_FAILED && e.errors && e.errors.length > 0) {
        for (const fe of e.errors) {
          if (fe.field in fieldErrors) {
            ;(fieldErrors as Record<string, string>)[fe.field] =
              fe.message ?? translateErrorCode(e.code)
          }
        }
        // 兜底若字段没匹配到
        if (!Object.values(fieldErrors).some(Boolean)) {
          ElMessage.error(translateErrorCode(e.code, e.message))
        }
      } else {
        ElMessage.error(translateErrorCode(e.code, e.message))
      }
    } else {
      ElMessage.error('提交失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

// ---------- 删除 ----------
async function onDelete(row: StudentDetail) {
  try {
    await ElMessageBox.confirm(
      `确定删除学生「${row.realName}」(登录名 ${row.loginName})？此操作将级联删除其所有成绩记录。`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteStudent(row.id)
    ElMessage.success('删除成功')
    await reload()
  } catch (e) {
    if (e instanceof ApiError) {
      ElMessage.error(translateErrorCode(e.code, e.message))
    } else {
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

onMounted(() => void reload())
watch(() => [state.page, state.size], () => void reload())
</script>

<template>
  <div class="csm-students">
    <div class="csm-students__header">
      <h2>学生管理</h2>
      <el-button type="primary" @click="openCreate">新增学生</el-button>
    </div>

    <el-form inline class="csm-students__filter" @submit.prevent="onSearch">
      <el-form-item label="姓名 / 登录名">
        <el-input
          v-model="keywordInput"
          placeholder="输入姓名或登录名"
          clearable
          @keyup.enter="onSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" @click="onSearch">搜索</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-if="items.length > 0"
      v-loading="loading"
      :data="items"
      stripe
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="loginName" label="登录名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="grade" label="年级" width="90" />
      <el-table-column prop="tel" label="联系方式" />
      <el-table-column prop="address" label="地址" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row as StudentDetail)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row as StudentDetail)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <EmptyState
      v-else-if="!loading"
      icon="👨‍🎓"
      title="暂无学生"
      description="请调整搜索条件或点击右上角新增学生"
    />

    <el-pagination
      v-if="total > 0"
      class="csm-students__pager"
      :current-page="state.page"
      :page-size="state.size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      @current-change="toPage"
      @size-change="toSize"
    />

    <!-- 新增 / 编辑 对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="480px"
    >
      <el-form :model="form" label-width="90px">
        <el-form-item label="登录名" prop="loginName" :error="fieldErrors.loginName">
          <el-input v-model="form.loginName" placeholder="字母数字下划线，最长 50" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName" :error="fieldErrors.realName">
          <el-input v-model="form.realName" placeholder="最长 20 字" />
        </el-form-item>
        <el-form-item
          :label="dialogMode === 'create' ? '密码' : '重置密码'"
          prop="password"
          :error="fieldErrors.password"
        >
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="dialogMode === 'create' ? '6-32 位' : '留空表示不修改'"
          />
        </el-form-item>
        <el-form-item label="联系电话" prop="tel" :error="fieldErrors.tel">
          <el-input v-model="form.tel" placeholder="8-11 位数字" />
        </el-form-item>
        <el-form-item label="地址" prop="address" :error="fieldErrors.address">
          <el-input v-model="form.address" placeholder="最长 50 字" />
        </el-form-item>
        <el-form-item label="年级" prop="grade" :error="fieldErrors.grade">
          <el-input-number v-model="form.grade" :min="1" :max="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.csm-students {
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }
  &__filter {
    margin-bottom: 16px;
  }
  &__pager {
    margin-top: 16px;
  }
}
</style>
