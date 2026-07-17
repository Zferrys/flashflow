package com.flashflow.inventory.config;

import org.springframework.amqp.core.*;
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
    public static final String RK_ORDER_CREATED    = "order.created";
    public static final String RK_ORDER_CANCELLED  = "order.cancelled";
    public static final String RK_ORDER_PAID       = "order.paid";

    /** 声明 TopicExchange（确保发布者声明前已存在） */
    @Bean
    public TopicExchange inventoryOrderExchange() {
        return new TopicExchange(EXCHANGE_ORDER, true, false);
    }

    @Bean
    public Queue deductQueue() {
        return QueueBuilder.durable(QUEUE_DEDUCT).build();
    }

    @Bean
    public Queue releaseQueue() {
        return QueueBuilder.durable(QUEUE_RELEASE).build();
    }

    @Bean
    public Queue confirmQueue() {
        return QueueBuilder.durable(QUEUE_CONFIRM).build();
    }

    @Bean
    public Binding deductBinding() {
        return BindingBuilder.bind(deductQueue())
                .to(inventoryOrderExchange()).with(RK_ORDER_CREATED);
    }

    @Bean
    public Binding releaseBinding() {
        return BindingBuilder.bind(releaseQueue())
                .to(inventoryOrderExchange()).with(RK_ORDER_CANCELLED);
    }

    @Bean
    public Binding confirmBinding() {
        return BindingBuilder.bind(confirmQueue())
                .to(inventoryOrderExchange()).with(RK_ORDER_PAID);
    }
}
