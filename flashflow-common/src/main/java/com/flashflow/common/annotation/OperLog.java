package com.flashflow.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解 — 标记需要审计的管理操作
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {
    /** 模块名（如"用户管理"） */
    String module();
    /** 操作类型（新增/修改/删除/导出） */
    String operation();
}
