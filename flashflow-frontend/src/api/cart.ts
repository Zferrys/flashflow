import request from './request'

export interface CartItem {
  id?: number
  userId: number
  skuId: number
  skuName: string
  skuImage?: string
  price: number
  quantity: number
  checked?: number
}

// userId 由 Gateway 注入 X-User-Id 头，后端通过 UserContext 获取

export function getCart() {
  return request.get('/order/cart')
}

export function addToCart(data: CartItem) {
  return request.post('/order/cart', data)
}

export function updateCartQuantity(id: number, quantity: number) {
  return request.put(`/order/cart/${id}/quantity`, null, { params: { quantity } })
}

export function removeFromCart(id: number) {
  return request.delete(`/order/cart/${id}`)
}

export function clearCheckedCart() {
  return request.delete('/order/cart/checked')
}

export function toggleChecked(id: number, checked: number) {
  return request.put(`/order/cart/${id}/checked`, null, { params: { checked } })
}

// Address

export function getAddresses() {
  return request.get('/order/address')
}

export function saveAddress(data: any) {
  return request.post('/order/address', data)
}

export function updateAddress(data: any) {
  return request.put('/order/address', data)
}

export function deleteAddress(id: number) {
  return request.delete(`/order/address/${id}`)
}

export function setDefaultAddress(id: number) {
  return request.put(`/order/address/${id}/default`)
}
