import http from './request'
import type { UploadedFile } from '@/types'

export const uploadFile = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<UploadedFile>('/files/upload', formData)
}
