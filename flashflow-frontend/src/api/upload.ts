import request from './request'

/**
 * 上传图片文件
 * @param file 图片文件
 * @returns 图片URL
 */
export default async function uploadImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await request.post('/auth/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data
}
