package com.flashflow.promotion.config;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置（用于秒杀成功后调用订单服务）
 *
 * 使用 Apache HttpClient5 连接池，替代 SimpleClientHttpRequestFactory。
 * 后者在高并发下因无连接池 + 内部同步导致吞吐量陡降（实测 200 线程 QPS 仅 8.7）。
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // 连接池
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);          // 全局最大连接数
        cm.setDefaultMaxPerRoute(50); // 每个路由（Order 服务）最大连接数

        var httpClient = HttpClientBuilder.create()
                .setConnectionManager(cm)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(2000);
        factory.setConnectionRequestTimeout(2000); // 从连接池获取连接的超时

        return new RestTemplate(factory);
    }
}
