package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.DriverViolationTripletResponse;
import com.swp391.gr3.ev_management.enums.TripletStatus;
import com.swp391.gr3.ev_management.mapper.DriverViolationTripletMapper;
import com.swp391.gr3.ev_management.repository.DriverViolationTripletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service // Đánh dấu là Spring Service (chứa logic nghiệp vụ)
@RequiredArgsConstructor // Tự động tạo constructor cho các field final
@Transactional(readOnly = true) // Mặc định các method chỉ đọc (đỡ lock/ tăng hiệu năng)
public class DriverViolationTripletServiceImpl implements DriverViolationTripletService {

    private final DriverViolationTripletRepository tripletRepository; // Repository truy vấn Triplet (violation + driver + invoice)
    private final DriverViolationTripletMapper tripletMapper;         // Mapper chuyển Entity -> Response DTO

    @Override
    public List<DriverViolationTripletResponse> getAllTriplets() {
        // Lấy tất cả Triplet đã join sẵn Driver & User để trả về đủ thông tin hiển thị
        return tripletRepository.findAllWithDriverAndUser()
                .stream()
                .map(tripletMapper::toResponse) // Map từng entity sang DTO
                .toList();
    }

    @Override
    public List<DriverViolationTripletResponse> getTripletsByUserPhone(String phoneNumber) {
        // Tìm các Triplet theo số điện thoại của User
        return tripletRepository.findByUserPhoneNumber(phoneNumber)
                .stream()
                .map(tripletMapper::toResponse) // Map entity -> DTO
                .toList();
    }

    @Override
    @Transactional // Ghi DB nên cần tắt readOnly để cập nhật
    public DriverViolationTripletResponse updateTripletStatusToPaid(Long tripletId) {
        // Tìm Triplet theo id; nếu không có -> ném lỗi
        var triplet = tripletRepository.findById(tripletId)
                .orElseThrow(() -> new RuntimeException("Triplet not found with id: " + tripletId));

        // Cập nhật trạng thái sang PAID (đã thanh toán)
        triplet.setStatus(TripletStatus.PAID);
        // Ghi nhận thời điểm đóng Triplet (kết thúc vòng đời)
        triplet.setClosedAt(java.time.LocalDateTime.now());

        // Lưu thay đổi
        tripletRepository.save(triplet);

        // Trả về DTO phản hồi
        return tripletMapper.toResponse(triplet);
    }

    @Override
    @Transactional // Ghi DB nên cần tắt readOnly để cập nhật
    public DriverViolationTripletResponse updateTripletStatusToCanceled(Long tripletId) {
        // Tìm Triplet theo id; nếu không có -> ném lỗi
        var triplet = tripletRepository.findById(tripletId)
                .orElseThrow(() -> new RuntimeException("Triplet not found with id: " + tripletId));

        // Cập nhật trạng thái sang CANCELED (hủy, không tiếp tục xử lý)
        triplet.setStatus(TripletStatus.CANCELED);
        // Ghi nhận thời điểm đóng Triplet
        triplet.setClosedAt(java.time.LocalDateTime.now());

        // Lưu thay đổi
        tripletRepository.save(triplet);

        // Trả về DTO phản hồi
        return tripletMapper.toResponse(triplet);
    }
}
