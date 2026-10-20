<template>
  <div class="designer-page">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" :icon="DocumentAdd" @click="newDiagram">新建</el-button>
          <el-button :icon="FolderOpened" @click="triggerOpen">打开</el-button>
          <el-button :icon="MagicStick" @click="loadExample">加载示例</el-button>
          <el-button :icon="Download" @click="downloadXml">导出XML</el-button>
          <el-button :icon="Picture" @click="downloadSvg">导出SVG</el-button>
          <input ref="fileInputRef" type="file" accept=".bpmn,.xml,.bpmn20.xml" hidden @change="onFileChange" />
        </div>
        <div class="toolbar-right">
          <el-input v-model="deployName" placeholder="部署名称(可选)" style="width: 200px" />
          <el-button type="success" :icon="UploadFilled" :loading="deploying" @click="deploy">
            保存并部署
          </el-button>
        </div>
      </div>
    </el-card>

    <div class="designer-body">
      <div ref="canvasRef" class="canvas"></div>

      <div class="side-panel">
        <el-tabs v-model="activeTab" stretch>
          <el-tab-pane label="属性配置" name="props">
            <el-empty v-if="!current" description="请选择画布中的节点" />
            <el-form v-else label-width="88px" size="small" class="prop-form">
              <el-form-item label="节点类型">
                <el-tag size="small">{{ currentTypeLabel }}</el-tag>
              </el-form-item>
              <el-form-item label="ID">
                <el-input v-model="propForm.id" @change="onIdChange" />
              </el-form-item>
              <el-form-item label="名称">
                <el-input v-model="propForm.name" @change="onNameChange" />
              </el-form-item>

              <template v-if="isUserTask">
                <el-divider content-position="left">审批人配置</el-divider>

                <el-form-item label="审批人类型">
                  <el-select
                    v-model="propForm.approverType"
                    style="width: 100%"
                    @change="onApproverTypeChange"
                  >
                    <el-option
                      v-for="item in approverTypes"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item v-if="needsUser" label="指定用户">
                  <el-select
                    v-model="propForm.approverParam"
                    filterable
                    placeholder="请选择用户"
                    style="width: 100%"
                    @change="onApproverParamChange"
                  >
                    <el-option
                      v-for="user in activeUsers"
                      :key="user.id"
                      :label="`${user.realName} (${user.username})${user.deptName ? ' · ' + user.deptName : ''}`"
                      :value="String(user.id)"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item v-else-if="needsRole" label="角色/候选组">
                  <el-select
                    v-model="propForm.approverParam"
                    filterable
                    allow-create
                    default-first-option
                    placeholder="请选择或输入角色"
                    style="width: 100%"
                    @change="onApproverParamChange"
                  >
                    <el-option v-for="role in roles" :key="role" :label="role" :value="role" />
                  </el-select>
                </el-form-item>

                <el-form-item v-else-if="needsDepartment" label="审批部门">
                  <el-select
                    v-model="propForm.approverParam"
                    filterable
                    placeholder="请选择部门"
                    style="width: 100%"
                    @change="onApproverParamChange"
                  >
                    <el-option
                      v-for="department in activeDepartments"
                      :key="department.deptCode"
                      :label="`${department.deptName} (${department.deptCode})`"
                      :value="department.deptCode"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item v-else-if="needsPosition" label="审批岗位">
                  <el-select
                    v-model="propForm.approverParam"
                    filterable
                    placeholder="请选择岗位"
                    style="width: 100%"
                    @change="onApproverParamChange"
                  >
                    <el-option
                      v-for="position in activePositions"
                      :key="position.positionCode"
                      :label="`${position.positionName} (${position.positionCode})`"
                      :value="position.positionCode"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item v-else-if="needsVariable" label="变量名">
                  <el-input
                    v-model="propForm.approverParam"
                    placeholder="如 approverId"
                    @change="onApproverParamChange"
                  />
                </el-form-item>

                <el-form-item v-else-if="needsExpression" label="表达式">
                  <el-input
                    v-model="propForm.approverParam"
                    type="textarea"
                    :rows="2"
                    placeholder="如 ${approverResolver.deptLeader(applyUserId)}"
                    @change="onApproverParamChange"
                  />
                </el-form-item>

                <el-alert
                  type="info"
                  :closable="false"
                  show-icon
                  :title="approverHint"
                  style="margin-bottom: 12px"
                />

                <el-form-item :label="propForm.approverType === 'ROLE' ? '候选组' : '生成表达式'">
                  <el-input
                    :model-value="propForm.approverType === 'ROLE' ? propForm.candidateGroups : propForm.assignee"
                    readonly
                  />
                </el-form-item>

                <el-form-item v-if="isMultiInstance" label="多实例配置">
                  <el-input :model-value="multiInstancePreview" type="textarea" :rows="3" readonly />
                </el-form-item>
              </template>

              <template v-if="isUserTask || isStartEvent">
                <el-divider content-position="left">绑定表单</el-divider>
                <el-form-item label="表单标识">
                  <el-select
                    v-model="propForm.formKey"
                    placeholder="选择或输入表单标识"
                    filterable
                    allow-create
                    default-first-option
                    clearable
                    style="width: 100%"
                    @change="(v: string) => onFlowableAttr('formKey', v)"
                  >
                    <el-option
                      v-for="form in forms"
                      :key="form.formKey"
                      :label="`${form.name} (${form.formKey})`"
                      :value="form.formKey"
                    />
                  </el-select>
                </el-form-item>
              </template>

              <template v-if="isSequenceFlow">
                <el-divider content-position="left">连线条件</el-divider>
                <el-form-item label="条件">
                  <el-input
                    v-model="propForm.condition"
                    type="textarea"
                    :rows="3"
                    placeholder="如 ${approved == true}"
                    @change="onConditionChange"
                  />
                </el-form-item>
              </template>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="XML 源码" name="xml">
            <div class="xml-panel">
              <el-alert type="info" :closable="false" show-icon title="可直接编辑 BPMN XML, 修改后点击应用" />
              <el-input v-model="xmlText" type="textarea" :rows="22" spellcheck="false" class="xml-textarea" />
              <el-button type="primary" size="small" style="margin-top: 8px" @click="applyXml">
                应用 XML
              </el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  DocumentAdd,
  Download,
  FolderOpened,
  MagicStick,
  Picture,
  UploadFilled
} from '@element-plus/icons-vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import flowableModdle from '@/bpmn/flowable-moddle.json'
import exampleXml from '@/bpmn/leave-example.bpmn?raw'
import { customTranslateModule } from '@/bpmn/i18n'
import { deployProcess, getProcessXml } from '@/api/process'
import { getDepartmentTree, getOrgUsers, getPositions } from '@/api/org'
import { listAllForms } from '@/api/form'
import type { DepartmentInfo, FormDefinition, OrgUserInfo, PositionInfo } from '@/types'

