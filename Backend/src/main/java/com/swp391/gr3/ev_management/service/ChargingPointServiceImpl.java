package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.CreateChargingPointRequest;
import com.swp391.gr3.ev_management.dto.request.StopChargingPointRequest;
import com.swp391.gr3.ev_management.dto.response.ChargingPointResponse;
import com.swp391.gr3.ev_management.entity.ChargingPoint;
import com.swp391.gr3.ev_management.enums.ChargingPointStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ChargingPointMapper;
import com.swp391.gr3.ev_management.repository.ChargingPointRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service // Đánh dấu lớp này là một Spring Service xử lý nghiệp vụ cho ChargingPoint
@RequiredArgsConstructor // Tự động tạo constructor cho các field final (DI)
@Slf4j // Cung cấp logger (log.info, log.warn, log.error, ...)
public class ChargingPointServiceImpl implements ChargingPointService {

    // ====== Dependencies (được Spring inject qua constructor) ======
    private final ChargingPointRepository chargingPointRepository;          // Repository thao tác DB cho bảng ChargingPoint (CRUD, query)
    private final ChargingPointMapper chargingPointMapper;                  // Mapper chuyển đổi giữa Entity <-> DTO (request/response)
    private final ChargingStationService chargingStationService;            // Service xử lý nghiệp vụ liên quan tới ChargingStation (trạm sạc)
    private final ConnectorTypeService connectorTypeService;                // Service xử lý nghiệp vụ liên quan tới ConnectorType (loại đầu cắm)

    @Override
    @Transactional // Thao tác dừng điểm sạc cần transaction để đảm bảo tính nhất quán dữ liệu
    public ChargingPointResponse stopChargingPoint(StopChargingPointRequest request) {

        // 1) Tìm ChargingPoint theo pointId gửi từ client trong request
        //    - request.getPointId(): ID của điểm sạc cần dừng
        //    - findById(...) trả về Optional<ChargingPoint>
        //    - orElseThrow(...) nếu không có -> ném ErrorException với message "Charging point not found"
        ChargingPoint point = chargingPointRepository.findById(request.getPointId())
                .orElseThrow(() -> new ErrorException("Charging point not found"));

        

        // 3) Cập nhật trạng thái ChargingPoint theo newStatus trong request
        //    - newStatus có thể là: MAINTENANCE, INACTIVE, AVAILABLE, ...
        //    - Quy tắc nghiệp vụ: front-end/back-end phải đảm bảo truyền vào trạng thái hợp lệ
        point.setStatus(request.getNewStatus());

        // 4) Cập nhật thời gian updatedAt để log lại lúc thay đổi trạng thái
        point.setUpdatedAt(LocalDateTime.now());

        // 5) Lưu entity đã cập nhật trở lại DB
        //    - Vì có @Transactional nên nếu ở giữa có lỗi, toàn bộ transaction sẽ rollback
        chargingPointRepository.save(point); // 4) Lưu lại thay đổi vào DB

        // 6) Map từ Entity ChargingPoint -> ChargingPointResponse DTO
        //    - DTO dùng để trả dữ liệu về cho client (ẩn bớt field nhạy cảm, format đẹp hơn, ...)
        return chargingPointMapper.toResponse(point);
    }

    @Override
    public List<ChargingPointResponse> getAllPoints() {
        // 1) Lấy toàn bộ bản ghi ChargingPoint từ DB bằng findAll()
        //    - Trả về List<ChargingPoint>
        // 2) Chuyển từng ChargingPoint -> ChargingPointResponse bằng mapper
        // 3) Thu kết quả về List<ChargingPointResponse> bằng stream().toList()
        return chargingPointRepository.findAll()
                .stream()                            // chuyển sang stream để dùng map
                .map(chargingPointMapper::toResponse) // map từng entity -> response DTO
                .toList();                           // thu về danh sách
    }

