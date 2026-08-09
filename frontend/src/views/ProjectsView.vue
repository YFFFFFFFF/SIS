<template>
  <section class="split-panel">
    <el-card shadow="never">
      <template #header>项目列表</template>
      <el-table :data="pagedProjects" highlight-current-row @current-change="wb.selectProject">
        <el-table-column prop="code" label="项目编码" width="150" />
        <el-table-column prop="name" label="项目名称" min-width="180" />
        <el-table-column label="状态" width="110"><template #default="{ row }">{{ projectStatusName(row.status) }}</template></el-table-column>
        <el-table-column prop="department" label="所属部门" width="140" />
      </el-table>
      <Pager v-model:current-page="page" v-model:page-size="size" :total="wb.projects.length" />
    </el-card>
    <el-card shadow="never">
      <template #header>{{ wb.selectedProject ? '编辑项目' : '创建项目' }}</template>
      <el-form label-position="top" class="dense-form">
        <el-form-item label="项目编码"><el-input v-model="wb.projectForm.code" :disabled="Boolean(wb.selectedProject) || !wb.canManageProject" /></el-form-item>
        <el-form-item label="项目名称"><el-input v-model="wb.projectForm.name" :disabled="!wb.canManageProject" /></el-form-item>
        <el-form-item label="项目类型"><el-select v-model="wb.projectForm.projectType" :disabled="!wb.canManageProject"><el-option label="产业项目" value="INDUSTRIAL" /><el-option label="基础设施" value="INFRASTRUCTURE" /><el-option label="科技项目" value="TECHNOLOGY" /><el-option label="其他" value="OTHER" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="wb.projectForm.status" :disabled="!wb.canManageProject"><el-option label="草稿" value="DRAFT" /><el-option label="启用" value="ACTIVE" /><el-option label="已归档" value="ARCHIVED" /></el-select></el-form-item>
        <el-form-item label="所属部门"><el-input v-model="wb.projectForm.department" :disabled="!wb.canManageProject" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="wb.projectForm.tags" :disabled="!wb.canManageProject" /></el-form-item>
        <el-form-item label="项目说明"><el-input v-model="wb.projectForm.description" :disabled="!wb.canManageProject" :rows="3" type="textarea" /></el-form-item>
        <div class="form-actions"><el-button @click="resetProjectForm">重置</el-button><el-button :disabled="!wb.canManageProject" :loading="wb.loading.projects" type="primary" @click="saveProject">保存项目</el-button></div>
      </el-form>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiPost, apiPut } from '@/shared/api/http'
import { useWorkbenchStore } from '@/stores/workbench'
import type { Project } from '@/shared/types/domain'
import { projectStatusName } from '@/shared/i18n/display'
import Pager from '@/components/Pager.vue'

const wb = useWorkbenchStore()
const page = ref(1)
const size = ref(10)
const pagedProjects = computed(() => wb.projects.slice((page.value - 1) * size.value, page.value * size.value))

onMounted(() => { if (wb.projects.length === 0) wb.loadProjects() })

async function saveProject() {
  if (!wb.canManageProject) return wb.notifyForbidden()
  wb.loading.projects = true
  try {
    if (wb.selectedProject) await apiPut<Project>(`/projects/${wb.selectedProject.id}`, wb.projectForm)
    else await apiPost<Project>('/projects', wb.projectForm)
    ElMessage.success('项目已保存')
    resetProjectForm()
    await wb.loadProjects()
  } catch (err) { wb.notifyError(err) } finally { wb.loading.projects = false }
}

function resetProjectForm() {
  wb.selectedProject = null
  Object.assign(wb.projectForm, { code: '', name: '', projectType: 'INDUSTRIAL', status: 'DRAFT', department: '', tags: '', description: '' })
}
</script>
