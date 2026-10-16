<template>
  <div class="overview-page">
    <header class="toolbar">
      <div class="brand">
        <span class="brand-icon">◈</span>
        <span>记忆库总览</span>
      </div>
      <div class="toolbar-actions">
        <el-button type="primary" @click="router.push('/front/memories')">+ 去记录一条记忆</el-button>
        <el-button @click="refreshAll" :loading="refreshing">刷新数据</el-button>
      </div>
    </header>

    <div class="stats-grid">
      <div class="stat-card"><div class="stat-icon green">✦</div><div><span>收藏总数</span><strong>{{ stats.memories }}</strong></div></div>
      <div class="stat-card"><div class="stat-icon gold">▣</div><div><span>文件夹</span><strong>{{ stats.folders }}</strong></div></div>
      <div class="stat-card"><div class="stat-icon purple">#</div><div><span>标签</span><strong>{{ stats.tags }}</strong></div></div>
      <div class="stat-card"><div class="stat-icon orange">↗</div><div><span>已归类收藏</span><strong>{{ stats.assigned }}</strong></div></div>
      <div class="stat-card"><div class="stat-icon blue">✓</div><div><span>归类关系</span><strong>{{ stats.relations }}</strong></div></div>
    </div>

    <el-tabs v-model="activeTab" class="overview-tabs">
      <!-- ============ 仪表盘 ============ -->
      <el-tab-pane label="仪表盘" name="dashboard">
        <div class="dashboard-grid">
          <section class="panel">
            <div class="panel-title"><h2>最近收藏</h2><el-button link type="primary" @click="router.push('/front/memories')">去记忆库查看</el-button></div>
            <el-skeleton v-if="loadingRecent" :rows="5" animated />
            <div v-else-if="recent.length" class="recent-list">
              <div v-for="item in recent" :key="item.id" class="recent-item">
                <div class="recent-type">{{ typeLabel(item.sourceType) }}</div>
                <div class="recent-main"><strong>{{ item.title || item.sourceTitle || '未命名收藏' }}</strong><span>{{ item.note || item.originalText || '暂无备注' }}</span></div>
                <time>{{ formatDate(item.createdAt) }}</time>
              </div>
            </div>
            <el-empty v-else description="还没有收藏" :image-size="70" />
          </section>
          <section class="panel">
            <div class="panel-title"><h2>快速入口</h2></div>
            <div class="quick-list">
              <button @click="activeTab = 'tags'"># 标签管理<div>新增、修改、删除标签</div></button>
              <button @click="activeTab = 'folders'">▣ 文件夹管理<div>维护多级文件夹结构</div></button>
              <button @click="activeTab = 'relations'">↗ 归类关系<div>查看/解除收藏与文件夹的关联</div></button>
            </div>
            <div class="tip">
              <strong>使用建议</strong>
              <span>先随手记录标题、链接和备注，再在记忆详情中选择标签与文件夹；标签/文件夹可在此总览页统一维护。</span>
            </div>
          </section>
        </div>
      </el-tab-pane>

      <!-- ============ 标签管理 ============ -->
      <el-tab-pane label="标签管理" name="tags">
        <div class="page-card">
          <div class="page-toolbar">
            <el-input v-model="tagKeyword" clearable placeholder="搜索标签名称" @keyup.enter="loadTags" />
            <el-button type="primary" @click="loadTags">搜索</el-button>
            <el-button @click="tagKeyword = ''; loadTags()">重置</el-button>
            <el-button type="success" @click="createTagInline">+ 新建标签</el-button>
          </div>
          <el-table :data="filteredTags" stripe v-loading="loadingTags">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="标签名称" min-width="220" />
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="renameTag(scope.row)">重命名</el-button>
                <el-button link type="danger" @click="deleteTag(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ============ 文件夹管理 ============ -->
      <el-tab-pane label="文件夹管理" name="folders">
        <div class="page-card">
          <div class="page-toolbar">
            <el-input v-model="folderKeyword" clearable placeholder="搜索文件夹名称" @keyup.enter="loadFolders" />
            <el-button type="primary" @click="loadFolders">搜索</el-button>
            <el-button @click="folderKeyword = ''; loadFolders()">重置</el-button>
            <el-button type="success" @click="createFolderInline(null)">+ 新建根文件夹</el-button>
          </div>
          <el-table :data="filteredFolders" stripe v-loading="loadingFolders" row-key="id" :tree-props="{ children: 'children' }" :default-expand-all="true">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="父级" width="120">
              <template #default="scope">{{ scope.row.parentId || '根目录' }}</template>
            </el-table-column>
            <el-table-column prop="name" label="文件夹名称" min-width="220" />
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="createFolderInline(scope.row)">新建子级</el-button>
                <el-button link @click="renameFolderRow(scope.row)">重命名</el-button>
                <el-button link type="danger" @click="deleteFolderRow(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ============ 收藏归类关系 ============ -->
      <el-tab-pane label="归类关系" name="relations">
        <div class="page-card">
          <div class="page-toolbar">
            <el-input v-model="relKeyword" clearable placeholder="搜索收藏 ID 或文件夹 ID" @keyup.enter="loadRelations" />
            <el-button type="primary" @click="loadRelations">搜索</el-button>
            <el-button @click="relKeyword = ''; loadRelations()">重置</el-button>
            <el-button type="success" :loading="addingRelation" @click="addRelationInline">+ 新增归类</el-button>
          </div>
          <el-table :data="filteredRelations" stripe v-loading="loadingRelations">
            <el-table-column prop="id" label="关系 ID" width="100" />
            <el-table-column label="收藏标题" min-width="240">
              <template #default="scope">{{ memoryTitle(scope.row.memoryId) || ('#' + scope.row.memoryId) }}</template>
            </el-table-column>
            <el-table-column label="文件夹" min-width="180">
              <template #default="scope">{{ folderNameFor(scope.row.folderId) || ('#' + scope.row.folderId) }}</template>
            </el-table-column>
            <el-table-column prop="memoryId" label="收藏 ID" width="100" />
            <el-table-column prop="folderId" label="文件夹 ID" width="100" />
            <el-table-column prop="createdAt" label="归类时间" width="180" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button link type="danger" @click="deleteRelation(scope.row)">解除归类</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const activeTab = ref('dashboard')
