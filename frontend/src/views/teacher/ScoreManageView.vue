<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { searchStudents } from '@/api/teacherStudents'
import type { StudentDetail } from '@/types/api'
import { usePagination } from '@/composables/usePagination'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

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

function goDetail(row: StudentDetail) {
  void router.push({ name: 'teacher.scoreDetail', params: { id: row.id } })
}
function goEntry(row: StudentDetail) {
  void router.push({ name: 'teacher.scoreEntry', params: { id: row.id } })
}

onMounted(() => void reload())
watch(() => [state.page, state.size], () => void reload())
</script>

<template>
  <div class="csm-score-manage">
    <h2>成绩管理</h2>
    <el-form inline class="csm-score-manage__filter" @submit.prevent="onSearch">
      <el-form-item label="姓名">
        <el-input
          v-model="keywordInput"
          placeholder="输入学生姓名"
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
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="goDetail(row as StudentDetail)">查看成绩</el-button>
          <el-button link type="primary" @click="goEntry(row as StudentDetail)">录入成绩</el-button>
        </template>
      </el-table-column>
    </el-table>
    <EmptyState
      v-else-if="!loading"
      icon="👨‍🎓"
      title="暂无学生"
      description="请调整搜索条件后重试"
    />

    <el-pagination
      v-if="total > 0"
      class="csm-score-manage__pager"
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
.csm-score-manage {
  &__filter {
    margin-bottom: 16px;
  }
  &__pager {
    margin-top: 16px;
  }
}
</style>
