package com.swp391.gr3.ev_management.service;

import com.swp391.gr3.ev_management.dto.request.ConnectorTypeCreateRequest;
import com.swp391.gr3.ev_management.dto.request.ConnectorTypeUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.ConnectorTypeResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.ConnectorTypeMapper;
import com.swp391.gr3.ev_management.repository.ConnectorTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service // Đánh dấu lớp là Spring Service (xử lý nghiệp vụ cho ConnectorType)
@RequiredArgsConstructor // Tự động tạo constructor cho các field final (DI qua constructor)
public class ConnectorTypeServiceImpl implements ConnectorTypeService {

    private final ConnectorTypeRepository connectorTypeRepository; // Repository thao tác DB bảng ConnectorType
    private final ConnectorTypeMapper connectorTypeMapper;         // Mapper chuyển đổi Entity <-> DTO

    @Override
    public List<ConnectorTypeResponse> getAllConnectorTypes() {
        // Lấy toàn bộ ConnectorType từ DB
        return connectorTypeRepository.findAll().stream() // Chuyển danh sách sang stream để map
                .map(connectorTypeMapper::toResponse)     // Map từng entity sang DTO response
                .collect(Collectors.toList());            // Gom về dạng List
    }

    @Override
    public ConnectorTypeResponse getConnectorTypeById(Long connectorTypeId) {
        // Tìm connector type theo ID
        ConnectorType connectorType = connectorTypeRepository.findById(connectorTypeId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy connector type với ID: " + connectorTypeId));
        // Map sang DTO để trả về client
        return connectorTypeMapper.toResponse(connectorType);
    }

    @Override
    @Transactional // Tạo mới là thao tác ghi DB nên cần transaction để đảm bảo toàn vẹn dữ liệu
    public ConnectorTypeResponse createConnectorType(ConnectorTypeCreateRequest request) {

        // Kiểm tra ràng buộc duy nhất: combination (code, mode) phải duy nhất trong database
        if (connectorTypeRepository.existsByCodeAndMode(request.getCode(), request.getMode())) {
            throw new ErrorException("Code and Mode không được trùng: " + request.getCode() + "And" + request.getMode());
        }

        // Khởi tạo entity từ request bằng builder pattern
        ConnectorType connectorType = ConnectorType.builder()
                .code(request.getCode())                         // Mã đầu nối ví dụ: "CCS", "CHADEMO"
                .mode(request.getMode())                         // Chế độ (AC/DC)
                .displayName(request.getDisplayName())           // Tên hiển thị cho giao diện
                .defaultMaxPowerKW(request.getDefaultMaxPowerKW()) // Công suất tối đa mặc định
                .isDeprecated(request.getIsDeprecated())         // Đánh dấu loại connector có còn dùng hay không
                .build();

        // Lưu vào DB (INSERT)
        ConnectorType saved = connectorTypeRepository.save(connectorType);

        // Trả DTO response cho client
        return connectorTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional // Update là ghi DB -> cần transaction để rollback nếu có lỗi
    public ConnectorTypeResponse updateConnectorType(Long connectorTypeId, ConnectorTypeUpdateRequest request) {

        // Tìm entity cần cập nhật
        ConnectorType connectorType = connectorTypeRepository.findById(connectorTypeId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy connector type với ID: " + connectorTypeId));

        // Nếu update code và code mới khác code cũ
        // -> Kiểm tra trùng combination (code, mode)
        if (request.getCode() != null && !request.getCode().equals(connectorType.getCode())) {
            if (connectorTypeRepository.existsByCodeAndMode(request.getCode(), request.getMode())) {
                throw new ErrorException("Code and Mode không được trùng: " + request.getCode() + "And" + request.getMode());
            }
            connectorType.setCode(request.getCode()); // Hợp lệ -> cập nhật code
        }

        // Cập nhật mode nếu request có cung cấp
        if (request.getMode() != null) {
            connectorType.setMode(request.getMode());
        }

        // Cập nhật tên hiển thị nếu request có truyền
        if (request.getDisplayName() != null) {
            connectorType.setDisplayName(request.getDisplayName());
        }

        // Cập nhật công suất tối đa, chỉ update nếu khác 0
        // (Lưu ý: Nếu hệ thống cần set bằng 0 thì điều kiện này cần thay đổi)
        if (request.getDefaultMaxPowerKW() != 0) {
            connectorType.setDefaultMaxPowerKW(request.getDefaultMaxPowerKW());
        }

        // Cập nhật trạng thái deprecate
        if (request.getIsDeprecated() != null) {
            connectorType.setIsDeprecated(request.getIsDeprecated());
        }

        // Lưu lại các thay đổi (UPDATE)
        ConnectorType updated = connectorTypeRepository.save(connectorType);

        // Trả về DTO response
        return connectorTypeMapper.toResponse(updated);
    }

    @Override
    public Optional<ConnectorType> findById(Long connectorTypeId) {
        // Trả Optional để cho caller tự handle null-safe
        return connectorTypeRepository.findById(connectorTypeId);
    }

    @Override
    public List<ConnectorType> findAllById(List<Long> connectorTypeIds) {
        // Lấy danh sách connector type theo list ID
        return connectorTypeRepository.findAllById(connectorTypeIds);
    }

    @Override
    public List<ConnectorType> findDistinctByChargingPoints_Station_StationId(Long stationId) {
        // Lấy danh sách connector type duy nhất theo stationId thông qua quan hệ trong DB
        return connectorTypeRepository.findDistinctByChargingPoints_Station_StationId(stationId);
    }
}
