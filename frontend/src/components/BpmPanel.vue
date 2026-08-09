<template>
  <div class="bpm-panel">
    <div class="section-title">流程追踪时间线</div>
    <div class="timeline-bar">
      <el-input-number v-model="instanceIdInput" :min="1" placeholder="审批流程 ID" class="id-input" />
      <el-button :loading="loadingTimeline" @click="loadTimeline">查询时间线</el-button>
      <span v-if="timeline" class="hint">{{ timeline.flowName ?? '未绑定流程' }} · 状态：{{ approvalStatusName(timeline.status) }}</span>
    </div>
    <template v-if="timeline">
      <el-steps :active="activeStep" align-center finish-status="success" class="steps">
        <el-step v-for="n in timeline.flowNodes" :key="n.nodeCode" :title="n.nodeName" :description="roleName(n.approverRole)" :status="n.current ? 'process' : n.passed ? 'success' : 'wait'" />
        <el-step title="办结" :status="timeline.status === 'APPROVED' ? 'success' : timeline.status === 'REJECTED' ? 'error' : 'wait'" />
      </el-steps>
      <el-timeline class="events">
        <el-timeline-item v-for="(e, i) in timeline.events" :key="i" :timestamp="e.operatedAt" :type="e.decision === 'REJECT' ? 'danger' : e.decision === 'APPROVE' ? 'success' : 'primary'">
          {{ approvalNodeName(e.nodeCode) }} · {{ decisionName(e.decision) }}<span v-if="e.commentText" class="comment">「{{ e.commentText }}」</span>
        </el-timeline-item>
      </el-timeline>
    </template>
    <el-empty v-else description="输入审批流程 ID 查看节点进度与操作留痕" :image-size="80" />

    <template v-if="isAdmin">
      <div class="section-title">
        审批流定义（管理员）
        <el-button size="small" class="add-btn" @click="openCreate">+ 新建流程</el-button>
      </div>
      <el-table :data="flows" size="small" empty-text="暂无流程定义" class="tbl">
        <el-table-column prop="code" label="编码" width="180" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="节点链" min-width="240">
          <template #default="{ row }">{{ row.nodes.map((n: ApprovalFlowNode) => n.nodeName).join(' → ') }}</template>
        </el-table-column>
        <el-table-column label="默认" width="70" align="center">
          <template #default="{ row }"><el-tag v-if="row.isDefault" size="small" type="success">默认</el-tag></template>
        </el-table-column>
        <el-table-column label="启用" width="70" align="center">
          <template #default="{ row }"><el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" text type="danger" :disabled="row.isDefault" @click="removeFlow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <el-dialog v-model="dialogVisible" :title="editingFlow ? '编辑审批流' : '新建审批流'" width="640px">
      <el-form label-width="80px">
        <el-form-item label="编码"><el-input v-model="flowForm.code" :disabled="Boolean(editingFlow)" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="flowForm.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="flowForm.description" /></el-form-item>
        <el-form-item label="默认"><el-switch v-model="flowForm.isDefault" /></el-form-item>
        <el-form-item label="节点链">
          <div v-for="(n, i) in flowForm.nodes" :key="i" class="node-row">
            <el-input v-model="n.nodeName" placeholder="节点名" class="n-name" />
            <el-input v-model="n.nodeCode" placeholder="编码" class="n-code" />
            <el-select v-model="n.approverRole" placeholder="审批角色" class="n-role">
              <el-option v-for="r in roleOptions" :key="r" :label="roleName(r)" :value="r" />
            </el-select>
            <el-input v-model="n.conditionExpr" placeholder="条件规则（可选）" class="n-cond" />
            <el-button size="small" text type="danger" :disabled="flowForm.nodes.length <= 1" @click="flowForm.nodes.splice(i, 1)">删</el-button>
          </div>
          <el-button size="small" @click="addNode">+ 添加节点</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveFlow">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiDelete, apiGet, apiPost, apiPut } from '@/shared/api/http'
import type { ApprovalFlow, ApprovalFlowNode, ApprovalTimeline } from '@/shared/types/domain'
import { approvalNodeName, approvalStatusName, roleName } from '@/shared/i18n/display'
import { useWorkbenchStore } from '@/stores/workbench'

const wb = useWorkbenchStore()
const isAdmin = computed(() => wb.hasRole('ADMIN', 'SYSTEM_ADMINISTRATOR'))
const roleOptions = ['INVESTMENT_ANALYST', 'FINANCE_SPECIALIST', 'TECHNICAL_ENGINEER', 'PROJECT_MANAGER', 'SYSTEM_ADMINISTRATOR']

