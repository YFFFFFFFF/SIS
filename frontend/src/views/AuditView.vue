<template>
  <el-card shadow="never"><template #header>审计查询</template><div class="inline-actions"><el-input v-model="auditQuery.targetType" class="audit-input" placeholder="对象类型" /><el-input v-model="auditQuery.targetId" class="audit-input" placeholder="对象 ID" /><el-button :disabled="!wb.canQueryAudit" :loading="wb.loading.audit" type="primary" @click="loadAuditEvents">查询</el-button><el-button :disabled="!wb.canQueryAudit" :loading="wb.loading.audit" @click="verifyAuditChain">校验日志链</el-button></div><el-alert v-if="chainVerification" :closable="false" :type="chainVerification.intact ? 'success' : 'error'" style="margin-bottom:8px" :title="chainVerification.intact ? '日志链完整：' + chainVerification.linkedEvents + ' 条已链接事件校验通过（总数 ' + chainVerification.totalEvents + '）' : '日志链存在篡改迹象：' + chainVerification.brokenCount + ' 条事件校验失败，ID：' + chainVerification.brokenEventIds.join(', ')" /><el-table :data="pagedEvents" empty-text="暂无数据"><el-table-column prop="createdAt" label="时间" width="190" /><el-table-column label="操作" width="180"><template #default="{ row }">{{ auditActionName(row.action) }}</template></el-table-column><el-table-column label="对象类型" width="140"><template #default="{ row }">{{ targetTypeName(row.targetType) }}</template></el-table-column><el-table-column prop="targetId" label="对象 ID" width="120" /><el-table-column prop="afterValue" label="操作后内容" min-width="220" show-overflow-tooltip /><el-table-column label="链哈希" width="120"><template #default="{ row }"><span v-if="row.hash" :title="row.hash">{{ row.hash.substring(0, 8) }}…</span><span v-else style="color:#c0c4cc">未链接</span></template></el-table-column></el-table><Pager v-model:current-page="page" v-model:page-size="size" :total="auditEvents.length" /></el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { apiGet } from '@/shared/api/http'
import { useWorkbenchStore } from '@/stores/workbench'
import type { AuditChainVerification, AuditEvent } from '@/shared/types/domain'
import { auditActionName, targetTypeName } from '@/shared/i18n/display'
import Pager from '@/components/Pager.vue'

const wb = useWorkbenchStore()
const auditEvents = ref<AuditEvent[]>([])
const chainVerification = ref<AuditChainVerification | null>(null)
const auditQuery = reactive({ targetType: 'SCENARIO', targetId: '' })
const page = ref(1)
const size = ref(10)
const pagedEvents = computed(() => auditEvents.value.slice((page.value - 1) * size.value, page.value * size.value))

watch(() => wb.selectedScenario, (scenario) => { if (scenario) auditQuery.targetId = String(scenario.id) }, { immediate: true })

async function loadAuditEvents() { if (!wb.canQueryAudit) return wb.notifyForbidden(); wb.loading.audit = true; try { auditEvents.value = await apiGet<AuditEvent[]>(`/audit-events?targetType=${encodeURIComponent(auditQuery.targetType)}&targetId=${encodeURIComponent(auditQuery.targetId)}`); page.value = 1 } catch (err) { wb.notifyError(err) } finally { wb.loading.audit = false } }
async function verifyAuditChain() { if (!wb.canQueryAudit) return wb.notifyForbidden(); wb.loading.audit = true; try { chainVerification.value = await apiGet<AuditChainVerification>('/audit-events/chain/verify') } catch (err) { wb.notifyError(err) } finally { wb.loading.audit = false } }
</script>
