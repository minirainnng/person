<template>
  <div class="sop-editor">
    <el-form label-position="top" class="editor-form">
      <el-form-item label="标题" required>
        <el-input v-model="local.title" maxlength="255" show-word-limit placeholder="给这份 SOP 起个清晰的名字" />
      </el-form-item>
      <el-form-item label="正文（支持富文本）">
        <div class="editor-toolbar-extra">
          <span class="hint">💡 在右侧收藏列表 <b>双击</b> 即可插入引用；<b>单击</b> 预览收藏详情</span>
        </div>
        <div class="editor-box">
          <Toolbar
            :editor="editorRef"
            :default-config="toolbarConfig"
            mode="default"
            class="editor-toolbar"
          />
          <Editor
            v-model="local.contentHtml"
            :default-config="editorConfig"
            mode="default"
            class="editor-content"
            @on-created="onCreated"
            @on-blur="onBlur"
          />
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, shallowRef, watch } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const props = defineProps({
  sop: { type: Object, required: true }
})
const editorSopId = ref(props.sop.id || null)
const emit = defineEmits(['saved', 'cancel', 'dirty'])

const local = reactive({
  title: props.sop.title || '',
  contentHtml: props.sop.contentHtml || '',
  sortOrder: props.sop.sortOrder ?? 0
})

const editorRef = shallowRef(null)
const toolbarConfig = { excludeKeys: ['insertVideo'] }
const editorConfig = {
  placeholder: '开始书写你的 SOP…',
  MENU_CONF: {
    uploadImage: {
      customUpload: (file, insertFn) => uploadAndInsert(file, insertFn)
    }
  }
}

function onCreated(editor) {
  editorRef.value = editor
  let booting = true
  setTimeout(() => { booting = false }, 0)
  editor.on('change', () => {
    if (booting) return
    local.contentHtml = editor.getHtml()
    emit('dirty', true)
  })
}
function markClean() { emit('dirty', false) }
function onBlur() { /* no-op */ }

async function uploadAndInsert(file, insertFn) {
  const form = new FormData()
  form.append('file', file)
  try {
    const baseUrl = import.meta.env.VITE_BASE_URL || 'http://localhost:9090'
    const resp = await fetch(`${baseUrl}/files/wang/upload`, { method: 'POST', body: form })
    const raw = await resp.text()
    let res
    try { res = JSON.parse(raw) } catch { throw new Error('后端返回了非 JSON：' + raw.slice(0, 300)) }
    if (res && res.errno === 0 && Array.isArray(res.data) && res.data[0]?.url) {
      insertFn(res.data[0].url, file.name, '')
      ElMessage.success('图片已插入')
    } else {
      throw new Error('后端返回格式不符合 wangEditor 规范：' + raw.slice(0, 300))
    }
  } catch (e) {
    ElMessage.error('图片上传失败：' + e.message)
  }
}

// 新建 SOP 时，先收集插入的引用 id，保存后一次性建关联
const pendingRefIds = ref(new Set())

/** 给父组件调用：把 memory 插入到当前光标处 */
function insertRef(memory) {
  const editor = editorRef.value
  if (!editor) throw new Error('编辑器尚未初始化')
  const title = memory.title || memory.sourceTitle || '未命名收藏'
  const safeTitle = String(title).replace(/[&<>"']/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]))
  const html = `<a href="#memory-${memory.id}" class="sop-memory-ref" data-memory-id="${memory.id}">📎 ${safeTitle}</a>&nbsp;`

  try {
    editor.focus?.()
    const sel = editor.selection
    if (sel && Array.isArray(sel.anchor?.path) && sel.anchor.path.length) {
      editor.dangerouslyInsertHtml(html)
    } else {
      const el = document.querySelector('.editor-content [contenteditable="true"]')
      if (el) { el.focus(); editor.dangerouslyInsertHtmlAtEnd(html) } else { editor.dangerouslyInsertHtmlAtEnd(html) }
    }
  } catch (e) {
    console.error('[insertRef] insert failed, fallback to end', e)
    try { editor.dangerouslyInsertHtmlAtEnd(html) } catch (_) { /* swallow */ }
  }

  local.contentHtml = editor.getHtml()
  emit('dirty', true)

  if (editorSopId.value) {
    persistRef(editorSopId.value, memory.id).catch(() => { /* 失败在保存时还有兜底 */ })
  } else {
    pendingRefIds.value.add(memory.id)
    ElMessage.info('已插入引用块；保存 SOP 后会自动建立关联')
  }
}

async function persistRef(sopId, memoryId) {
  try {
    const res = await request.post(`/sops/${sopId}/refs/${memoryId}`)
    if (res.code === '200') ElMessage.success('已插入引用并建立关联')
  } catch (e) {
    ElMessage.warning('引用块已插入，但关联关系建立失败：' + (e.message || ''))
  }
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) editor.destroy()
})

// ============ 保存 ============
const saving = ref(false)
async function save() {
  if (!local.title.trim()) return ElMessage.warning('请填写标题')
  saving.value = true
  try {
    const editor = editorRef.value
    if (editor) local.contentHtml = editor.getHtml()

    const payload = {
      title: local.title.trim(),
      contentHtml: local.contentHtml,
      sortOrder: local.sortOrder
    }
    let res
    if (props.sop.id) {
      payload.id = props.sop.id
      res = await request.put('/sops/update', payload)
      if (res.code !== '200') throw new Error(res.msg || '更新失败')
      ElMessage.success('已保存')
      markClean()
      emit('saved', { ...props.sop, ...payload })
    } else {
      res = await request.post('/sops/add', payload)
      if (res.code !== '200') throw new Error(res.msg || '新建失败')
      const id = res.data
      editorSopId.value = id
      ElMessage.success('已创建')

      const ids = new Set(extractRefIds(local.contentHtml))
      for (const memoryId of pendingRefIds.value) ids.add(memoryId)
      pendingRefIds.value.clear()
      for (const memoryId of ids) {
        try { await request.post(`/sops/${id}/refs/${memoryId}`) } catch (_) { /* single failure ignored */ }
      }
      markClean()
      emit('saved', { ...payload, id })
    }
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally { saving.value = false }
}

watch(() => local.title, () => emit('dirty', true))

function extractRefIds(html) {
  if (!html) return []
  const ids = new Set()
  const re = /data-memory-id="(\d+)"/g
  let m
  while ((m = re.exec(html)) !== null) ids.add(Number(m[1]))
  return [...ids]
}

defineExpose({ save, markClean, insertRef })
</script>

<style scoped>
.sop-editor { display: flex; flex-direction: column; gap: 12px; width: 100%; }
.editor-form { display: flex; flex-direction: column; }
.editor-toolbar-extra { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.editor-toolbar-extra .hint { color: #6c7a90; font-size: 12px; }

.editor-box { border: 1px solid #dcdfe6; border-radius: 6px; overflow: hidden; background: #fff; }
.editor-toolbar { border-bottom: 1px solid #ebeef5; }
.editor-content { min-height: 360px; max-height: 60vh; overflow-y: auto; }
.editor-content :deep(.w-e-text) { min-height: 360px; }
.editor-content :deep(a.sop-memory-ref) {
  display: inline-block;
  padding: 1px 8px;
  margin: 0 2px;
  background: #eaf2ff;
  color: #2a68cb;
  border: 1px solid #b9d2f3;
  border-radius: 5px;
  text-decoration: none;
  font-size: 13px;
}
</style>
