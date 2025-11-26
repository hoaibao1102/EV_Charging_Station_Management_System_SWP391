package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.TariffCreateRequest;
import com.swp391.gr3.ev_management.dto.request.TariffUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.TariffResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.Tariff;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.TariffResponseMapper;
import com.swp391.gr3.ev_management.repository.TariffRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service // Đánh dấu class là Spring Service (chứa logic nghiệp vụ về biểu giá Tariff)
@RequiredArgsConstructor // Lombok tự tạo constructor cho các field final để DI
public class TariffServiceImpl implements TariffService {

    private final TariffRepository tariffRepository;           // Repository thao tác CRUD với bảng Tariff
    private final ConnectorTypeService connectorTypeService;   // Service để kiểm tra / lấy ConnectorType
    private final TariffResponseMapper tariffResponseMapper;   // Mapper Entity Tariff -> DTO TariffResponse

    /**
     * Lấy toàn bộ Tariff trong hệ thống và map sang TariffResponse.
     */
    @Override
    public List<TariffResponse> getAllTariffs() {
        // 1) Lấy tất cả Tariff từ DB
        // 2) Dùng mapper để chuyển List<Tariff> -> List<TariffResponse>
        return tariffResponseMapper.toResponseList(tariffRepository.findAll());
    }

    /**
     * Lấy chi tiết 1 Tariff theo ID.
     * - Nếu không tìm thấy -> ném ErrorException.
     */
    @Override
    public TariffResponse getTariffById(long tariffId) {
        // 1) Tìm Tariff theo khóa chính (id)
        Tariff tariff = tariffRepository.findById(tariffId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy tariff với ID: " + tariffId));
        // 2) Map sang DTO
        return tariffResponseMapper.toResponse(tariff);
    }

    /**
     * Tạo mới Tariff (biểu giá).
     * - Validate khoảng thời gian hiệu lực (effectiveFrom < effectiveTo).
     * - Kiểm tra ConnectorType có tồn tại.
     * - Lưu Tariff mới và trả về DTO.
     */
    @Override
    @Transactional // Thao tác ghi DB nên cần Transaction
    public TariffResponse createTariff(TariffCreateRequest request) {
        // 1) Kiểm tra effectiveFrom phải trước effectiveTo
        if (request.getEffectiveFrom().isAfter(request.getEffectiveTo())) {
            throw new ErrorException("EffectiveFrom phải trước EffectiveTo");
        }

        // 2) Kiểm tra ConnectorType tồn tại (foreign key)
        ConnectorType connectorType = connectorTypeService.findById(request.getConnectorTypeId())
                .orElseThrow(() -> new ErrorException("Không tìm thấy connector type với ID: " + request.getConnectorTypeId()));

        // 3) Build entity Tariff từ request
        Tariff tariff = Tariff.builder()
                .connectorType(connectorType)              // gắn loại đầu nối
                .pricePerKWh(request.getPricePerKWh())     // giá theo kWh
                .pricePerMin(request.getPricePerMin())     // giá theo phút (nếu có)
                .currency(request.getCurrency())           // đơn vị tiền tệ (VD: "VND")
                .effectiveFrom(request.getEffectiveFrom()) // thời gian bắt đầu hiệu lực
                .effectiveTo(request.getEffectiveTo())     // thời gian kết thúc hiệu lực
                .build();

        // 4) Lưu Tariff mới vào DB
        Tariff saved = tariffRepository.save(tariff);
        // 5) Map sang DTO trả về
        return tariffResponseMapper.toResponse(saved);
    }

    /**
     * Cập nhật Tariff.
     * - Cho phép cập nhật từng field nếu request có giá trị.
     * - Kiểm tra lại điều kiện effectiveFrom < effectiveTo sau khi cập nhật.
     */
    @Override
    @Transactional
    public TariffResponse updateTariff(long tariffId, TariffUpdateRequest request) {
        // 1) Tìm Tariff cần update
        Tariff tariff = tariffRepository.findById(tariffId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy tariff với ID: " + tariffId));

        // 2) Cập nhật ConnectorType nếu request gửi connectorTypeId > 0
        if (request.getConnectorTypeId() > 0) {
            ConnectorType connectorType = connectorTypeService.findById(request.getConnectorTypeId())
                    .orElseThrow(() -> new ErrorException("Không tìm thấy connector type với ID: " + request.getConnectorTypeId()));
            tariff.setConnectorType(connectorType);
        }

        // 3) Cập nhật giá theo kWh nếu request > 0 (bảo vệ: không cho set 0 hoặc âm)
        if (request.getPricePerKWh() > 0) {
            tariff.setPricePerKWh(request.getPricePerKWh());
        } else {
            // giữ nguyên giá cũ (dòng else này thực tế không thay đổi gì, nhưng thể hiện rõ ý đồ)
            tariff.setPricePerKWh(tariff.getPricePerKWh());
        }

        // 4) Cập nhật giá theo phút nếu request > 0, tương tự trên
        if (request.getPricePerMin() > 0) {
            tariff.setPricePerMin(request.getPricePerMin());
        } else {
            tariff.setPricePerMin(tariff.getPricePerMin());
        }

        // 5) Cập nhật currency nếu request không null, nếu null thì giữ nguyên
        if (request.getCurrency() != null) {
            tariff.setCurrency(request.getCurrency());
        } else {
            tariff.setCurrency(tariff.getCurrency());
        }

        // 6) Cập nhật thời gian bắt đầu hiệu lực nếu request có gửi
        if (request.getEffectiveFrom() != null) {
            tariff.setEffectiveFrom(request.getEffectiveFrom());
        } else {
            tariff.setEffectiveFrom(tariff.getEffectiveFrom());
        }

        // 7) Cập nhật thời gian kết thúc hiệu lực nếu request có gửi
        if (request.getEffectiveTo() != null) {
            tariff.setEffectiveTo(request.getEffectiveTo());
        } else {
            tariff.setEffectiveTo(tariff.getEffectiveTo());
        }

        // 8) Sau khi cập nhật xong, validate lại điều kiện thời gian hiệu lực
        if (tariff.getEffectiveFrom().isAfter(tariff.getEffectiveTo())) {
            throw new ErrorException("EffectiveFrom phải trước EffectiveTo");
        }

        // 9) Lưu Tariff đã cập nhật
        Tariff updated = tariffRepository.save(tariff);
        // 10) Map sang DTO và trả về
        return tariffResponseMapper.toResponse(updated);
    }

    /**
     * Tìm Tariff hiện tại theo ConnectorType (không quan tâm thời gian).
     */
    @Override
    public Optional<Tariff> findByConnectorType(ConnectorType connectorType) {
        return tariffRepository.findByConnectorType(connectorType)
                .stream()
                .findFirst(); // lấy bản ghi đầu tiên
    }

    /**
     * Tìm Tariff mới nhất (ORDER BY effectiveFrom DESC) cho 1 connectorType,
     * mà khoảng thời gian hiệu lực bao trùm pricingTime.
     * - Dùng khi cần áp dụng giá tại một thời điểm cụ thể.
     */
    @Override
    public Optional<Tariff> findTopByConnectorType_ConnectorTypeIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(Long connectorTypeId, LocalDateTime pricingTime, LocalDateTime pricingTime1) {
        // Delegate hoàn toàn cho repository, giữ nguyên chữ ký method
        return tariffRepository.findTopByConnectorType_ConnectorTypeIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(connectorTypeId, pricingTime, pricingTime1);
    }

    /**
     * Tìm tất cả Tariff đang ACTIVE cho connectorType tại một thời điểm pricingTime.
     * - Có thể trả về nhiều bản ghi nếu cấu hình cho phép overlap thời gian (tuỳ design).
     */
    @Override
    public Collection<Tariff> findActiveByConnectorType(Long connectorTypeId, LocalDateTime pricingTime) {
        return tariffRepository.findActiveByConnectorType(connectorTypeId, pricingTime);
    }

    @Override
    public Optional<Double> findPricePerMinActive(Long connectorTypeId, LocalDateTime now) {
        return tariffRepository.findPricePerMinActive(connectorTypeId, now);
    }

    @Override
    public Collection<Tariff> findTariffByConnectorType(Long connectorTypeId) {
        return tariffRepository.findTariffByConnectorType(connectorTypeId);
    }

}
