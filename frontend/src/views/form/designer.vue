<template>
  <div class="designer-page">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button :icon="Back" @click="goBack">返回</el-button>
          <span class="form-title">{{ meta.name || '未命名表单' }}</span>
          <el-tag size="small" type="info" effect="plain">{{ meta.formKey }}</el-tag>
        </div>
        <div class="toolbar-right">
          <el-button :icon="View" @click="openPreview">预览</el-button>
          <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存</el-button>
        </div>
      </div>
    </el-card>

    <div class="designer-body" v-loading="loading">
      <el-card shadow="never" class="palette">
        <div class="panel-title">字段类型</div>
        <div class="palette-list">
          <div
            v-for="item in FIELD_TYPES"
            :key="item.type"
            class="palette-item"
            @click="addField(item.type)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="canvas">
        <div class="panel-title">表单字段 (点击选中, 可拖动排序)</div>
        <el-empty v-if="!fields.length" description="点击左侧字段类型添加到表单" />
        <div
          v-for="(field, index) in fields"
          :key="field.key"
          class="field-card"
          :class="{ active: index === selectedIndex }"
          draggable="true"
          @click="selectedIndex = index"
          @dragstart="dragIndex = index"
          @dragover.prevent
          @drop="onDrop(index)"
        >
          <div class="field-card-main">
            <span class="field-type">{{ typeLabel(field.type) }}</span>
            <span class="field-label">{{ field.label }}</span>
            <span class="field-key">{{ field.key }}</span>
            <el-tag v-if="field.required" size="small" type="danger" effect="plain">必填</el-tag>
          </div>
          <div class="field-card-ops" @click.stop>
            <el-button link :icon="ArrowUp" :disabled="index === 0" @click="move(index, -1)" />
            <el-button link :icon="ArrowDown" :disabled="index === fields.length - 1" @click="move(index, 1)" />
            <el-button link type="danger" :icon="Delete" @click="removeField(index)" />
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="props">
        <div class="panel-title">字段属性</div>
        <el-empty v-if="!selected" description="请选择字段" />
        <el-form v-else label-width="80px" size="small" class="prop-form">
          <el-form-item label="字段名称">
            <el-input v-model="selected.label" />
          </el-form-item>
          <el-form-item label="字段标识">
            <el-input v-model="selected.key" placeholder="英文标识, 对应流程变量名" />
          </el-form-item>
          <el-form-item label="字段类型">
            <el-select v-model="selected.type" style="width: 100%" @change="onTypeChange">
              <el-option v-for="item in FIELD_TYPES" :key="item.type" :label="item.label" :value="item.type" />
            </el-select>
          </el-form-item>
          <el-form-item label="占位提示">
            <el-input v-model="selected.placeholder" />
          </el-form-item>
          <el-form-item label="默认值">
            <el-input v-model="defaultValue" placeholder="默认值(可选)" />
          </el-form-item>
          <el-form-item label="栅格">
            <el-radio-group v-model="selected.span">
              <el-radio :value="1">半行</el-radio>
              <el-radio :value="2">整行</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="必填">
            <el-switch v-model="selected.required" />
          </el-form-item>

          <el-form-item v-if="selected.type === 'user'" label="角色过滤">
            <el-input v-model="selected.role" placeholder="如 consultant，留空显示全部用户" />
          </el-form-item>

          <template v-if="selected.type === 'file' || selected.type === 'image'">
            <el-form-item label="允许类型">
              <el-input v-model="selected.accept" placeholder="如 image/* 或 .pdf,.docx" />
            </el-form-item>
            <el-form-item label="允许多个">
              <el-switch v-model="selected.multiple" />
            </el-form-item>
            <el-form-item label="最大数量">
              <el-input-number v-model="selected.maxCount" :min="1" :max="10" style="width: 100%" />
            </el-form-item>
          </template>

          <template v-if="selected.type === 'number'">
            <el-form-item label="最小值">
              <el-input-number v-model="selected.min" style="width: 100%" />
            </el-form-item>
            <el-form-item label="最大值">
              <el-input-number v-model="selected.max" style="width: 100%" />
            </el-form-item>
            <el-form-item label="精度">
              <el-input-number v-model="selected.precision" :min="0" :max="6" style="width: 100%" />
            </el-form-item>
          </template>

          <template v-if="hasOptions(selected.type)">
            <el-form-item label="值类型">
              <el-select v-model="selected.valueType" style="width: 100%">
                <el-option label="字符串" value="string" />
                <el-option label="数字" value="number" />
                <el-option label="布尔" value="boolean" />
              </el-select>
            </el-form-item>
            <el-form-item label="选项">
              <div class="option-list">
                <div v-for="(opt, i) in selected.options" :key="i" class="option-row">
                  <el-input v-model="opt.label" placeholder="显示名" />
                  <el-input
                    :model-value="String(opt.value)"
                    placeholder="值"
                    @update:model-value="(v: string) => (opt.value = v)"
                  />
                  <el-button link type="danger" :icon="Delete" @click="removeOption(i)" />
                </div>
                <el-button link type="primary" :icon="Plus" @click="addOption">添加选项</el-button>
              </div>
            </el-form-item>
          </template>
        </el-form>
      </el-card>
    </div>

    <el-dialog v-model="previewVisible" title="表单预览" width="720px">
      <DynamicForm v-if="previewVisible" :fields="fields" :model-value="previewValues" @update:model-value="previewValues = $event" />
      <div class="preview-json">
        <el-collapse>
          <el-collapse-item title="查看提交数据结构">
            <pre>{{ JSON.stringify(previewValues, null, 2) }}</pre>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown,
  ArrowUp,
  Back,
  Check,
  Delete,
  Plus,
  View
} from '@element-plus/icons-vue'
import DynamicForm from '@/components/DynamicForm.vue'
import { getForm, updateForm } from '@/api/form'
import type { FormDefinition, FormFieldSchema, FormFieldType } from '@/types'

