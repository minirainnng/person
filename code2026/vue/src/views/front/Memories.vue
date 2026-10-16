<template>
  <div class="memories-page">
    <header class="toolbar">
      <div class="brand"><span class="brand-icon">✦</span><span>我的记忆库</span></div>
    </header>

    <div class="workspace">
      <aside class="sidebar">
        <section class="sidebar-section folder-section">
          <div class="sidebar-heading">文件夹 <el-button link type="primary" @click="openFolderCreate(null)">+ 新建</el-button></div>
          <button class="nav-item" :class="{ active: selectedFolder === null }" @click="selectFolder(null)">📚 全部收藏 <span>{{ memories.length }}</span></button>
          <div v-if="folderLoading" class="sidebar-hint">正在加载文件夹…</div>
          <div v-else-if="folderError" class="sidebar-hint">{{ folderError }} <el-button link type="primary" @click="loadFolders">重试</el-button></div>
          <el-tree v-else-if="folders.length" :data="folderTree" node-key="id" :props="{ label: 'name', children: 'children' }" default-expand-all :expand-on-click-node="false" class="folder-tree">
            <template #default="{ data: folder }">
              <div class="tree-node-row" :class="{ active: selectedFolder === folder.id }">
                <span class="tree-node" @click.stop="selectFolder(folder.id)">📁 {{ folder.name }}</span>
                <el-button class="tree-add-button" link type="primary" title="新建子文件夹" @click.stop="openFolderCreate(folder.id)">＋</el-button>
              </div>
            </template>
          </el-tree>
          <el-empty v-else description="还没有文件夹" :image-size="54" />
        </section>
        <section class="sidebar-section tag-section">
          <div class="sidebar-heading tags-heading">标签 <el-button link type="primary" @click="newTag">+ 新建</el-button></div>
          <div class="tag-list-scroll">
            <button v-for="tag in tags" :key="tag.id" class="nav-item tag-item" :class="{ active: selectedTags.includes(tag.id) }" @click="toggleTag(tag.id)"># {{ tag.name }}</button>
          </div>
          <p class="local-hint">标签需手动添加和确认；AI 提炼接口已预留，尚未接入。</p>
        </section>
        <section class="sidebar-section sop-section">
          <div class="sidebar-heading sop-heading">
            <span>SOP 手册 <el-tag v-if="sopList.length" size="small" effect="plain">{{ sopList.length }}</el-tag></span>
            <el-button link type="primary" @click="openSops">查看全部</el-button>
          </div>
          <div class="sop-list-scroll">
            <button v-if="sopLoading" class="nav-item disabled" disabled>加载中…</button>
            <button v-else-if="!sopList.length" class="nav-item disabled" disabled>还没有 SOP</button>
            <button v-for="sop in sopList" :key="sop.id" class="nav-item sop-item" :title="sop.title" @click="openSops(sop)">📘 {{ sop.title || '未命名 SOP' }}</button>
          </div>
          <p class="local-hint">SOP 写作工具：可在富文本里引用收藏，点击引用跳回此处。</p>
        </section>
      </aside>

      <main class="list-panel">
        <div class="list-header"><div><h2>{{ currentFolderName }}</h2><p>共 {{ filteredMemories.length }} 条收藏</p></div><el-button :loading="loading" @click="refresh">刷新</el-button></div>
        <div class="list-actions">
          <el-input v-model="query" class="global-search" clearable placeholder="搜索标题或备注…" :prefix-icon="Search" />
          <el-select v-model="selectedTags" multiple clearable collapse-tags placeholder="筛选标签（交集）" class="tag-filter">
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
          <div class="toolbar-actions">
            <el-button type="primary" @click="startCreate">+ 新增收藏</el-button>
            <el-button @click="folderDialog = true">📁 管理文件夹</el-button>
          </div>
        </div>
        <div class="bulk-toolbar" @click.stop><el-radio-group v-model="folderStatus" class="folder-status" @click.stop><el-radio value="all">全部</el-radio><el-radio value="assigned">已放入文件夹</el-radio><el-radio value="unassigned">未放入文件夹</el-radio></el-radio-group><el-checkbox v-model="selectPageAll" label="勾选当前页" @click.stop /><span>已选 {{ selectedMemoryIds.length }} 条</span><el-select v-model="bulkFolders" multiple collapse-tags placeholder="选择目标文件夹" class="bulk-select" @click.stop><el-option v-for="folder in flatFolders" :key="folder.id" :label="folder.label" :value="String(folder.id)" /></el-select><el-button type="primary" :loading="bulkSaving" @click.stop="addToFolders">加入文件夹</el-button></div>
        <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" class="load-alert" />
        <div v-if="loading" class="loading-state">正在加载收藏…</div>
        <template v-else-if="filteredMemories.length">
          <div v-for="item in pagedMemories" :key="item.id" class="memory-card" :class="{ selected: selectedId === item.id && !creating, 'is-focus': Number(route.query.focus) === Number(item.id) }" @click="selectMemory(item)">
            <el-checkbox :model-value="selectedMemoryIds.includes(String(item.id))" @mousedown.stop @click.stop @change="checked => toggleMemory(item.id, checked)" />
            <div class="card-head"><el-tag size="small" effect="plain" :type="typeStyle(item.sourceType)">{{ typeLabel(item.sourceType) }}</el-tag><span class="card-date">{{ formatDate(item.createdAt) }}</span></div>
            <h3>{{ item.title || item.sourceTitle || '未命名收藏' }}</h3>
            <p v-if="item.note" class="card-note">{{ item.note }}</p><p v-else-if="item.originalText" class="card-note muted">{{ item.originalText }}</p>
            <div class="card-foot"><span v-for="tag in tagsFor(item.id)" :key="tag.id" class="tag-chip">#{{ tag.name }}</span><span v-for="folderId in (metaFor(item.id).folders || []).slice(0, 3)" :key="folderId" class="tag-chip">📁 {{ folderName(folderId) }}</span></div>
          </div>
          <el-pagination v-if="filteredMemories.length > pageSize" v-model:current-page="page" :page-size="pageSize" :total="filteredMemories.length" layout="prev, pager, next" class="pagination" />
        </template>
        <el-empty v-else description="没有匹配的收藏，试试新建一条或清除筛选" />
      </main>

      <section class="detail-panel">
        <template v-if="creating || selectedId !== null">
          <div class="detail-header"><div><div class="eyebrow">{{ creating ? 'CREATE MEMORY' : 'MEMORY DETAIL' }}</div><h2>{{ creating ? '新增收藏' : '收藏详情 / 编辑' }}</h2></div><el-button v-if="!creating" link type="danger" @click="removeMemory">删除收藏</el-button></div>
          <el-form label-position="top" class="detail-form" @submit.prevent="save">
            <el-form-item label="标题"><el-input v-model="form.title" maxlength="255" show-word-limit placeholder="给它取个好找的名字" /></el-form-item>
            <el-form-item label="平台原始标题"><el-input v-model="form.sourceTitle" maxlength="255" placeholder="原始网页 / 视频的标题（可选）" /></el-form-item>
            <el-form-item label="收藏类型"><el-select v-model="form.sourceType" placeholder="选择类型"><el-option label="视频" value="video" /><el-option label="网页" value="web" /><el-option label="图片" value="image" /><el-option label="文字" value="text" /></el-select></el-form-item>
            <el-form-item label="原始链接"><el-input v-model="form.originalUrl" placeholder="https://…" /><a v-if="safeUrl(form.originalUrl)" :href="safeUrl(form.originalUrl)" target="_blank" rel="noopener noreferrer" class="open-link">↗ 打开原链接</a></el-form-item>
            <el-form-item label="图片路径"><el-input v-model="form.imagePath" placeholder="图片存储路径（可选，暂不支持上传）" /></el-form-item>
            <el-form-item label="原始内容 / 摘录"><el-input v-model="form.originalText" type="textarea" :rows="4" placeholder="网页正文、图片内容或想保留的原文" /></el-form-item>
            <el-form-item label="📝 我的备注"><el-input v-model="form.note" type="textarea" :rows="7" placeholder="为什么收藏？哪些地方触动了你？以后要怎么用？可以写很长…" /></el-form-item>
            <div class="section-label">🏷️ 标签（手动确认） <el-button link type="primary" @click="newTag">+ 新建标签</el-button><el-button link type="success" :loading="aiAnalyzing" @click="analyzeCurrentMemory">重新分析</el-button></div>
            <div v-if="suggestedTags.length" class="ai-suggestions"><span class="suggestion-label">AI 建议：</span><el-checkbox-group v-model="draftTags"><el-checkbox v-for="tag in suggestedTags" :key="tag.key" :label="tag.key">#{{ tag.name }}</el-checkbox></el-checkbox-group></div>
            <el-select v-model="draftTags" multiple clearable filterable placeholder="勾选这条收藏的标签" class="full-width"><el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" /></el-select>
            <p class="feature-hint">保存后会自动调用 DeepSeek 分析标题、备注和原始内容；AI 只生成候选标签，必须由你勾选确认。</p>
            <div class="section-label folder-label">📁 所属文件夹（可多选）</div>
            <el-select v-model="draftFolders" multiple clearable placeholder="选择多个文件夹" class="full-width"><el-option v-for="folder in flatFolders" :key="folder.id" :label="folder.label" :value="String(folder.id)" /></el-select>
            <div class="save-row"><el-button type="primary" :loading="saving" @click="save">{{ creating ? '创建收藏' : '保存修改' }}</el-button><el-button v-if="creating" @click="cancelCreate">取消</el-button></div>
          </el-form>
        </template>
        <el-empty v-else description="在中间选择一条收藏，或新增一条" />
      </section>
    </div>

    <el-dialog v-model="folderDialog" title="管理文件夹" width="460px">
      <p class="feature-hint">文件夹名称、层级和收藏归属均保存在数据库。</p>
      <div v-for="folder in flatFolders" :key="folder.id" class="folder-row"><span>{{ folder.label }}</span><span><el-button link type="primary" @click="openFolderCreate(folder.id)">添加子文件夹</el-button><el-button link @click="renameFolder(folder)">重命名</el-button><el-button link type="danger" @click="deleteFolder(folder)">删除</el-button></span></div>
      <template #footer><el-button @click="folderDialog = false">完成</el-button><el-button type="primary" @click="openFolderCreate(null)">+ 根文件夹</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const local = reactive({ assignments: {} })
