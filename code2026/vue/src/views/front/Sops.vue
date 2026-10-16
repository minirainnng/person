<template>
  <div class="sops-page">
    <header class="toolbar">
      <div class="brand">
        <span class="brand-icon">📘</span>
        <span>SOP 知识手册</span>
      </div>
      <div class="toolbar-actions">
        <el-button type="primary" @click="newSop">+ 新建 SOP</el-button>
        <el-button @click="loadList" :loading="loading">刷新</el-button>
      </div>
    </header>

    <div class="workspace">
      <!-- 左：SOP 列表（始终在） -->
      <aside class="sop-list-panel">
        <div class="list-header">
          <el-input v-model="keyword" placeholder="搜索标题或正文…" clearable @keyup.enter="loadList" @clear="loadList" />
        </div>
        <div v-if="loading" class="loading-state">加载中…</div>
        <el-empty v-else-if="!sops.length" description="还没有 SOP，点右上角新建一份" :image-size="60" />
        <ul v-else class="sop-list">
          <li v-for="item in sops" :key="item.id" class="sop-item" :class="{ active: selectedId === item.id }" @click="openSop(item)">
            <div class="sop-item-title">{{ item.title || '未命名 SOP' }}</div>
            <div class="sop-item-meta">
              <span>{{ formatDate(item.updatedAt) }}</span>
            </div>
            <p v-if="item.contentText" class="sop-item-preview">{{ truncate(item.contentText, 80) }}</p>
          </li>
        </ul>
      </aside>

      <!-- 中：阅读 / 编辑 / 新建 同一区域切换 -->
      <main class="sop-reader">
        <!-- 阅读态 -->
        <template v-if="mode === 'reader' && current">
          <div class="reader-header">
            <div>
              <div class="eyebrow">SOP DOCUMENT</div>
              <h2>{{ current.title }}</h2>
              <p class="meta">更新于 {{ formatDate(current.updatedAt) }}</p>
            </div>
            <div class="reader-actions">
              <el-button @click="editSop(current)">✎ 编辑</el-button>
              <el-button type="danger" link @click="removeSop(current)">删除</el-button>
            </div>
          </div>

          <div class="reader-body">
            <div class="sop-html" v-html="current.contentHtml" @click="onReaderClick"></div>
          </div>

          <div class="refs-section" v-if="refs.length">
            <h3>📎 引用的收藏（{{ refs.length }}）</h3>
            <ul class="refs-list">
              <li v-for="ref in refs" :key="ref.id" class="ref-item" :class="{ dead: !ref.memoryAlive }">
                <span class="ref-bullet">●</span>
                <a v-if="ref.memoryAlive" class="ref-link" @click.prevent="goMemory(ref.memoryId)">
                  {{ ref.memoryTitle || ref.memoryTitleSnapshot }}
                </a>
                <span v-else class="ref-link dead">
                  {{ ref.memoryTitleSnapshot }} <el-tag size="small" type="info" effect="plain">收藏已删除</el-tag>
                </span>
              </li>
            </ul>
          </div>
        </template>

        <!-- 编辑态 / 新建态 -->
        <template v-else-if="mode === 'editor' || mode === 'new'">
          <div class="reader-header">
            <div>
              <div class="eyebrow">{{ mode === 'new' ? 'NEW DOCUMENT' : 'EDITING' }}</div>
              <h2>{{ mode === 'new' ? '新建 SOP' : '编辑：' + (editing?.title || '未命名') }}</h2>
              <p class="meta">{{ mode === 'new' ? '写完保存后会自动出现在左侧列表' : '保存后立即回到阅读视图' }}</p>
            </div>
            <div class="reader-actions">
              <el-button @click="cancelEdit">取消</el-button>
              <el-button type="primary" :loading="saving" @click="triggerSave">保存</el-button>
            </div>
          </div>

          <div class="editor-stage">
            <SopEditor ref="editorRef" :sop="editing" @saved="onSaved" @cancel="cancelEdit" @dirty="onEditorDirty" />
          </div>
        </template>

        <!-- 初始空状态 -->
        <el-empty v-else description="在左侧选择一份 SOP，或点右上角「+ 新建 SOP」" :image-size="80" />
      </main>

      <!-- 右：固定栏，编辑/新建时显示收藏一览，否则显示占位 -->
      <aside class="sop-rail-panel">
        <MemoryRail v-if="mode === 'editor' || mode === 'new'" :inserter="railInserter" />
        <div v-else class="rail-placeholder">
          <div class="placeholder-icon">🧩</div>
          <div class="placeholder-title">此区域待开发</div>
          <p class="placeholder-hint">点「+ 新建 SOP」或选中 SOP 后点「✎ 编辑」，这里会自动切换为收藏一览，便于引用。</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import SopEditor from './SopEditor.vue'
