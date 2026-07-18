package com.flashflow.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
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
import reactor.core.scheduler.Schedulers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private final RedissonClient redissonClient; // Redis 不可用时为 null，黑名单降级
    private static final String BLACKLIST_PREFIX = "flashflow:auth:jwt:blacklist:";

    /** 白名单——从 yml 注入，支持环境差异化 */
    private List<String> whiteList = List.of();

    public AuthGlobalFilter(@org.springframework.beans.factory.annotation.Value("${jwt.secret}") String jwtSecret,
                            ObjectProvider<RedissonClient> redissonProvider) {
        this.jwtSecret = jwtSecret;
        this.redissonClient = redissonProvider.getIfAvailable();
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
        String method = exchange.getRequest().getMethod() != null
                ? exchange.getRequest().getMethod().name() : "GET";
        if (isWhiteListed(path, method)) return chain.filter(exchange);

        String token = extractToken(exchange.getRequest());
        if (!StringUtils.hasText(token)) return unauthorized(exchange, "Missing Authorization header");

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();

            // JWT 黑名单检查（登出后 Token 立即失效）
            // 使用反应式链避免阻塞 WebFlux 事件循环
            String jti = claims.getId();
            if (jti != null && !jti.isEmpty() && redissonClient != null) {
                return Mono.fromCallable(() ->
                        redissonClient.getBucket(BLACKLIST_PREFIX + jti).isExists())
                        .subscribeOn(Schedulers.boundedElastic())
                        .timeout(Duration.ofSeconds(2))
                        .onErrorResume(e -> {
                            log.warn("Redis 黑名单查询失败，降级放行: jti={}, error={}", jti, e.getMessage());
                            return Mono.just(false);
                        })
                        .flatMap(blacklisted -> {
                            if (Boolean.TRUE.equals(blacklisted)) {
                                log.warn("Token 已被吊销: jti={}", jti);
                                return unauthorized(exchange, "Token has been revoked");
                            }
                            return forwardWithUserHeaders(exchange, chain, claims, token);
                        });
            }

            return forwardWithUserHeaders(exchange, chain, claims, token);
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    @Override public int getOrder() { return -100; }

    /**
     * 白名单路径匹配。
     * 格式：
     *   - "GET:/path"  → 仅 GET 请求匹配（用于公开浏览接口）
     *   - "/path"       → 所有方法匹配（用于登录/回调等）
     *   - "/path/**"    → 前缀匹配所有子路径
     */
    private boolean isWhiteListed(String path, String method) {
        return whiteList.stream().anyMatch(pattern -> {
            // 方法限定匹配：GET:/path 格式
            if (pattern.startsWith("GET:")) {
                if (!"GET".equalsIgnoreCase(method)) return false;
                pattern = pattern.substring(4); // 去掉 "GET:" 前缀
            }
            // 前缀匹配
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                return path.startsWith(prefix);
            }
            // 精确匹配 + 子路径匹配
            return path.equals(pattern) || path.startsWith(pattern + "/");
        });
    }

    private String extractToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) return bearer.substring(7);
        return null;
    }

    /** 将用户信息写入 Header 并转发请求 */
    private Mono<Void> forwardWithUserHeaders(ServerWebExchange exchange, GatewayFilterChain chain,
                                               Claims claims, String token) {
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Name", claims.get("username", String.class))
                .header("X-User-Role", claims.get("role", String.class))
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
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
