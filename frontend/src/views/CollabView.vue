<template>
  <section class="collab-view">
    <!-- 顶栏：标题 + 在线状态 + 方案选择 -->
    <el-card shadow="never" class="top-card">
      <div class="top-bar">
        <div class="title-block">
          <b class="doc-title">{{ wb.selectedProject?.name ?? '协同编辑' }}<span v-if="wb.selectedScenario"> · {{ wb.selectedScenario.name }}</span></b>
          <span class="save-hint">变更自动留痕 · SSE 实时推送</span>
        </div>
        <div class="presence-block">
          <div class="avatars">
            <span v-for="p in presence" :key="p.userId" class="avatar" :title="p.userName">{{ initialOf(p.userName) }}</span>
            <span class="online-count">{{ presence.length }} 人在线</span>
            <span class="sse" :class="{ on: sseConnected }">{{ sseConnected ? '● 已连接' : '○ 未连接' }}</span>
          </div>
        </div>
        <div class="picker-block">
          <el-select :model-value="wb.selectedProject?.id ?? null" placeholder="选择项目" class="picker" @change="onProjectChange">
            <el-option v-for="p in wb.projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-select :model-value="wb.selectedScenario?.id ?? null" placeholder="选择方案" class="picker" :disabled="!wb.selectedProject" @change="onScenarioChange">
            <el-option v-for="s in wb.scenarios" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </div>
      </div>
    </el-card>

    <div v-if="!wb.selectedScenario" class="empty-wrap">
      <el-empty description="请选择项目与测算方案，进入协同编辑" />
    </div>

    <div v-else class="main-grid">
      <!-- 左：基础数据协同表（字段级锁定） -->
      <el-card shadow="never" class="table-card">
        <template #header>
          <div class="card-head">
            <span>基础数据协同表</span>
            <span class="sub">技术部维护工程量 · 财务部维护折旧/税率 · 投资部维护融资</span>
            <el-button size="small" text type="primary" :loading="loadingFields" @click="loadFields">刷新</el-button>
          </div>
        </template>
        <el-table :data="fields" size="small" height="560" v-loading="loadingFields">
          <el-table-column label="数据项" min-width="200">
            <template #default="{ row }">
              <div class="item-name">{{ row.itemName }}</div>
              <div class="field-key">{{ row.fieldKey }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="ownerDept" label="责任部门" width="90" align="center">
            <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.ownerDept }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="currentValue" label="当前值" min-width="110" align="right">
            <template #default="{ row }"><span class="cur-val">{{ row.currentValue }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="130" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.lockHolder" size="small" :type="isMyLock(row) ? 'success' : 'warning'">
                {{ isMyLock(row) ? '我编辑中' : row.lockHolder + ' 编辑中' }}
              </el-tag>
              <el-tag v-else size="small" type="info" effect="plain">可编辑</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最后编辑" width="140">
            <template #default="{ row }">
              <div v-if="row.lastEditor" class="last-edit">{{ row.lastEditor }}<br><span class="te">{{ row.lastEditAt }}</span></div>
              <span v-else class="te">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="!row.lockHolder" size="small" type="primary" plain @click="acquire(row)">锁定编辑</el-button>
              <el-button v-else-if="isMyLock(row)" size="small" type="success" plain @click="release(row)">完成并释放</el-button>
              <template v-else>
                <el-button size="small" disabled>他人占用</el-button>
                <el-button v-if="isAdmin" size="small" type="danger" text @click="forceRelease(row)">强释</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 右：变更时间线 + 评论 -->
      <div class="side-col">
        <el-card shadow="never" class="timeline-card">
          <template #header>变更记录（可追溯）</template>
          <el-timeline v-if="changes.length" class="tl">
            <el-timeline-item v-for="c in changes" :key="c.id" :timestamp="fmtTime(c.createdAt)" placement="top"
                              :type="c.changeType.includes('LOCK') ? 'warning' : 'primary'">
              <div class="tl-line">
                <span class="ver">v{{ c.versionNo }}</span>
                <b>{{ c.operatorName }}</b>
                <span class="ctype">{{ changeTypeName(c.changeType) }}</span>
              </div>
              <div v-if="c.fieldName" class="tl-field">{{ c.fieldName }}</div>
              <div v-if="c.newValue" class="tl-detail">{{ c.newValue }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无变更" :image-size="60" />
        </el-card>

        <el-card shadow="never" class="comment-card">
          <template #header>评论</template>
          <div class="comment-list">
            <div v-for="c in [...comments].reverse()" :key="c.id" class="cmt">
              <span class="cm-avatar" :data-n="initialOf(c.authorName)">{{ initialOf(c.authorName) }}</span>
              <div class="cm-body">
                <div class="cm-head">
                  <b>{{ c.authorName }}</b><span class="cm-time">{{ fmtTime(c.createdAt) }}</span>
                  <el-button v-if="canDeleteComment(c)" type="danger" text size="small" @click="deleteComment(c)">删除</el-button>
                </div>
                <div class="cm-text" v-html="highlight(c.content)" />
              </div>
            </div>
            <el-empty v-if="!comments.length" description="暂无评论" :image-size="50" />
          </div>
          <div class="cm-input">
            <el-input v-model="commentText" placeholder="发表评论，@ 提及同事…" @keyup.enter="postComment" />
            <el-button type="primary" size="small" :disabled="!commentText.trim()" :loading="posting" @click="postComment">发送</el-button>
          </div>
        </el-card>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiDelete, apiGet, apiPost } from '@/shared/api/http'
