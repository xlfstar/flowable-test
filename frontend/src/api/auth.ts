import http from './request'
import type { LoginResponse, UserInfo } from '@/types'

export interface LoginParams {
  username: string
  password: string
}

export const login = (data: LoginParams) => http.post<LoginResponse>('/auth/login', data)

export const getUserInfo = () => http.get<UserInfo>('/auth/info')

export const logout = () => http.post<void>('/auth/logout')