import MemoryRail from './MemoryRail.vue'

const router = useRouter()
const sops = ref([])
const loading = ref(false)
const keyword = ref('')
const selectedId = ref(null)
const current = ref(null)
const refs = ref([])

// 就地编辑模式：'list' | 'reader' | 'editor' | 'new'
const mode = ref('list')
const editorRef = ref(null)
const editing = ref(null)
const saving = ref(false)
const editorDirty = ref(false) // 编辑器当前是否处于"有未保存改动"的状态

/** 给右侧 MemoryRail 使用的"插入"回调。MemoryRail 会在双击/弹窗按钮时调用 */
const railInserter = (memory) => {
  if (!editorRef.value) throw new Error('编辑器尚未挂载')
  editorRef.value.insertRef(memory)
}

function formatDate(d) { return d ? String(d).replace('T', ' ').slice(0, 16) : '' }
function truncate(str, n) { return str.length > n ? str.slice(0, n) + '…' : str }

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/sops/selectAll', { params: { keyword: keyword.value } })
    if (res.code !== '200') throw new Error(res.msg || '加载失败')
    sops.value = res.data || []
    if (selectedId.value && !sops.value.some(s => s.id === selectedId.value)) {
      selectedId.value = null
      current.value = null
      refs.value = []
    } else if (selectedId.value) {
      await openSop(sops.value.find(s => s.id === selectedId.value))
    }
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function openSop(item) {
  if (!item) return
  // 切走前如果当前正在编辑且有改动，提示
  if ((mode.value === 'editor' || mode.value === 'new') && editorDirty.value) {
    try {
      await ElMessageBox.confirm('当前编辑有未保存的改动，确定放弃吗？', '未保存', { type: 'warning' })
    } catch { return }
  }
  selectedId.value = item.id
  current.value = item
  refs.value = []
  mode.value = 'reader'
  editorDirty.value = false
  try {
    const res = await request.get(`/sops/${item.id}/refs`)
    if (res.code === '200') refs.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message || '引用加载失败')
  }
}

function newSop() {
  // 如果正在编辑且有改动，提示
  if ((mode.value === 'editor' || mode.value === 'new') && editorDirty.value) {
    ElMessageBox.confirm('当前编辑有未保存的改动，确定放弃并新建吗？', '未保存', { type: 'warning' })
      .then(() => doNew())
      .catch(() => {})
    return
  }
  doNew()
}
function doNew() {
  editing.value = { title: '', contentHtml: '', sortOrder: 0 }
  mode.value = 'new'
  editorDirty.value = false
  nextTick(() => editorRef.value?.markClean?.())
}

function editSop(item) {
  editing.value = { ...item }
  mode.value = 'editor'
  editorDirty.value = false
  nextTick(() => editorRef.value?.markClean?.())
}

async function removeSop(item) {
  try { await ElMessageBox.confirm(`确定删除 SOP「${item.title}」？关联的引用也会一并删除。`, '删除 SOP', { type: 'warning' }) } catch { return }
  try {
    const res = await request.delete(`/sops/delete/${item.id}`)
    if (res.code !== '200') throw new Error(res.msg || '删除失败')
    ElMessage.success('已删除')
    if (selectedId.value === item.id) {
      selectedId.value = null
      current.value = null
      refs.value = []
      mode.value = 'list'
    }
    await loadList()
  } catch (e) { ElMessage.error(e.message || '删除失败') }
}

