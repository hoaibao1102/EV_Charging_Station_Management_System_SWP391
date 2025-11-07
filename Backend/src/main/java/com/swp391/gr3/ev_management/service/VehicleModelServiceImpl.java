package com.swp391.gr3.ev_management.service;

import com.cloudinary.Cloudinary;
import com.swp391.gr3.ev_management.enums.VehicleModelStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.swp391.gr3.ev_management.dto.request.VehicleModelCreateRequest;
import com.swp391.gr3.ev_management.dto.request.VehicleModelUpdateRequest;
import com.swp391.gr3.ev_management.dto.response.VehicleModelResponse;
import com.swp391.gr3.ev_management.entity.ConnectorType;
import com.swp391.gr3.ev_management.entity.VehicleModel;
import com.swp391.gr3.ev_management.exception.ConflictException;
import com.swp391.gr3.ev_management.exception.ErrorException;
import com.swp391.gr3.ev_management.mapper.VehicleModelMapper;
import com.swp391.gr3.ev_management.repository.ConnectorTypeRepository;
import com.swp391.gr3.ev_management.repository.VehicleModelRepository;
import com.swp391.gr3.ev_management.repository.VehicleRepisitory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VehicleModelServiceImpl implements VehicleModelService {
    private static final Logger logger = LoggerFactory.getLogger(VehicleModelServiceImpl.class);
    private final Cloudinary cloudinary;

    private final VehicleModelRepository vehicleModelRepository;
    private final ConnectorTypeRepository connectorTypeRepository;
    private final VehicleRepisitory vehicleRepisitory;
    private final VehicleModelMapper vehicleModelMapper;

    @Transactional
    public VehicleModelResponse create(VehicleModelCreateRequest request) {
        if (vehicleModelRepository.existsByBrandIgnoreCaseAndModelIgnoreCaseAndYear(
                request.getBrand(), request.getModel(), request.getYear())) {
            throw new ConflictException("Vehicle model already exists for brand/model/year");
        }
        ConnectorType connectorType = connectorTypeRepository.findById(request.getConnectorTypeId())
                .orElseThrow(() -> new ErrorException("ConnectorType not found with id " + request.getConnectorTypeId()));
        if (Boolean.TRUE.equals(connectorType.getIsDeprecated())) {
            throw new ErrorException("Cannot create vehicle_model: connector type is deprecated");
        }

        VehicleModel entity = vehicleModelMapper.toEntityForCreate(request, connectorType);
        VehicleModel saved = vehicleModelRepository.save(entity);
        return vehicleModelMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleModelResponse getById(Long id) {
    VehicleModel vm = vehicleModelRepository.findById(id)
        .orElseThrow(() -> new ErrorException("VehicleModel not found with id " + id));
    return vehicleModelMapper.toResponse(vm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleModelResponse> getAll() {
        return vehicleModelRepository.findAll().stream().map(vehicleModelMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleModelResponse> search(String brand, String model, Integer year, Integer connectorTypeId) {
    return vehicleModelRepository.search(brand, model, year, connectorTypeId)
        .stream().map(vehicleModelMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public VehicleModelResponse update(Long id, VehicleModelUpdateRequest request) {
        VehicleModel vm = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ErrorException("VehicleModel not found with id " + id));

        String oldPublicId = vm.getImagePublicId();

        String newBrand = request.getBrand() != null ? request.getBrand() : vm.getBrand();
        String newModel = request.getModel() != null ? request.getModel() : vm.getModel();
        int newYear     = request.getYear()  != null ? request.getYear()  : vm.getYear();

        if (vehicleModelRepository.existsByBrandIgnoreCaseAndModelIgnoreCaseAndYearAndModelIdNot(
                newBrand, newModel, newYear, id)) {
            throw new ConflictException("Another VehicleModel with same brand/model/year already exists");
        }

        ConnectorType ctIfChanged = null;
        if (request.getConnectorTypeId() != null) {
            ctIfChanged = connectorTypeRepository.findById(request.getConnectorTypeId())
                    .orElseThrow(() -> new ErrorException("ConnectorType not found with id " + request.getConnectorTypeId()));
        }

        // 👉 dùng mapper để patch
        vehicleModelMapper.applyUpdates(vm, request, ctIfChanged);

        VehicleModel saved = vehicleModelRepository.save(vm);

        String newPublicId = request.getImagePublicId();
        if (oldPublicId != null && newPublicId != null && !oldPublicId.equals(newPublicId)) {
            try {
                logger.info("Deleting old image from Cloudinary: {}", oldPublicId);
                cloudinary.uploader().destroy(oldPublicId, Map.of());
            } catch (Exception e) {
                logger.error("Failed to delete old image from Cloudinary: {}. Error: {}", oldPublicId, e.getMessage());
            }
        }
        return vehicleModelMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        VehicleModel vm = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ErrorException("VehicleModel not found with id " + id));

        long usage = vehicleRepisitory.countByModel_ModelId(id);
        if (usage > 0) {
            throw new ConflictException("Cannot delete VehicleModel in use by " + usage + " vehicle(s)");
        }
        // --- THAY ĐỔI: Xóa ảnh trên Cloudinary TRƯỚC khi xóa khỏi DB ---
        String publicId = vm.getImagePublicId();
        if (publicId != null && !publicId.isEmpty()) {
            try {
                logger.info("Deleting image from Cloudinary: {}", publicId);
                cloudinary.uploader().destroy(publicId, Map.of());
            } catch (Exception e) {
                // Ghi log lỗi nhưng không dừng giao dịch
                logger.error("Failed to delete image from Cloudinary: {}. Error: {}", publicId, e.getMessage());
            }
        }
        vehicleModelRepository.delete(vm);
    }

    @Override
    @Transactional
    public VehicleModelResponse updateStatus(Long id, VehicleModelStatus status) {
        if (status == null) {
            throw new ErrorException("Status must not be null");
        }

        VehicleModel vm = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ErrorException("VehicleModel not found with id " + id));

        // Nếu không đổi trạng thái thì trả luôn
        if (vm.getStatus() == status) {
            return vehicleModelMapper.toResponse(vm);
        }

        // Quy tắc nghiệp vụ:
        // Không cho chuyển sang INACTIVE nếu model đang được sử dụng
        if (status == VehicleModelStatus.INACTIVE) {
            long usage = vehicleRepisitory.countByModel_ModelId(id);
            if (usage > 0) {
                throw new ConflictException(
                        "Cannot set status to INACTIVE. Model is used by " + usage + " vehicle(s)"
                );
            }
        }

        VehicleModelStatus oldStatus = vm.getStatus();
        vm.setStatus(status);
        VehicleModel saved = vehicleModelRepository.save(vm);

        logger.info("Updated VehicleModel(id={}) status from {} to {}", id, oldStatus, status);

        return vehicleModelMapper.toResponse(saved);
    }


}

