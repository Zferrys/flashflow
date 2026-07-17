package com.flashflow.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置（用于订单创建时调用优惠券服务计算折扣+核销）
 *
 * 超时控制：优惠券服务不可达时降级不使用优惠券，不阻塞订单创建主流程。
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
