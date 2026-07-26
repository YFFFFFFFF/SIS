<template>
  <WorkbenchLayout>
    <section class="overview-grid">
      <el-card shadow="never">
        <template #header>Project Workflow</template>
        <p>Create projects, manage scenarios and maintain modeling inputs for M1 calculations.</p>
      </el-card>
      <el-card shadow="never">
        <template #header>Calculation Tasks</template>
        <p>Submit financial calculation tasks, poll status and inspect persisted metrics and cash-flow rows.</p>
      </el-card>
      <el-card shadow="never">
        <template #header>Governance</template>
        <p>Use report export, approval records, edit locks and audit logs to keep decisions traceable.</p>
      </el-card>
    </section>
  </WorkbenchLayout>
</template>

<script setup lang="ts">
import WorkbenchLayout from '@/layouts/WorkbenchLayout.vue'
</script>