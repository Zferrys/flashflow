package com.flashflow.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付模块 RabbitMQ 配置（仅声明发布的 exchange，队列由消费者 Order 模块声明）
 */
@Configuration
public class RabbitPaymentConfig {

    public static final String EXCHANGE_PAYMENT   = "exchange.payment";
    public static final String RK_PAYMENT_SUCCESS = "payment.success";
    public static final String RK_PAYMENT_FAIL    = "payment.fail";
    public static final String RK_REFUND_SUCCESS  = "payment.refund.success";

    /** 支付交换机（发布方声明，确保消息可路由） */
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(EXCHANGE_PAYMENT, true, false);
    }
}
