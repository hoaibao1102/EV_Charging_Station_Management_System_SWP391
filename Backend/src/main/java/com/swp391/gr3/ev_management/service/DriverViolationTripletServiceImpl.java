package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.DriverViolationTripletResponse;
import com.swp391.gr3.ev_management.entity.DriverViolationTriplet;
import com.swp391.gr3.ev_management.enums.TripletStatus;
import com.swp391.gr3.ev_management.mapper.DriverViolationTripletMapper;
import com.swp391.gr3.ev_management.repository.DriverViolationTripletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service // Đánh dấu là Spring Service (chứa logic nghiệp vụ)
@RequiredArgsConstructor // Tự động tạo constructor cho các field final
@Transactional(readOnly = true) // Mặc định các method chỉ đọc (đỡ lock/ tăng hiệu năng)
public class DriverViolationTripletServiceImpl implements DriverViolationTripletService {

    // Repository thao tác với bảng DriverViolationTriplet (CRUD + query custom)
    private final DriverViolationTripletRepository tripletRepository; // Repository truy vấn Triplet (violation + driver + invoice)
    // Mapper chuyển Entity DriverViolationTriplet -> DriverViolationTripletResponse (DTO) để trả ra API
    private final DriverViolationTripletMapper tripletMapper;         // Mapper chuyển Entity -> Response DTO

    @Override
    public List<DriverViolationTripletResponse> getAllTriplets() {
        // 1️⃣ Gọi repository để lấy tất cả DriverViolationTriplet từ DB
        //    - findAllWithDriverAndUser(): thường là query đã join fetch Driver & User để tránh N+1
        return tripletRepository.findAllWithDriverAndUser()
                .stream()                        // 2️⃣ Chuyển sang Stream để xử lý từng phần tử
                .map(tripletMapper::toResponse)  // 3️⃣ Map từng entity Triplet -> DTO DriverViolationTripletResponse
                .toList();                       // 4️⃣ Thu kết quả về List
    }

    @Override
    public List<DriverViolationTripletResponse> getTripletsByUserPhone(String phoneNumber) {
        // 1️⃣ Gọi repository để tìm danh sách Triplet theo số điện thoại của User
        //    - findByUserPhoneNumber(phoneNumber) là query custom dựa trên quan hệ User.phoneNumber
        return tripletRepository.findByUserPhoneNumber(phoneNumber)
                .stream()                        // 2️⃣ Stream kết quả
                .map(tripletMapper::toResponse)  // 3️⃣ Map từng entity -> DTO response để trả cho client
                .toList();                       // 4️⃣ Gom thành List
    }

    @Override
    @Transactional // Ghi DB nên cần tắt readOnly để cập nhật
    public DriverViolationTripletResponse updateTripletStatusToPaid(Long tripletId) {
        // 1️⃣ Tìm Triplet theo id; nếu không tồn tại -> ném RuntimeException (có thể được controller bắt và trả 404)
        var triplet = tripletRepository.findById(tripletId)
                .orElseThrow(() -> new RuntimeException("Triplet not found with id: " + tripletId));

        // 2️⃣ Cập nhật trạng thái Triplet sang PAID (đã thanh toán vi phạm)
        triplet.setStatus(TripletStatus.PAID);
        // 3️⃣ Ghi nhận thời điểm đóng Triplet (đánh dấu thời điểm kết thúc vòng đời của vi phạm này)
        triplet.setClosedAt(java.time.LocalDateTime.now());

        // 4️⃣ Lưu thay đổi trạng thái & closedAt xuống DB
        tripletRepository.save(triplet);

        // 5️⃣ Map entity đã cập nhật sang DTO và trả về cho client
        return tripletMapper.toResponse(triplet);
    }

    @Override
    @Transactional // Ghi DB nên cần tắt readOnly để cập nhật
    public DriverViolationTripletResponse updateTripletStatusToCanceled(Long tripletId) {
        // 1️⃣ Tìm Triplet theo id; nếu không tồn tại -> ném RuntimeException
        var triplet = tripletRepository.findById(tripletId)
                .orElseThrow(() -> new RuntimeException("Triplet not found with id: " + tripletId));

        // 2️⃣ Cập nhật trạng thái sang CANCELED (vi phạm này bị hủy/bỏ, không tiếp tục xử lý)
        triplet.setStatus(TripletStatus.CANCELED);
        // 3️⃣ Ghi nhận thời điểm đóng Triplet (tương tự PAID, nhưng lý do là CANCELED)
        triplet.setClosedAt(java.time.LocalDateTime.now());

        // 4️⃣ Lưu thay đổi vào DB
        tripletRepository.save(triplet);

        // 5️⃣ Map entity đã cập nhật sang DTO để trả về
        return tripletMapper.toResponse(triplet);
    }

    @Override
    public boolean existsByViolation(Long violationId) {
        // 1️⃣ Kiểm tra xem với violationId này đã có Triplet nào tồn tại chưa
        //    - Dùng cho nghiệp vụ tránh tạo trùng bộ ba (driver - violation - invoice)
        return tripletRepository.existsByViolation(violationId);
    }

    @Override
    public Collection<DriverViolationTriplet> findOpenByDriver(Long driverId) {
        // 1️⃣ Lấy tất cả Triplet đang "open"/chưa đóng của 1 driver (theo driverId)
        //    - Thường phục vụ logic: driver còn bao nhiêu vi phạm chưa xử lý
        return tripletRepository.findOpenByDriver(driverId);
    }

    @Override
    public DriverViolationTriplet save(DriverViolationTriplet triplet) {
        // 1️⃣ Lưu hoặc cập nhật 1 Triplet (entity) bất kỳ được truyền vào
        //    - Dùng cho các luồng nghiệp vụ khác tạo/modify Triplet
        return tripletRepository.save(triplet);
    }

    @Override
    public void addDriverViolationTriplet(DriverViolationTriplet triplet) {
        // 1️⃣ Hàm tiện ích thêm 1 Triplet mới vào DB (không trả về)
        //    - Có thể dùng trong các rule tạo Triplet khi phát hiện vi phạm
        tripletRepository.save(triplet);
    }
}
