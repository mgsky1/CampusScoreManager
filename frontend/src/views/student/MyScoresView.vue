<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { listMyScores } from '@/api/studentScores'
import type { ScoreItem } from '@/types/api'
import { usePagination } from '@/composables/usePagination'
import EmptyState from '@/components/EmptyState.vue'
import ScoreBadge from '@/components/ScoreBadge.vue'

const items = ref<ScoreItem[]>([])
const total = ref(0)
const loading = ref(false)

const { state, toPage, toSize } = usePagination({ defaultSize: 50, defaultSort: 'grade:desc' })

async function reload() {
  loading.value = true
  try {
    const sort = state.sort ? state.sort.replace(':', ',') : undefined
    const r = await listMyScores({ page: state.page, size: state.size, sort })
    items.value = r.items
    total.value = r.total
  } finally {
    loading.value = false
  }
}

function onSortChange(payload: { prop: string | null; order: string | null }) {
  const prop = payload.prop
  const order = payload.order
  if (!order || !prop) {
    state.sort = ''
  } else {
    // element-plus: 'ascending' / 'descending'
    const dir = order === 'ascending' ? 'asc' : 'desc'
    const field = prop === 'score' ? 'score' : prop === 'subjectName' ? 'subject_name' : 'grade'
    state.sort = `${field}:${dir}`
  }
  state.page = 1
  void reload()
}

onMounted(() => void reload())
watch(() => [state.page, state.size], () => void reload())
</script>

<template>
  <div class="csm-scores">
    <h2>我的成绩</h2>
    <el-table
      v-if="items.length > 0"
      v-loading="loading"
      :data="items"
      stripe
      @sort-change="onSortChange"
    >
      <el-table-column prop="subjectName" label="课程" sortable="custom" />
      <el-table-column prop="teacherName" label="任课教师" />
      <el-table-column prop="grade" label="学年" sortable="custom" width="90" />
      <el-table-column prop="score" label="分数" sortable="custom" width="140">
        <template #default="{ row }">
          <ScoreBadge :score="row.score" :is-failing="row.isFailing" />
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
    </el-table>
    <EmptyState
      v-else-if="!loading"
      icon="📭"
      title="暂无成绩记录"
      description="等待教师录入成绩后再来查看"
    />
    <el-pagination
      v-if="total > 0"
      class="csm-scores__pager"
      :current-page="state.page"
      :page-size="state.size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      @current-change="toPage"
      @size-change="toSize"
    />
  </div>
</template>

<style scoped lang="scss">
.csm-scores__pager {
  margin-top: 16px;
}
</style>