const EMPTY_XML = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:flowable="http://flowable.org/bpmn"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
  targetNamespace="http://example.com/flowable">
  <process id="newProcess" name="新建流程" isExecutable="true">
    <startEvent id="start" name="开始"/>
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="newProcess">
      <bpmndi:BPMNShape id="Shape_start" bpmnElement="start">
        <omgdc:Bounds x="160" y="160" width="36" height="36"/>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>`

const route = useRoute()
const router = useRouter()
const canvasRef = ref<HTMLDivElement>()
const fileInputRef = ref<HTMLInputElement>()
const activeTab = ref('props')
const deployName = ref('')
const deploying = ref(false)
const xmlText = ref('')
const current = ref<any>(null)

type ApproverType =
  | 'INITIATOR'
  | 'USER'
  | 'ROLE'
  | 'DEPT_LEADER'
  | 'UPPER_DEPT_LEADER'
  | 'DEPT_ANY'
  | 'DEPT_ALL'
  | 'SPECIFIED_DEPT_ANY'
  | 'SPECIFIED_DEPT_ALL'
  | 'POSITION'
  | 'POSITION_IN_DEPT'
  | 'VARIABLE'
  | 'EXPRESSION'

interface ApproverTypeOption {
  value: ApproverType
  label: string
}

interface MultiInstanceConfig {
  collection: string
  elementVariable: string
  completionCondition?: string
}

interface ApproverBuildResult {
  assignee: string
  candidateGroups: string
  multiInstance?: MultiInstanceConfig
}

interface DesignerPropForm {
  id: string
  name: string
  approverType: ApproverType
  approverParam: string
  assignee: string
  candidateGroups: string
  candidateUsers: string
  formKey: string
  condition: string
}

const approverTypes: ApproverTypeOption[] = [
  { value: 'DEPT_LEADER', label: '部门领导（按申请人部门）' },
  { value: 'UPPER_DEPT_LEADER', label: '上级部门领导' },
  { value: 'DEPT_ANY', label: '本部门任意一人审批（或签）' },
  { value: 'DEPT_ALL', label: '本部门所有人会签' },
  { value: 'SPECIFIED_DEPT_ANY', label: '指定部门任意一人审批（或签）' },
  { value: 'SPECIFIED_DEPT_ALL', label: '指定部门所有人会签' },
  { value: 'POSITION_IN_DEPT', label: '本部门指定岗位' },
  { value: 'POSITION', label: '指定岗位（全局唯一）' },
  { value: 'USER', label: '指定用户' },
  { value: 'ROLE', label: '角色 / 候选组' },
  { value: 'INITIATOR', label: '发起人' },
  { value: 'VARIABLE', label: '表单字段 / 流程变量（用户ID）' },
  { value: 'EXPRESSION', label: '自定义表达式' }
]

const propForm = reactive<DesignerPropForm>({
  id: '',
  name: '',
  approverType: 'EXPRESSION',
  approverParam: '',
  assignee: '',
  candidateGroups: '',
  candidateUsers: '',
  formKey: '',
  condition: ''
})

let modeler: any = null

const forms = ref<FormDefinition[]>([])
const orgUsers = ref<OrgUserInfo[]>([])
const positions = ref<PositionInfo[]>([])
const departments = ref<DepartmentInfo[]>([])

const isUserTask = computed(() => current.value?.type === 'bpmn:UserTask')
const isSequenceFlow = computed(() => current.value?.type === 'bpmn:SequenceFlow')
const isStartEvent = computed(() => current.value?.type === 'bpmn:StartEvent')

const typeLabels: Record<string, string> = {
  'bpmn:Process': '流程',
  'bpmn:StartEvent': '开始事件',
  'bpmn:EndEvent': '结束事件',
  'bpmn:IntermediateThrowEvent': '中间抛出事件',
  'bpmn:IntermediateCatchEvent': '中间捕获事件',
  'bpmn:BoundaryEvent': '边界事件',
  'bpmn:UserTask': '用户任务',
  'bpmn:ServiceTask': '服务任务',
  'bpmn:ScriptTask': '脚本任务',
  'bpmn:ManualTask': '手动任务',
  'bpmn:BusinessRuleTask': '业务规则任务',
  'bpmn:SendTask': '发送任务',
  'bpmn:ReceiveTask': '接收任务',
  'bpmn:Task': '任务',
  'bpmn:CallActivity': '调用活动',
  'bpmn:SubProcess': '子流程',
  'bpmn:ExclusiveGateway': '排他网关',
  'bpmn:ParallelGateway': '并行网关',
  'bpmn:InclusiveGateway': '包含网关',
  'bpmn:ComplexGateway': '复杂网关',
  'bpmn:EventBasedGateway': '事件网关',
  'bpmn:SequenceFlow': '顺序流',
  'bpmn:MessageFlow': '消息流',
  'bpmn:Association': '关联',
  'bpmn:DataObjectReference': '数据对象引用',
  'bpmn:DataStoreReference': '数据存储引用',
  'bpmn:TextAnnotation': '文本注释',
  'bpmn:Group': '分组',
  'bpmn:Participant': '池/参与者',
  'bpmn:Lane': '泳道',
  'label': '标签'
}

const currentTypeLabel = computed(() => {
  const type = current.value?.type || ''
  return typeLabels[type] || type || '-'
})

const needsUser = computed(() => propForm.approverType === 'USER')
const needsRole = computed(() => propForm.approverType === 'ROLE')
const needsPosition = computed(() =>
  propForm.approverType === 'POSITION' || propForm.approverType === 'POSITION_IN_DEPT'
)
const needsDepartment = computed(() =>
  propForm.approverType === 'SPECIFIED_DEPT_ANY' || propForm.approverType === 'SPECIFIED_DEPT_ALL'
)
const needsVariable = computed(() => propForm.approverType === 'VARIABLE')
const needsExpression = computed(() => propForm.approverType === 'EXPRESSION')
const isMultiInstance = computed(() =>
  ['DEPT_ANY', 'DEPT_ALL', 'SPECIFIED_DEPT_ANY', 'SPECIFIED_DEPT_ALL'].includes(
    propForm.approverType
  )
)

const activeUsers = computed(() =>
  orgUsers.value.filter((user) => user.status === undefined || user.status === 1)
)

const activePositions = computed(() =>
  positions.value.filter((position) => position.status === undefined || position.status === 1)
)

const activeDepartments = computed(() => {
  const result: DepartmentInfo[] = []
  const walk = (nodes: DepartmentInfo[]) => {
    for (const node of nodes) {
      if (node.status === undefined || node.status === 1) {
        result.push(node)
      }
      if (node.children?.length) {
        walk(node.children)
      }
    }
  }
  walk(departments.value)
  return result
})

const roles = computed(() => {
  const values = new Set<string>()
  for (const user of activeUsers.value) {
    if (user.role) values.add(user.role)
  }
  return Array.from(values)
})

const approverHint = computed(() => {
  switch (propForm.approverType) {
    case 'INITIATOR':
      return '办理人 = 发起流程的当前用户。'
    case 'USER':
      return '直接指定一个固定用户作为办理人。'
    case 'ROLE':
      return '按角色/候选组分配，拥有该角色的用户可在待办中看到任务。'
    case 'DEPT_LEADER':
      return '按申请人所在部门负责人动态解析；申请人本人是负责人时自动向上级查找。'
    case 'UPPER_DEPT_LEADER':
      return '按申请人部门树上一级部门的负责人动态解析。'
    case 'DEPT_ANY':
      return '申请人所在部门每个人都会收到任务，任意一人审批后其余任务自动取消（或签）。'
    case 'DEPT_ALL':
      return '申请人所在部门每个人都会收到任务，必须全部审批完成（会签）。'
    case 'SPECIFIED_DEPT_ANY':
      return '指定部门每个人都会收到任务，任意一人审批后其余任务自动取消（或签）。'
    case 'SPECIFIED_DEPT_ALL':
      return '指定部门每个人都会收到任务，必须全部审批完成（会签）。'
    case 'POSITION':
      return '按岗位全局匹配唯一用户；多人岗位请改用“本部门指定岗位”。'
    case 'POSITION_IN_DEPT':
      return '按申请人所在部门 + 指定岗位匹配唯一用户。'
    case 'VARIABLE':
      return '填写流程变量名，变量值必须是用户ID；表单字段会作为流程变量传入。'
    case 'EXPRESSION':
      return '直接写入 flowable:assignee 表达式，适合高级用法。'
    default:
      return ''
  }
})

function initModeler() {
  modeler = new BpmnModeler({
    container: canvasRef.value,
    keyboard: { bindTo: document },
    moddleExtensions: { flowable: flowableModdle },
    additionalModules: [customTranslateModule]
  })

  const eventBus: any = modeler.get('eventBus')
  eventBus.on('selection.changed', (e: any) => {
    current.value = e.newSelection && e.newSelection.length ? e.newSelection[0] : null
    syncProperties()
  })
  eventBus.on('element.changed', (e: any) => {
    if (current.value && e.element === current.value) {
      syncProperties()
    }
  })
  eventBus.on('commandStack.changed', () => {
    saveXmlToPanel()
  })
}

async function importXml(xml: string) {
  try {
    await modeler.importXML(xml)
    const canvas = modeler.get('canvas')
    canvas.zoom('fit-viewport', 'auto')
    current.value = null
    await saveXmlToPanel()
  } catch (e: any) {
    ElMessage.error('加载流程图失败: ' + (e?.message || e))
  }
}

async function saveXmlToPanel() {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    xmlText.value = xml
  } catch {
    // ignore
  }
}

function syncProperties() {
  const el = current.value
  if (!el) {
    propForm.id = ''
    propForm.name = ''
    propForm.approverType = 'EXPRESSION'
    propForm.approverParam = ''
    propForm.assignee = ''
    propForm.candidateGroups = ''
    propForm.candidateUsers = ''
    propForm.formKey = ''
    propForm.condition = ''
    return
  }
  const bo = el.businessObject || {}
  const attrs = bo.$attrs || {}
  propForm.id = el.id || ''
  propForm.name = bo.name || ''
  propForm.assignee = bo.assignee || attrs['flowable:assignee'] || ''
  propForm.candidateGroups = bo.candidateGroups || attrs['flowable:candidateGroups'] || ''
  propForm.candidateUsers = bo.candidateUsers || attrs['flowable:candidateUsers'] || ''
  propForm.formKey = bo.formKey || attrs['flowable:formKey'] || ''
  propForm.condition = bo.conditionExpression?.body || ''

  const parsed = parseApprover(propForm.assignee, propForm.candidateGroups, bo.loopCharacteristics)
  propForm.approverType = parsed.type
  propForm.approverParam = parsed.param
}

function parseApprover(
  assignee: string,
  candidateGroups: string,
  loopCharacteristics?: any
): { type: ApproverType; param: string } {
  if (loopCharacteristics?.collection) {
    const collection = String(loopCharacteristics.collection)
    const hasCompletion = Boolean(loopCharacteristics.completionCondition?.body)
    if (collection.includes('departmentUserIdsByCode')) {
      const match = collection.match(/departmentUserIdsByCode\('([^']+)'/)
      return {
        type: hasCompletion ? 'SPECIFIED_DEPT_ANY' : 'SPECIFIED_DEPT_ALL',
        param: match ? match[1] : ''
      }
    }
    if (collection.includes('departmentUserIds')) {
      return { type: hasCompletion ? 'DEPT_ANY' : 'DEPT_ALL', param: '' }
    }
  }
  if (candidateGroups) {
    return { type: 'ROLE', param: candidateGroups }
  }
  const value = (assignee || '').trim()
  if (!value) {
    return { type: 'EXPRESSION', param: '' }
  }
  if (value === '${initiator}' || value === '${applyUserId}') {
    return { type: 'INITIATOR', param: '' }
  }
  if (/^\d+$/.test(value)) {
    return { type: 'USER', param: value }
  }
  const resolverMatch = value.match(/^\$\{approverResolver\.(\w+)\((.*)\)\}$/)
  if (resolverMatch) {
    const method = resolverMatch[1]
    const args = splitArgs(resolverMatch[2])
    if (method === 'deptLeader') {
      return { type: 'DEPT_LEADER', param: '' }
    }
    if (method === 'upperDeptLeader') {
      return { type: 'UPPER_DEPT_LEADER', param: '' }
    }
    if (method === 'position' || method === 'positionInDept') {
      return {
        type: method === 'position' ? 'POSITION' : 'POSITION_IN_DEPT',
        param: stripQuotes(args[1] || '')
      }
    }
    if (method === 'user') {
      const argument = (args[0] || '').trim()
      if (isQuoted(argument)) {
        return { type: 'USER', param: stripQuotes(argument) }
      }
      if (argument) {
        return { type: 'VARIABLE', param: argument }
      }
    }
  }
  if (value.startsWith('${') && value.endsWith('}')) {
    const name = value.slice(2, -1).trim()
    if (name === 'initiator' || name === 'applyUserId') {
      return { type: 'INITIATOR', param: '' }
    }
    if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(name)) {
      return { type: 'VARIABLE', param: name }
    }
  }
  return { type: 'EXPRESSION', param: value }
}

function splitArgs(value: string): string[] {
  return value.split(',').map((part) => part.trim())
}

function isQuoted(value: string): boolean {
  return /^['"].*['"]$/.test(value)
}

function stripQuotes(value: string): string {
  return value.replace(/^['"]|['"]$/g, '')
}

function buildApproverProps(): ApproverBuildResult {
  const param = propForm.approverParam.trim()
  switch (propForm.approverType) {
    case 'INITIATOR':
      return { assignee: '${initiator}', candidateGroups: '' }
    case 'USER':
      return {
        assignee: param ? `\${approverResolver.user('${param}')}` : '',
        candidateGroups: ''
      }
    case 'ROLE':
      return { assignee: '', candidateGroups: param }
    case 'DEPT_LEADER':
      return { assignee: '${approverResolver.deptLeader(applyUserId)}', candidateGroups: '' }
    case 'UPPER_DEPT_LEADER':
      return {
        assignee: '${approverResolver.upperDeptLeader(applyUserId)}',
        candidateGroups: ''
      }
    case 'DEPT_ANY':
      return {
        assignee: '${approverId}',
        candidateGroups: '',
        multiInstance: {
          collection: '${approverResolver.departmentUserIds(applyUserId)}',
          elementVariable: 'approverId',
          completionCondition: '${nrOfCompletedInstances >= 1}'
        }
      }
    case 'DEPT_ALL':
      return {
        assignee: '${approverId}',
        candidateGroups: '',
        multiInstance: {
          collection: '${approverResolver.departmentUserIds(applyUserId)}',
          elementVariable: 'approverId'
        }
      }
    case 'SPECIFIED_DEPT_ANY':
      if (!param) {
        return { assignee: '', candidateGroups: '' }
      }
      return {
        assignee: '${approverId}',
        candidateGroups: '',
        multiInstance: {
          collection: `\${approverResolver.departmentUserIdsByCode('${param}', applyUserId)}`,
          elementVariable: 'approverId',
          completionCondition: '${nrOfCompletedInstances >= 1}'
        }
      }
    case 'SPECIFIED_DEPT_ALL':
      if (!param) {
        return { assignee: '', candidateGroups: '' }
      }
      return {
        assignee: '${approverId}',
        candidateGroups: '',
        multiInstance: {
          collection: `\${approverResolver.departmentUserIdsByCode('${param}', applyUserId)}`,
          elementVariable: 'approverId'
        }
      }
    case 'POSITION':
      return {
        assignee: param ? `\${approverResolver.position(applyUserId, '${param}')}` : '',
        candidateGroups: ''
      }
    case 'POSITION_IN_DEPT':
      return {
        assignee: param ? `\${approverResolver.positionInDept(applyUserId, '${param}')}` : '',
        candidateGroups: ''
      }
    case 'VARIABLE':
      return {
        assignee: param ? `\${approverResolver.user(${param})}` : '',
        candidateGroups: ''
      }
    case 'EXPRESSION':
    default:
      return { assignee: param, candidateGroups: '' }
  }
}

const multiInstancePreview = computed(() => {
  const built = buildApproverProps()
  if (!built.multiInstance) {
    return ''
  }
  const completionCondition = built.multiInstance.completionCondition || '全部完成'
  return `集合: ${built.multiInstance.collection}\n完成条件: ${completionCondition}`
})

function applyApprover() {
  if (!current.value || !isUserTask.value) return
  const modeling = modeler.get('modeling')
  const bpmnFactory = modeler.get('bpmnFactory')
  const bo = current.value.businessObject
  if (!bo.$attrs) {
    bo.$attrs = {}
  }
  const built = buildApproverProps()
  const attrs: Record<string, string> = {
    'flowable:assignee': built.assignee,
    'flowable:candidateGroups': built.candidateGroups
  }
  for (const [key, value] of Object.entries(attrs)) {
    if (value) {
      bo.$attrs[key] = value
    } else {
      delete bo.$attrs[key]
    }
  }
  delete bo.$attrs['flowable:candidateUsers']

  let loopCharacteristics
  if (built.multiInstance) {
    const completionCondition = built.multiInstance.completionCondition
      ? bpmnFactory.create('bpmn:FormalExpression', {
          body: built.multiInstance.completionCondition
        })
      : undefined
    loopCharacteristics = bpmnFactory.create('bpmn:MultiInstanceLoopCharacteristics', {
      isSequential: false,
      collection: built.multiInstance.collection,
      elementVariable: built.multiInstance.elementVariable,
      completionCondition
    })
  }

  modeling.updateProperties(current.value, {
    assignee: built.assignee || undefined,
    candidateGroups: built.candidateGroups || undefined,
    candidateUsers: undefined,
    loopCharacteristics
  })

  propForm.assignee = built.assignee
  propForm.candidateGroups = built.candidateGroups
  propForm.candidateUsers = ''
}

function onApproverTypeChange() {
  propForm.approverParam = ''
  applyApprover()
}

function onApproverParamChange() {
  applyApprover()
}

function onIdChange(value: string) {
  if (!current.value || !value) return
  try {
    modeler.get('modeling').updateProperties(current.value, { id: value })
  } catch (e: any) {
    ElMessage.error('修改ID失败: ' + (e?.message || e))
    syncProperties()
  }
}

function onNameChange(value: string) {
  if (!current.value) return
  modeler.get('modeling').updateProperties(current.value, { name: value })
}

function onFlowableAttr(attr: string, value: string) {
  if (!current.value) return
  const modeling = modeler.get('modeling')
  const bo = current.value.businessObject
  if (!bo.$attrs) {
    bo.$attrs = {}
  }
  const key = `flowable:${attr}`
  if (value) {
    bo.$attrs[key] = value
  } else {
    delete bo.$attrs[key]
  }
  modeling.updateProperties(current.value, { [attr]: value || undefined })
}

function onConditionChange(value: string) {
  if (!current.value) return
  const bpmnFactory = modeler.get('bpmnFactory')
  const modeling = modeler.get('modeling')
  const condition = value ? bpmnFactory.create('bpmn:FormalExpression', { body: value }) : undefined
  modeling.updateProperties(current.value, { conditionExpression: condition })
}

function newDiagram() {
  router.replace({ path: '/process/designer' })
  importXml(EMPTY_XML)
}

function loadExample() {
  router.replace({ path: '/process/designer' })
  importXml(exampleXml)
}

function triggerOpen() {
  fileInputRef.value?.click()
}

function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    importXml(String(reader.result))
    deployName.value = file.name.replace(/\.(bpmn20\.xml|bpmn|xml)$/i, '')
  }
  reader.readAsText(file, 'utf-8')
  target.value = ''
}

async function downloadXml() {
  const { xml } = await modeler.saveXML({ format: true })
  downloadBlob(new Blob([xml], { type: 'application/xml' }), `${deployName.value || 'process'}.bpmn20.xml`)
}

async function downloadSvg() {
  const { svg } = await modeler.saveSVG()
  downloadBlob(new Blob([svg], { type: 'image/svg+xml' }), `${deployName.value || 'process'}.svg`)
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function applyXml() {
  if (!xmlText.value) return
  importXml(xmlText.value)
  ElMessage.success('已应用 XML')
}

async function deploy() {
  const { xml } = await modeler.saveXML({ format: true })
  const blob = new Blob([xml], { type: 'application/xml' })
  const file = new File([blob], `${deployName.value || 'process'}.bpmn20.xml`, {
    type: 'application/xml'
  })
  deploying.value = true
  try {
    const res = await deployProcess(file, deployName.value || undefined)
    ElMessage.success(`部署成功: ${res.data.name} (v${res.data.version})`)
  } finally {
    deploying.value = false
  }
}

function processNameFromXml(xml: string): string {
  const match = xml.match(/<[^:]*process[^>]*\bname="([^"]+)"/)
  return match ? match[1] : ''
}

onMounted(async () => {
  await nextTick()
  initModeler()

  const definitionId = typeof route.query.definitionId === 'string'
    ? route.query.definitionId
    : undefined
  if (definitionId) {
    try {
      const res = await getProcessXml(definitionId)
      deployName.value = processNameFromXml(res.data)
      await importXml(res.data)
    } catch {
      ElMessage.error('加载流程定义失败，已打开空白流程')
      await importXml(EMPTY_XML)
    }
  } else {
    await importXml(EMPTY_XML)
  }

  const [formsRes, usersRes, positionsRes, departmentsRes] = await Promise.allSettled([
    listAllForms(),
    getOrgUsers(),
    getPositions(),
    getDepartmentTree()
  ])
  if (formsRes.status === 'fulfilled') {
    forms.value = formsRes.value.data
  }
  if (usersRes.status === 'fulfilled') {
    orgUsers.value = usersRes.value.data
  }
  if (positionsRes.status === 'fulfilled') {
    positions.value = positionsRes.value.data
  }
  if (departmentsRes.status === 'fulfilled') {
    departments.value = departmentsRes.value.data
  }
})

onBeforeUnmount(() => {
  modeler?.destroy()
  modeler = null
})
</script>

<style scoped lang="scss">
.designer-page {
  padding: 12px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar-card {
  :deep(.el-card__body) {
    padding: 12px;
  }
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

.designer-body {
  flex: 1;
  display: flex;
  gap: 12px;
  min-height: 0;
}

.canvas {
  flex: 1;
  background-color: #fff;
  background-image: linear-gradient(90deg, #f5f5f5 1px, transparent 1px),
    linear-gradient(#f5f5f5 1px, transparent 1px);
  background-size: 20px 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  height: calc(100vh - 180px);
}

.side-panel {
  width: 340px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 12px;
  overflow: auto;
  height: calc(100vh - 180px);
}

.prop-form {
  margin-top: 8px;
}

.xml-panel {
  display: flex;
  flex-direction: column;
}

.xml-textarea :deep(textarea) {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}
</style>
