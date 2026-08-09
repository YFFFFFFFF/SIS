<template>
  <section class="triple-panel">
    <el-card shadow="never">
      <template #header>测算参数<span v-if="!wb.selectedScenario" class="risk-hint">　请先在“测算方案”页选择方案</span></template>
      <el-alert v-if="lockedParamLabels.length" type="warning" :closable="false" class="lock-alert"
                :title="'以下字段正被他人锁定编辑，暂不可修改：' + lockedParamLabels.join('、')" show-icon />
      <el-form label-position="top" class="dense-form two-cols" :disabled="!wb.canSaveParameters">
        <el-form-item><template #label>WACC<el-tag v-if="lockOf('wacc')" size="small" :type="isLockedByOther('wacc')?'warning':'success'" class="lk">{{ isLockedByOther('wacc') ? lockOf('wacc')!.holderName+'锁' : '我锁' }}</el-tag></template><el-input-number v-model="wb.parameterForm.wacc" :precision="4" :step="0.01" :disabled="isLockedByOther('wacc')" /></el-form-item><el-form-item><template #label>所得税率<el-tag v-if="lockOf('taxRate')" size="small" :type="isLockedByOther('taxRate')?'warning':'success'" class="lk">{{ isLockedByOther('taxRate') ? lockOf('taxRate')!.holderName+'锁' : '我锁' }}</el-tag></template><el-input-number v-model="wb.parameterForm.taxRate" :precision="4" :step="0.01" :disabled="isLockedByOther('taxRate')" /></el-form-item>
        <el-form-item><template #label>折旧年限<el-tag v-if="lockOf('depreciationYears')" size="small" :type="isLockedByOther('depreciationYears')?'warning':'success'" class="lk">{{ isLockedByOther('depreciationYears') ? lockOf('depreciationYears')!.holderName+'锁' : '我锁' }}</el-tag></template><el-input-number v-model="wb.parameterForm.depreciationYears" :min="1" :disabled="isLockedByOther('depreciationYears')" /></el-form-item><el-form-item><template #label>残值率<el-tag v-if="lockOf('residualRate')" size="small" :type="isLockedByOther('residualRate')?'warning':'success'" class="lk">{{ isLockedByOther('residualRate') ? lockOf('residualRate')!.holderName+'锁' : '我锁' }}</el-tag></template><el-input-number v-model="wb.parameterForm.residualRate" :precision="4" :step="0.01" :disabled="isLockedByOther('residualRate')" /></el-form-item>
        <el-form-item><template #label>贷款比例上限<el-tag v-if="lockOf('loanRatioLimit')" size="small" :type="isLockedByOther('loanRatioLimit')?'warning':'success'" class="lk">{{ isLockedByOther('loanRatioLimit') ? lockOf('loanRatioLimit')!.holderName+'锁' : '我锁' }}</el-tag></template><el-input-number v-model="wb.parameterForm.loanRatioLimit" :precision="4" :step="0.01" :disabled="isLockedByOther('loanRatioLimit')" /></el-form-item><el-form-item><template #label>单位售价<el-tag v-if="lockOf('pricePerUnit')" size="small" :type="isLockedByOther('pricePerUnit')?'warning':'success'" class="lk">{{ isLockedByOther('pricePerUnit') ? lockOf('pricePerUnit')!.holderName+'锁' : '我锁' }}</el-tag></template><el-input-number v-model="wb.parameterForm.pricePerUnit" :min="0" :disabled="isLockedByOther('pricePerUnit')" /></el-form-item>
        <el-form-item><template #label>单位成本<el-tag v-if="lockOf('unitCost')" size="small" :type="isLockedByOther('unitCost')?'warning':'success'" class="lk">{{ isLockedByOther('unitCost') ? lockOf('unitCost')!.holderName+'锁' : '我锁' }}</el-tag></template><el-input-number v-model="wb.parameterForm.unitCost" :min="0" :disabled="isLockedByOther('unitCost')" /></el-form-item><el-form-item><template #label>年产量<el-tag v-if="lockOf('annualOutput')" size="small" :type="isLockedByOther('annualOutput')?'warning':'success'" class="lk">{{ isLockedByOther('annualOutput') ? lockOf('annualOutput')!.holderName+'锁' : '我锁' }}</el-tag></template><el-input-number v-model="wb.parameterForm.annualOutput" :min="0" :disabled="isLockedByOther('annualOutput')" /></el-form-item>
        <el-form-item><template #label>固定运营成本<el-tag v-if="lockOf('fixedOperatingCost')" size="small" :type="isLockedByOther('fixedOperatingCost')?'warning':'success'" class="lk">{{ isLockedByOther('fixedOperatingCost') ? lockOf('fixedOperatingCost')!.holderName+'锁' : '我锁' }}</el-tag></template><el-input-number v-model="wb.parameterForm.fixedOperatingCost" :min="0" :disabled="isLockedByOther('fixedOperatingCost')" /></el-form-item><el-form-item label="公式版本"><el-input v-model="wb.parameterForm.formulaVersion" /></el-form-item>
        <el-form-item label="折旧政策"><el-select v-model="wb.parameterForm.depreciationPolicy"><el-option label="年限平均法" value="STRAIGHT_LINE" /><el-option label="双倍余额递减法" value="DOUBLE_DECLINING" /><el-option label="年数总和法" value="SUM_OF_YEARS_DIGITS" /></el-select></el-form-item>
        <el-form-item label="摊销年限（0=不摊销）"><el-input-number v-model="wb.parameterForm.amortizationYears" :min="0" /></el-form-item>
        <el-form-item label="摊销基数"><el-input-number v-model="wb.parameterForm.amortizableAmount" :min="0" /></el-form-item>
        <el-form-item label="还款方式"><el-select v-model="wb.parameterForm.repaymentMethod"><el-option label="等额本金" value="EQUAL_PRINCIPAL" /><el-option label="等额本息" value="EQUAL_PAYMENT" /><el-option label="到期一次还本" value="BULLET" /></el-select></el-form-item>
        <el-form-item label="税率梯度(JSON)" class="span-2"><el-input v-model="wb.parameterForm.taxSchedule" placeholder='[{"fromYear":1,"toYear":3,"rate":0}]' /></el-form-item>
        <el-form-item label="投产负荷(JSON)" class="span-2"><el-input v-model="wb.parameterForm.rampUp" placeholder='[{"year":1,"loadFactor":0.6}]' /></el-form-item>
      </el-form>
      <el-button :disabled="!wb.canSaveParameters" :loading="wb.loading.parameters" type="primary" @click="saveParameters">保存参数</el-button>
    </el-card>
    <el-card shadow="never">
      <template #header>投资项目</template>
      <el-form label-position="top" class="dense-form">
        <el-form-item label="投资类别"><el-select v-model="wb.investmentForm.category" :disabled="!wb.canAddInvestment"><el-option label="建设投资" value="CONSTRUCTION" /><el-option label="建筑工程费" value="CONSTRUCTION_BUILDING" /><el-option label="设备购置及安装费" value="CONSTRUCTION_EQUIPMENT" /><el-option label="工程建设其他费用" value="CONSTRUCTION_OTHER" /><el-option label="流动资金" value="WORKING_CAPITAL" /><el-option label="建设期利息" value="INTEREST_DURING_CONSTRUCTION" /></el-select></el-form-item>
        <el-form-item label="项目名称"><el-input v-model="wb.investmentForm.name" :disabled="!wb.canAddInvestment" /></el-form-item><el-form-item label="金额"><el-input-number v-model="wb.investmentForm.amount" :disabled="!wb.canAddInvestment" :min="0" /></el-form-item><el-form-item label="发生年份"><el-input-number v-model="wb.investmentForm.yearNo" :disabled="!wb.canAddInvestment" :min="0" /></el-form-item>
        <el-form-item label="分项编码"><el-input v-model="wb.investmentForm.itemCode" :disabled="!wb.canAddInvestment" placeholder="选填" /></el-form-item>
        <el-button :disabled="!wb.canAddInvestment" :loading="wb.loading.inputs" type="primary" @click="addInvestmentItem">新增投资项目</el-button>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <template #header>成本分项</template>
      <el-form label-position="top" class="dense-form">
        <el-form-item label="成本类别"><el-select v-model="wb.costForm.category" :disabled="!wb.canAddCost"><el-option label="外购原材料及燃料动力" value="RAW_MATERIAL" /><el-option label="人工及制造费用" value="LABOR_MANUFACTURING" /><el-option label="其他经营成本" value="OTHER_OPERATING" /></el-select></el-form-item>
        <el-form-item label="分项名称"><el-input v-model="wb.costForm.name" :disabled="!wb.canAddCost" /></el-form-item><el-form-item label="金额"><el-input-number v-model="wb.costForm.amount" :disabled="!wb.canAddCost" :min="0" /></el-form-item><el-form-item label="运营年(0=达产年)"><el-input-number v-model="wb.costForm.yearNo" :disabled="!wb.canAddCost" :min="0" /></el-form-item>
        <el-button :disabled="!wb.canAddCost" :loading="wb.loading.inputs" type="primary" @click="addCostItem">新增成本分项</el-button>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <template #header>融资方案</template>
      <el-form label-position="top" class="dense-form">
        <el-form-item label="资金来源"><el-select v-model="wb.financingForm.sourceType" :disabled="!wb.canAddFinancing"><el-option label="资本金" value="EQUITY" /><el-option label="银行贷款" value="LOAN" /></el-select></el-form-item><el-form-item label="比例"><el-input-number v-model="wb.financingForm.ratio" :disabled="!wb.canAddFinancing" :precision="4" :step="0.1" /></el-form-item>
        <el-form-item label="金额"><el-input-number v-model="wb.financingForm.amount" :disabled="!wb.canAddFinancing" :min="0" /></el-form-item><el-form-item label="利率"><el-input-number v-model="wb.financingForm.interestRate" :disabled="!wb.canAddFinancing" :precision="4" :step="0.01" /></el-form-item><el-form-item label="期限（年）"><el-input-number v-model="wb.financingForm.termYears" :disabled="!wb.canAddFinancing" :min="0" /></el-form-item>
        <el-form-item label="还款方式"><el-select v-model="wb.financingForm.repaymentMethod" :disabled="!wb.canAddFinancing"><el-option label="等额本金" value="EQUAL_PRINCIPAL" /><el-option label="等额本息" value="EQUAL_PAYMENT" /><el-option label="到期一次还本" value="BULLET" /></el-select></el-form-item><el-form-item label="宽限期（年）"><el-input-number v-model="wb.financingForm.graceYears" :disabled="!wb.canAddFinancing" :min="0" /></el-form-item>
        <el-button :disabled="!wb.canAddFinancing" :loading="wb.loading.inputs" type="primary" @click="addFinancingPlan">新增融资方案</el-button>
      </el-form>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet, apiPost, apiPut } from '@/shared/api/http'
