package com.swp391.gr3.ev_management.config;

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@Configuration                     // 🔧 Đánh dấu đây là class cấu hình Spring
@EnableAsync                       // 🚀 Bật hỗ trợ chạy bất đồng bộ (@Async)
@EnableRetry                       // 🔁 Bật Spring Retry cho phép retry tự động khi lỗi
public class AsyncConfig implements AsyncConfigurer {

    // ======================================================================
    // ✅ Bean Executor mặc định dùng cho tất cả @Async không chỉ định tên
    // ======================================================================
    @Bean(name = "taskExecutor")   // 🏷️ Đặt tên bean là "taskExecutor"
    @Primary                       // ⭐ Đánh dấu đây là Executor mặc định -> không còn cảnh báo MissingTaskExecutor
    public Executor taskExecutor() {
        return new ThreadPoolTaskExecutorBuilder()
                .threadNamePrefix("async-") // 🧵 Tất cả thread tạo ra sẽ có prefix "async-"
                .corePoolSize(8)            // 🔹 Số lượng thread chạy thường trực
                .maxPoolSize(16)            // 🔹 Tối đa thread có thể mở rộng khi tải cao
                .queueCapacity(200)         // 📌 Sẵn sàng chứa tối đa 200 task chờ xử lý
                .build();                   // 🏗️ Tạo ra ThreadPoolTaskExecutor
    }

    // ======================================================================
    // ✅ Executor riêng cho tác vụ gửi email (nếu dùng @Async("mailExecutor"))
    // ======================================================================
    @Bean(name = "mailExecutor")   // 🏷️ Tạo một executor riêng cho email service
    public Executor mailExecutor() {
        return new ThreadPoolTaskExecutorBuilder()
                .threadNamePrefix("mail-")  // 🧵 Prefix để dễ debug log
                .corePoolSize(2)            // ✉️ mail nhẹ nên chỉ cần ít thread
                .maxPoolSize(8)             // 🔼 Có thể mở rộng khi gửi mail hàng loạt
                .queueCapacity(100)         // 📌 Hàng đợi chứa 100 mail pending
                .build();
    }
}
