package com.swp391.gr3.ev_management.repository;

import com.swp391.gr3.ev_management.entity.BookingSlotLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // ✅ Đánh dấu đây là một Spring Data Repository (tầng truy cập dữ liệu)
public interface BookingSlotLogRepository extends JpaRepository<BookingSlotLog, Long> {
    // ✅ Kế thừa JpaRepository<BookingSlotLog, Long> giúp ta có sẵn các hàm CRUD như:
    // findAll(), findById(), save(), deleteById(), existsById(), v.v.
    // Trong đó, entity chính là BookingSlotLog và khóa chính (ID) có kiểu Long.

    /**
     * ✅ Xóa bản ghi BookingSlotLog dựa theo bookingId của booking liên kết.
     *
     * Giả sử entity BookingSlotLog có quan hệ với entity Booking thông qua thuộc tính `booking`.
     * Khi đó `BookingSlotLog.booking.bookingId` là cách truy cập đến ID của booking.
     *
     * Spring Data JPA sẽ tự động tạo truy vấn SQL tương ứng, ví dụ:
     * DELETE FROM booking_slot_log WHERE booking_id = :bookingId;
     *
     * @param bookingId ID của booking mà bạn muốn xóa log liên quan.
     */
    void deleteByBooking_BookingId(Long bookingId);

    /**
     * ✅ Kiểm tra xem có tồn tại log nào (BookingSlotLog) gắn với một booking cụ thể hay không.
     *
     * Spring Data JPA tự sinh câu truy vấn:
     * SELECT COUNT(*) > 0 FROM booking_slot_log WHERE booking_id = :bookingId;
     *
     * @param bookingId ID của booking cần kiểm tra.
     * @return true nếu tồn tại ít nhất một log liên kết với bookingId này, ngược lại false.
     */
    boolean existsByBooking_BookingId(Long bookingId);
}
