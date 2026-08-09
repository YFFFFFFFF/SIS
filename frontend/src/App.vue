<template>
  <WorkbenchLayout v-if="auth.isAuthenticated" />
  <RouterView v-else />
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'

const auth = useAuthStore()
</script>
