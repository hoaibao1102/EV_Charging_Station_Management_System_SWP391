package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Transaction;
import com.swp391.gr3.ev_management.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    // ✅ Repository này quản lý entity "Transaction" — đại diện cho giao dịch thanh toán
    // (liên kết với hóa đơn, phiên sạc, và người dùng thông qua driver → vehicle → booking → session → invoice).

    /**
     * ✅ Lấy toàn bộ giao dịch (Transaction) của một người dùng (driver),
     *    đồng thời fetch (tải trước) tất cả các entity liên quan để tránh N+1 query problem.
     *
     * 👉 Ý nghĩa:
     * - Lấy danh sách các giao dịch thanh toán của một người dùng cụ thể.
     * - Sử dụng **JOIN FETCH** để lấy toàn bộ các thông tin liên quan đến giao dịch đó trong một truy vấn duy nhất.
     *   Cụ thể:
     *     - Transaction → Invoice
     *     - Invoice → ChargingSession
     *     - ChargingSession → Booking
     *     - Booking → Vehicle
     *     - Vehicle → Driver
     *     - Driver → User
     * - Điều này giúp tránh tình trạng “Lazy Loading” (N+1 problem), tức là phải truy vấn nhiều lần DB để lấy dữ liệu liên quan.
     *
     * ⚙️ JPQL Query:
     * SELECT DISTINCT t
     * FROM Transaction t
     *   JOIN FETCH t.invoice i
     *   JOIN FETCH i.session s
     *   JOIN FETCH s.booking b
     *   JOIN FETCH b.vehicle v
     *   JOIN FETCH v.driver d
     *   JOIN FETCH d.user u
     * WHERE u.userId = :userId
     * ORDER BY t.createdAt DESC
     *
     * 💡 Giải thích:
     * - `DISTINCT`: tránh bị trùng kết quả nếu có nhiều JOIN.
     * - `JOIN FETCH`: ép Hibernate load toàn bộ quan hệ chỉ trong 1 truy vấn.
     * - `order by t.createdAt desc`: sắp xếp giao dịch mới nhất lên đầu.
     *
     * 🧩 Dùng trong các màn hình như “Lịch sử thanh toán” của tài xế.
     *
     * @param userId ID của người dùng (User liên kết với Driver)
     * @return danh sách Transaction (bao gồm đầy đủ thông tin liên quan)
     */
    @Query("""
           select distinct t
           from Transaction t
             join fetch t.invoice i
             join fetch i.session s
             join fetch s.booking b
             join fetch b.vehicle v
             join fetch v.driver d
             join fetch d.user u
           where u.userId = :userId
           order by t.createdAt desc
           """)
    List<Transaction> findAllDeepGraphByDriverUserId(@Param("userId") Long userId);

    /**
     * ✅ Tính tổng số tiền (amount) của tất cả các giao dịch (Transaction)
     *    được tạo ra trong khoảng thời gian từ `start` đến `end`.
     *
     * 👉 Ý nghĩa:
     * - Dùng để thống kê tổng doanh thu trong một khoảng thời gian cụ thể.
     * - Truy vấn này sử dụng JPQL để tính tổng giá trị của trường `amount`
     *   trong bảng Transaction dựa trên điều kiện về thời gian tạo (`createdAt`).
     *
     * ⚙️ JPQL Query:
     * SELECT COALESCE(SUM(t.amount), 0)
     * FROM Transaction t
     * WHERE t.createdAt >= :start
     *   AND t.createdAt < :end
     *
     * 💡 Giải thích:
     * - `SUM(t.amount)`: tính tổng giá trị của trường `amount`.
     * - `COALESCE(..., 0)`: nếu không có giao dịch nào trong khoảng thời gian đó,
     *   trả về 0 thay vì null.
     * - Điều kiện `t.createdAt >= :start AND t.createdAt < :end` đảm bảo
     *   chỉ tính các giao dịch trong khoảng thời gian đã cho.
     *
     * 🧩 Dùng trong báo cáo tài chính, thống kê doanh thu.
     *
     * @param start thời điểm bắt đầu (inclusive)
     * @param end   thời điểm kết thúc (exclusive)
     * @return tổng số tiền của các giao dịch trong khoảng thời gian
     */
    @Query("""
           SELECT COALESCE(SUM(t.amount), 0)
           FROM Transaction t
           WHERE t.createdAt >= :start
             AND t.createdAt < :end
           """)
    Double sumAmountByCreatedAtBetween(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    List<Transaction> findTop5ByStatusOrderByCreatedAtDesc(TransactionStatus completed);
}