package com.flashflow.common.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举（6 位数字编码，按模块分段）
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ========== 通用 (10xxxx) ==========
    SUCCESS                 (0,       "成功"),
    SYSTEM_ERROR            (100001,  "系统繁忙，请稍后重试"),
    PARAM_ERROR             (100002,  "参数校验失败"),
    ILLEGAL_REQUEST         (100003,  "非法请求"),
    RATE_LIMITED            (100004,  "请求过于频繁，请稍后再试"),
    METHOD_NOT_SUPPORTED    (100005,  "不支持的请求方法"),
    MEDIA_TYPE_NOT_SUPPORTED(100006,  "不支持的 Content-Type"),
    MISSING_REQUEST_BODY    (100007,  "请求体不能为空"),

    // ========== 认证授权 (11xxxx) ==========
    TOKEN_EXPIRED           (110001,  "Token 已过期，请重新登录"),
    TOKEN_INVALID           (110002,  "Token 无效或已被篡改"),
    UNAUTHORIZED            (110003,  "未登录，请先登录"),
    FORBIDDEN               (110004,  "权限不足，拒绝访问"),
    USER_DISABLED           (110005,  "账号已被禁用"),
    LOGIN_FAILED            (110006,  "用户名或密码错误"),
    LOGIN_LOCKED            (110007,  "登录失败次数过多，账号已锁定 30 分钟"),
    PASSWORD_EXPIRED        (110008,  "密码已过期，请修改密码"),
    USER_NOT_FOUND          (110009,  "用户不存在"),
    PHONE_EXISTED           (110010,  "手机号已被注册"),
    EMAIL_EXISTED           (110011,  "邮箱已被注册"),
    CAPTCHA_ERROR           (110012,  "验证码错误或已过期"),

    // ========== 订单 (12xxxx) ==========
    ORDER_NOT_FOUND         (120001,  "订单不存在"),
    ORDER_STATUS_ERROR      (120002,  "订单状态不允许此操作"),
    ORDER_CANNOT_CANCEL     (120003,  "当前订单状态不可取消"),
    ORDER_EXPIRED           (120004,  "订单已超时关闭"),
    ORDER_CANNOT_PAY        (120005,  "当前订单状态不可支付"),
    ORDER_CANNOT_REFUND     (120006,  "当前订单状态不可退款"),
    ORDER_SN_GENERATE_FAIL  (120007,  "订单号生成失败"),
    INVALID_ORDER_ITEM      (120008,  "订单商品信息异常"),

    // ========== 库存 (13xxxx) ==========
    STOCK_NOT_ENOUGH        (130001,  "库存不足"),
    STOCK_LOCK_FAILED       (130002,  "获取分布式锁失败，请重试"),
    STOCK_SHARD_EXHAUSTED   (130003,  "当前分片库存耗尽，请重试"),
    STOCK_SKU_NOT_FOUND     (130004,  "SKU 不存在或已下架"),
    STOCK_FROZEN_FAILED     (130005,  "库存冻结失败"),
    STOCK_RELEASE_FAILED    (130006,  "库存释放失败"),

    // ========== 支付 (14xxxx) ==========
    PAYMENT_FAILED          (140001,  "支付失败"),
    PAYMENT_TIMEOUT         (140002,  "支付超时"),
    REFUND_FAILED           (140003,  "退款失败"),
    SIGN_VERIFY_FAILED      (140004,  "支付签名验证失败"),
    NOTIFY_DUPLICATE        (140005,  "重复的回调通知"),
    PAYMENT_NOT_FOUND       (140006,  "支付记录不存在"),
    REFUND_AMOUNT_EXCEED    (140007,  "退款金额超出可退金额"),

    // ========== 营销 (15xxxx) ==========
    ACTIVITY_NOT_STARTED    (150001,  "活动尚未开始"),
    ACTIVITY_ENDED          (150002,  "活动已结束"),
    ACTIVITY_NOT_FOUND      (150003,  "活动不存在或已删除"),
    ACTIVITY_CLOSED         (150004,  "活动已关闭"),
    BUY_LIMIT_EXCEEDED      (150005,  "超过每人限购数量"),
    DUPLICATE_PARTICIPATION (150006,  "您已参与过此活动"),
    ACTIVITY_SKU_NOT_FOUND  (150007,  "活动商品不存在"),
    PROMOTION_NOT_AVAILABLE (150008,  "当前不在活动期间"),
    ;

    private final int code;
    private final String message;
}
