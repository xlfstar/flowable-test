<template>
  <div class="page-container instance-detail-page">
    <el-page-header
      class="page-header"
      :content="instance?.processDefinitionName || '流程实例详情'"
      @back="router.back()"
    />

    <el-card v-loading="loading" shadow="never" class="info-card">
      <el-descriptions v-if="instance" :column="4" border size="small">
        <el-descriptions-item label="流程名称">{{ instance.processDefinitionName }}</el-descriptions-item>
        <el-descriptions-item label="业务Key">{{ instance.businessKey || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发起人">{{ instance.startUserName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="instance.ended ? 'info' : 'success'" size="small">
            {{ instance.ended ? '已结束' : '运行中' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ instance.startTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ instance.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="流程版本">{{ instance.processDefinitionVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实例ID">{{ instance.id }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="15">
        <el-card shadow="never" class="diagram-card">
          <template #header>
            <div class="card-header">
              <span>流程图</span>
              <span class="hint">绿色为已完成节点，红色为当前节点，点击节点查看表单</span>
            </div>
          </template>
          <div v-loading="loading" class="diagram-box">
            <ProcessViewer
              v-if="xml"
              :xml="xml"
              :active-ids="activeIds"
              :completed-ids="completedIds"
              @element-click="onElementClick"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="9">
        <el-card shadow="never" class="form-card">
          <template #header>
            <div class="card-header">
              <span>{{ selectedForm?.taskName || selectedNode?.name || '节点表单' }}</span>
              <el-tag v-if="selectedForm?.formName" size="small" type="info">
                {{ selectedForm.formName }}
              </el-tag>
            </div>
          </template>

          <el-empty
            v-if="!selectedNode"
            description="点击流程图中的节点查看表单"
            :image-size="80"
          />
          <template v-else>
            <div v-if="selectedOccurrences.length > 1" class="occurrence-select">
              <el-select v-model="selectedOccurrenceIndex" style="width: 100%">
                <el-option
                  v-for="(form, index) in selectedOccurrences"
                  :key="form.taskId || index"
                  :label="`第 ${index + 1} 次 · ${form.assigneeName || '-'} · ${form.endTime || ''}`"
                  :value="index"
                />
              </el-select>
            </div>

            <template v-if="selectedForm">
              <el-descriptions :column="1" size="small" border class="form-meta">
                <el-descriptions-item label="节点">
                  {{ selectedForm.taskName || selectedNode.name }}
                </el-descriptions-item>
                <el-descriptions-item label="办理人">{{ selectedAssignee }}</el-descriptions-item>
                <el-descriptions-item :label="selectedTimeLabel">
                  {{ selectedTime || '-' }}
                </el-descriptions-item>
              </el-descriptions>

              <el-alert
                v-if="isActiveSelected && !canEditSelected"
                type="warning"
                :closable="false"
                show-icon
                :title="activeWarning"
                style="margin-bottom: 12px"
              />

              <template v-if="canEditSelected">
                <div class="form-data-title">填写表单</div>
                <DynamicForm
                  ref="currentFormRef"
                  :fields="currentFormFields"
                  :model-value="currentFormValues"
                  @update:model-value="currentFormValues = $event"
                />
                <template v-if="!formHasComment">
                  <div class="form-data-title">审批意见</div>
                  <el-input
                    v-model="comment"
                    type="textarea"
                    :rows="2"
                    placeholder="请输入审批意见(可选)"
                  />
                </template>
                <div class="current-actions">
                  <el-button type="primary" :loading="submitting" @click="submitCurrentTask">
                    提交
                  </el-button>
                </div>
              </template>
              <template v-else>
                <div class="form-data-title">表单内容</div>
                <FormDataView
                  :fields="parseFields(selectedForm.schemaJson)"
                  :values="selectedForm.values"
                />
              </template>
            </template>
            <el-empty v-else description="该节点没有可展示的表单" :image-size="60" />
          </template>
        </el-card>

        <el-card shadow="never" class="comments-card">
          <template #header>审批记录</template>
          <el-timeline v-if="comments.length">
            <el-timeline-item
              v-for="comment in comments"
              :key="comment.id"
              :timestamp="comment.time"
              placement="top"
            >
              <b>{{ comment.userName || comment.userId }}</b>: {{ comment.message }}
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审批记录" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ProcessViewer from '@/components/ProcessViewer.vue'
import FormDataView from '@/components/FormDataView.vue'
import DynamicForm from '@/components/DynamicForm.vue'
import {
  getActiveActivities,
  getProcessFormData,
  getProcessInstance,
  getProcessXml
} from '@/api/process'
import { claimTask, completeTask, getComments } from '@/api/task'
import type {
  CommentInfo,
  FormFieldSchema,
  FormInstanceData,
  ProcessFormData,
  ProcessInstance
} from '@/types'

interface SelectedNode {
  id: string
  type: string
  name: string
}

const route = useRoute()
const router = useRouter()
const processInstanceId = route.params.id as string

const loading = ref(false)
const instance = ref<ProcessInstance | null>(null)
const xml = ref('')
const formData = ref<ProcessFormData | null>(null)
const comments = ref<CommentInfo[]>([])
const activeIds = ref<string[]>([])
const selectedNode = ref<SelectedNode | null>(null)
const selectedOccurrenceIndex = ref(0)
const currentFormRef = ref<InstanceType<typeof DynamicForm>>()
const currentFormFields = ref<FormFieldSchema[]>([])
const currentFormValues = ref<Record<string, unknown>>({})
const comment = ref('')
const submitting = ref(false)

const startEventId = computed(() => {
  if (!xml.value) return ''
  const doc = new DOMParser().parseFromString(xml.value, 'text/xml')
  return doc.getElementsByTagNameNS('*', 'startEvent')[0]?.getAttribute('id') || ''
})

const taskFormsByKey = computed<Record<string, FormInstanceData[]>>(() => {
  const result: Record<string, FormInstanceData[]> = {}
  for (const form of formData.value?.taskForms || []) {
    const key = form.taskDefinitionKey || ''
    if (!key) continue
    if (!result[key]) result[key] = []
    result[key].push(form)
  }
  return result
})

const completedIds = computed(() => {
  const ids = new Set<string>()
  if (startEventId.value) ids.add(startEventId.value)
  for (const key of Object.keys(taskFormsByKey.value)) ids.add(key)
  return Array.from(ids)
})

const selectedOccurrences = computed<FormInstanceData[]>(() => {
  if (!selectedNode.value) return []
  if (selectedNode.value.type === 'bpmn:StartEvent') {
    return formData.value?.startForm ? [formData.value.startForm] : []
  }
  return taskFormsByKey.value[selectedNode.value.id] || []
})

const selectedForm = computed(
  () => selectedOccurrences.value[selectedOccurrenceIndex.value] || null
)

const isStartNode = computed(() => selectedNode.value?.type === 'bpmn:StartEvent')
const isActiveSelected = computed(() => selectedForm.value?.active === true)
const canEditSelected = computed(
  () => isActiveSelected.value && selectedForm.value?.canHandle === true
)
const formHasComment = computed(() =>
  currentFormFields.value.some((field) => field.key === 'comment')
)

const selectedAssignee = computed(() => {
  if (isStartNode.value) {
    return instance.value?.startUserName || '-'
  }
  if (
    isActiveSelected.value &&
    !selectedForm.value?.assigneeName &&
    !selectedForm.value?.assigneeId
  ) {
    return '待认领'
  }
  return selectedForm.value?.assigneeName || selectedForm.value?.assigneeId || '-'
})

const selectedTimeLabel = computed(() => {
  if (isStartNode.value) {
    return '发起时间'
  }
  return isActiveSelected.value ? '任务创建时间' : '完成时间'
})

const selectedTime = computed(() => {
  if (isStartNode.value) {
    return selectedForm.value?.startTime
  }
  return isActiveSelected.value ? selectedForm.value?.startTime : selectedForm.value?.endTime
})

const activeWarning = computed(() => {
  if (!isActiveSelected.value || canEditSelected.value) {
    return ''
  }
  if (!selectedForm.value?.assigneeName && !selectedForm.value?.assigneeId) {
    return '当前节点待有权限的人员认领，您只能查看。'
  }
  return `当前节点由 ${selectedForm.value?.assigneeName || selectedForm.value?.assigneeId} 处理，您只能查看。`
})

function parseFields(schemaJson?: string): FormFieldSchema[] {
  if (!schemaJson) return []
  try {
    const parsed = JSON.parse(schemaJson)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const resetOnOpenKeys = new Set([
  'approved',
  'comment',
  'financeResult',
  'consultantResult',
  'accountingResult',
  'finalResult'
])

function buildInitialValues(
  fields: FormFieldSchema[],
  variables: Record<string, unknown>
): Record<string, unknown> {
  const values: Record<string, unknown> = {}
  for (const field of fields) {
    if (field.type === 'section') {
      continue
    }
    const existing = variables[field.key]
    if (!resetOnOpenKeys.has(field.key) && existing !== undefined && existing !== null) {
      values[field.key] = field.type === 'user' ? String(existing) : existing
    } else if (field.defaultValue !== undefined) {
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

function coerceValue(field: FormFieldSchema, value: unknown): unknown {
  if (value === undefined || value === null || value === '') return value
  switch (field.type) {
    case 'number':
      return Number(value)
    case 'switch':
      return Boolean(value)
    case 'checkbox':
      return (Array.isArray(value) ? value : []).map((item) => coerceOption(field, item))
    case 'select':
    case 'radio':
      return coerceOption(field, value)
    default:
      return value
  }
}

function buildVariables(): Record<string, unknown> {
  const variables: Record<string, unknown> = {}
  for (const field of currentFormFields.value) {
    if (field.type === 'section') {
      continue
    }
    const value = coerceValue(field, currentFormValues.value[field.key])
    if (
      value === undefined ||
      value === null ||
      (typeof value === 'number' && Number.isNaN(value))
    ) {
      continue
    }
    variables[field.key] = value
  }
  return variables
}

watch(selectedForm, (form) => {
  currentFormFields.value = form ? parseFields(form.schemaJson) : []
  currentFormValues.value = form ? buildInitialValues(currentFormFields.value, form.values) : {}
  comment.value = ''
})

function onElementClick(payload: SelectedNode) {
  selectedNode.value = payload
  selectedOccurrenceIndex.value = Math.max(selectedOccurrences.value.length - 1, 0)
}

async function submitCurrentTask() {
  const form = selectedForm.value
  if (!form?.taskId) {
    return
  }
  if (currentFormFields.value.length && currentFormRef.value) {
    const valid = await currentFormRef.value.validate()
    if (!valid) {
      return
    }
  }
  submitting.value = true
  try {
    if (!form.assigneeId) {
      await claimTask(form.taskId)
    }
    await completeTask(form.taskId, {
      variables: buildVariables(),
      comment: comment.value
    })
    ElMessage.success('办理成功')
    await loadDetail()
  } finally {
    submitting.value = false
  }
}

async function loadDetail() {
  loading.value = true
  try {
    const info = (await getProcessInstance(processInstanceId)).data
    instance.value = info
    const [xmlRes, formDataRes, commentsRes] = await Promise.all([
      getProcessXml(info.processDefinitionId),
      getProcessFormData(processInstanceId),
      getComments(processInstanceId)
    ])
    xml.value = xmlRes.data
    formData.value = formDataRes.data
    comments.value = commentsRes.data
    if (!info.ended) {
      activeIds.value = (await getActiveActivities(processInstanceId)).data
    }
    await nextTick()
    const activeTask =
      (formData.value?.taskForms || []).find((form) => form.active && form.canHandle) ||
      (formData.value?.taskForms || []).find((form) => form.active)
    if (activeTask?.taskDefinitionKey) {
      onElementClick({
        id: activeTask.taskDefinitionKey,
        type: 'bpmn:UserTask',
        name: activeTask.taskName || ''
      })
    } else if (startEventId.value) {
      onElementClick({ id: startEventId.value, type: 'bpmn:StartEvent', name: '开始' })
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped lang="scss">
.page-header {
  margin-bottom: 12px;
}

.info-card {
  margin-bottom: 16px;
}

.diagram-card,
.form-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.hint {
  color: #909399;
  font-size: 12px;
  font-weight: normal;
}

.diagram-box {
  height: 640px;
}

.form-card {
  min-height: 320px;
}

.occurrence-select {
  margin-bottom: 12px;
}

.form-meta {
  margin-bottom: 12px;
}

.form-data-title {
  margin: 12px 0 8px;
  font-weight: 600;
  color: #303133;
}

.current-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.comments-card {
  margin-bottom: 16px;
}

@media (max-width: 1200px) {
  .diagram-box {
    height: 480px;
  }
}
</style>
