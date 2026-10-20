import http from './request'
import type { LeaveInfo, PageResult } from '@/types'
import type { PageParams } from './process'

export interface LeaveApplyParams {
  days: number
  startDate: string
  endDate: string
  reason: string
}

export const applyLeave = (data: LeaveApplyParams) =>
  http.post<LeaveInfo>('/leave/apply', data)

export const pageMyLeave = (params: PageParams) =>
  http.get<PageResult<LeaveInfo>>('/leave/my', params)

export const getLeave = (id: number) => http.get<LeaveInfo>(`/leave/${id}`)
