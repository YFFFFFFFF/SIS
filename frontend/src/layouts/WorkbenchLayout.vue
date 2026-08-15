<template>
  <div class="app-shell">
    <!-- 顶导（固定 · #005eba） -->
    <header class="app-header">
      <img class="hd-logo" src="../img/logo1.png" alt="京东方" />
      <span class="hd-title">投资测算系统</span>
      <div class="hd-user">
        <span class="uicon"><el-icon><UserFilled /></el-icon></span>
        <span class="uname">{{ auth.displayName }}</span>
        <el-button class="hd-logout" size="small" text @click="logout">退出登录</el-button>
      </div>
    </header>

    <div class="app-body">
      <!-- 侧导（固定 · 一级菜单默认展开） -->
      <aside class="app-sidebar">
        <el-menu :default-active="activePath" :default-openeds="openeds" class="shell-menu" router>
          <el-menu-item index="/dashboard">
            <el-icon><Odometer /></el-icon>
            <span>工作台看板</span>
          </el-menu-item>

          <el-sub-menu index="biz">
            <template #title><el-icon><Folder /></el-icon><span>核心业务</span></template>
            <el-menu-item index="/projects"><el-icon><Folder /></el-icon><span>项目管理</span></el-menu-item>
            <el-menu-item index="/scenarios"><el-icon><Files /></el-icon><span>测算方案</span></el-menu-item>
            <el-menu-item index="/inputs"><el-icon><EditPen /></el-icon><span>测算输入</span></el-menu-item>
            <el-menu-item index="/calculation"><el-icon><DataAnalysis /></el-icon><span>测算执行</span></el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="analysis">
            <template #title><el-icon><Warning /></el-icon><span>分析与决策</span></template>
            <el-menu-item index="/risk"><el-icon><Warning /></el-icon><span>风险分析</span></el-menu-item>
            <el-menu-item index="/compare"><el-icon><ScaleToOriginal /></el-icon><span>方案比选</span></el-menu-item>
            <el-menu-item index="/collab"><el-icon><UserFilled /></el-icon><span>协同编辑</span></el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="asset">
            <template #title><el-icon><Collection /></el-icon><span>资产与报告</span></template>
            <el-menu-item index="/library"><el-icon><Collection /></el-icon><span>项目库</span></el-menu-item>
            <el-menu-item index="/reports"><el-icon><Document /></el-icon><span>报告中心</span></el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="sys">
            <template #title><el-icon><Stamp /></el-icon><span>系统治理</span></template>
            <el-menu-item index="/governance"><el-icon><Stamp /></el-icon><span>流程治理</span></el-menu-item>
            <el-menu-item index="/audit"><el-icon><Search /></el-icon><span>审计日志</span></el-menu-item>
          </el-sub-menu>
        </el-menu>
      </aside>

      <!-- 主列：主内容区 + Footer -->
      <div class="app-main-col">
        <main class="app-main">
          <div class="content-wrapper">
            <RouterView />
          </div>
        </main>
        <footer class="app-footer">CopyRight© 京东方科技集团股份有限公司版权所有</footer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, DataAnalysis, Document, EditPen, Files, Folder, Odometer, ScaleToOriginal, Search, Stamp, UserFilled, Warning } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const activePath = computed(() => route.path)
// 侧导当前菜单的一级菜单保持展开
const openeds = ['biz', 'analysis', 'asset', 'sys']

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>
