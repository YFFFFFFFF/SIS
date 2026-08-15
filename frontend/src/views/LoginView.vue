<template>
  <main class="login-page">
    <section class="login-panel">
      <img class="login-logo" src="../img/logo.png" alt="京东方" />
      <h1 class="login-system-name">投资测算系统</h1>
      <el-form class="login-form" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" size="large" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" autocomplete="current-password" size="large" show-password type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-alert v-if="error" :closable="false" :title="error" type="error" />
        <el-button :loading="loading" class="login-button" native-type="submit" size="large" type="primary">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({ username: 'investment_analyst', password: 'Password123!' })

onMounted(() => { document.title = '投资测算系统' })

async function submit() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirect)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>
