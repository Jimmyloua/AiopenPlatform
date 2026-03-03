package com.aiplatform.connection;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Connection Service Application
 * Connection service for local agents and remote model APIs
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.aiplatform.connection.repository")
public class ConnectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectionServiceApplication.class, args);
    }

}