    @Override
    public List<ChargingPointResponse> getPointsByStationId(Long stationId) {
        log.info("Fetching charging points for stationId={}", stationId); // Log để debug/monitor API

        // 1) Truy vấn tất cả ChargingPoint thuộc về một station cụ thể
        //    - finder method: findByStation_StationId(...) do Spring Data JPA generate theo tên
        //    - stationId: ID của trạm sạc mà ta muốn lấy danh sách điểm sạc
        List<ChargingPoint> points = chargingPointRepository.findByStation_StationId(stationId);

        // 2) Nếu không tìm thấy điểm sạc nào cho stationId này
        //    - Ghi log cảnh báo (level WARN) để dễ tracking vấn đề cấu hình dữ liệu/trạm
        //    - Trả về List rỗng (List.of()) thay vì null để tránh NullPointerException ở client
        if (points.isEmpty()) {
            log.warn("No charging points found for stationId={}", stationId);
            return List.of();
        }

        // 3) Nếu có dữ liệu:
        //    - Dùng mapper để chuyển List<ChargingPoint> -> List<ChargingPointResponse>
        //    - chargingPointMapper.toResponses(points): method mapper hỗ trợ convert danh sách
        return chargingPointMapper.toResponses(points);
    }

    @Override
    @Transactional // Tạo mới điểm sạc gồm nhiều bước kiểm tra + lưu -> cần transaction để rollback nếu lỗi
    public ChargingPointResponse createChargingPoint(CreateChargingPointRequest request) {
        // 1) Kiểm tra trạm sạc (ChargingStation) có tồn tại không
        //    - request.getStationId(): ID trạm mà điểm sạc này thuộc về
        //    - Nếu không tồn tại -> ném ErrorException("Station not found")
        var station = chargingStationService.findById(request.getStationId())
                .orElseThrow(() -> new ErrorException("Station not found"));

        // 2) Kiểm tra loại đầu nối (ConnectorType) có tồn tại không
        //    - request.getConnectorTypeId(): ID của loại đầu cắm (AC, DC, CCS, CHAdeMO, ...)
        //    - Nếu không tồn tại -> ném ErrorException("Connector type not found")
        var connectorType = connectorTypeService.findById(request.getConnectorTypeId())
                .orElseThrow(() -> new ErrorException("Connector type not found"));

        // 3) Ràng buộc nghiệp vụ:
        //    - Không cho phép tạo điểm sạc mới nếu connector type đã bị đánh dấu deprecated
        //    - getIsDeprecated(): field Boolean xác định loại đầu nối có còn được dùng hay không
        //    - Boolean.TRUE.equals(...) để tránh NullPointerException khi isDeprecated có thể null
        if (Boolean.TRUE.equals(connectorType.getIsDeprecated())) {
            throw new ErrorException("Cannot create charging point: connector type is deprecated");
        }

        // 4) Chống trùng dữ liệu: kiểm tra các ràng buộc unique trước khi insert
        // 4.1) Trong cùng một trạm (station), pointNumber phải là duy nhất
        //      - Ví dụ: Station A, pointNumber = 1 chỉ được tồn tại đúng 1 điểm sạc
        //      - findByStation_StationIdAndPointNumber(...) trả về Optional
        //      - Nếu isPresent() -> đã tồn tại -> ném ErrorException
        if (chargingPointRepository.findByStation_StationIdAndPointNumber(
                request.getStationId(), request.getPointNumber()).isPresent()) {
            throw new ErrorException("Point number already exists in this station");
        }

        // 4.2) SerialNumber là duy nhất trên toàn hệ thống
        //      - SerialNumber là mã định danh phần cứng của bộ sạc (thường in trên thiết bị)
        //      - Không được trùng nhau để tránh nhầm lẫn thiết bị khi tích hợp OCPP/điều khiển từ xa
        if (chargingPointRepository.findBySerialNumber(request.getSerialNumber()).isPresent()) {
            throw new ErrorException("Serial number already exists");
        }

        // 5) Khi đã pass tất cả validation:
        //    - Dùng mapper để map CreateChargingPointRequest + station + connectorType -> Entity ChargingPoint
        //    - toEntity(...) sẽ set đầy đủ các field: station, connectorType, status default, createdAt, ...
        ChargingPoint point = chargingPointMapper.toEntity(request, station, connectorType);

        // 6) Lưu entity mới vào DB
        //    - Sau lệnh save, entity có thể được fill thêm ID (tự tăng) và các field managed khác
        chargingPointRepository.save(point);

        // 7) Map entity đã lưu -> DTO để trả lại cho client
        //    - toResponse(...) sẽ build ChargingPointResponse phù hợp (ẩn bớt thông tin không cần thiết)
        return chargingPointMapper.toResponse(point);
    }

    @Override
    public List<ChargingPoint> findByStation_StationId(Long stationId) {
        // Hàm tiện ích: trả về trực tiếp Entity ChargingPoint theo stationId
        // Được các service khác dùng để xử lý logic nâng cao hơn (không cần DTO)
        return chargingPointRepository.findByStation_StationId(stationId);
    }

