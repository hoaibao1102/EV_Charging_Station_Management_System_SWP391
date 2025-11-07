package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.ChargingStationRequest;
import com.swp391.gr3.ev_management.dto.response.ChargingStationResponse;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.enums.ChargingStationStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ChargingStationMapper;
import com.swp391.gr3.ev_management.repository.ChargingStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service // Đánh dấu class là một Spring Service (xử lý logic nghiệp vụ về trạm sạc)
@RequiredArgsConstructor // Tự động sinh constructor chứa các field final
public class ChargingStationServiceImpl implements ChargingStationService {

    // ====== Dependencies được inject qua constructor ======
    private final ChargingStationRepository chargingStationRepository; // Repository thao tác DB bảng ChargingStation
    private final ChargingStationMapper chargingStationMapper;         // Mapper chuyển đổi giữa Entity và DTO

    /**
     * Tìm trạm sạc theo ID.
     * - Gọi repository để lấy ChargingStation từ DB.
     * - Map entity sang DTO response để trả về.
     */
    @Override
    public ChargingStationResponse findByStationId(long id) {
        // Lấy thông tin trạm theo ID
        ChargingStation station = chargingStationRepository.findByStationId(id);
        // Map entity sang DTO response
        return chargingStationMapper.toResponse(station);
    }

    /**
     * Lấy danh sách tất cả các trạm sạc trong hệ thống.
     * - Gọi repository findAll().
     * - Map danh sách entity sang danh sách DTO bằng Stream API.
     */
    @Override
    public List<ChargingStationResponse> getAllStations() {
        return chargingStationRepository.findAll() // Lấy toàn bộ danh sách từ DB
                .stream()
                .map(chargingStationMapper::toResponse) // Map từng entity -> DTO
                .collect(Collectors.toList()); // Gom lại thành danh sách
    }

    /**
     * Tạo mới một trạm sạc.
     * - Map dữ liệu từ request sang entity.
     * - Đặt thời gian tạo nếu chưa có.
     * - Lưu vào DB và map sang DTO response để trả về.
     */
    @Override
    public ChargingStationResponse addChargingStation(ChargingStationRequest request) {
        // Map dữ liệu request sang entity
        ChargingStation station = chargingStationMapper.toEntity(request);

        // Nếu chưa có thời gian tạo (null) -> đặt thời gian hiện tại
        if (station.getCreatedAt() == null) {
            station.setCreatedAt(LocalDateTime.now());
        }

        // Lưu trạm mới vào cơ sở dữ liệu
        ChargingStation saved = chargingStationRepository.save(station);

        // Trả về DTO response sau khi lưu
        return chargingStationMapper.toResponse(saved);
    }

    /**
     * Cập nhật thông tin trạm sạc dựa theo ID.
     * - Tìm trạm hiện có trong DB.
     * - Nếu không tồn tại -> trả về null.
     * - Nếu có -> cập nhật các trường từ request và lưu lại.
     */
    @Override
    public ChargingStationResponse updateChargingStation(long id, ChargingStationRequest request) {
        // Tìm trạm hiện tại theo ID
        ChargingStation existing = chargingStationRepository.findByStationId(id);

        // Nếu không tìm thấy -> trả null (có thể đổi thành exception nếu muốn)
        if (existing == null) return null;

        // Cập nhật các thuộc tính của entity từ request thông qua mapper
        chargingStationMapper.updateEntity(existing, request);

        // Lưu bản ghi đã cập nhật vào DB
        ChargingStation updated = chargingStationRepository.save(existing);

        // Trả về DTO response đã cập nhật
        return chargingStationMapper.toResponse(updated);
    }

    /**
     * Cập nhật trạng thái hoạt động của trạm sạc.
     * - Kiểm tra trạm có tồn tại không.
     * - Nếu có, thay đổi status (ACTIVE, INACTIVE, MAINTENANCE, v.v...).
     * - Lưu lại và trả về DTO phản hồi.
     */
    @Override
    public ChargingStationResponse updateStationStatus(long stationId, ChargingStationStatus newStatus) {
        // Tìm trạm theo ID
        ChargingStation station = chargingStationRepository.findByStationId(stationId);

        // Nếu không tìm thấy -> ném lỗi
        if (station == null) {
            throw new ErrorException("Station not found with id: " + stationId);
        }

        // ⚡ Cập nhật trạng thái trạm và thời gian chỉnh sửa gần nhất
        station.setStatus(newStatus);
        station.setUpdatedAt(LocalDateTime.now());

        // Lưu thay đổi vào DB
        ChargingStation updated = chargingStationRepository.save(station);

        // Map sang DTO và trả về
        return chargingStationMapper.toResponse(updated);
    }
}
