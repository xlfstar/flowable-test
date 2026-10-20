<template>
  <div class="form-data-view">
    <template v-for="(item, index) in displayItems" :key="index">
      <el-divider v-if="item.kind === 'section'" content-position="left">
        {{ item.label }}
      </el-divider>
      <div v-else class="form-data-row">
        <div class="form-data-label">{{ item.label }}</div>
        <div class="form-data-value">
          <template v-if="item.files.length">
            <div v-for="file in item.files" :key="file.id" class="form-data-file">
              <el-image
                v-if="isImageFile(file)"
                :src="file.url"
                :preview-src-list="[file.url]"
                fit="cover"
                class="form-data-image"
              />
              <el-link :href="file.url" target="_blank" type="primary">{{ file.name }}</el-link>
            </div>
          </template>
          <span v-else>{{ item.text || '-' }}</span>
        </div>
      </div>
    </template>
    <el-empty v-if="!displayItems.length" description="暂无表单数据" :image-size="60" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getOrgUsers } from '@/api/org'
import type {
  FormFieldCondition,
  FormFieldSchema,
  OrgUserInfo,
  UploadedFile
} from '@/types'

const props = defineProps<{
  fields: FormFieldSchema[]
  values: Record<string, unknown>
}>()

const orgUsers = ref<OrgUserInfo[]>([])

const textMap: Record<string, string> = {
  true: '是',
  false: '否',
  pass: '通过',
  reject: '退回',
  registered: '备案地址',
  actual: '实际地址',
  electronic: '电子版账本',
  backup: '账套备份或迁移码'
}

const sensitiveKeys = new Set(['etaxPassword', 'individualTaxPassword'])

interface DisplayItem {
  kind: 'section' | 'field'
  label: string
  field?: FormFieldSchema
  text?: string
  files: UploadedFile[]
}

const displayItems = computed<DisplayItem[]>(() => {
  const items: DisplayItem[] = []
  for (const field of props.fields) {
    if (field.type === 'section') {
      items.push({ kind: 'section', label: field.label, files: [] })
      continue
    }
    if (!isFieldVisible(field)) {
      continue
    }
    const value = props.values[field.key]
    if (!hasValue(value)) {
      continue
    }
    items.push({
      kind: 'field',
      label: field.label,
      field,
      text: displayText(field, value),
      files: extractFiles(value)
    })
  }

  const groups: { section: DisplayItem | null; fields: DisplayItem[] }[] = []
  let current: { section: DisplayItem | null; fields: DisplayItem[] } = {
    section: null,
    fields: []
  }
  for (const item of items) {
    if (item.kind === 'section') {
      if (current.section || current.fields.length) {
        groups.push(current)
      }
      current = { section: item, fields: [] }
    } else {
      current.fields.push(item)
    }
  }
  if (current.section || current.fields.length) {
    groups.push(current)
  }

  const result: DisplayItem[] = []
  for (const group of groups) {
    if (!group.fields.length) {
      continue
    }
    if (group.section) {
      result.push(group.section)
    }
    result.push(...group.fields)
  }
  return result
})

function isFieldVisible(field: FormFieldSchema): boolean {
  if (!field.visibleWhen?.length) {
    return true
  }
  return field.visibleWhen.every((condition) =>
    matchesCondition(condition, props.values[condition.field])
  )
}

function matchesCondition(condition: FormFieldCondition, actual: unknown): boolean {
  if (condition.in) {
    return condition.in.some((expected) => valuesEqual(actual, expected))
  }
  if (Object.prototype.hasOwnProperty.call(condition, 'notEquals')) {
    return !valuesEqual(actual, condition.notEquals)
  }
  if (Object.prototype.hasOwnProperty.call(condition, 'equals')) {
    return valuesEqual(actual, condition.equals)
  }
  return true
}

function valuesEqual(actual: unknown, expected: unknown): boolean {
  if (typeof expected === 'boolean') {
    return actual === expected || String(actual) === String(expected)
  }
  if (typeof expected === 'number') {
    return Number(actual) === expected
  }
  return String(actual) === String(expected)
}

function hasValue(value: unknown): boolean {
  if (value === undefined || value === null || value === '') {
    return false
  }
  if (Array.isArray(value)) {
    return value.length > 0
  }
  return true
}

function displayText(field: FormFieldSchema, value: unknown): string {
  if (sensitiveKeys.has(field.key)) {
    return '******'
  }
  if (field.type === 'user') {
    const user = orgUsers.value.find((item) => String(item.id) === String(value))
    if (user) {
      return `${user.realName || user.username} (${user.username})`
    }
  }
  if (field.type === 'radio' || field.type === 'select') {
    const option = (field.options || []).find((item) => String(item.value) === String(value))
    if (option) {
      return option.label
    }
  }
  if (field.type === 'checkbox' && Array.isArray(value)) {
    return value
      .map((item) => {
        const option = (field.options || []).find((opt) => String(opt.value) === String(item))
        return option ? option.label : String(item)
      })
      .join('、')
  }
  if (field.type === 'switch') {
    return value ? '是' : '否'
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }
  if (Array.isArray(value)) {
    const files = extractFiles(value)
    if (files.length) {
      return files.map((file) => file.name).join('、')
    }
    return value.map((item) => String(item)).join('、')
  }
  if (value !== null && typeof value === 'object') {
    return JSON.stringify(value)
  }
  const text = String(value)
  return textMap[text] ?? text
}

function extractFiles(value: unknown): UploadedFile[] {
  if (Array.isArray(value)) {
    return value.filter(isUploadedFile)
  }
  if (typeof value === 'string' && value.trim()) {
    try {
      const parsed = JSON.parse(value)
      return Array.isArray(parsed) ? parsed.filter(isUploadedFile) : []
    } catch {
      return []
    }
  }
  return isUploadedFile(value) ? [value] : []
}

function isUploadedFile(value: unknown): value is UploadedFile {
  if (!value || typeof value !== 'object') {
    return false
  }
  const file = value as Record<string, unknown>
  return typeof file.id === 'number' && typeof file.name === 'string' && typeof file.url === 'string'
}

function isImageFile(file: UploadedFile): boolean {
  if (file.contentType?.startsWith('image/')) {
    return true
  }
  return /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(file.name)
}

onMounted(() => {
  getOrgUsers()
    .then((res) => {
      orgUsers.value = res.data
    })
    .catch(() => {
      orgUsers.value = []
    })
})
</script>

<style scoped lang="scss">
.form-data-view {
  padding: 4px 0;
}

.form-data-row {
  display: flex;
  padding: 8px 12px;
  border-bottom: 1px solid #f2f3f5;
}

.form-data-row:last-child {
  border-bottom: none;
}

.form-data-label {
  width: 190px;
  flex-shrink: 0;
  color: #606266;
  font-weight: 500;
}

.form-data-value {
  flex: 1;
  color: #303133;
  word-break: break-word;
}

.form-data-file {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.form-data-image {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
</style>
