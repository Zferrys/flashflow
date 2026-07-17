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

    // 死信队列（用于人工处理/告警）—— 需在 RabbitMQ 侧配置 policy 绑定到主队列:
    //   rabbitmqctl set_policy DLX "queue\\.order\\." '{"dead-letter-exchange":"exchange.order.dlx"}' --apply-to queues
    private static final String DLX_ORDER = "exchange.order.dlx";
    static final String DLQ_PAID   = "queue.order.paid.dlq";
    static final String DLQ_FAILED = "queue.order.payment.fail.dlq";
    static final String DLQ_REFUND = "queue.order.refund.success.dlq";

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

    /** 死信交换机 + 死信队列（声明后通过 RabbitMQ policy 绑定） */
    @Bean
    public TopicExchange orderDlxExchange() { return new TopicExchange(DLX_ORDER, true, false); }
    @Bean public Queue dlqPaidQueue()   { return QueueBuilder.durable(DLQ_PAID).build(); }
    @Bean public Queue dlqFailedQueue() { return QueueBuilder.durable(DLQ_FAILED).build(); }
    @Bean public Queue dlqRefundQueue() { return QueueBuilder.durable(DLQ_REFUND).build(); }

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