const memories = ref([])
const folders = ref([])
const tags = ref([])
const tagAssignments = ref({})
const selectedTags = ref([])
const selectedMemoryIds = ref([])
const bulkFolders = ref([])
const bulkSaving = ref(false)
const folderLoading = ref(false)
const folderError = ref('')
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const query = ref('')
const selectedFolder = ref(null)
const folderStatus = ref('all')
const selectedId = ref(null)
const creating = ref(false)
const folderDialog = ref(false)
const page = ref(1)
const pageSize = 10
const form = reactive({ title: '', sourceTitle: '', sourceType: 'text', originalUrl: '', originalText: '', imagePath: '', note: '' })
const draftFolders = ref([])
const draftTags = ref([])
const suggestedTags = ref([])
const aiAnalyzing = ref(false)
const tagMatchIds = ref(null)
const sopList = ref([])
const sopLoading = ref(false)
const tagsFor = id => tagAssignments.value[String(id)] || []
function toggleTag(id) { selectedTags.value = selectedTags.value.includes(id) ? selectedTags.value.filter(value => value !== id) : [...selectedTags.value, id] }
function toggleMemory(id, checked) { selectedMemoryIds.value = checked ? [...new Set([...selectedMemoryIds.value, String(id)])] : selectedMemoryIds.value.filter(value => value !== String(id)) }
const selectPageAll = computed({ get: () => pagedMemories.value.length > 0 && pagedMemories.value.every(item => selectedMemoryIds.value.includes(String(item.id))), set: checked => { for (const item of pagedMemories.value) toggleMemory(item.id, checked) } })
const metaFor = id => local.assignments[String(id)] || { folders: [] }
const folderTree = computed(() => {
  const nodes = folders.value.map(folder => ({
    ...folder,
    id: String(folder.id),
    parentId: folder.parentId == null || folder.parentId === '' || String(folder.parentId) === '0' ? null : String(folder.parentId),
    children: []
  }))
  const byId = new Map(nodes.map(folder => [folder.id, folder]))
  const roots = []
  for (const node of nodes) {
    if (node.parentId && byId.has(node.parentId)) byId.get(node.parentId).children.push(node)
    else roots.push(node)
  }
  const sortNodes = list => {
    list.sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0) || a.name.localeCompare(b.name, 'zh-CN'))
    list.forEach(node => sortNodes(node.children))
  }
  sortNodes(roots)
  return roots
})
const flatFolders = computed(() => {
  const result = []
  const visit = (nodes, depth = 0) => nodes.forEach(node => { result.push({ ...node, label: `${'　'.repeat(depth)}${node.name}` }); visit(node.children, depth + 1) })
  visit(folderTree.value)
  return result
})
const currentFolderName = computed(() => selectedFolder.value === null ? '全部收藏' : folders.value.find(folder => String(folder.id) === String(selectedFolder.value))?.name || '全部收藏')
function descendantIds(id) {
  const rootId = String(id)
  const ids = new Set([rootId])
  let changed = true
  while (changed) {
    changed = false
    for (const folder of folders.value) {
      const folderId = String(folder.id)
      const parentId = folder.parentId == null || folder.parentId === '' || String(folder.parentId) === '0' ? null : String(folder.parentId)
      if (!ids.has(folderId) && parentId && ids.has(parentId)) { ids.add(folderId); changed = true }
    }
  }
  return ids
}
const filteredMemories = computed(() => {
  const word = query.value.trim().toLocaleLowerCase()
  return memories.value.filter(item => {
    const assigned = (metaFor(item.id).folders || []).length > 0
    const statusMatch = folderStatus.value === 'all' || (folderStatus.value === 'assigned' ? assigned : !assigned)
    const tagMatch = !tagMatchIds.value || tagMatchIds.value.has(String(item.id))
    const textMatch = !word || [item.title, item.sourceTitle, item.note].some(value => String(value || '').toLocaleLowerCase().includes(word))
    return statusMatch && tagMatch && textMatch
  })
})
watch([query, selectedFolder, folderStatus], () => {
  page.value = 1
  selectedMemoryIds.value = []
})
watch(selectedTags, async () => {
  page.value = 1
  try {
    if (!selectedTags.value.length) { tagMatchIds.value = null; return }
    const res = await request.get('/tags/search', { params: { tagIds: selectedTags.value.join(','), matchAll: true } })
    if (res.code !== '200') throw new Error(res.msg || '标签筛选失败')
    tagMatchIds.value = new Set((res.data || []).map(String))
  } catch (e) { tagMatchIds.value = new Set(); ElMessage.error(e.message || '标签筛选失败') }
}, { deep: true })
const pagedMemories = computed(() => filteredMemories.value.slice((page.value - 1) * pageSize, page.value * pageSize))
async function selectFolder(id) {
  selectedFolder.value = id == null ? null : String(id)
  page.value = 1
  selectedMemoryIds.value = []
  await load()
  await loadTagAssignments()
  await loadAllMemoryFolderAssignments()
}
function folderName(id) { return folders.value.find(folder => String(folder.id) === String(id))?.name || '文件夹' }
function typeLabel(type) { return ({ video: '视频', web: '网页', image: '图片', text: '文字' })[type] || '收藏' }
function typeStyle(type) { return ({ video: 'danger', web: 'success', image: 'warning', text: 'info' })[type] || 'info' }
function formatDate(date) { return date ? String(date).replace('T', ' ').slice(0, 16) : '' }
function safeUrl(url) { try { const parsed = new URL(url); return ['https:', 'http:'].includes(parsed.protocol) ? parsed.href : '' } catch { return '' } }
async function fillForm(item = {}) {
  for (const field of Object.keys(form)) form[field] = item[field] || (field === 'sourceType' ? 'text' : '')
  draftFolders.value = item.id == null ? [] : await loadMemoryFolders(item.id)
  if (item.id == null) { draftTags.value = []; suggestedTags.value = []; return }
  try {
    const res = await request.get(`/tags/memories/${item.id}`)
    if (res.code !== '200' || !Array.isArray(res.data)) throw new Error(res.msg || '加载标签失败')
    tagAssignments.value[String(item.id)] = res.data
    draftTags.value = res.data.map(tag => tag.id)
    suggestedTags.value = []
  } catch (e) { draftTags.value = []; ElMessage.error(e.message || '加载标签失败') }
}
async function loadMemoryFolders(memoryId) {
  try {
    const res = await request.get('/memoryFolders/selectAll', { params: { memoryId } })
    if (res.code !== '200' || !Array.isArray(res.data)) throw new Error(res.msg || '读取收藏文件夹失败')
    const ids = (Array.isArray(res.data) ? res.data : []).map(relation => String(relation.folderId))
    local.assignments[String(memoryId)] = { folders: ids }
    return ids
  } catch (e) {
    ElMessage.error(e.message || '读取收藏文件夹失败')
    return []
  }
}
async function selectMemory(item) {
  creating.value = false
  selectedId.value = item.id
  await fillForm(item)
}
async function startCreate() { creating.value = true; selectedId.value = null; await fillForm(); if (selectedFolder.value !== null) draftFolders.value = [String(selectedFolder.value)] }
async function cancelCreate() { creating.value = false; if (memories.value.length) await selectMemory(memories.value[0]) }
async function load() {
  const requestedFolder = selectedFolder.value
  loading.value = true; loadError.value = ''
  try {
    const res = await request.get('/memories/selectAll', { params: requestedFolder === null ? {} : { folderId: requestedFolder } })
    if (requestedFolder !== selectedFolder.value) return
    if (res.code !== '200') throw new Error(res.msg || '加载失败')
    memories.value = Array.isArray(res.data) ? res.data : []
    if (!creating.value) {
      const selected = memories.value.find(item => String(item.id) === String(selectedId.value))
      if (selected) { if (document.activeElement?.tagName !== 'INPUT' && document.activeElement?.tagName !== 'TEXTAREA') await selectMemory(selected) }
      else if (memories.value.length) await selectMemory(memories.value[0])
      else selectedId.value = null
    }
  } catch (e) {
    if (requestedFolder !== selectedFolder.value) return
    loadError.value = e.message || '加载失败，请检查后端是否运行'; ElMessage.error(loadError.value)
  } finally { loading.value = false }
}
async function save() {
  if (!Object.entries(form).some(([key, value]) => key !== 'sourceType' && String(value).trim())) return ElMessage.warning('请至少填写标题、链接、正文或备注')
  if (form.originalUrl && !safeUrl(form.originalUrl)) return ElMessage.warning('原始链接必须是 http 或 https 地址')
  saving.value = true
  try {
    const payload = { ...form }
    if (!creating.value) payload.id = selectedId.value
    const res = creating.value ? await request.post('/memories/add', payload) : await request.put('/memories/update', payload)
    if (res.code !== '200') throw new Error(res.msg || '保存失败')
    // 新增接口返回自增 ID；先保存关联，再按当前文件夹刷新列表。
    const id = creating.value ? res.data : selectedId.value
    if (id == null || id === '') throw new Error('收藏已保存，但接口没有返回新 ID，请重启后端后检查 /memories/add')
    const relationRes = await request.put(`/memoryFolders/memory/${id}`, draftFolders.value.map(String))
    if (relationRes.code !== '200') throw new Error(relationRes.msg || '收藏已保存，但文件夹关系保存失败')
    const selectedTagIds = await resolveSelectedTagIds()
    const tagRes = await request.put(`/tags/memories/${id}`, selectedTagIds)
    if (tagRes.code !== '200') throw new Error(tagRes.msg || '收藏已保存，但标签保存失败')
    local.assignments[String(id)] = { folders: [...draftFolders.value].map(String) }
    creating.value = false
    selectedId.value = id
    await load()
    await loadAllMemoryFolderAssignments()
    await loadTagAssignments()
    await analyzeMemory(id, true)
    ElMessage.success('收藏、文件夹和标签已保存')
  } catch (e) { ElMessage.error(e.message || '保存失败') }
  finally { saving.value = false }
}
async function removeMemory() {
  if (selectedId.value === null) return
  try { await ElMessageBox.confirm('确定删除这条收藏吗？此操作无法撤销。', '删除收藏', { type: 'warning' }) } catch { return }
  try {
    const res = await request.delete(`/memories/delete/${selectedId.value}`)
    if (res.code !== '200') throw new Error(res.msg || '删除失败')
    delete local.assignments[String(selectedId.value)]
    selectedId.value = null
    await load()
    await loadAllMemoryFolderAssignments()
    await loadTagAssignments()
    ElMessage.success('已删除')
  } catch (e) { ElMessage.error(e.message || '删除失败') }
}
async function analyzeMemory(memoryId, auto = false) {
  aiAnalyzing.value = true
  try {
    const res = await request.post(`/tags/suggest/${memoryId}`)
    if (res.code !== '200') throw new Error(res.msg || 'AI 标签分析失败')
    const suggestions = []
    for (const name of (Array.isArray(res.data) ? res.data : [])) {
      const cleanName = String(name || '').trim()
      if (!cleanName || suggestions.some(tag => tag.name === cleanName)) continue
      const existing = tags.value.find(tag => tag.name === cleanName)
      suggestions.push({ key: existing ? existing.id : `suggest:${cleanName}`, id: existing?.id || null, name: cleanName })
    }
    suggestedTags.value = suggestions
    if (!auto) ElMessage.success(suggestions.length ? 'AI 已生成候选标签，请勾选确认' : 'AI 没有提取到合适标签')
  } catch (e) { if (!auto) ElMessage.error(e.message || 'AI 标签分析失败') }
  finally { aiAnalyzing.value = false }
}
async function resolveSelectedTagIds() {
  const ids = []
  for (const value of draftTags.value) {
    const selected = suggestedTags.value.find(tag => String(tag.key) === String(value))
    if (!selected || selected.id) {
      const id = Number(value)
      if (Number.isFinite(id)) ids.push(id)
      continue
    }
    const name = selected.name
    const res = await request.post('/tags', { name })
    if (res.code !== '200' || !res.data?.id) throw new Error(res.msg || `标签「${name}」创建失败`)
    selected.id = res.data.id
    selected.key = String(res.data.id)
    ids.push(res.data.id)
  }
  return [...new Set(ids)]
}