import type { CollabFieldItem, FieldLock, ScenarioChange, ScenarioComment, ScenarioPresence } from '@/shared/types/domain'
import { useAuthStore } from '@/stores/auth'
import { useWorkbenchStore } from '@/stores/workbench'

const wb = useWorkbenchStore()
const auth = useAuthStore()

const fields = ref<CollabFieldItem[]>([])
const comments = ref<ScenarioComment[]>([])
const changes = ref<ScenarioChange[]>([])
const presence = ref<ScenarioPresence[]>([])
const commentText = ref('')
const posting = ref(false)
const loadingFields = ref(false)
const sseConnected = ref(false)
let eventSource: EventSource | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null

const isAdmin = computed(() => auth.user?.roles?.some(r => r === 'ROLE_ADMIN' || r === 'ROLE_SYSTEM_ADMINISTRATOR') ?? false)
const myName = computed(() => auth.displayName)
const myId = computed(() => (auth.user?.username ? hashName(auth.user.username) : 0))

if (!wb.projects.length) wb.loadProjects()

function onProjectChange(id: number) {
  const p = wb.projects.find(x => x.id === id) ?? null
  wb.selectProject(p)
}
function onScenarioChange(id: number) {
  const s = wb.scenarios.find(x => x.id === id) ?? null
  wb.selectScenario(s)
}

function isMyLock(row: CollabFieldItem) {
  return !!row.lockHolder && row.lockHolder === myName.value
}

async function loadFields() {
  if (!wb.selectedScenario) { fields.value = []; return }
  loadingFields.value = true
  try { fields.value = await apiGet<CollabFieldItem[]>(`/scenarios/${wb.selectedScenario.id}/collab/fields`) }
  catch (err) { ElMessage.error(err instanceof Error ? err.message : '加载协同数据失败') }
  finally { loadingFields.value = false }
}

async function loadSocial() {
  if (!wb.selectedScenario) { comments.value = []; changes.value = []; presence.value = []; return }
  try {
    const [cs, ch, ps] = await Promise.all([
      apiGet<ScenarioComment[]>(`/scenarios/${wb.selectedScenario.id}/comments`),
      apiGet<ScenarioChange[]>(`/scenarios/${wb.selectedScenario.id}/changes`),
      apiGet<ScenarioPresence[]>(`/scenarios/${wb.selectedScenario.id}/presence`)
    ])
    comments.value = cs; changes.value = ch; presence.value = ps
  } catch { /* 静默 */ }
}

async function acquire(row: CollabFieldItem) {
  if (!wb.selectedScenario) return
  try {
    await apiPost<FieldLock>(`/scenarios/${wb.selectedScenario.id}/field-locks`, {
      fieldKey: row.fieldKey, holderId: myId.value, holderName: myName.value, ttlMinutes: 30
    })
    ElMessage.success(`已锁定「${row.itemName}」，可前往"测算输入"页编辑`)
  } catch (err) { ElMessage.error(err instanceof Error ? err.message : '锁定失败') }
}

