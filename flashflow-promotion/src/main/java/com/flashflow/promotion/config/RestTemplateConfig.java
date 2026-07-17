package com.flashflow.promotion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置（用于秒杀成功后调用订单服务）
 *
 * 超时控制：秒杀是核心链路，订单服务不可达时快速失败，
 * 避免 Tomcat 线程池被耗尽。
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