const refreshing = ref(false)

const stats = reactive({ memories: 0, folders: 0, tags: 0, assigned: 0, relations: 0 })
const recent = ref([])
const loadingRecent = ref(false)

// 标签
const tags = ref([])
const loadingTags = ref(false)
const tagKeyword = ref('')
const filteredTags = computed(() => {
  const word = tagKeyword.value.trim().toLowerCase()
  return word ? tags.value.filter(t => String(t.name || '').toLowerCase().includes(word)) : tags.value
})

// 文件夹
const folderTreeData = ref([])
const flatFolders = ref([])
const loadingFolders = ref(false)
const folderKeyword = ref('')
const filteredFolders = computed(() => {
  const word = folderKeyword.value.trim().toLowerCase()
  return word ? flatFolders.value.filter(f => String(f.name || '').toLowerCase().includes(word)) : folderTreeData.value
})

// 归类关系
const relations = ref([])
const loadingRelations = ref(false)
const relKeyword = ref('')
const addingRelation = ref(false)
const memoryLookup = ref({})
const filteredRelations = computed(() => {
  const word = relKeyword.value.trim()
  if (!word) return relations.value
  return relations.value.filter(r => String(r.memoryId).includes(word) || String(r.folderId).includes(word))
})

const typeLabel = type => ({ video: '视频', web: '网页', image: '图片', text: '文字' })[type] || '收藏'
const formatDate = date => date ? String(date).replace('T', ' ').slice(0, 16) : ''

function memoryTitle(id) {
  return memoryLookup.value[String(id)] || ''
}
function folderNameFor(id) {
  return flatFolders.value.find(f => String(f.id) === String(id))?.name || ''
}

// ===== 数据加载 =====
async function loadDashboard() {
  loadingRecent.value = true
  try {
    const [memories, folders, tags, relations] = await Promise.all([
      request.get('/memories/selectAll'),
      request.get('/folders/selectAll'),
      request.get('/tags'),
      request.get('/memoryFolders/selectAll')
    ])
    const memList = Array.isArray(memories.data) ? memories.data : []
    recent.value = memList.slice(0, 8)
    stats.memories = memList.length
    stats.folders = Array.isArray(folders.data) ? folders.data.length : 0
    stats.tags = Array.isArray(tags.data) ? tags.data.length : 0
    const relList = Array.isArray(relations.data) ? relations.data : []
    stats.relations = relList.length
    stats.assigned = new Set(relList.map(item => String(item.memoryId))).size
  } finally { loadingRecent.value = false }
}