async function cancelEdit() {
  if (editorDirty.value) {
    try {
      await ElMessageBox.confirm('当前编辑有未保存的改动，确定放弃吗？', '未保存', { type: 'warning' })
    } catch { return }
  }
  editorDirty.value = false
  if (mode.value === 'new') {
    editing.value = null
    if (current.value) {
      mode.value = 'reader'
    } else {
      selectedId.value = null
      mode.value = 'list'
    }
  } else {
    mode.value = 'reader'
  }
}

function onEditorDirty(v) { editorDirty.value = !!v }
function triggerSave() {
  editorRef.value?.save()
}

async function onSaved(saved) {
  saving.value = true
  try {
    await loadList()
    if (saved?.id) {
      const target = sops.value.find(s => s.id === saved.id) || saved
      await openSop(target)
    }
  } finally {
    saving.value = false
  }
}

function goMemory(memoryId) {
  router.push({ path: '/front/memories', query: { focus: memoryId } })
}

// 阅读视图点击代理：拦截 sop-memory-ref 锚点，跳转到收藏页
function onReaderClick(e) {
  const anchor = e.target?.closest?.('a.sop-memory-ref')
  if (!anchor) return
  e.preventDefault()
  const id = anchor.getAttribute('data-memory-id')
  if (id) goMemory(Number(id))
}

onMounted(loadList)
</script>

<style scoped>
.sops-page {
  --pixel-ink: #263d2e;
  --pixel-deep: #315b45;
  --pixel-green: #6f9c72;
  --pixel-light: #dce6c7;
  --pixel-paper: #f3edcf;
  --pixel-paper-deep: #e5dfbb;
  --pixel-wood: #9a6842;
  --pixel-gold: #f0ca58;
  --pixel-shadow: #294a38;
  height: calc(100vh - 60px);
  min-height: 600px;
  display: flex;
  flex-direction: column;
  color: var(--pixel-ink);
  background-color: #9bbd83;
  background-image:
      linear-gradient(45deg, rgba(255,255,255,.08) 25%, transparent 25%, transparent 75%, rgba(255,255,255,.08) 75%),
      linear-gradient(45deg, rgba(255,255,255,.08) 25%, transparent 25%, transparent 75%, rgba(255,255,255,.08) 75%);
  background-position: 0 0, 8px 8px;
  background-size: 16px 16px;
  font-family: "Courier New", "Microsoft YaHei", monospace;
  image-rendering: pixelated;
}
.sops-page * { box-sizing: border-box; }

.toolbar {
  min-height: 78px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 20px;
  background: var(--pixel-deep);
  color: var(--pixel-paper);
  border: 4px solid var(--pixel-ink);
  border-bottom: 5px solid var(--pixel-shadow);
  box-shadow: inset 0 0 0 3px #9cbd87;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 21px;
  font-weight: 700;
  margin-right: auto;
  color: #fff4c8;
  text-shadow: 2px 2px 0 #243d2d;
}
.brand-icon {
  display: grid;
  place-items: center;
  width: 38px; height: 38px;
  border-radius: 0;
  background: var(--pixel-gold);
  color: var(--pixel-ink);
  border: 3px solid var(--pixel-ink);
  box-shadow: 3px 3px 0 #203a2c;
  font-size: 18px;
}
.toolbar-actions { display: flex; gap: 8px; }

