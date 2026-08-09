<template>
  <section class="governance-page">
    <div class="split-panel">
      <el-card shadow="never"><template #header>审批流程</template><div class="vertical-actions"><el-button :disabled="!canSubmit" type="primary" @click="submitApproval">提交方案</el-button><el-input-number v-model="approvalInstanceId" :min="1" placeholder="审批流程 ID" /><el-button :disabled="!canReview" @click="reviewApprove">财务复核通过</el-button><el-button :disabled="!canFinal" @click="finalApprove">最终审批通过</el-button><el-button :disabled="!canReject" type="danger" @click="rejectApproval">驳回</el-button></div><el-descriptions v-if="approval" :column="2" border class="task-summary"><el-descriptions-item label="流程 ID">{{ approval.id }}</el-descriptions-item><el-descriptions-item label="当前节点">{{ approvalNodeName(approval.currentNode) }}</el-descriptions-item><el-descriptions-item label="状态">{{ approvalStatusName(approval.status) }}</el-descriptions-item><el-descriptions-item label="方案 ID">{{ approval.scenarioId }}</el-descriptions-item></el-descriptions></el-card>
      <el-card shadow="never"><template #header>编辑锁</template><el-form label-position="top" class="dense-form"><el-form-item label="持有人 ID"><el-input-number v-model="lockForm.holderId" :min="1" :disabled="!wb.canUseLock" /></el-form-item><el-form-item label="持有人"><el-input v-model="lockForm.holderName" :disabled="!wb.canUseLock" /></el-form-item><el-form-item label="锁定时长（分钟）"><el-input-number v-model="lockForm.ttlMinutes" :min="1" :disabled="!wb.canUseLock" /></el-form-item><div class="form-actions"><el-button :disabled="!canAcquire" type="primary" @click="acquireLock">获取编辑锁</el-button><el-button :disabled="!canRelease" @click="releaseLock">释放编辑锁</el-button></div></el-form><el-descriptions v-if="editLock" :column="2" border class="task-summary"><el-descriptions-item label="持有人">{{ editLock.holderName }}</el-descriptions-item><el-descriptions-item label="到期时间">{{ editLock.expireAt }}</el-descriptions-item></el-descriptions></el-card>
    </div>
    <el-card shadow="never" class="bpm-card">
      <template #header>BPM 流程定义与追踪（FR-04-03）</template>
      <BpmPanel ref="bpmPanelRef" />
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { apiDelete, apiPost } from '@/shared/api/http'
import { useWorkbenchStore } from '@/stores/workbench'
import type { ApprovalInstance, EditLock } from '@/shared/types/domain'
import { approvalNodeName, approvalStatusName } from '@/shared/i18n/display'
import BpmPanel from '@/components/BpmPanel.vue'

const wb = useWorkbenchStore()
const approval = ref<ApprovalInstance | null>(null)
const editLock = ref<EditLock | null>(null)
const approvalInstanceId = ref<number>()
const lockForm = reactive({ holderId: 1, holderName: '投资分析师', ttlMinutes: 30 })
const bpmPanelRef = ref<InstanceType<typeof BpmPanel>>()

const canSubmit = computed(() => wb.canSubmitApproval)
const canReview = computed(() => wb.canReviewApproval && Boolean(approvalInstanceId.value))
const canFinal = computed(() => wb.canFinalApprove && Boolean(approvalInstanceId.value))
const canReject = computed(() => wb.canRejectApproval && Boolean(approvalInstanceId.value))
const canAcquire = computed(() => wb.canUseLock && !(editLock.value && editLock.value.holderId !== lockForm.holderId))
const canRelease = computed(() => wb.canUseLock && Boolean(editLock.value) && editLock.value?.holderId === lockForm.holderId)

async function submitApproval() { if (!wb.selectedScenario || !canSubmit.value) return wb.notifyForbidden(); approval.value = await apiPost<ApprovalInstance>(`/scenarios/${wb.selectedScenario.id}/approval/submit`, { comment: '从工作台提交审批' }); approvalInstanceId.value = approval.value.id }
async function reviewApprove() { if (!approvalInstanceId.value || !canReview.value) return wb.notifyForbidden(); approval.value = await apiPost<ApprovalInstance>(`/approval-instances/${approvalInstanceId.value}/review/approve`, { comment: '财务复核通过' }) }
async function finalApprove() { if (!approvalInstanceId.value || !canFinal.value) return wb.notifyForbidden(); approval.value = await apiPost<ApprovalInstance>(`/approval-instances/${approvalInstanceId.value}/approve`, { comment: '审批通过' }) }
async function rejectApproval() { if (!approvalInstanceId.value || !canReject.value) return wb.notifyForbidden(); approval.value = await apiPost<ApprovalInstance>(`/approval-instances/${approvalInstanceId.value}/reject`, { comment: '从工作台驳回' }) }
async function acquireLock() { if (!wb.selectedScenario || !canAcquire.value) return wb.notifyForbidden(); editLock.value = await apiPost<EditLock>(`/scenarios/${wb.selectedScenario.id}/lock`, lockForm) }
async function releaseLock() { if (!wb.selectedScenario || !canRelease.value) return wb.notifyForbidden(); await apiDelete(`/scenarios/${wb.selectedScenario.id}/lock`, { holderId: lockForm.holderId }); editLock.value = null; ElMessage.success('编辑锁已释放') }

// 提交/审批操作后联动刷新时间线
watch(approvalInstanceId, (id) => {
  if (id && bpmPanelRef.value) {
    bpmPanelRef.value.instanceIdInput = id
    bpmPanelRef.value.loadTimeline()
  }
})
</script>

<style scoped>
.governance-page { display: flex; flex-direction: column; gap: 14px; }
.bpm-card { width: 100%; }
</style>
