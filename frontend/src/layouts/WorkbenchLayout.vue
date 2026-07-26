<template>
  <el-container class="workbench-shell">
    <el-aside class="workbench-sidebar" width="248px">
      <div class="brand-block">
        <div class="brand-mark">ID</div>
        <div>
          <div class="brand-title">IIDS</div>
          <div class="brand-subtitle">Investment Workbench</div>
        </div>
      </div>
      <el-menu default-active="projects" class="shell-menu">
        <el-menu-item index="projects">
          <el-icon><Folder /></el-icon>
          <span>Projects</span>
        </el-menu-item>
        <el-menu-item index="calculation">
          <el-icon><DataAnalysis /></el-icon>
          <span>Calculation</span>
        </el-menu-item>
        <el-menu-item index="reports">
          <el-icon><Document /></el-icon>
          <span>Reports</span>
        </el-menu-item>
        <el-menu-item index="approval">
          <el-icon><Stamp /></el-icon>
          <span>Approval</span>
        </el-menu-item>
        <el-menu-item index="audit">
          <el-icon><Search /></el-icon>
          <span>Audit</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="workbench-header">
        <div>
          <h1>Investment Decision Support</h1>
          <p>Project, scenario and calculation operations</p>
        </div>
        <div class="header-actions">
          <el-tag effect="plain">{{ auth.displayName }}</el-tag>
          <el-button @click="logout">Sign out</el-button>
        </div>
      </el-header>
      <el-main class="workbench-main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { DataAnalysis, Document, Folder, Search, Stamp } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>