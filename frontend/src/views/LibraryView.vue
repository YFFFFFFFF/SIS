<template>
  <div class="library-page">
    <el-card shadow="never">
      <template #header>项目库（FR-03-03）——多维检索与知识沉淀</template>
      <div class="filter-bar">
        <el-input v-model="filters.keyword" placeholder="编码/名称关键字" clearable class="f-kw" @change="load" />
        <el-select v-model="filters.status" placeholder="状态" clearable class="f-sel" @change="load">
          <el-option label="草稿" value="DRAFT" /><el-option label="启用" value="ACTIVE" /><el-option label="已归档" value="ARCHIVED" />
        </el-select>
        <el-select v-model="filters.projectType" placeholder="类型" clearable class="f-sel" @change="load">
          <el-option label="产业项目" value="INDUSTRIAL" /><el-option label="基础设施" value="INFRASTRUCTURE" /><el-option label="科技项目" value="TECHNOLOGY" /><el-option label="其他" value="OTHER" />
        </el-select>
        <el-input v-model="filters.tag" placeholder="标签" clearable class="f-tag" @change="load" />
        <el-button type="primary" :loading="loading" @click="load">检索</el-button>
      </div>
      <el-table :data="pagedItems" size="small" empty-text="暂无数据" class="tbl">
        <el-table-column prop="code" label="编码" width="130" />
        <el-table-column prop="name" label="项目名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ projectTypeName(row.projectType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : row.status === 'ARCHIVED' ? 'info' : 'warning'">{{ projectStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="140">
          <template #default="{ row }">
            <el-tag v-for="t in row.tags" :key="t" size="small" effect="plain" class="tag">{{ t }}</el-tag>
            <span v-if="!row.tags.length" class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="最新 NPV" width="120" align="right">
          <template #default="{ row }"><span class="num">{{ fmt(row.latestNpv) }}</span></template>
        </el-table-column>
        <el-table-column label="最新 IRR" width="90" align="right">
          <template #default="{ row }"><span class="num">{{ row.latestIrr != null ? (row.latestIrr * 100).toFixed(1) + '%' : '—' }}</span></template>
        </el-table-column>
        <el-table-column label="复盘" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.hasReview" size="small" type="success">已复盘</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openTags(row)">标签</el-button>
            <el-button size="small" text type="primary" @click="openReview(row)">复盘</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pager v-model:current-page="page" v-model:page-size="size" :total="items.length" />
    </el-card>

    <el-dialog v-model="tagsDialogVisible" :title="`标签管理 — ${activeProject?.name ?? ''}`" width="440px">
      <el-select v-model="editTags" multiple filterable allow-create default-first-option placeholder="输入后回车创建标签" class="full" />
      <template #footer>
        <el-button @click="tagsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTags">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialogVisible" :title="`项目复盘 — ${activeProject?.name ?? ''}`" width="720px">
      <template v-if="review">
        <el-descriptions :column="4" border size="small" class="compare-tbl">
          <el-descriptions-item label="指标">　</el-descriptions-item>
          <el-descriptions-item label="计划（测算）">　</el-descriptions-item>
          <el-descriptions-item label="实际（投运）">　</el-descriptions-item>
          <el-descriptions-item label="偏差">　</el-descriptions-item>
          <el-descriptions-item label="NPV">　</el-descriptions-item>
          <el-descriptions-item><span class="num">{{ fmt(review.plannedNpv) }}</span></el-descriptions-item>
          <el-descriptions-item><span class="num">{{ fmt(review.actualNpv) }}</span></el-descriptions-item>
          <el-descriptions-item><span class="num" :class="devClass(review.npvDeviation)">{{ dev(review.npvDeviation) }}</span></el-descriptions-item>
          <el-descriptions-item label="IRR">　</el-descriptions-item>
          <el-descriptions-item><span class="num">{{ pct(review.plannedIrr) }}</span></el-descriptions-item>
          <el-descriptions-item><span class="num">{{ pct(review.actualIrr) }}</span></el-descriptions-item>
          <el-descriptions-item><span class="num" :class="devClass(review.irrDeviation)">{{ dev(review.irrDeviation) }}</span></el-descriptions-item>
          <el-descriptions-item label="总投资">　</el-descriptions-item>
          <el-descriptions-item><span class="num">{{ fmt(review.plannedInvestment) }}</span></el-descriptions-item>
          <el-descriptions-item><span class="num">{{ fmt(review.actualInvestment) }}</span></el-descriptions-item>
          <el-descriptions-item>　</el-descriptions-item>
          <el-descriptions-item label="回收期(年)">　</el-descriptions-item>
          <el-descriptions-item><span class="num">{{ fmt(review.plannedPaybackYears) }}</span></el-descriptions-item>
          <el-descriptions-item><span class="num">{{ fmt(review.actualPaybackYears) }}</span></el-descriptions-item>
          <el-descriptions-item>　</el-descriptions-item>
        </el-descriptions>
        <el-alert v-if="review.lessons" type="info" :closable="false" title="经验教训" :description="review.lessons" class="lessons" />
        <div class="review-meta">对照方案：{{ review.scenarioName ?? '—' }} · 投产日期：{{ review.operationStartDate ?? '—' }} · 记录人：{{ review.createdBy ?? '—' }}</div>
      </template>
      <el-empty v-else description="暂无复盘记录，在下方录入" :image-size="70" />

      <el-divider content-position="left">{{ review ? '更新复盘' : '录入复盘' }}</el-divider>
      <el-form label-width="110px" class="review-form">
        <el-form-item label="对照测算方案">
          <el-select v-model="reviewForm.scenarioId" class="full" placeholder="选择本项目的方案（取计划指标）">
            <el-option v-for="s in projectScenarios" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="实际 NPV"><el-input-number v-model="reviewForm.actualNpv" :precision="2" controls-position="right" class="full" /></el-form-item>
          <el-form-item label="实际 IRR（小数）"><el-input-number v-model="reviewForm.actualIrr" :precision="4" :step="0.01" controls-position="right" class="full" /></el-form-item>
          <el-form-item label="实际总投资"><el-input-number v-model="reviewForm.actualInvestment" :precision="2" controls-position="right" class="full" /></el-form-item>
          <el-form-item label="实际回收期(年)"><el-input-number v-model="reviewForm.actualPaybackYears" :precision="2" :step="0.5" controls-position="right" class="full" /></el-form-item>
        </div>
        <el-form-item label="投产日期">
          <el-date-picker v-model="reviewForm.operationStartDate" type="date" value-format="YYYY-MM-DD" class="full" />
        </el-form-item>
        <el-form-item label="经验教训">
          <el-input v-model="reviewForm.lessons" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="saving" :disabled="!reviewForm.scenarioId" @click="saveReview">保存复盘</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet, apiPost, apiPut } from '@/shared/api/http'
import type { ProjectLibraryItem, ProjectReview, Scenario } from '@/shared/types/domain'
import { projectStatusName, projectTypeName } from '@/shared/i18n/display'
import Pager from '@/components/Pager.vue'

const items = ref<ProjectLibraryItem[]>([])
const loading = ref(false)
const saving = ref(false)
const filters = reactive({ keyword: '', status: '', projectType: '', tag: '' })
const page = ref(1)
const size = ref(10)
const pagedItems = computed(() => items.value.slice((page.value - 1) * size.value, page.value * size.value))

const tagsDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const activeProject = ref<ProjectLibraryItem | null>(null)
const editTags = ref<string[]>([])
const review = ref<ProjectReview | null>(null)
const projectScenarios = ref<Scenario[]>([])
const reviewForm = reactive({ scenarioId: undefined as number | undefined, actualNpv: undefined as number | undefined, actualIrr: undefined as number | undefined, actualInvestment: undefined as number | undefined, actualPaybackYears: undefined as number | undefined, operationStartDate: '', lessons: '' })

async function load() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (filters.keyword) params.set('keyword', filters.keyword)
    if (filters.status) params.set('status', filters.status)
    if (filters.projectType) params.set('projectType', filters.projectType)
    if (filters.tag) params.set('tag', filters.tag)
    items.value = await apiGet<ProjectLibraryItem[]>(`/project-library?${params.toString()}`)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '检索失败')
  } finally {
    loading.value = false
  }
}

