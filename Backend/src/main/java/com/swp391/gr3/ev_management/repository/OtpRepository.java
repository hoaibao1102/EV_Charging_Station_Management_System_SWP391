package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpVerification, Long> {
    // ✅ Repository này quản lý entity "OtpVerification" — dùng để lưu thông tin mã OTP (email, mã, thời gian hết hạn,...)
    // ✅ Kế thừa JpaRepository => có sẵn các hàm CRUD (findAll, save, delete, findById, ...)

    /**
     * ✅ Lấy bản ghi OTP mới nhất (gần nhất theo thời gian tạo) của một email.
     *
     * 👉 Ý nghĩa:
     * - Khi người dùng yêu cầu xác thực email (đăng ký, quên mật khẩu,...),
     *   hệ thống có thể đã gửi nhiều OTP trước đó.
     * - Hàm này giúp lấy **OTP mới nhất** để kiểm tra xem người dùng nhập đúng mã hợp lệ không.
     *
     * ⚙️ Query tự động được Spring Data JPA tạo ra:
     * SELECT * FROM otp_verification
     * WHERE email = :email
     * ORDER BY created_at DESC
     * LIMIT 1
     *
     * 💡 Giải thích cú pháp:
     * - `findTopBy...OrderBy...Desc` nghĩa là:
     *   → Lấy **bản ghi đầu tiên** (Top 1) sau khi sắp xếp giảm dần theo `createdAt`.
     * - `Optional<OtpVerification>` giúp tránh lỗi NullPointer (nếu không tìm thấy OTP nào).
     *
     * @param email địa chỉ email cần kiểm tra OTP
     * @return OTP gần nhất được gửi tới email này (nếu có)
     */
    Optional<OtpVerification> findTopByEmailOrderByCreatedAtDesc(String email);
}