import { useWorkbenchStore } from '@/stores/workbench'
import { useAuthStore } from '@/stores/auth'
import type { FieldLock } from '@/shared/types/domain'

const wb = useWorkbenchStore()
const auth = useAuthStore()

// R-15c 字段锁感知：他人锁定的参数项禁用，顶部提示
const fieldLocks = ref<FieldLock[]>([])
const myName = computed(() => auth.displayName)
const lockByField = computed(() => {
  const m = new Map<string, FieldLock>()
  fieldLocks.value.forEach(l => m.set(l.fieldKey, l))
  return m
})
/** 被他人锁定的参数字段中文名列表（用于提示条）。 */
const lockedParamLabels = computed(() => {
  const names: string[] = []
  fieldLocks.value.forEach(l => {
    if (l.fieldKey.startsWith('param.') && l.holderName !== myName.value) {
      names.push(`${paramLabel(l.fieldKey.slice(6))}（${l.holderName}）`)
    }
  })
  return names
})
function lockOf(paramKey: string) { return lockByField.value.get('param.' + paramKey) }
function isLockedByOther(paramKey: string) {
  const l = lockOf(paramKey)
  return !!l && l.holderName !== myName.value
}
function paramLabel(key: string) {
  return ({
    wacc: 'WACC', taxRate: '所得税率', depreciationYears: '折旧年限', residualRate: '残值率',
    loanRatioLimit: '贷款比例上限', pricePerUnit: '单位售价', unitCost: '单位成本', annualOutput: '年产量',
    fixedOperatingCost: '固定运营成本', depreciationPolicy: '折旧政策', amortizationYears: '摊销年限',
    amortizableAmount: '摊销基数', repaymentMethod: '还款方式', taxSchedule: '税率梯度', rampUp: '投产负荷'
  } as Record<string, string>)[key] ?? key
}
async function loadLocks() {
  if (!wb.selectedScenario) { fieldLocks.value = []; return }
  try { fieldLocks.value = await apiGet<FieldLock[]>(`/scenarios/${wb.selectedScenario.id}/field-locks`) } catch { /* 静默 */ }
}
watch(() => wb.selectedScenario?.id, loadLocks, { immediate: true })
// 30s 轮询锁状态（输入页无 SSE，轻量同步）
const lockTimer = setInterval(loadLocks, 30000)
onBeforeUnmount(() => clearInterval(lockTimer))

