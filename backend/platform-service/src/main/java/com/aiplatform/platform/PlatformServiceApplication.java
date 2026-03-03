package com.aiplatform.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Platform Service Application
 * Core service for user, conversation, assistant, and skill management
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.aiplatform.platform.repository")
public class PlatformServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformServiceApplication.class, args);
    }

}