async function loadTags() {
  loadingTags.value = true
  try {
    const res = await request.get('/tags')
    if (res.code !== '200') throw new Error(res.msg || '标签加载失败')
    tags.value = Array.isArray(res.data) ? res.data : []
  } catch (e) { ElMessage.error(e.message || '标签加载失败') }
  finally { loadingTags.value = false }
}

async function loadFolders() {
  loadingFolders.value = true
  try {
    const res = await request.get('/folders/selectAll')
    if (res.code !== '200') throw new Error(res.msg || '文件夹加载失败')
    const list = Array.isArray(res.data) ? res.data : []
    flatFolders.value = list
    folderTreeData.value = buildFolderTree(list)
  } catch (e) { ElMessage.error(e.message || '文件夹加载失败') }
  finally { loadingFolders.value = false }
}

function buildFolderTree(list) {
  const nodes = list.map(f => ({ ...f, id: String(f.id), children: [] }))
  const byId = new Map(nodes.map(f => [f.id, f]))
  const roots = []
  for (const node of nodes) {
    const pid = node.parentId == null || node.parentId === '' || String(node.parentId) === '0' ? null : String(node.parentId)
    if (pid && byId.has(pid)) byId.get(pid).children.push(node)
    else roots.push(node)
  }
  const sortFn = arr => { arr.sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0) || a.name.localeCompare(b.name, 'zh-CN')); arr.forEach(n => sortFn(n.children)) }
  sortFn(roots)
  return roots
}

async function loadRelations() {
  loadingRelations.value = true
  try {
    const [relRes, memRes] = await Promise.all([
      request.get('/memoryFolders/selectAll'),
      request.get('/memories/selectAll')
    ])
    relations.value = Array.isArray(relRes.data) ? relRes.data : []
    const map = {}
    for (const m of (Array.isArray(memRes.data) ? memRes.data : [])) {
      map[String(m.id)] = m.title || m.sourceTitle || ('收藏 #' + m.id)
    }
    memoryLookup.value = map
  } catch (e) { ElMessage.error(e.message || '归类关系加载失败') }
  finally { loadingRelations.value = false }
}

async function refreshAll() {
  refreshing.value = true
  try {
    await Promise.all([loadDashboard(), loadTags(), loadFolders(), loadRelations()])
  } finally { refreshing.value = false }
}

// ===== 标签操作 =====
async function createTagInline() {
  try {
    const { value } = await ElMessageBox.prompt('输入新标签名称', '新建标签', { inputValidator: v => v?.trim() ? true : '名称不能为空' })
    const res = await request.post('/tags', { name: value.trim() })
    if (res.code !== '200') throw new Error(res.msg || '新增标签失败')
    ElMessage.success('标签已创建')
    await loadTags()
    stats.tags = tags.value.length
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message || '新增失败') }
}
async function renameTag(tag) {
  try {
    const { value } = await ElMessageBox.prompt('输入新的标签名称', '重命名标签', {
      inputValue: tag.name,
      inputValidator: v => v?.trim() ? true : '名称不能为空'
    })
    const res = await request.put(`/tags/${tag.id}`, { name: value.trim() })
    if (res.code !== '200') throw new Error(res.msg || '重命名失败')
    ElMessage.success('标签已重命名')
    await loadTags()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message || '重命名失败') }
}
async function deleteTag(tag) {
  try {
    await ElMessageBox.confirm(`删除标签「${tag.name}」会解除所有收藏上的该标签，但不会删除收藏。继续吗？`, '删除标签', { type: 'warning' })
    const res = await request.delete(`/tags/${tag.id}`)
    if (res.code !== '200') throw new Error(res.msg || '删除失败')
    ElMessage.success('标签已删除')
    await Promise.all([loadTags(), loadRelations(), loadDashboard()])
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message || '删除失败') }
}

