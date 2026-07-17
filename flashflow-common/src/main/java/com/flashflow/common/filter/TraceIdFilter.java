package com.flashflow.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 全链路追踪过滤器
 * 每个请求生成唯一 TraceId，写入 MDC 和响应头，方便 ELK/SkyWalking 等日志系统关联
 */
public class TraceIdFilter extends OncePerRequestFilter {

    /** TraceId 的 header 名称 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 优先使用上游传入的 TraceId（Gateway 传递），否则生成新的
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        try {
            // 写入 MDC（Logback 可用 %X{traceId} 输出到日志）
            MDC.put("traceId", traceId);
            // 写入响应头（方便前端排查问题）
            response.setHeader(TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            // 清理 MDC（防止线程池复用时的内存泄漏）
            MDC.remove("traceId");
        }
    }
}
