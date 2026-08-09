<template>
  <el-card shadow="never">
    <template #header>风险分析<span v-if="!wb.selectedScenario" class="risk-hint">　请先在“测算方案”页选择方案</span></template>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="敏感性分析（FR-02-01）" name="sensitivity">
        <SensitivityPanel :scenario-id="wb.selectedScenario?.id ?? null" />
      </el-tab-pane>
      <el-tab-pane label="目标反算（FR-02-02）" name="reverse">
        <ReversePanel :scenario-id="wb.selectedScenario?.id ?? null" />
      </el-tab-pane>
      <el-tab-pane label="盈亏平衡（FR-02-02）" name="breakeven">
        <BreakEvenPanel :scenario-id="wb.selectedScenario?.id ?? null" />
      </el-tab-pane>
      <el-tab-pane label="蒙特卡洛（FR-02-03）" name="montecarlo">
        <MonteCarloPanel :scenario-id="wb.selectedScenario?.id ?? null" />
      </el-tab-pane>
      <el-tab-pane label="风险预警（FR-02-04）" name="alerts">
        <RiskAlertPanel :scenario-id="wb.selectedScenario?.id ?? null" />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import SensitivityPanel from '@/components/SensitivityPanel.vue'
import ReversePanel from '@/components/ReversePanel.vue'
import BreakEvenPanel from '@/components/BreakEvenPanel.vue'
import MonteCarloPanel from '@/components/MonteCarloPanel.vue'
import RiskAlertPanel from '@/components/RiskAlertPanel.vue'
import { useWorkbenchStore } from '@/stores/workbench'

const wb = useWorkbenchStore()
const activeTab = ref('sensitivity')
</script>
