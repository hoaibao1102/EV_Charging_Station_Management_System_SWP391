package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.repository.StationStaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service // Đánh dấu class là Spring Service (xử lý nghiệp vụ liên quan StationStaff)
@RequiredArgsConstructor // Lombok tạo constructor cho các thuộc tính final
@Slf4j // Cung cấp logger
public class StationStaffServiceImpl implements StationStaffService {

    private final StationStaffRepository stationStaffRepository; // Repository truy vấn bảng StationStaff

    /**
     * Tìm StationStaff đang ACTIVE dựa theo staffId (khóa chính của bảng station_staff).
     * @param staffId ID của Staff trong bảng StationStaff
     * @return Optional<StationStaff> nếu tìm thấy
     */
    @Override
    public Optional<StationStaff> findActiveByStationStaffId(Long staffId) {
        // Gọi repository để tìm staff có trạng thái Active theo staffId
        return stationStaffRepository.findActiveByStationStaffId(staffId);
    }

    /**
     * Tìm StationStaff đang ACTIVE theo userId.
     * Một user có thể có nhiều StationStaff theo lịch sử station assignment,
     * nhưng chỉ có 1 record đang active.
     */
    @Override
    public Optional<StationStaff> findActiveByUserId(Long userId) {
        // Kiểm tra user nào đang làm việc tại station nào (Active)
        return stationStaffRepository.findActiveByUserId(userId);
    }

    /**
     * Alias method — tương tự findActiveByStationStaffId, chỉ khác tên.
     * Tìm ACTIVE staff theo staffId.
     */
    @Override
    public Optional<StationStaff> findActiveByStaffId(Long staffId) {
        // Tìm StationStaff đang active theo staffId
        return stationStaffRepository.findActiveByStaffId(staffId);
    }

    /**
     * Lưu một entity StationStaff vào DB.
     * Dùng cho việc cập nhật hoặc tạo mới.
     */
    @Override
    public void save(StationStaff active) {
        // Lưu entity (persist hoặc update)
        stationStaffRepository.save(active);
    }

    /**
     * Tạo mới hoặc cập nhật StationStaff rồi trả về entity sau khi lưu.
     */
    @Override
    public StationStaff saveStationStaff(StationStaff stationStaff) {
        // Lưu và trả về StationStaff sau khi persist
        return stationStaffRepository.save(stationStaff);
    }
}
