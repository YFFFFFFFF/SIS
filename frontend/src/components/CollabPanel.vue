<template>
  <div class="collab-panel">
    <div class="presence-bar">
      <span class="dot" />
      <span class="online-text">在线 {{ presence.length }} 人：</span>
      <el-tag v-for="p in presence" :key="p.userId" size="small" class="who">{{ p.userName }}</el-tag>
      <span v-if="sseConnected" class="sse-state on">SSE 已连接</span>
      <span v-else class="sse-state">SSE 未连接</span>
    </div>

    <div class="collab-grid">
      <div class="col">
        <div class="section-title">评论（支持 @提及）</div>
        <div class="comment-input">
          <el-input v-model="commentText" type="textarea" :rows="2" maxlength="2000" placeholder="输入评论，@用户名 可提及同事" />
          <el-button type="primary" size="small" :disabled="!commentText.trim() || !scenarioId" :loading="posting" @click="postComment">发表</el-button>
        </div>
        <div v-if="comments.length" class="comment-list">
          <div v-for="c in [...comments].reverse()" :key="c.id" class="comment-item">
            <div class="comment-head">
              <span class="author">{{ c.authorName }}</span>
              <span class="time">{{ formatTime(c.createdAt) }}</span>
              <el-button v-if="canDeleteComment(c)" type="danger" text size="small" @click="deleteComment(c)">删除</el-button>
            </div>
            <div class="comment-body" v-html="highlightMentions(c.content)" />
            <div v-if="c.mentions" class="mentions">提及：{{ c.mentions }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无评论" :image-size="60" />
      </div>

      <div class="col">
        <div class="section-title">变更时间线</div>
        <el-timeline v-if="changes.length" class="change-list">
          <el-timeline-item v-for="c in changes" :key="c.id" :timestamp="formatTime(c.createdAt)" placement="top">
            <span class="ver">v{{ c.versionNo }}</span>
            <span class="ctype">{{ changeTypeName(c.changeType) }}</span>
            <span class="operator">{{ c.operatorName }}</span>
            <div v-if="c.newValue" class="change-detail">{{ c.newValue }}</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无变更记录" :image-size="60" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { apiDelete, apiGet, apiPost } from '@/shared/api/http'
import type { ScenarioChange, ScenarioComment, ScenarioPresence } from '@/shared/types/domain'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{ scenarioId: number | null }>()
const auth = useAuthStore()

const comments = ref<ScenarioComment[]>([])
const changes = ref<ScenarioChange[]>([])
const presence = ref<ScenarioPresence[]>([])
const commentText = ref('')
const posting = ref(false)
const sseConnected = ref(false)
let eventSource: EventSource | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
const myId = computed(() => auth.user?.username ? hashName(auth.user.username) : 0)
const myUsername = computed(() => auth.user?.username ?? '')
const isAdmin = computed(() => auth.user?.roles?.some(role => role === 'ROLE_ADMIN' || role === 'ROLE_SYSTEM_ADMINISTRATOR') ?? false)

async function loadAll() {
  if (!props.scenarioId) {
    comments.value = []; changes.value = []; presence.value = []
    return
  }
  try {
    const [cs, ch, ps] = await Promise.all([
      apiGet<ScenarioComment[]>(`/scenarios/${props.scenarioId}/comments`),
      apiGet<ScenarioChange[]>(`/scenarios/${props.scenarioId}/changes`),
      apiGet<ScenarioPresence[]>(`/scenarios/${props.scenarioId}/presence`)
    ])
    comments.value = cs; changes.value = ch; presence.value = ps
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载协同数据失败')
  }
}

async function postComment() {
  if (!props.scenarioId || !commentText.value.trim()) return
  posting.value = true
  try {
    const c = await apiPost<ScenarioComment>(`/scenarios/${props.scenarioId}/comments`, { content: commentText.value.trim() })
    comments.value.push(c)
    commentText.value = ''
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '评论失败')
  } finally {
    posting.value = false
  }
}

function canDeleteComment(comment: ScenarioComment) {
  return isAdmin.value || comment.authorName === myUsername.value
}

