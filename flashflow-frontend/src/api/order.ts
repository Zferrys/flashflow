import request from './request'

export function getOrderPage(params: { page: number; size: number; status?: number; userId?: number }) {
  return request.get('/order/page', { params })
}

export function getOrderStats() {
  return request.get('/order/stats')
}

export function mockPay(orderSn: string, amount?: number) {
  const params: any = { orderSn }
  if (amount) params.amount = amount
  return request.post('/payment/mock-pay', null, { params })
}

export function getOrderBySn(orderSn: string) {
  return request.get(`/order/orderSn/${orderSn}`)
}

export function getOrderItems(orderId: number) {
  return request.get(`/order/${orderId}/items`)
}

export function getOrderEvents(orderId: number) {
  return request.get(`/order/${orderId}/events`)
}

export function confirmDeliver(orderId: number) {
  return request.post(`/order/${orderId}/deliver`)
}

export function cancelOrder(orderId: number, reason?: string) {
  return request.post(`/order/${orderId}/cancel`, null, { params: { reason: reason || '用户主动取消' } })
}
