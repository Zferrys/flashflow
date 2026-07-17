package com.flashflow.common.aspect;

import com.flashflow.common.annotation.OperLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作审计 AOP 切面
 * 拦截 @OperLog 注解的方法，记录操作日志
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint point, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        boolean success = true;
        String errorMsg = null;

        try {
            result = point.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            // 获取请求信息
            HttpServletRequest request = null;
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
            }

            // 结构化记录操作审计日志（JSON格式，方便ELK采集）
            log.info("OPER_AUDIT | module={} | operation={} | url={} | success={} | costMs={} | error={}",
                    operLog.module(),
                    operLog.operation(),
                    request != null ? request.getRequestURI() : "N/A",
                    success,
                    cost,
                    errorMsg != null ? errorMsg : "N/A");
        }
    }
}
