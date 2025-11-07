package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.UserVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepisitory extends JpaRepository<UserVehicle, Long> {

    /**
     * ✅ Đếm số lượng xe (UserVehicle) thuộc một model cụ thể.
     *
     * 👉 Ý nghĩa:
     * - Dùng để kiểm tra xem có bao nhiêu xe đang sử dụng model đó.
     * - Ví dụ: trước khi xóa model, cần đảm bảo không có xe nào đang dùng model đó.
     *
     * ⚙️ Cơ chế:
     * - Sử dụng truy vấn tự động của Spring Data JPA.
     * - Dựa trên quan hệ giữa UserVehicle và VehicleModel (qua thuộc tính `model`).
     *
     * 💡 Ví dụ:
     * countByModel_ModelId(5L)
     * → Trả về số lượng xe có `modelId = 5`.
     *
     * 🧩 Ứng dụng:
     * - Trong Service/Controller để kiểm tra ràng buộc khi admin muốn xóa model xe.
     * - Tránh lỗi ràng buộc dữ liệu (foreign key constraint) khi model vẫn đang được dùng.
     */
    long countByModel_ModelId(Long modelId);
}
