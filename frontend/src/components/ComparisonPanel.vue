<template>
  <div class="comparison-panel">
    <div class="toolbar">
      <span class="hint">对当前项目下全部方案的最新成功测算结果做横向对比</span>
      <el-button type="primary" :loading="loading" :disabled="!projectId" @click="load">生成对比矩阵</el-button>
    </div>

    <template v-if="matrix">
      <el-alert v-if="rankingText" :closable="false" type="success" class="ranking-bar"
                :title="`排序建议（按 NPV）：${rankingText}`" />
      <el-alert v-else :closable="false" type="info" class="ranking-bar"
                title="暂无可排序方案：请先对至少一个方案执行测算" />

      <table class="cmp-tbl">
        <thead>
          <tr>
            <th class="metric-col">指标</th>
            <th v-for="col in matrix.scenarios" :key="col.scenarioId">
              {{ col.scenarioName }}
              <span v-if="!col.calculated" class="uncalc">（未测算）</span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in matrix.metrics" :key="row.metricCode">
            <td class="metric-col">
              {{ row.metricName }}
              <span v-if="row.unit !== '-'" class="unit">（{{ row.unit }}）</span>
            </td>
            <td v-for="(col, idx) in matrix.scenarios" :key="col.scenarioId"
                :class="{ best: isBest(row, col.scenarioId) }">
              <template v-if="row.values[idx] != null">
                {{ formatValue(row.values[idx]!) }}
                <span v-if="isBest(row, col.scenarioId)" class="star">★</span>
              </template>
              <span v-else class="empty">—</span>
            </td>
          </tr>
        </tbody>
      </table>
    </template>
    <el-empty v-else description="点击“生成对比矩阵”，输出各方案核心指标横向对比与排序建议" :image-size="90" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet } from '@/shared/api/http'
import type { ComparisonMatrix, ComparisonMetricRow } from '@/shared/types/domain'

const props = defineProps<{ projectId: number | null }>()

const loading = ref(false)
const matrix = ref<ComparisonMatrix | null>(null)

const rankingText = computed(() => {
  if (!matrix.value || matrix.value.ranking.length === 0) return ''
  return matrix.value.ranking.map((r) => r.scenarioName).join(' › ')
})

function isBest(row: ComparisonMetricRow, scenarioId: number) {
  return row.bestScenarioIds.includes(scenarioId)
}

function formatValue(v: number) {
  return Number.isInteger(v) ? v.toLocaleString() : v.toFixed(4).replace(/0+$/, '').replace(/\.$/, '')
}

async function load() {
  if (!props.projectId) return
  loading.value = true
  try {
    matrix.value = await apiGet<ComparisonMatrix>(`/projects/${props.projectId}/comparison`)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '对比矩阵加载失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.hint {
  color: #909399;
  font-size: 13px;
}

.ranking-bar {
  margin-bottom: 12px;
}

.cmp-tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.cmp-tbl th,
.cmp-tbl td {
  border: 1px solid #ebeef5;
  padding: 8px 12px;
  text-align: right;
}

.cmp-tbl th {
  background: #f5f7fa;
  text-align: center;
}

.cmp-tbl .metric-col {
  text-align: left;
  font-weight: 600;
  min-width: 180px;
}

.cmp-tbl td.best {
  color: #67c23a;
  font-weight: 700;
  background: #f0f9eb;
}

.star {
  margin-left: 2px;
}

.unit {
  color: #909399;
  font-weight: 400;
  font-size: 12px;
}

.uncalc {
  color: #e6a23c;
  font-size: 12px;
  font-weight: 400;
}

.empty {
  color: #c0c4cc;
}
</style>
