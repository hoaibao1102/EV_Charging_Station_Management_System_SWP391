package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.SlotAvailabilityCreateRequest;
import com.swp391.gr3.ev_management.dto.response.SlotAvailabilityResponse;
import com.swp391.gr3.ev_management.entity.*;
import com.swp391.gr3.ev_management.enums.SlotStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.SlotAvailabilityMapper;
import com.swp391.gr3.ev_management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service // Đánh dấu class là một Spring Service (chứa logic nghiệp vụ cho SlotAvailability)
@RequiredArgsConstructor // Lombok tạo constructor cho các field final, dùng cho DI
public class SlotAvailabilityServiceImpl implements SlotAvailabilityService {

    // ====== Các dependency chính thao tác với DB & mapping ======
    private final SlotAvailabilityRepository slotAvailabilityRepository; // CRUD cho SlotAvailability
    private final SlotTemplateService slotTemplateService;               // Lấy SlotTemplate theo id, config, time
    private final SlotConfigRepository slotConfigRepository;             // Lấy cấu hình SlotConfig
    private final ConnectorTypeService connectorTypeService;             // Lấy danh sách ConnectorType
    private final ChargingPointService chargingPointService;             // Lấy ChargingPoint theo station/connectorType
    private final SlotAvailabilityMapper mapper;                          // Map entity SlotAvailability -> DTO response

    /**
     * Tạo SlotAvailability cho danh sách template + danh sách connectorType được chọn.
     * Quy trình:
     *  - Validate request (templateIds & connectorTypeIds không rỗng)
     *  - Lấy list SlotTemplate theo templateIds
     *  - Lấy list ConnectorType theo connectorTypeIds
     *  - Với mỗi template:
     *      + Lấy stationId từ template → config → station
     *      + Chuẩn hóa ngày về 00:00:00
     *      + Với mỗi connector type:
     *          * Tìm tất cả ChargingPoint ở station đó có connectorType tương ứng
     *          * Với mỗi point:
     *              - Nếu chưa tồn tại SlotAvailability (template + point + date) thì tạo mới
     *  - saveAll() & map sang DTO
     */
    @Override
    @Transactional // Có thao tác ghi DB (saveAll) → cần transaction để đảm bảo toàn vẹn
    public List<SlotAvailabilityResponse> createForTemplates(SlotAvailabilityCreateRequest req) {
        // 1️⃣ Validate: danh sách templateIds phải có
        if (req.getTemplateIds() == null || req.getTemplateIds().isEmpty()) {
            throw new ErrorException("templateIds không được rỗng");
        }
        // 2️⃣ Validate: danh sách connectorTypeIds phải có
        if (req.getConnectorTypeIds() == null || req.getConnectorTypeIds().isEmpty()) {
            throw new ErrorException("connectorTypeIds không được rỗng");
        }

        // 3️⃣ Lấy danh sách SlotTemplate từ DB theo list templateIds
        List<SlotTemplate> templates = slotTemplateService.findAllById(req.getTemplateIds());
        if (templates.isEmpty()) return Collections.emptyList(); // Không có template nào -> không tạo gì hết

        // 4️⃣ Lấy danh sách ConnectorType từ DB theo list connectorTypeIds
        List<ConnectorType> connectorTypes = connectorTypeService.findAllById(req.getConnectorTypeIds());
        if (connectorTypes.isEmpty()) return Collections.emptyList(); // Không có connector type -> dừng

        // 5️⃣ Danh sách SlotAvailability sẽ được tạo mới & lưu batch
        List<SlotAvailability> toSave = new ArrayList<>();

        // 6️⃣ Lặp qua từng template để build SlotAvailability tương ứng
        for (SlotTemplate template : templates) {
            // 6.1) Lấy stationId từ template -> config -> station (giả định quan hệ không null)
            Long stationId = Optional.ofNullable(template.getConfig())
                    .map(SlotConfig::getStation)
                    .map(ChargingStation::getStationId)
                    .orElseThrow(() -> new ErrorException(
                            "Template " + template.getTemplateId() + " không có liên kết Station qua Config"));

            // 6.2) Chuẩn hóa ngày: lấy ngày từ startTime của template và set giờ về 00:00:00
            LocalDateTime date = template.getStartTime()
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);

            // 6.3) Với mỗi connector type được chọn
            for (ConnectorType ct : connectorTypes) {
                // 6.3.1) Lấy tất cả ChargingPoint tại station này có connectorType = ct
                List<ChargingPoint> points =
                        chargingPointService.findByStation_StationIdAndConnectorType_ConnectorTypeId(
                                stationId, ct.getConnectorTypeId()
                        );

                // 6.3.2) Với mỗi point, kiểm tra SlotAvailability đã tồn tại hay chưa
                for (ChargingPoint point : points) {
                    boolean exists = slotAvailabilityRepository
                            .existsByTemplate_TemplateIdAndChargingPoint_PointIdAndDate(
                                    template.getTemplateId(), point.getPointId(), date);

                    // 6.3.3) Nếu chưa tồn tại -> tạo mới SlotAvailability AVAILABLE cho (template, point, date)
                    if (!exists) {
                        SlotAvailability sa = SlotAvailability.builder()
                                .template(template)
                                .chargingPoint(point)             // ✅ gán đúng ChargingPoint
                                .status(SlotStatus.AVAILABLE)     // mặc định là AVAILABLE để book
                                .date(date)
                                .build();
                        toSave.add(sa); // thêm vào list chờ save batch
                    }
                }
            }
        }

