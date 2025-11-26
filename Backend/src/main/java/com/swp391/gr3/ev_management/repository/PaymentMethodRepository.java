package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.enums.PaymentProvider;
import com.swp391.gr3.ev_management.enums.PaymentType;
import com.swp391.gr3.ev_management.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod,Long> {
    // ✅ Repository này quản lý entity "PaymentMethod" — lưu thông tin phương thức thanh toán (ví dụ: VNPAY, EVM, thẻ ngân hàng,...)
    // ✅ Kế thừa JpaRepository => có sẵn các hàm CRUD (findAll, save, deleteById, findById, ...)

    /**
     * ✅ Tìm phương thức thanh toán theo loại (methodType) và nhà cung cấp (provider).
     *
     * 👉 Ý nghĩa:
     * - Dùng khi hệ thống cần lấy ra thông tin chi tiết của một phương thức thanh toán cụ thể,
     *   ví dụ: "PaymentType = E-WALLET" và "Provider = VNPAY".
     *
     * ⚙️ Query tự động được Spring Data JPA sinh ra:
     * SELECT * FROM payment_method
     * WHERE method_type = :methodType
     *   AND provider = :provider
     * LIMIT 1
     *
     * 💡 `Optional<PaymentMethod>` giúp tránh lỗi NullPointer nếu không tìm thấy.
     *
     * @param methodType loại phương thức thanh toán (ví dụ: E-WALLET, BANK, INTERNAL)
     * @param provider nhà cung cấp (ví dụ: VNPAY, MOMO, EVM)
     * @return Optional chứa PaymentMethod nếu tồn tại
     */
    Optional<PaymentMethod> findByMethodTypeAndProvider(PaymentType methodType, PaymentProvider provider);


    /**
     * ✅ Kiểm tra xem một phương thức thanh toán có tồn tại hay chưa,
     *    dựa vào loại, nhà cung cấp và số tài khoản.
     *
     * 👉 Ý nghĩa:
     * - Dùng để ngăn việc thêm trùng một phương thức thanh toán (ví dụ: cùng provider và accountNo).
     * - Thường được dùng khi admin tạo mới hoặc cập nhật danh sách phương thức thanh toán.
     *
     * ⚙️ Query tự động sinh ra:
     * SELECT COUNT(*) > 0
     * FROM payment_method
     * WHERE method_type = :methodType
     *   AND provider = :provider
     *   AND account_no = :accountNo
     *
     * 💡 Trả về true nếu tồn tại, false nếu chưa có.
     *
     * @param methodType loại phương thức (E-WALLET, BANK, INTERNAL,...)
     * @param provider nhà cung cấp (VNPAY, MOMO, EVM,...)
     * @param accountNo số tài khoản (accountNo hoặc mã định danh)
     * @return true nếu phương thức thanh toán đã tồn tại, ngược lại false
     */
    boolean existsByMethodTypeAndProviderAndAccountNo(
            PaymentType methodType, PaymentProvider provider, String accountNo
    );

    Optional<PaymentMethod> findByProvider(PaymentProvider evm);
}
