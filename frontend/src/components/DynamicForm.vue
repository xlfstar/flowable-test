<template>
  <el-form
    ref="formRef"
    :model="modelValue"
    :rules="rules"
    :label-width="labelWidth"
    :disabled="disabled"
    label-position="right"
  >
    <el-row :gutter="16">
      <el-col
        v-for="field in visibleFields"
        :key="field.key"
        :span="field.type === 'section' ? 24 : field.span === 2 ? 24 : 12"
      >
        <el-divider v-if="field.type === 'section'" content-position="left">
          {{ field.label }}
        </el-divider>
        <el-form-item
          v-else
          :label="field.label"
          :prop="field.key"
          :required="field.required"
        >
          <el-input
            v-if="field.type === 'input'"
            :model-value="asText(getVal(field))"
            :type="field.inputType === 'password' ? 'password' : 'text'"
            :show-password="field.inputType === 'password'"
            :placeholder="field.placeholder"
            clearable
            @update:model-value="(v: string) => setVal(field, v)"
          />
          <el-input
            v-else-if="field.type === 'textarea'"
            :model-value="asText(getVal(field))"
            :placeholder="field.placeholder"
            type="textarea"
            :rows="3"
            @update:model-value="(v: string) => setVal(field, v)"
          />
          <el-input-number
            v-else-if="field.type === 'number'"
            :model-value="asNumber(getVal(field))"
            :min="field.min"
            :max="field.max"
            :precision="field.precision"
            style="width: 100%"
            @update:model-value="(v: number | undefined) => setVal(field, v)"
          />
          <el-select
            v-else-if="field.type === 'select'"
            :model-value="asScalar(getVal(field))"
            :placeholder="field.placeholder || '请选择'"
            clearable
            style="width: 100%"
            @update:model-value="(v: unknown) => setVal(field, v)"
          >
            <el-option
              v-for="opt in field.options || []"
              :key="String(opt.value)"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-else-if="field.type === 'user'"
            :model-value="asUserValue(getVal(field))"
            filterable
            :placeholder="field.placeholder || '请选择用户'"
            style="width: 100%"
            @update:model-value="(v: unknown) => setVal(field, v)"
          >
            <el-option
              v-for="user in userOptions(field)"
              :key="user.id"
              :label="userOptionLabel(user)"
              :value="String(user.id)"
            />
          </el-select>
          <el-radio-group
            v-else-if="field.type === 'radio'"
            :model-value="asScalar(getVal(field))"
            @update:model-value="(v: unknown) => setVal(field, v)"
          >
            <el-radio v-for="opt in field.options || []" :key="String(opt.value)" :value="opt.value">
              {{ opt.label }}
            </el-radio>
          </el-radio-group>
          <el-checkbox-group
            v-else-if="field.type === 'checkbox'"
            :model-value="asArray(getVal(field))"
            @update:model-value="(v: unknown[]) => setVal(field, v)"
          >
            <el-checkbox v-for="opt in field.options || []" :key="String(opt.value)" :value="opt.value">
              {{ opt.label }}
            </el-checkbox>
          </el-checkbox-group>
          <div v-else-if="field.type === 'image' || field.type === 'file'" class="upload-field">
            <el-upload
              :show-file-list="false"
              :http-request="(options: UploadRequestOptions) => uploadFieldFile(field, options)"
              :before-upload="(file: UploadRawFile) => beforeUploadFile(field, file)"
              :accept="field.accept"
              :multiple="field.multiple"
            >
              <el-button type="primary" plain>
                {{ field.type === 'image' ? '上传图片' : '上传文件' }}
              </el-button>
            </el-upload>
            <div v-if="fileValues(field).length" class="upload-list">
              <div v-for="file in fileValues(field)" :key="file.id" class="upload-item">
                <el-image
                  v-if="field.type === 'image' && isImage(file)"
                  :src="file.url"
                  :preview-src-list="[file.url]"
                  fit="cover"
                  class="upload-thumb"
                />
                <el-link :href="file.url" target="_blank" type="primary">{{ file.name }}</el-link>
                <el-button link type="danger" @click="removeFieldFile(field, file.id)">删除</el-button>
              </div>
            </div>
          </div>
          <el-date-picker
            v-else-if="field.type === 'date'"
            :model-value="asText(getVal(field))"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="field.placeholder || '请选择日期'"
            style="width: 100%"
            @update:model-value="(v: string) => setVal(field, v)"
          />
          <el-date-picker
            v-else-if="field.type === 'datetime'"
            :model-value="asText(getVal(field))"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :placeholder="field.placeholder || '请选择时间'"
            style="width: 100%"
            @update:model-value="(v: string) => setVal(field, v)"
          />
          <el-switch
            v-else-if="field.type === 'switch'"
            :model-value="Boolean(getVal(field))"
            @update:model-value="(v: string | number | boolean) => setVal(field, v)"
          />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  FormInstance,
  FormItemRule,
  FormRules,
  UploadRawFile,
  UploadRequestOptions
} from 'element-plus'
import { getOrgUsers } from '@/api/org'
import { uploadFile } from '@/api/file'
import type { FormFieldCondition, FormFieldSchema, OrgUserInfo, UploadedFile } from '@/types'

