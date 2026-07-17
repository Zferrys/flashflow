import request from './request'

export interface ProductSpu {
  id?: number
  spuName: string
  categoryId?: number
  description?: string
  mainImage?: string
  images?: string
  status?: number
  createTime?: string
}

export interface ProductSku {
  id?: number
  spuId: number
  skuName: string
  specs?: string
  price: number
  image?: string
  stock?: number
  status?: number
}

export function getSpuPage(params: { page: number; size: number; keyword?: string }) {
  return request.get('/promotion/product/spu/page', { params })
}

export function getSpuDetail(id: number) {
  return request.get(`/promotion/product/spu/${id}`)
}

export function createSpu(data: ProductSpu) {
  return request.post('/promotion/product/spu', data)
}

export function updateSpu(data: ProductSpu) {
  return request.put('/promotion/product/spu', data)
}

export function getSkuList(spuId: number) {
  return request.get(`/promotion/product/sku/${spuId}`)
}

export function createSku(data: ProductSku) {
  return request.post('/promotion/product/sku', data)
}

export function updateSku(data: ProductSku) {
  return request.put('/promotion/product/sku', data)
}

export function deleteSku(id: number) {
  return request.delete(`/promotion/product/sku/${id}`)
}

export function getActiveSpus() {
  return request.get('/promotion/product/spu/active')
}

export function getActiveSkus() {
  return request.get('/promotion/product/sku/active')
}
