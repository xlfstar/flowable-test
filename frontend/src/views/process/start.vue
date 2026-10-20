<template>
  <div class="page-container">
    <el-card shadow="never" class="start-card">
      <template #header>
        <span>发起流程</span>
      </template>

      <el-form label-width="100px" style="max-width: 720px">
        <el-form-item label="选择流程">
          <el-select
            v-model="selectedDefinitionId"
            placeholder="请选择要发起的流程"
            style="width: 100%"
            @change="onDefinitionChange"
          >
            <el-option
              v-for="def in definitions"
              :key="def.id"
              :label="`${def.name} (${def.key} v${def.version})`"
              :value="def.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="业务Key">
          <el-input v-model="businessKey" placeholder="业务标识(可选)" />
        </el-form-item>
      </el-form>

      <template v-if="selectedDefinitionId">
        <div class="section-title">流程表单</div>
        <el-alert
          v-if="formLoading"
          type="info"
          :closable="false"
          show-icon
          title="正在加载表单..."
        />
        <el-alert
          v-else-if="!formFields.length"
          type="warning"
          :closable="false"
          show-icon
          title="该流程未配置启动表单, 可直接发起(不含表单数据)"
        />
        <DynamicForm
          v-else
          ref="formRef"
          :fields="formFields"
          :model-value="formValues"
          @update:model-value="formValues = $event"
        />

        <div class="start-actions">
          <el-button type="primary" :icon="Promotion" :loading="submitting" @click="submit">发起流程</el-button>
        </div>
      </template>
      <el-empty v-else description="请先选择要发起的流程" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import DynamicForm from '@/components/DynamicForm.vue'
import { pageProcessDefinitions, startProcess } from '@/api/process'
import { resolveStartForm } from '@/api/form'
import type { FormFieldSchema, ProcessDefinition } from '@/types'

const router = useRouter()
const definitions = ref<ProcessDefinition[]>([])
const selectedDefinitionId = ref('')
const businessKey = ref('')
const formLoading = ref(false)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof DynamicForm>>()
const formFields = ref<FormFieldSchema[]>([])
const formValues = ref<Record<string, unknown>>({})

function selectedDefinition(): ProcessDefinition | undefined {
  return definitions.value.find((def) => def.id === selectedDefinitionId.value)
}

async function loadDefinitions() {
  const res = await pageProcessDefinitions({ page: 1, size: 100 })
  definitions.value = res.data.records
}

async function onDefinitionChange() {
  formFields.value = []
  formValues.value = {}
  const definition = selectedDefinition()
  if (!definition) return
  formLoading.value = true
  try {
    const res = await resolveStartForm(definition.id)
    const form = res.data
    if (!form?.schemaJson) return
    let parsed: FormFieldSchema[] = []
    try {
      const value = JSON.parse(form.schemaJson)
      parsed = Array.isArray(value) ? value : []
    } catch {
      parsed = []
    }
    formFields.value = parsed
    formValues.value = buildInitialValues(parsed)
  } finally {
    formLoading.value = false
  }
}

function buildInitialValues(fields: FormFieldSchema[]): Record<string, unknown> {
  const values: Record<string, unknown> = {}
  for (const field of fields) {
    if (field.type === 'section') {
      continue
    }
    if (field.defaultValue !== undefined) {
      values[field.key] = field.defaultValue
    } else if (field.type === 'checkbox' || field.type === 'file' || field.type === 'image') {
      values[field.key] = []
    } else if (field.type === 'switch') {
      values[field.key] = false
    } else {
      values[field.key] = ''
    }
  }
  return values
}

function coerceOption(field: FormFieldSchema, value: unknown): unknown {
  if (field.valueType === 'number') return Number(value)
  if (field.valueType === 'boolean') return value === true || value === 'true'
  return value
}

function buildVariables(): Record<string, unknown> {
  const variables: Record<string, unknown> = {}
  for (const field of formFields.value) {
    if (field.type === 'section') {
      continue
    }
    let value = formValues.value[field.key]
    if (value === undefined || value === null) continue
    if (field.type === 'number') value = Number(value)
    else if (field.type === 'switch') value = Boolean(value)
    else if (field.type === 'checkbox') value = (Array.isArray(value) ? value : []).map((v) => coerceOption(field, v))
    else if (field.type === 'select' || field.type === 'radio') value = coerceOption(field, value)
    if (typeof value === 'number' && Number.isNaN(value)) continue
    variables[field.key] = value
  }
  return variables
}

async function submit() {
  const definition = selectedDefinition()
  if (!definition) {
    ElMessage.warning('请选择要发起的流程')
    return
  }
  if (formFields.value.length && formRef.value) {
    const valid = await formRef.value.validate()
    if (!valid) return
  }
  submitting.value = true
  try {
    await startProcess({
      processDefinitionKey: definition.key,
      businessKey: businessKey.value || undefined,
      variables: buildVariables()
    })
    ElMessage.success('发起成功')
    router.push('/process/instance')
  } finally {
    submitting.value = false
  }
}

onMounted(loadDefinitions)
</script>

<style scoped lang="scss">
.start-card {
  max-width: 900px;
}

.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
  color: #303133;
}

.start-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
