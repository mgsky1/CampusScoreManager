<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createScore, getEntryOptions } from '@/api/teacherScores'
import type { EntryOptions } from '@/types/api'
import { ApiErrorCode } from '@/types/api'
import { ApiError } from '@/utils/http'
import { translateErrorCode } from '@/utils/errorMap'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const studentId = computed<number>(() => Number(route.params.id))

const loading = ref(false)
const submitting = ref(false)
const data = ref<EntryOptions | null>(null)

const form = reactive({
  subjectId: undefined as number | undefined,
  score: 0,
})

const fieldErrors = reactive({
  subjectId: '',
  score: '',
})

const hasOptions = computed<boolean>(() => (data.value?.options ?? []).length > 0)
const allEntered = computed<boolean>(() => data.value?.allEntered === true)

async function reload() {
  loading.value = true
  try {
    const r = await getEntryOptions(studentId.value)
    data.value = r
    // 默认选中第一个候选课程，减少交互
    if (r.options.length > 0) {
      form.subjectId = r.options[0].subjectId
    } else {
      form.subjectId = undefined
    }
  } catch (e) {
    if (e instanceof ApiError && e.code === ApiErrorCode.NOT_FOUND) {
      ElMessage.error('学生不存在')
      void router.replace({ name: 'teacher.scoreManage' })
    } else if (e instanceof ApiError) {
      ElMessage.error(translateErrorCode(e.code, e.message))
    }
  } finally {
    loading.value = false
  }
}

function validate(): boolean {
  fieldErrors.subjectId = ''
  fieldErrors.score = ''
  if (form.subjectId == null) {
    fieldErrors.subjectId = '请选择要录入的课程'
    return false
  }
  const s = form.score
  if (typeof s !== 'number' || Number.isNaN(s)) {
    fieldErrors.score = '请输入 0-100 之间的整数'
    return false
  }
  if (s < 0 || s > 100) {
    fieldErrors.score = '成绩必须在 0-100 之间'
    return false
  }
  if (!Number.isInteger(s)) {
    fieldErrors.score = '成绩必须为整数'
    return false
  }
  return true
}

async function onSubmit() {
  if (!validate() || form.subjectId == null) return
  submitting.value = true
  try {
    await createScore(studentId.value, { subjectId: form.subjectId, score: form.score })
    ElMessage.success('录入成功')
    void router.push({ name: 'teacher.scoreDetail', params: { id: studentId.value } })
  } catch (e) {
    if (e instanceof ApiError) {
      if (e.code === ApiErrorCode.VALIDATION_FAILED && e.errors && e.errors.length > 0) {
        for (const fe of e.errors) {
          if (fe.field === 'score') fieldErrors.score = fe.message ?? '成绩不合法'
          if (fe.field === 'subjectId') fieldErrors.subjectId = fe.message ?? '课程不合法'
        }
      } else {
        ElMessage.error(translateErrorCode(e.code, e.message))
      }
      // 已存在 / 全部录入 → 拉取一次刷新候选
      if (
        e.code === ApiErrorCode.SCORE_ALREADY_EXISTS ||
        e.code === ApiErrorCode.SCORE_ALL_ENROLLED
      ) {
        await reload()
      }
    } else {
      ElMessage.error('录入失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

function goBack() {
  void router.push({ name: 'teacher.scoreManage' })
}

onMounted(() => void reload())
</script>

<template>
  <div class="csm-score-entry" v-loading="loading">
    <div class="csm-score-entry__header">
      <h2>录入学生成绩</h2>
      <div>
        <el-button @click="goBack">返回</el-button>
      </div>
    </div>

    <el-descriptions
      v-if="data?.student"
      :column="3"
      border
      class="csm-score-entry__meta"
    >
      <el-descriptions-item label="学生 ID">{{ data.student.id }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ data.student.realName }}</el-descriptions-item>
      <el-descriptions-item label="年级">{{ data.student.grade }}</el-descriptions-item>
    </el-descriptions>

    <!-- 情况 A：allEntered=true -->
    <el-alert
      v-if="allEntered"
      title="该学生本教师课程已全部录入完毕"
      type="success"
      :closable="false"
      show-icon
    />

    <!-- 情况 B：无任何候选 -->
    <EmptyState
      v-else-if="!hasOptions && !loading"
      icon="📚"
      title="暂无可录入的课程"
      description="您当前没有面向该学生年级授课的课程"
    />

    <!-- 情况 C：正常表单 -->
    <el-form
      v-else-if="hasOptions"
      :model="form"
      label-width="72px"
      class="csm-score-entry__form"
    >
      <el-form-item label="课程" prop="subjectId" :error="fieldErrors.subjectId">
        <el-select
          v-model="form.subjectId"
          placeholder="请选择要录入的课程"
        >
          <el-option
            v-for="opt in data?.options"
            :key="opt.subjectId"
            :value="opt.subjectId"
            :label="`${opt.subjectName}（${opt.grade} 年级）`"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="成绩" prop="score" :error="fieldErrors.score">
        <el-input-number
          v-model="form.score"
          :min="0"
          :max="100"
          :step="1"
          :precision="0"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="onSubmit">提交</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped lang="scss">
.csm-score-entry {
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  &__meta {
    margin: 16px 0;
  }
  &__form {
    max-width: 480px;
    margin-top: 16px;
  }
}
</style>
