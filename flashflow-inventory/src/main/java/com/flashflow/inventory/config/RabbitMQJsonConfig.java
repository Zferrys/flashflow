package com.flashflow.inventory.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ JSON 序列化配置（替代 Java 序列化，解决 LinkedHashMap 反序列化安全限制）
 */
@Configuration
public class RabbitMQJsonConfig {

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter c) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(c);
        return t;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf, SimpleRabbitListenerContainerFactoryConfigurer cfg, Jackson2JsonMessageConverter c) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        cfg.configure(f, cf);
        f.setMessageConverter(c);
        f.setDefaultRequeueRejected(false);
        return f;
    }
}