/* ===== Element Plus 输入与按钮：像素风覆盖 ===== */
.sops-page :deep(.el-input__wrapper),
.sops-page :deep(.el-select__wrapper) {
  min-height: 38px;
  border-radius: 0;
  background: #f7f0d5;
  box-shadow: inset 0 0 0 2px #a3b88a, 0 0 0 2px #263d2e !important;
}
.sops-page :deep(.el-input__inner),
.sops-page :deep(.el-select__placeholder),
.sops-page :deep(.el-input__inner::placeholder) { color: #52634a; }
.sops-page :deep(.el-textarea__inner) {
  border-radius: 0;
  background: #fff9e5;
  color: #304b36;
  box-shadow: inset 0 0 0 2px #b4c39a, 2px 2px 0 #8b9f76;
  font-family: inherit;
}
.sops-page :deep(.el-button) {
  border-radius: 0;
  font-family: inherit;
  font-weight: 700;
  border: 2px solid #294332;
  box-shadow: 2px 2px 0 #294332;
  transition: transform .08s, box-shadow .08s;
}
.sops-page :deep(.el-button:active) { transform: translate(2px, 2px); box-shadow: none; }
.sops-page :deep(.el-button--primary) {
  --el-button-bg-color: #eac85e;
  --el-button-border-color: #294332;
  --el-button-text-color: #263d2e;
  --el-button-hover-bg-color: #f6d978;
  --el-button-hover-border-color: #294332;
  --el-button-hover-text-color: #263d2e;
}
.sops-page :deep(.el-button:not(.el-button--primary):not(.is-link)) {
  background: #e8e5c8; color: #2b4936;
}
.sops-page :deep(.el-button.is-link) {
  border: 0; box-shadow: none; color: #2a68cb;
}
.sops-page :deep(.el-tag) { border-radius: 0; font-weight: 700; }

/* ===== 三栏布局 ===== */
.workspace {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr) 320px;
  gap: 8px;
  padding: 8px;
  flex: 1;
  min-height: 0;
}
.sop-list-panel, .sop-reader, .sop-rail-panel {
  min-width: 0;
  overflow-y: auto;
  border: 3px solid var(--pixel-ink);
  box-shadow: inset 0 0 0 3px #c6d5a8, 4px 4px 0 rgba(38,61,46,.48);
}

