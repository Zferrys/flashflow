package com.flashflow.inventory.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 库存模块 RabbitMQ 配置
 *
 * 监听订单事件：order.created → 扣库存，order.cancelled → 释放库存
 */
@Configuration
public class RabbitInventoryConfig {

    public static final String EXCHANGE_ORDER      = "exchange.order";
    public static final String QUEUE_DEDUCT        = "queue.inventory.deduct";
    public static final String QUEUE_RELEASE       = "queue.inventory.release";
    public static final String QUEUE_CONFIRM       = "queue.inventory.confirm";

    /** 死信交换机 + 死信队列（通过 RabbitMQ policy 绑定到主队列） */
    private static final String DLX_EXCHANGE = "exchange.order.dlx";
    static final String DLQ_DEDUCT  = "queue.inventory.deduct.dlq";
    static final String DLQ_RELEASE = "queue.inventory.release.dlq";
    static final String DLQ_CONFIRM = "queue.inventory.confirm.dlq";

    public static final String RK_ORDER_CREATED    = "order.created";
    public static final String RK_ORDER_CANCELLED  = "order.cancelled";
    public static final String RK_ORDER_PAID       = "order.paid";
    public static final String RK_ORDER_REFUNDED   = "order.refunded";

    /** 声明 TopicExchange（确保发布者声明前已存在） */
    @Bean
    public TopicExchange inventoryOrderExchange() {
        return new TopicExchange(EXCHANGE_ORDER, true, false);
    }

    /** 死信交换机 */
    @Bean
    public TopicExchange inventoryDlxExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue deductQueue()  { return QueueBuilder.durable(QUEUE_DEDUCT).build(); }
    @Bean
    public Queue releaseQueue() { return QueueBuilder.durable(QUEUE_RELEASE).build(); }
    @Bean
    public Queue confirmQueue() { return QueueBuilder.durable(QUEUE_CONFIRM).build(); }

    /** 死信队列 — 消息超限后进入，可人工处理/告警 */
    @Bean
    public Queue dlqDeductQueue()  { return QueueBuilder.durable(DLQ_DEDUCT).build(); }
    @Bean
    public Queue dlqReleaseQueue() { return QueueBuilder.durable(DLQ_RELEASE).build(); }
    @Bean
    public Queue dlqConfirmQueue() { return QueueBuilder.durable(DLQ_CONFIRM).build(); }

    @Bean
    public Binding dlqDeductBinding()  { return BindingBuilder.bind(dlqDeductQueue()).to(inventoryDlxExchange()).with("dlq.deduct"); }
    @Bean
    public Binding dlqReleaseBinding() { return BindingBuilder.bind(dlqReleaseQueue()).to(inventoryDlxExchange()).with("dlq.release"); }
    @Bean
    public Binding dlqConfirmBinding() { return BindingBuilder.bind(dlqConfirmQueue()).to(inventoryDlxExchange()).with("dlq.confirm"); }

    @Bean
    public Binding deductBinding() {
        return BindingBuilder.bind(deductQueue())
                .to(inventoryOrderExchange()).with(RK_ORDER_CREATED);
    }

    @Bean
    public Binding releaseBindingCancel() {
        return BindingBuilder.bind(releaseQueue())
                .to(inventoryOrderExchange()).with(RK_ORDER_CANCELLED);
    }

    @Bean
    public Binding releaseBindingRefund() {
        return BindingBuilder.bind(releaseQueue())
                .to(inventoryOrderExchange()).with(RK_ORDER_REFUNDED);
    }

    @Bean
    public Binding confirmBinding() {
        return BindingBuilder.bind(confirmQueue())
                .to(inventoryOrderExchange()).with(RK_ORDER_PAID);
    }

    /** 消费者容器工厂：prefetch=10 防止无限拉取导致 OOM，手动 ACK 保证可靠消费 */
    @Bean("inventoryListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory inventoryListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPrefetchCount(10);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false); // 不重新入队，去死信队列
        return factory;
    }
}
