export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  role: string
  deptId?: number
  deptName?: string
  positionId?: number
  positionName?: string
  email?: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  user: UserInfo
}

export interface ProcessDefinition {
  id: string
  key: string
  name: string
  version: number
  deploymentId: string
  resourceName: string
  diagramResourceName?: string
  category?: string
  description?: string
  suspensionState: number
  suspended: boolean
  deployTime?: string
}

export interface ProcessInstance {
  id: string
  processDefinitionId: string
  processDefinitionKey: string
  processDefinitionName?: string
  processDefinitionVersion?: string
  businessKey?: string
  startUserId?: string
  startUserName?: string
  startTime?: string
  endTime?: string
  durationInMillis?: number
  deleteReason?: string
  ended: boolean
  currentActivityName?: string
  activeActivityIds?: string[]
}

export interface FormInstanceData {
  taskId?: string
  taskName?: string
  taskDefinitionKey?: string
  formKey?: string
  formName?: string
  schemaJson?: string
  assigneeId?: string
  assigneeName?: string
  startTime?: string
  endTime?: string
  active?: boolean
  canHandle?: boolean
  values: Record<string, unknown>
}

export interface ProcessFormData {
  processInstanceId: string
  startForm?: FormInstanceData
  taskForms: FormInstanceData[]
  variables: Record<string, unknown>
}

export interface TaskInfo {
  id: string
  name: string
  taskDefinitionKey: string
  processInstanceId: string
  processDefinitionId: string
  processDefinitionKey?: string
  processDefinitionName?: string
  businessKey?: string
  assignee?: string
  assigneeName?: string
  description?: string
  category?: string
  formKey?: string
  createTime?: string
  claimTime?: string
  endTime?: string
  durationInMillis?: number
  deleteReason?: string
}

export interface CommentInfo {
  id: string
  type?: string
  userId?: string
  userName?: string
  message?: string
  taskId?: string
  processInstanceId?: string
  time?: string
}

export interface TaskDetail {
  task: TaskInfo
  variables: Record<string, unknown>
  comments: CommentInfo[]
  processInstance: ProcessInstance
  activeActivityIds: string[]
}

export interface StartProcessParams {
  processDefinitionKey: string
  businessKey?: string
  variables?: Record<string, unknown>
}

export type FormFieldType =
  | 'input'
  | 'textarea'
  | 'number'
  | 'select'
  | 'radio'
  | 'checkbox'
  | 'date'
  | 'datetime'
  | 'switch'
  | 'section'
  | 'user'
  | 'image'
  | 'file'

export type FormValueType = 'string' | 'number' | 'boolean'

export interface FormFieldOption {
  label: string
  value: string | number | boolean
}

export interface FormFieldCondition {
  field: string
  equals?: string | number | boolean
  notEquals?: string | number | boolean
  in?: (string | number | boolean)[]
}

export interface FormFieldSchema {
  key: string
  label: string
  type: FormFieldType
  required?: boolean
  placeholder?: string
  defaultValue?: string | number | boolean
  valueType?: FormValueType
  options?: FormFieldOption[]
  min?: number
  max?: number
  precision?: number
  span?: number
  role?: string
  inputType?: 'text' | 'password'
  accept?: string
  multiple?: boolean
  maxCount?: number
  visibleWhen?: FormFieldCondition[]
}

export interface UploadedFile {
  id: number
  name: string
  url: string
  size?: number
  contentType?: string
}

export interface FormDefinition {
  id?: number
  formKey: string
  name: string
  category?: string
  schemaJson: string
  remark?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface LeaveInfo {
  id: number
  processInstanceId?: string
  applyUserId?: number
  applyUserName?: string
  deptId?: number
  deptName?: string
  positionId?: number
  positionName?: string
  deptLeaderId?: number
  deptLeaderName?: string
  upperLeaderId?: number
  upperLeaderName?: string
  days: number
  startDate: string
  endDate: string
  reason: string
  status: number
  statusText?: string
  currentActivityName?: string
  currentTaskId?: string
  currentAssigneeName?: string
  createTime?: string
  updateTime?: string
}

export interface DepartmentInfo {
  id: number
  deptCode: string
  deptName: string
  parentId?: number
  leaderId?: number
  leaderName?: string
  sort?: number
  status?: number
  children?: DepartmentInfo[]
}

export interface PositionInfo {
  id: number
  positionCode: string
  positionName: string
  positionLevel?: number
  sort?: number
  status?: number
}

export interface OrgUserInfo {
  id: number
  username: string
  realName: string
  role: string
  deptId?: number
  deptName?: string
  positionId?: number
  positionName?: string
  departmentLeader?: boolean
  status?: number
}

export interface ApproverInfo {
  userId: number
  userName: string
  deptId?: number
  deptName?: string
  positionName?: string
  level: number
  levelName: string
}

export interface ApprovalChainInfo {
  days: number
  multiLevel: boolean
  valid: boolean
  message?: string
  applicantId?: number
  applicantName?: string
  deptId?: number
  deptName?: string
  positionName?: string
  approvers: ApproverInfo[]
}
