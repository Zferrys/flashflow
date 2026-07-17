import request from './request'

export interface LoginData {
  username: string
  password: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: {
    id: number
    username: string
    realName: string
    roleCode: string
  }
}

export function loginApi(data: LoginData) {
  return request.post<any, { code: number; msg: string; data: LoginResult }>('/auth/login', data)
}

export function getUserInfoApi() {
  return request.get('/auth/me')
}

export function getUserCount() {
  return request.get('/auth/user/count')
}
