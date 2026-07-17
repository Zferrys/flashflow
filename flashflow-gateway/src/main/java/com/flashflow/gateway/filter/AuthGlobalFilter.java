package com.flashflow.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gateway 全局 JWT 鉴权过滤器。白名单路径从 application.yml 加载。
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "gateway.auth")
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String jwtSecret;

    /** 白名单——从 yml 注入，支持环境差异化 */
    private List<String> whiteList = List.of();

    public AuthGlobalFilter(@org.springframework.beans.factory.annotation.Value("${jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    void validateKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT secret 长度不足 256bit，无法安全用于 HS256。请在 yml 或环境变量 JWT_SECRET 中配置至少 32 字符的密钥");
        }
        log.info("白名单路径: {}", whiteList);
    }

    public void setWhiteList(List<String> whiteList) { this.whiteList = whiteList; }
    public List<String> getWhiteList() { return whiteList; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isWhiteListed(path)) return chain.filter(exchange);

        String token = extractToken(exchange.getRequest());
        if (!StringUtils.hasText(token)) return unauthorized(exchange, "Missing Authorization header");

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Name", claims.get("username", String.class))
                    .header("X-User-Role", claims.get("role", String.class))
                    .header("Authorization", "Bearer " + token)
                    .build();
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    @Override public int getOrder() { return -100; }

    private boolean isWhiteListed(String path) {
        return whiteList.stream().anyMatch(path::startsWith);
    }

    private String extractToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) return bearer.substring(7);
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = objectMapper.writeValueAsBytes(new ErrorResponse(401, msg));
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return exchange.getResponse().setComplete();
        }
    }

    @Data
    static class ErrorResponse { private final int code; private final String msg; }
}
