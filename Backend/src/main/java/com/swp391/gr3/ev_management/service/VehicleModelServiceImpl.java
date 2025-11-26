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
import com.swp391.gr3.ev_management.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service // Đánh dấu class là 1 Spring Service xử lý nghiệp vụ cho VehicleModel
@RequiredArgsConstructor // Lombok tự tạo constructor cho các field final (DI)
public class VehicleModelServiceImpl implements VehicleModelService {
    private static final Logger logger = LoggerFactory.getLogger(VehicleModelServiceImpl.class);
    private final Cloudinary cloudinary; // Bean Cloudinary dùng để thao tác ảnh (upload/xoá) trên Cloudinary

    private final VehicleModelRepository vehicleModelRepository; // Repository thao tác với bảng vehicle_model
    private final ConnectorTypeService connectorTypeService;     // Service để lấy/kiểm tra ConnectorType
    private final UserVehicleService userVehicleService;         // Service dùng để kiểm tra model đang được xe nào sử dụng không
    private final VehicleModelMapper vehicleModelMapper;         // Mapper chuyển Entity <-> DTO

    @Transactional // Thao tác tạo mới (ghi DB) nên cần transaction
    public VehicleModelResponse create(VehicleModelCreateRequest request) {
        // 1) Kiểm tra model đã tồn tại chưa (unique theo brand + model + year, không phân biệt hoa/thường)
        if (vehicleModelRepository.existsByBrandIgnoreCaseAndModelIgnoreCaseAndYear(
                request.getBrand(), request.getModel(), request.getYear())) {
            throw new ConflictException("Vehicle model already exists for brand/model/year");
        }

        // 2) Lấy ConnectorType theo ID trong request, nếu không có thì ném lỗi
        ConnectorType connectorType = connectorTypeService.findById(request.getConnectorTypeId())
                .orElseThrow(() -> new ErrorException("ConnectorType not found with id " + request.getConnectorTypeId()));

        // 3) Ràng buộc nghiệp vụ: không cho tạo model mới nếu connector type đã deprecated
        if (Boolean.TRUE.equals(connectorType.getIsDeprecated())) {
            throw new ErrorException("Cannot create vehicle_model: connector type is deprecated");
        }

        // 4) Dùng mapper chuyển request + connectorType sang entity VehicleModel
        VehicleModel entity = vehicleModelMapper.toEntityForCreate(request, connectorType);

        // 5) Lưu entity vào DB
        VehicleModel saved = vehicleModelRepository.save(entity);

        // 6) Map sang DTO response và trả về
        return vehicleModelMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true) // Chỉ đọc dữ liệu
    public VehicleModelResponse getById(Long id) {
        // 1) Tìm VehicleModel theo id, không thấy thì ném ErrorException
        VehicleModel vm = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ErrorException("VehicleModel not found with id " + id));

