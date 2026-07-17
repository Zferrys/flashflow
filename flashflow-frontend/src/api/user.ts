import request from './request'

export interface SysUser {
  id?: number
  username: string
  password?: string
  realName?: string
  email?: string
  mobile?: string
  status?: number
  createTime?: string
}

export function getUserPage(params: { page: number; size: number; keyword?: string }) {
  return request.get('/auth/user/page', { params })
}

export function createUser(data: SysUser) {
  return request.post('/auth/user', data)
}

export function updateUser(data: SysUser) {
  return request.put('/auth/user', data)
}

export function deleteUser(id: number) {
  return request.delete(`/auth/user/${id}`)
}
