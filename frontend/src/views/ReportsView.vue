<template>
  <section class="stack-panel">
    <el-card shadow="never"><template #header>报告生成与下载</template><div class="inline-actions"><el-button :disabled="!wb.currentTask || !wb.canGenerateReport" :loading="wb.loading.report" type="primary" @click="generateReport('EXCEL')">生成Excel报告</el-button><el-button :disabled="!wb.currentTask || !wb.canGenerateReport" :loading="wb.loading.report" @click="generateReport('PDF')">生成PDF报告</el-button><el-button :disabled="!wb.currentReport" @click="downloadReport">下载报告</el-button><span v-if="!wb.currentTask" class="risk-hint">请先在“测算执行”页完成一次测算</span></div></el-card>
    <el-card shadow="never">
      <template #header>现金流量表</template>
      <el-tabs v-model="wb.activeStatementType" @tab-change="onStatementTabChange">
        <el-tab-pane v-for="t in statementTypes" :key="t" :label="statementTypeName(t)" :name="t" />
      </el-tabs>
      <el-table :data="pagedStatements" empty-text="暂无数据"><el-table-column prop="periodNo" label="期间" width="80" /><el-table-column prop="inflow" label="流入" /><el-table-column prop="outflow" label="流出" /><el-table-column prop="netCashFlow" label="净现金流" /><el-table-column prop="discountedCashFlow" label="折现现金流" /><el-table-column prop="cumulativeCashFlow" label="累计现金流" /></el-table>
      <Pager v-model:current-page="stmtPage" v-model:page-size="stmtSize" :total="wb.statementRows.length" />
    </el-card>
    <el-card shadow="never"><template #header>利润流向分解（达产年）</template><el-table :data="pagedProfitFlow" empty-text="暂无数据"><el-table-column prop="seq" label="序号" width="70" /><el-table-column prop="label" label="项目" min-width="180" /><el-table-column prop="value" label="金额" min-width="140" /></el-table><Pager v-model:current-page="profitPage" v-model:page-size="profitSize" :total="wb.profitFlow.length" /></el-card>
    <el-card shadow="never"><template #header>还本付息计划</template><el-table :data="pagedLoanSchedule" empty-text="暂无数据"><el-table-column prop="yearNo" label="运营年" width="80" /><el-table-column prop="openingBalance" label="期初余额" /><el-table-column prop="principalPaid" label="还本" /><el-table-column prop="interestPaid" label="付息" /><el-table-column prop="closingBalance" label="期末余额" /></el-table><Pager v-model:current-page="loanPage" v-model:page-size="loanSize" :total="wb.loanSchedule.length" /></el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiDownload, apiPost } from '@/shared/api/http'
import { useWorkbenchStore } from '@/stores/workbench'
import type { ReportDocument } from '@/shared/types/domain'
import { statementTypeName } from '@/shared/i18n/display'
import Pager from '@/components/Pager.vue'

const wb = useWorkbenchStore()
const statementTypes = ['PROJECT_CASH_FLOW', 'EQUITY_CASH_FLOW', 'FINANCIAL_PLAN']

const stmtPage = ref(1)
const stmtSize = ref(10)
const profitPage = ref(1)
const profitSize = ref(10)
const loanPage = ref(1)
const loanSize = ref(10)
const pagedStatements = computed(() => wb.statementRows.slice((stmtPage.value - 1) * stmtSize.value, stmtPage.value * stmtSize.value))
const pagedProfitFlow = computed(() => wb.profitFlow.slice((profitPage.value - 1) * profitSize.value, profitPage.value * profitSize.value))
const pagedLoanSchedule = computed(() => wb.loanSchedule.slice((loanPage.value - 1) * loanSize.value, loanPage.value * loanSize.value))
function onStatementTabChange() { stmtPage.value = 1; wb.loadStatements() }

async function generateReport(format: 'EXCEL' | 'PDF') { if (!wb.currentTask || !wb.canGenerateReport) return wb.notifyForbidden(); wb.loading.report = true; try { wb.currentReport = await apiPost<ReportDocument>(`/calculation-tasks/${wb.currentTask.id}/reports?format=${format}`); ElMessage.success(format === 'PDF' ? 'PDF 报告已生成' : 'Excel 报告已生成') } catch (err) { wb.notifyError(err) } finally { wb.loading.report = false } }
async function downloadReport() { if (!wb.currentReport) return; const blob = await apiDownload(`/reports/${wb.currentReport.id}/download`); const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = wb.currentReport.fileName; link.click(); URL.revokeObjectURL(url) }
</script>
