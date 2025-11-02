package com.swp391.gr3.ev_management.config;

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableRetry
public class AsyncConfig implements AsyncConfigurer {

    // ✅ Executor mặc định cho @Async (tên taskExecutor). Đánh dấu @Primary để hết cảnh báo.
    @Bean(name = "taskExecutor")
    @Primary
    public Executor taskExecutor() {
        return new ThreadPoolTaskExecutorBuilder()
                .threadNamePrefix("async-")
                .corePoolSize(8)
                .maxPoolSize(16)
                .queueCapacity(200)
                .build();
    }

    // ✅ Executor riêng cho mail (nếu bạn dùng @Async("mailExecutor"))
    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        return new ThreadPoolTaskExecutorBuilder()
                .threadNamePrefix("mail-")
                .corePoolSize(2)
                .maxPoolSize(8)
                .queueCapacity(100)
                .build();
    }
}
