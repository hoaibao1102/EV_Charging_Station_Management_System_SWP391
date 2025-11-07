package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.CreateChargingPointRequest;
import com.swp391.gr3.ev_management.dto.request.StopChargingPointRequest;
import com.swp391.gr3.ev_management.dto.response.ChargingPointResponse;
import com.swp391.gr3.ev_management.entity.ChargingPoint;
import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ChargingPointMapper;
import com.swp391.gr3.ev_management.repository.ChargingPointRepository;
import com.swp391.gr3.ev_management.repository.ChargingStationRepository;
import com.swp391.gr3.ev_management.repository.ConnectorTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service // Đánh dấu lớp này là một Spring Service xử lý nghiệp vụ cho ChargingPoint
@RequiredArgsConstructor // Tự động tạo constructor cho các field final (DI)
@Slf4j // Cung cấp logger (log.info, log.warn, log.error, ...)
public class ChargingPointServiceImpl implements ChargingPointService {

    // ====== Dependencies (được Spring inject qua constructor) ======
    private final ChargingPointRepository pointRepository;           // CRUD cho ChargingPoint
    private final ChargingPointMapper chargingPointMapper;           // Map Entity <-> DTO
    private final ChargingStationRepository chargingStationRepository; // Kiểm tra & lấy Station
    private final ConnectorTypeRepository connectorTypeRepository;   // Kiểm tra & lấy ConnectorType

    @Override
    @Transactional // Thao tác dừng điểm sạc cần transaction để đảm bảo tính nhất quán
    public ChargingPointResponse stopChargingPoint(StopChargingPointRequest request) {

        // 1) Tìm ChargingPoint theo pointId; nếu không tồn tại -> ném lỗi
        ChargingPoint point = pointRepository.findById(request.getPointId())
                .orElseThrow(() -> new ErrorException("Charging point not found"));

        // 2) Không cho dừng nếu điểm sạc đang bận (OCCUPIED) để tránh cắt ngang phiên sạc
        if (point.getStatus() == ChargingPointStatus.OCCUPIED) {
            throw new RuntimeException("Cannot stop point while in use");
        }

        // 3) Cập nhật trạng thái theo yêu cầu (ví dụ: MAINTENANCE/INACTIVE/AVAILABLE ...)
        point.setStatus(request.getNewStatus());
        point.setUpdatedAt(LocalDateTime.now());
        pointRepository.save(point); // 4) Lưu lại thay đổi vào DB

        // 5) Trả về DTO phản hồi cho client
        return chargingPointMapper.toResponse(point);
    }

    @Override
    public List<ChargingPointResponse> getAllPoints() {
        // Lấy tất cả ChargingPoint từ DB, map sang DTO và trả về danh sách
        return pointRepository.findAll()
                .stream()
                .map(chargingPointMapper::toResponse)
                .toList();
    }

    @Override
    public List<ChargingPointResponse> getPointsByStationId(Long stationId) {
        log.info("Fetching charging points for stationId={}", stationId);

        // 1) Truy vấn tất cả điểm sạc thuộc một trạm theo stationId
        List<ChargingPoint> points = pointRepository.findByStation_StationId(stationId);

        // 2) Nếu không có dữ liệu -> log cảnh báo và trả danh sách rỗng
        if (points.isEmpty()) {
            log.warn("No charging points found for stationId={}", stationId);
            return List.of();
        }

        // 3) Map danh sách Entity sang DTO và trả về
        return chargingPointMapper.toResponses(points);
    }

    @Override
    @Transactional // Tạo mới điểm sạc gồm nhiều bước kiểm tra + lưu -> cần transaction
    public ChargingPointResponse createChargingPoint(CreateChargingPointRequest request) {
        // 1) Kiểm tra trạm tồn tại
        var station = chargingStationRepository.findById(request.getStationId())
                .orElseThrow(() -> new ErrorException("Station not found"));

        // 2) Kiểm tra loại đầu nối (connector type) tồn tại
        var connectorType = connectorTypeRepository.findById(request.getConnectorTypeId())
                .orElseThrow(() -> new ErrorException("Connector type not found"));

        // 3) Ràng buộc nghiệp vụ: không cho tạo điểm sạc với connector type đã bị deprecate
        if (Boolean.TRUE.equals(connectorType.getIsDeprecated())) {
            throw new ErrorException("Cannot create charging point: connector type is deprecated");
        }

        // 4) Chống trùng dữ liệu:
        //    - Trong cùng một trạm, pointNumber phải là duy nhất
        if (pointRepository.findByStation_StationIdAndPointNumber(
                request.getStationId(), request.getPointNumber()).isPresent()) {
            throw new ErrorException("Point number already exists in this station");
        }
        //    - SerialNumber toàn hệ thống phải là duy nhất
        if (pointRepository.findBySerialNumber(request.getSerialNumber()).isPresent()) {
            throw new ErrorException("Serial number already exists");
        }

        // 5) Map từ request sang Entity (gắn station & connectorType), sau đó lưu vào DB
        ChargingPoint point = chargingPointMapper.toEntity(request, station, connectorType);
        pointRepository.save(point);

        // 6) Map lại sang DTO và trả về cho client
        return chargingPointMapper.toResponse(point);
    }
}
