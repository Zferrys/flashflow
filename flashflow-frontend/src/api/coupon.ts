import request from './request'

// userId 由 Gateway 注入 X-User-Id 头，后端通过 UserContext 获取，无需前端传参

export function getAvailableCoupons() {
  return request.get('/promotion/coupon/available')
}

export function getMyCoupons() {
  return request.get('/promotion/coupon/mine')
}

export function claimCoupon(couponId: number) {
  return request.post('/promotion/coupon/claim', null, { params: { couponId } })
}

export function calculateDiscount(userCouponId: number, amount: number) {
  return request.get('/promotion/coupon/calculate', { params: { userCouponId, amount } })
}

export function getCouponsByScope(categoryId?: number, skuId?: number) {
  return request.get('/promotion/coupon/by-scope', { params: { categoryId, skuId } })
}

export function autoGrantCoupons(grantType: string) {
  return request.post('/promotion/coupon/auto-grant', null, { params: { grantType } })
}

export function markCouponUsed(userCouponId: number, orderSn: string) {
  return request.post('/promotion/coupon/mark-used', null, { params: { userCouponId, orderSn } })
}