async function loadAllMemoryFolderAssignments() {
  try {
    const res = await request.get('/memoryFolders/selectAll')
    if (res.code !== '200' || !Array.isArray(res.data)) throw new Error(res.msg || '读取收藏文件夹关系失败')
    const assignments = {}
    for (const relation of res.data) {
      const key = String(relation.memoryId)
      if (!assignments[key]) assignments[key] = { folders: [] }
      assignments[key].folders.push(String(relation.folderId))
    }
    for (const key of Object.keys(local.assignments)) delete local.assignments[key]
    for (const [memoryId, meta] of Object.entries(assignments)) local.assignments[memoryId] = meta
  } catch (e) { ElMessage.error(e.message || '读取收藏文件夹关系失败') }
}
async function loadFolders() {
  folderLoading.value = true
  folderError.value = ''
  try {
    const res = await request.get('/folders/selectAll')
    if (res.code !== '200' || !Array.isArray(res.data)) throw new Error(res.msg || '文件夹加载失败')
    folders.value = res.data
    if (selectedFolder.value !== null && !folders.value.some(folder => String(folder.id) === String(selectedFolder.value))) selectedFolder.value = null
    return true
  } catch (e) {
    folderError.value = e.message || '文件夹加载失败，请确认后端和 folders 表可用'
    ElMessage.error(folderError.value)
    return false
  } finally {
    folderLoading.value = false
  }
}
async function openFolderCreate(parentId) {
  let value
  const parent = parentId == null ? null : folders.value.find(folder => String(folder.id) === String(parentId))
  try {
    const result = await ElMessageBox.prompt(
        parent ? `在「${parent.name}」下面新建子文件夹` : '输入根文件夹名称',
        '新建文件夹',
        { inputValidator: text => Boolean(text?.trim()) && text.trim().length <= 100 || '名称不能为空且不能超过 100 字' }
    )
    value = result.value.trim()
  } catch { return }
  try {
    const payload = {
      name: value,
      parentId: parent ? String(parent.id) : null,
      sortOrder: 0
    }
    const res = await request.post('/folders/add', payload)
    if (res.code !== '200') throw new Error(res.msg || '新建文件夹失败')
    const loaded = await loadFolders()
    if (loaded) {
      if (parent) selectedFolder.value = String(parent.id)
      ElMessage.success(parent ? `已在「${parent.name}」下创建「${value}」` : `已创建根文件夹「${value}」`)
    }
  } catch (e) { ElMessage.error(e.message || '新建文件夹失败') }
}
async function renameFolder(folder) {
  let value
  try {
    const result = await ElMessageBox.prompt('修改文件夹名称', '重命名', { inputValue: folder.name, inputValidator: text => Boolean(text?.trim()) && text.trim().length <= 100 || '名称不能为空且不能超过 100 字' })
    value = result.value.trim()
  } catch { return }
  try {
    const res = await request.put('/folders/update', { id: folder.id, name: value })
    if (res.code !== '200') throw new Error(res.msg || '重命名失败')
    if (await loadFolders()) ElMessage.success('文件夹名称已更新')
  } catch (e) { ElMessage.error(e.message || '重命名失败') }
}
async function deleteFolder(folder) {
  try { await ElMessageBox.confirm(`删除「${folder.name}」及其子文件夹？收藏本身不会删除。`, '删除文件夹', { type: 'warning' }) } catch { return }
  const removed = descendantIds(folder.id)
  const subtree = folders.value.filter(item => removed.has(String(item.id)))
  const depthOf = item => {
    let depth = 0
    let parentId = item.parentId == null || item.parentId === '' || String(item.parentId) === '0' ? null : String(item.parentId)
    while (parentId && depth <= folders.value.length) {
      depth += 1
      const parent = folders.value.find(candidate => String(candidate.id) === parentId)
      parentId = parent && parent.parentId != null && parent.parentId !== '' && String(parent.parentId) !== '0' ? String(parent.parentId) : null
    }
    return depth
  }
  subtree.sort((a, b) => depthOf(b) - depthOf(a))
  try {
    for (const item of subtree) {
      const res = await request.delete(`/folders/delete/${item.id}`)
      if (res.code !== '200') throw new Error(res.msg || `删除文件夹「${item.name}」失败`)
    }
    for (const meta of Object.values(local.assignments)) meta.folders = (meta.folders || []).filter(id => !removed.has(String(id)))
    draftFolders.value = draftFolders.value.filter(id => !removed.has(String(id)))
    if (selectedFolder.value !== null && removed.has(String(selectedFolder.value))) selectedFolder.value = null
    await loadFolders()
    await loadAllMemoryFolderAssignments()
    await load()
    ElMessage.success('文件夹及子文件夹已删除')
  } catch (e) {
    await loadFolders()
    ElMessage.error(e.message || '删除文件夹失败；已刷新文件夹列表')
  }
}
async function newTag() {
  let name
  try {
    const response = await ElMessageBox.prompt('输入标签名称', '新增标签', { inputValidator: value => Boolean(value?.trim()) && value.trim().length <= 100 || '标签须为1到100字' })
    name = response.value.trim()
  } catch { return }
  try {
    const res = await request.post('/tags', { name })
    if (res.code !== '200') throw new Error(res.msg || '新增标签失败')
    await loadTags()
    if ((creating.value || selectedId.value != null) && !draftTags.value.includes(res.data.id)) draftTags.value.push(res.data.id)
  } catch (e) { ElMessage.error(e.message || '新增标签失败') }
}
async function loadTags() {
  const res = await request.get('/tags')
  if (res.code !== '200') throw new Error(res.msg || '标签加载失败')
  tags.value = res.data || []
}
async function loadTagAssignments() {
  const result = {}
  // 本次列表可能是文件夹子集；逐条加载确保标签展示与数据库一致。
  await Promise.all(memories.value.map(async item => {
    const res = await request.get(`/tags/memories/${item.id}`)
    if (res.code !== '200') throw new Error(res.msg || '加载收藏标签失败')
    result[String(item.id)] = res.data || []
  }))
  tagAssignments.value = result
}
async function refresh() { await load(); await loadTagAssignments(); await loadAllMemoryFolderAssignments(); await loadSopList() }