// ===== 文件夹操作 =====
async function createFolderInline(parent) {
  try {
    const { value } = await ElMessageBox.prompt(
      parent ? `在「${parent.name}」下新建子文件夹` : '输入根文件夹名称',
      '新建文件夹',
      { inputValidator: v => v?.trim() ? true : '名称不能为空' }
    )
    const res = await request.post('/folders/add', { name: value.trim(), parentId: parent?.id || null, sortOrder: 0 })
    if (res.code !== '200') throw new Error(res.msg || '创建失败')
    ElMessage.success('文件夹已创建')
    await loadFolders()
    await loadDashboard()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message || '创建失败') }
}
async function renameFolderRow(folder) {
  try {
    const { value } = await ElMessageBox.prompt('修改文件夹名称', '重命名', { inputValue: folder.name, inputValidator: v => v?.trim() ? true : '名称不能为空' })
    const res = await request.put('/folders/update', { id: folder.id, name: value.trim() })
    if (res.code !== '200') throw new Error(res.msg || '重命名失败')
    ElMessage.success('已更新')
    await loadFolders()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message || '更新失败') }
}
async function deleteFolderRow(folder) {
  try {
    await ElMessageBox.confirm('删除文件夹不会删除收藏。确定继续吗？', '删除确认', { type: 'warning' })
    const res = await request.delete(`/folders/delete/${folder.id}`)
    if (res.code !== '200') throw new Error(res.msg || '删除失败')
    ElMessage.success('已删除')
    await Promise.all([loadFolders(), loadRelations(), loadDashboard()])
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message || '删除失败') }
}

// ===== 归类关系操作 =====
async function addRelationInline() {
  addingRelation.value = true
  try {
    if (!flatFolders.value.length) { ElMessage.warning('请先创建文件夹'); return }
    if (!memoryLookup.value || !Object.keys(memoryLookup.value).length) { ElMessage.warning('请先创建收藏'); return }
    // 简单收集：让用户先选 memoryId，再选 folderId
    let memoryId
    try {
      const { value } = await ElMessageBox.prompt('输入收藏 ID（数字）', '新增归类', { inputValidator: v => /^\d+$/.test(String(v || '').trim()) || '请输入数字 ID' })
      memoryId = Number(value)
      if (!memoryLookup.value[String(memoryId)]) { ElMessage.warning('该收藏不存在'); return }
    } catch { return }
    let folderId
    try {
      const { value } = await ElMessageBox.prompt('输入文件夹 ID（数字）', '选择文件夹', { inputValidator: v => /^\d+$/.test(String(v || '').trim()) || '请输入数字 ID' })
      folderId = Number(value)
      if (!flatFolders.value.some(f => String(f.id) === String(folderId))) { ElMessage.warning('该文件夹不存在'); return }
    } catch { return }
    // 调用 put：覆盖式
    const existing = relations.value.filter(r => String(r.memoryId) === String(memoryId)).map(r => String(r.folderId))
    if (existing.includes(String(folderId))) { ElMessage.info('该收藏已在此文件夹中'); return }
    const ids = [...new Set([...existing, String(folderId)])]
    const res = await request.put(`/memoryFolders/memory/${memoryId}`, ids)
    if (res.code !== '200') throw new Error(res.msg || '保存失败')
    ElMessage.success('已新增归类')
    await loadRelations()
    await loadDashboard()
  } catch (e) { if (e?.message) ElMessage.error(e.message || '操作失败') }
  finally { addingRelation.value = false }
}
async function deleteRelation(row) {
  try {
    await ElMessageBox.confirm('确定解除这条收藏与文件夹的关联吗？', '操作确认', { type: 'warning' })
    const res = await request.delete(`/memoryFolders/delete/${row.id}`)
    if (res.code !== '200') throw new Error(res.msg || '操作失败')
    ElMessage.success('已解除关联')
    await loadRelations()
    await loadDashboard()
  } catch (e) { if (e !== 'cancel' && e?.message) ElMessage.error(e.message || '操作失败') }
}

onMounted(refreshAll)
</script>

