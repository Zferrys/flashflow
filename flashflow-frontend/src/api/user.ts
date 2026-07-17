import request from './request'

/** C端用户（user_info 表） */
export interface CUser {
  id?: number
  email?: string
  phone?: string
  password?: string
  nickname?: string
  avatar?: string
  gender?: number
  status?: number
  createTime?: string
}

export function getUserPage(params: { page: number; size: number; keyword?: string }) {
  return request.get('/auth/user/page', { params })
}

export function createUser(data: CUser) {
  return request.post('/auth/user', data)
}

export function updateUser(data: CUser) {
  return request.put('/auth/user', data)
}

export function deleteUser(id: number) {
  return request.delete(`/auth/user/${id}`)
}
