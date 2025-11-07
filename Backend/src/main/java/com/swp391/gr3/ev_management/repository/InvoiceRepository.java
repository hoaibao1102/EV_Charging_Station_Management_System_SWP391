package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice,Long> {
    // ✅ Repository này quản lý entity "Invoice" (hóa đơn thanh toán)
    // Kế thừa JpaRepository => có sẵn các hàm CRUD cơ bản: save, findById, delete, findAll, ...

    /**
     * ✅ Tìm hóa đơn theo session ID.
     *
     * 👉 Ý nghĩa:
     * - Mỗi phiên sạc (ChargingSession) chỉ có 1 hóa đơn đi kèm.
     * - Dùng để lấy hóa đơn của phiên sạc cụ thể khi cần xác nhận thanh toán hoặc tạo giao dịch.
     *
     * ⚙️ Cách hoạt động:
     * - Truy vấn theo mối quan hệ: Invoice → Session → sessionId.
     *
     * @param sessionId ID của phiên sạc
     * @return Optional chứa hóa đơn nếu tìm thấy
     */
    Optional<Invoice> findBySession_SessionId(Long sessionId);


    /**
     * ✅ Lấy danh sách hóa đơn chưa thanh toán (UNPAID) của một trạm sạc cụ thể.
     *
     * 👉 Ý nghĩa:
     * - Dùng trong giao diện quản lý của admin/staff để kiểm tra các hóa đơn chưa được thanh toán tại trạm.
     *
     * 🔍 JPQL:
     * SELECT i FROM Invoice i
     * WHERE i.session.booking.station.stationId = :stationId
     *   AND i.status = 'unpaid'
     * ORDER BY i.issuedAt DESC
     *
     * ⚙️ Cách hoạt động:
     * - Join ngầm: Invoice → Session → Booking → Station để lọc theo stationId.
     * - Chỉ lấy các hóa đơn có status = 'unpaid'.
     * - Sắp xếp theo ngày phát hành mới nhất.
     *
     * @param stationId ID của trạm sạc
     * @return Danh sách hóa đơn chưa thanh toán tại trạm
     */
    @Query("SELECT i FROM Invoice i " +
            "WHERE i.session.booking.station.stationId = :stationId " +
            "AND i.status = 'unpaid' " +
            "ORDER BY i.issuedAt DESC")
    List<Invoice> findUnpaidInvoicesByStation(@Param("stationId") Long stationId);


    /**
     * ✅ Tính tổng doanh thu (sum amount) của tất cả hóa đơn trong khoảng thời gian nhất định.
     *
     * 👉 Ý nghĩa:
     * - Dùng để thống kê doanh thu theo ngày, tuần, tháng, quý, ...
     * - Chỉ tính những hóa đơn có ngày phát hành nằm trong khoảng `from` → `to`.
     *
     * 🔍 JPQL:
     * SELECT COALESCE(SUM(i.amount), 0)
     * FROM Invoice i
     * WHERE i.issuedAt BETWEEN :from AND :to
     *
     * ⚙️ Ghi chú:
     * - `COALESCE(..., 0)` để tránh trả về null nếu không có hóa đơn nào.
     *
     * @param from thời gian bắt đầu
     * @param to thời gian kết thúc
     * @return tổng tiền (double)
     */
    @Query("""
      SELECT COALESCE(SUM(i.amount), 0)
      FROM Invoice i
      WHERE i.issuedAt BETWEEN :from AND :to
    """)
    double sumAmountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);


    /**
     * ✅ Tính tổng doanh thu toàn hệ thống (từ trước tới nay).
     *
     * 👉 Ý nghĩa:
     * - Dùng để hiển thị tổng doanh thu trên dashboard tổng quan cho admin.
     *
     * 🔍 JPQL:
     * SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i
     *
     * ⚙️ Trả về 0 nếu chưa có hóa đơn nào.
     *
     * @return tổng doanh thu toàn hệ thống
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i")
    double sumAll();


    /**
     * ✅ Tính tổng doanh thu của một trạm cụ thể trong khoảng thời gian xác định.
     *
     * 👉 Ý nghĩa:
     * - Dùng để thống kê doanh thu riêng của từng trạm (ví dụ: trạm A trong tháng 10/2025).
     *
     * 🔍 JPQL:
     * SELECT COALESCE(SUM(i.amount), 0)
     * FROM Invoice i
     * JOIN i.session s
     * JOIN s.booking b
     * WHERE b.station.stationId = :stationId
     *   AND i.issuedAt BETWEEN :from AND :to
     *
     * ⚙️ Cách hoạt động:
     * - Join từ Invoice → Session → Booking → Station để lọc theo trạm.
     * - Chỉ tính hóa đơn có issuedAt trong khoảng thời gian.
     *
     * @param stationId ID trạm
     * @param from thời gian bắt đầu
     * @param to thời gian kết thúc
     * @return tổng doanh thu của trạm trong khoảng thời gian
     */
    @Query("""
      SELECT COALESCE(SUM(i.amount), 0)
      FROM Invoice i
      JOIN i.session s
      JOIN s.booking b
      WHERE b.station.stationId = :stationId
        AND i.issuedAt BETWEEN :from AND :to
    """)
    double sumByStationBetween(@Param("stationId") Long stationId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

}