/* ===== 左侧 SOP 列表 ===== */
.sop-list-panel {
  background: var(--pixel-paper);
  border-right: 3px solid var(--pixel-ink);
  padding: 18px 13px;
}
.list-header { margin-bottom: 14px; }
.sop-list { list-style: none; margin: 0; padding: 0; }
.sop-item {
  padding: 11px 12px;
  border-radius: 0;
  cursor: pointer;
  margin-bottom: 8px;
  border: 2px solid transparent;
  background: #f8f2d9;
  box-shadow: 2px 2px 0 #a8b78e, inset 0 0 0 2px #fffbe7;
  transition: transform .12s, box-shadow .12s, background .12s;
}
.sop-item:hover {
  transform: translate(-1px, -1px);
  border-color: #4c7650;
  box-shadow: 4px 4px 0 #78966d, inset 0 0 0 2px #fffbe7;
}
.sop-item.active {
  background: #fff1a9;
  border-color: #98733c;
  box-shadow: 4px 4px 0 #98733c, inset 0 0 0 2px #fff9d6;
}
.sop-item-title { font-weight: 700; color: #263d2e; margin-bottom: 4px; }
.sop-item-meta { color: #72836a; font-size: 12px; }
.sop-item-preview { color: #52664c; font-size: 12px; line-height: 1.55; margin: 6px 0 0; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; }

/* ===== 中间阅读/编辑区 ===== */
.sop-reader {
  background: #e4e8c9;
  padding: 22px 26px;
}
.reader-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 2px dashed #a9b78c;
}
.reader-header h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #263d2e;
  text-shadow: 1px 1px #fff9dd;
}
.eyebrow { color: #64875e; font-size: 11px; font-weight: 700; letter-spacing: .12em; }
.meta { color: #607858; font-size: 13px; margin: 4px 0 0; }
.reader-actions { display: flex; gap: 8px; flex-shrink: 0; }

.reader-body { padding: 18px 0 8px; }
.sop-html { line-height: 1.85; color: #2b4936; font-size: 15px; }
.sop-html :deep(h1), .sop-html :deep(h2), .sop-html :deep(h3) { color: #1f3253; margin: 1.6em 0 .6em; }
.sop-html :deep(p) { margin: 0 0 14px; }
.sop-html :deep(a) { color: #2a68cb; }
.sop-html :deep(a.sop-memory-ref) {
  display: inline-block; padding: 1px 8px; margin: 0 2px;
  background: #fff1a9; color: #2a68cb;
  border: 2px solid #98733c;
  border-radius: 0;
  box-shadow: 2px 2px 0 #98733c;
  text-decoration: none; font-size: 13px;
}
.sop-html :deep(a.sop-memory-ref:hover) { background: #f6d978; }
.sop-html :deep(blockquote) {
  margin: 14px 0; padding: 8px 16px;
  border-left: 4px solid #b9d2f3;
  color: #5a6a83; background: #f5f9ff;
}
.sop-html :deep(code) {
  background: #eef2f7; padding: 1px 5px;
  border-radius: 0; font-size: 13px;
}
.sop-html :deep(pre) {
  background: #f4efcf; padding: 14px;
  border: 2px solid #a7b88e;
  border-radius: 0; overflow-x: auto;
}
.sop-html :deep(ul), .sop-html :deep(ol) { padding-left: 24px; }
.sop-html :deep(img) { max-width: 100%; border-radius: 0; border: 2px solid #a7b88e; }

.editor-stage { padding: 18px 0; }

.refs-section {
  margin-top: 24px; padding: 18px 22px;
  background: #f3edcf;
  border: 3px solid #91a77c;
  border-radius: 0;
  box-shadow: 3px 3px 0 #a8b78e, inset 0 0 0 2px #fffbe7;
}
.refs-section h3 { margin: 0 0 12px; font-size: 15px; color: #263d2e; }
.refs-list { list-style: none; padding: 0; margin: 0; }
.ref-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 0; border-bottom: 2px dotted #b6c19a;
}
.ref-item:last-child { border-bottom: 0; }
.ref-bullet { color: #6f9c72; font-size: 12px; }
.ref-link { color: #2a68cb; cursor: pointer; font-weight: 700; }
.ref-link:hover { text-decoration: underline; }
.ref-link.dead { color: #9aa4b3; cursor: not-allowed; }
.loading-state { padding: 40px; text-align: center; color: #58744f; }

/* ===== 右侧固定栏 ===== */
.sop-rail-panel {
  background: var(--pixel-paper);
  border-left: 3px solid var(--pixel-ink);
  padding: 18px 13px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.sop-rail-panel > .memory-rail { flex: 1; min-height: 0; }
.rail-placeholder {
  margin: 12px 4px;
  padding: 28px 18px;
  border: 3px dashed #a9b78c;
  border-radius: 0;
  background: #f3edcf;
  box-shadow: 3px 3px 0 rgba(38,61,46,.22);
  text-align: center;
  color: #52664c;
}
.placeholder-icon { font-size: 32px; line-height: 1; margin-bottom: 8px; }
.placeholder-title { font-size: 15px; font-weight: 700; color: #263d2e; margin-bottom: 6px; }
.placeholder-hint { font-size: 12px; line-height: 1.6; color: #607858; margin: 0; }

/* Empty 元素 */
.sops-page :deep(.el-empty__description p) { color: #728365; }

/* Dialog（如有） */
.sops-page :deep(.el-dialog) {
  border: 4px solid #294332;
  border-radius: 0;
  background: #f3edcf;
  box-shadow: 6px 6px 0 rgba(38,61,46,.45);
}
.sops-page :deep(.el-dialog__header) {
  margin-right: 0;
  padding: 18px 20px;
  background: #527c57;
  border-bottom: 3px solid #294332;
}
.sops-page :deep(.el-dialog__title) { color: #fff4cf; font-weight: 700; }
.sops-page :deep(.el-dialog__body) { color: #304b36; }

@media (max-width: 1100px) {
  .workspace { grid-template-columns: minmax(220px, 260px) minmax(0, 1fr) 300px; gap: 6px; padding: 6px; }
  .toolbar { gap: 10px; padding: 10px 14px; }
  .sop-reader { padding: 18px 18px; }
}
@media (max-width: 900px) {
  .workspace { grid-template-columns: 1fr; }
  .sop-list-panel { max-height: 240px; }
  .sop-rail-panel { border-left: 3px solid var(--pixel-ink); border-top: 3px solid var(--pixel-ink); }
}
</style>