const FIELD_TYPES: { type: FormFieldType; label: string; icon: string }[] = [
  { type: 'input', label: '单行文本', icon: 'EditPen' },
  { type: 'textarea', label: '多行文本', icon: 'Document' },
  { type: 'number', label: '数字', icon: 'Finished' },
  { type: 'select', label: '下拉选择', icon: 'Select' },
  { type: 'radio', label: '单选', icon: 'Finished' },
  { type: 'checkbox', label: '多选', icon: 'List' },
  { type: 'date', label: '日期', icon: 'Timer' },
  { type: 'datetime', label: '日期时间', icon: 'Timer' },
  { type: 'switch', label: '开关', icon: 'Switch' },
  { type: 'section', label: '分组标题', icon: 'Files' },
  { type: 'user', label: '组织用户', icon: 'User' },
  { type: 'image', label: '图片上传', icon: 'Picture' },
  { type: 'file', label: '文件上传', icon: 'UploadFilled' }
]

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const formId = Number(route.params.id)

const meta = reactive<FormDefinition>({ formKey: '', name: '', category: '', schemaJson: '[]', remark: '', status: 1 })
const fields = ref<FormFieldSchema[]>([])
const selectedIndex = ref(-1)
const dragIndex = ref(-1)

const previewVisible = ref(false)
const previewValues = ref<Record<string, unknown>>({})

const selected = computed(() => fields.value[selectedIndex.value] ?? null)

const defaultValue = computed({
  get: () => {
    const value = selected.value?.defaultValue
    return value === undefined || value === null ? '' : String(value)
  },
  set: (value: string) => {
    if (selected.value) {
      selected.value.defaultValue = value === '' ? undefined : value
    }
  }
})

function typeLabel(type: FormFieldType): string {
  return FIELD_TYPES.find((item) => item.type === type)?.label || type
}

function hasOptions(type: FormFieldType): boolean {
  return type === 'select' || type === 'radio' || type === 'checkbox'
}

function nextKey(): string {
  let n = fields.value.length + 1
  const exists = (k: string) => fields.value.some((f) => f.key === k)
  while (exists(`field_${n}`)) n += 1
  return `field_${n}`
}

function addField(type: FormFieldType) {
  const field: FormFieldSchema = {
    key: nextKey(),
    label: type === 'section' ? '分组标题' : typeLabel(type),
    type,
    span: type === 'textarea' || type === 'section' ? 2 : 1,
    required: false
  }
  if (hasOptions(type)) {
    field.options = [
      { label: '选项一', value: '1' },
      { label: '选项二', value: '2' }
    ]
    field.valueType = 'string'
  }
  if (type === 'image') {
    field.accept = 'image/*'
    field.multiple = false
    field.maxCount = 1
  }
  if (type === 'file') {
    field.accept = '.pdf,.doc,.docx,.xls,.xlsx,.zip,.rar,.txt'
    field.multiple = false
    field.maxCount = 1
  }
  fields.value.push(field)
  selectedIndex.value = fields.value.length - 1
}

function removeField(index: number) {
  fields.value.splice(index, 1)
  if (selectedIndex.value === index) selectedIndex.value = -1
  else if (selectedIndex.value > index) selectedIndex.value -= 1
}

