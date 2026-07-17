package com.flashflow.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置（用于支付回调通知订单服务）
 *
 * 超时控制：支付回调是异步通知，订单服务暂时不可达时快速失败，
 * 支付平台会按策略重试通知。
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);   // 连接超时 2 秒
        factory.setReadTimeout(5000);      // 读取超时 5 秒
        return new RestTemplate(factory);
    }
}
