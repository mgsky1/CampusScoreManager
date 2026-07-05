<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createSubject,
  deleteSubject,
  listMySubjects,
  updateSubject,
} from '@/api/teacherSubjects'
import type {
  CreateSubjectPayload,
  SubjectMineItem,
  UpdateSubjectPayload,
} from '@/types/api'
import { ApiErrorCode } from '@/types/api'
import { ApiError } from '@/utils/http'
import { translateErrorCode } from '@/utils/errorMap'
import { usePagination } from '@/composables/usePagination'
import EmptyState from '@/components/EmptyState.vue'

// ---------- 列表状态 ----------
const items = ref<SubjectMineItem[]>([])
const total = ref(0)
const loading = ref(false)
const keywordInput = ref('')

const { state, toPage, toSize } = usePagination({ defaultSize: 20 })

async function reload() {
  loading.value = true
  try {
    const r = await listMySubjects({
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
  name: '',
  grade: 1 as number,
})

const fieldErrors = reactive({
  name: '',
  grade: '',
})

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增课程' : '编辑课程'))

function resetForm() {
  form.name = ''
  form.grade = 1
  fieldErrors.name = ''
  fieldErrors.grade = ''
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: SubjectMineItem) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  resetForm()
  form.name = row.name
  form.grade = row.grade
  dialogVisible.value = true
}

function validate(): boolean {
  fieldErrors.name = ''
  fieldErrors.grade = ''
  let ok = true
  if (!form.name.trim()) {
    fieldErrors.name = '课程名不能为空'
    ok = false
  } else if (form.name.length > 50) {
    fieldErrors.name = '课程名长度不能超过 50'
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
      const payload: CreateSubjectPayload = {
        name: form.name.trim(),
        grade: Number(form.grade),
      }
      await createSubject(payload)
      ElMessage.success('新增成功')
    } else if (editingId.value != null) {
      const payload: UpdateSubjectPayload = {
        name: form.name.trim(),
        grade: Number(form.grade),
      }
      await updateSubject(editingId.value, payload)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    await reload()
  } catch (e) {
    if (e instanceof ApiError) {
      if (e.code === ApiErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER) {
        fieldErrors.name = '该教师已存在同名课程'
      } else if (e.code === ApiErrorCode.VALIDATION_FAILED && e.errors && e.errors.length > 0) {
        for (const fe of e.errors) {
          if (fe.field === 'name') fieldErrors.name = fe.message ?? '课程名不合法'
          if (fe.field === 'grade') fieldErrors.grade = fe.message ?? '年级不合法'
        }
        if (!fieldErrors.name && !fieldErrors.grade) {
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
async function onDelete(row: SubjectMineItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除课程「${row.name}」（${row.grade} 年级）？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteSubject(row.id)
    ElMessage.success('删除成功')
    await reload()
  } catch (e) {
    if (e instanceof ApiError) {
      if (e.code === ApiErrorCode.SUBJECT_HAS_SCORES) {
        ElMessage.error('该课程存在成绩记录，请先删除相关成绩')
      } else {
        ElMessage.error(translateErrorCode(e.code, e.message))
      }
    } else {
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

onMounted(() => void reload())
watch(() => [state.page, state.size], () => void reload())
</script>

<template>
  <div class="csm-subjects">
    <div class="csm-subjects__header">
      <h2>我的课程</h2>
      <el-button type="primary" @click="openCreate">新增课程</el-button>
    </div>

    <el-form inline class="csm-subjects__filter" @submit.prevent="onSearch">
      <el-form-item label="课程名">
        <el-input
          v-model="keywordInput"
          placeholder="输入课程名关键字"
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
      <el-table-column prop="name" label="课程名" />
      <el-table-column prop="grade" label="年级" width="100" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row as SubjectMineItem)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row as SubjectMineItem)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <EmptyState
      v-else-if="!loading"
      icon="📚"
      title="暂无课程"
      description="点击右上角新增课程开始授课"
    />

    <el-pagination
      v-if="total > 0"
      class="csm-subjects__pager"
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
      width="420px"
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="课程名" prop="name" :error="fieldErrors.name">
          <el-input v-model="form.name" placeholder="最长 50 字" />
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
.csm-subjects {
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