const props = withDefaults(
  defineProps<{
    fields: FormFieldSchema[]
    modelValue: Record<string, unknown>
    disabled?: boolean
    labelWidth?: string
  }>(),
  {
    disabled: false,
    labelWidth: '110px'
  }
)

const emit = defineEmits<{ (e: 'update:modelValue', value: Record<string, unknown>): void }>()

const formRef = ref<FormInstance>()
const orgUsers = ref<OrgUserInfo[]>([])

const visibleFields = computed(() => props.fields.filter(isFieldVisible))

const rules = computed<FormRules>(() => {
  const result: FormRules = {}
  for (const field of visibleFields.value) {
    if (!field.required || field.type === 'section') {
      continue
    }
    const trigger = field.type === 'input' || field.type === 'textarea' ? 'blur' : 'change'
    const rule: FormItemRule = {
      required: true,
      message: `${field.label}不能为空`,
      trigger
    }
    result[field.key] = [rule]
  }
  return result
})

function isFieldVisible(field: FormFieldSchema): boolean {
  if (!field.visibleWhen?.length) {
    return true
  }
  return field.visibleWhen.every((condition) =>
    matchesCondition(condition, props.modelValue[condition.field])
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

function getVal(field: FormFieldSchema): unknown {
  const value = props.modelValue[field.key]
  return value === undefined ? field.defaultValue : value
}

function setVal(field: FormFieldSchema, value: unknown): void {
  emit('update:modelValue', { ...props.modelValue, [field.key]: value })
}

function userOptions(field: FormFieldSchema): OrgUserInfo[] {
  return orgUsers.value.filter((user) => {
    if (user.status !== undefined && user.status !== 1) {
      return false
    }
    if (field.role && user.role !== field.role) {
      return false
    }
    return true
  })
}

function userOptionLabel(user: OrgUserInfo): string {
  const name = user.realName || user.username
  const dept = user.deptName ? ` · ${user.deptName}` : ''
  return `${name} (${user.username})${dept}`
}

function fileValues(field: FormFieldSchema): UploadedFile[] {
  const value = getVal(field)
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

function isImage(file: UploadedFile): boolean {
  if (file.contentType?.startsWith('image/')) {
    return true
  }
  return /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(file.name)
}

function beforeUploadFile(field: FormFieldSchema, rawFile: UploadRawFile): boolean {
  const maxCount = field.maxCount || (field.multiple ? 5 : 1)
  if (fileValues(field).length >= maxCount) {
    ElMessage.warning(`最多上传 ${maxCount} 个文件`)
    return false
  }
  if (rawFile.size > 20 * 1024 * 1024) {
    ElMessage.warning('文件不能超过 20MB')
    return false
  }
  return true
}

async function uploadFieldFile(field: FormFieldSchema, options: UploadRequestOptions): Promise<void> {
  try {
    const res = await uploadFile(options.file)
    setVal(field, [...fileValues(field), res.data])
  } catch {
    ElMessage.error('上传失败')
  }
}

function removeFieldFile(field: FormFieldSchema, id: number): void {
  setVal(
    field,
    fileValues(field).filter((file) => file.id !== id)
  )
}

function asText(value: unknown): string {
  return value === undefined || value === null ? '' : String(value)
}

function asNumber(value: unknown): number | undefined {
  if (value === undefined || value === null || value === '') return undefined
  const num = Number(value)
  return Number.isNaN(num) ? undefined : num
}

function asScalar(value: unknown): string | number | boolean | undefined {
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return value
  }
  return undefined
}

function asUserValue(value: unknown): string | undefined {
  if (value === undefined || value === null || value === '') return undefined
  return String(value)
}

function asArray(value: unknown): (string | number | boolean)[] {
  return Array.isArray(value) ? (value as (string | number | boolean)[]) : []
}

async function validate(): Promise<boolean> {
  if (!formRef.value) return true
  return formRef.value.validate().then(
    () => true,
    () => false
  )
}

onMounted(async () => {
  try {
    const res = await getOrgUsers()
    orgUsers.value = res.data
  } catch {
    orgUsers.value = []
  }
})

defineExpose({ validate })
</script>

<style scoped lang="scss">
.upload-field {
  width: 100%;
}

.upload-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.upload-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-thumb {
  width: 64px;
  height: 64px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
</style>
