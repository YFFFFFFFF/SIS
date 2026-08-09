<template>
  <el-pagination
    class="pager"
    background
    layout="total, sizes, prev, pager, next, jumper"
    :total="total"
    :page-sizes="[10, 20, 50, 100]"
    :current-page="currentPage"
    :page-size="pageSize"
    prev-text="上一页"
    next-text="下一页"
    @current-change="$emit('update:currentPage', $event)"
    @size-change="onSizeChange"
  />
</template>

<script setup lang="ts">
import { watch } from 'vue'

const props = defineProps<{ total: number; currentPage: number; pageSize: number }>()
const emit = defineEmits<{ 'update:currentPage': [number]; 'update:pageSize': [number] }>()
function onSizeChange(size: number) {
  emit('update:pageSize', size)
  emit('update:currentPage', 1)
}

// 数据量收缩（筛选/切换页签）后，当前页可能越界导致空白表，这里全局钳到末页
watch(() => props.total, (t) => {
  const max = Math.max(1, Math.ceil(t / props.pageSize))
  if (props.currentPage > max) emit('update:currentPage', max)
})
</script>

<style scoped>
.pager { margin-top: 14px; justify-content: flex-end; }
</style>