function move(index: number, offset: number) {
  const target = index + offset
  const list = fields.value
  const [item] = list.splice(index, 1)
  list.splice(target, 0, item)
  selectedIndex.value = target
}

function onDrop(index: number) {
  if (dragIndex.value === -1 || dragIndex.value === index) return
  const [item] = fields.value.splice(dragIndex.value, 1)
  fields.value.splice(index, 0, item)
  selectedIndex.value = index
  dragIndex.value = -1
}

function onTypeChange() {
  const field = selected.value
  if (!field) return
  if (hasOptions(field.type)) {
    if (!field.options) {
      field.options = [
        { label: '选项一', value: '1' },
        { label: '选项二', value: '2' }
      ]
    }
    if (!field.valueType) field.valueType = 'string'
  } else {
    delete field.options
    delete field.valueType
  }
  if (field.type === 'section') {
    field.span = 2
    field.required = false
  }
  if (field.type !== 'image' && field.type !== 'file') {
    delete field.accept
    delete field.multiple
    delete field.maxCount
  }
  if (field.type === 'image') {
    field.accept = field.accept || 'image/*'
    field.multiple = field.multiple ?? false
    field.maxCount = field.maxCount ?? 1
  }
  if (field.type === 'file') {
    field.accept = field.accept || '.pdf,.doc,.docx,.xls,.xlsx,.zip,.rar,.txt'
    field.multiple = field.multiple ?? false
    field.maxCount = field.maxCount ?? 1
  }
}

function addOption() {
  selected.value?.options?.push({ label: '新选项', value: String((selected.value.options?.length || 0) + 1) })
}

function removeOption(index: number) {
  selected.value?.options?.splice(index, 1)
}

function buildPreviewValues(): Record<string, unknown> {
  const values: Record<string, unknown> = {}
  for (const field of fields.value) {
    if (field.type === 'section') {
      continue
    }
    if (field.type === 'file' || field.type === 'image') {
      values[field.key] = []
      continue
    }
    values[field.key] = field.defaultValue ?? (field.type === 'switch' ? false : '')
  }
  return values
}

function openPreview() {
  previewValues.value = buildPreviewValues()
  previewVisible.value = true
}

async function load() {
  if (!formId || Number.isNaN(formId)) {
    ElMessage.warning('缺少表单ID')
    router.replace('/form/list')
    return
  }
  loading.value = true
  try {
    const res = await getForm(formId)
    Object.assign(meta, res.data)
    try {
      const parsed = res.data.schemaJson ? JSON.parse(res.data.schemaJson) : []
      fields.value = Array.isArray(parsed) ? parsed : []
    } catch {
      fields.value = []
    }
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!fields.value.length) {
    ElMessage.warning('请至少添加一个字段')
    return
  }
  const keys = fields.value.map((f) => f.key)
  if (keys.some((k) => !k)) {
    ElMessage.warning('存在字段标识为空的字段')
    return
  }
  if (new Set(keys).size !== keys.length) {
    ElMessage.warning('字段标识不能重复')
    return
  }
  saving.value = true
  try {
    await updateForm(formId, {
      formKey: meta.formKey,
      name: meta.name,
      category: meta.category,
      schemaJson: JSON.stringify(fields.value),
      remark: meta.remark,
      status: meta.status
    })
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/form/list')
}

onMounted(load)
</script>

<style scoped lang="scss">
.designer-page {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar-card :deep(.el-card__body) {
  padding: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-title {
  font-weight: 600;
  font-size: 15px;
}

.designer-body {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.panel-title {
  font-weight: 600;
  margin-bottom: 10px;
  color: #303133;
}

.palette {
  width: 170px;
  flex-shrink: 0;
}

.palette-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
}

.palette-item:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

.canvas {
  flex: 1;
  min-height: 400px;
}

.field-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.field-card:hover {
  border-color: #c6e2ff;
}

.field-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.field-card-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.field-type {
  color: #909399;
  font-size: 12px;
  border: 1px solid #ebeef5;
  border-radius: 3px;
  padding: 1px 6px;
  flex-shrink: 0;
}

.field-label {
  font-weight: 500;
}

.field-key {
  color: #909399;
  font-size: 12px;
}

.props {
  width: 320px;
  flex-shrink: 0;
}

.prop-form {
  margin-top: 4px;
}

.option-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.preview-json {
  margin-top: 12px;
}

.preview-json pre {
  margin: 0;
  font-size: 12px;
  color: #606266;
}
</style>
