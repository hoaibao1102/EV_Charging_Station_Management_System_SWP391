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
import java.util.stream.Collectors;

@Service // Đánh dấu lớp là Spring Service (xử lý nghiệp vụ cho ConnectorType)
@RequiredArgsConstructor // Tự động tạo constructor cho các field final (DI qua constructor)
public class ConnectorTypeServiceImpl implements ConnectorTypeService {

    private final ConnectorTypeRepository connectorTypeRepository; // Repository thao tác DB bảng ConnectorType
    private final ConnectorTypeMapper connectorTypeMapper;         // Mapper mới được tách riêng

    @Override
    public List<ConnectorTypeResponse> getAllConnectorTypes() {
        // Lấy tất cả ConnectorType từ DB, map sang DTO response rồi trả về danh sách
        return connectorTypeRepository.findAll().stream()
                .map(connectorTypeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ConnectorTypeResponse getConnectorTypeById(Long connectorTypeId) {
        // Tìm connector type theo ID; nếu không có -> ném ErrorException
        ConnectorType connectorType = connectorTypeRepository.findById(connectorTypeId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy connector type với ID: " + connectorTypeId));
        // Map sang DTO để trả về
        return connectorTypeMapper.toResponse(connectorType);
    }

    @Override
    @Transactional // Tạo mới là thao tác ghi DB, cần transaction để đảm bảo toàn vẹn
    public ConnectorTypeResponse createConnectorType(ConnectorTypeCreateRequest request) {
        // Kiểm tra ràng buộc duy nhất: combination (code, mode) không được trùng
        if (connectorTypeRepository.existsByCodeAndMode(request.getCode(), request.getMode())) {
            throw new ErrorException("Code and Mode không được trùng: " + request.getCode() + "And" + request.getMode());
        }

        // Khởi tạo entity từ request
        ConnectorType connectorType = ConnectorType.builder()
                .code(request.getCode())
                .mode(request.getMode())
                .displayName(request.getDisplayName())
                .defaultMaxPowerKW(request.getDefaultMaxPowerKW())
                .isDeprecated(request.getIsDeprecated())
                .build();

        // Lưu vào DB
        ConnectorType saved = connectorTypeRepository.save(connectorType);
        // Trả về DTO
        return connectorTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional // Cập nhật cũng là thao tác ghi DB -> cần transaction
    public ConnectorTypeResponse updateConnectorType(Long connectorTypeId, ConnectorTypeUpdateRequest request) {
        // Tìm entity cần cập nhật; nếu không có -> lỗi
        ConnectorType connectorType = connectorTypeRepository.findById(connectorTypeId)
                .orElseThrow(() -> new ErrorException("Không tìm thấy connector type với ID: " + connectorTypeId));

        // Nếu muốn cập nhật code và code mới khác code hiện tại -> kiểm tra trùng (code, mode)
        if (request.getCode() != null && !request.getCode().equals(connectorType.getCode())) {
            if (connectorTypeRepository.existsByCodeAndMode(request.getCode(), request.getMode())) {
                throw new ErrorException("Code and Mode không được trùng: " + request.getCode() + "And" + request.getMode());
            }
            connectorType.setCode(request.getCode()); // hợp lệ -> set code mới
        }

        // Cập nhật các field khác nếu request có giá trị
        if (request.getMode() != null) {
            connectorType.setMode(request.getMode());
        }
        if (request.getDisplayName() != null) {
            connectorType.setDisplayName(request.getDisplayName());
        }
        if (request.getDefaultMaxPowerKW() != 0) {
            // Lưu ý: điều kiện != 0 nghĩa là nếu muốn set đúng bằng 0 thì sẽ không cập nhật (tuỳ chủ ý thiết kế)
            connectorType.setDefaultMaxPowerKW(request.getDefaultMaxPowerKW());
        }
        if (request.getIsDeprecated() != null) {
            connectorType.setIsDeprecated(request.getIsDeprecated());
        }

        // Lưu lại thay đổi
        ConnectorType updated = connectorTypeRepository.save(connectorType);
        // Trả về DTO
        return connectorTypeMapper.toResponse(updated);
    }
}
