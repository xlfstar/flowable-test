import http from './request'
import type { ApprovalChainInfo, DepartmentInfo, OrgUserInfo, PositionInfo } from '@/types'

export const getDepartmentTree = () => http.get<DepartmentInfo[]>('/org/departments')

export const getPositions = () => http.get<PositionInfo[]>('/org/positions')

export const getOrgUsers = () => http.get<OrgUserInfo[]>('/org/users')

export const getApprovalChain = (days: number) =>
  http.get<ApprovalChainInfo>('/org/approval-chain', { days })
