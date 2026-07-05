<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type {
  StudentDetail,
  TeacherScoreItem,
} from '@/types/api'
import { ApiErrorCode } from '@/types/api'
import {
  deleteScore as apiDeleteScore,
  listStudentScores,
  updateScore as apiUpdateScore,
} from '@/api/teacherScores'
import { getStudent } from '@/api/teacherStudents'
import { usePagination } from '@/composables/usePagination'
import { ApiError } from '@/utils/http'
import { translateErrorCode } from '@/utils/errorMap'
import EmptyState from '@/components/EmptyState.vue'
import ScoreBadge from '@/components/ScoreBadge.vue'

const route = useRoute()
const router = useRouter()

const studentId = computed<number>(() => Number(route.params.id))

const student = ref<StudentDetail | null>(null)
const items = ref<TeacherScoreItem[]>([])
const total = ref(0)
const loading = ref(false)

const { state, toPage, toSize } = usePagination({ defaultSize: 20, defaultSort: 'grade:desc' })

// ---------- 修改弹框状态 ----------
const dialogVisible = ref(false)
const editing = ref<TeacherScoreItem | null>(null)
const form = reactive({ score: 0 })
const fieldError = ref<string>('')

async function reloadStudent() {
  try {
    student.value = await getStudent(studentId.value)
  } catch (e) {
    if (e instanceof ApiError && e.code === ApiErrorCode.NOT_FOUND) {
      ElMessage.error('学生不存在')
      void router.replace({ name: 'teacher.scoreManage' })
    }
  }
}

async function reloadScores() {
  loading.value = true
  try {
    const sort = state.sort ? state.sort.replace(':', ',') : undefined
    const r = await listStudentScores(studentId.value, {
      page: state.page, size: state.size, sort,
    })
    items.value = r.items
    total.value = r.total
  } finally {
    loading.value = false
  }
}

function openEdit(row: TeacherScoreItem) {
  editing.value = row
  form.score = row.score
  fieldError.value = ''
  dialogVisible.value = true
}

function closeEdit() {
  dialogVisible.value = false
  editing.value = null
  fieldError.value = ''
}

function validate(): boolean {
  const s = form.score
  if (typeof s !== 'number' || Number.isNaN(s)) {
    fieldError.value = '请输入 0-100 之间的整数'
    return false
  }
  if (s < 0 || s > 100) {
    fieldError.value = '成绩必须在 0-100 之间'
    return false
  }
  if (!Number.isInteger(s)) {
    fieldError.value = '成绩必须为整数'
    return false
  }
  fieldError.value = ''
  return true
}

async function onSubmitEdit() {
  if (!editing.value) return
  if (!validate()) return
  try {
    await apiUpdateScore(editing.value.id, { score: form.score })
    ElMessage.success('修改成功')
    dialogVisible.value = false
    editing.value = null
    await reloadScores()
  } catch (e) {
    if (e instanceof ApiError) {
      if (e.code === ApiErrorCode.VALIDATION_FAILED && e.errors && e.errors.length > 0) {
        fieldError.value = e.errors[0].message ?? translateErrorCode(e.code)
      } else {
        ElMessage.error(translateErrorCode(e.code, e.message))
      }
    } else {
      ElMessage.error('修改失败，请稍后重试')
    }
  }
}

async function onDelete(row: TeacherScoreItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除《${row.subjectName}》(${row.score} 分) 这条成绩吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await apiDeleteScore(row.id)
    ElMessage.success('删除成功')
    await reloadScores()
  } catch (e) {
    if (e instanceof ApiError) {
      ElMessage.error(translateErrorCode(e.code, e.message))
    } else {
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

function goEntry() {
  void router.push({ name: 'teacher.scoreEntry', params: { id: studentId.value } })
}

function goBack() {
  void router.push({ name: 'teacher.scoreManage' })
}

onMounted(async () => {
  await reloadStudent()
  await reloadScores()
})
watch(
  () => [state.page, state.size],
  () => void reloadScores(),
)
</script>

<template>
  <div class="csm-score-detail">
    <div class="csm-score-detail__header">
      <h2>学生成绩详情</h2>
      <div class="csm-actions">
        <el-button @click="goBack">返回</el-button>
        <el-button type="primary" @click="goEntry">录入成绩</el-button>
      </div>
    </div>

    <el-descriptions v-if="student" :column="3" border class="csm-score-detail__meta">
      <el-descriptions-item label="学生 ID">{{ student.id }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ student.realName }}</el-descriptions-item>
      <el-descriptions-item label="年级">{{ student.grade }}</el-descriptions-item>
    </el-descriptions>

    <el-table
      v-if="items.length > 0"
      v-loading="loading"
      :data="items"
      stripe
    >
      <el-table-column prop="subjectName" label="课程" />
      <el-table-column prop="grade" label="学年" width="90" />
      <el-table-column label="成绩" width="120">
        <template #default="{ row }">
          <ScoreBadge :score="row.score" :is-failing="row.isFailing" />
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="200" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-tooltip
            :content="row.editable ? '修改成绩' : '仅可查看历史成绩'"
            :disabled="row.editable"
          >
            <el-button
              link
              type="primary"
              :disabled="!row.editable"
              @click="openEdit(row as TeacherScoreItem)"
            >修改</el-button>
          </el-tooltip>
          <el-tooltip
            :content="row.editable ? '删除成绩' : '仅可查看历史成绩'"
            :disabled="row.editable"
          >
            <el-button
              link
              type="danger"
              :disabled="!row.editable"
              @click="onDelete(row as TeacherScoreItem)"
            >删除</el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <EmptyState
      v-else-if="!loading"
      icon="📄"
      title="该学生暂无您所授课程的成绩记录"
      description="可点击右上角『录入成绩』新增"
    />

    <el-pagination
      v-if="total > 0"
      class="csm-score-detail__pager"
      :current-page="state.page"
      :page-size="state.size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      @current-change="toPage"
      @size-change="toSize"
    />

    <!-- 修改成绩弹框 -->
    <el-dialog
      v-model="dialogVisible"
      title="修改成绩"
      width="360px"
      :close-on-click-modal="false"
      @close="closeEdit"
    >
      <el-form :model="form" label-width="72px">
        <el-form-item
          label="课程"
        >
          <span>{{ editing?.subjectName }}</span>
        </el-form-item>
        <el-form-item
          label="成绩"
          prop="score"
          :error="fieldError"
        >
          <el-input-number
            v-model="form.score"
            :min="0"
            :max="100"
            :step="1"
            :precision="0"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeEdit">取消</el-button>
        <el-button type="primary" @click="onSubmitEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.csm-score-detail {
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  &__meta {
    margin: 16px 0;
  }
  &__pager {
    margin-top: 16px;
  }
}
.csm-actions {
  display: flex;
  gap: 8px;
}
</style>
