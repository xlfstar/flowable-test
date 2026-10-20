import http from './request'
import type { FormDefinition, PageResult } from '@/types'
import type { PageParams } from './process'

export interface FormSaveParams {
  formKey: string
  name: string
  category?: string
  schemaJson?: string
  remark?: string
  status?: number
}

export const pageForms = (params: PageParams) =>
  http.get<PageResult<FormDefinition>>('/forms', params)

export const listAllForms = () => http.get<FormDefinition[]>('/forms/all')

export const getForm = (id: number) => http.get<FormDefinition>(`/forms/${id}`)

export const getFormByKey = (formKey: string) =>
  http.get<FormDefinition | null>(`/forms/by-key/${encodeURIComponent(formKey)}`)

export const resolveTaskForm = (taskId: string) =>
  http.get<FormDefinition | null>(`/forms/resolve/task/${taskId}`)

export const resolveStartForm = (processDefinitionId: string) =>
  http.get<FormDefinition | null>(`/forms/resolve/start/${processDefinitionId}`)

export const createForm = (data: FormSaveParams) =>
  http.post<FormDefinition>('/forms', data)

export const updateForm = (id: number, data: FormSaveParams) =>
  http.put<FormDefinition>(`/forms/${id}`, data)

export const deleteForm = (id: number) => http.delete<void>(`/forms/${id}`)
