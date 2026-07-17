package com.flashflow.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单模块 RabbitMQ 配置（交换机 + 队列 + 绑定）
 */
@Configuration
public class RabbitOrderConfig {

    /** 交换机 */
    public static final String EXCHANGE_ORDER   = "exchange.order";
    public static final String EXCHANGE_PAYMENT = "exchange.payment";

    /** 订单队列 */
    public static final String QUEUE_PAID    = "queue.order.paid";
    public static final String QUEUE_FAILED  = "queue.order.payment.fail";
    public static final String QUEUE_REFUND  = "queue.order.refund.success";

    /** 路由键（必须与发布者一致） */
    public static final String RK_PAID    = "payment.success";
    public static final String RK_FAILED  = "payment.fail";
    public static final String RK_REFUND  = "payment.refund.success";

    // ========== 交换机 ==========

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_ORDER, true, false);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(EXCHANGE_PAYMENT, true, false);
    }

    // ========== 队列 ==========

    @Bean
    public Queue paidQueue() {
        return QueueBuilder.durable(QUEUE_PAID).build();
    }

    @Bean
    public Queue paymentFailQueue() {
        return QueueBuilder.durable(QUEUE_FAILED).build();
    }

    @Bean
    public Queue refundQueue() {
        return QueueBuilder.durable(QUEUE_REFUND).build();
    }

    // ========== 绑定 ==========

    @Bean
    public Binding paidBinding() {
        return BindingBuilder.bind(paidQueue())
                .to(paymentExchange()).with(RK_PAID);
    }

    @Bean
    public Binding paymentFailBinding() {
        return BindingBuilder.bind(paymentFailQueue())
                .to(paymentExchange()).with(RK_FAILED);
    }

    @Bean
    public Binding refundBinding() {
        return BindingBuilder.bind(refundQueue())
                .to(paymentExchange()).with(RK_REFUND);
    }
}
