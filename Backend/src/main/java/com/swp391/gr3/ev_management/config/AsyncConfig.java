package com.swp391.gr3.ev_management.config;

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableRetry // nếu dùng @Retryable
public class AsyncConfig {
    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        return new ThreadPoolTaskExecutorBuilder()
                .corePoolSize(2).maxPoolSize(8).queueCapacity(100)
                .threadNamePrefix("mail-")
                .build();
    }
}