const instanceIdInput = ref<number>()
const timeline = ref<ApprovalTimeline | null>(null)
const loadingTimeline = ref(false)
const flows = ref<ApprovalFlow[]>([])
const dialogVisible = ref(false)
const editingFlow = ref<ApprovalFlow | null>(null)
const saving = ref(false)
const flowForm = reactive({ code: '', name: '', description: '', isDefault: false, nodes: [{ nodeCode: 'REVIEW', nodeName: '财务复核', approverRole: 'FINANCE_SPECIALIST', conditionExpr: '' }] as Array<{ nodeCode: string; nodeName: string; approverRole: string; conditionExpr: string }> })

const activeStep = computed(() => {
  if (!timeline.value) return 0
  const passed = timeline.value.flowNodes.filter(n => n.passed).length
  return timeline.value.status === 'APPROVED' || timeline.value.status === 'REJECTED' ? timeline.value.flowNodes.length + 1 : passed
})

async function loadTimeline() {
  if (!instanceIdInput.value) return
  loadingTimeline.value = true
  try {
    timeline.value = await apiGet<ApprovalTimeline>(`/approval-instances/${instanceIdInput.value}/timeline`)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '查询失败')
  } finally {
    loadingTimeline.value = false
  }
}

async function loadFlows() {
  if (!isAdmin.value) return
  try {
    flows.value = await apiGet<ApprovalFlow[]>('/admin/approval-flows')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载失败')
  }
}

function openCreate() {
  editingFlow.value = null
  Object.assign(flowForm, { code: '', name: '', description: '', isDefault: false, nodes: [{ nodeCode: 'REVIEW', nodeName: '财务复核', approverRole: 'FINANCE_SPECIALIST', conditionExpr: '' }] })
  dialogVisible.value = true
}

function openEdit(row: ApprovalFlow) {
  editingFlow.value = row
  Object.assign(flowForm, {
    code: row.code, name: row.name, description: row.description ?? '', isDefault: row.isDefault,
    nodes: row.nodes.map(n => ({ nodeCode: n.nodeCode, nodeName: n.nodeName, approverRole: n.approverRole, conditionExpr: n.conditionExpr ?? '' }))
  })
  dialogVisible.value = true
}

function addNode() {
  flowForm.nodes.push({ nodeCode: '', nodeName: '', approverRole: 'PROJECT_MANAGER', conditionExpr: '' })
}

async function saveFlow() {
  saving.value = true
  try {
    const payload = {
      code: flowForm.code, name: flowForm.name, description: flowForm.description,
      isDefault: flowForm.isDefault, enabled: true,
      nodes: flowForm.nodes.map((n, i) => ({ nodeCode: n.nodeCode, nodeName: n.nodeName, seq: i + 1, approverRole: n.approverRole, conditionExpr: n.conditionExpr || null }))
    }
    if (editingFlow.value) {
      await apiPut(`/admin/approval-flows/${editingFlow.value.id}`, payload)
    } else {
      await apiPost('/admin/approval-flows', payload)
    }
    ElMessage.success('审批流已保存')
    dialogVisible.value = false
    await loadFlows()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function removeFlow(row: ApprovalFlow) {
  try {
    await ElMessageBox.confirm(`确认删除审批流「${row.name}」？`, '删除审批流', { type: 'warning' })
    await apiDelete(`/admin/approval-flows/${row.id}`)
    ElMessage.success('已删除')
    await loadFlows()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err instanceof Error ? err.message : '删除失败')
  }
}

function decisionName(d: string) {
  return { SUBMIT: '提交', APPROVE: '通过', REJECT: '驳回' }[d] ?? d
}

onMounted(loadFlows)

defineExpose({ loadTimeline, instanceIdInput })
</script>

<style scoped>
.bpm-panel { display: flex; flex-direction: column; gap: 12px; }
.section-title { font-size: 13px; font-weight: 600; color: #374151; display: flex; align-items: center; gap: 10px; }
.add-btn { margin-left: auto; }
.timeline-bar { display: flex; align-items: center; gap: 10px; }
.id-input { width: 180px; }
.hint { font-size: 12px; color: #9ca3af; }
.steps { margin: 8px 0; }
.events { padding-left: 4px; }
.comment { color: #6b7280; margin-left: 6px; }
.tbl { width: 100%; }
.node-row { display: grid; grid-template-columns: 1fr 110px 150px 1fr 40px; gap: 6px; margin-bottom: 6px; }
</style>
