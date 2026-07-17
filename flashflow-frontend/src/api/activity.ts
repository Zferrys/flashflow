import request from './request'

export interface PromotionActivity {
  id?: number
  activityType: string
  name: string
  startTime: string
  endTime: string
  status?: number
  remark?: string
  createTime?: string
}

export interface PromotionSku {
  id?: number
  activityId: number
  skuId: number
  skuName: string
  skuImage: string
  originalPrice: number
  activityPrice: number
  stockLimit: number
  perUserLimit: number
  soldCount: number
  sort: number
}

export interface FlashSaleResult {
  success: boolean
  message: string
  orderSn: string
}

export function getActivityPage(params: { page: number; size: number; keyword?: string }) {
  return request.get('/promotion/activity/page', { params })
}

export function getActivityById(id: number) {
  return request.get(`/promotion/activity/${id}`)
}

export function createActivity(data: PromotionActivity) {
  return request.post('/promotion/activity', data)
}

export function updateActivity(data: PromotionActivity) {
  return request.put('/promotion/activity', data)
}

export function publishActivity(id: number) {
  return request.post(`/promotion/activity/${id}/publish`)
}

export function closeActivity(id: number) {
  return request.post(`/promotion/activity/${id}/close`)
}

export function addSku(activityId: number, data: PromotionSku) {
  return request.post(`/promotion/activity/${activityId}/sku`, data)
}

export function updateSku(activityId: number, data: PromotionSku) {
  return request.put(`/promotion/activity/${activityId}/sku`, data)
}

export function deleteSku(activityId: number, skuId: number) {
  return request.delete(`/promotion/activity/${activityId}/sku/${skuId}`)
}

export function getActivitySkuList(activityId: number) {
  return request.get(`/promotion/activity/${activityId}/sku`)
}

export function flashSale(data: { activityId: number; skuId: number; userId: number; quantity: number }) {
  return request.post<any, { code: number; msg: string; data: FlashSaleResult }>('/promotion/flash/sale', data)
}

export function getActivityCount() {
  return request.get('/promotion/activity/count')
}

export function getFlashNow() {
  return request.get('/promotion/flash/now')
}