async function release(row: CollabFieldItem) {
  if (!wb.selectedScenario) return
  try {
    await apiPost(`/scenarios/${wb.selectedScenario.id}/field-locks/release`, {
      fieldKey: row.fieldKey, holderId: myId.value, holderName: myName.value
    })
    ElMessage.success('已释放字段锁')
  } catch (err) { ElMessage.error(err instanceof Error ? err.message : '释放失败') }
}

async function forceRelease(row: CollabFieldItem) {
  if (!wb.selectedScenario) return
  try {
    await ElMessageBox.confirm(`强制释放「${row.itemName}」上 ${row.lockHolder} 的锁？`, '管理员强制释放', { type: 'warning' })
  } catch { return }
  try {
    await apiPost(`/scenarios/${wb.selectedScenario.id}/field-locks/force-release?fieldKey=${encodeURIComponent(row.fieldKey)}`)
    ElMessage.success('已强制释放')
  } catch (err) { ElMessage.error(err instanceof Error ? err.message : '强释失败') }
}

async function postComment() {
  if (!wb.selectedScenario || !commentText.value.trim()) return
  posting.value = true
  try {
    const c = await apiPost<ScenarioComment>(`/scenarios/${wb.selectedScenario.id}/comments`, { content: commentText.value.trim() })
    if (!comments.value.some(x => x.id === c.id)) comments.value.push(c)
    commentText.value = ''
  } catch (err) { ElMessage.error(err instanceof Error ? err.message : '评论失败') }
  finally { posting.value = false }
}

function canDeleteComment(comment: ScenarioComment) {
  return isAdmin.value || comment.authorName === auth.user?.username
}

async function deleteComment(comment: ScenarioComment) {
  if (!wb.selectedScenario) return
  try {
    await apiDelete(`/scenarios/${wb.selectedScenario.id}/comments/${comment.id}`)
    comments.value = comments.value.filter(item => item.id !== comment.id)
  } catch (err) { ElMessage.error(err instanceof Error ? err.message : '删除评论失败') }
}

async function connectSse() {
  disconnect()
  if (!wb.selectedScenario) return
  const base = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
  try {
    const credential = await apiPost<{ ticket: string; expiresAt: string }>(`/scenarios/${wb.selectedScenario.id}/collab/tickets`)
    const url = `${base}/scenarios/${wb.selectedScenario.id}/collab/stream?ticket=${encodeURIComponent(credential.ticket)}`
    eventSource = new EventSource(url)
    eventSource.onopen = () => { sseConnected.value = true }
    eventSource.onerror = () => { sseConnected.value = false }
    eventSource.addEventListener('comment', (e) => {
      const c = JSON.parse((e as MessageEvent).data) as ScenarioComment
      if (!comments.value.some(x => x.id === c.id)) comments.value.push(c)
    })
    eventSource.addEventListener('comment-deleted', (e) => {
      const payload = JSON.parse((e as MessageEvent).data) as { commentId: number }
      comments.value = comments.value.filter(item => item.id !== payload.commentId)
    })
    eventSource.addEventListener('change', (e) => {
      const c = JSON.parse((e as MessageEvent).data) as ScenarioChange
      if (!changes.value.some(x => x.id === c.id)) changes.value.unshift(c)
    })
    eventSource.addEventListener('presence', (e) => {
      presence.value = JSON.parse((e as MessageEvent).data) as ScenarioPresence[]
    })
    // 字段锁事件 → 重新拉目录（锁状态与值联动）
    eventSource.addEventListener('fieldlock', () => { loadFields() })
  } catch { sseConnected.value = false }
}

function startHeartbeat() {
  stopHeartbeat()
  if (!wb.selectedScenario) return
  const beat = () => {
    if (!wb.selectedScenario) return
    apiPost<ScenarioPresence[]>(`/scenarios/${wb.selectedScenario.id}/presence`, {
      userId: myId.value, userName: myName.value
    }).then(ps => { presence.value = ps }).catch(() => {})
  }
  beat()
  heartbeatTimer = setInterval(beat, 60000)
}

function disconnect() { eventSource?.close(); eventSource = null; sseConnected.value = false }
function stopHeartbeat() { if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null } }
function leavePresence(scenarioId: number | null | undefined) {
  if (!scenarioId || !myId.value) return
  void apiDelete(`/scenarios/${scenarioId}/presence/${myId.value}`).catch(() => {})
}