async function deleteComment(comment: ScenarioComment) {
  if (!props.scenarioId) return
  try {
    await apiDelete(`/scenarios/${props.scenarioId}/comments/${comment.id}`)
    comments.value = comments.value.filter(item => item.id !== comment.id)
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除评论失败')
  }
}

async function connectSse() {
  disconnect()
  if (!props.scenarioId) return
  const base = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
  try {
    const credential = await apiPost<{ ticket: string; expiresAt: string }>(`/scenarios/${props.scenarioId}/collab/tickets`)
    const url = `${base}/scenarios/${props.scenarioId}/collab/stream?ticket=${encodeURIComponent(credential.ticket)}`
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
  } catch {
    sseConnected.value = false
  }
}

function startHeartbeat() {
  stopHeartbeat()
  if (!props.scenarioId) return
  const beat = () => {
    if (!props.scenarioId) return
    apiPost<ScenarioPresence[]>(`/scenarios/${props.scenarioId}/presence`, {
      userId: myId.value,
      userName: auth.user?.displayName ?? auth.user?.username ?? 'anonymous'
    }).then(ps => { presence.value = ps }).catch(() => { /* 心跳失败静默 */ })
  }
  beat()
  heartbeatTimer = setInterval(beat, 60000)
}

function disconnect() {
  eventSource?.close()
  eventSource = null
  sseConnected.value = false
}

function stopHeartbeat() {
  if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null }
}

function leavePresence(scenarioId: number | null | undefined) {
  if (!scenarioId || !myId.value) return
  void apiDelete(`/scenarios/${scenarioId}/presence/${myId.value}`).catch(() => {})
}

function hashName(name: string) {
  let h = 0
  for (let i = 0; i < name.length; i++) { h = (h * 31 + name.charCodeAt(i)) | 0 }
  return Math.abs(h)
}

function highlightMentions(content: string) {
  return content.replace(/@([\w\u4e00-\u9fa5]+)/g, '<span class="mention">@$1</span>')
}

function formatTime(t: string) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}

function changeTypeName(t: string) {
  return {
    COMMENT_ADDED: '发表评论', COMMENT_DELETED: '删除评论', FIELD_UPDATED: '更新字段', LOCK_ACQUIRED: '获取编辑锁',
    LOCK_RELEASED: '释放编辑锁', CALCULATION_RUN: '运行测算', APPROVAL_ACTION: '审批操作'
  }[t] ?? t
}

watch(() => props.scenarioId, (_, previousScenarioId) => {
  leavePresence(previousScenarioId)
  loadAll(); connectSse(); startHeartbeat()
}, { immediate: true })

onBeforeUnmount(() => { leavePresence(props.scenarioId); disconnect(); stopHeartbeat() })
</script>

<style scoped>
.collab-panel { display: flex; flex-direction: column; gap: 12px; }
.presence-bar { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #16a34a; }
.online-text { color: #374151; }
.who { margin-right: 2px; }
.sse-state { margin-left: auto; font-size: 11px; color: #9ca3af; }
.sse-state.on { color: #16a34a; }
.collab-grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 16px; }
.section-title { font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 8px; }
.comment-input { display: flex; gap: 8px; align-items: flex-end; margin-bottom: 10px; }
.comment-list { display: flex; flex-direction: column; gap: 8px; max-height: 360px; overflow-y: auto; }
.comment-item { border: 1px solid #e5e7eb; border-radius: 8px; padding: 8px 10px; background: #fafafa; }
.comment-head { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 4px; }
.author { font-weight: 600; color: #1f2937; }
.time { color: #9ca3af; }
.comment-body { font-size: 13px; line-height: 1.6; word-break: break-word; }
.comment-body :deep(.mention) { color: #005eba; font-weight: 600; }
.mentions { font-size: 11px; color: #005eba; margin-top: 4px; }
.change-list { max-height: 420px; overflow-y: auto; padding-left: 4px; }
.ver { font-weight: 700; color: #7c3aed; margin-right: 6px; }
.ctype { margin-right: 6px; }
.operator { color: #6b7280; font-size: 12px; }
.change-detail { font-size: 12px; color: #6b7280; margin-top: 2px; word-break: break-word; }
</style>