async function loadSopList() {
  sopLoading.value = true
  try {
    const res = await request.get('/sops/selectAll', { params: { keyword: '' } })
    if (res.code !== '200') throw new Error(res.msg || 'SOP 加载失败')
    sopList.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    sopList.value = []
  } finally { sopLoading.value = false }
}

function openSops(sop) {
  if (sop?.id) router.push({ path: '/front/sops', query: { focus: sop.id } })
  else router.push('/front/sops')
}

// 监听来自 SOP 阅读视图的 ?focus=收藏ID
watch(() => route.query.focus, async (memoryId) => {
  if (!memoryId) return
  await nextTick()
  const id = Number(memoryId)
  const target = memories.value.find(m => Number(m.id) === id)
  if (target) {
    await selectMemory(target)
    await nextTick()
    // 滚动到对应卡片
    const el = document.querySelector(`.memory-card.is-focus`)
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    // 清除 query，避免下次刷新重复触发
    router.replace({ path: route.path, query: {} })
  } else {
    ElMessage.info('该收藏不在当前文件夹/标签下，切换到"全部收藏"后再试')
  }
})
async function addToFolders() {
  if (!selectedMemoryIds.value.length) return ElMessage.warning('请先勾选要加入文件夹的收藏')
  if (!bulkFolders.value.length) return ElMessage.warning('请选择目标文件夹')
  bulkSaving.value = true
  try {
    for (const memoryId of selectedMemoryIds.value) {
      const res = await request.get('/memoryFolders/selectAll', { params: { memoryId } })
      if (res.code !== '200') throw new Error(res.msg || '读取文件夹归属失败')
      const ids = [...new Set([...(res.data || []).map(row => String(row.folderId)), ...bulkFolders.value])]
      const saved = await request.put(`/memoryFolders/memory/${memoryId}`, ids)
      if (saved.code !== '200') throw new Error(saved.msg || '批量归类失败')
    }
    selectedMemoryIds.value = []
    await refresh()
    ElMessage.success('已批量加入文件夹')
  } catch (e) { ElMessage.error(`部分收藏可能已修改：${e.message || '批量操作失败'}`) }
  finally { bulkSaving.value = false }
}
onMounted(async () => {
  try { await loadFolders(); await loadTags(); await load(); await loadAllMemoryFolderAssignments(); await loadTagAssignments(); await loadSopList() }
  catch (e) { ElMessage.error(e.message || '加载失败；请先执行标签建表 SQL 并重启后端') }
})
</script>

