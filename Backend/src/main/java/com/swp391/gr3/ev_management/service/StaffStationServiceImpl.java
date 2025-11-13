package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.response.StationStaffResponse;
import com.swp391.gr3.ev_management.entity.StationStaff;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.StationStaffResponseMapper;
import com.swp391.gr3.ev_management.repository.StationStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service // Đánh dấu class là Service (chứa nghiệp vụ quản lý Staff tại các Station)
@RequiredArgsConstructor // Lombok tạo constructor để inject các phụ thuộc final
public class StaffStationServiceImpl implements StaffStationService {

    private final StationStaffRepository stationStaffRepository;           // Repository truy vấn StationStaff
    private final ChargingStationService chargingStationService;           // Service để tìm Station
    private final StationStaffResponseMapper stationStaffResponseMapper;   // Mapper chuyển Entity -> Response DTO

    /**
     * Lấy thông tin StationStaff theo userId.
     * - Repository trả về projection StationStaffResponse nên chỉ cần trả đúng dữ liệu.
     */
    @Override
    public StationStaffResponse getStaffByUserId(Long userId) {
        // Tìm Staff theo userId, có thể trả về null nếu không tìm thấy
        return stationStaffRepository.findByUserId(userId).orElse(null);
    }

    /**
     * Cập nhật station cho 1 staff dựa trên staffId và stationId.
     * - Tìm staff hiện tại theo staffId.
     * - Tìm trạm mới theo stationId.
     * - Nếu giống trạm cũ → trả về luôn thông tin cũ.
     * - Ngược lại, cập nhật record StationStaff và set thời gian assigned/unassigned.
     */
    @Transactional
    @Override
    public StationStaffResponse updateStation(Long staffId, Long stationId) {
        // 1) Kiểm tra staff theo staffId, nếu không có -> ném lỗi
        StationStaff ss = stationStaffRepository.findEntityByStaffId(staffId)
                .orElseThrow(() -> new ErrorException("Staff not found with staffId " + staffId));

        // 2) Tìm trạm theo stationId, nếu không có -> lỗi
        var newStation = chargingStationService.findById(stationId)
                .orElseThrow(() -> new ErrorException("Station not found with id " + stationId));

        // 3) Nếu staff đã thuộc đúng trạm cần update → không làm gì thêm, trả dữ liệu đang có
        if (ss.getStation() != null && ss.getStation().getStationId().equals(stationId)) {
            return stationStaffRepository.findByStaffId(staffId)
                    .orElseThrow(() -> new ErrorException("Failed to load staff after update"));
        }

        // 4) Cập nhật station mới cho staff
        ss.setStation(newStation);

        // Ghi nhận mốc thời gian thay đổi (tùy mục đích bạn sử dụng)
        ss.setAssignedAt(LocalDateTime.now());   // thời điểm được gán vào station mới
        ss.setUnassignedAt(LocalDateTime.now()); // thời điểm rời trạm cũ (nếu dùng để lưu lịch sử)

        // 5) Lưu lại thay đổi
        stationStaffRepository.save(ss);

        // 6) Trả lại dữ liệu projection cho client
        return stationStaffRepository.findByStaffId(staffId)
                .orElseThrow(() -> new ErrorException("Failed to load staff after update"));
    }

    /**
     * Lấy danh sách toàn bộ StationStaff (đã map sang response).
     */
    @Override
    public List<StationStaffResponse> getAll() {
        return stationStaffRepository.findAll()              // lấy toàn bộ entity
                .stream()
                .map(stationStaffResponseMapper::mapToResponse) // map sang response DTO
                .toList();
    }

    /**
     * Lấy danh sách staff theo userId (một user có thể có nhiều record StationStaff).
     */
    @Override
    @Transactional(readOnly = true)
    public List<StationStaffResponse> getByStationStaffUserId(Long userId) {
        return stationStaffRepository.findStationStaffByUserId(userId) // list projection/entity
                .stream()
                .map(stationStaffResponseMapper::mapToResponse)        // map sang DTO
                .toList();
    }
}
