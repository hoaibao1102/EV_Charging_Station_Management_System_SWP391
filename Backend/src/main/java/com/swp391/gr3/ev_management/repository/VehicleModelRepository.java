package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    /**
     * ✅ Kiểm tra xem model xe đã tồn tại trong hệ thống hay chưa.
     *
     * 👉 Ý nghĩa:
     * - Dùng khi tạo mới một VehicleModel để đảm bảo không bị trùng dữ liệu.
     * - So sánh theo 3 thuộc tính: brand (hãng xe), model (tên xe), và year (năm sản xuất).
     * - Sử dụng `IgnoreCase` để bỏ qua phân biệt chữ hoa/chữ thường.
     *
     * 💡 Ví dụ:
     * existsByBrandIgnoreCaseAndModelIgnoreCaseAndYear("Tesla", "Model 3", 2023)
     * → true nếu đã có xe Tesla Model 3 năm 2023 trong DB.
     */
    boolean existsByBrandIgnoreCaseAndModelIgnoreCaseAndYear(String brand, String model, int year);


    /**
     * ✅ Kiểm tra trùng model nhưng **loại trừ** một model cụ thể (theo modelId).
     *
     * 👉 Ý nghĩa:
     * - Dùng trong quá trình **update** model xe.
     * - Khi admin cập nhật thông tin model, cần đảm bảo không trùng với bản ghi khác.
     * - Ví dụ: Khi sửa "Tesla Model 3 (2023)", kiểm tra trùng nhưng **không tính chính nó**.
     *
     * 💡 Ví dụ:
     * existsByBrandIgnoreCaseAndModelIgnoreCaseAndYearAndModelIdNot("Tesla", "Model 3", 2023, 5L)
     * → true nếu có một bản ghi khác (không phải ID = 5) trùng thông tin.
     */
    boolean existsByBrandIgnoreCaseAndModelIgnoreCaseAndYearAndModelIdNot(String brand, String model, int year, Long modelId);


    /**
     * ✅ Tìm kiếm model xe theo nhiều tiêu chí tùy chọn (dynamic search).
     *
     * 👉 Ý nghĩa:
     * - Dùng cho chức năng tìm kiếm hoặc lọc danh sách model xe trong trang quản lý.
     * - Có thể truyền vào một hoặc nhiều tham số (nếu null thì bỏ qua điều kiện đó).
     *
     * ⚙️ JPQL Query:
     * SELECT vm FROM VehicleModel vm
     * WHERE
     *   (:brand IS NULL OR LOWER(vm.brand) LIKE LOWER(CONCAT('%', :brand, '%')))
     *   AND (:model IS NULL OR LOWER(vm.model) LIKE LOWER(CONCAT('%', :model, '%')))
     *   AND (:year IS NULL OR vm.year = :year)
     *   AND (:connectorTypeId IS NULL OR vm.connectorType.connectorTypeId = :connectorTypeId)
     *
     * 💡 Giải thích:
     * - `:brand IS NULL` → Nếu không truyền brand, bỏ qua điều kiện lọc theo brand.
     * - `LOWER(...) LIKE` → Cho phép tìm kiếm không phân biệt hoa thường và theo kiểu "chứa".
     * - `:connectorTypeId` → Lọc theo loại đầu sạc (connector type).
     *
     * 🧩 Ví dụ:
     * search("Tesla", null, null, null)
     * → Tìm tất cả các model thuộc hãng Tesla.
     *
     * search(null, "Model", 2023, 1)
     * → Tìm tất cả các xe có chữ "Model" trong tên, năm 2023, dùng connector type = 1.
     */
    @Query("SELECT vm FROM VehicleModel vm " +
            "WHERE (:brand IS NULL OR LOWER(vm.brand) LIKE LOWER(CONCAT('%', :brand, '%'))) " +
            "AND (:model IS NULL OR LOWER(vm.model) LIKE LOWER(CONCAT('%', :model, '%'))) " +
            "AND (:year IS NULL OR vm.year = :year) " +
            "AND (:connectorTypeId IS NULL OR vm.connectorType.connectorTypeId = :connectorTypeId)")
    List<VehicleModel> search(String brand, String model, Integer year, Integer connectorTypeId);
}
