<template>
  <el-card shadow="never">
    <template #header>方案比选<span v-if="!wb.selectedProject" class="risk-hint">　横向对比需先在“项目管理”页选择项目</span></template>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="多方案横向对比（FR-03-01）" name="comparison">
        <ComparisonPanel :project-id="wb.selectedProject?.id ?? null" />
      </el-tab-pane>
      <el-tab-pane label="组合优化（FR-03-02）" name="portfolio">
        <PortfolioPanel />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ComparisonPanel from '@/components/ComparisonPanel.vue'
import PortfolioPanel from '@/components/PortfolioPanel.vue'
import { useWorkbenchStore } from '@/stores/workbench'

const wb = useWorkbenchStore()
const activeTab = ref('comparison')
</script>