    @Override
    public List<ChargingPoint> findByStation_StationIdAndConnectorType_ConnectorTypeId(Long stationId, Long connectorTypeId) {
        // Hàm tiện ích: lấy danh sách ChargingPoint theo cả stationId và connectorTypeId
        // Ví dụ: cần tìm tất cả điểm sạc DC tại một trạm cụ thể để hiển thị / tính năng đặt chỗ
        return chargingPointRepository.findByStation_StationIdAndConnectorType_ConnectorTypeId(stationId, connectorTypeId);
    }

    @Override
    public Map<String, Long> countGroupByStatus() {
        // Lấy tất cả điểm sạc rồi group theo status và đếm số lượng
        return chargingPointRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        point -> point.getStatus().name(),   // key: tên enum (AVAILABLE, OCCUPIED, ...)
                        Collectors.counting()                // value: số lượng
                ));
    }

    @Override
    public ChargingPointResponse getPointById (Long pointId) {
        // Tìm điểm sạc theo ID và map sang DTO để trả về
        ChargingPoint point = chargingPointRepository.findById(pointId)
                .orElseThrow(() -> new ErrorException("Charging point not found"));
        return chargingPointMapper.toResponse(point);
    }

    @Override
    @Transactional
    public ChargingPointResponse updateChargingPoint(Long pointId, CreateChargingPointRequest request){
        // 1) Lấy điểm sạc hiện tại
        ChargingPoint point = chargingPointRepository.findById(pointId)
                .orElseThrow(() -> new ErrorException("Charging point not found"));

        // 2) Validate trạm sạc
        var station = chargingStationService.findById(request.getStationId())
                .orElseThrow(() -> new ErrorException("Station not found"));

        // 3) Validate loại đầu nối
        var connectorType = connectorTypeService.findById(request.getConnectorTypeId())
                .orElseThrow(() -> new ErrorException("Connector type not found"));

        // 4) Không cho dùng loại đầu nối đã deprecated
        if (Boolean.TRUE.equals(connectorType.getIsDeprecated())) {
            throw new ErrorException("Cannot update charging point: connector type is deprecated");
        }

        // 5) Check trùng pointNumber trong cùng station (chỉ check nếu có thay đổi)
        if (!point.getPointNumber().equals(request.getPointNumber())) {
            if (chargingPointRepository.findByStation_StationIdAndPointNumber(
                    request.getStationId(), request.getPointNumber()).isPresent()) {
                throw new ErrorException("Point number already exists in this station");
            }
        }

        // 6) Check trùng serialNumber toàn hệ thống (chỉ check nếu có thay đổi)
        if (!point.getSerialNumber().equals(request.getSerialNumber())) {
            if (chargingPointRepository.findBySerialNumber(request.getSerialNumber()).isPresent()) {
                throw new ErrorException("Serial number already exists");
            }
        }

        // 7) Gán lại dữ liệu vào entity
        point.setStation(station);
        point.setConnectorType(connectorType);
        point.setPointNumber(request.getPointNumber());
        point.setSerialNumber(request.getSerialNumber());

        // installationDate và lastMaintenanceDate: dùng dữ liệu request (không default nữa)
        point.setInstallationDate(request.getInstallationDate());
        point.setLastMaintenanceDate(request.getLastMaintenanceDate());

        // maxPowerKW: nếu null hoặc <= 0 thì fallback giống mapper (22.0)
        Double reqPower = request.getMaxPowerKW();
        point.setMaxPowerKW(reqPower != null && reqPower > 0 ? reqPower : 22.0);

        // status: dùng status từ request
        point.setStatus(request.getStatus());

        // updatedAt: cập nhật thời gian sửa
        point.setUpdatedAt(LocalDateTime.now());

        // 8) Lưu lại vào DB
        chargingPointRepository.save(point);

        // 9) Map sang response
        return chargingPointMapper.toResponse(point);
    }

    @Override
    @Transactional
    public void deleteChargingPoint(Long pointId) {

        // 1) Đảm bảo điểm sạc tồn tại
        ChargingPoint point = chargingPointRepository.findById(pointId)
                .orElseThrow(() -> new ErrorException("Charging point not found"));
        // 2) Xoá
        chargingPointRepository.delete(point);
    }
}