<style scoped>
.memories-page { height: calc(100vh - 60px); min-height: 600px; display: flex; flex-direction: column; background: #f5f7fb; color: #283449; }
.toolbar { min-height: 72px; display: flex; align-items: center; gap: 24px; padding: 12px 26px; background: white; border-bottom: 1px solid #e9edf4; }
.brand { display: flex; align-items: center; gap: 10px; font-size: 20px; font-weight: 700; white-space: nowrap; }
.brand-icon { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 10px; background: #e6efff; color: #4480df; }
.global-search { width: min(360px, 32%); margin-left: auto; }
.tag-filter { width: min(260px, 23%); }
.ai-suggestions { padding: 10px; margin-bottom: 10px; border: 1px solid #dbeafe; border-radius: 8px; background: #f5f9ff; }
.suggestion-label { display: block; color: #3979d3; font-size: 12px; margin-bottom: 6px; }
.ai-suggestions :deep(.el-checkbox-group) { display: flex; flex-wrap: wrap; gap: 4px 12px; }
.toolbar-actions { display: flex; gap: 8px; white-space: nowrap; }
.workspace { display: grid; grid-template-columns: 235px minmax(300px, 1fr) minmax(350px, 410px); min-height: 0; flex: 1; }
.sidebar, .list-panel, .detail-panel { overflow-y: auto; min-width: 0; }
.sidebar { padding: 24px 14px; background: #fff; border-right: 1px solid #e9edf4; }
.sidebar-heading { display: flex; justify-content: space-between; align-items: center; font-size: 13px; font-weight: 700; color: #77849a; margin: 0 4px 12px; }
.tags-heading { border-top: 1px solid #eef0f5; margin-top: 26px; padding-top: 20px; }
.nav-item { width: 100%; padding: 10px 12px; display: flex; justify-content: space-between; border: 0; border-radius: 9px; background: transparent; text-align: left; color: #4d5c70; cursor: pointer; }
.nav-item:hover, .nav-item.active, .tree-node.active { background: #eaf2ff; color: #2a68cb; }
.folder-tree { margin-top: 8px; }
.tree-node-row { display: flex; align-items: center; width: 100%; min-width: 0; border-radius: 6px; }
.tree-node-row:hover { background: #f2f6fc; }
.tree-node-row.active { background: #eaf2ff; color: #2a68cb; }
.tree-node-row .tree-node { flex: 1; min-width: 0; }
.tree-add-button { flex: 0 0 auto; width: 24px; height: 24px; padding: 0; opacity: 0; font-size: 17px; }
.tree-node-row:hover .tree-add-button, .tree-node-row.active .tree-add-button { opacity: 1; }
.sidebar-hint, .local-hint { padding: 0 10px; color: #9aa4b3; font-size: 12px; }
.local-hint { margin-top: 28px; line-height: 1.6; }
.list-panel { padding: 28px 24px; }
.bulk-toolbar { position: sticky; top: 0; z-index: 3; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; padding: 10px; border: 1px solid #e8edf4; border-radius: 10px; background: white; }
.folder-status { display: flex; align-items: center; flex-wrap: wrap; margin-right: 4px; }
.folder-status :deep(.el-radio) { margin-right: 10px; }
.bulk-select { width: min(320px, 100%); }
.list-header, .detail-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 22px; }
h2 { font-size: 19px; margin: 0 0 5px; } .list-header p { margin: 0; color: #8b98ab; font-size: 13px; }
.memory-card { background: #fff; border: 1px solid #e8edf4; border-radius: 12px; padding: 18px; margin-bottom: 12px; cursor: pointer; transition: border-color .2s, box-shadow .2s; }
.memory-card:hover, .memory-card.selected { border-color: #8db5f4; box-shadow: 0 5px 18px rgba(56, 102, 168, .1); }
.card-head, .card-foot { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }.card-date { margin-left: auto; color: #99a5b5; font-size: 12px; }
.memory-card h3 { margin: 13px 0 8px; font-size: 16px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.card-note { margin: 0 0 12px; color: #59677c; line-height: 1.6; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; white-space: pre-wrap; }.muted { color: #9aa4b3; }
.tag-chip { font-size: 12px; color: #3979d3; background: #edf4ff; padding: 3px 8px; border-radius: 5px; }.folder-count { margin-left: auto; color: #9aa4b3; font-size: 12px; }
.pagination { justify-content: center; padding: 16px 0; }.loading-state { padding: 40px; text-align: center; color: #8b98ab; }.load-alert { margin-bottom: 12px; }
.detail-panel { background: #fff; border-left: 1px solid #e9edf4; padding: 26px 24px; }.eyebrow { color: #7da4e2; font-size: 11px; font-weight: 700; letter-spacing: .12em; margin-bottom: 6px; }
.detail-form :deep(.el-form-item) { margin-bottom: 17px; }.detail-form :deep(.el-form-item__label) { font-weight: 600; color: #526077; padding-bottom: 5px; }.detail-form :deep(.el-select) { width: 100%; }.open-link { display: block; margin-top: 6px; font-size: 12px; word-break: break-all; }
.section-label { font-weight: 600; margin: 18px 0 10px; }.folder-label { margin-top: 24px; }.tag-editor { display: flex; flex-wrap: wrap; gap: 6px; }.feature-hint { font-size: 12px; color: #9aa4b3; line-height: 1.5; }.full-width { width: 100%; }.save-row { display: flex; gap: 8px; padding: 24px 0 40px; }
.folder-row { display: flex; justify-content: space-between; align-items: center; min-height: 42px; border-bottom: 1px solid #eef0f5; gap: 8px; }.folder-row > span:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
@media (max-width: 1100px) { .workspace { grid-template-columns: 195px minmax(280px, 1fr) minmax(310px, 36%); } .sidebar, .list-panel, .detail-panel { padding-left: 14px; padding-right: 14px; } }
@media (max-width: 800px) { .memories-page { height: auto; min-height: 100vh; }.toolbar { flex-wrap: wrap; }.global-search { width: 100%; order: 3; }.workspace { display: block; }.sidebar, .list-panel, .detail-panel { overflow: visible; }.detail-panel { border-left: 0; border-top: 1px solid #e9edf4; }.sidebar { max-height: 280px; overflow-y: auto; } }


/* ===== Retro pixel-game skin: visual-only overrides; API and business logic unchanged ===== */
.memories-page {
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
.memories-page * { box-sizing: border-box; }
.toolbar {
  min-height: 78px;
  gap: 14px;
  padding: 12px 20px;
  background: var(--pixel-deep);
  color: var(--pixel-paper);
  border: 4px solid var(--pixel-ink);
  border-bottom: 5px solid var(--pixel-shadow);
  box-shadow: inset 0 0 0 3px #9cbd87;
}
.brand { color: #fff4c8; font-size: 21px; text-shadow: 2px 2px 0 #243d2d; }
.brand-icon {
  width: 38px; height: 38px; border-radius: 0;
  background: var(--pixel-gold); color: var(--pixel-ink);
  border: 3px solid var(--pixel-ink);
  box-shadow: 3px 3px 0 #203a2c;
}
.global-search, .tag-filter { --el-input-border-color: #496c50; --el-border-color: #496c50; }
.memories-page :deep(.el-input__wrapper),
.memories-page :deep(.el-select__wrapper) {
  min-height: 38px;
  border-radius: 0;
  background: #f7f0d5;
  box-shadow: inset 0 0 0 2px #a3b88a, 0 0 0 2px #263d2e !important;
}
.memories-page :deep(.el-input__inner),
.memories-page :deep(.el-select__placeholder),
.memories-page :deep(.el-input__inner::placeholder) { color: #52634a; }
.memories-page :deep(.el-button) {
  border-radius: 0;
  font-family: inherit;
  font-weight: 700;
  border: 2px solid #294332;
  box-shadow: 2px 2px 0 #294332;
  transition: transform .08s, box-shadow .08s;
}
.memories-page :deep(.el-button:active) { transform: translate(2px, 2px); box-shadow: none; }
.memories-page :deep(.el-button--primary) {
  --el-button-bg-color: #eac85e;
  --el-button-border-color: #294332;
  --el-button-text-color: #263d2e;
  --el-button-hover-bg-color: #f6d978;
  --el-button-hover-border-color: #294332;
  --el-button-hover-text-color: #263d2e;
}
.memories-page :deep(.el-button:not(.el-button--primary):not(.is-link)) {
  background: #e8e5c8; color: #2b4936;
}
.workspace {
  grid-template-columns: 238px minmax(300px, 1fr) minmax(350px, 410px);
  gap: 8px;
  padding: 8px;
  min-height: 0;
  flex: 1;
}
.sidebar, .list-panel, .detail-panel {
  min-width: 0;
  overflow-y: auto;
  border: 3px solid var(--pixel-ink);
  box-shadow: inset 0 0 0 3px #c6d5a8, 4px 4px 0 rgba(38,61,46,.48);
}
.sidebar {
  padding: 20px 13px;
  background: var(--pixel-paper);
  border-right: 3px solid var(--pixel-ink);
}
.sidebar-heading {
  color: #476849;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: .04em;
}
.tags-heading { border-top: 2px dashed #a9b78c; }
.nav-item {
  border-radius: 0;
  margin: 3px 0;
  border: 2px solid transparent;
  color: #344c37;
  font-weight: 700;
}
.nav-item:hover, .nav-item.active, .tree-node.active {
  background: #f2d66e;
  color: #263d2e;
  border-color: #7c6036;
  box-shadow: 2px 2px 0 #8c7747;
}
.folder-tree { background: transparent; }
.memories-page :deep(.el-tree) { background: transparent; color: var(--pixel-ink); }
.memories-page :deep(.el-tree-node__content) { height: 34px; }
.memories-page :deep(.el-tree-node__content:hover) { background: #e7e4c1; }
.tree-node-row { border-radius: 0; }
.tree-node-row.active { background: #f2d66e; color: #263d2e; }
.sidebar-hint, .local-hint { color: #6e8064; }
.list-panel {
  padding: 22px 18px;
  background: #e4e8c9;
}
.list-header { align-items: center; margin-bottom: 16px; }
.list-header h2, .detail-header h2 { color: #263d2e; text-shadow: 1px 1px #fff9dd; }
.list-header p { color: #607858; }
.bulk-toolbar {
  border: 2px solid #526f4c;
  border-radius: 0;
  background: #f4efcf;
  box-shadow: 3px 3px 0 rgba(38,61,46,.22);
}
.memories-page :deep(.el-radio__inner),
.memories-page :deep(.el-checkbox__inner) { border-radius: 0; border-color: #45664a; }
.memories-page :deep(.el-radio__input.is-checked .el-radio__inner),
.memories-page :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #639365; border-color: #294332;
}
.memory-card {
  position: relative;
  border: 3px solid #91a77c;
  border-radius: 0;
  padding: 15px 16px;
  margin-bottom: 12px;
  background: #f8f2d9;
  box-shadow: 3px 3px 0 #a8b78e, inset 0 0 0 2px #fffbe7;
  transition: transform .12s, box-shadow .12s, background .12s;
}
.memory-card:hover {
  transform: translate(-1px, -1px);
  border-color: #4c7650;
  box-shadow: 4px 4px 0 #78966d, inset 0 0 0 2px #fffbe7;
}
.memory-card.selected {
  background: #fff1a9;
  border-color: #98733c;
  box-shadow: 4px 4px 0 #98733c, inset 0 0 0 2px #fff9d6;
}
.memories-page :deep(.el-tag) { border-radius: 0; font-weight: 700; }
.card-date { color: #72836a; }
.memory-card h3 { color: #263d2e; font-size: 17px; }
.card-note { color: #52664c; }
.tag-chip {
  border: 1px solid #9eae7e;
  border-radius: 0;
  color: #365c3b;
  background: #e3e9c6;
  padding: 3px 7px;
}
.pagination { --el-pagination-button-bg-color: #f4efcf; --el-pagination-hover-color: #3c7549; }
.detail-panel {
  padding: 22px 20px;
  background: #f3edcf;
  border-left: 3px solid var(--pixel-ink);
}
.eyebrow { color: #64875e; letter-spacing: .1em; }
.detail-header { border-bottom: 2px dashed #a9b78c; padding-bottom: 13px; }
.detail-form :deep(.el-form-item__label) { color: #405d41; }
.detail-form :deep(.el-textarea__inner) {
  border-radius: 0;
  background: #fff9e5;
  color: #304b36;
  box-shadow: inset 0 0 0 2px #b4c39a, 2px 2px 0 #8b9f76;
  font-family: inherit;
}
.detail-form :deep(.el-input__wrapper),
.detail-form :deep(.el-select__wrapper) { border-radius: 0; }
.open-link { color: #356c42; font-weight: 700; }
.section-label { color: #36563b; }
.ai-suggestions {
  border: 2px dashed #7fa174;
  border-radius: 0;
  background: #e8edcf;
}
.suggestion-label { color: #3e7548; font-weight: 700; }
.feature-hint { color: #6c7e5e; }
.folder-row { border-bottom: 2px dotted #b6c19a; }
.loading-state { color: #58744f; }
.memories-page :deep(.el-empty__description p) { color: #728365; }
.memories-page :deep(.el-dialog) {
  border: 4px solid #294332;
  border-radius: 0;
  background: #f3edcf;
  box-shadow: 6px 6px 0 rgba(38,61,46,.45);
}
.memories-page :deep(.el-dialog__header) {
  margin-right: 0;
  padding: 18px 20px;
  background: #527c57;
  border-bottom: 3px solid #294332;
}
.memories-page :deep(.el-dialog__title) { color: #fff4cf; font-weight: 700; }
.memories-page :deep(.el-dialog__body) { color: #304b36; }
@media (max-width: 1100px) {
  .workspace { grid-template-columns: 205px minmax(270px, 1fr) minmax(300px, 36%); gap: 6px; padding: 6px; }
  .toolbar { gap: 10px; padding: 10px 14px; }
}
@media (max-width: 800px) {
  .memories-page { height: auto; min-height: 100vh; }
  .toolbar { flex-wrap: wrap; }
  .global-search { width: 100%; order: 3; }
  .workspace { display: block; padding: 6px; }
  .sidebar, .list-panel, .detail-panel { overflow: visible; margin-bottom: 8px; }
  .detail-panel { border-left: 3px solid var(--pixel-ink); border-top: 3px solid var(--pixel-ink); }
  .sidebar { max-height: 300px; overflow-y: auto; }
  .bulk-toolbar { position: static; }
}


/* 左侧文件夹区与标签区各占侧栏高度的约三分之一，内容超出时各自滚动 */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow: hidden;
}
.sidebar-section {
  flex: 0 0 33.333%;
  height: 33.333%;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 3px 8px;
  scrollbar-width: thin;
  scrollbar-color: #8ca77a #e8e6c8;
}
.folder-section { border-bottom: 2px dashed #a9b78c; }
.tag-section { border-top: 2px dashed #a9b78c; padding-top: 8px; }
.sop-section { border-top: 2px dashed #a9b78c; padding-top: 8px; }
.sop-list-scroll { min-height: 0; max-height: calc(100% - 60px); overflow-y: auto; }
.sidebar-section .sop-heading { margin-top: 0; padding-top: 0; border-top: 0; }
.sop-section .nav-item.sop-item { font-size: 12px; padding: 7px 10px; }
.sop-section .nav-item.disabled { color: #9aa4b3; cursor: default; background: transparent; }
.section-label.sop-label { margin-top: 24px; }
.sop-ref-actions { display: flex; gap: 8px; align-items: center; }
.sop-ref-actions .full-width { flex: 1; }
.memory-card.is-focus { animation: sopFocusBlink 1.6s ease-in-out 2; }
@keyframes sopFocusBlink {
  0%, 100% { box-shadow: 3px 3px 0 #a8b78e, inset 0 0 0 2px #fffbe7; }
  50% { box-shadow: 0 0 0 4px #f0ca58, inset 0 0 0 2px #fffbe7; }
}
.tag-list-scroll { min-height: 0; }
.sidebar-section .sidebar-heading {
  position: sticky;
  top: 0;
  z-index: 2;
  padding: 7px 0;
  background: var(--pixel-paper);
}
.sidebar-section .tags-heading { margin-top: 0; }
.sidebar-section .folder-tree { margin-top: 6px; }
.sidebar-section .local-hint { margin: 10px 0 4px; }
@media (max-width: 800px) {
  .sidebar { display: block; overflow-y: auto; }
  .sidebar-section { height: auto; max-height: 33vh; margin-bottom: 10px; }
}



/* ===== Keep the middle panel title and filter bar visible while scrolling ===== */
.list-panel {
  /* Keep scrolling within the middle column */
  position: relative;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}
.list-header {
  position: sticky;
  top: -22px; /* counter the panel's top padding so the heading stays at the top */
  z-index: 8;
  margin: -22px -18px 0;
  padding: 22px 18px 12px;
  background: #e4e8c9;
  border-bottom: 2px solid #a7b88e;
}
.bulk-toolbar {
  position: sticky;
  top: 69px;
  z-index: 7;
  margin-top: 0;
  background: #f4efcf;
  box-shadow: 3px 3px 0 rgba(38,61,46,.22);
}
@media (max-width: 1100px) {
  .list-header {
    margin-left: -14px;
    margin-right: -14px;
    padding-left: 14px;
    padding-right: 14px;
  }
}
@media (max-width: 800px) {
  .list-panel {
    overflow: visible;
    overscroll-behavior: auto;
  }
  .list-header {
    position: static;
    margin: 0 0 16px;
    padding: 0;
    border-bottom: 0;
  }
  .bulk-toolbar {
    position: static;
    margin-bottom: 14px;
  }
}



/* 搜索与操作区移动到中间栏标题下方；右侧详情栏和业务逻辑保持不变 */
.toolbar {
  justify-content: flex-start;
}
.toolbar .brand {
  margin-right: auto;
}
.list-actions {
  position: sticky;
  top: 69px;
  z-index: 7;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(130px, .8fr);
  align-items: center;
  gap: 10px;
  margin: 0 -2px 12px;
  padding: 10px 2px;
  background: #e4e8c9;
  border-bottom: 2px solid #a7b88e;
}
.list-actions .global-search,
.list-actions .tag-filter {
  width: 100%;
  min-width: 0;
  margin-left: 0;
}
.list-actions .toolbar-actions {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.list-actions .toolbar-actions :deep(.el-button) {
  margin-left: 0;
}
.bulk-toolbar {
  top: 151px;
}
@media (max-width: 1100px) {
  .list-actions {
    margin-left: -2px;
    margin-right: -2px;
  }
}
@media (max-width: 800px) {
  .list-actions {
    position: static;
    display: flex;
    flex-direction: column;
    align-items: stretch;
    margin: 0 0 12px;
    padding: 0 0 12px;
    border-bottom: 0;
  }
  .list-actions .global-search,
  .list-actions .tag-filter {
    width: 100%;
  }
  .list-actions .toolbar-actions {
    display: flex;
    width: 100%;
  }
  .bulk-toolbar {
    position: static;
    top: auto;
  }
}
</style>