<template>
  <div class="memory-rail">
    <div class="rail-header">
      <span class="rail-title">📚 我的收藏</span>
      <el-button link type="primary" size="small" @click="loadAllMemories" :loading="loading">刷新</el-button>
    </div>
    <el-input
      v-model="keyword"
      class="rail-search"
      size="small"
      clearable
      placeholder="搜索标题或备注…"
    />
    <div v-if="loading && !allMemories.length" class="loading-state">加载收藏中…</div>
    <div v-else-if="error" class="rail-error">
      {{ error }}
      <el-button link type="primary" @click="loadAllMemories">重试</el-button>
    </div>
    <ul v-else class="rail-list">
      <li
        v-for="m in filteredMemories"
        :key="m.id"
        class="rail-item"
        :class="{ active: previewId === m.id }"
        @click="previewMemory(m)"
        @dblclick="onInsert(m)"
        title="双击插入到光标处；单击预览"
      >
        <div class="rail-row">
          <el-tag size="small" effect="plain" :type="typeStyle(m.sourceType)">{{ typeLabel(m.sourceType) }}</el-tag>
          <span class="rail-name">{{ m.title || m.sourceTitle || '未命名收藏' }}</span>
        </div>
        <p v-if="m.note" class="rail-note">{{ m.note }}</p>
      </li>
      <el-empty v-if="!filteredMemories.length" description="没有匹配的收藏" :image-size="60" />
    </ul>

    <!-- 单击预览弹窗 -->
    <el-dialog v-model="previewVisible" width="640px" append-to-body :title="previewTitle" class="memory-preview-dialog">
      <template v-if="previewing">
        <div class="preview-meta">
          <el-tag size="small" effect="plain" :type="typeStyle(previewing.sourceType)">{{ typeLabel(previewing.sourceType) }}</el-tag>
          <span class="preview-date">{{ formatDate(previewing.createdAt) }}</span>
        </div>
        <h3 class="preview-title">{{ previewing.title || previewing.sourceTitle || '未命名收藏' }}</h3>
        <a
          v-if="safeUrl(previewing.originalUrl)"
          :href="safeUrl(previewing.originalUrl)"
          target="_blank"
          rel="noopener noreferrer"
          class="preview-link"
        >↗ 打开原链接</a>
        <div v-if="previewing.sourceTitle && previewing.sourceTitle !== previewing.title" class="preview-source">
          平台原始标题：{{ previewing.sourceTitle }}
        </div>
        <div v-if="previewing.originalText" class="preview-block">
          <div class="preview-label">原始内容 / 摘录</div>
          <pre class="preview-text">{{ previewing.originalText }}</pre>
        </div>
        <div v-if="previewing.note" class="preview-block">
          <div class="preview-label">📝 我的备注</div>
          <pre class="preview-text">{{ previewing.note }}</pre>
        </div>
        <div v-if="!previewing.originalText && !previewing.note && !safeUrl(previewing.originalUrl)" class="preview-empty">
          这条收藏还没有内容。
        </div>
      </template>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="primary" @click="onInsert(previewing); previewVisible = false">📎 插入到光标处</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const props = defineProps({
  /** 由父组件注入：执行"插入到光标处" */
  inserter: { type: Function, required: true }
})
const emit = defineEmits(['inserted'])

const allMemories = ref([])
const loading = ref(false)
const error = ref('')
const keyword = ref('')

const filteredMemories = computed(() => {
  const word = keyword.value.trim().toLocaleLowerCase()
  const list = word
    ? allMemories.value.filter(m =>
        [m.title, m.sourceTitle, m.note].some(v => String(v || '').toLocaleLowerCase().includes(word))
      )
    : allMemories.value
  return list.slice(0, 300)
})

async function loadAllMemories() {
  loading.value = true
  error.value = ''
  try {
    const res = await request.get('/memories/selectAll')
    if (res.code !== '200') throw new Error(res.msg || '加载失败')
    allMemories.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    error.value = e.message || '加载收藏失败'
  } finally {
    loading.value = false
  }
}

function typeLabel(type) { return ({ video: '视频', web: '网页', image: '图片', text: '文字' })[type] || '收藏' }
function typeStyle(type) { return ({ video: 'danger', web: 'success', image: 'warning', text: 'info' })[type] || 'info' }
function formatDate(d) { return d ? String(d).replace('T', ' ').slice(0, 16) : '' }
function safeUrl(url) { try { const parsed = new URL(url); return ['https:', 'http:'].includes(parsed.protocol) ? parsed.href : '' } catch { return '' } }

// 预览
const previewVisible = ref(false)
const previewing = ref(null)
const previewId = ref(null)
const previewTitle = computed(() => previewing.value
  ? (previewing.value.title || previewing.value.sourceTitle || '收藏详情')
  : '收藏详情')

function previewMemory(m) {
  previewId.value = m.id
  previewing.value = m
  previewVisible.value = true
}

// 真正执行"插入到光标处"
function onInsert(memory) {
  if (!memory) return
  try {
    props.inserter(memory)
    emit('inserted', memory)
  } catch (e) {
    ElMessage.error('插入失败：' + (e.message || '未知错误'))
  }
}

onMounted(loadAllMemories)
</script>

<style scoped>
.memory-rail {
  background: #fafbfd;
  border: 1px solid #e8edf4;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
}
.rail-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.rail-title { font-weight: 700; color: #2c3a52; font-size: 14px; }
.rail-search { margin-bottom: 8px; }
.rail-list { list-style: none; margin: 0; padding: 0; overflow-y: auto; flex: 1; min-height: 0; }
.rail-item {
  padding: 8px 10px;
  border: 1px solid #e9edf4;
  border-radius: 8px;
  margin-bottom: 6px;
  background: #fff;
  cursor: pointer;
  user-select: none;
  transition: background .12s, border-color .12s, box-shadow .12s;
}
.rail-item:hover { background: #f5f9ff; border-color: #b9d2f3; }
.rail-item.active {
  background: #eaf2ff;
  border-color: #8db5f4;
  box-shadow: 0 0 0 1px #b9d2f3 inset;
}
.rail-row { display: flex; align-items: center; gap: 8px; }
.rail-name {
  color: #2c3a52;
  font-weight: 500;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}
.rail-note {
  color: #6c7a90;
  font-size: 12px;
  margin: 4px 0 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.rail-error { color: #c45656; font-size: 12px; padding: 8px 4px; }
.loading-state { padding: 18px; text-align: center; color: #8b98ab; font-size: 13px; }

/* 预览弹窗 */
.preview-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.preview-date { color: #99a5b5; font-size: 12px; }
.preview-title { margin: 0 0 6px; font-size: 18px; color: #1f3253; }
.preview-link { font-size: 12px; color: #2a68cb; word-break: break-all; }
.preview-source { color: #6c7a90; font-size: 12px; margin: 4px 0 12px; }
.preview-block { margin-top: 12px; }
.preview-label { font-weight: 600; color: #526077; font-size: 12px; margin-bottom: 4px; }
.preview-text {
  background: #f5f7fb;
  border: 1px solid #e9edf4;
  border-radius: 6px;
  padding: 10px 12px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 13px;
  color: #2c3a52;
  max-height: 240px;
  overflow-y: auto;
}
.preview-empty { color: #9aa4b3; font-size: 13px; padding: 16px 0; text-align: center; }
</style>
