<script setup lang="ts">
interface Props {
  icon?: string
  title: string
  description?: string
  actionText?: string
}

const props = withDefaults(defineProps<Props>(), {
  icon: '📭',
  description: '',
  actionText: '',
})

const emit = defineEmits<{
  (e: 'action'): void
}>()

const { actionText } = props
</script>

<template>
  <div class="csm-empty">
    <div class="csm-empty__icon">{{ icon }}</div>
    <div class="csm-empty__title">{{ title }}</div>
    <div v-if="description" class="csm-empty__desc">{{ description }}</div>
    <el-button v-if="actionText" type="primary" @click="emit('action')">
      {{ actionText }}
    </el-button>
  </div>
</template>

<style scoped lang="scss">
.csm-empty {
  padding: var(--csm-spacing-xl) var(--csm-spacing-md);
  text-align: center;
  color: var(--csm-color-text-secondary);

  &__icon {
    font-size: 48px;
    margin-bottom: var(--csm-spacing-md);
  }
  &__title {
    font-size: var(--csm-font-size-lg);
    color: var(--csm-color-text);
    margin-bottom: var(--csm-spacing-sm);
  }
  &__desc {
    font-size: var(--csm-font-size-base);
    margin-bottom: var(--csm-spacing-md);
  }
}
</style>