function hashName(name: string) {
  let h = 0
  for (let i = 0; i < name.length; i++) { h = (h * 31 + name.charCodeAt(i)) | 0 }
  return Math.abs(h)
}
function initialOf(name: string) { return name ? name.trim().charAt(0).toUpperCase() : '?' }
function fmtTime(t?: string | null) { return t ? t.replace('T', ' ').substring(5, 16) : '' }
function highlight(content: string) {
  return content.replace(/@([\w一-龥]+)/g, '<span class="mention">@$1</span>')
}
function changeTypeName(t: string) {
  return {
    COMMENT_ADDED: '发表评论', COMMENT_DELETED: '删除评论', FIELD_UPDATED: '更新字段', LOCK_ACQUIRED: '锁定字段',
    LOCK_RELEASED: '释放字段', CALCULATION_RUN: '运行测算', APPROVAL_ACTION: '审批操作'
  }[t] ?? t
}

watch(() => wb.selectedScenario?.id, (_, previousScenarioId) => {
  leavePresence(previousScenarioId)
  loadFields(); loadSocial(); connectSse(); startHeartbeat()
}, { immediate: true })
onBeforeUnmount(() => { leavePresence(wb.selectedScenario?.id); disconnect(); stopHeartbeat() })
</script>

<style scoped>
.collab-view { display: flex; flex-direction: column; gap: 14px; }
.top-card :deep(.el-card__body) { padding: 12px 16px; }
.top-bar { display: flex; align-items: center; gap: 20px; flex-wrap: wrap; }
.title-block { display: flex; flex-direction: column; }
.doc-title { font-size: 15px; color: #1f2937; }
.save-hint { font-size: 11px; color: #16a34a; margin-top: 2px; }
.presence-block { flex: 1; display: flex; justify-content: center; }
.avatars { display: flex; align-items: center; gap: 0; }
.avatar { width: 30px; height: 30px; border-radius: 50%; background: linear-gradient(135deg, #f59e0b, #ef4444); color: #fff; display: inline-flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; margin-left: -6px; border: 2px solid #fff; }
.avatar:first-child { margin-left: 0; }
.online-count { font-size: 12px; color: #6b7280; margin-left: 10px; }
.sse { font-size: 11px; color: #9ca3af; margin-left: 10px; }
.sse.on { color: #16a34a; }
.picker-block { display: flex; gap: 8px; }
.picker { width: 180px; }
.empty-wrap { padding: 40px 0; }
.main-grid { display: grid; grid-template-columns: 1.6fr 1fr; gap: 14px; align-items: start; }
.card-head { display: flex; align-items: center; gap: 10px; }
.card-head .sub { font-size: 11px; color: #9ca3af; font-weight: 400; }
.item-name { font-size: 13px; color: #1f2937; }
.field-key { font-size: 11px; color: #9ca3af; font-family: monospace; }
.cur-val { font-weight: 600; color: #111827; }
.last-edit { font-size: 12px; color: #374151; line-height: 1.3; }
.te { font-size: 11px; color: #9ca3af; }
.side-col { display: flex; flex-direction: column; gap: 14px; }
.tl { max-height: 300px; overflow-y: auto; padding-left: 4px; }
.tl-line { display: flex; align-items: center; gap: 6px; font-size: 13px; flex-wrap: wrap; }
.ver { font-weight: 700; color: #7c3aed; }
.ctype { color: #6b7280; font-size: 12px; }
.tl-field { font-size: 11px; color: #005eba; font-family: monospace; margin-top: 2px; }
.tl-detail { font-size: 12px; color: #6b7280; margin-top: 2px; word-break: break-word; }
.comment-list { max-height: 220px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; margin-bottom: 10px; }
.cmt { display: flex; gap: 8px; }
.cm-avatar { width: 28px; height: 28px; border-radius: 50%; background: linear-gradient(135deg, #22c55e, #0ea5e9); color: #fff; display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; flex-shrink: 0; }
.cm-body { flex: 1; }
.cm-head { display: flex; justify-content: space-between; font-size: 12px; }
.cm-time { color: #9ca3af; }
.cm-text { font-size: 13px; color: #374151; line-height: 1.5; word-break: break-word; }
.cm-text :deep(.mention) { color: #005eba; font-weight: 600; }
.cm-input { display: flex; gap: 8px; }
</style>
