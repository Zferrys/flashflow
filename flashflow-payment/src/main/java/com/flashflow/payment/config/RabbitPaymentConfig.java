package com.flashflow.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付模块 RabbitMQ 配置（交换机和队列声明，防止启动顺序问题）
 */
@Configuration
public class RabbitPaymentConfig {

    public static final String EXCHANGE_PAYMENT   = "exchange.payment";
    public static final String RK_PAYMENT_SUCCESS = "payment.success";
    public static final String RK_PAYMENT_FAIL    = "payment.fail";
    public static final String QUEUE_PAID         = "queue.order.paid";
    public static final String QUEUE_FAILED       = "queue.order.payment.fail";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(EXCHANGE_PAYMENT, true, false);
    }

    @Bean
    public Queue paymentPaidQueue() {
        return QueueBuilder.durable(QUEUE_PAID).build();
    }

    @Bean
    public Queue paymentFailQueue() {
        return QueueBuilder.durable(QUEUE_FAILED).build();
    }

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentPaidQueue())
                .to(paymentExchange()).with(RK_PAYMENT_SUCCESS);
    }

    @Bean
    public Binding paymentFailBinding() {
        return BindingBuilder.bind(paymentFailQueue())
                .to(paymentExchange()).with(RK_PAYMENT_FAIL);
    }
}
