<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  score: number
  /** 显式失败判定；若不传则按 <60 判定。 */
  isFailing?: boolean
  /** 及格线，默认 60。 */
  passLine?: number
}

const props = withDefaults(defineProps<Props>(), {
  isFailing: undefined,
  passLine: 60,
})

const failing = computed<boolean>(() => {
  return props.isFailing ?? props.score < props.passLine
})
</script>

<template>
  <span :class="['csm-score-badge', failing ? 'is-failing' : 'is-passing']">
    {{ score }}
    <span v-if="failing" class="csm-score-badge__tag">不及格</span>
  </span>
</template>

<style scoped lang="scss">
.csm-score-badge {
  display: inline-flex;
  align-items: baseline;
  gap: var(--csm-spacing-xs);
  font-variant-numeric: tabular-nums;
  font-weight: 600;

  &.is-passing {
    color: var(--csm-color-text);
  }
  &.is-failing {
    color: var(--csm-color-danger);
  }

  &__tag {
    font-size: var(--csm-font-size-sm);
    font-weight: 400;
    padding: 0 var(--csm-spacing-xs);
    border: 1px solid var(--csm-color-danger);
    border-radius: var(--csm-radius-sm);
    color: var(--csm-color-danger);
    background: rgba(220, 38, 38, 0.08);
  }
}
</style>
