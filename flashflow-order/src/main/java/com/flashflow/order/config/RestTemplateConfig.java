package com.flashflow.order.config;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置（用于订单创建时调用优惠券服务）
 *
 * 使用 Apache HttpClient5 连接池，替代 SimpleClientHttpRequestFactory。
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(30);

        var httpClient = HttpClientBuilder.create()
                .setConnectionManager(cm)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(2000);
        factory.setConnectionRequestTimeout(2000);

        return new RestTemplate(factory);
    }
}