        // 7️⃣ Lưu hàng loạt SlotAvailability và map sang DTO trước khi trả về
        return slotAvailabilityRepository.saveAll(toSave)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tạo SlotAvailability cho một SlotConfig trong một ngày cụ thể.
     * Flow:
     *  - Tìm SlotConfig theo configId
     *  - Lấy các SlotTemplate thuộc config trong khoảng [date 00:00, date+1 00:00)
     *  - Lấy danh sách ConnectorType đang tồn tại tại station của config
     *  - Build request SlotAvailabilityCreateRequest và tái sử dụng logic createForTemplates()
     */
    @Override
    @Transactional
    public List<SlotAvailabilityResponse> createForConfigInDate(Long configId, LocalDate date) {
        // 1️⃣ Lấy SlotConfig theo configId
        SlotConfig config = slotConfigRepository.findByConfigId(configId);
        if (config == null) throw new ErrorException("Không tìm thấy SlotConfig id=" + configId);

        // 2️⃣ Xác định khoảng thời gian của ngày [start, end)
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = start.plusDays(1);

        // 3️⃣ Lấy các template thuộc config trong khoảng thời gian trong ngày
        List<SlotTemplate> templates = slotTemplateService
                .findByConfig_ConfigIdAndStartTimeBetween(configId, start, end);

        if (templates.isEmpty()) return Collections.emptyList(); // Không có template -> không làm gì

        // 4️⃣ Lấy danh sách connectorTypes có mặt ở station ứng với config
        Long stationId = config.getStation().getStationId();
        List<ConnectorType> connectorTypes =
                connectorTypeService.findDistinctByChargingPoints_Station_StationId(stationId);

        if (connectorTypes.isEmpty()) return Collections.emptyList(); // Không có connector type -> dừng

        // 5️⃣ Chuẩn bị request để tái sử dụng logic createForTemplates
        SlotAvailabilityCreateRequest req = new SlotAvailabilityCreateRequest();
        // 5.1) Set danh sách templateIds
        req.setTemplateIds(templates.stream().map(SlotTemplate::getTemplateId).toList());
        // 5.2) Set danh sách connectorTypeIds
        req.setConnectorTypeIds(connectorTypes.stream().map(ConnectorType::getConnectorTypeId).toList());

        // 6️⃣ Gọi lại hàm createForTemplates để tránh trùng logic
        return createForTemplates(req);
    }

    /**
     * Cập nhật trạng thái của một SlotAvailability.
     * - Thường dùng để đánh dấu AVAILABLE/BOOKED/UNAVAILABLE...
     */
    @Override
    @Transactional
    public SlotAvailabilityResponse updateStatus(Long slotAvailabilityId, SlotStatus status) {
        // 1️⃣ Tìm SlotAvailability theo id
        SlotAvailability sa = slotAvailabilityRepository.findById(slotAvailabilityId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy SlotAvailability id=" + slotAvailabilityId));
        // 2️⃣ Cập nhật trạng thái
        sa.setStatus(status);
        // 3️⃣ Lưu và map sang DTO
        return mapper.toResponse(slotAvailabilityRepository.save(sa));
    }

    /**
     * Lấy tất cả SlotAvailability theo pointId (tất cả slot theo một ChargingPoint).
     */
    @Override
    public List<SlotAvailabilityResponse> findByPointId(Long pointId) {
        // 1️⃣ Lấy tất cả slotAvailability của 1 charging point
        List<SlotAvailability> slots = slotAvailabilityRepository.findAllByChargingPoint_PointId(pointId);

        // 2️⃣ Nếu không có -> ném lỗi nghiệp vụ
        if (slots.isEmpty()) {
            throw new ErrorException("Không tìm thấy SlotAvailability cho PointId = " + pointId);
        }

        // 3️⃣ Map sang DTO trả về
        return slots.stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Lấy tất cả SlotAvailability (dùng cho admin xem tổng quan).
     */
    @Override
    public List<SlotAvailabilityResponse> findAll() {
        return slotAvailabilityRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tìm SlotAvailability theo danh sách id (dùng nội bộ cho các service khác).
     */
    @Override
    public List<SlotAvailability> findAllById(List<Long> slotIds) {
        return slotAvailabilityRepository.findAllById(slotIds);
    }

    /**
     * Lưu một SlotAvailability (dùng khi cập nhật trạng thái từng slot).
     */
    @Override
    public void save(SlotAvailability slot) {
        slotAvailabilityRepository.save(slot);
    }

    /**
     * Xóa tất cả SlotAvailability theo configId trong khoảng ngày [start, end).
     * Dùng cho job reset daily.
     */
    @Override
    @Transactional
    public int deleteByTemplate_Config_ConfigIdAndDateBetween(Long configId, LocalDateTime start, LocalDateTime end) {
        return slotAvailabilityRepository.deleteByTemplate_Config_ConfigIdAndDateBetween(configId, start, end);
    }

    /**
     * Lưu hàng loạt SlotAvailability (dùng cho scheduler/job tạo slot theo config).
     */
    @Override
    public Collection<SlotAvailability> saveAll(ArrayList<SlotAvailability> toSave) {
        return slotAvailabilityRepository.saveAll(toSave);
    }

    /**
     * Tìm SlotAvailability theo configId trong khoảng ngày [start, end).
     * Dùng cho các logic lọc slot theo config và ngày.
     */
    @Override
    public List<SlotAvailability> findByConfigAndDateBetween(Long configId, LocalDateTime start, LocalDateTime end) {
        return slotAvailabilityRepository.findByTemplate_Config_ConfigIdAndDateBetween(configId, start, end);
    }
}
