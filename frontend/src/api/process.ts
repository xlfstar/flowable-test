import http from './request'
import type {
  PageResult,
  ProcessDefinition,
  ProcessFormData,
  ProcessInstance,
  StartProcessParams
} from '@/types'

export interface PageParams {
  page?: number
  size?: number
  [key: string]: unknown
}

export const pageProcessDefinitions = (params: PageParams) =>
  http.get<PageResult<ProcessDefinition>>('/process-definitions', params)

export const deployProcess = (file: File, name?: string) => {
  const formData = new FormData()
  formData.append('file', file)
  if (name) {
    formData.append('name', name)
  }
  return http.post<ProcessDefinition>('/process-definitions/deploy', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const deleteDeployment = (deploymentId: string, cascade = true) =>
  http.delete<void>(`/process-definitions/${deploymentId}`, { cascade })

export const getProcessXml = (processDefinitionId: string) =>
  http.get<string>(`/process-definitions/${processDefinitionId}/xml`)

export const suspendProcess = (processDefinitionId: string) =>
  http.post<void>(`/process-definitions/${processDefinitionId}/suspend`)

export const activateProcess = (processDefinitionId: string) =>
  http.post<void>(`/process-definitions/${processDefinitionId}/activate`)

export const startProcess = (data: StartProcessParams) =>
  http.post<ProcessInstance>('/process-instances', data)

export const pageProcessInstances = (params: PageParams) =>
  http.get<PageResult<ProcessInstance>>('/process-instances', params)

export const pageMyProcessInstances = (params: PageParams) =>
  http.get<PageResult<ProcessInstance>>('/process-instances/my', params)

export const getProcessInstance = (processInstanceId: string) =>
  http.get<ProcessInstance>(`/process-instances/${processInstanceId}`)

export const getActiveActivities = (processInstanceId: string) =>
  http.get<string[]>(`/process-instances/${processInstanceId}/activities`)

export const getProcessFormData = (processInstanceId: string) =>
  http.get<ProcessFormData>(`/process-instances/${processInstanceId}/form-data`)

export const deleteProcessInstance = (processInstanceId: string, reason?: string) =>
  http.delete<void>(`/process-instances/${processInstanceId}`, { reason })
