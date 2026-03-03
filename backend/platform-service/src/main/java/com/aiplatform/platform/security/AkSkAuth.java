package com.aiplatform.platform.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AK/SK Authentication Annotation
 * 用于标注需要AK/SK认证的端点
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AkSkAuth {

    /**
     * 允许的凭证类型
     * meeting: 会议凭证
     * assistant: 助手凭证
     * cui_backend: CUI后端凭证
     */
    String[] credentialTypes() default {};

    /**
     * 需要的权限
     */
    String[] permissions() default {};

    /**
     * 是否验证签名
     */
    boolean requireSignature() default false;

    /**
     * 是否验证IP白名单
     */
    boolean verifyIpWhitelist() default true;
}