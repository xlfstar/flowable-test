import http from './request'
import type { CommentInfo, PageResult, TaskDetail, TaskInfo } from '@/types'
import type { PageParams } from './process'

export const pageTodo = (params: PageParams) =>
  http.get<PageResult<TaskInfo>>('/tasks/todo', params)

export const pageDone = (params: PageParams) =>
  http.get<PageResult<TaskInfo>>('/tasks/done', params)

export const getTaskDetail = (taskId: string) =>
  http.get<TaskDetail>(`/tasks/${taskId}`)

export interface CompleteTaskParams {
  variables?: Record<string, unknown>
  comment?: string
}

export const completeTask = (taskId: string, data: CompleteTaskParams) =>
  http.post<void>(`/tasks/${taskId}/complete`, data)

export const claimTask = (taskId: string) => http.post<void>(`/tasks/${taskId}/claim`)

export const unclaimTask = (taskId: string) => http.post<void>(`/tasks/${taskId}/unclaim`)

export const getComments = (processInstanceId: string) =>
  http.get<CommentInfo[]>('/tasks/comments', { processInstanceId })