async function saveParameters() { if (!wb.selectedScenario || !wb.canSaveParameters) return wb.notifyForbidden(); wb.loading.parameters = true; try { await apiPut(`/scenarios/${wb.selectedScenario.id}/parameters`, wb.parameterForm); ElMessage.success('测算参数已保存'); loadLocks() } catch (err) { wb.notifyError(err) } finally { wb.loading.parameters = false } }
async function addInvestmentItem() { if (!wb.selectedScenario || !wb.canAddInvestment) return wb.notifyForbidden(); wb.loading.inputs = true; try { await apiPost(`/scenarios/${wb.selectedScenario.id}/investment-items`, wb.investmentForm); ElMessage.success('投资项目已新增') } catch (err) { wb.notifyError(err) } finally { wb.loading.inputs = false } }
async function addCostItem() { if (!wb.selectedScenario || !wb.canAddCost) return wb.notifyForbidden(); wb.loading.inputs = true; try { await apiPost(`/scenarios/${wb.selectedScenario.id}/cost-items`, wb.costForm); ElMessage.success('成本分项已新增') } catch (err) { wb.notifyError(err) } finally { wb.loading.inputs = false } }
async function addFinancingPlan() { if (!wb.selectedScenario || !wb.canAddFinancing) return wb.notifyForbidden(); wb.loading.inputs = true; try { await apiPost(`/scenarios/${wb.selectedScenario.id}/financing-plans`, wb.financingForm); ElMessage.success('融资方案已新增') } catch (err) { wb.notifyError(err) } finally { wb.loading.inputs = false } }
</script>

<style scoped>
.lock-alert { margin-bottom: 12px; }
.lk { margin-left: 6px; vertical-align: middle; }
</style>
