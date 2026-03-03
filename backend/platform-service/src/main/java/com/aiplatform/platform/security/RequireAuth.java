package com.aiplatform.platform.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Require Authentication Annotation
 * 用于标注需要认证的端点
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {

    /**
     * 允许的认证类型
     */
    String[] types() default {"jwt", "aksk"};

    /**
     * 需要的权限
     */
    String[] permissions() default {};

    /**
     * 是否允许匿名访问
     */
    boolean allowAnonymous() default false;
}