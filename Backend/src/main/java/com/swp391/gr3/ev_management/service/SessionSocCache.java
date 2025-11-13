package com.swp391.gr3.ev_management.service;

import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Optional;

@Component // Đánh dấu class này là 1 Spring Bean để có thể inject vào service khác
public class SessionSocCache {

    // Tạo cache Caffeine dạng (sessionId -> currentSOC)
    // ➜ Lưu SOC tạm thời trong suốt phiên sạc, tránh ghi DB liên tục
    private final Cache<Long, Integer> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(2))   // Mỗi phần tử hết hạn sau 2 giờ tính từ lần ghi cuối
            .maximumSize(10000)                      // Giới hạn tối đa 10.000 session trong cache để tránh tràn bộ nhớ
            .build();

    /**
     * Lưu giá trị SOC cho 1 sessionId vào cache.
     * - Được gọi khi phiên sạc bắt đầu hoặc khi worker cập nhật SOC từ thiết bị.
     */
    public void put(Long sessionId, int soc) {
        cache.put(sessionId, soc); // Ghi dữ liệu vào cache
    }

    /**
     * Lấy giá trị SOC hiện tại của 1 session (nếu có).
     * - Trả Optional để tránh null pointer.
     * - Nếu không tồn tại hoặc đã expire → Optional.empty()
     */
    public Optional<Integer> get(Long sessionId) {
        return Optional.ofNullable(cache.getIfPresent(sessionId)); // Kiểm tra và lấy từ cache
    }

    /**
     * Xoá SOC của session khỏi cache sau khi phiên sạc kết thúc.
     * - Giải phóng bộ nhớ và tránh giữ SOC không còn dùng.
     */
    public void remove(Long sessionId) {
        cache.invalidate(sessionId); // Xoá entry
    }
}
