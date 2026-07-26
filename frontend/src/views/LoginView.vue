<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <div class="brand-mark large">ID</div>
        <h1>IIDS Workbench</h1>
        <p>Use your analyst account to access project modeling, calculation tasks and approval workflows.</p>
      </div>
      <el-form class="login-form" label-position="top" @submit.prevent="submit">
        <el-form-item label="Username">
          <el-input v-model="form.username" autocomplete="username" size="large" />
        </el-form-item>
        <el-form-item label="Password">
          <el-input v-model="form.password" autocomplete="current-password" size="large" show-password type="password" />
        </el-form-item>
        <el-alert v-if="error" :closable="false" :title="error" type="error" />
        <el-button :loading="loading" class="login-button" native-type="submit" size="large" type="primary">
          Sign in
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({ username: 'analyst', password: 'Password123!' })

async function submit() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirect)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Login failed'
  } finally {
    loading.value = false
  }
}
</script>