package com.flashflow.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置（用于注册后异步发放优惠券等跨服务调用）
 *
 * 超时控制：优惠券服务不可达时快速失败，避免阻塞注册流程。
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
