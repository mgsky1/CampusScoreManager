<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

onMounted(() => {
  auth.hydrateFromStorage()
})
</script>

<template>
  <RouterView />
</template>

<style lang="scss">
@import '@/styles/tokens.scss';

html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: var(--csm-font-family);
  color: var(--csm-color-text);
  background: var(--csm-color-bg);
}

* {
  box-sizing: border-box;
}
</style>
