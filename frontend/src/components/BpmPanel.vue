<template>
  <div class="bpm-panel">
    <el-alert title="当前测试阶段采用固定审批流" description="提交 → 财务复核 → 项目经理审批。流程定义配置已冻结。" type="info" :closable="false" show-icon />
    <div class="timeline-bar">
      <el-input-number v-model="instanceIdInput" :min="1" placeholder="审批流程 ID" class="id-input" />
      <el-button :loading="loading" @click="loadTimeline">查询时间线</el-button>
      <span v-if="timeline" class="hint">{{ timeline.flowName ?? '固定审批流' }} · {{ approvalStatusName(timeline.status) }}</span>
    </div>
    <template v-if="timeline">
      <el-steps :active="activeStep" align-center finish-status="success">
        <el-step v-for="node in timeline.flowNodes" :key="node.nodeCode" :title="node.nodeName" :description="roleName(node.approverRole)" :status="node.current ? 'process' : node.passed ? 'success' : 'wait'" />
        <el-step title="办结" :status="timeline.status === 'APPROVED' ? 'success' : timeline.status === 'REJECTED' ? 'error' : 'wait'" />
      </el-steps>
      <el-timeline>
        <el-timeline-item v-for="(event, index) in timeline.events" :key="index" :timestamp="event.operatedAt" :type="event.decision === 'REJECT' ? 'danger' : event.decision === 'APPROVE' ? 'success' : 'primary'">
          {{ approvalNodeName(event.nodeCode) }} · {{ decisionName(event.decision) }}<span v-if="event.commentText">「{{ event.commentText }}」</span>
        </el-timeline-item>
      </el-timeline>
    </template>
    <el-empty v-else description="输入审批流程 ID 查看进度" :image-size="80" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet } from '@/shared/api/http'
import type { ApprovalTimeline } from '@/shared/types/domain'
import { approvalNodeName, approvalStatusName, roleName } from '@/shared/i18n/display'

const instanceIdInput = ref<number>()
const timeline = ref<ApprovalTimeline | null>(null)
const loading = ref(false)
const activeStep = computed(() => timeline.value ? timeline.value.flowNodes.filter(node => node.passed).length : 0)

async function loadTimeline() {
  if (!instanceIdInput.value) return
  loading.value = true
  try { timeline.value = await apiGet<ApprovalTimeline>(`/approval-instances/${instanceIdInput.value}/timeline`) }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '查询失败') }
  finally { loading.value = false }
}

function decisionName(value: string) { return { SUBMIT: '提交', APPROVE: '通过', REJECT: '驳回' }[value] ?? value }
defineExpose({ loadTimeline, instanceIdInput })
</script>

<style scoped>
.bpm-panel { display: flex; flex-direction: column; gap: 14px; }
.timeline-bar { display: flex; align-items: center; gap: 10px; }
.id-input { width: 180px; }
.hint { font-size: 12px; color: #6b7280; }
</style>