<style scoped>
.overview-page {
  --pixel-ink: #263d2e;
  --pixel-deep: #315b45;
  --pixel-green: #6f9c72;
  --pixel-paper: #f3edcf;
  --pixel-gold: #f0ca58;
  --pixel-shadow: #294a38;
  min-height: calc(100vh - 60px);
  padding: 0 0 24px;
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
.overview-page * { box-sizing: border-box; }

.toolbar {
  min-height: 78px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 26px;
  background: var(--pixel-deep);
  color: var(--pixel-paper);
  border: 4px solid var(--pixel-ink);
  border-bottom: 5px solid var(--pixel-shadow);
  box-shadow: inset 0 0 0 3px #9cbd87;
}
.brand {
  display: flex; align-items: center; gap: 10px;
  font-size: 21px; font-weight: 700;
  margin-right: auto;
  color: #fff4c8;
  text-shadow: 2px 2px 0 #243d2d;
}
.brand-icon {
  display: grid; place-items: center;
  width: 38px; height: 38px;
  border-radius: 0;
  background: var(--pixel-gold);
  color: var(--pixel-ink);
  border: 3px solid var(--pixel-ink);
  box-shadow: 3px 3px 0 #203a2c;
  font-size: 18px;
}
.toolbar-actions { display: flex; gap: 8px; }

/* 统计卡 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  padding: 16px 20px 0;
}
.stat-card {
  display: flex; align-items: center; gap: 14px;
  padding: 18px;
  background: #f3edcf;
  border: 3px solid var(--pixel-ink);
  border-radius: 0;
  box-shadow: 3px 3px 0 #a8b78e, inset 0 0 0 2px #fffbe7;
}
.stat-card span { display: block; color: #607858; font-size: 13px; }
.stat-card strong { display: block; margin-top: 4px; font-size: 24px; color: #263d2e; }
.stat-icon {
  display: grid; place-items: center;
  width: 42px; height: 42px;
  border-radius: 0;
  border: 2px solid #294332;
  box-shadow: 2px 2px 0 #294332;
  font-size: 20px; font-weight: 700;
  flex-shrink: 0;
}
.stat-icon.green  { background: #cfe5cf; color: #2f7a3a; }
.stat-icon.gold   { background: #f6d978; color: #6e5224; }
.stat-icon.purple { background: #e2d6f3; color: #5a3e95; }
.stat-icon.orange { background: #f8d2a0; color: #a04a18; }
.stat-icon.blue   { background: #cfe0f5; color: #2b5d92; }

/* Tabs */
.overview-tabs {
  margin: 16px 20px 0;
  background: #f3edcf;
  border: 3px solid var(--pixel-ink);
  border-radius: 0;
  box-shadow: 3px 3px 0 rgba(38,61,46,.45), inset 0 0 0 2px #fffbe7;
  padding: 4px 4px 18px;
}
.overview-tabs :deep(.el-tabs__header) {
  margin: 0;
  background: #527c57;
  border-bottom: 3px solid #294332;
}
.overview-tabs :deep(.el-tabs__nav-wrap::after) { height: 0; }
.overview-tabs :deep(.el-tabs__item) {
  height: 50px; line-height: 50px;
  font-family: inherit; font-weight: 700;
  color: #dce6c7;
  padding: 0 22px;
  border: 0;
}
.overview-tabs :deep(.el-tabs__item.is-active) {
  color: #fff4c8;
  background: #294332;
  text-shadow: 1px 1px 0 #000;
}
.overview-tabs :deep(.el-tabs__active-bar) { display: none; }
.overview-tabs :deep(.el-tabs__content) { padding: 16px 4px 0; }

/* 仪表盘 */
.dashboard-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 14px;
}
.panel {
  padding: 20px;
  background: #f3edcf;
  border: 3px solid #91a77c;
  border-radius: 0;
  box-shadow: 2px 2px 0 #a8b78e, inset 0 0 0 2px #fffbe7;
}
.panel-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.panel-title h2 { margin: 0; font-size: 16px; color: #263d2e; }
.recent-item { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 2px dotted #b6c19a; }
.recent-item:last-child { border-bottom: 0; }
.recent-type {
  width: 46px; padding: 5px 4px;
  border: 2px solid #294332;
  border-radius: 0;
  text-align: center; color: #263d2e;
  background: #f6d978;
  font-size: 12px; font-weight: 700;
  box-shadow: 2px 2px 0 #294332;
  flex-shrink: 0;
}
.recent-main { flex: 1; min-width: 0; }
.recent-main strong, .recent-main span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.recent-main strong { color: #263d2e; }
.recent-main span { margin-top: 4px; color: #607858; font-size: 12px; }
.recent-item time { color: #6e8064; font-size: 11px; }
.quick-list { display: grid; gap: 10px; }
.quick-list button {
  padding: 14px;
  border: 3px solid #91a77c;
  border-radius: 0;
  background: #f8f2d9;
  color: #263d2e;
  text-align: left;
  cursor: pointer;
  font-size: 14px; font-weight: 700;
  font-family: inherit;
  box-shadow: 2px 2px 0 #a8b78e, inset 0 0 0 2px #fffbe7;
  transition: transform .12s, box-shadow .12s;
}
.quick-list button:hover {
  transform: translate(-1px, -1px);
  border-color: #4c7650;
  box-shadow: 3px 3px 0 #78966d, inset 0 0 0 2px #fffbe7;
}
.quick-list button div { margin-top: 5px; color: #607858; font-size: 12px; font-weight: 400; }
.tip {
  display: flex; gap: 15px;
  margin-top: 14px;
  padding: 14px 16px;
  border: 2px dashed #7fa174;
  border-radius: 0;
  background: #e8edcf;
  color: #52664c;
  font-size: 13px;
}
.tip strong { color: #36563b; white-space: nowrap; }

/* 表格页通用 */
.page-card {
  background: #f3edcf;
  border: 3px solid #91a77c;
  border-radius: 0;
  padding: 16px;
  box-shadow: 2px 2px 0 #a8b78e, inset 0 0 0 2px #fffbe7;
}
.page-toolbar { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 14px; }
.page-toolbar .el-input { max-width: 320px; }

/* Element 覆盖（仅作用于总览页内） */
.overview-page :deep(.el-input__wrapper),
.overview-page :deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: 0;
  background: #f7f0d5;
  box-shadow: inset 0 0 0 2px #a3b88a, 0 0 0 2px #263d2e !important;
}
.overview-page :deep(.el-input__inner),
.overview-page :deep(.el-select__placeholder) { color: #52634a; }
.overview-page :deep(.el-button) {
  border-radius: 0;
  font-family: inherit;
  font-weight: 700;
  border: 2px solid #294332;
  box-shadow: 2px 2px 0 #294332;
  transition: transform .08s, box-shadow .08s;
}
.overview-page :deep(.el-button:active) { transform: translate(2px, 2px); box-shadow: none; }
.overview-page :deep(.el-button--primary) {
  --el-button-bg-color: #eac85e;
  --el-button-border-color: #294332;
  --el-button-text-color: #263d2e;
  --el-button-hover-bg-color: #f6d978;
  --el-button-hover-border-color: #294332;
  --el-button-hover-text-color: #263d2e;
}
.overview-page :deep(.el-button--success) {
  --el-button-bg-color: #7fb583;
  --el-button-border-color: #294332;
  --el-button-text-color: #1d3a25;
  --el-button-hover-bg-color: #94c597;
  --el-button-hover-border-color: #294332;
  --el-button-hover-text-color: #1d3a25;
}
.overview-page :deep(.el-button:not(.el-button--primary):not(.el-button--success):not(.is-link)) {
  background: #e8e5c8; color: #2b4936;
}
.overview-page :deep(.el-button.is-link) { border: 0; box-shadow: none; color: #2a68cb; }
.overview-page :deep(.el-table) {
  --el-table-border-color: #b6c19a;
  --el-table-header-bg-color: #527c57;
  --el-table-header-text-color: #fff4c8;
  --el-table-row-hover-bg-color: #f4efcf;
  --el-table-bg-color: #fbf6df;
  border: 2px solid #294332;
  border-radius: 0;
  font-family: inherit;
}
.overview-page :deep(.el-table th.el-table__cell) {
  background: #527c57;
  color: #fff4c8;
  font-weight: 700;
  border-bottom: 2px solid #294332;
}
.overview-page :deep(.el-table td.el-table__cell) { border-bottom: 1px dashed #b6c19a; }
.overview-page :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: #f4efcf;
}
.overview-page :deep(.el-tag) { border-radius: 0; font-weight: 700; }
.overview-page :deep(.el-pagination) { --el-pagination-bg-color: transparent; }
.overview-page :deep(.el-dialog) {
  border: 4px solid #294332;
  border-radius: 0;
  background: #f3edcf;
  box-shadow: 6px 6px 0 rgba(38,61,46,.45);
}
.overview-page :deep(.el-dialog__header) {
  margin-right: 0;
  padding: 18px 20px;
  background: #527c57;
  border-bottom: 3px solid #294332;
}
.overview-page :deep(.el-dialog__title) { color: #fff4cf; font-weight: 700; }
.overview-page :deep(.el-dialog__body) { color: #304b36; }
.overview-page :deep(.el-empty__description p) { color: #728365; }

@media (max-width: 1100px) {
  .stats-grid { grid-template-columns: repeat(3, 1fr); }
  .dashboard-grid { grid-template-columns: 1fr; }
}
@media (max-width: 700px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
