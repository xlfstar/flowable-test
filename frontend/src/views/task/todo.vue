<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-form :inline="true" @submit.prevent>
          <el-form-item label="任务名称">
            <el-input v-model="query.name" placeholder="请输入任务名称" clearable @keyup.enter="load" @clear="load" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="load">查询</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="name" label="任务名称" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="openInstance(row)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="processDefinitionName" label="流程名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="businessKey" label="业务Key" width="110" />
        <el-table-column prop="assignee" label="办理人" width="110">
          <template #default="{ row }">{{ row.assigneeName || row.assignee || '待认领' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openHandle(row)">办理</el-button>
            <el-button link type="primary" @click="openInstance(row)">流程详情</el-button>
            <el-button v-if="!row.assignee" link type="success" @click="claim(row)">认领</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="handleVisible" title="办理任务" width="920px" top="5vh" @closed="onDialogClosed">
      <div v-if="detail" v-loading="detailLoading" class="handle-body">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="任务名称">{{ detail.task.name }}</el-descriptions-item>
          <el-descriptions-item label="流程名称">{{ detail.task.processDefinitionName }}</el-descriptions-item>
          <el-descriptions-item label="业务Key">{{ detail.task.businessKey || '-' }}</el-descriptions-item>
        </el-descriptions>

        <template v-if="detail.variables.days">
          <div class="section-title">申请信息</div>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="申请人">{{ detail.variables.applyUserName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ detail.variables.deptName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="岗位">{{ detail.variables.positionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="请假天数">{{ detail.variables.days ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="开始日期">{{ detail.variables.startDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="结束日期">{{ detail.variables.endDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="部门领导">{{ detail.variables.deptLeaderName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上级领导">{{ detail.variables.upperLeaderName || '无需' }}</el-descriptions-item>
            <el-descriptions-item label="事由">{{ detail.variables.reason || '-' }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <template v-else>
          <div class="section-title">业务数据</div>
          <el-collapse v-model="businessDataOpen">
            <el-collapse-item title="查看提交的业务数据" name="business">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="发起人">
                  {{ detail.variables.applyUserName || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="业务Key">
                  {{ detail.task.businessKey || '-' }}
                </el-descriptions-item>
                <el-descriptions-item
                  v-for="item in businessVariables"
                  :key="item.key"
                  :label="item.label"
                >
                  <template v-if="item.files.length">
                    <div v-for="file in item.files" :key="file.id" class="business-file">
                      <el-image
                        v-if="isImageFile(file)"
                        :src="file.url"
                        :preview-src-list="[file.url]"
                        fit="cover"
                        class="business-file-image"
                      />
                      <el-link :href="file.url" target="_blank" type="primary">
                        {{ file.name }}
                      </el-link>
                    </div>
                  </template>
                  <span v-else>{{ item.value }}</span>
                </el-descriptions-item>
              </el-descriptions>
              <el-empty
                v-if="!businessVariables.length"
                description="暂无业务数据"
                :image-size="60"
              />
            </el-collapse-item>
          </el-collapse>
        </template>

        <template v-if="formFields.length">
          <div class="section-title">审批表单</div>
          <DynamicForm
            ref="formRef"
            :fields="formFields"
            :model-value="formValues"
            @update:model-value="formValues = $event"
          />
        </template>

        <div class="section-title">流程进度</div>
        <div class="chart-box">
          <ProcessViewer v-if="handleVisible && detailXml" :xml="detailXml" :active-ids="detail.activeActivityIds" />
        </div>

        <template v-if="!formHasComment">
          <div class="section-title">审批意见</div>
          <el-input v-model="comment" type="textarea" :rows="2" placeholder="请输入审批意见(可选)" />
        </template>

        <div class="handle-actions">
          <el-button
            v-if="hasForm"
            type="primary"
            :icon="Select"
            :loading="submitting"
            @click="submit()"
          >
            提交
          </el-button>
          <template v-else>
            <el-button type="danger" :icon="CloseBold" :loading="submitting" @click="submit(false)">驳回</el-button>
            <el-button type="success" :icon="Select" :loading="submitting" @click="submit(true)">同意</el-button>
            <el-button @click="submit()">仅完成任务</el-button>
          </template>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CloseBold, Refresh, Search, Select } from '@element-plus/icons-vue'
import ProcessViewer from '@/components/ProcessViewer.vue'
import DynamicForm from '@/components/DynamicForm.vue'
import { claimTask, completeTask, getTaskDetail, pageTodo } from '@/api/task'
import { getProcessXml } from '@/api/process'
import { getOrgUsers } from '@/api/org'
import { resolveTaskForm } from '@/api/form'
import type { FormFieldSchema, OrgUserInfo, TaskDetail, TaskInfo, UploadedFile } from '@/types'

const router = useRouter()
const loading = ref(false)
const list = ref<TaskInfo[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, name: '' })

const handleVisible = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const detail = ref<TaskDetail | null>(null)
const detailXml = ref('')
const comment = ref('')

const formRef = ref<InstanceType<typeof DynamicForm>>()
const formFields = ref<FormFieldSchema[]>([])
const formValues = ref<Record<string, unknown>>({})
const businessDataOpen = ref(['business'])
const orgUsers = ref<OrgUserInfo[]>([])

const hasForm = computed(() => formFields.value.some((field) => field.type !== 'section'))
const formHasComment = computed(() => formFields.value.some((field) => field.key === 'comment'))

const variableLabels: Record<string, string> = {
  contractScanUploaded: '合同扫描件已上传',
  taxReportScreenshotUploaded: '税务报道截图已上传',
  accountingStandardScreenshotUploaded: '会计准则备案截图已上传',
  etaxAccount: '电子税务局账号',
  etaxPassword: '电子税务局密码',
  individualTaxPassword: '个税申报密码',
  individualTaxInfoTable: '个税申报信息表',
  invoiceManaged: '发票是否托管',
  invoiceQuotaNeeded: '是否需要申请发票额度',
  socialInsurancePurchased: '社保是否购买',
  housingFundPurchased: '公积金是否购买',
  publicAccountOpened: '是否开通公户',
  addressType: '地址情况',
  addressDetail: '地址详情',
  otherSupplement: '其他补充',
  hasInitialData: '是否有期初资料',
  accountSetType: '账套类型',
  financialStatements: '成立至今财务报表',
  subjectBalance: '成立至今科目余额表',
  generalLedger: '成立至今总账',
  detailLedger: '成立至今明细账',
  vouchers: '成立至今记账凭证',
  fixedAssets: '固定资产明细表',
  inventoryDetail: '库存明细表',
  financeContractScanComplete: '合同扫描件是否上传',
  financeContractScanConsistent: '合同扫描件是否与系统一致',
  financeTaxReportCompleted: '税务是否完成报道',
  financeAccountingStandardProvided: '会计准则是否提供',
  financeFullYearPaymentReceived: '是否收完整年款项',
  financeResult: '财务审核结论',
  supplementNote: '补充说明',
  consultantManagerId: '派工顾问主管',
  accountingManagerId: '派工会计主管',
  consultantId: '派工顾问',
  consultantDispatchNote: '顾问派工说明',
  accountantId: '派工会计',
  accountantDispatchNote: '会计派工说明',
  consultantYiqiCustomerCreated: '亿企客户已创建',
  consultantEtaxPasswordCorrect: '电子税务局密码正确',
  consultantIndividualTaxPasswordCorrect: '个税申报密码正确',
  consultantInitialDataComplete: '期初资料齐全',
  consultantOverdue: '是否存在逾期',
  consultantTaxOwed: '是否存在欠税款',
  consultantViolations: '是否存在违法违章',
  consultantWorkInjuryAbnormal: '工伤是否存在异常',
  consultantBankAuthorizationHandled: '银行授权是否处理好',
  consultantBankAuthorizationRemark: '银行授权备注',
  consultantFirstCallWithin3Days: '3个工作日内首电拉群',
  consultantFirstCallProof: '首电/拉群截图说明',
  consultantResult: '顾问审核结论',
  accountingAccountSetEstablished: '是否建立好账套',
  accountingEtaxPasswordCorrect: '电子税务局密码正确',
  accountingIndividualTaxPasswordCorrect: '个税申报密码正确',
  accountingInitialDataComplete: '期初资料齐全',
  accountingOverdue: '是否存在逾期',
  accountingTaxOwed: '是否存在欠税款',
  accountingViolations: '是否存在违法违章',
  accountingOldAccountIssue: '客户期初旧账问题',
  accountingOldAccountRemark: '旧账问题说明',
  accountingResult: '会计审核结论',
  finalHandoverMonth: '最终完成交接月份',
  commissionStartMonth: '开始核算提成月份',
  finalResult: '财务最终确认'
}

const hiddenVariableKeys = new Set([
  'initiator',
  'applyUserId',
  'applyUserName',
  'leaveId',
  'leaveDays',
  'deptId',
  'deptName',
  'positionName',
  'deptLeaderId',
  'deptLeaderName',
  'upperLeaderId',
  'upperLeaderName',
  'multiLevel',
  'approved',
  'comment',
  'approverName',
  'days',
  'startDate',
  'endDate',
  'reason'
])

const sensitiveVariableKeys = new Set(['etaxPassword', 'individualTaxPassword'])

const businessVariables = computed(() => {
  const taskDetail = detail.value
  if (!taskDetail) return []
  return Object.entries(taskDetail.variables)
    .filter(
      ([key, value]) =>
        !hiddenVariableKeys.has(key) &&
        !key.startsWith('taskFormData_') &&
        value !== undefined &&
        value !== null &&
        value !== ''
    )
    .map(([key, value]) => ({
      key,
      label: variableLabels[key] || key,
      value: formatVariableValue(key, value),
      files: extractFiles(value)
    }))
})

const valueTexts: Record<string, string> = {
  true: '是',
  false: '否',
  pass: '通过',
  reject: '退回',
  registered: '备案地址',
  actual: '实际地址',
  electronic: '电子版账本',
  backup: '账套备份或迁移码'
}

function isUploadedFile(value: unknown): value is UploadedFile {
  if (!value || typeof value !== 'object') {
    return false
  }
  const file = value as Record<string, unknown>
  return typeof file.id === 'number' && typeof file.name === 'string' && typeof file.url === 'string'
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

function isImageFile(file: UploadedFile): boolean {
  if (file.contentType?.startsWith('image/')) {
    return true
  }
  return /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(file.name)
}

function formatVariableValue(key: string, value: unknown): string {
  if (sensitiveVariableKeys.has(key)) {
    return '******'
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
  if (
    key === 'consultantId' ||
    key === 'accountantId' ||
    key === 'consultantManagerId' ||
    key === 'accountingManagerId'
  ) {
    const user = orgUsers.value.find((item) => String(item.id) === String(value))
    if (user) {
      return `${user.realName || user.username} (${user.username})`
    }
  }
  const text = String(value)
  return valueTexts[text] ?? text
}

async function load() {
  loading.value = true
  try {
    const res = await pageTodo({ page: query.page, size: query.size, name: query.name })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.name = ''
  query.page = 1
  load()
}

function openInstance(row: TaskInfo) {
  router.push({ name: 'ProcessInstanceDetail', params: { id: row.processInstanceId } })
}

async function openHandle(row: TaskInfo) {
  comment.value = ''
  detail.value = null
  formFields.value = []
  formValues.value = {}
  handleVisible.value = true
  detailLoading.value = true
  try {
    const res = await getTaskDetail(row.id)
    detail.value = res.data
    detailXml.value = (await getProcessXml(res.data.task.processDefinitionId)).data
    await loadForm(row.id, res.data.variables)
  } finally {
    detailLoading.value = false
  }
}

async function loadForm(taskId: string, variables: Record<string, unknown>) {
  if (!detail.value?.task.formKey) return
  const res = await resolveTaskForm(taskId)
  const definition = res.data
  if (!definition?.schemaJson) return
  let parsed: FormFieldSchema[] = []
  try {
    const value = JSON.parse(definition.schemaJson)
    parsed = Array.isArray(value) ? value : []
  } catch {
    parsed = []
  }
  formFields.value = parsed
  formValues.value = buildInitialValues(parsed, variables)
}

const resetOnOpenKeys = new Set([
  'approved',
  'comment',
  'financeResult',
  'consultantResult',
  'accountingResult',
  'finalResult'
])

function buildInitialValues(fields: FormFieldSchema[], variables: Record<string, unknown>): Record<string, unknown> {
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
      return (Array.isArray(value) ? value : []).map((v) => coerceOption(field, v))
    case 'select':
    case 'radio':
      return coerceOption(field, value)
    default:
      return value
  }
}

function buildVariables(): Record<string, unknown> {
  const variables: Record<string, unknown> = {}
  for (const field of formFields.value) {
    if (field.type === 'section') {
      continue
    }
    const value = coerceValue(field, formValues.value[field.key])
    if (value === undefined || value === null || (typeof value === 'number' && Number.isNaN(value))) continue
    variables[field.key] = value
  }
  return variables
}

function onDialogClosed() {
  detail.value = null
  detailXml.value = ''
  formFields.value = []
  formValues.value = {}
}

async function submit(approved?: boolean) {
  if (!detail.value) return
  if (formFields.value.length && formRef.value) {
    const valid = await formRef.value.validate()
    if (!valid) return
  }
  const variables = buildVariables()
  if (approved !== undefined) {
    variables.approved = approved
  }
  submitting.value = true
  try {
    await completeTask(detail.value.task.id, { variables, comment: comment.value })
    ElMessage.success(approved === false ? '已驳回' : '办理成功')
    handleVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function claim(row: TaskInfo) {
  await ElMessageBox.confirm('确定认领该任务吗?', '提示', { type: 'info' })
  await claimTask(row.id)
  ElMessage.success('认领成功')
  load()
}

onMounted(() => {
  load()
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
.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
  color: #303133;
}

.chart-box {
  height: 320px;
  border: 1px solid #ebeef5;
}

.business-file {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.business-file-image {
  width: 56px;
  height: 56px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.handle-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