        // 2) Map entity sang DTO và trả về
        return vehicleModelMapper.toResponse(vm);
    }

    @Override
    @Transactional(readOnly = true) // Chỉ đọc
    public List<VehicleModelResponse> getAll() {
        // 1) Lấy tất cả VehicleModel từ DB
        // 2) Map từng entity sang DTO và trả về danh sách
        return vehicleModelRepository.findAll().stream().map(vehicleModelMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true) // Chỉ đọc
    public List<VehicleModelResponse> search(String brand, String model, Integer year, Integer connectorTypeId) {
        // 1) Gọi repository search theo các tiêu chí (có thể null để bỏ qua filter)
        // 2) Map kết quả sang DTO
        return vehicleModelRepository.search(brand, model, year, connectorTypeId)
                .stream().map(vehicleModelMapper::toResponse).toList();
    }

    @Override
    @Transactional // Cập nhật là thao tác ghi DB
    public VehicleModelResponse update(Long id, VehicleModelUpdateRequest request) {
        // 1) Tìm VehicleModel cần cập nhật; nếu không có -> ném lỗi
        VehicleModel vm = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ErrorException("VehicleModel not found with id " + id));

        // 2) Lưu lại imagePublicId cũ để nếu có đổi ảnh thì xoá ảnh cũ trên Cloudinary
        String oldPublicId = vm.getImagePublicId();

        // 3) Tính toán brand/model/year "mới" dựa trên request, nếu request null thì giữ giá trị hiện tại
        String newBrand = request.getBrand() != null ? request.getBrand() : vm.getBrand();
        String newModel = request.getModel() != null ? request.getModel() : vm.getModel();
        int newYear     = request.getYear()  != null ? request.getYear()  : vm.getYear();

        // 4) Kiểm tra trùng dữ liệu với model khác: brand/model/year phải là unique (ngoại trừ chính nó)
        if (vehicleModelRepository.existsByBrandIgnoreCaseAndModelIgnoreCaseAndYearAndModelIdNot(
                newBrand, newModel, newYear, id)) {
            throw new ConflictException("Another VehicleModel with same brand/model/year already exists");
        }

        // 5) Nếu có yêu cầu đổi connectorTypeId thì load connectorType mới để dùng cho mapper
        ConnectorType ctIfChanged = null;
        if (request.getConnectorTypeId() != null) {
            ctIfChanged = connectorTypeService.findById(request.getConnectorTypeId())
                    .orElseThrow(() -> new ErrorException("ConnectorType not found with id " + request.getConnectorTypeId()));
        }

        // 6) Dùng mapper để apply các thay đổi từ request vào entity (patch)
        vehicleModelMapper.applyUpdates(vm, request, ctIfChanged);

        // 7) Lưu entity đã cập nhật
        VehicleModel saved = vehicleModelRepository.save(vm);

        // 8) Xử lý xoá ảnh cũ trên Cloudinary nếu imagePublicId thay đổi
        String newPublicId = request.getImagePublicId();
        // Điều kiện: có oldPublicId, có newPublicId và 2 cái khác nhau -> ảnh cũ không còn dùng nữa
        if (oldPublicId != null && newPublicId != null && !oldPublicId.equals(newPublicId)) {
            try {
                logger.info("Deleting old image from Cloudinary: {}", oldPublicId);
                // Gọi Cloudinary xoá ảnh theo publicId; Map.of() là options rỗng
                cloudinary.uploader().destroy(oldPublicId, Map.of());
            } catch (Exception e) {
                // Nếu xoá ảnh thất bại thì log lỗi nhưng KHÔNG rollback transaction (tránh ảnh hưởng tới update dữ liệu)
                logger.error("Failed to delete old image from Cloudinary: {}. Error: {}", oldPublicId, e.getMessage());
            }
        }

        // 9) Trả về DTO sau khi cập nhật
        return vehicleModelMapper.toResponse(saved);
    }

    @Override
    @Transactional // Xoá là thao tác ghi DB
    public void delete(Long id) {
        // 1) Tìm VehicleModel cần xoá
        VehicleModel vm = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ErrorException("VehicleModel not found with id " + id));

        // 2) Kiểm tra ràng buộc: model có đang được xe nào sử dụng không
        long usage = userVehicleService.countByModel_ModelId(id);
        if (usage > 0) {
            // Nếu đang có xe dùng model này → không cho xoá, ném ConflictException
            throw new ConflictException("Cannot delete VehicleModel in use by " + usage + " vehicle(s)");
        }

        // 3) Xoá ảnh trên Cloudinary TRƯỚC khi xoá record trong DB (nếu có publicId)
        String publicId = vm.getImagePublicId();
        if (publicId != null && !publicId.isEmpty()) {
            try {
                logger.info("Deleting image from Cloudinary: {}", publicId);
                cloudinary.uploader().destroy(publicId, Map.of());
            } catch (Exception e) {
                // Log lỗi nhưng không dừng việc xoá model (tuỳ theo policy hệ thống)
                logger.error("Failed to delete image from Cloudinary: {}. Error: {}", publicId, e.getMessage());
            }
        }

        // 4) Xoá VehicleModel khỏi DB
        vehicleModelRepository.delete(vm);
    }

    @Override
    @Transactional
    public VehicleModelResponse updateStatus(Long id, VehicleModelStatus status) {
        // 1) Validate tham số: status không được null
        if (status == null) {
            throw new ErrorException("Status must not be null");
        }

        // 2) Lấy VehicleModel theo id
        VehicleModel vm = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ErrorException("VehicleModel not found with id " + id));

        // 3) Nếu status không đổi, không cần xử lý gì thêm -> trả luôn DTO hiện tại
        if (vm.getStatus() == status) {
            return vehicleModelMapper.toResponse(vm);
        }

        // 4) Rule nghiệp vụ:
        //    Không cho set sang INACTIVE nếu đang có xe sử dụng model này
//        if (status == VehicleModelStatus.INACTIVE) {
//            long usage = userVehicleService.countByModel_ModelId(id);
//            if (usage > 0) {
//                throw new ConflictException(
//                        "Cannot set status to INACTIVE. Model is used by " + usage + " vehicle(s)"
//                );
//            }
//        }

        // 5) Lưu lại status cũ để log
        VehicleModelStatus oldStatus = vm.getStatus();

        // 6) Cập nhật status mới và lưu lại DB
        vm.setStatus(status);
        VehicleModel saved = vehicleModelRepository.save(vm);

        // 7) Ghi log thay đổi trạng thái
        logger.info("Updated VehicleModel(id={}) status from {} to {}", id, oldStatus, status);

        // 8) Trả về DTO
        return vehicleModelMapper.toResponse(saved);
    }

    @Override
    public Optional<VehicleModel> findById(Long modelId) {
        // Trả về Optional<VehicleModel> cho chỗ dùng nội bộ (không map DTO)
        return vehicleModelRepository.findById(modelId);
    }


}
