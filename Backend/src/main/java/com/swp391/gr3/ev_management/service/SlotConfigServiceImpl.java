package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.SlotConfigRequest;
import com.swp391.gr3.ev_management.dto.response.SlotConfigResponse;
import com.swp391.gr3.ev_management.entity.ChargingStation;
import com.swp391.gr3.ev_management.entity.SlotConfig;
import com.swp391.gr3.ev_management.enums.SlotConfigStatus;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.SlotConfigMapper;
import com.swp391.gr3.ev_management.repository.SlotConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service // Đánh dấu class là một Spring Service chứa logic chính cho SlotConfig
@RequiredArgsConstructor // Tự động tạo constructor cho các dependency final
public class SlotConfigServiceImpl implements SlotConfigService {

    private final SlotConfigRepository slotConfigRepository;     // Truy vấn / lưu SlotConfig
    private final ChargingStationService chargingStationService; // Lấy thông tin trạm sạc
    private final SlotConfigMapper mapper;                       // Mapper entity <-> DTO

    // Service sinh SlotTemplate cho từng ngày
    private final SlotTemplateService slotTemplateService;
    // Service tạo SlotAvailability dựa trên template
    private final SlotAvailabilityService slotAvailabilityService;

    @Override
    public SlotConfigResponse findByConfigId(Long slotConfigId) {
        // Tìm config theo id và map sang DTO
        SlotConfig slotConfig = slotConfigRepository.findByConfigId(slotConfigId);
        return mapper.toResponse(slotConfig);
    }

    @Override
    public SlotConfigResponse findByStation_StationId(Long stationId) {
        // Lấy config theo station
        SlotConfig slotConfig = slotConfigRepository.findByStation_StationId(stationId);
        return mapper.toResponse(slotConfig);
    }

    @Override
    public List<SlotConfigResponse> findAll() {
        // Lấy toàn bộ config và map sang DTO
        return slotConfigRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tạo 1 SlotConfig mới cho 1 trạm.
     * - Mỗi trạm chỉ có 1 ACTIVE config tại một thời điểm.
     * - Trước khi tạo mới → deactivate toàn bộ config đang ACTIVE.
     * - Sau đó generate SlotTemplates + SlotAvailability cho hôm nay.
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE) // đảm bảo an toàn khi nhiều người thao tác cùng lúc
    public SlotConfigResponse addSlotConfig(SlotConfigRequest req) {
        // 1) Kiểm tra trạm tồn tại
        ChargingStation station = chargingStationService.findEntityById(req.getStationId());
        if (station == null) {
            throw new ErrorException("Không tìm thấy trạm sạc có ID = " + req.getStationId());
        }

        LocalDateTime now = LocalDateTime.now();

        // 2) Nếu trạm có SlotConfig ACTIVE → deactivate toàn bộ (set activeExpire = now)
        boolean existedActive = slotConfigRepository.existsActiveConfig(req.getStationId(), SlotConfigStatus.ACTIVE);
        if (existedActive) {
            slotConfigRepository.deactivateActiveByStation(req.getStationId(), now);
        }

        // 3) Chuẩn bị request tạo SlotConfig mới dạng ACTIVE
        req.setActiveFrom(now);
        req.setActiveExpire(now);        // activeExpire có thể cập nhật sau
        req.setIsActive(SlotConfigStatus.ACTIVE);

        // 4) Map request -> entity, gán station vào
        SlotConfig entity = mapper.toEntity(req, station);
        SlotConfig saved = slotConfigRepository.save(entity);

        // 5) Sinh SlotTemplate & SlotAvailability cho ngày hiện tại
        generateDailyTemplates(saved.getConfigId(), now);

        return mapper.toResponse(saved);
    }

    /**
     * Cập nhật 1 SlotConfig đã có.
     * - Không tự động regenerate SlotTemplate, tùy nhu cầu mới gọi.
     */
    @Override
    @Transactional
    public SlotConfigResponse updateSlotConfig(Long configId, SlotConfigRequest req) {
        SlotConfig existing = slotConfigRepository.findByConfigId(configId);
        if (existing == null) return null;

        // Kiểm tra trạm tồn tại
        ChargingStation station = chargingStationService.findEntityById(req.getStationId());
        if (station == null) {
            throw new ErrorException("Không tìm thấy trạm sạc có ID = " + req.getStationId());
        }

        // Cập nhật entity bằng mapper
        mapper.updateEntity(existing, req, station);

        // Lưu lại thay đổi
        SlotConfig updated = slotConfigRepository.save(existing);

        // Ghi chú: có thể regenerate template nếu muốn
        // generateDailyTemplates(updated.getConfigId(), LocalDateTime.now());

        return mapper.toResponse(updated);
    }

    /**
     * Tạo template hằng ngày & availability tương ứng.
     */
    @Override
    @Transactional
    public void generateDailyTemplates(Long configId, LocalDateTime now) {
        // 1) Generate SlotTemplate cho ngày hôm nay
        slotTemplateService.generateDailyTemplates(configId, now, now.plusDays(1));

        // 2) Tạo thêm SlotAvailability cho ngày hôm nay dựa trên template
        if (slotAvailabilityService != null) {
            slotAvailabilityService.createForConfigInDate(configId, now.toLocalDate());
        }
    }

    /**
     * Chuyển 1 SlotConfig sang trạng thái INACTIVE (ngừng hoạt động).
     */
    @Transactional
    public SlotConfigResponse deactivateConfig(Long configId) {
        SlotConfig config = slotConfigRepository.findById(configId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy SlotConfig: " + configId));

        // Nếu đã inactive rồi → không làm lại lần nữa
        if (config.getIsActive() == SlotConfigStatus.INACTIVE) {
            throw new ErrorException("SlotConfig này đã INACTIVE rồi.");
        }

        // Đặt trạng thái INACTIVE + thời điểm hết hiệu lực
        config.setIsActive(SlotConfigStatus.INACTIVE);
        config.setActiveExpire(LocalDateTime.now());

        SlotConfig saved = slotConfigRepository.save(config);
        return mapper.toResponse(saved);
    }

    @Override
    public SlotConfig findEntityById(Long slotConfigId) {
        // Trả về entity để các service khác dùng
        return slotConfigRepository.findByConfigId(slotConfigId);
    }

    @Override
    public List<SlotConfig> findByIsActive(SlotConfigStatus slotConfigStatus) {
        // Tìm các config theo trạng thái (ACTIVE / INACTIVE)
        return slotConfigRepository.findByIsActive(slotConfigStatus);
    }
}
