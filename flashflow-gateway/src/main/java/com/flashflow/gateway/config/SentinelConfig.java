package com.flashflow.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Sentinel Gateway 限流配置
 * - BlockHandler：统一返回 429 JSON 响应
 * - 限流规则：通过 Nacos 控制台动态配置（Gateway 流控规则）
 *   或通过 application.yml 中 spring.cloud.sentinel.datasource 配置
 * - 默认建议：秒杀接口 500 QPS，全局 API 2000 QPS
 */
@Configuration
public class SentinelConfig {

    @Bean
    public BlockRequestHandler blockRequestHandler() {
        return (exchange, ex) -> ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"code\":100004,\"msg\":\"请求过于频繁，请稍后再试\"}");
    }
}
