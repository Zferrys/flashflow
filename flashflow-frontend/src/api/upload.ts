import request from './request'

/**
 * 上传图片文件
 * @param file 图片文件
 * @returns 图片URL
 */
export default async function uploadImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  // 不手动设 Content-Type，让 axios 自动带 boundary
  const res = await request.post('/auth/upload', formData)
  return res.data
}
