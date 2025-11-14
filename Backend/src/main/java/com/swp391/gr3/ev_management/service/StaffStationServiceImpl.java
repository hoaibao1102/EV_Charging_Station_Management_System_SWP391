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
    // 1) Lấy assignment đang ACTIVE (unassignedAt IS NULL). Nếu không có -> lỗi.
    StationStaff active = stationStaffRepository.findActiveByStaffId(staffId)
        .orElseThrow(() -> new ErrorException("Active assignment not found for staffId " + staffId));

    // 2) Tìm trạm theo stationId, nếu không có -> lỗi
    var newStation = chargingStationService.findById(stationId)
        .orElseThrow(() -> new ErrorException("Station not found with id " + stationId));

    // 3) Nếu đã cùng trạm thì trả về luôn dữ liệu hiện tại
    if (active.getStation() != null && active.getStation().getStationId().equals(stationId)) {
        return stationStaffResponseMapper.mapToResponse(active);
    }

    // 4) Đánh dấu bản ghi active hiện tại là unassigned (kết thúc assignment cũ)
    active.setUnassignedAt(LocalDateTime.now());
    stationStaffRepository.save(active);

    // 5) Tạo bản ghi mới cho assignment (preserve history)
    StationStaff fresh = StationStaff.builder()
        .assignedAt(LocalDateTime.now())
        .unassignedAt(null)
        .staff(active.getStaff())
        .station(newStation)
        .build();

    StationStaff saved = stationStaffRepository.save(fresh);

    // 6) Trả về DTO của bản ghi mới
    return stationStaffResponseMapper.mapToResponse(saved);
    }

    /**
     * Lấy danh sách toàn bộ StationStaff (đã map sang response).
     */
    @Override
    public List<StationStaffResponse> getAll() {
    // Only return active assignments (unassignedAt == null) so frontend shows the
    // currently assigned station per staff instead of historical records.
    return stationStaffRepository.findAll()
        .stream()
        .filter(s -> s.getUnassignedAt() == null)
        .map(stationStaffResponseMapper::mapToResponse)
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
