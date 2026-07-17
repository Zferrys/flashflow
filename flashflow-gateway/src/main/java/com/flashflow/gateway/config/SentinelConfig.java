package com.flashflow.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import reactor.core.publisher.Mono;

/**
 * Sentinel Gateway 限流配置
 * 触发限流时返回统一 JSON 错误响应（429 Too Many Requests）
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
