package com.flashflow.order.entity;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 订单状态枚举 + 状态转换规则（状态机核心）
 *
 * 状态流：
 *   PENDING → PAID → SHIPPED → DELIVERED → COMPLETED
 *       ↘ CANCELLED
 *       ↘ REFUNDING → REFUNDED
 */
@Getter
public enum OrderStatus {

    PENDING    (0, "待支付"),
    PAID       (1, "已支付"),
    SHIPPED    (2, "已发货"),
    DELIVERED  (3, "已收货"),
    COMPLETED  (4, "已完成"),
    CANCELLED  (5, "已取消"),
    REFUNDING  (6, "退款中"),
    REFUNDED   (7, "已退款"),
    ;

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 合法转换表 */
    private static final java.util.Map<OrderStatus, Set<OrderStatus>> TRANSITIONS =
            new java.util.HashMap<>();

    static {
        TRANSITIONS.put(PENDING,    new HashSet<>(Arrays.asList(PAID, CANCELLED)));
        TRANSITIONS.put(PAID,       new HashSet<>(Arrays.asList(SHIPPED, REFUNDING, REFUNDED, CANCELLED)));
        TRANSITIONS.put(SHIPPED,    new HashSet<>(Arrays.asList(DELIVERED, REFUNDING)));
        TRANSITIONS.put(DELIVERED,  new HashSet<>(Arrays.asList(COMPLETED)));
        TRANSITIONS.put(REFUNDING,  new HashSet<>(Arrays.asList(REFUNDED, PAID)));   // 撤销退款
        // COMPLETED, CANCELLED, REFUNDED 为终态
    }

    /**
     * 校验状态转换是否合法
     * @return true 如果允许转换
     */
    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * 根据 code 获取枚举
     */
    public static OrderStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知订单状态: " + code));
    }
}