async function openTags(row: ProjectLibraryItem) {
  activeProject.value = row
  editTags.value = await apiGet<string[]>(`/projects/${row.id}/tags`)
  tagsDialogVisible.value = true
}

async function saveTags() {
  if (!activeProject.value) return
  saving.value = true
  try {
    await apiPut(`/projects/${activeProject.value.id}/tags`, editTags.value)
    ElMessage.success('标签已保存')
    tagsDialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function openReview(row: ProjectLibraryItem) {
  activeProject.value = row
  reviewDialogVisible.value = true
  try {
    review.value = row.hasReview ? await apiGet<ProjectReview>(`/projects/${row.id}/review`) : null
    projectScenarios.value = await apiGet<Scenario[]>(`/projects/${row.id}/scenarios`)
    if (review.value) {
      Object.assign(reviewForm, {
        scenarioId: review.value.scenarioId ?? undefined,
        actualNpv: review.value.actualNpv ?? undefined,
        actualIrr: review.value.actualIrr ?? undefined,
        actualInvestment: review.value.actualInvestment ?? undefined,
        actualPaybackYears: review.value.actualPaybackYears ?? undefined,
        operationStartDate: review.value.operationStartDate ?? '',
        lessons: review.value.lessons ?? ''
      })
    } else {
      Object.assign(reviewForm, { scenarioId: undefined, actualNpv: undefined, actualIrr: undefined, actualInvestment: undefined, actualPaybackYears: undefined, operationStartDate: '', lessons: '' })
    }
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载复盘失败')
  }
}

async function saveReview() {
  if (!activeProject.value || !reviewForm.scenarioId) return
  saving.value = true
  try {
    review.value = await apiPost<ProjectReview>(`/projects/${activeProject.value.id}/review`, reviewForm)
    ElMessage.success('复盘已保存')
    await load()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function fmt(v?: number | null) {
  if (v == null) return '—'
  return v.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}
function pct(v?: number | null) {
  if (v == null) return '—'
  return (v * 100).toFixed(2) + '%'
}
function dev(v?: number | null) {
  if (v == null) return '—'
  return (v > 0 ? '+' : '') + (v * 100).toFixed(1) + '%'
}
function devClass(v?: number | null) {
  if (v == null) return ''
  return v >= 0 ? 'dev-pos' : 'dev-neg'
}

onMounted(load)
</script>

<style scoped>
.library-page { display: flex; flex-direction: column; gap: 14px; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 12px; }
.f-kw { width: 200px; }
.f-sel { width: 130px; }
.f-tag { width: 130px; }
.tbl { width: 100%; }
.tag { margin-right: 4px; }
.muted { color: #9ca3af; }
.num { font-variant-numeric: tabular-nums; }
.dev-pos { color: #16a34a; }
.dev-neg { color: #dc2626; }
.full { width: 100%; }
.compare-tbl { margin-bottom: 12px; }
.lessons { margin-bottom: 10px; }
.review-meta { font-size: 12px; color: #9ca3af; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
</style>
